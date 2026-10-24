package com.modernbank.modernbank.entity;

import jakarta.persistence.*;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;

@Entity
@Table(name = "bank_transactions")
public class BankTransaction {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "reference_id", nullable = false, unique = true, length = 36)
    private String referenceId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "bank_account_id", nullable = false)
    private BankAccount bankAccount;

    @Enumerated(EnumType.STRING)
    @Column(name = "transaction_type", nullable = false, length = 20)
    private TransactionType transactionType;

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal amount;

    @Column(name = "balance_after", nullable = false, precision = 19, scale = 2)
    private BigDecimal balanceAfter;

    @Column(length = 255)
    private String description;

    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt;
@ManyToOne(fetch = FetchType.LAZY)
@JoinColumn(name = "transfer_id")
private BankTransfer transfer;


    protected BankTransaction() {
        // Required by JPA
    }

    public BankTransaction(
            String referenceId,
            BankAccount bankAccount,
            TransactionType transactionType,
            BigDecimal amount,
            BigDecimal balanceAfter,
            String description) {

        this.referenceId = referenceId;
        this.bankAccount = bankAccount;
        this.transactionType = transactionType;
        this.amount = amount;
        this.balanceAfter = balanceAfter;
        this.description = description;
        this.createdAt = OffsetDateTime.now(ZoneOffset.UTC);
    }
public BankTransfer getTransfer() {
    return transfer;
}

public void setTransfer(BankTransfer transfer) {
    this.transfer = transfer;
}
    public Long getId() {
        return id;
    }

    public String getReferenceId() {
        return referenceId;
    }

    public BankAccount getBankAccount() {
        return bankAccount;
    }

    public TransactionType getTransactionType() {
        return transactionType;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public BigDecimal getBalanceAfter() {
        return balanceAfter;
    }

    public String getDescription() {
        return description;
    }

    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }
}