package com.rootssky.market.model;

import java.math.BigDecimal;
import java.time.Instant;

public class MarketItem {

    private final String itemId;
    private BigDecimal basePrice;
    private BigDecimal currentPrice;
    private BigDecimal alpha;
    private BigDecimal floorPrice;
    private BigDecimal ceilingPrice;
    private String category;
    private int volume24h;
    private Instant lastUpdate;

    public MarketItem(String itemId, BigDecimal basePrice, BigDecimal currentPrice,
                      BigDecimal alpha, String category, BigDecimal floorPrice, BigDecimal ceilingPrice,
                      int volume24h, Instant lastUpdate) {
        this.itemId = itemId;
        this.basePrice = basePrice;
        this.currentPrice = currentPrice;
        this.alpha = alpha;
        this.category = category;
        this.floorPrice = floorPrice;
        this.ceilingPrice = ceilingPrice;
        this.volume24h = volume24h;
        this.lastUpdate = lastUpdate;
    }

    public MarketItem(String itemId, BigDecimal basePrice, BigDecimal alpha, String category) {
        this.itemId = itemId;
        this.basePrice = basePrice;
        this.currentPrice = basePrice;
        this.alpha = alpha;
        this.category = category != null ? category : "UNCATEGORIZED";
        this.floorPrice = basePrice.multiply(BigDecimal.valueOf(0.2));
        this.ceilingPrice = basePrice.multiply(BigDecimal.valueOf(5.0));
        this.volume24h = 0;
        this.lastUpdate = Instant.now();
    }

    public MarketItem clone() {
        return new MarketItem(itemId, basePrice, currentPrice, alpha,
                category, floorPrice, ceilingPrice, volume24h, lastUpdate);
    }

    public String getItemId() { return itemId; }
    public BigDecimal getBasePrice() { return basePrice; }
    public void setBasePrice(BigDecimal basePrice) { this.basePrice = basePrice; }
    public BigDecimal getCurrentPrice() { return currentPrice; }
    public void setCurrentPrice(BigDecimal currentPrice) { this.currentPrice = currentPrice; }
    public BigDecimal getAlpha() { return alpha; }
    public void setAlpha(BigDecimal alpha) { this.alpha = alpha; }
    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }
    public BigDecimal getFloorPrice() { return floorPrice; }
    public void setFloorPrice(BigDecimal floorPrice) { this.floorPrice = floorPrice; }
    public BigDecimal getCeilingPrice() { return ceilingPrice; }
    public void setCeilingPrice(BigDecimal ceilingPrice) { this.ceilingPrice = ceilingPrice; }
    public int getVolume24h() { return volume24h; }
    public void setVolume24h(int volume24h) { this.volume24h = volume24h; }
    public Instant getLastUpdate() { return lastUpdate; }
    public void setLastUpdate(Instant lastUpdate) { this.lastUpdate = lastUpdate; }

    @Override
    public String toString() {
        return String.format("MarketItem{id='%s', category='%s', price=%s, volume=%d}",
                itemId, category, currentPrice, volume24h);
    }
}
