CREATE table IF NOT EXISTS accounts (
    id BIGINT,
    balance DECIMAL(15,2),
    currency VARCHAR(5),
    createdAt TIMESTAMP
    );