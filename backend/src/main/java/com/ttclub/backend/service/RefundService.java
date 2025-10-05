package com.ttclub.backend.service;

import com.stripe.exception.StripeException;
import com.stripe.model.PaymentIntent;
import com.stripe.model.Refund;
import com.stripe.param.RefundCreateParams;
import com.ttclub.backend.dto.RefundRequestDto;
import com.ttclub.backend.model.*;
import com.ttclub.backend.repository.OrderRepository;
import com.ttclub.backend.repository.RefundEventRepository;
import com.ttclub.backend.repository.RefundLineRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.LockModeType;
import jakarta.transaction.Transactional;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.*;

@Service
@Transactional
public class RefundService {

    private final OrderRepository        orders;
    private final RefundEventRepository  refundEvents;
    private final RefundLineRepository   refundLines;
    private final TaxService             tax;
    private final EntityManager          em;

    public RefundService(OrderRepository orders,
                         RefundEventRepository refundEvents,
                         RefundLineRepository refundLines,
                         TaxService tax,
                         EntityManager em) {
        this.orders       = orders;
        this.refundEvents = refundEvents;
        this.refundLines  = refundLines;
        this.tax          = tax;
        this.em           = em;
    }

    /**
     * Handles both full and partial refunds; supports online (Stripe) and offline orders.
     * Rules:<br>
     *  - If dto.lines are provided then we compute the refund amount from those lines
     *    (+ optional shipping) and restock exactly those quantities.<br>
     *  - If dto.amount is provided with NO lines then custom amount, no restock.<br>
     *  - If neither amount nor lines then full remaining refund (shipping is included
     *    if dto.refundShipping==TRUE, else excluded).
     */
    public void refund(Long orderId, RefundRequestDto dto) throws StripeException {
        Objects.requireNonNull(dto, "body must not be null");

        Order o = orders.findById(orderId)
                .orElseThrow(() -> new IllegalArgumentException("Order not found"));

        if (o.getStatus() != OrderStatus.PAID && o.getStatus() != OrderStatus.FULFILLED)
            throw new IllegalStateException("Only PAID / FULFILLED orders may be refunded");

        // Enforce 90-day refund window
        if (o.getCreatedAt().isBefore(Instant.now().minus(90, ChronoUnit.DAYS))) {
            throw new IllegalStateException("Refund window (90 days) has expired for this order.");
        }

        BigDecimal alreadyRefunded = sumRefundedAmount(orderId);
        BigDecimal remaining       = o.getTotalAmount().subtract(alreadyRefunded);
        if (remaining.compareTo(BigDecimal.ZERO) <= 0)
            throw new IllegalStateException("Order fully refunded already");

        /* Compute amount + shipping + accepted lines  */
        boolean hasLines     = dto.getLines() != null && !dto.getLines().isEmpty();
        boolean includeShip  = Boolean.TRUE.equals(dto.getRefundShipping()); // null/false -> exclude
        BigDecimal ask       = dto.getAmount();    // optional
        BigDecimal shippingAlreadyRefunded = sumShippingRefunded(orderId);

        // Build accepted lines (limited to remaining refundable qty per order-item)
        Map<OrderItem, Integer> accepted = new LinkedHashMap<>();
        if (hasLines) {
            dto.getLines().forEach(l -> {
                OrderItem oi = em.find(OrderItem.class, l.getOrderItemId());
                if (oi == null || !oi.getOrder().getId().equals(orderId)) {
                    return; // ignore foreign or missing
                }
                int alreadyQty = refundLines.sumRefundedQtyForOrderItem(oi.getId());
                int maxLeft    = Math.max(0, oi.getQuantity() - alreadyQty);
                int qty        = Math.max(0, Math.min(l.getQuantity(), maxLeft));
                if (qty > 0) accepted.put(oi, qty);
            });
            if (accepted.isEmpty()) {
                throw new IllegalArgumentException("No refundable quantity found for the selected lines.");
            }
        }

        // Derive amount if lines are provided, or for full-without-amount path
        BigDecimal computedAmount = null;
        BigDecimal shippingPortionForThisRefund = BigDecimal.ZERO;

        if (hasLines || ask == null) {
            // subtotal of selected items (or all items if full-with-lines absent)
            BigDecimal linesSubtotal = BigDecimal.ZERO;

            if (hasLines) {
                for (Map.Entry<OrderItem, Integer> e : accepted.entrySet()) {
                    BigDecimal lineTotal = e.getKey().getUnitPrice()
                            .multiply(BigDecimal.valueOf(e.getValue()));
                    linesSubtotal = linesSubtotal.add(lineTotal);
                }
            } else {
                // "full" path without lines: take all remaining quantities; avoid double-restock later
                for (OrderItem oi : o.getItems()) {
                    int alreadyQty = refundLines.sumRefundedQtyForOrderItem(oi.getId());
                    int maxLeft    = Math.max(0, oi.getQuantity() - alreadyQty);
                    if (maxLeft > 0) {
                        accepted.put(oi, maxLeft);
                        BigDecimal lineTotal = oi.getUnitPrice().multiply(BigDecimal.valueOf(maxLeft));
                        linesSubtotal = linesSubtotal.add(lineTotal);
                    }
                }
                if (accepted.isEmpty()) {
                    // no more quantities left to restock; this could be a pure-shipping remainder
                    linesSubtotal = BigDecimal.ZERO;
                }
            }

            // shipping portion (gross) available for this refund
            if (includeShip) {
                BigDecimal remainingShip = o.getShippingAmount().subtract(shippingAlreadyRefunded);
                if (remainingShip.compareTo(BigDecimal.ZERO) > 0) {
                    shippingPortionForThisRefund = remainingShip;
                }
            }

            BigDecimal preTaxBase = linesSubtotal.add(shippingPortionForThisRefund);
            BigDecimal taxAmt     = tax.calculate(preTaxBase);

            // coupon - discount applied post-tax
            BigDecimal orderPreDiscount = o.getSubtotalAmount()
                    .add(o.getShippingAmount())
                    .add(o.getTaxAmount());

            BigDecimal discountShare = BigDecimal.ZERO;
            if (orderPreDiscount.compareTo(BigDecimal.ZERO) > 0 && o.getDiscountAmount() != null) {
                BigDecimal ratio = preTaxBase.add(taxAmt)
                        .divide(orderPreDiscount, 8, RoundingMode.HALF_UP);
                discountShare = o.getDiscountAmount().multiply(ratio)
                        .setScale(2, RoundingMode.HALF_UP);
            }

            computedAmount = preTaxBase.add(taxAmt).subtract(discountShare)
                    .setScale(2, RoundingMode.HALF_UP);

            // Cap by remaining refundable balance
            if (computedAmount.compareTo(remaining) > 0) {
                computedAmount = remaining;
                // If we hit the cap due to rounding or earlier custom refunds, we still proceed.
            }
        }

        BigDecimal amount = (computedAmount != null) ? computedAmount : ask;
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Invalid refund amount");
        }
        if (amount.compareTo(remaining) > 0) amount = remaining;

