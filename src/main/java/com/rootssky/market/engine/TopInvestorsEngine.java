package com.rootssky.market.engine;

import com.rootssky.market.RootsSkyMarket;
import com.rootssky.market.model.MarketItem;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

public class TopInvestorsEngine extends BukkitRunnable {

    private final RootsSkyMarket plugin;

    public TopInvestorsEngine(RootsSkyMarket plugin) {
        this.plugin = plugin;
    }

    public void start() {
        if (!plugin.getConfig().getBoolean("top_investors.enabled", true)) return;
        
        long ticks = 24 * 60 * 60 * 20L; // Roda 1 vez por dia
        
        this.runTaskTimerAsynchronously(plugin, ticks, ticks);
    }

    @Override
    public void run() {
        plugin.getDatabaseManager().getAllSharesGlobal().thenAccept(allShares -> {
            Map<String, Double> wealth = new HashMap<>();
            
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
            
            List<Map.Entry<String, Double>> sorted = wealth.entrySet().stream()
                    .sorted((a, b) -> Double.compare(b.getValue(), a.getValue()))
                    .limit(3)
                    .collect(Collectors.toList());
                    
            if (sorted.isEmpty()) return;

            plugin.getServer().getScheduler().runTask(plugin, () -> {
                String top1Cmd = plugin.getConfig().getString("top_investors.tag_commands.top1", "lp user %player% parent addtemp lobo_wallstreet 1d");
                String top2Cmd = plugin.getConfig().getString("top_investors.tag_commands.top2", "lp user %player% parent addtemp magnata 1d");
                String top3Cmd = plugin.getConfig().getString("top_investors.tag_commands.top3", "lp user %player% parent addtemp especulador 1d");

                applyTag(sorted, 0, top1Cmd);
                applyTag(sorted, 1, top2Cmd);
                applyTag(sorted, 2, top3Cmd);
                
                Bukkit.getLogger().info("[RootsSkyMarket] Tags de Top Investidores atribuídas com sucesso.");
            });
        });
    }
    
    private void applyTag(List<Map.Entry<String, Double>> sorted, int index, String cmdTemplate) {
        if (sorted.size() > index && cmdTemplate != null && !cmdTemplate.isEmpty()) {
            OfflinePlayer op = Bukkit.getOfflinePlayer(UUID.fromString(sorted.get(index).getKey()));
            String name = op.getName();
            if (name != null) {
                String cmd = cmdTemplate.replace("%player%", name);
                Bukkit.dispatchCommand(Bukkit.getConsoleSender(), cmd);
            }
        }
    }
}
