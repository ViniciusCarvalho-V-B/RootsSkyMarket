package com.rootssky.market.commands;

import com.rootssky.market.RootsSkyMarket;
import com.rootssky.market.gui.ShopCategoryGUI;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

public class LojaCommand implements CommandExecutor {

    private final RootsSkyMarket plugin;

    public LojaCommand(RootsSkyMarket plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("§cEste comando só pode ser executado por jogadores.");
            return true;
        }

        if (args.length > 0) {
            String categoryName = args[0].toLowerCase();
            com.rootssky.market.engine.ShopManager.ShopCategory targetCat = null;
            
            for (com.rootssky.market.engine.ShopManager.ShopCategory cat : plugin.getShopManager().getCategories().values()) {
                if (cat.getId().equalsIgnoreCase(categoryName) || cat.getName().toLowerCase().contains(categoryName)) {
                    targetCat = cat;
                    break;
                }
            }
            
            if (targetCat != null) {
                boolean hideBack = false;
                if (args.length >= 2) {
                    String sub = args[1].toLowerCase();
                    hideBack = sub.equals("true") || sub.equals("esconder") || sub.equals("hide") || sub.equals("noback");
                }
                new com.rootssky.market.gui.ShopItemsGUI(plugin, targetCat, 1, hideBack).open(player);
                return true;
            } else {
                player.sendMessage("§cCategoria não encontrada: " + args[0]);
            }
        }

        new ShopCategoryGUI(plugin).open(player);
        return true;
    }
}
