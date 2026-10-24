package com.modernbank.modernbank.service;

import com.modernbank.modernbank.dto.AccountResponse;
import com.modernbank.modernbank.entity.AccountType;
import com.modernbank.modernbank.entity.BankAccount;
import com.modernbank.modernbank.entity.User;
import com.modernbank.modernbank.repository.BankAccountRepository;
import com.modernbank.modernbank.repository.UserRepository;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.security.SecureRandom;
import java.util.List;

@Service
public class BankAccountService {

    private static final SecureRandom RANDOM = new SecureRandom();

    private final BankAccountRepository bankAccountRepository;
    private final UserRepository userRepository;

    public BankAccountService(
            BankAccountRepository bankAccountRepository,
            UserRepository userRepository) {
        this.bankAccountRepository = bankAccountRepository;
        this.userRepository = userRepository;
    }

    @Transactional
    public AccountResponse openAccount(
            Long authenticatedUserId,
            AccountType accountType) {

        User user = userRepository.findById(authenticatedUserId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.UNAUTHORIZED,
                        "Authenticated user not found"
                ));

        String accountNumber = generateUniqueAccountNumber();

        BankAccount account = new BankAccount(
                accountNumber,
                user,
                accountType
        );

        BankAccount savedAccount = bankAccountRepository.save(account);

        return toResponse(savedAccount);
    }

    @Transactional(readOnly = true)
    public List<AccountResponse> getMyAccounts(Long authenticatedUserId) {

        return bankAccountRepository
                .findAllByUser_IdOrderByCreatedAtDesc(authenticatedUserId)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    private String generateUniqueAccountNumber() {

        for (int attempt = 0; attempt < 10; attempt++) {

            long number = 100_000_000_000L
                    + RANDOM.nextLong(900_000_000_000L);

            String accountNumber = Long.toString(number);

            if (!bankAccountRepository.existsByAccountNumber(accountNumber)) {
                return accountNumber;
            }
        }

        throw new IllegalStateException(
                "Could not generate a unique account number"
        );
    }

    private AccountResponse toResponse(BankAccount account) {

        return new AccountResponse(
                account.getId(),
                account.getAccountNumber(),
                account.getAccountType(),
                account.getCurrency(),
                account.getBalance(),
                account.getStatus(),
                account.getCreatedAt()
        );
    }
}