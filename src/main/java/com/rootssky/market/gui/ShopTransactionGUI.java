package com.rootssky.market.gui;

import com.rootssky.market.RootsSkyMarket;
import com.rootssky.market.engine.ShopManager.ShopCategory;
import com.rootssky.market.model.MarketItem;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

public class ShopTransactionGUI {

    private final RootsSkyMarket plugin;
    private final ShopCategory category;
    private final String itemId;

    public ShopTransactionGUI(RootsSkyMarket plugin, ShopCategory category, String itemId) {
        this.plugin = plugin;
        this.category = category;
        this.itemId = itemId;
    }

    public void open(Player player) {
        ShopTransactionHolder holder = new ShopTransactionHolder(category, itemId);
        String titleStr = plugin.getConfig().getString("gui.titles.transaction", "Transação de {item}").replace("{item}", itemId);
        Inventory inventory = Bukkit.createInventory(holder, 45, Component.text(net.md_5.bungee.api.ChatColor.translateAlternateColorCodes('&', titleStr)));
        holder.setInventory(inventory);

        // Fill background
        ItemStack glass = new ItemStack(Material.BLACK_STAINED_GLASS_PANE);
        ItemMeta glassMeta = glass.getItemMeta();
        glassMeta.displayName(Component.text(" "));
        glass.setItemMeta(glassMeta);

        for (int i = 0; i < inventory.getSize(); i++) {
            inventory.setItem(i, glass);
        }

        MarketItem marketItem = plugin.getMarketCache().getItem(itemId);
        double unitPrice = 0.0;
        if (marketItem != null) {
            unitPrice = category.isFixedPrice() ? marketItem.getBasePrice().doubleValue() : marketItem.getCurrentPrice().doubleValue();
        }

        // Central Item
        Material mat = Material.valueOf(itemId);
        ItemStack centerItem = new ItemStack(mat);
        ItemMeta centerMeta = centerItem.getItemMeta();
        if (centerMeta != null) {
            centerMeta.displayName(Component.text("§6" + itemId));
            List<Component> lore = new ArrayList<>();
            lore.add(Component.text("§7Preço Unitário: §a" + plugin.getVaultBridge().format(unitPrice)));
            centerMeta.lore(lore);
            centerItem.setItemMeta(centerMeta);
        }
        inventory.setItem(13, centerItem);

        // Buy options (Left Side)
        inventory.setItem(19, createActionBtn(Material.GREEN_STAINED_GLASS_PANE, "§aComprar 1", 1, unitPrice, true));
        inventory.setItem(20, createActionBtn(Material.GREEN_STAINED_GLASS_PANE, "§aComprar 16", 16, unitPrice, true));
        inventory.setItem(21, createActionBtn(Material.GREEN_STAINED_GLASS_PANE, "§aComprar 32", 32, unitPrice, true));
        inventory.setItem(28, createActionBtn(Material.GREEN_STAINED_GLASS_PANE, "§aComprar 64", 64, unitPrice, true));

        // Sell options (Right Side)
        inventory.setItem(23, createActionBtn(Material.RED_STAINED_GLASS_PANE, "§cVender 1", 1, unitPrice, false));
        inventory.setItem(24, createActionBtn(Material.RED_STAINED_GLASS_PANE, "§cVender 16", 16, unitPrice, false));
        inventory.setItem(25, createActionBtn(Material.RED_STAINED_GLASS_PANE, "§cVender 32", 32, unitPrice, false));
        inventory.setItem(34, createActionBtn(Material.RED_STAINED_GLASS_PANE, "§cVender 64", 64, unitPrice, false));

        // Buy Custom option
        ItemStack buyCustom = new ItemStack(Material.OAK_SIGN);
        ItemMeta bcMeta = buyCustom.getItemMeta();
        bcMeta.displayName(Component.text("§aComprar Quantidade Personalizada"));
        List<Component> bcLore = new ArrayList<>();
        bcLore.add(Component.text("§7Clique para digitar no"));
        bcLore.add(Component.text("§7chat a quantidade desejada."));
        bcMeta.lore(bcLore);
        buyCustom.setItemMeta(bcMeta);
        inventory.setItem(29, buyCustom);

        // Sell Custom option
        ItemStack sellCustom = new ItemStack(Material.OAK_SIGN);
        ItemMeta scMeta = sellCustom.getItemMeta();
        scMeta.displayName(Component.text("§cVender Quantidade Personalizada"));
        List<Component> scLore = new ArrayList<>();
        scLore.add(Component.text("§7Clique para digitar no"));
        scLore.add(Component.text("§7chat a quantidade desejada."));
        scMeta.lore(scLore);
        sellCustom.setItemMeta(scMeta);
        inventory.setItem(35, sellCustom);

        // Sell All option
        ItemStack sellAll = new ItemStack(Material.HOPPER);
        ItemMeta saMeta = sellAll.getItemMeta();
        saMeta.displayName(Component.text("§6Vender Tudo (Inventário)"));
        sellAll.setItemMeta(saMeta);
        inventory.setItem(32, sellAll);

        // Add back button
        ItemStack back = new ItemStack(Material.ARROW);
        ItemMeta backMeta = back.getItemMeta();
        backMeta.displayName(Component.text("§cVoltar"));
        back.setItemMeta(backMeta);
        inventory.setItem(40, back);

        player.openInventory(inventory);
    }

    private ItemStack createActionBtn(Material mat, String name, int amount, double unitPrice, boolean isBuy) {
        ItemStack item = new ItemStack(mat, amount);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.displayName(Component.text(name));
            List<Component> lore = new ArrayList<>();
            double total = unitPrice * amount;
            if (!isBuy) {
                // Remove the 5% spread from sell price by default to match EconomyShopGUI default behavior
                total = total * 0.95; 
            }
            
            lore.add(Component.text("§7Total: §f" + plugin.getVaultBridge().format(total)));
            lore.add(Component.text(""));
            lore.add(Component.text(isBuy ? "§eClique para comprar" : "§eClique para vender"));
            meta.lore(lore);
            item.setItemMeta(meta);
        }
        return item;
    }
}
