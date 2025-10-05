package com.ttclub.backend.service;

import com.ttclub.backend.model.OrderItem;
import com.ttclub.backend.model.ShippingAddress;
import com.ttclub.backend.model.ShippingMethod;

import java.math.BigDecimal;
import java.util.List;

/**
 * Strategy interface - different implementations can talk to
 * Canada Post, FedEx, manual tables, etc.
 */
public interface ShippingRateProvider {

    BigDecimal rateFor(ShippingMethod method,
                       List<OrderItem> items,
                       ShippingAddress to);
}
