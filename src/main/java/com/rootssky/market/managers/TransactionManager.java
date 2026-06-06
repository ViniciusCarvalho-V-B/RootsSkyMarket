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

        // Validação do Limite Anti-Monopólio (por item individual)
        int limit = getMaxSellLimit(player, itemId);
        int soldToday = plugin.getPlayerLimitManager().getSoldToday(player.getUniqueId().toString(), itemId);
        if (soldToday >= limit) {
            player.sendMessage("§cVocê já atingiu o limite diário de vendas para " + itemId + " (" + limit + " un.).");
            return;
        }

        int allowedAmount = amount;
        if (soldToday + amount > limit) {
            allowedAmount = limit - soldToday;
            player.sendMessage("§eLimite diário próximo! Venda de " + itemId + " limitada a " + allowedAmount + " unidades.");
        }

        removeItems(player, mat, allowedAmount);

        // Cálculo do Bônus VIP
        double baseEarned = unitPrice * allowedAmount * 0.95; // 5% spread
        double vipBonusPct = getVipBonusPercentage(player);
        double bonusAmount = baseEarned * (vipBonusPct / 100.0);
        double totalEarned = baseEarned + bonusAmount;

        plugin.getVaultBridge().deposit(player, totalEarned);
        
        // Registrar venda no cache de limites
        plugin.getPlayerLimitManager().addSoldAmount(player.getUniqueId().toString(), itemId, allowedAmount);

        if (bonusAmount > 0.0) {
            String bonusFormatted = plugin.getVaultBridge().format(bonusAmount);
            player.sendMessage("§aVocê vendeu §f" + allowedAmount + "x " + itemId + "§a por §f" + plugin.getVaultBridge().format(totalEarned) + " §e(+" + bonusFormatted + " Bônus VIP de " + (int)vipBonusPct + "%)");
        } else {
            player.sendMessage("§aVocê vendeu §f" + allowedAmount + "x " + itemId + "§a por §f" + plugin.getVaultBridge().format(totalEarned));
        }

        String soundName = plugin.getConfig().getString("sounds.success", "ENTITY_EXPERIENCE_ORB_PICKUP");
        try { player.playSound(player.getLocation(), org.bukkit.Sound.valueOf(soundName), 1.0f, 1.0f); } catch (Exception ignored) {}

        recordTransaction(player.getUniqueId().toString(), player.getName(), itemId,
                TransactionType.SELL, allowedAmount, BigDecimal.valueOf(unitPrice),
                BigDecimal.valueOf(totalEarned), BigDecimal.valueOf(unitPrice * allowedAmount * 0.05));
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
        double totalBonusEarnedGlobal = 0.0;
        int itemsSold = 0;
        double vipBonusPct = getVipBonusPercentage(player);
        
        for (com.rootssky.market.engine.ShopManager.ShopCategory cat : plugin.getShopManager().getCategories().values()) {
            for (String itemId : cat.getItems()) {
                org.bukkit.Material mat;
                try {
                    mat = org.bukkit.Material.valueOf(itemId);
                } catch (IllegalArgumentException e) { continue; }
                
                int count = countItems(player, mat);
                if (count > 0) {
                    // Checagem de limite para o item individual
                    int limit = getMaxSellLimit(player, itemId);
                    int soldToday = plugin.getPlayerLimitManager().getSoldToday(player.getUniqueId().toString(), itemId);
                    if (soldToday >= limit) continue;

                    int toSell = count;
                    if (soldToday + count > limit) {
                        toSell = limit - soldToday;
                    }

                    com.rootssky.market.model.MarketItem mItem = cache.getItem(itemId);
                    if (mItem != null) {
                        double unitPrice = cat.isFixedPrice() ? mItem.getBasePrice().doubleValue() : mItem.getCurrentPrice().doubleValue();
                        
                        removeItems(player, mat, toSell);
                        
                        double baseEarned = unitPrice * toSell * 0.95;
                        double bonusAmount = baseEarned * (vipBonusPct / 100.0);
                        double earned = baseEarned + bonusAmount;
                        
                        totalEarnedGlobal += earned;
                        totalBonusEarnedGlobal += bonusAmount;
                        itemsSold += toSell;
                        
                        plugin.getPlayerLimitManager().addSoldAmount(player.getUniqueId().toString(), itemId, toSell);
                        
                        recordTransaction(player.getUniqueId().toString(), player.getName(), itemId,
                                TransactionType.SELL, toSell, BigDecimal.valueOf(unitPrice),
                                BigDecimal.valueOf(earned), BigDecimal.valueOf(unitPrice * toSell * 0.05));
                    }
                }
            }
        }
        
        if (itemsSold > 0) {
            plugin.getVaultBridge().deposit(player, totalEarnedGlobal);
            if (totalBonusEarnedGlobal > 0.0) {
                player.sendMessage("§aVocê vendeu §f" + itemsSold + "§a itens por §f" + plugin.getVaultBridge().format(totalEarnedGlobal) + " §e(+" + plugin.getVaultBridge().format(totalBonusEarnedGlobal) + " Bônus VIP de " + (int)vipBonusPct + "%)");
            } else {
                player.sendMessage("§aVocê vendeu §f" + itemsSold + "§a itens por §f" + plugin.getVaultBridge().format(totalEarnedGlobal));
            }
            String soundName = plugin.getConfig().getString("sounds.success", "ENTITY_EXPERIENCE_ORB_PICKUP");
            try { player.playSound(player.getLocation(), org.bukkit.Sound.valueOf(soundName), 1.0f, 1.0f); } catch (Exception ignored) {}
        } else {
            player.sendMessage("§cVocê não possui itens válidos para venda (ou atingiu o limite de todos os itens).");
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

    public double getVipBonusPercentage(org.bukkit.entity.Player player) {
        if (!plugin.getConfig().getBoolean("economy.vip_sell_bonus.enabled", true)) {
            return 0.0;
        }
        double highestBonus = 0.0;
        var ranksSection = plugin.getConfig().getConfigurationSection("economy.vip_sell_bonus.ranks");
        if (ranksSection != null) {
            for (String key : ranksSection.getKeys(false)) {
                String permission = ranksSection.getString(key + ".permission");
                double percentage = ranksSection.getDouble(key + ".percentage", 0.0);
                if (permission != null && player.hasPermission(permission)) {
                    if (percentage > highestBonus) {
                        highestBonus = percentage;
                    }
                }
            }
        }
        return highestBonus;
    }

    public String getVipRankName(org.bukkit.entity.Player player) {
        if (!plugin.getConfig().getBoolean("economy.vip_sell_bonus.enabled", true)) {
            return null;
        }
        double highestBonus = 0.0;
        String rankName = null;
        var ranksSection = plugin.getConfig().getConfigurationSection("economy.vip_sell_bonus.ranks");
        if (ranksSection != null) {
            for (String key : ranksSection.getKeys(false)) {
                String permission = ranksSection.getString(key + ".permission");
                double percentage = ranksSection.getDouble(key + ".percentage", 0.0);
                if (permission != null && player.hasPermission(permission)) {
                    if (percentage > highestBonus) {
                        highestBonus = percentage;
                        rankName = key;
                    }
                }
            }
        }
        return rankName;
    }

    public int getMaxSellLimit(org.bukkit.entity.Player player, String itemId) {
        if (!plugin.getConfig().getBoolean("anti_monopoly.enabled", true)) {
            return Integer.MAX_VALUE;
        }
        int defaultLimit = plugin.getConfig().getInt("anti_monopoly.max_sell_per_day", 10000);
        int baseLimit = -1;

        // 1. Tentar ler do limits.yml dedicado
        org.bukkit.configuration.file.FileConfiguration limitsCfg = plugin.getLimitsConfig();
        String limitsPath = "limits." + itemId.toUpperCase();
        if (limitsCfg.contains(limitsPath)) {
            boolean enabled = limitsCfg.getBoolean(limitsPath + ".enabled", true);
            if (enabled) {
                baseLimit = limitsCfg.getInt(limitsPath + ".limit", -1);
            }
        }

        // 2. Fallback para config.yml (item_limits ou limite padrão)
        if (baseLimit == -1) {
            baseLimit = plugin.getConfig().getInt("anti_monopoly.item_limits." + itemId.toUpperCase(), -1);
        }

        if (baseLimit == -1) {
            baseLimit = defaultLimit;
        }

        double multiplier = 1.0;
        var vipLimitsSection = plugin.getConfig().getConfigurationSection("anti_monopoly.vip_limits");
        if (vipLimitsSection != null && defaultLimit > 0) {
            for (String permission : vipLimitsSection.getKeys(false)) {
                if (player.hasPermission(permission)) {
                    int vipLimit = vipLimitsSection.getInt(permission);
                    double currentMultiplier = (double) vipLimit / defaultLimit;
                    if (currentMultiplier > multiplier) {
                        multiplier = currentMultiplier;
                    }
                }
            }
        }
        return (int) (baseLimit * multiplier);
    }
}