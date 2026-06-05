package com.rootssky.market.integration.papi;

import com.rootssky.market.RootsSkyMarket;
import com.rootssky.market.model.MarketItem;
import com.rootssky.market.util.PriceIndicator;
import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Expansão legada para compatibilidade com scripts antigos que usam rootsbolsa.
 * Placeholders: %rootsbolsa_trend_ITEM%, %rootsbolsa_var_ITEM%, %rootsbolsa_price_ITEM%
 */
public class RootsBolsaLegacyExpansion extends PlaceholderExpansion {

    private final RootsSkyMarket plugin;

    public RootsBolsaLegacyExpansion(RootsSkyMarket plugin) {
        this.plugin = plugin;
    }

    @Override
    public @NotNull String getIdentifier() {
        return "rootsbolsa";
    }

    @Override
    public @NotNull String getAuthor() {
        return "RootsSky";
    }

    @Override
    public @NotNull String getVersion() {
        return plugin.getDescription().getVersion();
    }

    @Override
    public boolean persist() {
        return true;
    }

    @Override
    public boolean canRegister() {
        return true;
    }

    @Override
    public @Nullable String onRequest(org.bukkit.OfflinePlayer player, @NotNull String params) {
        if (params == null || params.isEmpty()) return null;

        String[] parts = params.split("_", 2);
        if (parts.length < 2) return null;

        String tag = parts[0].toLowerCase();
        String itemId = parts[1].toUpperCase();

        MarketItem item = plugin.getMarketCache().getItem(itemId);
        if (item == null) return "§7-";

        return switch (tag) {
            case "trend" -> {
                String color = PriceIndicator.getTrendColor(item);
                String icon = switch (color) {
                    case "GREEN" -> "§a▲";
                    case "RED" -> "§c▼";
                    default -> "§7→";
                };
                yield icon + " " + PriceIndicator.getPercentageChange(item);
            }
            case "var" -> PriceIndicator.getPercentageChange(item);
            case "price" -> plugin.getVaultBridge().format(item.getCurrentPrice().doubleValue());
            case "floor" -> plugin.getVaultBridge().format(item.getFloorPrice().doubleValue());
            case "ceil" -> plugin.getVaultBridge().format(item.getCeilingPrice().doubleValue());
            default -> null;
        };
    }
}
