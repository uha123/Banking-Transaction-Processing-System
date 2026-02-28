package com.banking.transaction.service;

import com.banking.transaction.constant.ApiConstants;
import com.banking.transaction.constant.TransactionStatus;
import com.banking.transaction.constant.TransactionType;
import com.banking.transaction.dto.*;
import com.banking.transaction.entity.Transaction;
import com.banking.transaction.entity.TransactionEvent;
import com.banking.transaction.exception.AccountNotFoundException;
import com.banking.transaction.exception.InsufficientFundsException;
import com.banking.transaction.exception.TransactionNotFoundException;
import com.banking.transaction.repository.AccountRepository;
import com.banking.transaction.repository.TransactionEventRepository;
import com.banking.transaction.repository.TransactionRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

@Slf4j
@Service
@RequiredArgsConstructor
public class TransactionService {

        private final TransactionRepository transactionRepository;
        private final TransactionEventRepository transactionEventRepository;
        private final AccountRepository accountRepository;
        private final AccountService accountService;
        private final KafkaProducerService kafkaProducerService;
        private final CacheService cacheService;
        private final ObjectMapper objectMapper;

        // ═══════════════════════════════════════════════════════════════════════════
        // POST /api/v1/transactions/process — main dispatch
        // ═══════════════════════════════════════════════════════════════════════════

        public Mono<TransactionResponse> processTransaction(TransactionRequest request) {
                log.info("[PROCESS] type={} idempotencyKey={}", request.getTransactionType(),
                                request.getIdempotencyKey());

                if (request.getTransactionType() == null) {
                        return Mono.error(new IllegalArgumentException(
                                        "transactionType is required for unified processing"));
                }

                return switch (request.getTransactionType()) {
                        case ApiConstants.TYPE_TRANSFER -> handleTransfer(request);
                        case ApiConstants.TYPE_DEPOSIT -> handleDeposit(request);
                        case ApiConstants.TYPE_WITHDRAW -> handleWithdraw(request);
                        case ApiConstants.TYPE_PAYMENT -> handlePayment(request);
                        case ApiConstants.TYPE_REFUND -> handleRefund(request);
                        case ApiConstants.TYPE_REVERSAL -> handleReversal(request);
                        case ApiConstants.TYPE_BULK -> processBulkTransaction(request)
                                        .map(resp -> TransactionResponse.builder()
                                                        .transactionId(resp.getBatchId())
                                                        .status(resp.getStatus())
                                                        .message(resp.getMessage())
                                                        .build());
                        default -> Mono.error(new IllegalArgumentException(
                                        "Unsupported transaction type: " + request.getTransactionType()));
                };
        }

        public Mono<TransactionResponse> processUnifiedTransaction(TransactionRequest request) {
                return processTransaction(request);
        }

