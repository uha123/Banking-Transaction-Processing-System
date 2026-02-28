package com.banking.transaction.repository;

import com.banking.transaction.entity.TransactionEvent;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;

import java.util.UUID;

@Repository
public interface TransactionEventRepository extends ReactiveCrudRepository<TransactionEvent, UUID> {

    Flux<TransactionEvent> findByTransactionIdOrderByVersionAsc(UUID transactionId);

    Flux<TransactionEvent> findByEventType(String eventType);
}
