package org.tenx.accounts;

import com.fasterxml.jackson.annotation.JsonIgnore;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class Transfer {
    private long id; //don't really need this as the transaction ID should be created at runtime
    private long sourceAccountId;
    private long destinationAccountId;
    private double amount;
    private String currency;
    private String createdAt;


    public Transfer(){
        final DateTimeFormatter dbTimeStampFormat = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
        createdAt = LocalDateTime.now().format(dbTimeStampFormat);
    }

    @JsonIgnore
    public long getId() { return id; }

    @JsonIgnore
    public void setId(long id) { this.id = id; }

    public long getSourceAccountId() { return sourceAccountId; }

    public void setSourceAccountId(long sourceAccountId) { this.sourceAccountId = sourceAccountId; }

    public long getDestinationAccountId() { return destinationAccountId; }

    public void setDestinationAccountId(long destinationAccountId) { this.destinationAccountId = destinationAccountId; }

    public double getAmount() { return amount; }

    public void setAmount(double amount) { this.amount = amount; }

    public String getCurrency() { return currency; }

    public void setCurrency(String currency) { this.currency = currency; }


    public String getCreatedAt() { return createdAt; }

    public void setCreatedAt(String createdAt) { this.createdAt = createdAt; }
}
