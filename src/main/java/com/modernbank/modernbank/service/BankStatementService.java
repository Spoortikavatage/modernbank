package com.modernbank.modernbank.service;

import com.modernbank.modernbank.dto.AccountStatementResponse;
import com.modernbank.modernbank.dto.TransactionResponse;
import com.modernbank.modernbank.entity.BankAccount;
import com.modernbank.modernbank.entity.BankTransaction;
import com.modernbank.modernbank.entity.TransactionType;
import com.modernbank.modernbank.repository.BankAccountRepository;
import com.modernbank.modernbank.repository.BankTransactionRepository;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;

@Service
public class BankStatementService {

    private final BankAccountRepository bankAccountRepository;
    private final BankTransactionRepository bankTransactionRepository;

    public BankStatementService(
            BankAccountRepository bankAccountRepository,
            BankTransactionRepository bankTransactionRepository) {

        this.bankAccountRepository = bankAccountRepository;
        this.bankTransactionRepository = bankTransactionRepository;
    }

    @Transactional(readOnly = true)
    public AccountStatementResponse getStatement(
            Long authenticatedUserId,
            Long accountId,
            LocalDate fromDate,
            LocalDate toDate) {

        // 1. Validate dates
        if (fromDate == null || toDate == null
                || fromDate.isAfter(toDate)
                || toDate.isAfter(fromDate.plusMonths(3))) {

            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Invalid statement date range (maximum 3 months)"
            );
        }

        // 2. Verify account ownership
        BankAccount account = bankAccountRepository.findById(accountId)
                .filter(a -> a.getUser().getId().equals(authenticatedUserId))
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Account not found"
                ));

        // 3. Convert dates to UTC timestamps
        var from = fromDate.atStartOfDay().atOffset(ZoneOffset.UTC);
        var toExclusive = toDate.plusDays(1)
                .atStartOfDay()
                .atOffset(ZoneOffset.UTC);

        // 4. Find opening balance
        BigDecimal openingBalance = bankTransactionRepository
                .findFirstByBankAccount_IdAndCreatedAtLessThanOrderByCreatedAtDescIdDesc(
                        accountId, from)
                .map(BankTransaction::getBalanceAfter)
                .orElse(BigDecimal.ZERO);

        // 5. Fetch transactions within the date range
        // Check the transaction count before loading records
long transactionCount = bankTransactionRepository
        .countByBankAccount_IdAndCreatedAtGreaterThanEqualAndCreatedAtLessThan(
                accountId, from, toExclusive);

// Reject statements containing more than 5000 transactions
if (transactionCount > 5000) {
    throw new ResponseStatusException(
            HttpStatus.UNPROCESSABLE_CONTENT,
            "Statement contains too many transactions. Select a shorter date range."
    );
}

// Load transactions only after validating the count
List<BankTransaction> transactions = bankTransactionRepository
        .findAllByBankAccount_IdAndCreatedAtGreaterThanEqualAndCreatedAtLessThanOrderByCreatedAtAscIdAsc(
                accountId, from, toExclusive);

        // 6. Calculate total credits and debits
        BigDecimal totalCredits = BigDecimal.ZERO;
        BigDecimal totalDebits = BigDecimal.ZERO;

        for (BankTransaction transaction : transactions) {

            TransactionType type = transaction.getTransactionType();

            if (type == TransactionType.DEPOSIT
                    || type == TransactionType.TRANSFER_IN) {

                totalCredits = totalCredits.add(transaction.getAmount());

            } else if (type == TransactionType.WITHDRAWAL
                    || type == TransactionType.TRANSFER_OUT) {

                totalDebits = totalDebits.add(transaction.getAmount());
            }
        }

        // 7. Calculate closing balance
        BigDecimal closingBalance = openingBalance
                .add(totalCredits)
                .subtract(totalDebits);

        // 8. Convert entities into response DTOs
        List<TransactionResponse> transactionResponses = transactions.stream()
                .map(transaction -> new TransactionResponse(
                        transaction.getReferenceId(),
                        account.getId(),
                        account.getAccountNumber(),
                        transaction.getTransactionType(),
                        transaction.getAmount(),
                        transaction.getBalanceAfter(),
                        transaction.getDescription(),
                        transaction.getCreatedAt()
                ))
                .toList();

        // 9. Build the statement response
        return new AccountStatementResponse(
                account.getId(),
                account.getAccountNumber(),
                fromDate,
                toDate,
                openingBalance,
                totalCredits,
                totalDebits,
                closingBalance,
                transactionResponses
        );
    }
}
