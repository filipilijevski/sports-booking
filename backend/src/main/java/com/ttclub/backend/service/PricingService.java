package com.ttclub.backend.service;

import com.ttclub.backend.model.Order;
import com.ttclub.backend.model.ShippingMethod;
import jakarta.transaction.Transactional;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;

@Service
@Transactional
public class PricingService {

    private final ShippingRateProvider shipping;
    private final TaxService           tax;

    public PricingService(ShippingRateProvider shipping, TaxService tax) {
        this.shipping = shipping;
        this.tax      = tax;
    }

    /**
     * Computes shipping, tax and grand-total in one place so the
     * numbers are always consistent across the app.
     */
    public void price(Order order) {

        BigDecimal shippingFee = shipping.rateFor(
                order.getShippingMethod(),
                order.getItems(),
                order.getShippingAddress());

        BigDecimal taxable = order.getSubtotalAmount().add(shippingFee);
        BigDecimal taxAmount = tax.calculate(taxable);

        order.setShippingAmount(shippingFee);
        order.setTaxAmount(taxAmount);

        /* Coupons subtract discountAmount here */
        BigDecimal grand = taxable.add(taxAmount).subtract(order.getDiscountAmount());
        order.setTotalAmount(grand);
    }
}
