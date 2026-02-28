package com.banking.transaction.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
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
public class AccountRequest {
    @NotBlank(message = "Account number is required")
    private String accountNumber;

    @NotBlank(message = "Account holder name is required")
    private String holderName;

    @NotNull(message = "Initial balance is required")
    @DecimalMin(value = "0.00", message = "Initial balance cannot be negative")
    private BigDecimal initialBalance;

    @NotBlank(message = "Currency is required")
    private String currency;

    @NotBlank(message = "Account type is required")
    private String accountType;
}
