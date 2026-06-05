package com.rootssky.market.gui;

import com.rootssky.market.RootsSkyMarket;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.event.player.PlayerQuitEvent;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class CustomAmountListener implements Listener {

    private static final Map<UUID, CustomAmountData> pendingAmounts = new ConcurrentHashMap<>();

    public static void addPending(Player player, CustomAmountData data) {
        pendingAmounts.put(player.getUniqueId(), data);
        player.closeInventory();
        player.sendMessage(" ");
        player.sendMessage("§aDigite no chat a quantidade que você deseja " + (data.isBuy() ? "COMPRAR" : "VENDER") + ".");
        player.sendMessage("§e(Ou digite 'cancelar' para abortar)");
        player.sendMessage(" ");
    }

    @EventHandler
    public void onChat(AsyncPlayerChatEvent event) {
        Player player = event.getPlayer();
        UUID uuid = player.getUniqueId();

        if (pendingAmounts.containsKey(uuid)) {
            event.setCancelled(true);
            CustomAmountData data = pendingAmounts.remove(uuid);
            String message = event.getMessage().trim();

            if (message.equalsIgnoreCase("cancelar")) {
                player.sendMessage("§cOperação cancelada.");
                Bukkit.getScheduler().runTask(RootsSkyMarket.getInstance(), () -> {
                    new ShopTransactionGUI(RootsSkyMarket.getInstance(), data.getCategory(), data.getItemId()).open(player);
                });
                return;
            }

            int amount;
            try {
                amount = Integer.parseInt(message);
                if (amount <= 0) throw new NumberFormatException();
            } catch (NumberFormatException e) {
                player.sendMessage("§cQuantidade inválida. Operação cancelada.");
                return;
            }

            // Execute transaction synchronously
            Bukkit.getScheduler().runTask(RootsSkyMarket.getInstance(), () -> {
                if (data.isBuy()) {
                    RootsSkyMarket.getInstance().getTransactionManager().processBuy(player, data.getItemId(), amount, data.getUnitPrice());
                } else {
                    RootsSkyMarket.getInstance().getTransactionManager().processSell(player, data.getItemId(), amount, data.getUnitPrice());
                }
            });
        }
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        pendingAmounts.remove(event.getPlayer().getUniqueId());
    }
}
