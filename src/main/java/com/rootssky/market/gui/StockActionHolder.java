package com.rootssky.market.gui;

import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.jetbrains.annotations.NotNull;

public class StockActionHolder implements InventoryHolder {

    private Inventory inventory;
    private final String itemId;

    public StockActionHolder(String itemId) {
        this.itemId = itemId;
    }

    public void setInventory(Inventory inventory) {
        this.inventory = inventory;
    }

    public String getItemId() {
        return itemId;
    }

    @Override
    public @NotNull Inventory getInventory() {
        return inventory;
    }
}
