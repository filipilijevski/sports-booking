/* src/main/java/com/ttclub/backend/service/ProductAuditService.java */
package com.ttclub.backend.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ttclub.backend.model.Product;
import com.ttclub.backend.model.ProductAudit;
import com.ttclub.backend.model.User;
import com.ttclub.backend.repository.ProductAuditRepository;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.util.LinkedHashMap;
import java.util.Map;

@Service
public class ProductAuditService {

    private final ProductAuditRepository repo;
    private final ObjectMapper om = new ObjectMapper();

    public ProductAuditService(ProductAuditRepository repo) {
        this.repo = repo;
    }

    public void logCreate(Product p, boolean imageModified) {
        ProductAudit a = base("CREATE", p, imageModified);
        repo.save(a);
    }

    public void logUpdate(Product before, Product after, boolean imageModified) {
        ProductAudit a = base("UPDATE", after, imageModified);
        a.setDetailsJson(diff(before, after));
        repo.save(a);
    }

    public void logStockChange(Product p, int newQty) {
        ProductAudit a = base("STOCK", p, false);
        a.setDetailsJson(json(Map.of("newInventoryQty", newQty)));
        repo.save(a);
    }

    public void logImageChange(Product p) {
        ProductAudit a = base("IMAGE", p, true);
        repo.save(a);
    }

    public void logDelete(Product snapshot) {
        ProductAudit a = base("DELETE", snapshot, false);
        repo.save(a);
    }

    private ProductAudit base(String action, Product p, boolean imageModified) {
        ProductAudit a = new ProductAudit();
        a.setAction(action);
        a.setProductId(p.getId());
        a.setSku(p.getSku());
        a.setName(p.getName());
        a.setPrice(p.getPrice());
        a.setInventoryQty(p.getInventoryQty());
        a.setBrand(p.getBrand());
        a.setGrams(p.getGrams());
        if (p.getCategory() != null) {
            a.setCategoryId(p.getCategory().getId());
            a.setCategoryName(p.getCategory().getName());
        }
        a.setImageModified(imageModified);
        a.setActorUserId(currentUserId());
        return a;
    }

    private Long currentUserId() {
        try {
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            if (auth != null && auth.getPrincipal() instanceof User u) {
                return u.getId();
            }
        } catch (Exception ignore) { }
        return null;
    }

    private String diff(Product b, Product a) {
        Map<String, Object> m = new LinkedHashMap<>();
        if (neq(b.getSku(), a.getSku())) m.put("sku", map(b.getSku(), a.getSku()));
        if (neq(b.getName(), a.getName())) m.put("name", map(b.getName(), a.getName()));
        if (neq(b.getPrice(), a.getPrice())) m.put("price", map(b.getPrice(), a.getPrice()));
        if (neq(b.getInventoryQty(), a.getInventoryQty())) m.put("inventoryQty", map(b.getInventoryQty(), a.getInventoryQty()));
        if (neq(b.getBrand(), a.getBrand())) m.put("brand", map(b.getBrand(), a.getBrand()));
        if (neq(b.getGrams(), a.getGrams())) m.put("grams", map(b.getGrams(), a.getGrams()));
        Long bc = b.getCategory() == null ? null : b.getCategory().getId();
        Long ac = a.getCategory() == null ? null : a.getCategory().getId();
        if (neq(bc, ac)) m.put("categoryId", map(bc, ac));
        return json(m);
    }

    private static boolean neq(Object x, Object y) {
        return (x == null && y != null) || (x != null && !x.equals(y));
    }

    private static Map<String, Object> map(Object from, Object to) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("from", from);
        m.put("to", to);
        return m;
    }

    private String json(Object o) {
        try { return om.writeValueAsString(o); } catch (Exception e) { return null; }
    }
}
