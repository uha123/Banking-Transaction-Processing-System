package com.banking.transaction.entity;

import com.banking.transaction.constant.TransactionStatus;
import com.banking.transaction.constant.TransactionType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.annotation.Version;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table("transactions")
public class Transaction {

    @Id
    private UUID id;

    @Column("source_account_id")
    private UUID sourceAccountId;

    @Column("target_account_id")
    private UUID targetAccountId;

    private BigDecimal amount;
    private String currency;
    private TransactionType type;
    private TransactionStatus status;

    @Column("reference_number")
    private String referenceNumber;

    private String description;

    @CreatedDate
    @Column("created_at")
    private LocalDateTime createdAt;

    @LastModifiedDate
    @Column("updated_at")
    private LocalDateTime updatedAt;

    @Version
    private Long version;

    // ── New columns — unified transaction support ──────────────────────────────
    @Column("idempotency_key")
    private String idempotencyKey;

    private String channel;

    @Column("merchant_id")
    private String merchantId;

    @Column("merchant_name")
    private String merchantName;

    @Column("payment_method")
    private String paymentMethod;

    @Column("order_id")
    private String orderId;

    @Column("batch_id")
    private String batchId;

    @Column("closing_balance")
    private BigDecimal closingBalance;

    @Column("reversal_reason")
    private String reversalReason;

    @Column("initiated_by")
    private String initiatedBy;

    @Column("approved_by")
    private String approvedBy;

    @Column("original_transaction_id")
    private String originalTransactionId;
}
