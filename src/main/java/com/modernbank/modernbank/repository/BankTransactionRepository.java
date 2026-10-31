package com.modernbank.modernbank.repository;

import com.modernbank.modernbank.entity.BankTransaction;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

public interface BankTransactionRepository
        extends JpaRepository<BankTransaction, Long> {

    Page<BankTransaction>
    findAllByBankAccount_IdOrderByCreatedAtDescIdDesc(
            Long accountId,
            Pageable pageable
    );
    List<BankTransaction>
findAllByBankAccount_IdAndCreatedAtGreaterThanEqualAndCreatedAtLessThanOrderByCreatedAtAscIdAsc(
        Long accountId,
        OffsetDateTime from,
        OffsetDateTime toExclusive
);
Optional<BankTransaction>
findFirstByBankAccount_IdAndCreatedAtLessThanOrderByCreatedAtDescIdDesc(
        Long accountId,
        OffsetDateTime from
);
long countByBankAccount_IdAndCreatedAtGreaterThanEqualAndCreatedAtLessThan(
        Long accountId,
        OffsetDateTime from,
        OffsetDateTime toExclusive
);
}