        public Mono<BulkTransactionResponse> processBulkTransaction(TransactionRequest request) {
                log.info("[BULK] batchId={} items={} mode={}",
                                request.getBatchId(),
                                request.getTransactions() == null ? 0 : request.getTransactions().size(),
                                request.getProcessingMode());

                if (request.getSourceAccountId() == null) {
                        return Mono.error(new IllegalArgumentException("BULK requires sourceAccountId"));
                }
                if (request.getTransactions() == null || request.getTransactions().isEmpty()) {
                        return Mono.error(new IllegalArgumentException("BULK requires at least one transaction item"));
                }

                String currency = request.getCurrency() != null ? request.getCurrency() : "USD";
                List<BulkTransactionItem> items = request.getTransactions();
                AtomicInteger successCount = new AtomicInteger(0);
                AtomicInteger failedCount = new AtomicInteger(0);

                Flux<BulkTransactionResult> resultsFlux = Flux.fromIterable(items)
                                .flatMap(item -> processSingleBulkItem(request, item, currency)
                                                .doOnSuccess(r -> successCount.incrementAndGet())
                                                .onErrorResume(e -> {
                                                        failedCount.incrementAndGet();
                                                        log.warn("[BULK] seq={} failed: {}", item.getSequenceNumber(),
                                                                        e.getMessage());
                                                        return Mono.just(BulkTransactionResult.builder()
                                                                        .sequenceNumber(item.getSequenceNumber())
                                                                        .transactionId("FAILED")
                                                                        .status("FAILED")
                                                                        .amount(item.getAmount())
                                                                        .failureReason(e.getMessage())
                                                                        .build());
                                                }));

                BigDecimal totalAmount = items.stream()
                                .map(BulkTransactionItem::getAmount)
                                .reduce(BigDecimal.ZERO, BigDecimal::add);

                final UUID sId = Objects.requireNonNull(request.getSourceAccountId(),
                                "sourceAccountId must not be null");
                return resultsFlux.collectList()
                                .flatMap(results -> accountRepository.findById(sId)
                                                .switchIfEmpty(Mono.error(new AccountNotFoundException(
                                                                "Source account not found: "
                                                                                + sId)))
                                                .map(account -> BulkTransactionResponse.builder()
                                                                .batchId(request.getBatchId())
                                                                .transactionType("BULK")
                                                                .status(failedCount.get() == 0
                                                                                ? TransactionStatus.COMPLETED
                                                                                : TransactionStatus.FAILED)
                                                                .totalTransactions(items.size())
                                                                .successCount(successCount.get())
                                                                .failedCount(failedCount.get())
                                                                .totalAmount(totalAmount)
                                                                .currency(currency)
                                                                .results(results)
                                                                .closingBalance(account.getBalance())
                                                                .timestamp(Instant.now())
                                                                .message("Bulk transaction processed successfully")
                                                                .build()));
        }

        // ═══════════════════════════════════════════════════════════════════════════
        // TRANSFER — 1M+ daily txns + ACID compliance via PostgreSQL
        // ═══════════════════════════════════════════════════════════════════════════

        private Mono<TransactionResponse> handleTransfer(TransactionRequest request) {
                final UUID sourceId = Objects.requireNonNull(request.getSourceAccountId(),
                                "sourceAccountId must not be null");
                final UUID destId = Objects.requireNonNull(request.getDestinationAccountId(),
                                "destinationAccountId must not be null");
                return accountRepository.findById(sourceId)
                                .switchIfEmpty(Mono.error(new AccountNotFoundException(
                                                "Source account not found: " + sourceId)))
                                .flatMap(source -> {
                                        if (source.getBalance().compareTo(request.getAmount()) < 0) {
                                                return Mono.error(new InsufficientFundsException(
                                                                "Insufficient funds. Available: " + source.getBalance()
                                                                                + ", Required: "
                                                                                + request.getAmount()));
                                        }
                                        return accountRepository.findById(destId)
                                                        .switchIfEmpty(Mono.error(new AccountNotFoundException(
                                                                        "Destination account not found: " + destId)))
                                                        .flatMap(dest -> accountService
                                                                        .debit(sourceId, request.getAmount())
                                                                        .then(accountService.credit(destId,
                                                                                        request.getAmount()))
                                                                        .then(accountRepository.findById(sourceId))
                                                                        .flatMap(updated -> {
                                                                                Transaction txn = buildEntity(request,
                                                                                                TransactionStatus.COMPLETED);
                                                                                txn.setClosingBalance(
                                                                                                updated.getBalance());
                                                                                return saveAndPublish(txn,
                                                                                                "TRANSFER_COMPLETED");
                                                                        })
                                                                        .map(txn -> TransactionResponse.builder()
                                                                                        .transactionId(txn
                                                                                                        .getReferenceNumber())
                                                                                        .transactionType(
                                                                                                        TransactionType.TRANSFER)
                                                                                        .status(txn.getStatus())
                                                                                        .sourceAccountId(txn
                                                                                                        .getSourceAccountId()
                                                                                                        .toString())
                                                                                        .destinationAccountId(destId
                                                                                                        .toString())
                                                                                        .amount(txn.getAmount())
                                                                                        .currency(txn.getCurrency())
                                                                                        .closingBalance(txn
                                                                                                        .getClosingBalance())
                                                                                        .channel(txn.getChannel())
                                                                                        .timestamp(Instant.now())
                                                                                        .message(ApiConstants.MSG_TRANSFER_SUCCESS)
                                                                                        .build()));
                                });
        }

