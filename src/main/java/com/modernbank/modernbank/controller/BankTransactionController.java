package com.modernbank.modernbank.controller;

import com.modernbank.modernbank.dto.DepositRequest;
import com.modernbank.modernbank.dto.TransactionResponse;
import com.modernbank.modernbank.dto.WithdrawalRequest;
import com.modernbank.modernbank.service.BankTransactionService;

import jakarta.validation.Valid;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/accounts")
public class BankTransactionController {

    private final BankTransactionService bankTransactionService;

    public BankTransactionController(
            BankTransactionService bankTransactionService) {
        this.bankTransactionService = bankTransactionService;
    }

    @PostMapping("/{accountId}/deposits")
    public ResponseEntity<TransactionResponse> deposit(
            @PathVariable Long accountId,
            @Valid @RequestBody DepositRequest request,
            @AuthenticationPrincipal Jwt jwt) {

        Long authenticatedUserId =
                Long.valueOf(jwt.getSubject());

        TransactionResponse response =
                bankTransactionService.deposit(
                        authenticatedUserId,
                        accountId,
                        request.amount(),
                        request.description()
                );

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(response);
    }
    @PostMapping("/{accountId}/withdrawals")
public ResponseEntity<TransactionResponse> withdraw(
        @PathVariable Long accountId,
        @Valid @RequestBody WithdrawalRequest request,
        @AuthenticationPrincipal Jwt jwt) {

    Long authenticatedUserId = Long.valueOf(jwt.getSubject());

    TransactionResponse response = bankTransactionService.withdraw(
            authenticatedUserId,
            accountId,
            request.amount(),
            request.description()
    );

    return ResponseEntity
            .status(HttpStatus.CREATED)
            .body(response);
}

@GetMapping("/{accountId}/transactions")
public List<TransactionResponse> getTransactionHistory(
        @PathVariable Long accountId,
        @AuthenticationPrincipal Jwt jwt) {

    Long authenticatedUserId =
            Long.valueOf(jwt.getSubject());

    return bankTransactionService.getTransactionHistory(
            authenticatedUserId,
            accountId
    );
}
}