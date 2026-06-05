package com.rootssky.market.managers;

import com.rootssky.market.RootsSkyMarket;
import com.rootssky.market.cache.MarketCache;
import com.rootssky.market.model.MarketItem;
import com.rootssky.market.model.TransactionData;
import com.rootssky.market.model.TransactionType;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.logging.Level;
import java.util.logging.Logger;

public class TransactionManager {

    private final RootsSkyMarket plugin;
    private final DatabaseManager dbManager;
    private final MarketCache cache;
    private final Logger logger;
    private final ExecutorService executor;

    public TransactionManager(RootsSkyMarket plugin, DatabaseManager dbManager, MarketCache cache) {
        this.plugin = plugin;
        this.dbManager = dbManager;
        this.cache = cache;
        this.logger = plugin.getLogger();
        this.executor = Executors.newFixedThreadPool(2, r -> {
            Thread t = new Thread(r, "RootsSkyMarket-Transaction");
            t.setDaemon(true);
            return t;
        });
    }

    public void recordTransaction(String playerUuidStr, String playerName, String itemId,
                                   TransactionType type, int amount, BigDecimal unitPrice,
                                   BigDecimal totalPrice, BigDecimal taxApplied) {
        if (itemId == null || itemId.isEmpty()) {
            logger.warning("[Transaction] Null or empty itemId received from hook");
            return;
        }

        if (amount <= 0) {
            logger.warning("[Transaction] Invalid amount: " + amount + " for item " + itemId);
            return;
        }

        UUID uuid;
        try {
            uuid = UUID.fromString(playerUuidStr);
        } catch (IllegalArgumentException e) {
            logger.warning("[Transaction] Invalid UUID: " + playerUuidStr);
            return;
        }

        UUID finalUuid = uuid;
        String finalPlayerName = playerName != null ? playerName : "Unknown";
        String finalItemId = itemId;

        CompletableFuture.runAsync(() -> {
            MarketItem item = cache.getItem(finalItemId);
            if (item == null) {
                logger.fine("[Transaction] Item not tracked by market engine: " + finalItemId);
                return;
            }

            try {
                TransactionData data = new TransactionData(
                        finalUuid,
                        finalPlayerName,
                        finalItemId,
                        type,
                        amount,
                        unitPrice != null ? unitPrice : BigDecimal.ZERO,
                        totalPrice != null ? totalPrice : BigDecimal.ZERO,
                        taxApplied != null ? taxApplied : BigDecimal.ZERO,
                        Instant.now()
                );

                dbManager.logTransaction(data).join();

                int volumeDelta = type == TransactionType.BUY ? amount : -amount;
                cache.incrementVolume(finalItemId, volumeDelta);

                item.setLastUpdate(Instant.now());
                cache.updateItem(finalItemId, item);
                dbManager.savePrice(item).join();

                logger.fine("[Transaction] Recorded " + type + " " + amount + "x " + finalItemId
                        + " by " + finalPlayerName + " | volume=" + cache.getItem(finalItemId).getVolume24h());

            } catch (Exception e) {
                logger.log(Level.SEVERE, "[Transaction] Failed to record transaction for " + finalItemId, e);
            }
        }, executor);
    }

    public void shutdown() {
        executor.shutdownNow();
    }

    public void processBuy(org.bukkit.entity.Player player, String itemId, int amount, double unitPrice) {
        double totalCost = unitPrice * amount;

        if (!plugin.getVaultBridge().hasBalance(player, totalCost)) {
            player.sendMessage("§cVocê não tem " + plugin.getVaultBridge().format(totalCost) + " para comprar " + amount + "x " + itemId);
            return;
        }

        org.bukkit.Material mat;
        try {
            mat = org.bukkit.Material.valueOf(itemId);
        } catch (IllegalArgumentException e) {
            player.sendMessage("§cItem inválido.");
            return;
        }

        if (!hasInventorySpace(player, mat, amount)) {
            player.sendMessage("§cVocê não tem espaço no inventário para isso.");
            return;
        }

        if (plugin.getVaultBridge().withdraw(player, totalCost)) {
            player.getInventory().addItem(new org.bukkit.inventory.ItemStack(mat, amount));
            player.sendMessage("§aVocê comprou " + amount + "x " + itemId + " por " + plugin.getVaultBridge().format(totalCost));
            
            String soundName = plugin.getConfig().getString("sounds.success", "ENTITY_EXPERIENCE_ORB_PICKUP");
            try { player.playSound(player.getLocation(), org.bukkit.Sound.valueOf(soundName), 1.0f, 1.0f); } catch (Exception ignored) {}

            recordTransaction(player.getUniqueId().toString(), player.getName(), itemId,
                    TransactionType.BUY, amount, BigDecimal.valueOf(unitPrice),
                    BigDecimal.valueOf(totalCost), BigDecimal.ZERO);
        }
    }

    public void processSell(org.bukkit.entity.Player player, String itemId, int amount, double unitPrice) {
        org.bukkit.Material mat;
        try {
            mat = org.bukkit.Material.valueOf(itemId);
        } catch (IllegalArgumentException e) {
            player.sendMessage("§cItem inválido.");
            return;
        }
        
        int count = countItems(player, mat);
        if (count < amount) {
            player.sendMessage("§cVocê só tem " + count + "x " + itemId + " no inventário.");
            return;
        }

        removeItems(player, mat, amount);

        double totalEarned = unitPrice * amount * 0.95; // 5% spread like EconomyShopGUI
        plugin.getVaultBridge().deposit(player, totalEarned);
        player.sendMessage("§aVocê vendeu " + amount + "x " + itemId + " por " + plugin.getVaultBridge().format(totalEarned));

        String soundName = plugin.getConfig().getString("sounds.success", "ENTITY_EXPERIENCE_ORB_PICKUP");
        try { player.playSound(player.getLocation(), org.bukkit.Sound.valueOf(soundName), 1.0f, 1.0f); } catch (Exception ignored) {}

        recordTransaction(player.getUniqueId().toString(), player.getName(), itemId,
                TransactionType.SELL, amount, BigDecimal.valueOf(unitPrice),
                BigDecimal.valueOf(totalEarned), BigDecimal.valueOf(unitPrice * amount * 0.05));
    }

