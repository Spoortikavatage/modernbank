
package com.modernbank.modernbank.repository;
import com.modernbank.modernbank.entity.BankTransfer;


import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface BankTransferRepository
        extends JpaRepository<BankTransfer, Long> {

    Optional<BankTransfer> findByFromAccount_IdAndIdempotencyKey(
            Long fromAccountId,
            String idempotencyKey
    );

}
