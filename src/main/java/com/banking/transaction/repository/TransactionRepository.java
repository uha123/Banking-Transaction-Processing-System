package com.banking.transaction.repository;

import com.banking.transaction.constant.TransactionStatus;
import com.banking.transaction.entity.Transaction;
import org.springframework.data.r2dbc.repository.Query;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;
import java.util.UUID;

@Repository
public interface TransactionRepository extends ReactiveCrudRepository<Transaction, UUID> {

    Flux<Transaction> findBySourceAccountId(UUID sourceAccountId);

    Flux<Transaction> findByTargetAccountId(UUID targetAccountId);

    Flux<Transaction> findByStatus(TransactionStatus status);

    Mono<Transaction> findByReferenceNumber(String referenceNumber);

    @Query("SELECT * FROM transactions WHERE source_account_id = :accountId OR target_account_id = :accountId ORDER BY created_at DESC")
    Flux<Transaction> findByAccountId(UUID accountId);

    @Query("SELECT * FROM transactions WHERE created_at BETWEEN :startDate AND :endDate ORDER BY created_at DESC")
    Flux<Transaction> findByDateRange(LocalDateTime startDate, LocalDateTime endDate);
}
