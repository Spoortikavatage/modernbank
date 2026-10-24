
package com.modernbank.modernbank.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

public record TransferRequest(

        @NotNull(message = "Sender account ID is required")
        Long fromAccountId,

        @NotNull(message = "Receiver account ID is required")
        Long toAccountId,

        @NotNull(message = "Amount is required")
        @DecimalMin(value = "0.01",
                message = "Amount must be greater than zero")
        @Digits(integer = 17, fraction = 2,
                message = "Amount must have at most 2 decimal places")
        BigDecimal amount,

        @Size(max = 255,
                message = "Description cannot exceed 255 characters")
        String description,

        @NotBlank(message = "Idempotency key is required")
        @Size(max = 100,
                message = "Idempotency key cannot exceed 100 characters")
        String idempotencyKey
) {
}
