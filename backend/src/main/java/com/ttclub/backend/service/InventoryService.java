/* src/main/java/com/ttclub/backend/service/InventoryService.java */
package com.ttclub.backend.service;

import com.ttclub.backend.model.Product;
import com.ttclub.backend.repository.ProductRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Read-only helpers for available stock.
 * Policy: we DO NOT reserve inventory for PENDING_PAYMENT.
 * Availability == product.inventoryQty (non-negative).
 */
@Service
public class InventoryService {

    private final ProductRepository products;

    @PersistenceContext
    private EntityManager em;

    public InventoryService(ProductRepository products) {
        this.products = products;
    }

    public int availableQty(Long productId) {
        return products.findById(productId)
                .map(Product::getInventoryQty)
                .map(q -> Math.max(0, q))
                .orElse(0);
    }

    /** Bulk helper: each id -> available qty (0 for unknown ids). */
    public Map<Long, Integer> availabilityFor(Collection<Long> productIds) {
        if (productIds == null || productIds.isEmpty()) return Collections.emptyMap();
        Set<Long> ids = productIds.stream().filter(Objects::nonNull).collect(Collectors.toSet());
        Map<Long, Integer> invById = new HashMap<>();
        products.findAllById(ids).forEach(p -> invById.put(p.getId(), Math.max(0, p.getInventoryQty())));

        Map<Long, Integer> result = new LinkedHashMap<>();
        for (Long id : ids) result.put(id, invById.getOrDefault(id, 0));
        return result;
    }
}
