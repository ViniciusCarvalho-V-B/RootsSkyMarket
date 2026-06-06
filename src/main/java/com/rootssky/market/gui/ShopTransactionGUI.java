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
    private final boolean hideBackButton;

    public ShopTransactionGUI(RootsSkyMarket plugin, ShopCategory category, String itemId) {
        this(plugin, category, itemId, false);
    }

    public ShopTransactionGUI(RootsSkyMarket plugin, ShopCategory category, String itemId, boolean hideBackButton) {
        this.plugin = plugin;
        this.category = category;
        this.itemId = itemId;
        this.hideBackButton = hideBackButton;
    }

    public void open(Player player) {
        ShopTransactionHolder holder = new ShopTransactionHolder(category, itemId, hideBackButton);
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

        double vipBonusPct = plugin.getTransactionManager().getVipBonusPercentage(player);

        // Central Item
        Material mat = Material.valueOf(itemId);
        ItemStack centerItem = new ItemStack(mat);
        ItemMeta centerMeta = centerItem.getItemMeta();
        if (centerMeta != null) {
            centerMeta.displayName(Component.text("§6" + itemId));
            List<Component> lore = new ArrayList<>();
            lore.add(Component.text("§7Preço Unitário: §a" + plugin.getVaultBridge().format(unitPrice)));
            
            double baseSellPrice = unitPrice * 0.95;
            if (vipBonusPct > 0.0) {
                double vipSellPrice = baseSellPrice * (1.0 + (vipBonusPct / 100.0));
                lore.add(Component.text("§7Venda Unitária: §c" + plugin.getVaultBridge().format(vipSellPrice) + " §e(+" + (int)vipBonusPct + "% VIP)"));
                lore.add(Component.text("§8(Preço base: " + plugin.getVaultBridge().format(baseSellPrice) + ")"));
            } else {
                lore.add(Component.text("§7Venda Unitária: §c" + plugin.getVaultBridge().format(baseSellPrice)));
            }
            centerMeta.lore(lore);
            centerItem.setItemMeta(centerMeta);
        }
        inventory.setItem(13, centerItem);

        // Buy options (Left Side)
        inventory.setItem(19, createActionBtn(Material.GREEN_STAINED_GLASS_PANE, "§aComprar 1", 1, unitPrice, true, 0.0));
        inventory.setItem(20, createActionBtn(Material.GREEN_STAINED_GLASS_PANE, "§aComprar 16", 16, unitPrice, true, 0.0));
        inventory.setItem(21, createActionBtn(Material.GREEN_STAINED_GLASS_PANE, "§aComprar 32", 32, unitPrice, true, 0.0));
        inventory.setItem(28, createActionBtn(Material.GREEN_STAINED_GLASS_PANE, "§aComprar 64", 64, unitPrice, true, 0.0));

        // Sell options (Right Side)
        inventory.setItem(23, createActionBtn(Material.RED_STAINED_GLASS_PANE, "§cVender 1", 1, unitPrice, false, vipBonusPct));
        inventory.setItem(24, createActionBtn(Material.RED_STAINED_GLASS_PANE, "§cVender 16", 16, unitPrice, false, vipBonusPct));
        inventory.setItem(25, createActionBtn(Material.RED_STAINED_GLASS_PANE, "§cVender 32", 32, unitPrice, false, vipBonusPct));
        inventory.setItem(34, createActionBtn(Material.RED_STAINED_GLASS_PANE, "§cVender 64", 64, unitPrice, false, vipBonusPct));

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
        if (!hideBackButton) {
            ItemStack back = new ItemStack(Material.ARROW);
            ItemMeta backMeta = back.getItemMeta();
            backMeta.displayName(Component.text("§cVoltar"));
            back.setItemMeta(backMeta);
            inventory.setItem(40, back);
        } else {
            ItemStack glassPane = new ItemStack(Material.BLACK_STAINED_GLASS_PANE);
            ItemMeta paneMeta = glassPane.getItemMeta();
            if (paneMeta != null) {
                paneMeta.displayName(Component.text(" "));
                glassPane.setItemMeta(paneMeta);
            }
            inventory.setItem(40, glassPane);
        }
 
        player.openInventory(inventory);
    }
 
    private ItemStack createActionBtn(Material mat, String name, int amount, double unitPrice, boolean isBuy, double vipBonusPct) {
        ItemStack item = new ItemStack(mat, amount);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.displayName(Component.text(name));
            List<Component> lore = new ArrayList<>();
            double total = unitPrice * amount;
            if (!isBuy) {
                total = total * 0.95; 
                if (vipBonusPct > 0.0) {
                    double vipTotal = total * (1.0 + (vipBonusPct / 100.0));
                    String formattedBase = plugin.getVaultBridge().format(total);
                    String formattedVip = plugin.getVaultBridge().format(vipTotal);
                    lore.add(Component.text("§7Total: §7§m" + formattedBase + "§r §a" + formattedVip + " §e(+" + (int)vipBonusPct + "% VIP)"));
                } else {
                    lore.add(Component.text("§7Total: §f" + plugin.getVaultBridge().format(total)));
                }
            } else {
                lore.add(Component.text("§7Total: §f" + plugin.getVaultBridge().format(total)));
            }
            
            lore.add(Component.text(""));
            lore.add(Component.text(isBuy ? "§eClique para comprar" : "§eClique para vender"));
            meta.lore(lore);
            item.setItemMeta(meta);
        }
        return item;
    }
}
