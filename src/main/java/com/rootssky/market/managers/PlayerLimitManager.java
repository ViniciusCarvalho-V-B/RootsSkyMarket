package com.rootssky.market.managers;

import com.rootssky.market.RootsSkyMarket;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class PlayerLimitManager implements Listener {

    private final RootsSkyMarket plugin;
    // UUID -> (ItemId -> AmountSoldToday)
    private final Map<String, Map<String, Integer>> playerLimits = new ConcurrentHashMap<>();

    public PlayerLimitManager(RootsSkyMarket plugin) {
        this.plugin = plugin;
    }

    public void initialize() {
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
        // Load for players already online (e.g. reload)
        for (Player p : plugin.getServer().getOnlinePlayers()) {
            loadPlayerLimits(p.getUniqueId().toString());
        }
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        loadPlayerLimits(event.getPlayer().getUniqueId().toString());
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        playerLimits.remove(event.getPlayer().getUniqueId().toString());
    }

    private void loadPlayerLimits(String uuid) {
        plugin.getDatabaseManager().getSoldAmountsToday(uuid).thenAccept(limits -> {
            playerLimits.put(uuid, new ConcurrentHashMap<>(limits));
        });
    }

    public int getSoldToday(String uuid, String itemId) {
        Map<String, Integer> limits = playerLimits.get(uuid);
        if (limits == null) return 0;
        return limits.getOrDefault(itemId, 0);
    }

    public void addSoldAmount(String uuid, String itemId, int amount) {
        playerLimits.computeIfAbsent(uuid, k -> new ConcurrentHashMap<>())
                .merge(itemId, amount, Integer::sum);
    }
}
