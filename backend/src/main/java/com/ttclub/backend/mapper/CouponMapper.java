package com.ttclub.backend.mapper;

import com.ttclub.backend.dto.CouponDto;
import com.ttclub.backend.model.Coupon;
import org.mapstruct.*;

import java.util.List;

@Mapper(componentModel = "spring")
public interface CouponMapper {
    CouponDto toDto(Coupon entity);
    Coupon toEntity(CouponDto dto);

    @IterableMapping(elementTargetType = CouponDto.class)
    List<CouponDto> toDtoList(List<Coupon> entities);
}
