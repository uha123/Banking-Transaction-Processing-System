package com.banking.transaction.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table("transaction_events")
public class TransactionEvent {

    @Id
    private UUID id;

    @Column("transaction_id")
    private UUID transactionId;

    @Column("event_type")
    private String eventType;

    private String payload;

    @Builder.Default
    private Integer version = 1;

    @CreatedDate
    @Column("created_at")
    private LocalDateTime createdAt;
}
