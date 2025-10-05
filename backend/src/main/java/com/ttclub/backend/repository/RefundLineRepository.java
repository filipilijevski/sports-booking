package com.ttclub.backend.repository;

import com.ttclub.backend.model.RefundLine;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface RefundLineRepository extends JpaRepository<RefundLine, Long> {

    @Query("""
           select coalesce(sum(rl.quantity), 0)
             from RefundLine rl
            where rl.orderItem.id = :orderItemId
           """)
    int sumRefundedQtyForOrderItem(Long orderItemId);
}
