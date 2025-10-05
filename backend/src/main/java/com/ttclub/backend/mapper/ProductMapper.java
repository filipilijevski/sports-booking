package com.ttclub.backend.mapper;

import com.ttclub.backend.dto.ProductDto;
import com.ttclub.backend.dto.UpsertProductDto;
import com.ttclub.backend.model.Category;
import com.ttclub.backend.model.Product;
import org.mapstruct.*;

import java.util.List;

@Mapper(componentModel = "spring",
        uses = {ProductImageMapper.class})
public interface ProductMapper {

    /* Read */
    @Mapping(source = "category.id",   target = "categoryId")   //  include id for admin filtering
    @Mapping(source = "category.name", target = "categoryName")
    ProductDto toDto(Product entity);

    @IterableMapping(elementTargetType = ProductDto.class)
    List<ProductDto> toDtoList(List<Product> entities);

    /* Write (admin create/update) */
    @Mapping(source = "categoryId", target = "category")
    Product toEntity(UpsertProductDto dto);

    /* MapStruct needs a helper to convert categoryId to Category */
    default Category map(Long id) {
        if (id == null) return null;
        Category c = new Category();
        c.setId(id);
        return c;
    }
}
