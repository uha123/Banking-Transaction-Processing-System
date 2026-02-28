package com.banking.transaction.controller;

import com.banking.transaction.constant.ApiConstants;
import com.banking.transaction.dto.ApiResponse;
import com.banking.transaction.dto.TransactionRequest;
import com.banking.transaction.dto.TransactionResponse;
import com.banking.transaction.service.TransactionService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

import java.util.List;

@Slf4j
@RestController
@RequestMapping(ApiConstants.BASE_URL)
@RequiredArgsConstructor
public class TransactionController {

        private final TransactionService transactionService;

        // ===================== PROCESS (unified) =====================
        @PostMapping(ApiConstants.PROCESS)
        public Mono<ResponseEntity<ApiResponse<TransactionResponse>>> process(
                        @Valid @RequestBody TransactionRequest request) {

                log.info("Process request received: type={}, idempotencyKey={}", request.getTransactionType(),
                                request.getIdempotencyKey());

                return transactionService.processTransaction(request)
                                .map(response -> ResponseEntity.status(HttpStatus.OK)
                                                .body(ApiResponse.success(
                                                                response,
                                                                "Transaction processed successfully",
                                                                HttpStatus.OK.value())));
        }

        // ===================== TRANSFER =====================
        @PostMapping(ApiConstants.TRANSFER)
        public Mono<ResponseEntity<ApiResponse<TransactionResponse>>> transfer(
                        @Valid @RequestBody TransactionRequest request) {

                log.info("Transfer request received for account: {}", request.getSourceAccountId());
                request.setTransactionType(ApiConstants.TYPE_TRANSFER);

                return transactionService.processTransaction(request)
                                .map(response -> ResponseEntity.status(HttpStatus.OK)
                                                .body(ApiResponse.success(
                                                                response,
                                                                ApiConstants.MSG_TRANSFER_SUCCESS,
                                                                HttpStatus.OK.value())));
        }

        // ===================== DEPOSIT =====================
        @PostMapping(ApiConstants.DEPOSIT)
        public Mono<ResponseEntity<ApiResponse<TransactionResponse>>> deposit(
                        @Valid @RequestBody TransactionRequest request) {

                log.info("Deposit request received for account: {}", request.getAccountId());
                request.setTransactionType(ApiConstants.TYPE_DEPOSIT);

                return transactionService.processTransaction(request)
                                .map(response -> ResponseEntity.status(HttpStatus.OK)
                                                .body(ApiResponse.success(
                                                                response,
                                                                ApiConstants.MSG_DEPOSIT_SUCCESS,
                                                                HttpStatus.OK.value())));
        }

        // ===================== WITHDRAW =====================
        @PostMapping(ApiConstants.WITHDRAW)
        public Mono<ResponseEntity<ApiResponse<TransactionResponse>>> withdraw(
                        @Valid @RequestBody TransactionRequest request) {

                log.info("Withdraw request received for account: {}", request.getAccountId());
                request.setTransactionType(ApiConstants.TYPE_WITHDRAW);

                return transactionService.processTransaction(request)
                                .map(response -> ResponseEntity.status(HttpStatus.OK)
                                                .body(ApiResponse.success(
                                                                response,
                                                                ApiConstants.MSG_WITHDRAW_SUCCESS,
                                                                HttpStatus.OK.value())));
        }

        // ===================== PAYMENT =====================
        @PostMapping(ApiConstants.PAYMENT)
        public Mono<ResponseEntity<ApiResponse<TransactionResponse>>> payment(
                        @Valid @RequestBody TransactionRequest request) {

                log.info("Payment request received for merchant: {}", request.getMerchantId());
                request.setTransactionType(ApiConstants.TYPE_PAYMENT);

                return transactionService.processTransaction(request)
                                .map(response -> ResponseEntity.status(HttpStatus.OK)
                                                .body(ApiResponse.success(
                                                                response,
                                                                ApiConstants.MSG_PAYMENT_SUCCESS,
                                                                HttpStatus.OK.value())));
        }

