package com.rootssky.market.engine;

import com.rootssky.market.RootsSkyMarket;
import com.rootssky.market.model.MarketItem;
import org.bukkit.Bukkit;
import org.bukkit.scheduler.BukkitRunnable;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

public class EventEngine extends BukkitRunnable {

    private final RootsSkyMarket plugin;
    private final Random random = new Random();

    public EventEngine(RootsSkyMarket plugin) {
        this.plugin = plugin;
    }

    public void start() {
        if (!plugin.getConfig().getBoolean("market_events.enabled", true)) {
            return;
        }
        int intervalMinutes = plugin.getConfig().getInt("market_events.interval_minutes", 60);
        this.runTaskTimer(plugin, 20L * 60 * intervalMinutes, 20L * 60 * intervalMinutes);
    }

    @Override
    public void run() {
        if (!plugin.getConfig().getBoolean("market_events.enabled", true)) {
            return;
        }

        int chance = plugin.getConfig().getInt("market_events.chance_percent", 50);
        if (random.nextInt(100) >= chance) {
            return; // No event this time
        }

        List<MarketItem> items = new ArrayList<>(plugin.getMarketCache().getAllItems());
        if (items.isEmpty()) return;

        Collections.shuffle(items, random);
        int numItems = random.nextInt(3) + 1; // 1 to 3 items affected

        double minIntensity = plugin.getConfig().getDouble("market_events.intensity_min", 0.10);
        double maxIntensity = plugin.getConfig().getDouble("market_events.intensity_max", 0.30);

        boolean isBoom = random.nextBoolean();
        double intensity = minIntensity + (maxIntensity - minIntensity) * random.nextDouble();

        List<String> affectedItemIds = new ArrayList<>();

        for (int i = 0; i < numItems && i < items.size(); i++) {
            MarketItem item = items.get(i);
            BigDecimal currentPrice = item.getCurrentPrice();

            BigDecimal multiplier;
            if (isBoom) {
                multiplier = BigDecimal.valueOf(1.0 + intensity);
            } else {
                multiplier = BigDecimal.valueOf(1.0 - intensity);
            }

            BigDecimal newPrice = currentPrice.multiply(multiplier).setScale(4, RoundingMode.HALF_UP);
            
            // Check floor/ceiling
            if (newPrice.compareTo(item.getFloorPrice()) < 0) newPrice = item.getFloorPrice();
            if (newPrice.compareTo(item.getCeilingPrice()) > 0) newPrice = item.getCeilingPrice();

            plugin.getMarketCache().updatePrice(item.getItemId(), newPrice);
            plugin.getDatabaseManager().savePrice(item);
            
            affectedItemIds.add(item.getItemId());
        }

        String itemsStr = String.join(", ", affectedItemIds);
        String percentageStr = String.format("%.0f%%", intensity * 100);

        if (isBoom) {
            Bukkit.broadcastMessage("§8[§6Bolsa§8] §e§l📰 Notícia Urgente: §aAlta demanda repentina! §fO preço de §b" + itemsStr + " §fsubiu em §a" + percentageStr + "§f!");
        } else {
            Bukkit.broadcastMessage("§8[§6Bolsa§8] §e§l📰 Notícia Urgente: §cExcesso de estoque! §fO preço de §b" + itemsStr + " §fcaiu em §c" + percentageStr + "§f!");
        }
    }
}
