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
import java.util.Comparator;
import java.util.List;

public class StockGUI {

    private static final int SIZE = 54;
    private static final int SLOT_HEADER = 4;
    private static final int SLOT_BACK = 49;
    private static final int SLOT_PREV = 45;
    private static final int SLOT_NEXT = 53;

    private static final int[] GAINER_SLOTS = {10, 11, 12, 19, 20, 21};
    private static final int[] LOSER_SLOTS = {14, 15, 16, 23, 24, 25};
    // Linha 3 (27-35) será usada como separador. Itens listados vão para a linha 4 (36-44).
    private static final int[] ITEM_SLOTS = {36, 37, 38, 39, 40, 41, 42, 43, 44};

    private final RootsSkyMarket plugin;
    private Inventory inventory;

    public StockGUI(RootsSkyMarket plugin) {
        this.plugin = plugin;
    }

    public void open(Player player, int page) {
        StockHolder holder = new StockHolder();
        holder.setCurrentPage(page);
        String titleStr = plugin.getConfig().getString("gui.titles.stock_market", "Bolsa de Valores") + " - Pag " + (page + 1);
        inventory = Bukkit.createInventory(holder, SIZE, Component.text(net.md_5.bungee.api.ChatColor.translateAlternateColorCodes('&', titleStr)));
        holder.setInventory(inventory);

        buildBoard(page);
        player.openInventory(inventory);
    }

    private void buildBoard(int page) {
        fillBackground();
        buildHeader();
        buildTopGainers();
        buildTopLosers();
        buildAllItems(page);
        buildBackButton();
    }

    private void fillBackground() {
        ItemStack glass = createItem(Material.BLACK_STAINED_GLASS_PANE,
                Component.text(" ").color(NamedTextColor.GRAY));
        for (int i = 0; i < SIZE; i++) {
            inventory.setItem(i, glass);
        }
    }

    private void buildHeader() {
        int totalVolume = plugin.getMarketCache().getAllItems().stream()
                .mapToInt(MarketItem::getVolume24h)
                .sum();
        int totalItems = plugin.getMarketCache().size();

        Component name = Component.text("Resumo Global")
                .color(NamedTextColor.GOLD)
                .decoration(TextDecoration.BOLD, true);
        List<Component> lore = new ArrayList<>();
        lore.add(Component.empty());
        lore.add(Component.text("Volume Negociado (24h): ").color(NamedTextColor.GRAY)
                .append(Component.text(totalVolume + " unidades").color(NamedTextColor.WHITE)));
        lore.add(Component.text("Itens Monitorados: ").color(NamedTextColor.GRAY)
                .append(Component.text(totalItems + " itens").color(NamedTextColor.WHITE)));
        lore.add(Component.empty());
        lore.add(Component.text("Maiores altas à esquerda").color(NamedTextColor.GREEN));
        lore.add(Component.text("Maiores quedas à direita").color(NamedTextColor.RED));

        inventory.setItem(SLOT_HEADER, createItem(Material.HOPPER, name, lore));
    }

    private void buildTopGainers() {
        List<MarketItem> allItems = new ArrayList<>(plugin.getMarketCache().getAllItems());
        List<MarketItem> gainers = new ArrayList<>();
        for (MarketItem item : allItems) {
            if (calculateVariation(item).compareTo(BigDecimal.ZERO) >= 0) {
                gainers.add(item);
            }
        }
        gainers.sort((a, b) -> calculateVariation(b).compareTo(calculateVariation(a)));

        for (int i = 0; i < GAINER_SLOTS.length; i++) {
            if (i < gainers.size()) {
                MarketItem item = gainers.get(i);
                inventory.setItem(GAINER_SLOTS[i], buildMarketDisplayItem(item, true));
            } else {
                inventory.setItem(GAINER_SLOTS[i], createItem(Material.LIME_STAINED_GLASS_PANE,
                        Component.text(" ").color(NamedTextColor.GRAY)));
            }
        }
    }

    private void buildTopLosers() {
        List<MarketItem> allItems = new ArrayList<>(plugin.getMarketCache().getAllItems());
        List<MarketItem> losers = new ArrayList<>();
        for (MarketItem item : allItems) {
            if (calculateVariation(item).compareTo(BigDecimal.ZERO) < 0) {
                losers.add(item);
            }
        }
        losers.sort(Comparator.comparing(this::calculateVariation));

        for (int i = 0; i < LOSER_SLOTS.length; i++) {
            if (i < losers.size()) {
                MarketItem item = losers.get(i);
                inventory.setItem(LOSER_SLOTS[i], buildMarketDisplayItem(item, false));
            } else {
                inventory.setItem(LOSER_SLOTS[i], createItem(Material.RED_STAINED_GLASS_PANE,
                        Component.text(" ").color(NamedTextColor.GRAY)));
            }
        }
    }

