package com.banking.transaction.service;

import com.banking.transaction.constant.ApiConstants;
import com.banking.transaction.constant.TransactionStatus;
import com.banking.transaction.repository.TransactionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.Objects;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class KafkaConsumerService {

    private final TransactionRepository transactionRepository;

    @KafkaListener(topics = ApiConstants.TOPIC_TRANSACTION_STATUS, groupId = "${spring.kafka.consumer.group-id}", containerFactory = "kafkaListenerContainerFactory")
    public void consumeTransactionStatus(ConsumerRecord<String, Object> record) {
        log.info("Received transaction status event: key={}, partition={}, offset={}",
                record.key(), record.partition(), record.offset());

        try {
            processTransactionStatusUpdate(record);
        } catch (Exception e) {
            log.error("Error processing transaction status event: {}", e.getMessage(), e);
        }
    }

    @KafkaListener(topics = ApiConstants.TOPIC_TRANSACTION_EVENTS, groupId = "${spring.kafka.consumer.group-id}", containerFactory = "kafkaListenerContainerFactory")
    public void consumeTransactionEvent(ConsumerRecord<String, Object> record) {
        log.info("Received transaction event: key={}, partition={}, offset={}",
                record.key(), record.partition(), record.offset());

        try {
            processTransactionEvent(record);
        } catch (Exception e) {
            log.error("Error processing transaction event: {}", e.getMessage(), e);
        }
    }

    private void processTransactionStatusUpdate(ConsumerRecord<String, Object> record) {
        if (record.value() instanceof Map<?, ?> payload) {
            String transactionId = (String) payload.get("transactionId");
            String status = (String) payload.get("status");

            if (transactionId != null && status != null) {
                final UUID txnUuid = Objects.requireNonNull(UUID.fromString(transactionId),
                        "parsed UUID must not be null");
                transactionRepository.findById(txnUuid)
                        .flatMap(transaction -> {
                            transaction.setStatus(TransactionStatus.valueOf(status));
                            return transactionRepository.save(transaction);
                        })
                        .subscribe(
                                updated -> log.info("Transaction {} status updated to {}",
                                        updated.getId(), updated.getStatus()),
                                error -> log.error("Failed to update transaction status: {}",
                                        error.getMessage()));
            }
        }
    }

    private void processTransactionEvent(ConsumerRecord<String, Object> record) {
        log.info("Processing transaction event for key: {}, value type: {}",
                record.key(),
                record.value() != null ? record.value().getClass().getSimpleName() : "null");
        // Event sourcing: events are stored by the producer side via
        // TransactionEventRepository.
        // This consumer handles cross-service event propagation, notifications,
        // analytics, etc.
    }
}
