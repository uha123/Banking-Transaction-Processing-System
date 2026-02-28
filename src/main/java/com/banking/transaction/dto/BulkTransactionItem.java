package com.banking.transaction.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BulkTransactionItem {
    @NotNull(message = "sequenceNumber is required")
    private Integer sequenceNumber;

    @NotNull(message = "destinationAccountId is required")
    private String destinationAccountId;

    @NotNull(message = "amount is required")
    @DecimalMin(value = "0.01", message = "Amount must be greater than zero")
    private BigDecimal amount;

    private String description;
}
