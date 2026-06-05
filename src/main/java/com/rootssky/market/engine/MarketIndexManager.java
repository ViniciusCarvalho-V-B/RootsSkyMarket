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
    }

    public MarketState getCurrentState() {
        return currentState;
    }
}
