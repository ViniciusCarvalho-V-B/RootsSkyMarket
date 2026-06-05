package com.rootssky.market.engine;

import com.rootssky.market.RootsSkyMarket;
import org.bukkit.Material;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class ShopManager {

    private final RootsSkyMarket plugin;
    private final Map<String, ShopCategory> categories = new LinkedHashMap<>();

    public ShopManager(RootsSkyMarket plugin) {
        this.plugin = plugin;
    }

    public void loadShops() {
        categories.clear();
        File file = new File(plugin.getDataFolder(), "shops.yml");
        if (!file.exists()) {
            return;
        }

        YamlConfiguration config = YamlConfiguration.loadConfiguration(file);
        if (config.contains("categories")) {
            for (String key : config.getConfigurationSection("categories").getKeys(false)) {
                String path = "categories." + key;
                String name = config.getString(path + ".name", key);
                String iconStr = config.getString(path + ".icon", "STONE");
                int slot = config.getInt(path + ".slot", 0);
                boolean fixedPrice = config.getBoolean(path + ".fixed_price", false);
                List<String> items = config.getStringList(path + ".items");

                Material icon;
                try {
                    icon = Material.valueOf(iconStr.toUpperCase());
                } catch (IllegalArgumentException e) {
                    icon = Material.STONE;
                    plugin.getLogger().warning("Ícone inválido para categoria " + key + ": " + iconStr);
                }

                categories.put(key, new ShopCategory(key, name, icon, slot, fixedPrice, items));
            }
        }
        plugin.getLogger().info("Carregadas " + categories.size() + " categorias da loja.");
    }

    public Map<String, ShopCategory> getCategories() {
        return categories;
    }

    public ShopCategory getCategory(String id) {
        return categories.get(id);
    }

    public static class ShopCategory {
        private final String id;
        private final String name;
        private final Material icon;
        private final int slot;
        private final boolean fixedPrice;
        private final List<String> items;

        public ShopCategory(String id, String name, Material icon, int slot, boolean fixedPrice, List<String> items) {
            this.id = id;
            this.name = name;
            this.icon = icon;
            this.slot = slot;
            this.fixedPrice = fixedPrice;
            this.items = items != null ? items : new ArrayList<>();
        }

        public String getId() { return id; }
        public String getName() { return name; }
        public Material getIcon() { return icon; }
        public int getSlot() { return slot; }
        public boolean isFixedPrice() { return fixedPrice; }
        public List<String> getItems() { return items; }
    }
}
