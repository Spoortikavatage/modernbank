package com.modernbank.modernbank.controller;

import com.modernbank.modernbank.dto.AccountStatementResponse;
import com.modernbank.modernbank.exception.ApiErrorResponse;
import com.modernbank.modernbank.service.BankStatementService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;

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

@Operation(
    summary = "Get bank account statement",
    description = "Retrieves the bank statement for an account " +
                  "owned by the authenticated customer within " +
                  "the specified date range. A maximum of " +
                  "5,000 transactions is supported."
)
@ApiResponse(
    responseCode = "200",
    description = "Bank statement retrieved successfully"
)
@ApiResponse(
    responseCode = "400",
    description = "Invalid date range or request parameters",
    content = @Content(
        mediaType = "application/json",
        schema = @Schema(implementation = ApiErrorResponse.class)
    )
)
@ApiResponse(
    responseCode = "401",
    description = "Authentication required or JWT is invalid",
    content = @Content(
        mediaType = "application/json",
        schema = @Schema(implementation = ApiErrorResponse.class)
    )
)
@ApiResponse(
    responseCode = "403",
    description = "Customer is not permitted to access this account",
    content = @Content(
        mediaType = "application/json",
        schema = @Schema(implementation = ApiErrorResponse.class)
    )
)
@ApiResponse(
    responseCode = "404",
    description = "Bank account not found",
    content = @Content(
        mediaType = "application/json",
        schema = @Schema(implementation = ApiErrorResponse.class)
    )
)

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