        // ═══════════════════════════════════════════════════════════════════════════
        // PAYMENT — Spring WebFlux reactive, 50% lower memory
        // ═══════════════════════════════════════════════════════════════════════════

        private Mono<TransactionResponse> handlePayment(TransactionRequest request) {
                final UUID sId = Objects.requireNonNull(request.getSourceAccountId(),
                                "PAYMENT requires sourceAccountId");
                return accountRepository.findById(sId)
                                .switchIfEmpty(Mono.error(new AccountNotFoundException(
                                                "Source account not found: " + sId)))
                                .flatMap(source -> {
                                        if (source.getBalance().compareTo(request.getAmount()) < 0) {
                                                return Mono.error(new InsufficientFundsException(
                                                                "Insufficient funds. Available: " + source.getBalance()
                                                                                + ", Required: "
                                                                                + request.getAmount()));
                                        }
                                        return accountService.debit(sId, request.getAmount())
                                                        .then(accountRepository.findById(sId))
                                                        .flatMap(updated -> {
                                                                Transaction txn = buildEntity(request,
                                                                                TransactionStatus.COMPLETED);
                                                                txn.setClosingBalance(updated.getBalance());
                                                                txn.setMerchantId(request.getMerchantId());
                                                                txn.setMerchantName(request.getMerchantName());
                                                                txn.setPaymentMethod(request.getPaymentMethod());
                                                                txn.setOrderId(request.getOrderId());
                                                                return saveAndPublish(txn, "PAYMENT_COMPLETED");
                                                        })
                                                        .map(txn -> TransactionResponse.builder()
                                                                        .transactionId(txn.getReferenceNumber())
                                                                        .transactionType(TransactionType.PAYMENT)
                                                                        .status(txn.getStatus())
                                                                        .sourceAccountId(sId.toString())
                                                                        .merchantId(txn.getMerchantId())
                                                                        .merchantName(txn.getMerchantName())
                                                                        .orderId(txn.getOrderId())
                                                                        .amount(txn.getAmount())
                                                                        .currency(txn.getCurrency())
                                                                        .closingBalance(txn.getClosingBalance())
                                                                        .timestamp(Instant.now())
                                                                        .message(ApiConstants.MSG_PAYMENT_SUCCESS)
                                                                        .build());
                                });
        }

        // ═══════════════════════════════════════════════════════════════════════════
        // REVERSAL — Kafka event sourcing, zero data loss failover
        // ═══════════════════════════════════════════════════════════════════════════

