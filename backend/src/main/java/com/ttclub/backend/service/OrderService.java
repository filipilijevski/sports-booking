package com.ttclub.backend.service;

import com.ttclub.backend.dto.*;
import com.ttclub.backend.mapper.OrderMapper;
import com.ttclub.backend.model.*;
import com.ttclub.backend.model.Order;
import com.ttclub.backend.repository.*;
import jakarta.persistence.*;
import jakarta.persistence.criteria.*;
import jakarta.transaction.Transactional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.*;

@Service
@Transactional
public class OrderService {

    public enum DiscountBase { PRE_TAX, POST_TAX }

    private static final Logger log = LoggerFactory.getLogger(OrderService.class);

    private final CartRepository      carts;
    private final OrderRepository     orders;
    private final ProductRepository   products;
    private final OrderMapper         mapper;
    private final UserRepository      users;
    private final RoleRepository      roles;
    private final PasswordEncoder     encoder;
    private final PricingService      pricing;
    private final TaxService          tax;
    private final CouponRepository    coupons;
    private final PaymentEventRepository paymentEvents;
    private final JavaMailSender      mailer;   

    @PersistenceContext
    private EntityManager em;

    public OrderService(CartRepository carts,
                        OrderRepository orders,
                        ProductRepository products,
                        OrderMapper mapper,
                        UserRepository users,
                        RoleRepository roles,
                        PasswordEncoder encoder,
                        PricingService pricing,
                        TaxService tax,
                        CouponRepository coupons,
                        PaymentEventRepository paymentEvents,
                        JavaMailSender mailer) {         
        this.carts    = carts;
        this.orders   = orders;
        this.products = products;
        this.mapper   = mapper;
        this.users    = users;
        this.roles    = roles;
        this.encoder  = encoder;
        this.pricing  = pricing;
        this.tax      = tax;
        this.coupons  = coupons;
        this.paymentEvents = paymentEvents;
        this.mailer   = mailer;  
    }

    /* Admin Search */

