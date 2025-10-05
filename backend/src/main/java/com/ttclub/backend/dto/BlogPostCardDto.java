package com.ttclub.backend.dto;

import java.time.Instant;

public record BlogPostCardDto(
        Long id,
        String title,
        String subtitle,
        String mainImageUrl,
        Instant createdAt,
        String excerpt   // first paragraph extracted from markdown
) { }
