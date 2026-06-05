package com.rootssky.market.integration.papi;

import com.rootssky.market.RootsSkyMarket;
import com.rootssky.market.model.MarketItem;
import com.rootssky.market.util.PriceIndicator;
import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * PlaceholderAPI Expansion para o RootsSkyMarket.
 * 
 * Placeholders disponíveis:
 *   %rootssky_market_indicator_ITEM%  -> Seta colorida (§a▲ ou §c▼ ou §7→)
 *   %rootssky_market_var_ITEM%        -> Variação percentual (+5.2% ou -3.1%)
 *   %rootssky_market_price_ITEM%      -> Preço atual formatado
 *   %rootssky_market_floor_ITEM%      -> Preço mínimo formatado
 *   %rootssky_market_ceil_ITEM%       -> Preço máximo formatado
 *   
 *   Compatibilidade legada (rootsbolsa):
 *   %rootsbolsa_trend_ITEM%           -> Seta + variação combinados
 *   %rootsbolsa_var_ITEM%             -> Variação percentual
 *   %rootsbolsa_price_ITEM%           -> Preço atual formatado
 *   %rootsbolsa_floor_ITEM%           -> Preço mínimo
 *   %rootsbolsa_ceil_ITEM%            -> Preço máximo
 */
public class RootsBolsaExpansion extends PlaceholderExpansion {

    private final RootsSkyMarket plugin;

    public RootsBolsaExpansion(RootsSkyMarket plugin) {
        this.plugin = plugin;
    }

    @Override
    public @NotNull String getIdentifier() {
        return "rootssky_market";
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
        if (params == null || params.isEmpty()) {
            return null;
        }

        // Formato: indicator_ITEM, var_ITEM, price_ITEM, floor_ITEM, ceil_ITEM
        String[] parts = params.split("_", 2);
        if (parts.length < 2) {
            return null;
        }

        String tag = parts[0].toLowerCase();
        String itemId = parts[1].toUpperCase();

        MarketItem item = plugin.getMarketCache().getItem(itemId);
        if (item == null) {
            return "§7-";
        }

        return switch (tag) {
            case "indicator" -> {
                String color = PriceIndicator.getTrendColor(item);
                yield switch (color) {
                    case "GREEN" -> "§a▲";
                    case "RED" -> "§c▼";
                    default -> "§7→";
                };
            }
            case "var" -> PriceIndicator.getPercentageChange(item);
            case "price" -> plugin.getVaultBridge().format(item.getCurrentPrice().doubleValue());
            case "floor" -> plugin.getVaultBridge().format(item.getFloorPrice().doubleValue());
            case "ceil" -> plugin.getVaultBridge().format(item.getCeilingPrice().doubleValue());
            default -> null;
        };
    }
}