package com.banking.transaction.service;

import com.banking.transaction.dto.AccountRequest;
import com.banking.transaction.dto.AccountResponse;
import com.banking.transaction.entity.Account;
import com.banking.transaction.exception.AccountNotFoundException;
import com.banking.transaction.repository.AccountRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class AccountService {

        private final AccountRepository accountRepository;
        private final CacheService cacheService;
        private final KafkaProducerService kafkaProducerService;

        public Mono<AccountResponse> createAccount(AccountRequest request) {
                String accountNumber = (request.getAccountNumber() != null && !request.getAccountNumber().isBlank())
                                ? request.getAccountNumber()
                                : generateAccountNumber();

                Account account = Account.builder()
                                .accountNumber(accountNumber)
                                .holderName(request.getHolderName())
                                .balance(request.getInitialBalance())
                                .currency(request.getCurrency())
                                .active(true)
                                .createdAt(LocalDateTime.now())
                                .updatedAt(LocalDateTime.now())
                                .build();

                return accountRepository.save(account)
                                .doOnSuccess(saved -> {
                                        log.info("Account created: {} for holder: {}", saved.getAccountNumber(),
                                                        saved.getHolderName());
                                        try {
                                                kafkaProducerService.publishAccountEvent(
                                                                saved.getId().toString(),
                                                                Map.of(
                                                                                "eventType", "ACCOUNT_CREATED",
                                                                                "accountId", saved.getId().toString(),
                                                                                "accountNumber",
                                                                                saved.getAccountNumber(),
                                                                                "holderName", saved.getHolderName()));
                                        } catch (Exception e) {
                                                log.warn("Failed to publish account event (non-critical): {}",
                                                                e.getMessage());
                                        }
                                })
                                .map(this::toResponse);
        }

        public Mono<AccountResponse> getAccountById(UUID id) {
                return accountRepository.findById(id)
                                .switchIfEmpty(Mono.error(
                                                new AccountNotFoundException("Account not found with ID: " + id)))
                                .map(this::toResponse);
        }

        public Mono<AccountResponse> getAccountByNumber(String accountNumber) {
                return accountRepository.findByAccountNumber(accountNumber)
                                .switchIfEmpty(Mono.error(
                                                new AccountNotFoundException("Account not found: " + accountNumber)))
                                .map(this::toResponse);
        }

        public Flux<AccountResponse> getAllAccounts() {
                return accountRepository.findAll()
                                .map(this::toResponse);
        }

        public Mono<BigDecimal> getBalance(UUID accountId) {
                return cacheService.getAccountBalance(accountId.toString())
                                .cast(BigDecimal.class)
                                .switchIfEmpty(
                                                accountRepository.findById(accountId)
                                                                .switchIfEmpty(Mono.error(new AccountNotFoundException(
                                                                                "Account not found with ID: "
                                                                                                + accountId)))
                                                                .flatMap(account -> cacheService
                                                                                .cacheAccountBalance(
                                                                                                accountId.toString(),
                                                                                                account.getBalance())
                                                                                .thenReturn(account.getBalance())));
        }

        public Mono<Account> debit(UUID accountId, BigDecimal amount) {
                return accountRepository.findById(accountId)
                                .switchIfEmpty(Mono
                                                .error(new AccountNotFoundException("Account not found: " + accountId)))
                                .flatMap(account -> {
                                        account.setBalance(account.getBalance().subtract(amount));
                                        account.setUpdatedAt(LocalDateTime.now());
                                        return accountRepository.save(account);
                                })
                                .doOnSuccess(account -> cacheService.evictAccountBalance(accountId.toString())
                                                .subscribe());
        }

        public Mono<Account> credit(UUID accountId, BigDecimal amount) {
                return accountRepository.findById(accountId)
                                .switchIfEmpty(Mono
                                                .error(new AccountNotFoundException("Account not found: " + accountId)))
                                .flatMap(account -> {
                                        account.setBalance(account.getBalance().add(amount));
                                        account.setUpdatedAt(LocalDateTime.now());
                                        return accountRepository.save(account);
                                })
                                .doOnSuccess(account -> cacheService.evictAccountBalance(accountId.toString())
                                                .subscribe());
        }

        private AccountResponse toResponse(Account account) {
                return AccountResponse.builder()
                                .id(account.getId())
                                .accountNumber(account.getAccountNumber())
                                .accountHolderName(account.getHolderName())
                                .balance(account.getBalance())
                                .currency(account.getCurrency())
                                .accountType("SAVINGS") // Placeholder or map from Entity if added
                                .createdAt(account.getCreatedAt())
                                .updatedAt(account.getUpdatedAt())
                                .build();
        }

        private String generateAccountNumber() {
                // Generate a bank-style account number: 10-digit numeric
                long number = (long) (Math.random() * 9_000_000_000L) + 1_000_000_000L;
                return String.valueOf(number);
        }
}
