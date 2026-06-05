package com.rootssky.market.managers;

import com.rootssky.market.RootsSkyMarket;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class PlayerSharesManager implements Listener {

    private final RootsSkyMarket plugin;
    // UUID -> (ItemId -> SharesAmount)
    private final Map<String, Map<String, Integer>> playerShares = new ConcurrentHashMap<>();

    public PlayerSharesManager(RootsSkyMarket plugin) {
        this.plugin = plugin;
    }

    public void initialize() {
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
        for (Player p : plugin.getServer().getOnlinePlayers()) {
            loadPlayerShares(p.getUniqueId().toString());
        }
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        loadPlayerShares(event.getPlayer().getUniqueId().toString());
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        playerShares.remove(event.getPlayer().getUniqueId().toString());
    }

    private void loadPlayerShares(String uuid) {
        plugin.getDatabaseManager().getShares(uuid).thenAccept(shares -> {
            playerShares.put(uuid, new ConcurrentHashMap<>(shares));
        });
    }

    public int getShares(String uuid, String itemId) {
        Map<String, Integer> shares = playerShares.get(uuid);
        if (shares == null) return 0;
        return shares.getOrDefault(itemId, 0);
    }

    public void addShares(String uuid, String itemId, int amount) {
        Map<String, Integer> shares = playerShares.computeIfAbsent(uuid, k -> new ConcurrentHashMap<>());
        int newAmount = shares.getOrDefault(itemId, 0) + amount;
        shares.put(itemId, newAmount);
        plugin.getDatabaseManager().updateShares(uuid, itemId, newAmount);
    }

    public boolean removeShares(String uuid, String itemId, int amount) {
        Map<String, Integer> shares = playerShares.get(uuid);
        if (shares == null) return false;
        int current = shares.getOrDefault(itemId, 0);
        if (current < amount) return false;
        
        int newAmount = current - amount;
        shares.put(itemId, newAmount);
        plugin.getDatabaseManager().updateShares(uuid, itemId, newAmount);
        return true;
    }
    
    public Map<String, Integer> getAllShares(String uuid) {
        return playerShares.getOrDefault(uuid, new ConcurrentHashMap<>());
    }
}
