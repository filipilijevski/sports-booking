package com.ttclub.backend.service;

import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;

/** Ontario HST - 13 % applied to (goods + shipping). */
@Service
public class TaxService {

    private static final BigDecimal RATE = new BigDecimal("0.13");

    public BigDecimal calculate(BigDecimal taxableBase) {
        return taxableBase
                .multiply(RATE)
                .setScale(2, RoundingMode.HALF_UP);
    }
}
