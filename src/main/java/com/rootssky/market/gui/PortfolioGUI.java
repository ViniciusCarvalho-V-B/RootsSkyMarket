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

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class PortfolioGUI {

    private final RootsSkyMarket plugin;
    private final Player player;
    private Inventory inventory;

    public PortfolioGUI(RootsSkyMarket plugin, Player player) {
        this.plugin = plugin;
        this.player = player;
    }

    public void open() {
        PortfolioHolder holder = new PortfolioHolder();
        String title = plugin.getConfig().getString("gui.titles.portfolio", "Sua Carteira de Ações");
        inventory = Bukkit.createInventory(holder, 54, Component.text(net.md_5.bungee.api.ChatColor.translateAlternateColorCodes('&', title)));
        holder.setInventory(inventory);
        
        buildBoard();
        player.openInventory(inventory);
    }

    private void buildBoard() {
        ItemStack glass = createItem(Material.BLACK_STAINED_GLASS_PANE, Component.text(" ").color(NamedTextColor.GRAY));
        for (int i = 0; i < 54; i++) {
            inventory.setItem(i, glass);
        }

        Map<String, Integer> shares = plugin.getPlayerSharesManager().getAllShares(player.getUniqueId().toString());
        
        int slot = 0;
        double totalValue = 0;
        
        for (Map.Entry<String, Integer> entry : shares.entrySet()) {
            if (entry.getValue() <= 0) continue;
            
            String itemId = entry.getKey();
            int amount = entry.getValue();
            
            MarketItem item = plugin.getMarketCache().getItem(itemId);
            if (item == null) continue;
            
            double currentPrice = item.getCurrentPrice().doubleValue();
            double value = currentPrice * amount;
            totalValue += value;
            
            Material mat = Material.STONE;
            try { mat = Material.valueOf(itemId); } catch (Exception ignored) {}
            
            Component name = Component.text(itemId).color(NamedTextColor.GOLD).decoration(TextDecoration.BOLD, true);
            List<Component> lore = new ArrayList<>();
            lore.add(Component.text("Ações: ").color(NamedTextColor.GRAY).append(Component.text(amount).color(NamedTextColor.WHITE)));
            lore.add(Component.text("Valor Atual/unidade: ").color(NamedTextColor.GRAY).append(Component.text(plugin.getVaultBridge().format(currentPrice)).color(NamedTextColor.WHITE)));
            lore.add(Component.text("Patrimônio no Ativo: ").color(NamedTextColor.GRAY).append(Component.text(plugin.getVaultBridge().format(value)).color(NamedTextColor.GREEN)));
            lore.add(Component.empty());
            lore.add(Component.text("Clique para negociar").color(NamedTextColor.YELLOW));
            
            if (slot < 45) {
                inventory.setItem(slot++, createItem(mat, name, lore));
            }
        }
        
        // Slot 45: Voltar
        Component backName = Component.text("Voltar ao Menu Principal").color(NamedTextColor.YELLOW).decoration(TextDecoration.BOLD, true);
        inventory.setItem(45, createItem(Material.ARROW, backName, List.of(Component.text("Clique para voltar").color(NamedTextColor.GRAY))));

        // Slot 49: Info / Total
        Component totalName = Component.text("Resumo da Carteira").color(NamedTextColor.GREEN).decoration(TextDecoration.BOLD, true);
        List<Component> totalLore = new ArrayList<>();
        totalLore.add(Component.text("Patrimônio Total: ").color(NamedTextColor.GRAY).append(Component.text(plugin.getVaultBridge().format(totalValue)).color(NamedTextColor.GOLD)));
        inventory.setItem(49, createItem(Material.EMERALD, totalName, totalLore));
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
