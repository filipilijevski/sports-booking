package com.ttclub.backend.mapper;

import com.ttclub.backend.dto.OrderItemDto;
import com.ttclub.backend.model.OrderItem;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring",
        uses = {ProductMapper.class})
public interface OrderItemMapper {

    OrderItemDto toDto(OrderItem entity);
}
