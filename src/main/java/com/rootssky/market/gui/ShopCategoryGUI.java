package com.rootssky.market.gui;

import com.rootssky.market.RootsSkyMarket;
import com.rootssky.market.engine.ShopManager.ShopCategory;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.List;

public class ShopCategoryGUI {

    private final RootsSkyMarket plugin;

    public ShopCategoryGUI(RootsSkyMarket plugin) {
        this.plugin = plugin;
    }

    public void open(Player player) {
        ShopCategoryHolder holder = new ShopCategoryHolder();
        String titleStr = plugin.getConfig().getString("gui.titles.categories", "Categorias da Loja");
        Inventory inventory = Bukkit.createInventory(holder, 36, Component.text(net.md_5.bungee.api.ChatColor.translateAlternateColorCodes('&', titleStr)));
        holder.setInventory(inventory);

        // Fill background
        ItemStack glass = new ItemStack(Material.GRAY_STAINED_GLASS_PANE);
        ItemMeta glassMeta = glass.getItemMeta();
        glassMeta.displayName(Component.text(" "));
        glass.setItemMeta(glassMeta);

        for (int i = 0; i < inventory.getSize(); i++) {
            inventory.setItem(i, glass);
        }

        // Add categories
        for (ShopCategory category : plugin.getShopManager().getCategories().values()) {
            ItemStack item = new ItemStack(category.getIcon());
            ItemMeta meta = item.getItemMeta();
            if (meta != null) {
                meta.displayName(Component.text(category.getName().replace("&", "§")));
                List<Component> lore = new ArrayList<>();
                lore.add(Component.text("§7Clique para ver os itens"));
                if (category.isFixedPrice()) {
                    lore.add(Component.text("§ePreços Fixos (Sem Bolsa)"));
                }
                meta.lore(lore);
                item.setItemMeta(meta);
            }
            if (category.getSlot() >= 0 && category.getSlot() < inventory.getSize()) {
                inventory.setItem(category.getSlot(), item);
            }
        }

        // Add back button or info
        ItemStack info = new ItemStack(Material.PAPER);
        ItemMeta infoMeta = info.getItemMeta();
        infoMeta.displayName(Component.text("§eInformações da Loja"));
        List<Component> infoLore = new ArrayList<>();
        infoLore.add(Component.text("§7A economia do servidor é dinâmica!"));
        infoLore.add(Component.text("§7Preços mudam com base na oferta e demanda."));
        infoLore.add(Component.text("§7Acompanhe as tendências em §e/bolsa§7."));
        infoMeta.lore(infoLore);
        info.setItemMeta(infoMeta);
        inventory.setItem(31, info);

        player.openInventory(inventory);
    }
}
