package com.modernbank.modernbank.controller;

import com.modernbank.modernbank.dto.AccountResponse;
import com.modernbank.modernbank.dto.OpenAccountRequest;
import com.modernbank.modernbank.service.BankAccountService;

import jakarta.validation.Valid;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/accounts")
public class BankAccountController {

    private final BankAccountService bankAccountService;

    public BankAccountController(
            BankAccountService bankAccountService) {
        this.bankAccountService = bankAccountService;
    }

    @PostMapping
    public ResponseEntity<AccountResponse> openAccount(
            @Valid @RequestBody OpenAccountRequest request,
            @AuthenticationPrincipal Jwt jwt) {

        Long authenticatedUserId =
                Long.valueOf(jwt.getSubject());

        AccountResponse response =
                bankAccountService.openAccount(
                        authenticatedUserId,
                        request.accountType()
                );

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(response);
    }

    @GetMapping
    public List<AccountResponse> getMyAccounts(
            @AuthenticationPrincipal Jwt jwt) {

        Long authenticatedUserId =
                Long.valueOf(jwt.getSubject());

        return bankAccountService
                .getMyAccounts(authenticatedUserId);
    }
}