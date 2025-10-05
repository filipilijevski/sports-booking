package com.ttclub.backend.service;

import com.ttclub.backend.dto.FinanceRowDto;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.springframework.stereotype.Service;

import java.sql.Timestamp;
import java.util.List;

@Service
public class FinanceService {

    @PersistenceContext
    private EntityManager em;

    @SuppressWarnings("unchecked")
    public List<FinanceRowDto> fetchBetween(Timestamp from, Timestamp to) {
        return em.createNativeQuery("""
                SELECT id,
                       ts,
                       action,
                       subtotal,
                       shipping,
                       tax,
                       total,
                       status
                FROM finance_ledger
                WHERE ts BETWEEN :from AND :to
                ORDER BY ts
                """, "FinanceRowMapping")
                .setParameter("from", from)
                .setParameter("to", to)
                .getResultList();
    }
}
