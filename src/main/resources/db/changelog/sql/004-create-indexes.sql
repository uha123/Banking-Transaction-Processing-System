-- Accounts indexes
CREATE INDEX IF NOT EXISTS idx_accounts_account_number ON accounts(account_number);
CREATE INDEX IF NOT EXISTS idx_accounts_holder_name ON accounts(holder_name);

-- Transactions indexes
CREATE INDEX IF NOT EXISTS idx_transactions_source_account ON transactions(source_account_id);
CREATE INDEX IF NOT EXISTS idx_transactions_target_account ON transactions(target_account_id);
CREATE INDEX IF NOT EXISTS idx_transactions_status ON transactions(status);
CREATE INDEX IF NOT EXISTS idx_transactions_reference ON transactions(reference_number);
CREATE INDEX IF NOT EXISTS idx_transactions_created_at ON transactions(created_at);
CREATE INDEX IF NOT EXISTS idx_transactions_type ON transactions(type);

-- Transaction Events indexes
CREATE INDEX IF NOT EXISTS idx_transaction_events_txn_id ON transaction_events(transaction_id);
CREATE INDEX IF NOT EXISTS idx_transaction_events_type ON transaction_events(event_type);
CREATE INDEX IF NOT EXISTS idx_transaction_events_txn_version ON transaction_events(transaction_id, version);
