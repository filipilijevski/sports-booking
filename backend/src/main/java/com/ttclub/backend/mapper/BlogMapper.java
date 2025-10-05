package com.ttclub.backend.mapper;

import com.ttclub.backend.dto.*;
import com.ttclub.backend.model.*;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.Comparator;
import java.util.List;

@Mapper(componentModel = "spring")
public interface BlogMapper {

    /* entity to DTO public */

    @Mapping(target = "excerpt", ignore = true)  // set in service (first paragraph)
    BlogPostCardDto toCardDto(BlogPost p);

    @Mapping(target = "images", expression = "java(sortImages(p.getImages()))")
    BlogPostDetailDto toDetailDto(BlogPost p);

    /* entity to dto admin */
    @Mapping(target = "images", expression = "java(sortImages(p.getImages()))")
    AdminBlogPostDto toAdminDto(BlogPost p);

    /* image */
    default List<BlogImageDto> sortImages(List<BlogImage> imgs) {
        return imgs.stream()
                .sorted(Comparator.<BlogImage>comparingInt(i -> i.getSortOrder())
                        .thenComparingLong(i -> i.getId() == null ? Long.MAX_VALUE : i.getId()))
                .map(i -> new BlogImageDto(i.getId(), i.getUrl(), i.getAltText(), i.getSortOrder()))
                .toList();
    }
}
