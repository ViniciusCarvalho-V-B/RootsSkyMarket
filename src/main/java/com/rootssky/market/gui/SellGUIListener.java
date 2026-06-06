package com.rootssky.market.gui;

import com.rootssky.market.RootsSkyMarket;
import com.rootssky.market.engine.ShopManager;
import com.rootssky.market.model.MarketItem;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;

public class SellGUIListener implements Listener {

    private final RootsSkyMarket plugin;

    public SellGUIListener(RootsSkyMarket plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onClose(InventoryCloseEvent event) {
        Inventory inv = event.getInventory();
        if (!(inv.getHolder() instanceof SellGUIHolder)) {
            return;
        }

        Player player = (Player) event.getPlayer();
        double totalEarned = 0.0;
        double totalBonusEarned = 0.0;
        int itemsSold = 0;
        Map<ItemStack, Integer> toDrop = new HashMap<>();

        double vipBonusPct = plugin.getTransactionManager().getVipBonusPercentage(player);

        for (int i = 0; i < inv.getSize(); i++) {
            ItemStack item = inv.getItem(i);
            if (item == null || item.getType() == Material.AIR) continue;

            String itemId = item.getType().name();
            ShopManager.ShopCategory category = null;

            for (ShopManager.ShopCategory cat : plugin.getShopManager().getCategories().values()) {
                if (cat.getItems().contains(itemId)) {
                    category = cat;
                    break;
                }
            }

            if (category == null) {
                toDrop.put(item, i);
                inv.setItem(i, null);
                continue;
            }

            MarketItem mItem = plugin.getMarketCache().getItem(itemId);
            if (mItem != null) {
                double unitPrice = category.isFixedPrice() ? mItem.getBasePrice().doubleValue() : mItem.getCurrentPrice().doubleValue();
                int amount = item.getAmount();

                int limit = plugin.getTransactionManager().getMaxSellLimit(player, itemId);
                int soldToday = plugin.getPlayerLimitManager().getSoldToday(player.getUniqueId().toString(), itemId);
                if (soldToday >= limit) {
                    // Limite atingindo para este item, devolve o item
                    toDrop.put(item, i);
                    inv.setItem(i, null);
                    continue;
                }

                int toSell = amount;
                if (soldToday + amount > limit) {
                    toSell = limit - soldToday;
                    // Devolve a diferença
                    ItemStack remainingItem = item.clone();
                    remainingItem.setAmount(amount - toSell);
                    toDrop.put(remainingItem, i);
                }
                
                double baseEarned = unitPrice * toSell * 0.95; // 5% spread
                double bonusAmount = baseEarned * (vipBonusPct / 100.0);
                double earned = baseEarned + bonusAmount;

                totalEarned += earned;
                totalBonusEarned += bonusAmount;
                itemsSold += toSell;

                plugin.getPlayerLimitManager().addSoldAmount(player.getUniqueId().toString(), itemId, toSell);
                
                plugin.getTransactionManager().recordTransaction(player.getUniqueId().toString(), player.getName(), itemId,
                        com.rootssky.market.model.TransactionType.SELL, toSell, BigDecimal.valueOf(unitPrice),
                        BigDecimal.valueOf(earned), BigDecimal.valueOf(unitPrice * toSell * 0.05));
                        
                inv.setItem(i, null); // remove sold item
            } else {
                toDrop.put(item, i);
                inv.setItem(i, null);
            }
        }

        if (itemsSold > 0) {
            plugin.getVaultBridge().deposit(player, totalEarned);
            if (totalBonusEarned > 0.0) {
                String bonusFormatted = plugin.getVaultBridge().format(totalBonusEarned);
                player.sendMessage("§aVocê vendeu §f" + itemsSold + "§a itens por §f" + plugin.getVaultBridge().format(totalEarned) + " §e(+" + bonusFormatted + " Bônus VIP de " + (int)vipBonusPct + "%)");
            } else {
                player.sendMessage("§aVocê vendeu §f" + itemsSold + "§a itens por §f" + plugin.getVaultBridge().format(totalEarned));
            }
            String soundName = plugin.getConfig().getString("sounds.success", "ENTITY_EXPERIENCE_ORB_PICKUP");
            try { player.playSound(player.getLocation(), org.bukkit.Sound.valueOf(soundName), 1.0f, 1.0f); } catch (Exception ignored) {}
        } else {
            player.sendMessage("§cVocê não vendeu nenhum item válido (ou atingiu o limite de vendas diário de todos os itens).");
        }

        // Drop/devolve invalid or limited items
        for (ItemStack dropItem : toDrop.keySet()) {
            player.getWorld().dropItemNaturally(player.getLocation(), dropItem);
            player.sendMessage("§cO item §f" + dropItem.getType().name() + " (" + dropItem.getAmount() + "x) §cnão pôde ser vendido (sem preço, não listado ou limite diário atingido) e foi devolvido.");
        }
    }
}
