package com.rootssky.market.engine;

import com.rootssky.market.RootsSkyMarket;
import org.bukkit.Bukkit;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.Random;

public class MarketIndexManager extends BukkitRunnable {

    public enum MarketState {
        NORMAL, BULL, BEAR
    }

    private final RootsSkyMarket plugin;
    private final Random random = new Random();
    private MarketState currentState = MarketState.NORMAL;

    public MarketIndexManager(RootsSkyMarket plugin) {
        this.plugin = plugin;
    }

    public void start() {
        if (!plugin.getConfig().getBoolean("market_index.enabled", true)) return;
        
        long intervalHours = plugin.getConfig().getLong("market_index.interval_hours", 12);
        long ticks = intervalHours * 60 * 60 * 20L;
        
        this.runTaskTimer(plugin, ticks, ticks);
    }

    @Override
    public void run() {
        int r = random.nextInt(100);
        if (r < 60) {
            setMarketState(MarketState.NORMAL);
        } else if (r < 80) {
            setMarketState(MarketState.BULL);
        } else {
            setMarketState(MarketState.BEAR);
        }
    }

    public void setMarketState(MarketState newState) {
        if (currentState == newState) return;
        
        MarketState oldState = currentState;
        currentState = newState;
        
        if (newState == MarketState.NORMAL) {
            Bukkit.broadcastMessage(com.rootssky.market.utils.MessageUtils.get(plugin.getConfig(), "market_normal"));
        } else if (newState == MarketState.BULL) {
            Bukkit.broadcastMessage(com.rootssky.market.utils.MessageUtils.get(plugin.getConfig(), "market_bull"));
            com.rootssky.market.utils.DiscordWebhook.send(plugin, "📈 **BULL MARKET!** A economia está em forte alta! Impostos foram reduzidos e agora é a hora de vender suas ações!");
        } else if (newState == MarketState.BEAR) {
            Bukkit.broadcastMessage(com.rootssky.market.utils.MessageUtils.get(plugin.getConfig(), "market_bear"));
            com.rootssky.market.utils.DiscordWebhook.send(plugin, "📉 **BEAR MARKET!** O mercado despencou! Impostos cobrados em dobro e dividendos suspensos. Segurem suas carteiras!");
        }

        applyMarketStatePriceShift(oldState, newState);
    }

    private void applyMarketStatePriceShift(MarketState oldState, MarketState newState) {
        double factor = 1.0;
        
        if (newState == MarketState.BULL) {
            factor = 1.20; // Sobe 20% imediato
        } else if (newState == MarketState.BEAR) {
            factor = 0.80; // Cai 20% imediato
        } else if (newState == MarketState.NORMAL) {
            if (oldState == MarketState.BULL) {
                factor = 1.0 / 1.20; // Reverte a alta
            } else if (oldState == MarketState.BEAR) {
                factor = 1.0 / 0.80; // Reverte a queda
            }
        }
        
        if (factor == 1.0) return;
        
        java.math.BigDecimal multiplier = java.math.BigDecimal.valueOf(factor);
        
        for (com.rootssky.market.model.MarketItem item : plugin.getMarketCache().getAllItems()) {
            // Apenas itens dinâmicos sofrem variação imediata
            boolean isFixed = false;
            if (plugin.getShopManager() != null) {
                for (com.rootssky.market.engine.ShopManager.ShopCategory cat : plugin.getShopManager().getCategories().values()) {
                    if (cat.getItems().contains(item.getItemId()) && cat.isFixedPrice()) {
                        isFixed = true;
                        break;
                    }
                }
            }
            
            if (isFixed) continue;
            
            java.math.BigDecimal newPrice = item.getCurrentPrice().multiply(multiplier);
            
            // Limita pisos e tetos absoluto
            newPrice = newPrice.max(item.getFloorPrice()).min(item.getCeilingPrice());
            
            item.setCurrentPrice(newPrice);
            item.setLastUpdate(java.time.Instant.now());
            plugin.getMarketCache().updateItem(item.getItemId(), item);
            plugin.getDatabaseManager().savePrice(item);
        }
    }

    public MarketState getCurrentState() {
        return currentState;
    }
}
