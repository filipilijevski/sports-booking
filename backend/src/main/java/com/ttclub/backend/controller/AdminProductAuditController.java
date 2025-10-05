/* src/main/java/com/ttclub/backend/controller/AdminProductAuditController.java */
package com.ttclub.backend.controller;

import com.ttclub.backend.model.ProductAudit;
import com.ttclub.backend.repository.ProductAuditRepository;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/** Admin endpoint to view recent product admin activity. */
@RestController
@RequestMapping("/api/admin/products/audit")
@PreAuthorize("hasRole('ADMIN') or hasRole('OWNER')")
public class AdminProductAuditController {

    private final ProductAuditRepository repo;

    public AdminProductAuditController(ProductAuditRepository repo) {
        this.repo = repo;
    }

    @GetMapping
    @ResponseStatus(HttpStatus.OK)
    public List<ProductAudit> recent(@RequestParam(defaultValue = "200") int limit) {
        int n = Math.max(1, Math.min(1000, limit));
        return repo.findAllByOrderByCreatedAtDesc(PageRequest.of(0, n));
    }
}
