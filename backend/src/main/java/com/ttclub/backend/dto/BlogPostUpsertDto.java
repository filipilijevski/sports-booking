package com.ttclub.backend.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** Payload used to create/update a blog post. */
public record BlogPostUpsertDto(
        @NotBlank @Size(max = 200) String title,
        @Size(max = 300) String subtitle,
        @NotBlank String bodyMarkdown,  // markdown content (bold, lists, etc.)
        Boolean visible,                // nullable - ignore on update
        Short sortOrder,                // nullable - ignore on update
        @Size(max = 255) String mainImageAlt // optional alt for primary image
) { }
