package com.modernbank.modernbank.dto;

import com.modernbank.modernbank.entity.AccountType;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

public record AccountResponse(
        Long id,
        String accountNumber,
        AccountType accountType,
        String currency,
        BigDecimal balance,
        String status,
        OffsetDateTime createdAt
) {
}