package com.banking.transaction.dto;

import com.banking.transaction.constant.TransactionStatus;
import com.banking.transaction.constant.TransactionType;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.Instant;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class TransactionResponse {
    private String transactionId;
    private TransactionType transactionType;
    private TransactionStatus status;
    private String sourceAccountId;
    private String destinationAccountId;
    private BigDecimal amount;
    private String currency;
    private BigDecimal closingBalance;
    private String channel;
    private Instant timestamp;
    private String message;

    // PAYMENT specific
    private String merchantId;
    private String merchantName;
    private String orderId;

    // REVERSAL specific
    private String originalTransactionId;
    private String reversalReason;
    private String initiatedBy;
    private String approvedBy;
}
