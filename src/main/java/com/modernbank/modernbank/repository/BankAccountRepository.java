package com.modernbank.modernbank.repository;

import com.modernbank.modernbank.entity.BankAccount;

import jakarta.persistence.LockModeType;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface BankAccountRepository
        extends JpaRepository<BankAccount, Long> {

    boolean existsByAccountNumber(String accountNumber);

    List<BankAccount> findAllByUser_IdOrderByCreatedAtDesc(
            Long userId
    );

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
        SELECT a
        FROM BankAccount a
        WHERE a.id = :accountId
          AND a.user.id = :userId
        """)
    Optional<BankAccount> findOwnedAccountForUpdate(
            @Param("accountId") Long accountId,
            @Param("userId") Long userId
    );

@Lock(LockModeType.PESSIMISTIC_WRITE)
@Query("""
    SELECT a
    FROM BankAccount a
    WHERE a.id = :accountId
    """)
Optional<BankAccount> findByIdForUpdate(
        @Param("accountId") Long accountId
);

}