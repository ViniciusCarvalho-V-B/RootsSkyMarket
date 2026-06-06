package com.rootssky.market.gui;

import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.inventory.InventoryHolder;

import com.rootssky.market.RootsSkyMarket;
import com.rootssky.market.engine.ShopManager.ShopCategory;
import com.rootssky.market.gui.ShopCategoryHolder;
import com.rootssky.market.gui.ShopCategoryGUI;
import com.rootssky.market.gui.ShopItemsHolder;
import com.rootssky.market.gui.ShopItemsGUI;
import com.rootssky.market.gui.ShopTransactionHolder;
import com.rootssky.market.gui.ShopTransactionGUI;

public class StockGUIListener implements Listener {

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) return;
        InventoryHolder holder = event.getInventory().getHolder();

        if (holder instanceof StockHolder stockHolder) {
            event.setCancelled(true);
            int slot = event.getSlot();

            if (slot == 49) {
                RootsSkyMarket plugin = RootsSkyMarket.getInstance();
                new MainMenuGUI(plugin).open(player);
            } else if (slot == 45) {
                if (stockHolder.getCurrentPage() > 0) {
                    RootsSkyMarket plugin = RootsSkyMarket.getInstance();
                    StockGUI gui = new StockGUI(plugin);
                    gui.open(player, stockHolder.getCurrentPage() - 1);
                }
            } else if (slot == 53) {
                com.rootssky.market.RootsSkyMarket plugin = com.rootssky.market.RootsSkyMarket.getInstance();
                int maxPages = (int) Math.ceil((double) plugin.getMarketCache().size() / 18);
                if (stockHolder.getCurrentPage() < maxPages - 1) {
                    StockGUI gui = new StockGUI(plugin);
                    gui.open(player, stockHolder.getCurrentPage() + 1);
                }
            } else if (event.getCurrentItem() != null && event.getCurrentItem().getType() != org.bukkit.Material.BLACK_STAINED_GLASS_PANE && event.getCurrentItem().getType() != org.bukkit.Material.AIR) {
                // Clicked an item, open StockActionGUI
                String itemId = event.getCurrentItem().getType().name();
                com.rootssky.market.RootsSkyMarket plugin = com.rootssky.market.RootsSkyMarket.getInstance();
                if (plugin.getMarketCache().getItem(itemId) != null) {
                    new StockActionGUI(plugin, player, itemId).open();
                }
            }
        } else if (event.getInventory().getHolder() instanceof StockActionHolder actionHolder) {
            event.setCancelled(true);
            int slot = event.getSlot();
            com.rootssky.market.RootsSkyMarket plugin = com.rootssky.market.RootsSkyMarket.getInstance();
            String itemId = actionHolder.getItemId();
            
            if (slot == 22) {
                new MainMenuGUI(plugin).open(player);
            } else if (slot == 11) { // Buy
                double price = plugin.getMarketCache().getItem(itemId).getCurrentPrice().doubleValue() * 10;
                if (plugin.getVaultBridge().withdraw(player, price)) {
                    plugin.getPlayerSharesManager().addShares(player.getUniqueId().toString(), itemId, 10);
                    
                    // Increase volume slightly to affect market
                    plugin.getMarketCache().incrementVolume(itemId, 10);
                    
                    player.sendMessage("§8[§6Bolsa§8] §aVocê comprou 10 ações de " + itemId + " por " + plugin.getVaultBridge().format(price));
                    new StockActionGUI(plugin, player, itemId).open(); // refresh
                } else {
                    player.sendMessage("§8[§6Bolsa§8] §cVocê não tem saldo suficiente.");
                }
            } else if (slot == 15) { // Sell
                if (plugin.getPlayerSharesManager().removeShares(player.getUniqueId().toString(), itemId, 10)) {
                    double price = plugin.getMarketCache().getItem(itemId).getCurrentPrice().doubleValue() * 10;
                    
                    double taxPercent = plugin.getConfig().getDouble("shares.tax_percent", 5.0);
                    
                    if (plugin.getMarketIndexManager().getCurrentState() == com.rootssky.market.engine.MarketIndexManager.MarketState.BULL) {
                        taxPercent = plugin.getConfig().getDouble("market_index.bull_tax_percent", 2.0);
                    } else if (plugin.getMarketIndexManager().getCurrentState() == com.rootssky.market.engine.MarketIndexManager.MarketState.BEAR) {
                        taxPercent = plugin.getConfig().getDouble("market_index.bear_tax_percent", 10.0);
                    }
                    
                    if (itemId.equals(plugin.getHotStockManager().getCurrentHotStockId())) {
                        taxPercent = 0.0;
                    }
                    
                    String exemptPerm = plugin.getConfig().getString("shares.vip_tax_exempt_permission", "rootssky.market.tax_exempt");
                    
                    double finalPrice = price;
                    double taxAmount = 0;
                    
                    if (!player.hasPermission(exemptPerm)) {
                        taxAmount = price * (taxPercent / 100.0);
                        finalPrice -= taxAmount;
                    }
                    
                    plugin.getVaultBridge().deposit(player, finalPrice);
                    
                    // Increase sell volume
                    plugin.getMarketCache().incrementVolume(itemId, -10);
                    
                    if (taxAmount > 0) {
                        player.sendMessage("§8[§6Bolsa§8] §aVocê vendeu 10 ações de " + itemId + " por " + plugin.getVaultBridge().format(finalPrice) + " §c(-" + taxPercent + "% taxa)");
                    } else {
                        player.sendMessage("§8[§6Bolsa§8] §aVocê vendeu 10 ações de " + itemId + " por " + plugin.getVaultBridge().format(finalPrice) + " §e(Isento de taxa VIP!)");
                    }
                    
                    new StockActionGUI(plugin, player, itemId).open(); // refresh
                } else {
                    player.sendMessage("§8[§6Bolsa§8] §cVocê não possui 10 ações deste item.");
                }
            }
        } else if (event.getInventory().getHolder() instanceof PortfolioHolder portHolder) {
            event.setCancelled(true);
            int slot = event.getSlot();
            com.rootssky.market.RootsSkyMarket plugin = com.rootssky.market.RootsSkyMarket.getInstance();
            if (slot == 45) { // Novo botão de Voltar
                new MainMenuGUI(plugin).open(player);
            } else if (event.getCurrentItem() != null && event.getCurrentItem().getType() != org.bukkit.Material.BLACK_STAINED_GLASS_PANE && event.getCurrentItem().getType() != org.bukkit.Material.AIR) {
                String itemId = event.getCurrentItem().getType().name();
                if (plugin.getMarketCache().getItem(itemId) != null) {
                    new StockActionGUI(plugin, player, itemId).open();
                }
            }
        } else if (event.getInventory().getHolder() instanceof ShopCategoryHolder) {
            event.setCancelled(true);
            if (event.getCurrentItem() != null && event.getCurrentItem().getType() != Material.AIR) {
                // Try to find clicked category
                for (ShopCategory cat : RootsSkyMarket.getInstance().getShopManager().getCategories().values()) {
                    if (cat.getSlot() == event.getSlot()) {
                        new ShopItemsGUI(RootsSkyMarket.getInstance(), cat).open(player);
                        break;
                    }
                }
            }
        } else if (event.getInventory().getHolder() instanceof ShopItemsHolder itemsHolder) {
            event.setCancelled(true);
            if (event.getSlot() == 48) {
                if (!itemsHolder.isHideBackButton()) {
                    new ShopCategoryGUI(RootsSkyMarket.getInstance()).open(player);
                }
            } else if (event.getSlot() == 45 && event.getCurrentItem() != null && event.getCurrentItem().getType() == Material.PAPER) {
                new ShopItemsGUI(RootsSkyMarket.getInstance(), itemsHolder.getCategory(), itemsHolder.getPage() - 1, itemsHolder.isHideBackButton()).open(player);
            } else if (event.getSlot() == 53 && event.getCurrentItem() != null && event.getCurrentItem().getType() == Material.PAPER) {
                new ShopItemsGUI(RootsSkyMarket.getInstance(), itemsHolder.getCategory(), itemsHolder.getPage() + 1, itemsHolder.isHideBackButton()).open(player);
            } else if (event.getSlot() == 49) {
                // Balance button, do nothing
            } else if (event.getCurrentItem() != null && event.getCurrentItem().getType() != Material.AIR && event.getCurrentItem().getType() != Material.BLACK_STAINED_GLASS_PANE) {
                String itemId = event.getCurrentItem().getType().name();
                if (itemsHolder.getCategory().getItems().contains(itemId)) {
                    if (event.isShiftClick()) {
                        com.rootssky.market.model.MarketItem marketItem = RootsSkyMarket.getInstance().getMarketCache().getItem(itemId);
                        double unitPrice = 0.0;
                        if (marketItem != null) {
                            unitPrice = itemsHolder.getCategory().isFixedPrice() ? marketItem.getBasePrice().doubleValue() : marketItem.getCurrentPrice().doubleValue();
                        }
                        RootsSkyMarket.getInstance().getTransactionManager().processSellAll(player, itemId, unitPrice);
                    } else {
                        new ShopTransactionGUI(RootsSkyMarket.getInstance(), itemsHolder.getCategory(), itemId, itemsHolder.isHideBackButton()).open(player);
                    }
                }
            }
        } else if (event.getInventory().getHolder() instanceof ShopTransactionHolder transactionHolder) {
            event.setCancelled(true);
            
            if (event.getSlot() == 40) { // Voltar
                new ShopItemsGUI(RootsSkyMarket.getInstance(), transactionHolder.getCategory(), 1, transactionHolder.isHideBackButton()).open(player);
                return;
            }

            if (event.getCurrentItem() == null || event.getCurrentItem().getType() == Material.AIR || event.getCurrentItem().getType() == Material.BLACK_STAINED_GLASS_PANE) {
                return;
            }

            String actionName = net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer.plainText().serialize(event.getCurrentItem().getItemMeta().displayName());
            double unitPrice = 0.0;
            com.rootssky.market.model.MarketItem marketItem = RootsSkyMarket.getInstance().getMarketCache().getItem(transactionHolder.getItemId());
            if (marketItem != null) {
                unitPrice = transactionHolder.getCategory().isFixedPrice() ? marketItem.getBasePrice().doubleValue() : marketItem.getCurrentPrice().doubleValue();
            }

            if (actionName.contains("Comprar Quantidade Personalizada")) {
                com.rootssky.market.gui.CustomAmountListener.addPending(player, new com.rootssky.market.gui.CustomAmountData(transactionHolder.getCategory(), transactionHolder.getItemId(), true, unitPrice));
            } else if (actionName.contains("Vender Quantidade Personalizada")) {
                com.rootssky.market.gui.CustomAmountListener.addPending(player, new com.rootssky.market.gui.CustomAmountData(transactionHolder.getCategory(), transactionHolder.getItemId(), false, unitPrice));
            } else if (actionName.contains("Comprar")) {
                int amount = event.getCurrentItem().getAmount();
                RootsSkyMarket.getInstance().getTransactionManager().processBuy(player, transactionHolder.getItemId(), amount, unitPrice);
            } else if (actionName.contains("Vender Tudo")) {
                RootsSkyMarket.getInstance().getTransactionManager().processSellAll(player, transactionHolder.getItemId(), unitPrice);
            } else if (actionName.contains("Vender")) {
                int amount = event.getCurrentItem().getAmount();
                RootsSkyMarket.getInstance().getTransactionManager().processSell(player, transactionHolder.getItemId(), amount, unitPrice);
            }
        }
    }

    @EventHandler
    public void onInventoryDrag(InventoryDragEvent event) {
        if (event.getInventory().getHolder() instanceof StockHolder || 
            event.getInventory().getHolder() instanceof StockActionHolder ||
            event.getInventory().getHolder() instanceof PortfolioHolder ||
            event.getInventory().getHolder() instanceof ShopCategoryHolder ||
            event.getInventory().getHolder() instanceof ShopItemsHolder ||
            event.getInventory().getHolder() instanceof ShopTransactionHolder) {
            event.setCancelled(true);
        }
    }
}