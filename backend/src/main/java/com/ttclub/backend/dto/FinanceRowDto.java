package com.ttclub.backend.dto;

import java.math.BigDecimal;
import java.sql.Timestamp;

public record FinanceRowDto(
        Long id,
        Timestamp ts,
        String action,
        BigDecimal subtotal,
        BigDecimal shipping,
        BigDecimal tax,
        BigDecimal total,
        String status
) {}
