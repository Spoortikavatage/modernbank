
package com.modernbank.modernbank.controller;

import com.modernbank.modernbank.dto.TransferRequest;
import com.modernbank.modernbank.dto.TransferResponse;
import com.modernbank.modernbank.service.BankTransferService;

import jakarta.validation.Valid;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import com.modernbank.modernbank.exception.ApiErrorResponse;
@RestController
@RequestMapping("/api/v1/transfers")
public class BankTransferController {

    private final BankTransferService bankTransferService;

    public BankTransferController(
            BankTransferService bankTransferService) {
        this.bankTransferService = bankTransferService;
    }

@Operation(
    summary = "Transfer money between bank accounts",
    description = "Transfers money from a source bank account " +
                  "to a destination bank account. " +
                  "Uses database transactions and pessimistic locking " +
                  "to protect account balances during concurrent transfers. " +
                  "An idempotency key prevents duplicate processing " +
                  "when the same transfer request is retried."
)
@ApiResponse(
    responseCode = "200",
    description = "Money transferred successfully"
)
@ApiResponse(
    responseCode = "400",
    description = "Invalid transfer request or insufficient balance",
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
    description = "Customer is not permitted to transfer from the source account",
    content = @Content(
        mediaType = "application/json",
        schema = @Schema(implementation = ApiErrorResponse.class)
    )
)
@ApiResponse(
    responseCode = "404",
    description = "Source or destination account not found",
    content = @Content(
        mediaType = "application/json",
        schema = @Schema(implementation = ApiErrorResponse.class)
    )
)
@ApiResponse(
    responseCode = "409",
    description = "Transfer conflicts with an existing request or account state",
    content = @Content(
        mediaType = "application/json",
        schema = @Schema(implementation = ApiErrorResponse.class)
    )
)

    @PostMapping
    public ResponseEntity<TransferResponse> transferMoney(
            @Valid @RequestBody TransferRequest request,
            @AuthenticationPrincipal Jwt jwt) {

        Long authenticatedUserId =
                Long.valueOf(jwt.getSubject());

        TransferResponse response =
                bankTransferService.transfer(
                        authenticatedUserId,
                        request
                );

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(response);
    }
}
