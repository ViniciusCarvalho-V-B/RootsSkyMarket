package com.rootssky.market.gui;

import com.rootssky.market.engine.ShopManager.ShopCategory;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.jetbrains.annotations.NotNull;

public class ShopTransactionHolder implements InventoryHolder {
    private Inventory inventory;
    private final ShopCategory category;
    private final String itemId;
    private boolean hideBackButton = false;

    public ShopTransactionHolder(ShopCategory category, String itemId) {
        this.category = category;
        this.itemId = itemId;
    }

    public ShopTransactionHolder(ShopCategory category, String itemId, boolean hideBackButton) {
        this.category = category;
        this.itemId = itemId;
        this.hideBackButton = hideBackButton;
    }

    @Override
    public @NotNull Inventory getInventory() {
        return inventory;
    }

    public void setInventory(Inventory inventory) {
        this.inventory = inventory;
    }

    public ShopCategory getCategory() {
        return category;
    }

    public String getItemId() {
        return itemId;
    }

    public boolean isHideBackButton() {
        return hideBackButton;
    }

    public void setHideBackButton(boolean hideBackButton) {
        this.hideBackButton = hideBackButton;
    }
}
