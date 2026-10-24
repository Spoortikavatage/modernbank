
package com.modernbank.modernbank.repository;

import com.modernbank.modernbank.entity.BankAccount;
import com.modernbank.modernbank.entity.BankTransfer;

import jakarta.persistence.LockModeType;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface BankTransferRepository
        extends JpaRepository<BankTransfer, Long> {

    Optional<BankTransfer> findByFromAccount_IdAndIdempotencyKey(
            Long fromAccountId,
            String idempotencyKey
    );


@Lock(LockModeType.PESSIMISTIC_WRITE)
@Query("SELECT a FROM BankAccount a WHERE a.id = :accountId")
Optional<BankAccount> findByIdForUpdate(
        @Param("accountId") Long accountId
);

}
