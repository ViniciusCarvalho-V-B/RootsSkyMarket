package com.rootssky.market.util;

import com.rootssky.market.model.MarketItem;

import java.math.BigDecimal;
import java.math.RoundingMode;

public class PriceIndicator {

    public static String getTrendIcon(MarketItem item) {
        BigDecimal change = item.getCurrentPrice().subtract(item.getBasePrice());
        if (change.compareTo(BigDecimal.ZERO) > 0) {
            return "↑";
        } else if (change.compareTo(BigDecimal.ZERO) < 0) {
            return "↓";
        } else {
            return "→";
        }
    }

    public static String getTrendColor(MarketItem item) {
        BigDecimal change = item.getCurrentPrice().subtract(item.getBasePrice());
        if (change.compareTo(BigDecimal.ZERO) > 0) {
            return "GREEN";
        } else if (change.compareTo(BigDecimal.ZERO) < 0) {
            return "RED";
        } else {
            return "GRAY";
        }
    }

    public static String getPercentageChange(MarketItem item) {
        BigDecimal basePrice = item.getBasePrice();
        if (basePrice.compareTo(BigDecimal.ZERO) == 0) {
            return "0.0%";
        }

        BigDecimal change = item.getCurrentPrice().subtract(basePrice);
        BigDecimal percentage = change.multiply(BigDecimal.valueOf(100))
                .divide(basePrice, 1, RoundingMode.HALF_UP);

        if (percentage.compareTo(BigDecimal.ZERO) > 0) {
            return "+" + percentage + "%";
        } else {
            return percentage + "%";
        }
    }
}