    public void processSellAll(org.bukkit.entity.Player player, String itemId, double unitPrice) {
        org.bukkit.Material mat;
        try {
            mat = org.bukkit.Material.valueOf(itemId);
        } catch (IllegalArgumentException e) {
            player.sendMessage("§cItem inválido.");
            return;
        }
        
        int count = countItems(player, mat);
        if (count <= 0) {
            player.sendMessage("§cVocê não tem " + itemId + " no inventário.");
            return;
        }
        
        processSell(player, itemId, count, unitPrice);
    }
    
    public void processGlobalSellAll(org.bukkit.entity.Player player) {
        double totalEarnedGlobal = 0.0;
        int itemsSold = 0;
        
        for (com.rootssky.market.engine.ShopManager.ShopCategory cat : plugin.getShopManager().getCategories().values()) {
            for (String itemId : cat.getItems()) {
                org.bukkit.Material mat;
                try {
                    mat = org.bukkit.Material.valueOf(itemId);
                } catch (IllegalArgumentException e) { continue; }
                
                int count = countItems(player, mat);
                if (count > 0) {
                    com.rootssky.market.model.MarketItem mItem = cache.getItem(itemId);
                    if (mItem != null) {
                        double unitPrice = cat.isFixedPrice() ? mItem.getBasePrice().doubleValue() : mItem.getCurrentPrice().doubleValue();
                        
                        removeItems(player, mat, count);
                        double earned = unitPrice * count * 0.95;
                        totalEarnedGlobal += earned;
                        itemsSold += count;
                        
                        recordTransaction(player.getUniqueId().toString(), player.getName(), itemId,
                                TransactionType.SELL, count, BigDecimal.valueOf(unitPrice),
                                BigDecimal.valueOf(earned), BigDecimal.valueOf(unitPrice * count * 0.05));
                    }
                }
            }
        }
        
        if (itemsSold > 0) {
            plugin.getVaultBridge().deposit(player, totalEarnedGlobal);
            player.sendMessage("§aVocê vendeu " + itemsSold + " itens por " + plugin.getVaultBridge().format(totalEarnedGlobal));
            String soundName = plugin.getConfig().getString("sounds.success", "ENTITY_EXPERIENCE_ORB_PICKUP");
            try { player.playSound(player.getLocation(), org.bukkit.Sound.valueOf(soundName), 1.0f, 1.0f); } catch (Exception ignored) {}
        } else {
            player.sendMessage("§cVocê não possui itens válidos para venda no inventário.");
        }
    }
    
    public void processSellHand(org.bukkit.entity.Player player) {
        org.bukkit.inventory.ItemStack hand = player.getInventory().getItemInMainHand();
        if (hand == null || hand.getType() == org.bukkit.Material.AIR) {
            player.sendMessage("§cVocê não está segurando nenhum item.");
            return;
        }
        
        String itemId = hand.getType().name();
        com.rootssky.market.engine.ShopManager.ShopCategory category = null;
        for (com.rootssky.market.engine.ShopManager.ShopCategory cat : plugin.getShopManager().getCategories().values()) {
            if (cat.getItems().contains(itemId)) {
                category = cat;
                break;
            }
        }
        
        if (category == null) {
            player.sendMessage("§cEste item não pode ser vendido na loja.");
            return;
        }
        
        com.rootssky.market.model.MarketItem mItem = cache.getItem(itemId);
        if (mItem != null) {
            double unitPrice = category.isFixedPrice() ? mItem.getBasePrice().doubleValue() : mItem.getCurrentPrice().doubleValue();
            processSellAll(player, itemId, unitPrice);
        }
    }

    private boolean hasInventorySpace(org.bukkit.entity.Player player, org.bukkit.Material material, int amount) {
        int freeSpace = 0;
        int maxStack = material.getMaxStackSize();
        for (org.bukkit.inventory.ItemStack item : player.getInventory().getStorageContents()) {
            if (item == null || item.getType() == org.bukkit.Material.AIR) {
                freeSpace += maxStack;
            } else if (item.getType() == material) {
                freeSpace += (maxStack - item.getAmount());
            }
        }
        return freeSpace >= amount;
    }

    private int countItems(org.bukkit.entity.Player player, org.bukkit.Material material) {
        int count = 0;
        for (org.bukkit.inventory.ItemStack item : player.getInventory().getStorageContents()) {
            if (item != null && item.getType() == material) {
                count += item.getAmount();
            }
        }
        return count;
    }

    private void removeItems(org.bukkit.entity.Player player, org.bukkit.Material material, int amount) {
        int remaining = amount;
        org.bukkit.inventory.ItemStack[] contents = player.getInventory().getStorageContents();
        for (int i = 0; i < contents.length; i++) {
            org.bukkit.inventory.ItemStack item = contents[i];
            if (item != null && item.getType() == material) {
                if (item.getAmount() <= remaining) {
                    remaining -= item.getAmount();
                    player.getInventory().setItem(i, null);
                } else {
                    item.setAmount(item.getAmount() - remaining);
                    remaining = 0;
                }
            }
            if (remaining <= 0) break;
        }
    }
}