    private void buildAllItems(int page) {
        List<MarketItem> allItems = new ArrayList<>(plugin.getMarketCache().getAllItems());
        allItems.sort(Comparator.comparing(MarketItem::getItemId));
        
        int itemsPerPage = ITEM_SLOTS.length;
        int maxPages = (int) Math.ceil((double) allItems.size() / itemsPerPage);
        
        int startIndex = page * itemsPerPage;
        for (int i = 0; i < itemsPerPage; i++) {
            if (startIndex + i < allItems.size()) {
                MarketItem item = allItems.get(startIndex + i);
                boolean isGainer = calculateVariation(item).compareTo(BigDecimal.ZERO) >= 0;
                inventory.setItem(ITEM_SLOTS[i], buildMarketDisplayItem(item, isGainer));
            } else {
                inventory.setItem(ITEM_SLOTS[i], createItem(Material.BLACK_STAINED_GLASS_PANE,
                        Component.text(" ").color(NamedTextColor.GRAY)));
            }
        }
        
        if (page > 0) {
            Component prevName = Component.text("◄ Página Anterior").color(NamedTextColor.YELLOW).decoration(TextDecoration.BOLD, true);
            inventory.setItem(SLOT_PREV, createItem(Material.ARROW, prevName));
        }
        if (page < maxPages - 1) {
            Component nextName = Component.text("Próxima Página ►").color(NamedTextColor.YELLOW).decoration(TextDecoration.BOLD, true);
            inventory.setItem(SLOT_NEXT, createItem(Material.ARROW, nextName));
        }
    }

    private ItemStack buildMarketDisplayItem(MarketItem item, boolean isGainer) {
        Material material = resolveMaterial(item.getItemId());
        BigDecimal variation = calculateVariation(item);
        NamedTextColor varColor = isGainer ? NamedTextColor.GREEN : NamedTextColor.RED;
        String varStr;
        if (variation.compareTo(BigDecimal.ZERO) >= 0) {
            varStr = "+" + variation.setScale(1, RoundingMode.HALF_UP) + "%";
        } else {
            varStr = variation.setScale(1, RoundingMode.HALF_UP) + "%";
        }

        Component name = Component.text(item.getItemId())
                .color(varColor)
                .decoration(TextDecoration.BOLD, true);

        List<Component> lore = new ArrayList<>();
        lore.add(Component.text("Preço Atual: ").color(NamedTextColor.GRAY)
                .append(Component.text(plugin.getVaultBridge().format(item.getCurrentPrice().doubleValue()))
                        .color(NamedTextColor.WHITE)));
        lore.add(Component.text("Variação: ").color(NamedTextColor.GRAY)
                .append(Component.text(varStr).color(varColor)));
        lore.add(Component.text("Volume (24h): ").color(NamedTextColor.GRAY)
                .append(Component.text(item.getVolume24h() + " un").color(NamedTextColor.WHITE)));

        if (isGainer) {
            lore.add(Component.text("Tendência: ▃ ▅ ▆ ▇ █").color(NamedTextColor.GREEN));
        } else {
            lore.add(Component.text("Tendência: █ ▇ ▆ ▅ ▃").color(NamedTextColor.RED));
        }

        return createItem(material, name, lore);
    }

    private Material resolveMaterial(String itemId) {
        try {
            return Material.valueOf(itemId);
        } catch (IllegalArgumentException e) {
            return Material.STONE;
        }
    }

    private void buildBackButton() {
        Component name = Component.text("Voltar ao Menu Principal")
                .color(NamedTextColor.YELLOW)
                .decoration(TextDecoration.BOLD, true);
        List<Component> lore = List.of(
                Component.text("Clique para voltar").color(NamedTextColor.GRAY)
        );
        inventory.setItem(SLOT_BACK, createItem(Material.ARROW, name, lore));
    }

    private List<MarketItem> getSortedByVariation() {
        List<MarketItem> items = new ArrayList<>(plugin.getMarketCache().getAllItems());
        items.sort(Comparator.comparing(this::calculateVariation));
        return items;
    }

    private BigDecimal calculateVariation(MarketItem item) {
        BigDecimal base = item.getBasePrice();
        if (base.compareTo(BigDecimal.ZERO) == 0) return BigDecimal.ZERO;
        return item.getCurrentPrice().subtract(base)
                .multiply(BigDecimal.valueOf(100))
                .divide(base, 2, RoundingMode.HALF_UP);
    }

    public List<MarketItem> getTopGainers(int limit) {
        List<MarketItem> sorted = getSortedByVariation();
        int count = Math.min(limit, sorted.size());
        List<MarketItem> result = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            result.add(sorted.get(sorted.size() - 1 - i));
        }
        return result;
    }

    public List<MarketItem> getTopLosers(int limit) {
        List<MarketItem> sorted = getSortedByVariation();
        int count = Math.min(limit, sorted.size());
        return sorted.subList(0, count);
    }

    private ItemStack createItem(Material material, Component name, List<Component> lore) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.itemName(name);
            if (lore != null && !lore.isEmpty()) {
                meta.lore(lore);
            }
            item.setItemMeta(meta);
        }
        return item;
    }

    private ItemStack createItem(Material material, Component name) {
        return createItem(material, name, null);
    }

    public Inventory getInventory() {
        return inventory;
    }
}