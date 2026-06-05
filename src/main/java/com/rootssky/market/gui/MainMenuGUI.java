package com.rootssky.market.gui;

import com.rootssky.market.RootsSkyMarket;
import com.rootssky.market.engine.MarketIndexManager;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.List;

public class MainMenuGUI implements Listener {

    private final RootsSkyMarket plugin;

    public MainMenuGUI(RootsSkyMarket plugin) {
        this.plugin = plugin;
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
    }

    public void open(Player player) {
        MainMenuHolder holder = new MainMenuHolder();
        String titleStr = plugin.getConfig().getString("gui.titles.main_menu", "RootsSkyMarket");
        Inventory inv = Bukkit.createInventory(holder, 27, Component.text(net.md_5.bungee.api.ChatColor.translateAlternateColorCodes('&', titleStr)));
        holder.setInventory(inv);

        // Slot 11: Mercado de Ações
        ItemStack marketItem = new ItemStack(Material.GOLD_BLOCK);
        ItemMeta marketMeta = marketItem.getItemMeta();
        marketMeta.displayName(Component.text("Mercado de Ações").color(NamedTextColor.YELLOW).decoration(TextDecoration.BOLD, true));
        List<Component> marketLore = new ArrayList<>();
        marketLore.add(Component.text("Acesse o painel para comprar e").color(NamedTextColor.GRAY));
        marketLore.add(Component.text("vender ações de itens do servidor.").color(NamedTextColor.GRAY));
        marketLore.add(Component.empty());
        marketLore.add(Component.text("Clique para acessar!").color(NamedTextColor.GREEN));
        marketMeta.lore(marketLore);
        marketItem.setItemMeta(marketMeta);
        inv.setItem(11, marketItem);

        // Slot 13: Status Global
        ItemStack statusItem = new ItemStack(Material.COMPASS);
        ItemMeta statusMeta = statusItem.getItemMeta();
        statusMeta.displayName(Component.text("Painel Global").color(NamedTextColor.AQUA).decoration(TextDecoration.BOLD, true));
        List<Component> statusLore = new ArrayList<>();
        
        MarketIndexManager.MarketState state = plugin.getMarketIndexManager().getCurrentState();
        String stateName = "NORMAL";
        NamedTextColor stateColor = NamedTextColor.GRAY;
        if (state == MarketIndexManager.MarketState.BULL) {
            stateName = "BULL MARKET (Em Alta)";
            stateColor = NamedTextColor.GREEN;
        } else if (state == MarketIndexManager.MarketState.BEAR) {
            stateName = "BEAR MARKET (Em Baixa)";
            stateColor = NamedTextColor.RED;
        }
        
        statusLore.add(Component.text("Estado Atual: ").color(NamedTextColor.WHITE).append(Component.text(stateName).color(stateColor)));
        statusLore.add(Component.empty());
        
        String hotStock = plugin.getHotStockManager().getCurrentHotStockId();
        if (hotStock == null) hotStock = "Nenhum";
        statusLore.add(Component.text("Queridinho do Dia: ").color(NamedTextColor.WHITE).append(Component.text(hotStock).color(NamedTextColor.YELLOW)));
        statusLore.add(Component.text(" (0% de taxas na negociação)").color(NamedTextColor.GRAY));
        
        statusMeta.lore(statusLore);
        statusItem.setItemMeta(statusMeta);
        inv.setItem(13, statusItem);

        // Slot 15: Carteira (Portfolio)
        ItemStack portfolioItem = new ItemStack(Material.WRITABLE_BOOK);
        ItemMeta portfolioMeta = portfolioItem.getItemMeta();
        portfolioMeta.displayName(Component.text("Sua Carteira").color(NamedTextColor.LIGHT_PURPLE).decoration(TextDecoration.BOLD, true));
        List<Component> portfolioLore = new ArrayList<>();
        portfolioLore.add(Component.text("Veja seus rendimentos e").color(NamedTextColor.GRAY));
        portfolioLore.add(Component.text("liquide suas ações.").color(NamedTextColor.GRAY));
        portfolioLore.add(Component.empty());
        portfolioLore.add(Component.text("Dica: Use /bolsa top para ver o ranking!").color(NamedTextColor.GRAY).decorate(TextDecoration.ITALIC));
        portfolioLore.add(Component.empty());
        portfolioLore.add(Component.text("Clique para acessar!").color(NamedTextColor.GREEN));
        portfolioMeta.lore(portfolioLore);
        portfolioItem.setItemMeta(portfolioMeta);
        inv.setItem(15, portfolioItem);

        player.openInventory(inv);
    }

    @EventHandler
    public void onClick(InventoryClickEvent event) {
        if (event.getInventory().getHolder() instanceof MainMenuHolder) {
            event.setCancelled(true);
            if (!(event.getWhoClicked() instanceof Player player)) return;

            int slot = event.getSlot();
            if (slot == 11) {
                player.closeInventory();
                plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
                    StockGUI stock = new StockGUI(plugin);
                    stock.open(player, 0);
                }, 1L);
            } else if (slot == 15) {
                player.closeInventory();
                plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
                    PortfolioGUI portfolio = new PortfolioGUI(plugin, player);
                    portfolio.open();
                }, 1L);
            }
        }
    }
}
