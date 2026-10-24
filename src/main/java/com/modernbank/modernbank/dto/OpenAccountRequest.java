package com.modernbank.modernbank.dto;

import com.modernbank.modernbank.entity.AccountType;
import jakarta.validation.constraints.NotNull;

public record OpenAccountRequest(
        @NotNull(message = "Account type is required")
        AccountType accountType
) {
}