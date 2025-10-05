package com.ttclub.backend.mapper;

import com.ttclub.backend.dto.OrderDto;
import com.ttclub.backend.model.Order;
import com.ttclub.backend.model.RefundEvent;
import org.mapstruct.*;

import java.math.BigDecimal;
import java.util.List;

@Mapper(componentModel = "spring",
        uses = {OrderItemMapper.class, ShippingAddressMapper.class, RefundMapper.class})
public interface OrderMapper {

    @Mappings({
            @Mapping(source = "user.id",     target = "userId"),
            @Mapping(target = "shippingMethod",
                    expression = "java(entity.getShippingMethod().name())"),
            @Mapping(target = "couponCode",
                    expression = "java(entity.getCoupon() != null ? entity.getCoupon().getCode() : null)"),
            @Mapping(target = "origin",
                    expression = "java(entity.getStripePaymentIntentId() != null ? \"ONLINE\" : \"IN_PERSON\")"),
            @Mapping(target = "offlinePaymentMethod",
                    expression = "java(entity.getOfflinePaymentMethod() != null ? entity.getOfflinePaymentMethod().name() : null)"),

            /* refund summary fields */
            @Mapping(target = "refunds",                source = "refundEvents"),
            @Mapping(target = "refundedAmount",         expression = "java(sumRefunds(entity))"),
            @Mapping(target = "shippingRefundedAmount", expression = "java(sumShippingRefunds(entity))"),
            @Mapping(target = "fullyRefunded",          expression = "java(isFullyRefunded(entity))")
    })
    OrderDto toDto(Order entity);

    @IterableMapping(elementTargetType = OrderDto.class)
    List<OrderDto> toDtoList(List<Order> entities);

    /* helpers */
    default BigDecimal sumRefunds(Order o) {
        List<RefundEvent> evts = o.getRefundEvents();
        if (evts == null || evts.isEmpty()) return BigDecimal.ZERO;
        return evts.stream()
                .map(RefundEvent::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    default BigDecimal sumShippingRefunds(Order o) {
        List<RefundEvent> evts = o.getRefundEvents();
        if (evts == null || evts.isEmpty()) return BigDecimal.ZERO;
        return evts.stream()
                .map(RefundEvent::getShippingAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    default boolean isFullyRefunded(Order o) {
        return sumRefunds(o).compareTo(o.getTotalAmount()) >= 0;
    }
}
