
package com.modernbank.modernbank.entity;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;

import jakarta.persistence.*;

@Entity
@Table(name = "bank_accounts")
public class BankAccount {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "account_number",
            nullable = false, unique = true, length = 12)
    private String accountNumber;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Enumerated(EnumType.STRING)
    @Column(name = "account_type", nullable = false, length = 20)
    private AccountType accountType;

    @Column(nullable = false, length = 3)
    private String currency;

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal balance;

    @Column(nullable = false, length = 20)
    private String status;

    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt;

    protected BankAccount() {
    }

    public BankAccount(
            String accountNumber,
            User user,
            AccountType accountType) {

        this.accountNumber = accountNumber;
        this.user = user;
        this.accountType = accountType;
        this.currency = "INR";
        this.balance = BigDecimal.ZERO;
        this.status = "ACTIVE";
        this.createdAt = OffsetDateTime.now(ZoneOffset.UTC);
    }

    public Long getId() {
        return id;
    }

    public String getAccountNumber() {
        return accountNumber;
    }

    public User getUser() {
        return user;
    }

    public AccountType getAccountType() {
        return accountType;
    }

    public String getCurrency() {
        return currency;
    }

    public BigDecimal getBalance() {
        return balance;
    }

    public String getStatus() {
        return status;
    }

    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }
    public void deposit(BigDecimal amount) {
    this.balance = this.balance.add(amount);
}
public void withdraw(BigDecimal amount) {
    if (this.balance.compareTo(amount) < 0) {
        throw new IllegalArgumentException(
                "Insufficient account balance"
        );
    }

    this.balance = this.balance.subtract(amount);
}
}
