package com.banking.transaction.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TransactionRequest {

    @NotNull(message = "idempotencyKey is required")
    private String idempotencyKey;

    private String transactionType;

    // ── TRANSFER / PAYMENT fields ───────────────────────────────────────────────
    private UUID sourceAccountId;
    private UUID destinationAccountId;

    @DecimalMin(value = "0.01", message = "Amount must be greater than zero")
    private BigDecimal amount;

    private String currency; // Defaults to USD in service if null
    private String description;
    private String channel; // MOBILE_APP | ONLINE | BRANCH | ATM

    // ── PAYMENT specific ────────────────────────────────────────────────────────
    private String merchantId;
    private String merchantName;
    private String paymentMethod; // DEBIT_CARD | CREDIT_CARD | NET_BANKING | UPI
    private String orderId;

    // ── BULK specific ───────────────────────────────────────────────────────────
    private String batchId;
    private String processingMode; // PARALLEL | SEQUENTIAL

    @Valid
    private List<BulkTransactionItem> transactions;

    // ── REVERSAL specific ───────────────────────────────────────────────────────
    private String originalTransactionId;
    private UUID accountId; // Account where credit will be reversed back to
    private String reversalReason;
    private String initiatedBy;
    private String approvedBy;
}
