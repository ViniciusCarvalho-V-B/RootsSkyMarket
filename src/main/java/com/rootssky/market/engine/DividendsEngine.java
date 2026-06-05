package com.rootssky.market.engine;

import com.rootssky.market.RootsSkyMarket;
import com.rootssky.market.model.MarketItem;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.scheduler.BukkitRunnable;

import java.math.BigDecimal;
import java.util.Map;
import java.util.UUID;

public class DividendsEngine extends BukkitRunnable {

    private final RootsSkyMarket plugin;

    public DividendsEngine(RootsSkyMarket plugin) {
        this.plugin = plugin;
    }

    public void start() {
        if (!plugin.getConfig().getBoolean("dividends.enabled", true)) return;
        
        long intervalHours = plugin.getConfig().getLong("dividends.interval_hours", 24);
        long ticks = intervalHours * 60 * 60 * 20L;
        
        this.runTaskTimerAsynchronously(plugin, ticks, ticks);
    }

    @Override
    public void run() {
        if (plugin.getMarketIndexManager().getCurrentState() == MarketIndexManager.MarketState.BEAR) {
            // Em Bear Market, não roda dividendos/custódia para proteger ou punir? 
            // Conforme plano, sem dividendos, mas a custódia continua!
            Bukkit.getLogger().info("[RootsSkyMarket] Executando rotina de Custódia (Bear Market ativo, sem dividendos)...");
        } else {
            Bukkit.getLogger().info("[RootsSkyMarket] Executando rotina de Dividendos e Custódia...");
        }

        double divPercent = plugin.getConfig().getDouble("dividends.dividend_percent", 1.5) / 100.0;
        double custPercent = plugin.getConfig().getDouble("dividends.custody_tax_percent", 1.0) / 100.0;

        plugin.getDatabaseManager().getAllSharesGlobal().thenAccept(allShares -> {
            for (Map.Entry<String, Map<String, Integer>> entry : allShares.entrySet()) {
                String uuidStr = entry.getKey();
                OfflinePlayer player = Bukkit.getOfflinePlayer(UUID.fromString(uuidStr));
                
                for (Map.Entry<String, Integer> shareEntry : entry.getValue().entrySet()) {
                    String itemId = shareEntry.getKey();
                    int amount = shareEntry.getValue();
                    if (amount <= 0) continue;

                    MarketItem item = plugin.getMarketCache().getItem(itemId);
                    if (item == null) continue;

                    double currentPrice = item.getCurrentPrice().doubleValue();
                    double basePrice = item.getBasePrice().doubleValue();
                    double totalValue = currentPrice * amount;

                    if (currentPrice > basePrice) {
                        // Lucrando = Dividendos (se não for Bear Market)
                        if (plugin.getMarketIndexManager().getCurrentState() != MarketIndexManager.MarketState.BEAR) {
                            double dividend = totalValue * divPercent;
                            plugin.getVaultBridge().deposit(player, dividend);
                            if (player.isOnline() && player.getPlayer() != null) {
                                String msg = com.rootssky.market.utils.MessageUtils.get(plugin.getConfig(), "dividend_received")
                                        .replace("{amount}", plugin.getVaultBridge().format(dividend))
                                        .replace("{item}", itemId);
                                player.getPlayer().sendMessage(msg);
                            }
                        }
                    } else if (currentPrice < basePrice) {
                        // Prejuízo = Taxa de Custódia
                        double custodyFee = totalValue * custPercent;
                        
                    if (plugin.getVaultBridge().withdraw(player, custodyFee)) {
                            if (player.isOnline() && player.getPlayer() != null) {
                                String msg = com.rootssky.market.utils.MessageUtils.get(plugin.getConfig(), "custody_charged")
                                        .replace("{amount}", plugin.getVaultBridge().format(custodyFee))
                                        .replace("{item}", itemId);
                                player.getPlayer().sendMessage(msg);
                            }
                        } else {
                            // Liquidação forçada
                            int amountToLiquidate = (int) Math.ceil(custodyFee / currentPrice);
                            if (amountToLiquidate > amount) amountToLiquidate = amount;
                            
                            plugin.getPlayerSharesManager().removeShares(uuidStr, itemId, amountToLiquidate);
                            if (player.isOnline() && player.getPlayer() != null) {
                                String msg = com.rootssky.market.utils.MessageUtils.get(plugin.getConfig(), "custody_liquidated")
                                        .replace("{amount}", String.valueOf(amountToLiquidate))
                                        .replace("{item}", itemId);
                                player.getPlayer().sendMessage(msg);
                            }
                        }
                    }
                }
            }
        });
    }
}
