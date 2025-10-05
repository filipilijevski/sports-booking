package com.ttclub.backend.repository;

import com.ttclub.backend.model.RefundEvent;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface RefundEventRepository extends JpaRepository<RefundEvent, Long> {
    List<RefundEvent> findByOrderId(Long orderId);
}
