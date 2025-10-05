/* src/main/java/com/ttclub/backend/repository/ProductAuditRepository.java */
package com.ttclub.backend.repository;

import com.ttclub.backend.model.ProductAudit;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface ProductAuditRepository extends JpaRepository<ProductAudit, Long> {
    List<ProductAudit> findAllByOrderByCreatedAtDesc(Pageable pageable);
}
