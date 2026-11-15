package com.modernbank.modernbank.service;

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
//import java.util.List;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
@Service
public class BankTransactionService {

    private static final Logger log =
        LoggerFactory.getLogger(BankTransactionService.class);
    private final BankAccountRepository bankAccountRepository;
    private final BankTransactionRepository bankTransactionRepository;

    public BankTransactionService(
            BankAccountRepository bankAccountRepository,
            BankTransactionRepository bankTransactionRepository) {
        this.bankAccountRepository = bankAccountRepository;
        this.bankTransactionRepository = bankTransactionRepository;
    }

    @Transactional
public TransactionResponse deposit(
        Long authenticatedUserId,
        Long accountId,
        BigDecimal amount,
        String description) {

    // Log that a deposit request has been received.
    log.info("Deposit requested for accountId: {}", accountId);

    // Validate the deposit amount.
    if (amount == null
            || amount.compareTo(BigDecimal.ZERO) <= 0
            || amount.scale() > 2
            || amount.precision() - amount.scale() > 17) {

        throw new ResponseStatusException(
                HttpStatus.BAD_REQUEST,
                "Amount must be positive and have at most two decimal places"
        );
    }

    // Find and lock the account belonging to the authenticated user.
    BankAccount account = bankAccountRepository
            .findOwnedAccountForUpdate(accountId, authenticatedUserId)
            .orElseThrow(() -> new ResponseStatusException(
                    HttpStatus.NOT_FOUND,
                    "Account not found"
            ));

    // Deposits are allowed only on active accounts.
    if (!"ACTIVE".equals(account.getStatus())) {

        log.warn("Deposit rejected: account {} is not active", accountId);

        throw new ResponseStatusException(
                HttpStatus.CONFLICT,
                "Deposits are allowed only on active accounts"
        );
    }

    // Update the account balance.
    account.deposit(amount);

    // Record the deposit and the resulting balance.
    BankTransaction transaction = new BankTransaction(
            UUID.randomUUID().toString(),
            account,
            TransactionType.DEPOSIT,
            amount,
            account.getBalance(),
            description
    );

    // Save the transaction.
    BankTransaction saved =
            bankTransactionRepository.save(transaction);

    // Log successful deposit.
    log.info(
            "Deposit completed successfully. Transaction reference: {}",
            saved.getReferenceId()
    );

    // Return the transaction response.
    return new TransactionResponse(
            saved.getReferenceId(),
            account.getId(),
            account.getAccountNumber(),
            saved.getTransactionType(),
            saved.getAmount(),
            saved.getBalanceAfter(),
            saved.getDescription(),
            saved.getCreatedAt()
    );
}
    @Transactional
public TransactionResponse withdraw(
        Long authenticatedUserId,
        Long accountId,
        BigDecimal amount,
        String description) {

    // Log that a withdrawal request has been received.
    log.info("Withdrawal requested for accountId: {}", accountId);

    // Validate the withdrawal amount.
    if (amount == null
            || amount.compareTo(BigDecimal.ZERO) <= 0
            || amount.scale() > 2
            || amount.precision() - amount.scale() > 17) {

        throw new ResponseStatusException(
                HttpStatus.BAD_REQUEST,
                "Amount must be positive and have at most two decimal places"
        );
    }

    // Find and lock the account belonging to the authenticated user.
    BankAccount account = bankAccountRepository
            .findOwnedAccountForUpdate(accountId, authenticatedUserId)
            .orElseThrow(() -> new ResponseStatusException(
                    HttpStatus.NOT_FOUND,
                    "Account not found"
            ));

    // Withdrawals are allowed only on active accounts.
    if (!"ACTIVE".equals(account.getStatus())) {

        log.warn("Withdrawal rejected: account {} is not active", accountId);

        throw new ResponseStatusException(
                HttpStatus.CONFLICT,
                "Withdrawals are allowed only on active accounts"
        );
    }

    // Check whether sufficient balance is available.
    if (account.getBalance().compareTo(amount) < 0) {

        log.warn("Withdrawal rejected: insufficient balance for account {}",
                accountId);

        throw new ResponseStatusException(
                HttpStatus.CONFLICT,
                "Insufficient account balance"
        );
    }

    // Reduce the balance. The account row is locked until commit.
    account.withdraw(amount);

    // Save the corresponding withdrawal record.
    BankTransaction transaction = new BankTransaction(
            UUID.randomUUID().toString(),
            account,
            TransactionType.WITHDRAWAL,
            amount,
            account.getBalance(),
            description
    );

    BankTransaction saved =
            bankTransactionRepository.save(transaction);

    // Log successful withdrawal.
    log.info(
            "Withdrawal completed successfully. Transaction reference: {}",
            saved.getReferenceId()
    );

    // Return the transaction response.
    return new TransactionResponse(
            saved.getReferenceId(),
            account.getId(),
            account.getAccountNumber(),
            saved.getTransactionType(),
            saved.getAmount(),
            saved.getBalanceAfter(),
            saved.getDescription(),
            saved.getCreatedAt()
    );
}
@Transactional(readOnly = true)
public Page<TransactionResponse> getTransactionHistory(
        Long authenticatedUserId,
        Long accountId,
        Pageable pageable) {

    // Step 1: Verify account ownership
    BankAccount account = bankAccountRepository
            .findById(accountId)
            .filter(a -> a.getUser().getId()
                    .equals(authenticatedUserId))
            .orElseThrow(() -> new ResponseStatusException(
                    HttpStatus.NOT_FOUND,
                    "Account not found"
            ));

    // Step 2: Retrieve only the requested page
    Page<BankTransaction> transactions =
            bankTransactionRepository
                    .findAllByBankAccount_IdOrderByCreatedAtDescIdDesc(
                            account.getId(),
                            pageable
                    );

    // Step 3: Convert entities to response DTOs
    return transactions.map(transaction ->
            new TransactionResponse(
                    transaction.getReferenceId(),
                    account.getId(),
                    account.getAccountNumber(),
                    transaction.getTransactionType(),
                    transaction.getAmount(),
                    transaction.getBalanceAfter(),
                    transaction.getDescription(),
                    transaction.getCreatedAt()
            )
    );
}

}