    @PreAuthorize("hasRole('ADMIN') or hasRole('OWNER')")
    public List<OrderDto> search(OrderSearchFilter f) {

        CriteriaBuilder cb = em.getCriteriaBuilder();
        CriteriaQuery<Order> cq = cb.createQuery(Order.class);
        Root<Order> root = cq.from(Order.class);
        Join<Order, User> user = root.join("user", JoinType.LEFT);   // LEFT join to include guest orders

        List<Predicate> ps = new ArrayList<>();

        if (f.getOrderId() != null) {
            ps.add(cb.equal(root.get("id"), f.getOrderId()));
        }

        // email filter: match user.email OR shipping_email (for guests)
        if (StringUtils.hasText(f.getEmail())) {
            String needle = "%" + f.getEmail().toLowerCase() + "%";
            Expression<String> userEmail = cb.lower(user.get("email"));
            Expression<String> shipEmail = cb.lower(root.get("shippingAddress").get("email"));
            ps.add(cb.or(
                    cb.like(userEmail, needle),
                    cb.like(shipEmail, needle)
            ));
        }

        // name filter: match user first+last OR shipping_full_name (for guests)
        if (StringUtils.hasText(f.getName())) {
            String needle = "%" + f.getName().toLowerCase() + "%";
            Expression<String> fullUser =
                    cb.concat(cb.concat(cb.coalesce(cb.lower(user.get("firstName")), ""), " "),
                            cb.coalesce(cb.lower(user.get("lastName")), ""));
            Expression<String> shipName = cb.lower(root.get("shippingAddress").get("fullName"));
            ps.add(cb.or(
                    cb.like(fullUser, needle),
                    cb.like(shipName, needle)
            ));
        }

        if (f.getStatus() != null) {
            ps.add(cb.equal(root.get("status"), f.getStatus()));
        }

        if (f.getMinTotal() != null) {
            ps.add(cb.greaterThanOrEqualTo(root.get("totalAmount"), f.getMinTotal()));
        }
        if (f.getMaxTotal() != null) {
            ps.add(cb.lessThanOrEqualTo(root.get("totalAmount"), f.getMaxTotal()));
        }

        if (f.getFrom() != null) {
            ps.add(cb.greaterThanOrEqualTo(root.get("createdAt"), f.getFrom()));
        }
        if (f.getTo() != null) {
            ps.add(cb.lessThanOrEqualTo(root.get("createdAt"), f.getTo()));
        }

        if (StringUtils.hasText(f.getOrigin())) {
            String o = f.getOrigin().trim().toUpperCase(Locale.ROOT);
            if ("ONLINE".equals(o)) {
                ps.add(cb.isNotNull(root.get("stripePaymentIntentId")));
            } else if ("IN_PERSON".equals(o) || "OFFLINE".equals(o)) {
                ps.add(cb.isNull(root.get("stripePaymentIntentId")));
            }
        }

        if (StringUtils.hasText(f.getOfflinePaymentMethod())) {
            try {
                OfflinePaymentMethod m = OfflinePaymentMethod.valueOf(
                        f.getOfflinePaymentMethod().trim().toUpperCase(Locale.ROOT));
                ps.add(cb.equal(root.get("offlinePaymentMethod"), m));
            } catch (IllegalArgumentException ignore) { /* invalid filter -> ignore */ }
        }

        // Exclude PENDING_PAYMENT and CANCELLED by default (unless explicitly asked)
        if (f.getStatus() != OrderStatus.PENDING_PAYMENT && !f.isIncludePendingPayment()) {
            ps.add(cb.notEqual(root.get("status"), OrderStatus.PENDING_PAYMENT));
        }
        if (f.getStatus() != OrderStatus.CANCELLED) {
            ps.add(cb.notEqual(root.get("status"), OrderStatus.CANCELLED));
        }

        cq.where(ps.toArray(Predicate[]::new))
                .orderBy(cb.desc(root.get("createdAt")));

        return mapper.toDtoList(em.createQuery(cq).setMaxResults(200).getResultList());
    }

    /* Checkout Flows */

    private void cancelPendingOrdersForUser(Long userId) {
        em.createQuery("""
                update Order o
                   set o.status = :cancelled
                 where o.user.id = :uid
                   and o.status  = :pending
                """)
                .setParameter("cancelled", OrderStatus.CANCELLED)
                .setParameter("pending",   OrderStatus.PENDING_PAYMENT)
                .setParameter("uid",       userId)
                .executeUpdate();
    }

    private void clearCartForUser(Long userId) {
        carts.findByUserId(userId).ifPresent(cart -> {
            List<CartItem> copy = new ArrayList<>(cart.getItems());
            copy.forEach(cart::removeItem);
            carts.save(cart);
        });
    }

    public OrderDto placeOrder(Long userId, ShippingAddress addr, ShippingMethod method) {
        cancelPendingOrdersForUser(userId);

        Cart cart = carts.findByUserId(userId)
                .orElseThrow(() -> new IllegalStateException("Cart is empty"));
        if (cart.getItems().isEmpty())
            throw new IllegalStateException("Cart is empty");

        // IMPORTANT: do NOT subtract pending holds. Inventory is released until PAID.
        for (CartItem ci : cart.getItems()) {
            Product p = em.find(Product.class, ci.getProduct().getId(), LockModeType.PESSIMISTIC_WRITE);
            int avail = p.getInventoryQty();
            if (ci.getQuantity() > avail)
                throw new IllegalArgumentException("Not enough stock for " + p.getName());
        }

        Order order = buildOrderSkeleton(cart.getUser(), addr, method);

        BigDecimal subtotal = BigDecimal.ZERO;
        for (CartItem ci : cart.getItems()) {
            subtotal = subtotal.add(addLine(order, ci.getProduct(), ci.getQuantity(), ci.getUnitPrice()));
        }
        initialiseTotals(order, subtotal);

        pricing.price(order);

        return mapper.toDto(orders.save(order));
    }

