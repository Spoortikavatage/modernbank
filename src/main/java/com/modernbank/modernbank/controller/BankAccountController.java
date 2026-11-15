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
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import com.modernbank.modernbank.exception.ApiErrorResponse;
@RestController
@RequestMapping("/api/v1/accounts")
public class BankAccountController {

    private final BankAccountService bankAccountService;

    public BankAccountController(
            BankAccountService bankAccountService) {
        this.bankAccountService = bankAccountService;
    }

@Operation(
    summary = "Open a new bank account",
    description = "Creates a bank account for the authenticated customer. " +
                  "The customer must provide a supported account type. " +
                  "The newly created account belongs to the authenticated user."
)
@ApiResponse(
    responseCode = "200",
    description = "Bank account created successfully"
)
@ApiResponse(
    responseCode = "400",
    description = "Invalid account creation request",
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

@Operation(
    summary = "Get customer bank accounts",
    description = "Retrieves all bank accounts belonging to " +
                  "the currently authenticated customer."
)
@ApiResponse(
    responseCode = "200",
    description = "Bank accounts retrieved successfully"
)
@ApiResponse(
    responseCode = "401",
    description = "Authentication required or JWT is invalid",
    content = @Content(
        mediaType = "application/json",
        schema = @Schema(implementation = ApiErrorResponse.class)
    )
)

    @GetMapping
    public List<AccountResponse> getMyAccounts(
            @AuthenticationPrincipal Jwt jwt) {

        Long authenticatedUserId =
                Long.valueOf(jwt.getSubject());

        return bankAccountService
                .getMyAccounts(authenticatedUserId);
    }
}