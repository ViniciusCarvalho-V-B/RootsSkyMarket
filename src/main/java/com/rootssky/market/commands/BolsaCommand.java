package com.rootssky.market.commands;

import com.rootssky.market.RootsSkyMarket;
import com.rootssky.market.gui.StockGUI;
import com.rootssky.market.model.MarketItem;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.stream.Collectors;

public class BolsaCommand implements CommandExecutor, TabCompleter {

    private final RootsSkyMarket plugin;

    public BolsaCommand(RootsSkyMarket plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(com.rootssky.market.utils.MessageUtils.get(plugin.getConfig(), "only_players"));
            return true;
        }

        if (args.length == 0) {
            player.closeInventory();
            plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
                com.rootssky.market.gui.MainMenuGUI mainMenu = new com.rootssky.market.gui.MainMenuGUI(plugin);
                mainMenu.open(player);
            }, 1L);
            return true;
        }

        switch (args[0].toLowerCase()) {
            case "info" -> handleInfo(player, args);
            case "carteira" -> {
                player.closeInventory();
                plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
                    com.rootssky.market.gui.PortfolioGUI portfolio = new com.rootssky.market.gui.PortfolioGUI(plugin, player);
                    portfolio.open();
                }, 1L);
            }
            case "top" -> handleTop(player);
            default -> player.sendMessage("§cUso: /bolsa, /bolsa info [item], /bolsa carteira ou /bolsa top");
        }

        return true;
    }

    private void handleInfo(Player player, String[] args) {
        String itemId;

        if (args.length >= 2) {
            itemId = args[1].toUpperCase();
        } else {
            var hand = player.getInventory().getItemInMainHand();
            if (hand == null || hand.getType().isAir()) {
                player.sendMessage(com.rootssky.market.utils.MessageUtils.get(plugin.getConfig(), "hold_item"));
                return;
            }
            itemId = hand.getType().name();
        }

        MarketItem item = plugin.getMarketCache().getItem(itemId);
        if (item == null) {
            player.sendMessage(com.rootssky.market.utils.MessageUtils.get(plugin.getConfig(), "item_not_found").replace("{item}", itemId));
            return;
        }

        BigDecimal currentPrice = item.getCurrentPrice();
        BigDecimal basePrice = item.getBasePrice();
        BigDecimal variationPct;

        if (basePrice.compareTo(BigDecimal.ZERO) == 0) {
            variationPct = BigDecimal.ZERO;
        } else {
            variationPct = currentPrice.subtract(basePrice)
                    .multiply(BigDecimal.valueOf(100))
                    .divide(basePrice, 2, RoundingMode.HALF_UP);
        }

        int comp = variationPct.compareTo(BigDecimal.ZERO);
        String varColor = comp > 0 ? "§a" : (comp < 0 ? "§c" : "§7");
        String varSign = comp > 0 ? "+" : "";

        player.sendMessage("§e§l=== Relatório: " + itemId + " ===");
        player.sendMessage("§7Preço Atual: §a" + plugin.getVaultBridge().format(currentPrice.doubleValue()));
        player.sendMessage("§7Variação (24h): " + varColor + varSign + variationPct.setScale(2, RoundingMode.HALF_UP) + "%");
        player.sendMessage("§7Mínimo Histórico: §f" + plugin.getVaultBridge().format(item.getFloorPrice().doubleValue()));
        player.sendMessage("§7Máximo Histórico: §f" + plugin.getVaultBridge().format(item.getCeilingPrice().doubleValue()));
        player.sendMessage("§7Volume Negociado: §f" + item.getVolume24h() + " unidades");
    }

    private void handleTop(Player player) {
        player.sendMessage(com.rootssky.market.utils.MessageUtils.get(plugin.getConfig(), "calculating_top"));
        
        plugin.getDatabaseManager().getAllSharesGlobal().thenAccept(allShares -> {
            java.util.Map<String, Double> wealth = new java.util.HashMap<>();
            
            for (var entry : allShares.entrySet()) {
                String uuid = entry.getKey();
                double total = 0;
                for (var shareEntry : entry.getValue().entrySet()) {
                    MarketItem item = plugin.getMarketCache().getItem(shareEntry.getKey());
                    if (item != null) {
                        total += item.getCurrentPrice().doubleValue() * shareEntry.getValue();
                    }
                }
                if (total > 0) {
                    wealth.put(uuid, total);
                }
            }
            
            List<java.util.Map.Entry<String, Double>> sorted = wealth.entrySet().stream()
                    .sorted((a, b) -> Double.compare(b.getValue(), a.getValue()))
                    .limit(10)
                    .collect(Collectors.toList());
                    
            plugin.getServer().getScheduler().runTask(plugin, () -> {
                player.sendMessage(com.rootssky.market.utils.MessageUtils.getNoPrefix(plugin.getConfig(), "top_header"));
                if (sorted.isEmpty()) {
                    player.sendMessage(com.rootssky.market.utils.MessageUtils.get(plugin.getConfig(), "top_empty"));
                    return;
                }
                int rank = 1;
                String format = com.rootssky.market.utils.MessageUtils.getNoPrefix(plugin.getConfig(), "top_format");
                for (var entry : sorted) {
                    org.bukkit.OfflinePlayer op = plugin.getServer().getOfflinePlayer(java.util.UUID.fromString(entry.getKey()));
                    String name = op.getName() != null ? op.getName() : "Desconhecido";
                    String line = format.replace("{rank}", String.valueOf(rank))
                                        .replace("{player}", name)
                                        .replace("{wealth}", plugin.getVaultBridge().format(entry.getValue()));
                    player.sendMessage(line);
                    rank++;
                }
            });
        });
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 1) {
            String partial = args[0].toLowerCase();
            return List.of("info", "carteira", "top").stream()
                    .filter(s -> s.startsWith(partial))
                    .collect(Collectors.toList());
        }

        if (args.length == 2 && args[0].equalsIgnoreCase("info")) {
            String partial = args[1].toUpperCase();
            return plugin.getMarketCache().getAllItemsMap().keySet().stream()
                    .filter(s -> s.toUpperCase().startsWith(partial))
                    .sorted()
                    .collect(Collectors.toList());
        }

        return List.of();
    }
}