    public Order placeOrderEntity(Long userId, ShippingAddress addr, ShippingMethod method) {
        Long id = placeOrder(userId, addr, method).getId();
        return orders.findById(id).orElseThrow();
    }

    public Order placeGuestOrder(GuestCheckoutRequestDto req, ShippingAddress addr, ShippingMethod method) {
        // Guests do not create users; proceed with a null-user order using the provided shipping/contact
        // Coupon usage for guests is blocked at the controller level.

        Order order = buildOrderSkeleton(null, addr, method); // user = null (guest)

        BigDecimal subtotal = BigDecimal.ZERO;
        for (GuestCartItemDto li : req.items()) {
            Product p = em.find(Product.class, li.productId(), LockModeType.PESSIMISTIC_WRITE);
            if (p == null)
                throw new IllegalArgumentException("Product " + li.productId() + " not found");

            int avail = p.getInventoryQty(); // do not subtract pending holds
            if (li.quantity() > avail)
                throw new IllegalArgumentException("Not enough stock for " + p.getName());

            subtotal = subtotal.add(addLine(order, p, li.quantity(), p.getPrice()));
        }
        initialiseTotals(order, subtotal);

        pricing.price(order);
        return orders.save(order);
    }

    /* Using Coupons */

    /**
     * Applies a coupon to the given order. Works for both transient (no ID yet)
     * and managed/detached orders. If the order already has an ID, the updated
     * totals are persisted; otherwise we only mutate the passed instance.
     */
    public void applyCoupon(Order order, String rawCode, DiscountBase mode) {
        if (order == null || !StringUtils.hasText(rawCode)) return;

        if (order.getUser() == null) {
            // Defensive: guest orders cannot use coupons (enforced earlier too).
            throw new IllegalArgumentException("Coupons require a logged-in customer.");
        }

        // Determine which instance we'll operate on without causing findById(null).
        Order managed = order;
        boolean hasId = order.getId() != null;

        if (!em.contains(order)) {
            if (hasId) {
                managed = orders.findById(order.getId()).orElse(null);
                if (managed == null) {
                    managed = em.merge(order);
                }
            } else {
                managed = order;
            }
        }

        String code = rawCode.trim();
        Coupon c = coupons.findByCodeIgnoreCase(code)
                .orElseThrow(() -> new IllegalArgumentException("Invalid coupon code."));

        if (!c.isCurrentlyActive()) {
            throw new IllegalArgumentException("Coupon is not active.");
        }

        Long currentOrderId = managed.getId(); // may be null
        if (hasUserUsedCoupon(managed.getUser().getId(), c.getId(), currentOrderId)) {
            throw new IllegalArgumentException("You have already used this coupon.");
        }

        BigDecimal spendBase = managed.getSubtotalAmount().add(managed.getShippingAmount());
        if (!c.meetsMinSpend(spendBase)) {
            throw new IllegalArgumentException("Order does not meet coupon minimum spend.");
        }

        BigDecimal discountBase = (mode == DiscountBase.POST_TAX)
                ? spendBase.add(managed.getTaxAmount())
                : spendBase;

        BigDecimal discount = c.computeDiscount(discountBase);

        managed.setCoupon(c);
        managed.setDiscountAmount(discount);
        managed.recalculateTotals();

        if (managed.getId() != null) {
            orders.save(managed);
            em.flush();
        }

        if (order != managed) {
            order.setCoupon(managed.getCoupon());
            order.setDiscountAmount(managed.getDiscountAmount());
            order.setSubtotalAmount(managed.getSubtotalAmount());
            order.setShippingAmount(managed.getShippingAmount());
            order.setTaxAmount(managed.getTaxAmount());
            order.setTotalAmount(managed.getTotalAmount());
        }
    }

