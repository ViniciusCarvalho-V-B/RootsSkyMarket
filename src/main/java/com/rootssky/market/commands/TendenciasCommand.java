package com.rootssky.market.commands;

import com.rootssky.market.RootsSkyMarket;
import com.rootssky.market.gui.StockGUI;
import com.rootssky.market.model.MarketItem;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.math.BigDecimal;
import java.math.RoundingMode;

public class TendenciasCommand implements CommandExecutor {

    private final RootsSkyMarket plugin;

    public TendenciasCommand(RootsSkyMarket plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("Este comando só pode ser usado por jogadores.");
            return true;
        }

        StockGUI stockGUI = new StockGUI(plugin);

        player.sendMessage(Component.empty());
        player.sendMessage(Component.text("=== Tendências do Mercado ===")
                .color(NamedTextColor.GOLD).decorate(TextDecoration.BOLD));
        player.sendMessage(Component.empty());

        player.sendMessage(Component.text("Maiores Altas")
                .color(NamedTextColor.GREEN).decorate(TextDecoration.BOLD));

        for (MarketItem item : stockGUI.getTopGainers(3)) {
            BigDecimal variation = calculateVariation(item);
            int comp = variation.compareTo(BigDecimal.ZERO);
            String varStr;
            NamedTextColor color;
            if (comp > 0) {
                varStr = "+" + variation.abs().setScale(1, RoundingMode.HALF_UP) + "%";
                color = NamedTextColor.GREEN;
            } else if (comp < 0) {
                varStr = "-" + variation.abs().setScale(1, RoundingMode.HALF_UP) + "%";
                color = NamedTextColor.RED;
            } else {
                varStr = "0.0%";
                color = NamedTextColor.GRAY;
            }
            player.sendMessage(Component.text("  " + item.getItemId() + " ")
                    .color(NamedTextColor.WHITE)
                    .append(Component.text(varStr).color(color))
                    .append(Component.text(" - ").color(NamedTextColor.GRAY))
                    .append(Component.text(plugin.getVaultBridge().format(item.getCurrentPrice().doubleValue()))
                            .color(NamedTextColor.YELLOW)));
        }

        player.sendMessage(Component.empty());
        player.sendMessage(Component.text("Maiores Quedas")
                .color(NamedTextColor.RED).decorate(TextDecoration.BOLD));

        for (MarketItem item : stockGUI.getTopLosers(3)) {
            BigDecimal variation = calculateVariation(item);
            int comp = variation.compareTo(BigDecimal.ZERO);
            String varStr;
            NamedTextColor color;
            if (comp > 0) {
                varStr = "+" + variation.abs().setScale(1, RoundingMode.HALF_UP) + "%";
                color = NamedTextColor.GREEN;
            } else if (comp < 0) {
                varStr = "-" + variation.abs().setScale(1, RoundingMode.HALF_UP) + "%";
                color = NamedTextColor.RED;
            } else {
                varStr = "0.0%";
                color = NamedTextColor.GRAY;
            }
            player.sendMessage(Component.text("  " + item.getItemId() + " ")
                    .color(NamedTextColor.WHITE)
                    .append(Component.text(varStr).color(color))
                    .append(Component.text(" - ").color(NamedTextColor.GRAY))
                    .append(Component.text(plugin.getVaultBridge().format(item.getCurrentPrice().doubleValue()))
                            .color(NamedTextColor.YELLOW)));
        }

        player.sendMessage(Component.empty());
        return true;
    }

    private BigDecimal calculateVariation(MarketItem item) {
        BigDecimal base = item.getBasePrice();
        if (base.compareTo(BigDecimal.ZERO) == 0) return BigDecimal.ZERO;
        return item.getCurrentPrice().subtract(base)
                .multiply(BigDecimal.valueOf(100))
                .divide(base, 2, RoundingMode.HALF_UP);
    }
}
