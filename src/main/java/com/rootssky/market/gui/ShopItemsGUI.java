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
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;

public class ShopItemsGUI {

    private final RootsSkyMarket plugin;
    private final ShopCategory category;
    private final int page;
    private final boolean hideBackButton;

    public ShopItemsGUI(RootsSkyMarket plugin, ShopCategory category) {
        this(plugin, category, 1, false);
    }

    public ShopItemsGUI(RootsSkyMarket plugin, ShopCategory category, int page) {
        this(plugin, category, page, false);
    }

    public ShopItemsGUI(RootsSkyMarket plugin, ShopCategory category, int page, boolean hideBackButton) {
        this.plugin = plugin;
        this.category = category;
        this.page = page;
        this.hideBackButton = hideBackButton;
    }

    public void open(Player player) {
        ShopItemsHolder holder = new ShopItemsHolder(category, hideBackButton);
        holder.setPage(page); // We need to add setPage and getPage to ShopItemsHolder
        
        String title = category.getName().replace("&", "§") + " §8- Pag " + page;
        String titleStr = plugin.getConfig().getString("gui.titles.items", "Comprar/Vender Itens");
        Inventory inventory = Bukkit.createInventory(holder, 54, Component.text(net.md_5.bungee.api.ChatColor.translateAlternateColorCodes('&', titleStr)));
        holder.setInventory(inventory);

        // Fill background
        ItemStack glass = new ItemStack(Material.BLACK_STAINED_GLASS_PANE);
        ItemMeta glassMeta = glass.getItemMeta();
        glassMeta.displayName(Component.text(" "));
        glass.setItemMeta(glassMeta);

        for (int i = 0; i < 9; i++) inventory.setItem(i, glass);
        for (int i = 45; i < 54; i++) inventory.setItem(i, glass);

        List<String> items = category.getItems();
        int maxItemsPerPage = 36;
        int startIndex = (page - 1) * maxItemsPerPage;
        int endIndex = Math.min(startIndex + maxItemsPerPage, items.size());

        int slot = 9;
        for (int i = startIndex; i < endIndex; i++) {
            String itemId = items.get(i);
            
            Material mat;
            try {
                mat = Material.valueOf(itemId.toUpperCase());
            } catch (IllegalArgumentException e) {
                plugin.getLogger().warning("Invalid item in category " + category.getId() + ": " + itemId);
                continue;
            }

            ItemStack item = new ItemStack(mat);
            ItemMeta meta = item.getItemMeta();
            if (meta != null) {
                MarketItem marketItem = plugin.getMarketCache().getItem(itemId);
                List<Component> lore = new ArrayList<>();
                lore.add(Component.text(""));

                if (marketItem != null && !category.isFixedPrice()) {
                    BigDecimal currentPrice = marketItem.getCurrentPrice();
                    BigDecimal basePrice = marketItem.getBasePrice();
                    BigDecimal sellPrice = currentPrice.multiply(BigDecimal.valueOf(0.95));
                    BigDecimal variationPct = BigDecimal.ZERO;

                    if (basePrice.compareTo(BigDecimal.ZERO) > 0) {
                        variationPct = currentPrice.subtract(basePrice)
                                .multiply(BigDecimal.valueOf(100))
                                .divide(basePrice, 2, RoundingMode.HALF_UP);
                    }

                    String varColor;
                    String varSign;
                    String formattedVariation;

                    int comp = variationPct.compareTo(BigDecimal.ZERO);
                    if (comp > 0) {
                        varColor = "§a";
                        varSign = "▲ +";
                        formattedVariation = variationPct.setScale(2, RoundingMode.HALF_UP) + "%";
                    } else if (comp < 0) {
                        varColor = "§c";
                        varSign = "▼ ";
                        formattedVariation = variationPct.setScale(2, RoundingMode.HALF_UP) + "%";
                    } else {
                        varColor = "§7";
                        varSign = "▶ ";
                        formattedVariation = "0.00%";
                    }

                    lore.add(Component.text("§7Preço de Compra: §a" + plugin.getVaultBridge().format(currentPrice.doubleValue())));
                    double vipBonusPct = plugin.getTransactionManager().getVipBonusPercentage(player);
                    if (vipBonusPct > 0.0) {
                        double vipSellPrice = sellPrice.doubleValue() * (1.0 + (vipBonusPct / 100.0));
                        String formattedBase = plugin.getVaultBridge().format(sellPrice.doubleValue());
                        String formattedVip = plugin.getVaultBridge().format(vipSellPrice);
                        lore.add(Component.text("§7Preço de Venda: §7§m" + formattedBase + "§r §a" + formattedVip + " §e(+" + (int)vipBonusPct + "% VIP)"));
                    } else {
                        lore.add(Component.text("§7Preço de Venda: §c" + plugin.getVaultBridge().format(sellPrice.doubleValue())));
                    }
                    lore.add(Component.text(""));
                    lore.add(Component.text("§7Mercado: " + varColor + varSign + formattedVariation));
                } else if (marketItem != null) {
                    BigDecimal sellPrice = marketItem.getBasePrice().multiply(BigDecimal.valueOf(0.95));
                    lore.add(Component.text("§7Preço de Compra: §a" + plugin.getVaultBridge().format(marketItem.getBasePrice().doubleValue())));
                    lore.add(Component.text("§7Preço de Venda: §c" + plugin.getVaultBridge().format(sellPrice.doubleValue())));
                    lore.add(Component.text(""));
                    lore.add(Component.text("§ePreço Fixo"));
                } else {
                    lore.add(Component.text("§7Valor: §cNão definido no mercado"));
                }

                lore.add(Component.text(""));
                lore.add(Component.text("§e[Clique Esquerdo] §7para Comprar/Vender"));
                lore.add(Component.text("§6[Shift + Clique] §7para Vender Tudo (Inventário)"));
                
                meta.lore(lore);
                item.setItemMeta(meta);
            }
            inventory.setItem(slot++, item);
        }

        // Add back button
        if (!hideBackButton) {
            ItemStack back = new ItemStack(Material.ARROW);
            ItemMeta backMeta = back.getItemMeta();
            backMeta.displayName(Component.text("§cVoltar"));
            back.setItemMeta(backMeta);
            inventory.setItem(48, back); // Moved to 48
        } else {
            ItemStack glassPane = new ItemStack(Material.BLACK_STAINED_GLASS_PANE);
            ItemMeta paneMeta = glassPane.getItemMeta();
            if (paneMeta != null) {
                paneMeta.displayName(Component.text(" "));
                glassPane.setItemMeta(paneMeta);
            }
            inventory.setItem(48, glassPane);
        }
        
        // Add balance button
        ItemStack balance = new ItemStack(Material.EMERALD);
        ItemMeta balanceMeta = balance.getItemMeta();
        balanceMeta.displayName(Component.text("§aSeu Saldo"));
        List<Component> balanceLore = new ArrayList<>();
        balanceLore.add(Component.text("§7" + plugin.getVaultBridge().format(plugin.getVaultBridge().getBalance(player))));
        balanceMeta.lore(balanceLore);
        balance.setItemMeta(balanceMeta);
        inventory.setItem(49, balance); // Center bottom
        
        // Pagination
        if (page > 1) {
            ItemStack prev = new ItemStack(Material.PAPER);
            ItemMeta prevMeta = prev.getItemMeta();
            prevMeta.displayName(Component.text("§ePágina Anterior"));
            prev.setItemMeta(prevMeta);
            inventory.setItem(45, prev);
        }
        
        if (items.size() > endIndex) {
            ItemStack next = new ItemStack(Material.PAPER);
            ItemMeta nextMeta = next.getItemMeta();
            nextMeta.displayName(Component.text("§ePróxima Página"));
            next.setItemMeta(nextMeta);
            inventory.setItem(53, next);
        }

        player.openInventory(inventory);
    }
}
