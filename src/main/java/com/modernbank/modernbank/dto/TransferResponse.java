
package com.modernbank.modernbank.dto;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

public record TransferResponse(
        String referenceId,
        Long fromAccountId,
        Long toAccountId,
        BigDecimal amount,
        String description,
        OffsetDateTime createdAt
) {
}
