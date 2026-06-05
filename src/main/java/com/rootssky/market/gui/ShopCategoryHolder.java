package com.rootssky.market.gui;

import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.jetbrains.annotations.NotNull;

public class ShopCategoryHolder implements InventoryHolder {
    private Inventory inventory;

    @Override
    public @NotNull Inventory getInventory() {
        return inventory;
    }

    public void setInventory(Inventory inventory) {
        this.inventory = inventory;
    }
}
