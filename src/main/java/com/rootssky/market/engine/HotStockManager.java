package com.rootssky.market.engine;

import com.rootssky.market.RootsSkyMarket;
import com.rootssky.market.model.MarketItem;
import org.bukkit.Bukkit;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class HotStockManager extends BukkitRunnable {

    private final RootsSkyMarket plugin;
    private final Random random = new Random();
    private String currentHotStockId = null;

    public HotStockManager(RootsSkyMarket plugin) {
        this.plugin = plugin;
    }

    public void start() {
        if (!plugin.getConfig().getBoolean("hot_stock.enabled", true)) return;
        
        long intervalHours = plugin.getConfig().getLong("hot_stock.interval_hours", 24);
        long ticks = intervalHours * 60 * 60 * 20L;
        
        // Pick one immediately, then repeat
        pickHotStock();
        this.runTaskTimer(plugin, ticks, ticks);
    }

    @Override
    public void run() {
        pickHotStock();
    }

    public void pickHotStock(String forceItemId) {
        if (forceItemId != null) {
            currentHotStockId = forceItemId;
        } else {
            List<MarketItem> items = new ArrayList<>(plugin.getMarketCache().getAllItems());
            if (items.isEmpty()) return;
            
            MarketItem picked = items.get(random.nextInt(items.size()));
            currentHotStockId = picked.getItemId();
        }
        
        String msg1 = com.rootssky.market.utils.MessageUtils.get(plugin.getConfig(), "hot_stock_picked").replace("{item}", currentHotStockId);
        String msg2 = com.rootssky.market.utils.MessageUtils.get(plugin.getConfig(), "hot_stock_benefit").replace("{item}", currentHotStockId);
        
        Bukkit.broadcastMessage(msg1);
        Bukkit.broadcastMessage(msg2);

        // Enviar Webhook pro Discord
        com.rootssky.market.utils.DiscordWebhook.send(plugin, "🌟 **QUERIDINHO DO DIA!** O item **" + currentHotStockId + "** foi escolhido. Aproveite 0% de impostos nas vendas!");
    }

    private void pickHotStock() {
        pickHotStock(null);
    }

    public String getCurrentHotStockId() {
        return currentHotStockId;
    }
}
