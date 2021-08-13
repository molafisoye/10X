create table IF not exists transactions (
    transactionId BIGINT,
    amount DECIMAL(15,2),
    type VARCHAR(10),
    sourceId BIGINT,
    destinationId BIGINT,
    currency VARCHAR(5),
    createdAt TIMESTAMP
    );