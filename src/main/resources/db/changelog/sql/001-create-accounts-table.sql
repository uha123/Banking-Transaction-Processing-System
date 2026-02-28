-- Accounts table
CREATE TABLE IF NOT EXISTS accounts (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    account_number  VARCHAR(20) NOT NULL UNIQUE,
    holder_name     VARCHAR(255) NOT NULL,
    balance         DECIMAL(19, 4) NOT NULL DEFAULT 0.0000,
    currency        VARCHAR(3) NOT NULL DEFAULT 'USD',
    active          BOOLEAN NOT NULL DEFAULT TRUE,
    created_at      TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMP NOT NULL DEFAULT NOW(),
    version         BIGINT NOT NULL DEFAULT 0
);