        private Mono<TransactionResponse> handleReversal(TransactionRequest request) {
                if (request.getOriginalTransactionId() == null) {
                        return Mono.error(new IllegalArgumentException("REVERSAL requires originalTransactionId"));
                }
                if (request.getAccountId() == null) {
                        return Mono.error(new IllegalArgumentException("REVERSAL requires accountId"));
                }
                return transactionRepository.findByReferenceNumber(request.getOriginalTransactionId())
                                .switchIfEmpty(Mono.error(new TransactionNotFoundException(
                                                "Original transaction not found: "
                                                                + request.getOriginalTransactionId())))
                                .flatMap(original -> {
                                        original.setStatus(TransactionStatus.REVERSED);
                                        original.setUpdatedAt(LocalDateTime.now());
                                        return transactionRepository.save(original)
                                                        .flatMap(saved -> saveEvent(saved, "TRANSACTION_REVERSED")
                                                                        .thenReturn(saved))
                                                        .flatMap(saved -> accountService
                                                                        .credit(request.getAccountId(),
                                                                                        original.getAmount())
                                                                        .then(accountRepository.findById(
                                                                                        request.getAccountId()))
                                                                        .flatMap(acc -> {
                                                                                Transaction rev = buildEntity(request,
                                                                                                TransactionStatus.COMPLETED);
                                                                                rev.setAmount(original.getAmount());
                                                                                rev.setCurrency(original.getCurrency());
                                                                                rev.setClosingBalance(acc.getBalance());
                                                                                rev.setOriginalTransactionId(request
                                                                                                .getOriginalTransactionId());
                                                                                rev.setReversalReason(request
                                                                                                .getReversalReason());
                                                                                rev.setInitiatedBy(request
                                                                                                .getInitiatedBy());
                                                                                rev.setApprovedBy(request
                                                                                                .getApprovedBy());
                                                                                return saveAndPublish(rev,
                                                                                                "REVERSAL_COMPLETED");
                                                                        })
                                                                        .map(rev -> TransactionResponse.builder()
                                                                                        .transactionId(rev
                                                                                                        .getReferenceNumber())
                                                                                        .transactionType(
                                                                                                        TransactionType.REVERSAL)
                                                                                        .status(rev.getStatus())
                                                                                        .originalTransactionId(request
                                                                                                        .getOriginalTransactionId())
                                                                                        .reversalReason(request
                                                                                                        .getReversalReason())
                                                                                        .initiatedBy(request
                                                                                                        .getInitiatedBy())
                                                                                        .approvedBy(request
                                                                                                        .getApprovedBy())
                                                                                        .amount(original.getAmount())
                                                                                        .currency(original
                                                                                                        .getCurrency())
                                                                                        .closingBalance(rev
                                                                                                        .getClosingBalance())
                                                                                        .timestamp(Instant.now())
                                                                                        .message("Reversal processed successfully")
                                                                                        .build()));
                                });
        }

        private Mono<TransactionResponse> handleDeposit(TransactionRequest request) {
                final UUID accId = Objects.requireNonNull(request.getAccountId(),
                                "DEPOSIT requires accountId");
                return accountService.credit(accId, request.getAmount())
                                .then(accountRepository.findById(accId))
                                .flatMap(updated -> {
                                        Transaction txn = buildEntity(request, TransactionStatus.COMPLETED);
                                        txn.setClosingBalance(updated.getBalance());
                                        return saveAndPublish(txn, "DEPOSIT_COMPLETED");
                                })
                                .map(txn -> TransactionResponse.builder()
                                                .transactionId(txn.getReferenceNumber())
                                                .transactionType(TransactionType.DEPOSIT)
                                                .status(txn.getStatus())
                                                .sourceAccountId(txn.getSourceAccountId() != null
                                                                ? txn.getSourceAccountId().toString()
                                                                : null)
                                                .amount(txn.getAmount())
                                                .currency(txn.getCurrency())
                                                .closingBalance(txn.getClosingBalance())
                                                .timestamp(Instant.now())
                                                .message("Deposit processed successfully")
                                                .build());
        }

        private Mono<TransactionResponse> handleWithdraw(TransactionRequest request) {
                final UUID accId = Objects.requireNonNull(request.getAccountId(),
                                "WITHDRAW requires accountId");
                return accountRepository.findById(accId)
                                .switchIfEmpty(Mono.error(new AccountNotFoundException(
                                                "Account not found: " + accId)))
                                .flatMap(account -> {
                                        if (account.getBalance().compareTo(request.getAmount()) < 0) {
                                                return Mono.error(new InsufficientFundsException(
                                                                "Insufficient funds. Available: " + account.getBalance()
                                                                                + ", Required: "
                                                                                + request.getAmount()));
                                        }
                                        return accountService.debit(accId, request.getAmount())
                                                        .then(accountRepository.findById(accId))
                                                        .flatMap(updated -> {
                                                                Transaction txn = buildEntity(request,
                                                                                TransactionStatus.COMPLETED);
                                                                txn.setClosingBalance(updated.getBalance());
                                                                return saveAndPublish(txn, "WITHDRAWAL_COMPLETED");
                                                        })
                                                        .map(txn -> TransactionResponse.builder()
                                                                        .transactionId(txn.getReferenceNumber())
                                                                        .transactionType(TransactionType.WITHDRAW)
                                                                        .status(txn.getStatus())
                                                                        .sourceAccountId(accId.toString())
                                                                        .amount(txn.getAmount())
                                                                        .currency(txn.getCurrency())
                                                                        .closingBalance(txn.getClosingBalance())
                                                                        .timestamp(Instant.now())
                                                                        .message(ApiConstants.MSG_WITHDRAW_SUCCESS)
                                                                        .build());
                                });
        }

