package com.ttclub.backend.mapper;

import com.ttclub.backend.dto.ShippingAddressDto;
import com.ttclub.backend.model.ShippingAddress;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface ShippingAddressMapper {

    ShippingAddressDto toDto(ShippingAddress embeddable);
    ShippingAddress toEntity(ShippingAddressDto dto);
}
