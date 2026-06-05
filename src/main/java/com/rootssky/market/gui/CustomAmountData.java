package com.rootssky.market.gui;

import com.rootssky.market.engine.ShopManager.ShopCategory;

public class CustomAmountData {
    private final ShopCategory category;
    private final String itemId;
    private final boolean isBuy;
    private final double unitPrice;

    public CustomAmountData(ShopCategory category, String itemId, boolean isBuy, double unitPrice) {
        this.category = category;
        this.itemId = itemId;
        this.isBuy = isBuy;
        this.unitPrice = unitPrice;
    }

    public ShopCategory getCategory() {
        return category;
    }

    public String getItemId() {
        return itemId;
    }

    public boolean isBuy() {
        return isBuy;
    }

    public double getUnitPrice() {
        return unitPrice;
    }
}
