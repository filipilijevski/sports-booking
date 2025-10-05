package com.ttclub.backend.dto;

import java.time.Instant;
import java.util.List;

/** Admin-facing full DTO including visibility, sort order and deletion marker. */
public record AdminBlogPostDto(
        Long id,
        String title,
        String subtitle,
        String bodyMarkdown,
        String mainImageUrl,
        String mainImageAlt,
        List<BlogImageDto> images,
        short sortOrder,
        boolean visible,
        Instant createdAt,
        Instant updatedAt,
        Instant deletedAt
) { }
