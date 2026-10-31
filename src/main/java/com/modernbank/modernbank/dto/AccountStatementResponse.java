package com.modernbank.modernbank.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public record AccountStatementResponse(
        Long accountId,
        String accountNumber,
        LocalDate fromDate,
        LocalDate toDate,
        BigDecimal openingBalance,
        BigDecimal totalCredits,
        BigDecimal totalDebits,
        BigDecimal closingBalance,
        List<TransactionResponse> transactions
) {
}