        // ===================== REFUND =====================
        @PostMapping(ApiConstants.REFUND)
        public Mono<ResponseEntity<ApiResponse<TransactionResponse>>> refund(
                        @Valid @RequestBody TransactionRequest request) {

                log.info("Refund request received for original transaction: {}", request.getOriginalTransactionId());
                request.setTransactionType(ApiConstants.TYPE_REFUND);

                return transactionService.processTransaction(request)
                                .map(response -> ResponseEntity.status(HttpStatus.OK)
                                                .body(ApiResponse.success(
                                                                response,
                                                                ApiConstants.MSG_REFUND_SUCCESS,
                                                                HttpStatus.OK.value())));
        }

        // ===================== REVERSAL =====================
        @PostMapping(ApiConstants.REVERSAL)
        public Mono<ResponseEntity<ApiResponse<TransactionResponse>>> reversal(
                        @Valid @RequestBody TransactionRequest request) {

                log.info("Reversal request received for original transaction: {}", request.getOriginalTransactionId());
                request.setTransactionType(ApiConstants.TYPE_REVERSAL);

                return transactionService.processTransaction(request)
                                .map(response -> ResponseEntity.status(HttpStatus.OK)
                                                .body(ApiResponse.success(
                                                                response,
                                                                ApiConstants.MSG_REVERSAL_SUCCESS,
                                                                HttpStatus.OK.value())));
        }

        // ===================== BULK =====================
        @PostMapping(ApiConstants.BULK)
        public Mono<ResponseEntity<ApiResponse<TransactionResponse>>> bulk(
                        @Valid @RequestBody TransactionRequest request) {

                log.info("Bulk request received, batch: {}", request.getBatchId());
                request.setTransactionType(ApiConstants.TYPE_BULK);

                return transactionService.processTransaction(request)
                                .map(response -> ResponseEntity.status(HttpStatus.OK)
                                                .body(ApiResponse.success(
                                                                response,
                                                                ApiConstants.MSG_BULK_SUCCESS,
                                                                HttpStatus.OK.value())));
        }

        // ===================== GET STATUS =====================
        @GetMapping(ApiConstants.STATUS)
        public Mono<ResponseEntity<ApiResponse<TransactionResponse>>> getStatus(
                        @PathVariable String transactionId) {

                log.info("Status request received for transaction: {}", transactionId);

                return transactionService.getTransactionStatus(transactionId)
                                .map(response -> ResponseEntity.status(HttpStatus.OK)
                                                .body(ApiResponse.success(
                                                                response,
                                                                ApiConstants.MSG_STATUS_FOUND,
                                                                HttpStatus.OK.value())));
        }

        // ===================== GET HISTORY =====================
        @GetMapping(ApiConstants.HISTORY)
        public Mono<ResponseEntity<ApiResponse<List<TransactionResponse>>>> getHistory(
                        @RequestParam String accountId) {

                log.info("History request received for account: {}", accountId);

                return transactionService.getTransactionHistory(accountId)
                                .collectList()
                                .map(history -> ResponseEntity.status(HttpStatus.OK)
                                                .body(ApiResponse.success(
                                                                history,
                                                                "Transaction history fetched successfully",
                                                                HttpStatus.OK.value())));
        }

        // ===================== UPDATE TRANSACTION =====================
        @PutMapping(ApiConstants.STATUS)
        public Mono<ResponseEntity<ApiResponse<TransactionResponse>>> updateTransaction(
                        @PathVariable String transactionId,
                        @Valid @RequestBody TransactionRequest request) {

                log.info("Update request received for transaction: {}", transactionId);

                return transactionService.updateTransaction(transactionId, request)
                                .map(response -> ResponseEntity.status(HttpStatus.OK)
                                                .body(ApiResponse.success(
                                                                response,
                                                                ApiConstants.MSG_UPDATE_SUCCESS,
                                                                HttpStatus.OK.value())));
        }
}
