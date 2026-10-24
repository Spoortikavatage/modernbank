
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

@RestController
@RequestMapping("/api/v1/transfers")
public class BankTransferController {

    private final BankTransferService bankTransferService;

    public BankTransferController(
            BankTransferService bankTransferService) {
        this.bankTransferService = bankTransferService;
    }

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
