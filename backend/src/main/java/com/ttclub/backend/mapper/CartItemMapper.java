package com.ttclub.backend.mapper;

import com.ttclub.backend.dto.CartItemDto;
import com.ttclub.backend.model.CartItem;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring",
        uses = {ProductMapper.class})
public interface CartItemMapper {

    @Mapping(target = "lineTotal", expression = "java(entity.getLineTotal())")
    CartItemDto toDto(CartItem entity);
}