    /** Returns true if the user has any prior non-cancelled order with this coupon */
    private boolean hasUserUsedCoupon(Long userId, Long couponId, Long currentOrderId) {
        Number n = (Number) em.createQuery("""
                select count(o)
                from Order o
                where o.user.id   = :uid
                  and o.coupon.id = :cid
                  and (:curId is null or o.id <> :curId)
                  and o.status in (:paid, :fulfilled, :refunded)
                """)
                .setParameter("uid", userId)
                .setParameter("cid", couponId)
                .setParameter("curId", currentOrderId)
                .setParameter("paid", OrderStatus.PAID)
                .setParameter("fulfilled", OrderStatus.FULFILLED)
                .setParameter("refunded", OrderStatus.REFUNDED)
                .getSingleResult();
        return n != null && n.longValue() > 0;
    }

    /** Public helper used by the validate endpoint. */
    @PreAuthorize("isAuthenticated()")
    public boolean hasUserUsedCouponPublic(Long userId, Long couponId) {
        return hasUserUsedCoupon(userId, couponId, null);
    }

    /* Manual or In-Person Checkout */

    @PreAuthorize("hasRole('ADMIN') or hasRole('OWNER')")
    public OrderDto placeOfflineOrderFromAdminCart(Long adminUserId, ManualCheckoutRequestDto req) {

        User buyer = null;
        boolean wantsCoupon = StringUtils.hasText(req.getCouponCode());

        if (req.getClientUserId() != null) {
            // Explicit association to an existing user id
            buyer = users.findById(req.getClientUserId())
                    .orElseThrow(() -> new IllegalArgumentException("Client user not found"));
        } else {
            // Name/email/phone must be provided for offline guest orders
            if (!StringUtils.hasText(req.getClientEmail()) ||
                    !StringUtils.hasText(req.getClientFullName()) ||
                    !StringUtils.hasText(req.getClientPhone())) {
                throw new IllegalArgumentException("Full client name, email and phone are required.");
            }

            String email = req.getClientEmail().trim();
            boolean exists = users.existsByEmail(email);

            // Only existing users may use coupons offline
            if (wantsCoupon && !exists) {
                throw new IllegalArgumentException(
                        "Only authenticated (existing) users may use a coupon. Please associate an existing member before applying a coupon.");
            }

            if (exists) {
                buyer = users.findByEmail(email).orElseThrow();
            } else {
                // do not create a user for manual guest orders. Proceed as guest (buyer = null).
                buyer = null;
            }
        }

        Cart adminCart = carts.findByUserId(adminUserId)
                .orElseThrow(() -> new IllegalStateException("Admin cart is empty"));
        if (adminCart.getItems().isEmpty())
            throw new IllegalStateException("Admin cart is empty");

        // Check stock without subtracting pending holds
        for (CartItem ci : adminCart.getItems()) {
            Product p = em.find(Product.class, ci.getProduct().getId(), LockModeType.PESSIMISTIC_WRITE);
            int avail = p.getInventoryQty();
            if (ci.getQuantity() > avail)
                throw new IllegalArgumentException("Not enough stock for " + p.getName());
        }

        // Build shipping address: prefer provided shippingAddress; else derive from client info (so email can be sent)
        ShippingAddress addr = null;
        if (req.getShippingAddress() != null) {
            addr = new ShippingAddress();
            addr.setFullName(req.getShippingAddress().getFullName());
            addr.setPhone(req.getShippingAddress().getPhone());
            addr.setEmail(req.getShippingAddress().getEmail());
            addr.setLine1(req.getShippingAddress().getLine1());
            addr.setLine2(req.getShippingAddress().getLine2());
            addr.setCity(req.getShippingAddress().getCity());
            addr.setProvince(req.getShippingAddress().getProvince());
            addr.setPostalCode(req.getShippingAddress().getPostalCode());
            addr.setCountry(req.getShippingAddress().getCountry());
        } else {
            addr = new ShippingAddress();
            addr.setFullName(req.getClientFullName());
            addr.setPhone(req.getClientPhone());
            addr.setEmail(req.getClientEmail());
        }

        Order order = buildOrderSkeleton(buyer, addr, req.getShippingMethod());

        BigDecimal subtotal = BigDecimal.ZERO;
        for (CartItem ci : adminCart.getItems()) {
            subtotal = subtotal.add(addLine(order, ci.getProduct(), ci.getQuantity(), ci.getUnitPrice()));
        }
        initialiseTotals(order, subtotal);

        if (req.getShippingFee() != null) {
            order.setShippingAmount(req.getShippingFee());
            BigDecimal taxable = order.getSubtotalAmount().add(order.getShippingAmount());
            order.setTaxAmount(tax.calculate(taxable));
            order.recalculateTotals();
        } else {
            pricing.price(order);
        }

        // Apply coupon (if provided) post-tax — only allowed when buyer != null (existing user)
        if (wantsCoupon) {
            if (buyer == null) {
                throw new IllegalArgumentException("Coupons require an existing authenticated user.");
            }
            applyCoupon(order, req.getCouponCode(), DiscountBase.POST_TAX);
        }

        order.setStatus(OrderStatus.PAID);
        order.setOfflinePaymentMethod(resolveOfflineMethod(req.getPaymentMethod()));
        orders.save(order);

        adjustInventory(order, -1);
        clearCartForUser(adminUserId);

        PaymentEvent ev = new PaymentEvent();
        ev.setOrder(order);
        ev.setProvider("OFFLINE");
        String method = order.getOfflinePaymentMethod() == null
                ? "OFFLINE" : order.getOfflinePaymentMethod().name();
        ev.setProviderTxnId(method + "-" + order.getId() + "-" + Instant.now().toEpochMilli()); // ensure unique
        ev.setAmount(order.getTotalAmount());
        ev.setCurrency("cad");
        ev.setStatus("succeeded");
        ev.setEventType("manual_checkout");
        ev.setPayloadJson(null);
        paymentEvents.save(ev);

        // Email confirmation (manual orders are paid immediately)
        safeSendConfirmationEmail(order);

        return mapper.toDto(order);
    }

