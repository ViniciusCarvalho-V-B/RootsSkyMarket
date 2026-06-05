package com.rootssky.market.model;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public class TransactionData {

    private final UUID playerUuid;
    private final String playerName;
    private final String itemId;
    private final TransactionType type;
    private final int amount;
    private final BigDecimal unitPrice;
    private final BigDecimal totalPrice;
    private final BigDecimal taxApplied;
    private final Instant timestamp;

    public TransactionData(UUID playerUuid, String playerName, String itemId, TransactionType type,
                           int amount, BigDecimal unitPrice, BigDecimal totalPrice,
                           BigDecimal taxApplied, Instant timestamp) {
        this.playerUuid = playerUuid;
        this.playerName = playerName;
        this.itemId = itemId;
        this.type = type;
        this.amount = amount;
        this.unitPrice = unitPrice;
        this.totalPrice = totalPrice;
        this.taxApplied = taxApplied;
        this.timestamp = timestamp;
    }

    public UUID getPlayerUuid() { return playerUuid; }
    public String getPlayerName() { return playerName; }
    public String getItemId() { return itemId; }
    public TransactionType getType() { return type; }
    public int getAmount() { return amount; }
    public BigDecimal getUnitPrice() { return unitPrice; }
    public BigDecimal getTotalPrice() { return totalPrice; }
    public BigDecimal getTaxApplied() { return taxApplied; }
    public Instant getTimestamp() { return timestamp; }

    @Override
    public String toString() {
        return String.format("Transaction{player=%s, item=%s, type=%s, amount=%d, total=%s}",
                playerUuid, itemId, type, amount, totalPrice);
    }
}