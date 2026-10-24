
package com.modernbank.modernbank.service;

import com.modernbank.modernbank.dto.TransferRequest;
import com.modernbank.modernbank.dto.TransferResponse;
import com.modernbank.modernbank.entity.BankAccount;
import com.modernbank.modernbank.entity.BankTransaction;
import com.modernbank.modernbank.entity.BankTransfer;
import com.modernbank.modernbank.entity.TransactionType;
import com.modernbank.modernbank.repository.BankAccountRepository;
import com.modernbank.modernbank.repository.BankTransactionRepository;
import com.modernbank.modernbank.repository.BankTransferRepository;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.util.UUID;

@Service
public class BankTransferService {

    private final BankAccountRepository accountRepository;
    private final BankTransferRepository transferRepository;
    private final BankTransactionRepository transactionRepository;

    public BankTransferService(
            BankAccountRepository accountRepository,
            BankTransferRepository transferRepository,
            BankTransactionRepository transactionRepository) {

        this.accountRepository = accountRepository;
        this.transferRepository = transferRepository;
        this.transactionRepository = transactionRepository;
    }

    @Transactional
    public TransferResponse transfer(
            Long authenticatedUserId,
            TransferRequest request) {

        Long fromId = request.fromAccountId();
        Long toId = request.toAccountId();
        BigDecimal amount = request.amount();

        if (fromId == null || toId == null || amount == null
                || request.idempotencyKey() == null
                || request.idempotencyKey().isBlank()) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST, "Invalid transfer request");
        }

        if (amount.compareTo(BigDecimal.ZERO) <= 0
                || amount.scale() > 2
                || amount.precision() - amount.scale() > 17) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST, "Invalid transfer amount");
        }

        if (fromId.equals(toId)) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Cannot transfer to the same account");
        }

        // Lock accounts in a consistent order to reduce deadlocks.
        Long firstId = Math.min(fromId, toId);
        Long secondId = Math.max(fromId, toId);

        BankAccount first = accountRepository.findByIdForUpdate(firstId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Account not found"));

        BankAccount second = accountRepository.findByIdForUpdate(secondId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Account not found"));

        BankAccount sender = first.getId().equals(fromId) ? first : second;
        BankAccount receiver = first.getId().equals(toId) ? first : second;

        if (!sender.getUser().getId().equals(authenticatedUserId)) {
            throw new ResponseStatusException(
                    HttpStatus.NOT_FOUND, "Account not found");
        }

        // Check for a completed transfer using the same key.
        var existing = transferRepository
                .findByFromAccount_IdAndIdempotencyKey(
                        fromId, request.idempotencyKey());

        if (existing.isPresent()) {
            BankTransfer previous = existing.get();

            boolean sameRequest =
                    previous.getToAccount().getId().equals(toId)
                    && previous.getAmount().compareTo(amount) == 0
                    && java.util.Objects.equals(
                            previous.getDescription(),
                            request.description());

            if (!sameRequest) {
                throw new ResponseStatusException(
                        HttpStatus.CONFLICT,
                        "Idempotency key already used for a different transfer");
            }

            return toResponse(previous);
        }

        if (!"ACTIVE".equals(sender.getStatus())
                || !"ACTIVE".equals(receiver.getStatus())) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Both accounts must be active");
        }

        if (!sender.getCurrency().equals(receiver.getCurrency())) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Account currencies must match");
        }

        if (sender.getBalance().compareTo(amount) < 0) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Insufficient account balance");
        }

        sender.withdraw(amount);
        receiver.deposit(amount);

        BankTransfer transfer = transferRepository.save(
                new BankTransfer(
                        sender,
                        receiver,
                        amount,
                        request.description(),
                        request.idempotencyKey()));

        BankTransaction outgoing = new BankTransaction(
                UUID.randomUUID().toString(),
                sender,
                TransactionType.TRANSFER_OUT,
                amount,
                sender.getBalance(),
                request.description());

        outgoing.setTransfer(transfer);

        BankTransaction incoming = new BankTransaction(
                UUID.randomUUID().toString(),
                receiver,
                TransactionType.TRANSFER_IN,
                amount,
                receiver.getBalance(),
                request.description());

        incoming.setTransfer(transfer);

        transactionRepository.save(outgoing);
        transactionRepository.save(incoming);

        return toResponse(transfer);
    }

    private TransferResponse toResponse(BankTransfer transfer) {
        return new TransferResponse(
                transfer.getReferenceId(),
                transfer.getFromAccount().getId(),
                transfer.getToAccount().getId(),
                transfer.getAmount(),
                transfer.getDescription(),
                transfer.getCreatedAt());
    }
}
