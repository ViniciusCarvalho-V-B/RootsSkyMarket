package com.rootssky.market.integration;

import com.rootssky.market.RootsSkyMarket;

import net.milkbowl.vault.economy.Economy;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.plugin.RegisteredServiceProvider;

import java.util.logging.Logger;

public class VaultBridge {

    private Economy economy;
    private final RootsSkyMarket plugin;
    private final Logger logger;
    private boolean hooked = false;

    public VaultBridge(RootsSkyMarket plugin) {
        this.plugin = plugin;
        this.logger = plugin.getLogger();
    }

    public boolean setupEconomy() {
        if (Bukkit.getPluginManager().getPlugin("Vault") == null) {
            logger.severe("[VaultBridge] Vault not found! Plugin requires Vault.");
            return false;
        }

        RegisteredServiceProvider<Economy> rsp = Bukkit.getServicesManager().getRegistration(Economy.class);
        if (rsp == null) {
            logger.severe("[VaultBridge] No economy provider found. Install an economy plugin (e.g., EssentialsX).");
            return false;
        }

        economy = rsp.getProvider();
        hooked = true;
        logger.info("[VaultBridge] Hooked into economy provider: " + economy.getName());
        return true;
    }

    public boolean isHooked() {
        return hooked && economy != null;
    }

    public double getBalance(OfflinePlayer player) {
        if (!isHooked()) return 0.0;
        return economy.getBalance(player);
    }

    public boolean hasBalance(OfflinePlayer player, double amount) {
        if (!isHooked()) return false;
        return economy.has(player, amount);
    }

    public boolean withdraw(OfflinePlayer player, double amount) {
        if (!isHooked()) return false;
        if (amount <= 0) return false;

        var result = economy.withdrawPlayer(player, amount);
        if (!result.transactionSuccess()) {
            logger.warning("[VaultBridge] Failed to withdraw " + amount + " from " + player.getName());
            return false;
        }
        return true;
    }

    public void deposit(OfflinePlayer player, double amount) {
        if (!isHooked()) return;
        if (amount <= 0) return;

        var result = economy.depositPlayer(player, amount);
        if (!result.transactionSuccess()) {
            logger.warning("[VaultBridge] Failed to deposit " + amount + " to " + player.getName());
        }
    }

    public String format(double amount) {
        java.text.DecimalFormat df = new java.text.DecimalFormat("#,##0.00", new java.text.DecimalFormatSymbols(new java.util.Locale("pt", "BR")));
        String formattedAmount = df.format(amount);
        
        if (plugin.getConfig().contains("economy.currency.symbol")) {
            String symbol = plugin.getConfig().getString("economy.currency.symbol");
            String format = plugin.getConfig().getString("economy.currency.format");
            if (format != null) {
                return format.replace("{symbol}", symbol).replace("{amount}", formattedAmount);
            }
            return symbol + formattedAmount;
        }

        if (!isHooked()) return formattedAmount;
        return economy.format(amount);
    }

    public Economy getEconomy() {
        return economy;
    }
}
