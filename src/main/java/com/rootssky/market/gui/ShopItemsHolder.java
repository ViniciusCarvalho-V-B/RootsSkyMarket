package com.rootssky.market.gui;

import com.rootssky.market.engine.ShopManager.ShopCategory;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.jetbrains.annotations.NotNull;

public class ShopItemsHolder implements InventoryHolder {
    private Inventory inventory;
    private final ShopCategory category;
    private int page = 1;
    private boolean hideBackButton = false;

    public ShopItemsHolder(ShopCategory category) {
        this.category = category;
    }

    public ShopItemsHolder(ShopCategory category, boolean hideBackButton) {
        this.category = category;
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

    public int getPage() {
        return page;
    }

    public void setPage(int page) {
        this.page = page;
    }

    public boolean isHideBackButton() {
        return hideBackButton;
    }

    public void setHideBackButton(boolean hideBackButton) {
        this.hideBackButton = hideBackButton;
    }
}
