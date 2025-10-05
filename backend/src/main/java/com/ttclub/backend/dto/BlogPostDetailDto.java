package com.ttclub.backend.dto;

import java.time.Instant;
import java.util.List;

public record BlogPostDetailDto(
        Long id,
        String title,
        String subtitle,
        String bodyMarkdown,
        String mainImageUrl,
        String mainImageAlt,
        List<BlogImageDto> images,
        Instant createdAt,
        Instant updatedAt
) { }
