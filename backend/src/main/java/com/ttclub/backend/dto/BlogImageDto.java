package com.ttclub.backend.dto;

public record BlogImageDto(
        Long id,
        String url,
        String altText,
        short sortOrder
) { }
