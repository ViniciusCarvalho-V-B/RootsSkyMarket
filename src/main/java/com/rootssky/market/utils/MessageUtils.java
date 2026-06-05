package com.rootssky.market.utils;

import org.bukkit.ChatColor;
import org.bukkit.configuration.file.FileConfiguration;

public class MessageUtils {

    public static String get(FileConfiguration config, String path) {
        String msg = config.getString("messages." + path, "");
        if (msg.isEmpty()) return "";
        
        String prefix = config.getString("messages.prefix", "&8[&6Bolsa&8] ");
        return ChatColor.translateAlternateColorCodes('&', prefix + msg);
    }
    
    public static String getNoPrefix(FileConfiguration config, String path) {
        String msg = config.getString("messages." + path, "");
        return ChatColor.translateAlternateColorCodes('&', msg);
    }
}
