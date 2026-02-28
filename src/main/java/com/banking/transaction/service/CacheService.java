package com.banking.transaction.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.ReactiveRedisTemplate;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.time.Duration;

@Slf4j
@Service
@RequiredArgsConstructor
public class CacheService {

    private final ReactiveRedisTemplate<String, Object> reactiveRedisTemplate;

    private static final String ACCOUNT_BALANCE_PREFIX = "account:balance:";
    private static final String TRANSACTION_PREFIX = "transaction:";
    private static final Duration DEFAULT_TTL = Duration.ofMinutes(15);
    private static final Duration BALANCE_TTL = Duration.ofMinutes(5);

    @SuppressWarnings("null")
    public Mono<Void> cacheAccountBalance(String accountId, Object balance) {
        String key = ACCOUNT_BALANCE_PREFIX + accountId;
        log.debug("Caching account balance for key: {}", key);
        return reactiveRedisTemplate.opsForValue()
                .set(key, balance, BALANCE_TTL)
                .then();
    }

    public Mono<Object> getAccountBalance(String accountId) {
        String key = ACCOUNT_BALANCE_PREFIX + accountId;
        return reactiveRedisTemplate.opsForValue().get(key);
    }

    @SuppressWarnings("null")
    public Mono<Void> cacheTransaction(String transactionId, Object transaction) {
        String key = TRANSACTION_PREFIX + transactionId;
        log.debug("Caching transaction for key: {}", key);
        return reactiveRedisTemplate.opsForValue()
                .set(key, transaction, DEFAULT_TTL)
                .then();
    }

    public Mono<Object> getCachedTransaction(String transactionId) {
        String key = TRANSACTION_PREFIX + transactionId;
        return reactiveRedisTemplate.opsForValue().get(key);
    }

    public Mono<Boolean> evictAccountBalance(String accountId) {
        String key = ACCOUNT_BALANCE_PREFIX + accountId;
        log.debug("Evicting account balance cache for key: {}", key);
        return reactiveRedisTemplate.delete(key).map(count -> count > 0);
    }

    public Mono<Boolean> evictTransaction(String transactionId) {
        String key = TRANSACTION_PREFIX + transactionId;
        return reactiveRedisTemplate.delete(key).map(count -> count > 0);
    }
}
