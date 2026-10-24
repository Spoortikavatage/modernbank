package com.modernbank.modernbank.dto;

import com.modernbank.modernbank.entity.TransactionType;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

public record TransactionResponse(
        String referenceId,
        Long accountId,
        String accountNumber,
        TransactionType transactionType,
        BigDecimal amount,
        BigDecimal balanceAfter,
        String description,
        OffsetDateTime createdAt
) {
}