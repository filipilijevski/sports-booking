package com.ttclub.backend.mapper;

import com.ttclub.backend.dto.RefundDto;
import com.ttclub.backend.dto.RefundLineSummaryDto;
import com.ttclub.backend.model.RefundEvent;
import com.ttclub.backend.model.RefundLine;
import org.mapstruct.*;

import java.util.List;

@Mapper(componentModel = "spring")
public interface RefundMapper {

    /* RefundEvent -> RefundDto */
    @Mappings({
            @Mapping(source = "provider",        target = "provider"),
            @Mapping(source = "providerTxnId",   target = "providerTxnId"),
            @Mapping(source = "amount",          target = "amount"),
            @Mapping(source = "currency",        target = "currency"),
            @Mapping(source = "status",          target = "status"),
            @Mapping(source = "reason",          target = "reason"),
            @Mapping(source = "includesShipping", target = "includesShipping"),
            @Mapping(source = "shippingAmount",   target = "shippingAmount"),
            @Mapping(source = "createdAt",       target = "createdAt"),
            @Mapping(source = "lines",           target = "lines")
    })
    RefundDto toDto(RefundEvent entity);

    @IterableMapping(elementTargetType = RefundDto.class)
    List<RefundDto> toDtoList(List<RefundEvent> entities);

    /* RefundLine -> RefundLineSummaryDto */
    @Mapping(source = "orderItem.id", target = "orderItemId")
    RefundLineSummaryDto toDto(RefundLine line);

    @IterableMapping(elementTargetType = RefundLineSummaryDto.class)
    List<RefundLineSummaryDto> toLineDtoList(List<RefundLine> lines);
}
