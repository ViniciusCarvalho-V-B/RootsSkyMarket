package com.rootssky.market.gui;

import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.jetbrains.annotations.NotNull;

public class StockHolder implements InventoryHolder {

    private Inventory inventory;
    private int currentPage = 0;

    public StockHolder() {
    }

    public void setInventory(Inventory inventory) {
        this.inventory = inventory;
    }

    public int getCurrentPage() {
        return currentPage;
    }

    public void setCurrentPage(int currentPage) {
        this.currentPage = currentPage;
    }

    @Override
    public @NotNull Inventory getInventory() {
        return inventory;
    }
}
