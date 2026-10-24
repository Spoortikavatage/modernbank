package com.modernbank.modernbank.repository;

import com.modernbank.modernbank.entity.BankTransaction;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface BankTransactionRepository
        extends JpaRepository<BankTransaction, Long> {

    List<BankTransaction>
    findAllByBankAccount_IdOrderByCreatedAtDesc(Long accountId);
}