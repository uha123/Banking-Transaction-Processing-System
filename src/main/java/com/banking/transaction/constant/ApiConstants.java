package com.banking.transaction.constant;

public final class ApiConstants {

    private ApiConstants() {
    }

    // Base URL
    public static final String BASE_URL = "/api/v1/transactions";
    public static final String ACCOUNTS_BASE = "/api/v1/accounts";

    // Endpoint Mappings
    public static final String CREATE = "/create";
    public static final String PROCESS = "/process";
    public static final String TRANSFER = "/transfer";
    public static final String DEPOSIT = "/deposit";
    public static final String WITHDRAW = "/withdraw";
    public static final String PAYMENT = "/payment";
    public static final String REFUND = "/refund";
    public static final String REVERSAL = "/reversal";
    public static final String BULK = "/bulk";
    public static final String STATUS = "/{transactionId}/status";
    public static final String HISTORY = "/history";
    public static final String ID = "/{id}";
    public static final String BALANCE = "/balance";
    public static final String BY_NUMBER = "/number/{accountNumber}";

    // Transaction Types
    public static final String TYPE_TRANSFER = "TRANSFER";
    public static final String TYPE_DEPOSIT = "DEPOSIT";
    public static final String TYPE_WITHDRAW = "WITHDRAW";
    public static final String TYPE_PAYMENT = "PAYMENT";
    public static final String TYPE_REFUND = "REFUND";
    public static final String TYPE_REVERSAL = "REVERSAL";
    public static final String TYPE_BULK = "BULK";

    // Transaction Status
    public static final String STATUS_PENDING = "PENDING";
    public static final String STATUS_COMPLETED = "COMPLETED";
    public static final String STATUS_FAILED = "FAILED";
    public static final String STATUS_PROCESSING = "PROCESSING";
    public static final String STATUS_REVERSED = "REVERSED";

    // Response Messages
    public static final String MSG_TRANSFER_SUCCESS = "Transfer processed successfully";
    public static final String MSG_DEPOSIT_SUCCESS = "Deposit processed successfully";
    public static final String MSG_WITHDRAW_SUCCESS = "Withdrawal processed successfully";
    public static final String MSG_PAYMENT_SUCCESS = "Payment processed successfully";
    public static final String MSG_REFUND_SUCCESS = "Refund processed successfully";
    public static final String MSG_REVERSAL_SUCCESS = "Reversal processed successfully";
    public static final String MSG_BULK_SUCCESS = "Bulk transaction processed successfully";
    public static final String MSG_STATUS_FOUND = "Transaction status fetched successfully";
    public static final String MSG_UPDATE_SUCCESS = "Transaction updated successfully";

    // Kafka Topics
    public static final String TOPIC_INITIATED = "banking.transactions.initiated";
    public static final String TOPIC_COMPLETED = "banking.transactions.completed";
    public static final String TOPIC_FAILED = "banking.transactions.failed";
    public static final String TOPIC_REFUND = "banking.transactions.refunds";
    public static final String TOPIC_REVERSAL = "banking.transactions.reversals";
    public static final String TOPIC_BULK = "banking.transactions.bulk";
    public static final String TOPIC_DLQ = "banking.transactions.deadletter";
    public static final String TOPIC_TRANSACTION_EVENTS = "transaction-events";
    public static final String TOPIC_TRANSACTION_STATUS = "transaction-status";
    public static final String TOPIC_ACCOUNT_EVENTS = "account-events";

    // Channels
    public static final String CHANNEL_MOBILE = "MOBILE_APP";
    public static final String CHANNEL_ATM = "ATM";
    public static final String CHANNEL_BRANCH = "BRANCH";
    public static final String CHANNEL_ONLINE = "ONLINE";

    // Redis
    public static final String REDIS_KEY_PREFIX = "txn:cache:";
    public static final long REDIS_TTL = 3600L;
}
