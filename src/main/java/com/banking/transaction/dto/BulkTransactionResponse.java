package com.banking.transaction.dto;

import com.banking.transaction.constant.TransactionStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BulkTransactionResponse {
    private String batchId;
    private String transactionType;
    private TransactionStatus status;
    private Integer totalTransactions;
    private Integer successCount;
    private Integer failedCount;
    private BigDecimal totalAmount;
    private String currency;
    private List<BulkTransactionResult> results;
    private BigDecimal closingBalance;
    private Instant timestamp;
    private String message;
}
