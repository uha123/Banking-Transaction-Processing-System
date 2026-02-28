package com.banking.transaction.exception;

import com.banking.transaction.dto.ApiResponse;
import com.banking.transaction.exception.AccountNotFoundException;
import com.banking.transaction.exception.DuplicateTransactionException;
import com.banking.transaction.exception.InsufficientFundsException;
import com.banking.transaction.exception.TransactionNotFoundException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.bind.support.WebExchangeBindException;
import reactor.core.publisher.Mono;

import java.util.stream.Collectors;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(AccountNotFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public Mono<ApiResponse<Void>> handleAccountNotFound(AccountNotFoundException ex) {
        log.error("Account not found: {}", ex.getMessage());
        return Mono.just(ApiResponse.error(ex.getMessage(), "ACCOUNT_NOT_FOUND"));
    }

    @ExceptionHandler(TransactionNotFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public Mono<ApiResponse<Void>> handleTransactionNotFound(TransactionNotFoundException ex) {
        log.error("Transaction not found: {}", ex.getMessage());
        return Mono.just(ApiResponse.error(ex.getMessage(), "TRANSACTION_NOT_FOUND"));
    }

    @ExceptionHandler(InsufficientFundsException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public Mono<ApiResponse<Void>> handleInsufficientFunds(InsufficientFundsException ex) {
        log.error("Insufficient funds: {}", ex.getMessage());
        return Mono.just(ApiResponse.error(ex.getMessage(), "INSUFFICIENT_FUNDS"));
    }

    @ExceptionHandler(DuplicateTransactionException.class)
    @ResponseStatus(HttpStatus.CONFLICT)
    public Mono<ApiResponse<Void>> handleDuplicateTransaction(DuplicateTransactionException ex) {
        log.error("Duplicate transaction: {}", ex.getMessage());
        return Mono.just(ApiResponse.error(ex.getMessage(), "DUPLICATE_TRANSACTION"));
    }

    @ExceptionHandler(WebExchangeBindException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public Mono<ApiResponse<Void>> handleValidationExceptions(WebExchangeBindException ex) {
        String errors = ex.getBindingResult().getFieldErrors().stream()
                .map(error -> error.getField() + ": " + error.getDefaultMessage())
                .collect(Collectors.joining(", "));
        log.error("Validation error: {}", errors);
        return Mono.just(ApiResponse.error("Validation failed: " + errors, "VALIDATION_ERROR"));
    }

    @ExceptionHandler(Exception.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public Mono<ApiResponse<Void>> handleGeneralException(Exception ex) {
        log.error("Internal server error", ex);
        return Mono.just(ApiResponse.error("An unexpected error occurred", "INTERNAL_SERVER_ERROR"));
    }
}
