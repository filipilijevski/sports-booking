package com.ttclub.backend.mapper;

import com.ttclub.backend.dto.CartDto;
import com.ttclub.backend.model.Cart;
import com.ttclub.backend.model.CartItem;
import org.mapstruct.*;

import java.math.BigDecimal;
import java.util.List;

@Mapper(componentModel = "spring",
        uses = {CartItemMapper.class})
public interface CartMapper {

    /* compute subtotal and item count on the fly */
    @Mapping(target = "userId",    source = "user.id")
    @Mapping(target = "subtotal",  expression = "java(calcSubtotal(entity))")
    @Mapping(target = "totalItemCount", expression = "java(calcCount(entity))")
    CartDto toDto(Cart entity);

    /* helpers */
    default BigDecimal calcSubtotal(Cart cart) {
        return cart.getItems().stream()
                .map(i -> i.getUnitPrice().multiply(java.math.BigDecimal.valueOf(i.getQuantity())))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }
    default Integer calcCount(Cart cart) {
        return cart.getItems().stream()
                .mapToInt(CartItem::getQuantity)
                .sum();
    }
}