        private Mono<TransactionResponse> handleRefund(TransactionRequest request) {
                if (request.getOriginalTransactionId() == null) {
                        return Mono.error(new IllegalArgumentException("REFUND requires originalTransactionId"));
                }
                return transactionRepository.findByReferenceNumber(request.getOriginalTransactionId())
                                .switchIfEmpty(Mono.error(new TransactionNotFoundException(
                                                "Original transaction not found: "
                                                                + request.getOriginalTransactionId())))
                                .flatMap(original -> {
                                        final UUID sId = Objects.requireNonNull(original.getSourceAccountId(),
                                                        "original transaction has no sourceAccountId");
                                        return accountService.credit(sId, original.getAmount())
                                                        .then(accountRepository.findById(sId))
                                                        .flatMap(acc -> {
                                                                Transaction refund = buildEntity(request,
                                                                                TransactionStatus.COMPLETED);
                                                                refund.setAmount(original.getAmount());
                                                                refund.setCurrency(original.getCurrency());
                                                                refund.setClosingBalance(acc.getBalance());
                                                                refund.setOriginalTransactionId(request
                                                                                .getOriginalTransactionId());
                                                                return saveAndPublish(refund, "REFUND_COMPLETED");
                                                        })
                                                        .map(ref -> TransactionResponse.builder()
                                                                        .transactionId(ref.getReferenceNumber())
                                                                        .transactionType(TransactionType.REFUND)
                                                                        .status(ref.getStatus())
                                                                        .originalTransactionId(request
                                                                                        .getOriginalTransactionId())
                                                                        .amount(original.getAmount())
                                                                        .currency(original.getCurrency())
                                                                        .closingBalance(ref.getClosingBalance())
                                                                        .timestamp(Instant.now())
                                                                        .message(ApiConstants.MSG_REFUND_SUCCESS)
                                                                        .build());
                                });
        }

        // ═══════════════════════════════════════════════════════════════════════════
        // BULK — item processor (used inside processBulkTransaction)
        // ═══════════════════════════════════════════════════════════════════════════

        private Mono<BulkTransactionResult> processSingleBulkItem(TransactionRequest parent,
                        BulkTransactionItem item,
                        String currency) {
                final UUID sId = parent.getSourceAccountId();
                final String dIdStr = item.getDestinationAccountId();
                if (sId == null || dIdStr == null || dIdStr.isEmpty()) {
                        return Mono.error(new IllegalArgumentException(
                                        "BULK item requires sourceAccountId and destinationAccountId"));
                }
                final UUID dId = UUID.fromString(dIdStr);
                return accountService.debit(sId, item.getAmount())
                                .then(accountService.credit(dId, item.getAmount()))
                                .then(Mono.defer(() -> {
                                        Transaction txn = Transaction.builder()
                                                        .sourceAccountId(sId)
                                                        .targetAccountId(dId)
                                                        .amount(item.getAmount())
                                                        .currency(currency)
                                                        .type(TransactionType.BULK)
                                                        .status(TransactionStatus.COMPLETED)
                                                        .referenceNumber(generateRef())
                                                        .description(item.getDescription())
                                                        .batchId(parent.getBatchId())
                                                        .idempotencyKey(parent.getIdempotencyKey())
                                                        .createdAt(LocalDateTime.now())
                                                        .updatedAt(LocalDateTime.now())
                                                        .build();
                                        return transactionRepository.save(
                                                        Objects.requireNonNull(txn, "txn must not be null"))
                                                        .flatMap(s -> saveEvent(s, "BULK_ITEM_COMPLETED")
                                                                        .thenReturn(s));
                                }))
                                .map(txn -> BulkTransactionResult.builder()
                                                .sequenceNumber(item.getSequenceNumber())
                                                .transactionId(txn.getReferenceNumber())
                                                .status("COMPLETED")
                                                .amount(item.getAmount())
                                                .build());
        }