        boolean eventIncludesShipping = shippingPortionForThisRefund.compareTo(BigDecimal.ZERO) > 0;

        /* Execute provider refund / persist event & lines */
        RefundEvent ev;

        if (o.getStripePaymentIntentId() != null) {
            PaymentIntent pi = PaymentIntent.retrieve(o.getStripePaymentIntentId());
            RefundCreateParams.Builder b = RefundCreateParams.builder()
                    .setPaymentIntent(pi.getId())
                    .setAmount(amount.movePointRight(2).longValueExact())
                    .setReason(resolveReason(dto.getReason()));
            Refund stripe = Refund.create(b.build());

            ev = new RefundEvent();
            ev.setOrder(o);
            ev.setProvider("STRIPE");
            ev.setProviderTxnId(stripe.getId());
            ev.setAmount(amount);
            ev.setCurrency(stripe.getCurrency());
            ev.setStatus(stripe.getStatus());
            ev.setReason(stripe.getReason());
            ev.setPayloadJson(stripe.toJson());
            ev.setIncludesShipping(eventIncludesShipping);
            ev.setShippingAmount(shippingPortionForThisRefund);
            refundEvents.save(ev);

        } else {
            // Offline refund: record and restock
            ev = new RefundEvent();
            ev.setOrder(o);
            ev.setProvider("OFFLINE");
            ev.setProviderTxnId(null);
            ev.setAmount(amount);
            ev.setCurrency("cad");
            ev.setStatus("succeeded");
            ev.setReason(dto.getReason());
            ev.setPayloadJson(null);
            ev.setIncludesShipping(eventIncludesShipping);
            ev.setShippingAmount(shippingPortionForThisRefund);
            refundEvents.save(ev);
        }

        // Persist refund lines (if any) & restock exactly those quantities
        if (!accepted.isEmpty()) {
            for (Map.Entry<OrderItem, Integer> e : accepted.entrySet()) {
                OrderItem oi = e.getKey();
                int qty = e.getValue();

                // Persist line
                RefundLine rl = new RefundLine();
                rl.setRefundEvent(ev);
                rl.setOrderItem(oi);
                rl.setQuantity(qty);
                ev.addLine(rl); // cascade saves it

                // Restock cautiously
                Product p = em.find(Product.class, oi.getProduct().getId(), LockModeType.PESSIMISTIC_WRITE);
                p.setInventoryQty(p.getInventoryQty() + qty);
            }
        }

        // If the order is now fully refunded, mark it REFUNDED
        BigDecimal newTotalRefunded = sumRefundedAmount(orderId);
        if (newTotalRefunded.compareTo(o.getTotalAmount()) >= 0) {
            o.setStatus(OrderStatus.REFUNDED);
        }
    }

    /** Convenience: full refund including shipping by default. */
    public void refundOrder(Long orderId, String reason) throws StripeException {
        RefundRequestDto dto = new RefundRequestDto();
        dto.setReason((reason == null || reason.isBlank()) ? "requested_by_customer" : reason);
        dto.setRefundShipping(Boolean.TRUE); // full refunds include shipping by default
        refund(orderId, dto);
    }

    /* helpers */

    private BigDecimal sumRefundedAmount(Long orderId) {
        return (BigDecimal) em.createQuery("""
                 select coalesce(sum(r.amount),0)
                 from RefundEvent r
                 where r.order.id = :oid
                 """)
                .setParameter("oid", orderId)
                .getSingleResult();
    }

    private BigDecimal sumShippingRefunded(Long orderId) {
        return (BigDecimal) em.createQuery("""
                select coalesce(sum(r.shippingAmount), 0)
                  from RefundEvent r
                 where r.order.id = :oid
                """).setParameter("oid", orderId)
                .getSingleResult();
    }

    private RefundCreateParams.Reason resolveReason(String r) {
        if (r == null || r.isBlank()) return RefundCreateParams.Reason.REQUESTED_BY_CUSTOMER;
        try {
            return RefundCreateParams.Reason.valueOf(r);
        } catch (IllegalArgumentException ex) {
            return RefundCreateParams.Reason.REQUESTED_BY_CUSTOMER;
        }
    }
}
