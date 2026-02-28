package com.banking.transaction.controller;

import com.banking.transaction.constant.ApiConstants;
import com.banking.transaction.dto.AccountRequest;
import com.banking.transaction.dto.AccountResponse;
import com.banking.transaction.dto.ApiResponse;
import com.banking.transaction.service.AccountService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.math.BigDecimal;
import java.util.UUID;

@Slf4j
@RestController
@RequestMapping(value = { ApiConstants.ACCOUNTS_BASE })
@RequiredArgsConstructor
public class AccountController {

    private final AccountService accountService;

    @PostMapping(ApiConstants.CREATE)
    public Mono<ResponseEntity<ApiResponse<AccountResponse>>> createAccount(
            @Valid @RequestBody AccountRequest request) {
        log.info("Received request to create account for holder: {}", request.getHolderName());
        return accountService.createAccount(request)
                .map(response -> ResponseEntity.status(HttpStatus.CREATED)
                        .body(ApiResponse.success(response, "Account created successfully",
                                HttpStatus.CREATED.value())));
    }

    @GetMapping(ApiConstants.ID)
    public Mono<ResponseEntity<ApiResponse<AccountResponse>>> getAccount(@PathVariable UUID id) {
        log.info("Received request to get account by ID: {}", id);
        return accountService.getAccountById(id)
                .map(response -> ResponseEntity.status(HttpStatus.OK)
                        .body(ApiResponse.success(response, "Account retrieved successfully", HttpStatus.OK.value())));
    }

    @GetMapping(ApiConstants.BY_NUMBER)
    public Mono<ResponseEntity<ApiResponse<AccountResponse>>> getAccountByNumber(
            @PathVariable String accountNumber) {
        log.info("Received request to get account by number: {}", accountNumber);
        return accountService.getAccountByNumber(accountNumber)
                .map(response -> ResponseEntity.status(HttpStatus.OK)
                        .body(ApiResponse.success(response, "Account retrieved successfully", HttpStatus.OK.value())));
    }

    @GetMapping
    public Mono<ResponseEntity<ApiResponse<Flux<AccountResponse>>>> getAllAccounts() {
        log.info("Received request to get all accounts");
        return Mono.just(ResponseEntity.status(HttpStatus.OK)
                .body(ApiResponse.success(accountService.getAllAccounts(), "Accounts retrieved successfully",
                        HttpStatus.OK.value())));
    }

    @GetMapping(ApiConstants.ID + ApiConstants.BALANCE)
    public Mono<ResponseEntity<ApiResponse<BigDecimal>>> getBalance(@PathVariable UUID id) {
        log.info("Received request to get balance for account ID: {}", id);
        return accountService.getBalance(id)
                .map(balance -> ResponseEntity.status(HttpStatus.OK)
                        .body(ApiResponse.success(balance, "Balance retrieved successfully", HttpStatus.OK.value())));
    }
}
