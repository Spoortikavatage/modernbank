
package com.modernbank.modernbank.dto;

import java.time.OffsetDateTime;

public record UserResponse(
        Long id,
        String firstName,
        String lastName,
        String email,
        OffsetDateTime createdAt
) {}
