package com.banking.transaction.service;

import com.banking.transaction.constant.ApiConstants;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.stereotype.Service;

import java.util.Objects;
import java.util.concurrent.CompletableFuture;

@Slf4j
@Service
@RequiredArgsConstructor
public class KafkaProducerService {

    private final KafkaTemplate<String, Object> kafkaTemplate;
    private final ObjectMapper objectMapper;

    public void publishTransactionEvent(String key, Object event) {
        publishEvent(ApiConstants.TOPIC_TRANSACTION_EVENTS, key, event);
    }

    public void publishTransactionStatus(String key, Object event) {
        publishEvent(ApiConstants.TOPIC_TRANSACTION_STATUS, key, event);
    }

    public void publishAccountEvent(String key, Object event) {
        publishEvent(ApiConstants.TOPIC_ACCOUNT_EVENTS, key, event);
    }

    private void publishEvent(String topic, String key, Object event) {
        try {
            String payload = objectMapper.writeValueAsString(event);
            log.info("Publishing event to topic [{}] with key [{}]: {}", topic, key, payload);

            CompletableFuture<SendResult<String, Object>> future = kafkaTemplate
                    .send(Objects.requireNonNull(topic, "topic"), Objects.requireNonNull(key, "key"), event);

            future.whenComplete((result, ex) -> {
                if (ex != null) {
                    log.error("Failed to publish event to topic [{}] with key [{}]: {}",
                            topic, key, ex.getMessage(), ex);
                } else {
                    log.info("Event published successfully to topic [{}], partition [{}], offset [{}]",
                            topic,
                            result.getRecordMetadata().partition(),
                            result.getRecordMetadata().offset());
                }
            });
        } catch (JsonProcessingException e) {
            log.error("Failed to serialize event for topic [{}]: {}", topic, e.getMessage(), e);
        }
    }
}
