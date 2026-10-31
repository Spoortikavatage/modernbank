package com.modernbank.modernbank.controller;

import com.modernbank.modernbank.dto.AccountStatementResponse;
import com.modernbank.modernbank.service.BankStatementService;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;

@RestController
@RequestMapping("/api/v1/accounts")
public class BankStatementController {

    private final BankStatementService bankStatementService;

    public BankStatementController(
            BankStatementService bankStatementService) {

        this.bankStatementService = bankStatementService;
    }

    @GetMapping("/{accountId}/statement")
    public AccountStatementResponse getStatement(
            @PathVariable Long accountId,

            @RequestParam("from")
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            LocalDate fromDate,

            @RequestParam("to")
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            LocalDate toDate,

            @AuthenticationPrincipal Jwt jwt) {

        Long authenticatedUserId = Long.valueOf(jwt.getSubject());

        return bankStatementService.getStatement(
                authenticatedUserId,
                accountId,
                fromDate,
                toDate
        );
    }
}