package com.rootssky.market.cache;

import com.rootssky.market.managers.DatabaseManager;
import com.rootssky.market.model.MarketItem;
import org.bukkit.Bukkit;

import java.math.BigDecimal;
import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Logger;

public class MarketCache {

    private final ConcurrentHashMap<String, MarketItem> cache = new ConcurrentHashMap<>();
    private final Logger logger;

    public MarketCache() {
        this.logger = Bukkit.getLogger();
    }

    public java.util.concurrent.CompletableFuture<Void> loadFromDatabase(DatabaseManager db) {
        cache.clear();
        return db.loadAllPrices().thenAccept(items -> {
            cache.putAll(items);
            logger.info("[Market] Cache loaded com " + items.size() + " itens.");
        }).exceptionally(ex -> {
            logger.severe("[Market] Falha ao carregar cache do banco: " + ex.getMessage());
            return null;
        });
    }

    public BigDecimal getPrice(String itemId) {
        MarketItem item = cache.get(itemId);
        if (item == null) {
            logger.warning("[Market] Item not found in cache: " + itemId);
            return BigDecimal.ZERO;
        }
        return item.getCurrentPrice();
    }

    public MarketItem getItem(String itemId) {
        return cache.get(itemId);
    }

    public void updatePrice(String itemId, BigDecimal newPrice) {
        cache.computeIfPresent(itemId, (key, item) -> {
            item.setCurrentPrice(newPrice);
            return item;
        });
    }

    public void updateItem(String itemId, MarketItem updatedItem) {
        cache.put(itemId, updatedItem);
    }

    public void incrementVolume(String itemId, int amount) {
        cache.computeIfPresent(itemId, (key, item) -> {
            item.setVolume24h(item.getVolume24h() + amount);
            return item;
        });
    }

    public void resetVolume(String itemId) {
        cache.computeIfPresent(itemId, (key, item) -> {
            item.setVolume24h(0);
            return item;
        });
    }

    public Collection<MarketItem> getAllItems() {
        return Collections.unmodifiableCollection(cache.values());
    }

    public Map<String, MarketItem> getAllItemsMap() {
        return Collections.unmodifiableMap(cache);
    }

    public boolean containsItem(String itemId) {
        return cache.containsKey(itemId);
    }

    public int size() {
        return cache.size();
    }

    public void clear() {
        cache.clear();
        logger.info("[Market] Cache cleared.");
    }
}
