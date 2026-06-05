package com.rootssky.market.gui;

import com.rootssky.market.RootsSkyMarket;
import com.rootssky.market.model.MarketItem;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;

public class StockActionGUI {

    private final RootsSkyMarket plugin;
    private final Player player;
    private final String itemId;
    private Inventory inventory;

    public StockActionGUI(RootsSkyMarket plugin, Player player, String itemId) {
        this.plugin = plugin;
        this.player = player;
        this.itemId = itemId;
    }

    public void open() {
        StockActionHolder holder = new StockActionHolder(itemId);
        String titleStr = plugin.getConfig().getString("gui.titles.stock_action", "Ação: {item}").replace("{item}", itemId);
        inventory = Bukkit.createInventory(holder, 27, Component.text(net.md_5.bungee.api.ChatColor.translateAlternateColorCodes('&', titleStr)));
        holder.setInventory(inventory);
        
        buildBoard();
        player.openInventory(inventory);
    }

    public void buildBoard() {
        ItemStack glass = createItem(Material.BLACK_STAINED_GLASS_PANE, Component.text(" ").color(NamedTextColor.GRAY));
        for (int i = 0; i < 27; i++) {
            inventory.setItem(i, glass);
        }

        MarketItem item = plugin.getMarketCache().getItem(itemId);
        if (item == null) return;

        int ownedShares = plugin.getPlayerSharesManager().getShares(player.getUniqueId().toString(), itemId);
        double currentPrice = item.getCurrentPrice().doubleValue();

        // Slot 11: Buy 10 Shares
        Component buyName = Component.text("Comprar 10 Ações").color(NamedTextColor.GREEN).decoration(TextDecoration.BOLD, true);
        List<Component> buyLore = new ArrayList<>();
        buyLore.add(Component.text("Preço Total: ").color(NamedTextColor.GRAY).append(Component.text(plugin.getVaultBridge().format(currentPrice * 10)).color(NamedTextColor.WHITE)));
        buyLore.add(Component.empty());
        buyLore.add(Component.text("Clique para comprar").color(NamedTextColor.YELLOW));
        inventory.setItem(11, createItem(Material.LIME_STAINED_GLASS_PANE, buyName, buyLore));

        // Slot 13: Info
        Material mat = Material.STONE;
        try { mat = Material.valueOf(itemId); } catch (Exception ignored) {}
        Component infoName = Component.text(itemId).color(NamedTextColor.GOLD).decoration(TextDecoration.BOLD, true);
        List<Component> infoLore = new ArrayList<>();
        infoLore.add(Component.text("Preço Unitário: ").color(NamedTextColor.GRAY).append(Component.text(plugin.getVaultBridge().format(currentPrice)).color(NamedTextColor.WHITE)));
        infoLore.add(Component.text("Ações que você possui: ").color(NamedTextColor.GRAY).append(Component.text(ownedShares).color(NamedTextColor.WHITE)));
        inventory.setItem(13, createItem(mat, infoName, infoLore));

        // Slot 15: Sell 10 Shares
        Component sellName = Component.text("Vender 10 Ações").color(NamedTextColor.RED).decoration(TextDecoration.BOLD, true);
        
        double taxPercent = plugin.getConfig().getDouble("shares.tax_percent", 5.0);
        
        if (plugin.getMarketIndexManager().getCurrentState() == com.rootssky.market.engine.MarketIndexManager.MarketState.BULL) {
            taxPercent = plugin.getConfig().getDouble("market_index.bull_tax_percent", 2.0);
        } else if (plugin.getMarketIndexManager().getCurrentState() == com.rootssky.market.engine.MarketIndexManager.MarketState.BEAR) {
            taxPercent = plugin.getConfig().getDouble("market_index.bear_tax_percent", 10.0);
        }
        
        if (itemId.equals(plugin.getHotStockManager().getCurrentHotStockId())) {
            taxPercent = 0.0;
        }

        String exemptPerm = plugin.getConfig().getString("shares.vip_tax_exempt_permission", "rootssky.market.tax_exempt");
        
        double sellPrice = currentPrice * 10;
        double taxAmount = 0;
        if (!player.hasPermission(exemptPerm)) {
            taxAmount = sellPrice * (taxPercent / 100.0);
        }
        double finalPrice = sellPrice - taxAmount;
        
        List<Component> sellLore = new ArrayList<>();
        sellLore.add(Component.text("Você recebe: ").color(NamedTextColor.GRAY).append(Component.text(plugin.getVaultBridge().format(finalPrice)).color(NamedTextColor.WHITE)));
        if (taxAmount > 0) {
            sellLore.add(Component.text("Taxa da Bolsa (" + taxPercent + "%): ").color(NamedTextColor.GRAY).append(Component.text("-" + plugin.getVaultBridge().format(taxAmount)).color(NamedTextColor.RED)));
        } else {
            sellLore.add(Component.text("Taxa da Bolsa: ").color(NamedTextColor.GRAY).append(Component.text("ISENTO (VIP)").color(NamedTextColor.GREEN)));
        }
        sellLore.add(Component.empty());
        if (ownedShares >= 10) {
            sellLore.add(Component.text("Clique para vender").color(NamedTextColor.YELLOW));
        } else {
            sellLore.add(Component.text("Você não possui 10 ações").color(NamedTextColor.DARK_RED));
        }
        inventory.setItem(15, createItem(Material.RED_STAINED_GLASS_PANE, sellName, sellLore));
        
        // Slot 22: Back
        Component backName = Component.text("Voltar").color(NamedTextColor.RED).decoration(TextDecoration.BOLD, true);
        inventory.setItem(22, createItem(Material.ARROW, backName));
    }

    private ItemStack createItem(Material material, Component name, List<Component> lore) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.displayName(name);
            if (lore != null) {
                meta.lore(lore);
            }
            item.setItemMeta(meta);
        }
        return item;
    }

    private ItemStack createItem(Material material, Component name) {
        return createItem(material, name, null);
    }
}
