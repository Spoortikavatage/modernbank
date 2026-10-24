package com.modernbank.modernbank.dto;

public record LoginResponse(
        String accessToken,
        String tokenType,
        long expiresIn
) {}