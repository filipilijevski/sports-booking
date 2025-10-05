package com.ttclub.backend.dto;

public record LoginResponseDto(
        String accessToken,
        String refreshToken,
        String firstName
) { }
