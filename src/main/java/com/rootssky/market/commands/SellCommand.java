package com.rootssky.market.commands;

import com.rootssky.market.RootsSkyMarket;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class SellCommand implements CommandExecutor {

    private final RootsSkyMarket plugin;

    public SellCommand(RootsSkyMarket plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("§cApenas jogadores podem usar este comando.");
            return true;
        }

        if (!player.hasPermission("rootssky.market.cmd.vender")) {
            player.sendMessage("§cVocê não tem permissão para usar este comando.");
            return true;
        }

        if (label.equalsIgnoreCase("sellgui") || label.equalsIgnoreCase("vendergui")) {
        String title = plugin.getConfig().getString("gui.titles.sell_gui", "Vender Itens");
            org.bukkit.inventory.Inventory inv = org.bukkit.Bukkit.createInventory(new com.rootssky.market.gui.SellGUIHolder(), 54, net.kyori.adventure.text.Component.text(net.md_5.bungee.api.ChatColor.translateAlternateColorCodes('&', title)));
            player.openInventory(inv);
            return true;
        }

        if (label.equalsIgnoreCase("sellall")) {
            if (args.length > 0 && args[0].equalsIgnoreCase("hand")) {
                plugin.getTransactionManager().processSellHand(player);
            } else {
                plugin.getTransactionManager().processGlobalSellAll(player);
            }
            return true;
        }

        if (label.equalsIgnoreCase("sellhand")) {
            plugin.getTransactionManager().processSellHand(player);
            return true;
        }

        if (args.length == 0) {
            player.sendMessage("§cUso correto:");
            player.sendMessage("§c/" + label + " tudo §7- Vende todo seu inventário");
            player.sendMessage("§c/" + label + " mao §7- Vende apenas o item da sua mão");
            return true;
        }

        String sub = args[0].toLowerCase();
        if (sub.equals("tudo") || sub.equals("all")) {
            plugin.getTransactionManager().processGlobalSellAll(player);
        } else if (sub.equals("mao") || sub.equals("hand")) {
            plugin.getTransactionManager().processSellHand(player);
        } else {
            player.sendMessage("§cUso correto: /" + label + " <tudo|mao>");
        }

        return true;
    }
}
