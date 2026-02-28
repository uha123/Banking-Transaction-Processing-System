package com.banking.transaction.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AccountResponse {
        private UUID id;
        private String accountNumber;
        private String accountHolderName;
        private BigDecimal balance;
        private String currency;
        private String accountType;
        private LocalDateTime createdAt;
        private LocalDateTime updatedAt;
}