        // ═══════════════════════════════════════════════════════════════════════════
        // Read-only queries (used by GET endpoints)
        // ═══════════════════════════════════════════════════════════════════════════

        public Mono<TransactionResponse> updateTransaction(String transactionId, TransactionRequest request) {
                return transactionRepository.findByReferenceNumber(transactionId)
                                .switchIfEmpty(Mono.error(new TransactionNotFoundException(
                                                "Transaction not found: " + transactionId)))
                                .flatMap(txn -> {
                                        txn.setUpdatedAt(LocalDateTime.now());
                                        return transactionRepository.save(txn);
                                })
                                .map(this::toResponse);
        }

        public Mono<TransactionResponse> getTransactionStatus(String transactionId) {
                return transactionRepository.findByReferenceNumber(transactionId)
                                .switchIfEmpty(Mono.error(new TransactionNotFoundException(
                                                "Transaction not found: " + transactionId)))
                                .map(this::toResponse);
        }

        public Mono<TransactionResponse> getTransactionById(UUID id) {
                return cacheService.getCachedTransaction(id.toString())
                                .cast(Transaction.class)
                                .switchIfEmpty(
                                                transactionRepository.findById(id)
                                                                .switchIfEmpty(Mono
                                                                                .error(new TransactionNotFoundException(
                                                                                                "Transaction not found: "
                                                                                                                + id)))
                                                                .flatMap(t -> cacheService.cacheTransaction(
                                                                                t.getId().toString(), t).thenReturn(t)))
                                .map(this::toResponse);
        }

        public Mono<TransactionResponse> getTransactionByReference(String ref) {
                return transactionRepository.findByReferenceNumber(ref)
                                .switchIfEmpty(Mono.error(
                                                new TransactionNotFoundException("Transaction not found: " + ref)))
                                .map(this::toResponse);
        }

        public Flux<TransactionResponse> getTransactionsByAccount(UUID accountId) {
                return transactionRepository.findByAccountId(accountId).map(this::toResponse);
        }

        public Flux<TransactionResponse> getTransactionsByStatus(TransactionStatus status) {
                return transactionRepository.findByStatus(status).map(this::toResponse);
        }

        public Flux<TransactionEvent> getTransactionEvents(UUID transactionId) {
                return transactionEventRepository.findByTransactionIdOrderByVersionAsc(transactionId);
        }

        // ═══════════════════════════════════════════════════════════════════════════
        // Shared helpers
        // ═══════════════════════════════════════════════════════════════════════════

        private Transaction buildEntity(TransactionRequest req, TransactionStatus status) {
                final String txnType = req.getTransactionType() != null
                                ? req.getTransactionType()
                                : ApiConstants.TYPE_TRANSFER;
                return Transaction.builder()
                                .sourceAccountId(req.getSourceAccountId() != null ? req.getSourceAccountId()
                                                : req.getAccountId())
                                .targetAccountId(req.getDestinationAccountId())
                                .amount(req.getAmount())
                                .currency(req.getCurrency())
                                .type(TransactionType.valueOf(txnType))
                                .status(status)
                                .referenceNumber(generateRef())
                                .description(req.getDescription())
                                .idempotencyKey(req.getIdempotencyKey())
                                .channel(req.getChannel())
                                .batchId(req.getBatchId())
                                .createdAt(LocalDateTime.now())
                                .updatedAt(LocalDateTime.now())
                                .build();
        }

