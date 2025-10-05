package com.ttclub.backend.service;

import com.ttclub.backend.dto.CouponDto;
import com.ttclub.backend.mapper.CouponMapper;
import com.ttclub.backend.model.Coupon;
import com.ttclub.backend.repository.CouponRepository;
import jakarta.transaction.Transactional;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Locale;

@Service
@Transactional
public class CouponService {

    private final CouponRepository repo;
    private final CouponMapper mapper;

    public CouponService(CouponRepository repo, CouponMapper mapper) {
        this.repo = repo;
        this.mapper = mapper;
    }

    /* Admin CRUD */

    public List<CouponDto> listAll() {
        return mapper.toDtoList(repo.findAll());
    }

    public CouponDto get(Long id) {
        Coupon c = repo.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Coupon not found"));
        return mapper.toDto(c);
    }

    public CouponDto create(CouponDto dto) {
        Coupon c = mapper.toEntity(dto);
        normalizeAndValidate(c, true);

        /* ensure NOT NULL columns align with DB defaults */
        Instant now = Instant.now();
        if (c.getCreatedAt() == null) c.setCreatedAt(now);
        if (c.getUpdatedAt() == null) c.setUpdatedAt(now);
        if (c.getStartsAt()  == null) c.setStartsAt(now); // defensive - normalizeAndValidate also sets this

        return mapper.toDto(repo.save(c));
    }

    public CouponDto update(Long id, CouponDto dto) {
        Coupon c = repo.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Coupon not found"));

        c.setCode(dto.getCode());
        c.setPercentOff(dto.getPercentOff());
        c.setAmountOff(dto.getAmountOff());
        c.setMinSpend(dto.getMinSpend());
        c.setStartsAt(dto.getStartsAt());
        c.setExpiresAt(dto.getExpiresAt());
        c.setActive(dto.isActive());

        normalizeAndValidate(c, false);
        c.setUpdatedAt(Instant.now()); // ensure updated_at is not null/old

        /* JPA will persist this */
        return mapper.toDto(c);
    }

    public void delete(Long id) { repo.deleteById(id); }

    private void normalizeAndValidate(Coupon c, boolean isCreate) {

        if (!StringUtils.hasText(c.getCode()))
            throw new IllegalArgumentException("code is required");

        c.setCode(c.getCode().trim().toUpperCase(Locale.ROOT));

        // Exactly one of percentOff / amountOff - DB also enforces.
        boolean hasPct = c.getPercentOff() != null;
        boolean hasAmt = c.getAmountOff() != null;
        if (hasPct == hasAmt) {
            throw new IllegalArgumentException("Specify exactly one of percentOff or amountOff.");
        }

        if (hasPct) {
            BigDecimal p = c.getPercentOff();
            if (p.compareTo(BigDecimal.ZERO) < 0 || p.compareTo(BigDecimal.ONE) > 0) {
                throw new IllegalArgumentException("percentOff must be between 0.0 and 1.0");
            }
        } else {
            if (c.getAmountOff().compareTo(BigDecimal.ZERO) < 0) {
                throw new IllegalArgumentException("amountOff must be >= 0");
            }
        }

        if (c.getStartsAt() == null) c.setStartsAt(Instant.now());
        if (c.getExpiresAt() == null)
            throw new IllegalArgumentException("expiresAt is required");

        if (c.getStartsAt().isAfter(c.getExpiresAt())) {
            throw new IllegalArgumentException("startsAt must be <= expiresAt");
        }

        // If expiry is in the past, force-disable immediately
        if (c.getExpiresAt().isBefore(Instant.now())) {
            c.setActive(false);
        }
    }
}
