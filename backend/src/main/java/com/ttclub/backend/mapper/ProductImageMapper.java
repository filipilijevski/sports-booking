package com.ttclub.backend.mapper;

import com.ttclub.backend.dto.ProductImageDto;
import com.ttclub.backend.model.ProductImage;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface ProductImageMapper {

    ProductImageDto toDto(ProductImage entity);
}