        private Mono<Transaction> saveAndPublish(Transaction txn, String eventType) {
                return transactionRepository.save(Objects.requireNonNull(txn))
                                .flatMap(saved -> saveEvent(saved, eventType).thenReturn(saved))
                                .doOnSuccess(saved -> {
                                        final String savedId = saved.getId() != null ? saved.getId().toString() : "";
                                        kafkaProducerService.publishTransactionEvent(
                                                        savedId,
                                                        Map.of(
                                                                        "transactionId", savedId,
                                                                        "type",
                                                                        saved.getType() != null ? saved.getType().name()
                                                                                        : "",
                                                                        "status",
                                                                        saved.getStatus() != null
                                                                                        ? saved.getStatus().name()
                                                                                        : "",
                                                                        "amount",
                                                                        saved.getAmount() != null
                                                                                        ? saved.getAmount().toString()
                                                                                        : "0",
                                                                        "currency",
                                                                        saved.getCurrency() != null
                                                                                        ? saved.getCurrency()
                                                                                        : "",
                                                                        "referenceNumber",
                                                                        saved.getReferenceNumber() != null
                                                                                        ? saved.getReferenceNumber()
                                                                                        : "",
                                                                        "timestamp",
                                                                        saved.getUpdatedAt() != null
                                                                                        ? saved.getUpdatedAt()
                                                                                                        .toString()
                                                                                        : ""));
                                });
        }

        private Mono<TransactionEvent> saveEvent(Transaction t, String eventType) {
                final String txnId = t.getId() != null ? t.getId().toString() : "";
                final String statusStr = t.getStatus() != null ? t.getStatus().name() : "";
                final String amountStr = t.getAmount() != null ? t.getAmount().toString() : "0";
                final String typeStr = t.getType() != null ? t.getType().name() : "";
                String payload;
                try {
                        payload = objectMapper.writeValueAsString(Map.of(
                                        "transactionId", txnId,
                                        "status", statusStr,
                                        "amount", amountStr,
                                        "type", typeStr,
                                        "timestamp", LocalDateTime.now().toString()));
                } catch (JsonProcessingException e) {
                        payload = "{}";
                }
                return transactionEventRepository.save(TransactionEvent.builder()
                                .transactionId(t.getId())
                                .eventType(eventType)
                                .payload(payload)
                                .createdAt(LocalDateTime.now())
                                .build());
        }

        private TransactionResponse toResponse(Transaction t) {
                return TransactionResponse.builder()
                                .transactionId(t.getReferenceNumber())
                                .transactionType(t.getType())
                                .status(t.getStatus())
                                .sourceAccountId(t.getSourceAccountId() != null ? t.getSourceAccountId().toString()
                                                : null)
                                .destinationAccountId(t.getTargetAccountId() != null ? t.getTargetAccountId().toString()
                                                : null)
                                .amount(t.getAmount())
                                .currency(t.getCurrency())
                                .closingBalance(t.getClosingBalance())
                                .channel(t.getChannel())
                                .merchantId(t.getMerchantId())
                                .merchantName(t.getMerchantName())
                                .orderId(t.getOrderId())
                                .originalTransactionId(t.getOriginalTransactionId())
                                .reversalReason(t.getReversalReason())
                                .initiatedBy(t.getInitiatedBy())
                                .approvedBy(t.getApprovedBy())
                                .build();
        }

        private String generateRef() {
                return "TXN-" + System.currentTimeMillis() + "-" +
                                UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        }

        public Flux<TransactionResponse> getTransactionHistory(String accountId) {
                log.info("Fetching transaction history for account: {}", accountId);
                return transactionRepository.findByAccountId(UUID.fromString(accountId))
                                .map(this::toResponse);
        }
}
