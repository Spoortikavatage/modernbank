package com.modernbank.modernbank.controller;

import com.modernbank.modernbank.dto.DepositRequest;
import com.modernbank.modernbank.dto.TransactionResponse;
import com.modernbank.modernbank.dto.WithdrawalRequest;
import com.modernbank.modernbank.service.BankTransactionService;

import jakarta.validation.Valid;

//import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import com.modernbank.modernbank.exception.ApiErrorResponse;
@RestController
@RequestMapping("/api/v1/accounts")
public class BankTransactionController {

    private final BankTransactionService bankTransactionService;

    public BankTransactionController(
            BankTransactionService bankTransactionService) {
        this.bankTransactionService = bankTransactionService;
    }

@Operation(
    summary = "Deposit money into a bank account",
    description = "Deposits a specified amount into an eligible " +
                  "bank account owned by the authenticated customer."
)
@ApiResponse(
    responseCode = "200",
    description = "Deposit completed successfully"
)
@ApiResponse(
    responseCode = "400",
    description = "Invalid deposit amount or request",
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

@Operation(
    summary = "Withdraw money from a bank account",
    description = "Withdraws a specified amount from an eligible " +
                  "bank account owned by the authenticated customer. " +
                  "The account must have sufficient available balance."
)
@ApiResponse(
    responseCode = "200",
    description = "Withdrawal completed successfully"
)
@ApiResponse(
    responseCode = "400",
    description = "Invalid withdrawal amount or insufficient balance",
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

@Operation(
    summary = "Get bank account transaction history",
    description = "Retrieves paginated transaction history for a " +
                  "bank account owned by the authenticated customer. " +
                  "Supports page number and page size parameters."
)
@ApiResponse(
    responseCode = "200",
    description = "Transaction history retrieved successfully"
)
@ApiResponse(
    responseCode = "400",
    description = "Invalid pagination parameters",
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

@GetMapping("/{accountId}/transactions")
public Page<TransactionResponse> getTransactionHistory(
        @PathVariable Long accountId,
        @RequestParam(defaultValue = "0") int page,
        @RequestParam(defaultValue = "10") int size,
        @AuthenticationPrincipal Jwt jwt) {

    // Step 1: Validate pagination parameters
    if (page < 0 || size < 1 || size > 100) {
        throw new ResponseStatusException(
                HttpStatus.BAD_REQUEST,
                "Page must be >= 0 and size must be between 1 and 100"
        );
    }

    // Step 2: Identify the logged-in user
    Long authenticatedUserId =
            Long.valueOf(jwt.getSubject());

    // Step 3: Create the pagination request
    PageRequest pageable = PageRequest.of(page, size);

    // Step 4: Fetch paginated transaction history
    return bankTransactionService.getTransactionHistory(
            authenticatedUserId,
            accountId,
            pageable
    );
}
}