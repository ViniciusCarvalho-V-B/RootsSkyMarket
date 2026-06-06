package com.rootssky.market.engine;

import com.rootssky.market.RootsSkyMarket;
import com.rootssky.market.cache.MarketCache;
import com.rootssky.market.managers.DatabaseManager;
import com.rootssky.market.model.MarketItem;
import org.bukkit.Bukkit;
import org.bukkit.scheduler.BukkitRunnable;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.time.Instant;
import java.util.logging.Level;
import java.util.logging.Logger;

public class EconomyEngine {

    private final RootsSkyMarket plugin;
    private final DatabaseManager dbManager;
    private final MarketCache cache;
    private final Logger logger;
    private final double decayRatePerHour;
    private final int updateIntervalMinutes;

    public EconomyEngine(RootsSkyMarket plugin, DatabaseManager dbManager, MarketCache cache) {
        this.plugin = plugin;
        this.dbManager = dbManager;
        this.cache = cache;
        this.logger = plugin.getLogger();

        var config = plugin.getConfig();
        this.decayRatePerHour = config.getDouble("economy.decay_rate_per_hour", 0.05);
        this.updateIntervalMinutes = config.getInt("economy.price_update_interval_minutes", 5);
    }

    public BigDecimal calculateNewPrice(MarketItem item) {
        // Se o item pertence a uma categoria de preço fixo, o preço não flutua (sempre preço base)
        if (plugin.getShopManager() != null) {
            String itemId = item.getItemId();
            for (com.rootssky.market.engine.ShopManager.ShopCategory cat : plugin.getShopManager().getCategories().values()) {
                if (cat.getItems().contains(itemId) && cat.isFixedPrice()) {
                    return item.getBasePrice();
                }
            }
        }

        int volume = item.getVolume24h();
        if (volume == 0) {
            return applyDecay(item);
        }

        double volumeDivisor = plugin.getConfig().getDouble("economy.volume_scale_divisor", 3.0);
        double maxScale = plugin.getConfig().getDouble("economy.max_volume_scale", 3.0);

        // Curva agressiva: em vez de log10 (muito fraco), usamos raiz quadrada (pow 0.5)
        // Isso faz com que pequenas transações já tenham um impacto mais perceptível.
        double scale = Math.pow(Math.abs(volume), 0.5) / volumeDivisor;
        double delta = Math.signum(volume) * Math.min(scale, maxScale);

        BigDecimal alpha = item.getAlpha(); // Sensibilidade do item (default 0.15 a 0.35)
        
        // Multiplicador extra configurável na config geral para dar um "boost" na volatilidade
        double volatilityBoost = plugin.getConfig().getDouble("economy.volatility_boost", 2.0);
        
        BigDecimal adjustment = BigDecimal.ONE.add(alpha.multiply(BigDecimal.valueOf(delta * volatilityBoost)));
        
        BigDecimal currentPrice = item.getCurrentPrice();
        BigDecimal newPrice = currentPrice.multiply(adjustment);

        newPrice = applyDecayFactor(newPrice, item);

        newPrice = newPrice.max(item.getFloorPrice()).min(item.getCeilingPrice());

        return newPrice.setScale(2, RoundingMode.HALF_UP);
    }

    public BigDecimal getEffectiveBasePrice(MarketItem item) {
        BigDecimal base = item.getBasePrice();
        if (plugin.getMarketIndexManager() != null) {
            var state = plugin.getMarketIndexManager().getCurrentState();
            if (state == MarketIndexManager.MarketState.BULL) {
                return base.multiply(BigDecimal.valueOf(1.20)); // Base atrai +20% para cima no Bull
            } else if (state == MarketIndexManager.MarketState.BEAR) {
                return base.multiply(BigDecimal.valueOf(0.80)); // Base atrai -20% para baixo no Bear
            }
        }
        return base;
    }

    private BigDecimal applyDecay(MarketItem item) {
        return applyDecayFactor(item.getCurrentPrice(), item);
    }

    private BigDecimal applyDecayFactor(BigDecimal price, MarketItem item) {
        Duration elapsed = Duration.between(item.getLastUpdate(), Instant.now());
        double hoursElapsed = elapsed.toMillis() / 3600000.0;

        if (hoursElapsed <= 0) return price;

        double decayFactor = Math.exp(-decayRatePerHour * hoursElapsed);
        
        BigDecimal basePrice = getEffectiveBasePrice(item);
        BigDecimal diff = price.subtract(basePrice);
        BigDecimal newDiff = diff.multiply(BigDecimal.valueOf(decayFactor));
        return basePrice.add(newDiff);
    }

    public void startPriceUpdateTask() {
        long ticks = updateIntervalMinutes * 60L * 20L;

        new BukkitRunnable() {
            @Override
            public void run() {
                updateAllPrices();
            }
        }.runTaskTimerAsynchronously(plugin, ticks, ticks);

        logger.info("[EconomyEngine] Price update task started. Interval: " + updateIntervalMinutes + " minutes.");
    }

    private void updateAllPrices() {
        if (!dbManager.isConnected()) {
            logger.warning("[EconomyEngine] Database not connected. Skipping price update.");
            return;
        }

        StringBuilder log = new StringBuilder("[Market] Prices updated. ");
        int updated = 0;

        for (MarketItem item : cache.getAllItems()) {
            BigDecimal oldPrice = item.getCurrentPrice();
            BigDecimal newPrice = calculateNewPrice(item);

            if (oldPrice.compareTo(newPrice) != 0) {
                item.setCurrentPrice(newPrice);
                item.setLastUpdate(Instant.now());
                cache.updateItem(item.getItemId(), item);
                dbManager.savePrice(item);

                log.append(item.getItemId()).append(": ")
                        .append(oldPrice.setScale(2, RoundingMode.HALF_UP)).append(" -> ")
                        .append(newPrice.setScale(2, RoundingMode.HALF_UP)).append(" | ");
                updated++;
            }

            cache.resetVolume(item.getItemId());
        }

        if (updated > 0) {
            logger.info(log.toString());
        }
    }

    public double getDecayRatePerHour() {
        return decayRatePerHour;
    }

    public int getUpdateIntervalMinutes() {
        return updateIntervalMinutes;
    }
}