    private OfflinePaymentMethod resolveOfflineMethod(String raw) {
        if (!StringUtils.hasText(raw)) return null;
        try {
            return OfflinePaymentMethod.valueOf(raw.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException ex) {
            return OfflinePaymentMethod.OTHER;
        }
    }

    private int pendingReservedQty(Long productId) {
        // kept for compatibility - NOT used in availability checks anymore
        Number n = (Number) em.createQuery("""
                select coalesce(sum(oi.quantity),0)
                from OrderItem oi
                where oi.product.id = :pid
                  and oi.order.status = :status
                """)
                .setParameter("pid", productId)
                .setParameter("status", OrderStatus.PENDING_PAYMENT)
                .getSingleResult();
        return n.intValue();
    }

    private void adjustInventory(Order order, int factor) {
        for (OrderItem oi : order.getItems()) {
            Product p = em.find(Product.class, oi.getProduct().getId(), LockModeType.PESSIMISTIC_WRITE);
            int cur = p.getInventoryQty();
            int delta = factor * oi.getQuantity();
            int next = cur + delta;

            if (delta < 0 && next < 0) {
                // Guard against oversell; do not allow negative stock.
                throw new IllegalStateException("Insufficient stock while finalizing payment for product " + p.getName());
            }
            p.setInventoryQty(next);
        }
    }

    public void attachPaymentIntent(Long orderId, String intentId) {
        orders.findById(orderId).ifPresent(o -> {
            o.setStripePaymentIntentId(intentId);
            orders.save(o);     // ensure persisted even if controller is non-transactional
        });
    }

    public void syncPaymentStatus(String intentId, String newStatus) {
        orders.findByStripePaymentIntentId(intentId).ifPresent(order -> {
            OrderStatus before = order.getStatus();
            switch (newStatus) {
                case "succeeded" -> {
                    if (order.getStatus() == OrderStatus.PENDING_PAYMENT) {
                        try {
                            adjustInventory(order, -1);  // decrement only on capture
                            order.setStatus(OrderStatus.PAID);
                            if (order.getUser() != null) {
                                clearCartForUser(order.getUser().getId());
                            }
                        } catch (IllegalStateException ex) {
                            // Inventory no longer available at capture time - cancel the order (no oversell)
                            log.warn("Payment succeeded but stock was insufficient. Cancelling order {}. Reason: {}",
                                    order.getId(), ex.getMessage());
                            order.setStatus(OrderStatus.CANCELLED);
                        }
                    }
                    // send confirmation only if we just transitioned to PAID
                    if (before != OrderStatus.PAID && order.getStatus() == OrderStatus.PAID) {
                        safeSendConfirmationEmail(order);
                    }
                }
                case "payment_failed", "canceled" -> {
                    order.setStatus(OrderStatus.CANCELLED);
                }
                default -> { /* ignore */ }
            }
            orders.save(order);
        });
    }

    public List<OrderDto> listOwn(Long userId) {
        return mapper.toDtoList(orders.findByUserId(userId));
    }

    public OrderDto getOwn(Long orderId, Long userId) {
        Order o = orders.findById(orderId)
                .orElseThrow(() -> new IllegalArgumentException("Order not found"));
        if (o.getUser() == null || !o.getUser().getId().equals(userId))
            throw new SecurityException("Not your order");
        return mapper.toDto(o);
    }

    @PreAuthorize("hasRole('ADMIN') or hasRole('OWNER')")
    public List<OrderDto> listAll() {
        return mapper.toDtoList(orders.findAll());
    }

    @PreAuthorize("hasRole('ADMIN') or hasRole('OWNER')")
    public OrderDto markFulfilled(Long orderId) {
        Order o = orders.findById(orderId)
                .orElseThrow(() -> new IllegalArgumentException("Order not found"));
        o.setStatus(OrderStatus.FULFILLED);
        return mapper.toDto(o);
    }

    @PreAuthorize("hasRole('ADMIN') or hasRole('OWNER')")
    public void markCancelled(Long id) {
        orders.findById(id).ifPresent(o -> o.setStatus(OrderStatus.CANCELLED));
    }

    private Order buildOrderSkeleton(User buyer, ShippingAddress addr, ShippingMethod method) {
        Order o = new Order();
        o.setUser(buyer); // may be null for guest
        o.setStatus(OrderStatus.PENDING_PAYMENT);
        o.setShippingAddress(addr);
        o.setShippingMethod(method);
        return o;
    }

    private BigDecimal addLine(Order order, Product p, int qty, BigDecimal unitPrice) {
        OrderItem oi = new OrderItem();
        oi.setOrder(order);
        oi.setProduct(p);
        oi.setQuantity(qty);
        oi.setUnitPrice(unitPrice);

        BigDecimal total = unitPrice.multiply(BigDecimal.valueOf(qty));
        oi.setTotalPrice(total);

        order.addItem(oi);
        return total;
    }

    private void initialiseTotals(Order o, BigDecimal subtotal) {
        o.setSubtotalAmount(subtotal);
        o.setShippingAmount(BigDecimal.ZERO);
        o.setTaxAmount(BigDecimal.ZERO);
        o.setTotalAmount(subtotal);
        o.setDiscountAmount(BigDecimal.ZERO);
    }

    /* Email confirmation (guests and users) */

    private void safeSendConfirmationEmail(Order order) {
        try {
            sendOrderConfirmationEmail(order);
        } catch (Exception ex) {
            // Never break the payment flow because of email issues.
            log.error("Failed to send order confirmation for order {}: {}", order.getId(), ex.getMessage());
        }
    }

    private void sendOrderConfirmationEmail(Order order) {
        String to = null;
        String name = "Customer";
        String phone = null;

        if (order.getShippingAddress() != null) {
            to = firstNonBlank(order.getShippingAddress().getEmail(), null);
            name = firstNonBlank(order.getShippingAddress().getFullName(), name);
            phone = order.getShippingAddress().getPhone();
        }
        if (to == null && order.getUser() != null) {
            to = order.getUser().getEmail();
            String fn = order.getUser().getFirstName();
            String ln = order.getUser().getLastName();
            String nn = ((fn == null ? "" : fn) + " " + (ln == null ? "" : ln)).trim();
            if (!nn.isBlank()) name = nn;
        }
        if (to == null || to.isBlank()) {
            log.info("Skipping confirmation email; no recipient for order {}", order.getId());
            return;
        }

        StringBuilder body = new StringBuilder(512);
        body.append("Hello ").append(name).append(",\n\n")
                .append("Thank you for your order. Your payment has been confirmed.\n\n")
                .append("Order #: ").append(order.getId()).append("\n")
                .append("Placed   : ").append(order.getCreatedAt()).append("\n")
                .append("Status   : ").append(order.getStatus()).append("\n\n");

        if (order.getShippingAddress() != null) {
            body.append("Shipping contact:\n");
            body.append("  Name   : ").append(firstNonBlank(order.getShippingAddress().getFullName(), "—")).append("\n");
            body.append("  Email  : ").append(firstNonBlank(order.getShippingAddress().getEmail(), "—")).append("\n");
            body.append("  Phone  : ").append(firstNonBlank(phone, "—")).append("\n");
            if (order.getShippingAddress().getLine1() != null) {
                body.append("  Address: ").append(order.getShippingAddress().getLine1());
                if (order.getShippingAddress().getLine2() != null && !order.getShippingAddress().getLine2().isBlank())
                    body.append(", ").append(order.getShippingAddress().getLine2());
                body.append(", ").append(firstNonBlank(order.getShippingAddress().getCity(), ""));
                if (order.getShippingAddress().getProvince() != null)
                    body.append(", ").append(order.getShippingAddress().getProvince());
                if (order.getShippingAddress().getPostalCode() != null)
                    body.append(" ").append(order.getShippingAddress().getPostalCode());
                if (order.getShippingAddress().getCountry() != null)
                    body.append(", ").append(order.getShippingAddress().getCountry());
                body.append("\n");
            }
            body.append("\n");
        }

        body.append("Items:\n");
        for (OrderItem it : order.getItems()) {
            body.append("  • ").append(it.getProduct().getName())
                    .append("  x").append(it.getQuantity())
                    .append(" @ ").append(it.getUnitPrice())
                    .append("   = ").append(it.getTotalPrice())
                    .append("\n");
        }
        body.append("\n");
        body.append("Subtotal : ").append(order.getSubtotalAmount()).append("\n");
        body.append("Shipping : ").append(order.getShippingAmount()).append("\n");
        body.append("Tax      : ").append(order.getTaxAmount()).append("\n");
        if (order.getDiscountAmount() != null && order.getDiscountAmount().signum() > 0) {
            body.append("Discount : -").append(order.getDiscountAmount()).append("\n");
        }
        body.append("Total    : ").append(order.getTotalAmount()).append("\n\n");
        body.append("If you have any questions, just reply to this email.\n");
        body.append("— TT Club");

        SimpleMailMessage msg = new SimpleMailMessage();
        msg.setTo(to);
        msg.setSubject("Your order #" + order.getId() + " is confirmed");
        msg.setText(body.toString());

        mailer.send(msg);
    }

    private static String firstNonBlank(String a, String b) {
        if (a != null && !a.isBlank()) return a;
        return b;
    }
}
