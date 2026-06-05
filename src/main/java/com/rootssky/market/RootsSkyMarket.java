package com.rootssky.market;

import com.rootssky.market.cache.MarketCache;
import com.rootssky.market.commands.BolsaCommand;
import com.rootssky.market.engine.EconomyEngine;
import com.rootssky.market.commands.TendenciasCommand;
import com.rootssky.market.gui.StockGUIListener;
import com.rootssky.market.integration.VaultBridge;
import com.rootssky.market.integration.papi.RootsBolsaExpansion;
import com.rootssky.market.managers.DatabaseManager;
import com.rootssky.market.managers.TransactionManager;
import com.rootssky.market.managers.PlayerSharesManager;
import com.rootssky.market.managers.PlayerLimitManager;
import com.rootssky.market.engine.EventEngine;
import com.rootssky.market.engine.HotStockManager;
import com.rootssky.market.engine.MarketIndexManager;
import com.rootssky.market.engine.DividendsEngine;
import com.rootssky.market.engine.TopInvestorsEngine;
import com.rootssky.market.model.MarketItem;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;

public class RootsSkyMarket extends JavaPlugin {

    private static RootsSkyMarket instance;

    private DatabaseManager databaseManager;
    private MarketCache marketCache;
    private VaultBridge vaultBridge;
    private EconomyEngine economyEngine;
    private TransactionManager transactionManager;
    
    private PlayerSharesManager playerSharesManager;
    private PlayerLimitManager playerLimitManager;
    private EventEngine eventEngine;
    private HotStockManager hotStockManager;
    private MarketIndexManager marketIndexManager;
    private DividendsEngine dividendsEngine;
    private TopInvestorsEngine topInvestorsEngine;

    private com.rootssky.market.engine.ShopManager shopManager;

    @Override
    public void onEnable() {
        instance = this;

        saveDefaultConfig();
        if (!new java.io.File(getDataFolder(), "shops.yml").exists()) {
            saveResource("shops.yml", false);
        }
        getLogger().info("========================================");
        getLogger().info("  RootsSkyMarket v" + getDescription().getVersion());
        getLogger().info("  Dynamic Economy Engine for Skyblock");
        getLogger().info("========================================");

        shopManager = new com.rootssky.market.engine.ShopManager(this);
        shopManager.loadShops();

        vaultBridge = new VaultBridge(this);
        if (!vaultBridge.setupEconomy()) {
            getLogger().severe("Integração com Vault falhou. Desativando plugin...");
            Bukkit.getPluginManager().disablePlugin(this);
            return;
        }

        databaseManager = new DatabaseManager(this);
        databaseManager.initialize(this);

        marketCache = new MarketCache();

        economyEngine = new EconomyEngine(this, databaseManager, marketCache);

        transactionManager = new TransactionManager(this, databaseManager, marketCache);
        
        playerSharesManager = new PlayerSharesManager(this);
        playerLimitManager = new PlayerLimitManager(this);
        eventEngine = new EventEngine(this);
        hotStockManager = new HotStockManager(this);
        marketIndexManager = new MarketIndexManager(this);
        dividendsEngine = new DividendsEngine(this);
        topInvestorsEngine = new TopInvestorsEngine(this);

        marketCache.loadFromDatabase(databaseManager).thenRun(() -> {
            getLogger().info("[Market] Preços carregados com sucesso.");
        });

        registerCommands();

        getCommand("tendencias").setExecutor(new TendenciasCommand(this));

        getServer().getPluginManager().registerEvents(new StockGUIListener(), this);
        getServer().getPluginManager().registerEvents(new com.rootssky.market.gui.CustomAmountListener(), this);
        getServer().getPluginManager().registerEvents(new com.rootssky.market.gui.SellGUIListener(this), this);

        if (Bukkit.getPluginManager().getPlugin("PlaceholderAPI") != null) {
            new RootsBolsaExpansion(this).register();
            new com.rootssky.market.integration.papi.RootsBolsaLegacyExpansion(this).register();
            getLogger().info("[RootsSkyMarket] PlaceholderAPI expansões registradas (rootssky_market + rootsbolsa).");
        }

        getServer().getScheduler().runTaskLater(this, () -> {
            economyEngine.startPriceUpdateTask();
            playerSharesManager.initialize();
            playerLimitManager.initialize();
            eventEngine.start();
            hotStockManager.start();
            marketIndexManager.start();
            dividendsEngine.start();
            topInvestorsEngine.start();
            getLogger().info("[RootsSkyMarket] Todos os sistemas inicializados.");
        }, 80L);

        getLogger().info("[RootsSkyMarket] Plugin ativado com sucesso!");
    }

    @Override
    public void onDisable() {
        getLogger().info("Desativando RootsSkyMarket...");

        // Fechar GUIs abertos para evitar bugs no reload ou reinício
        for (org.bukkit.entity.Player player : org.bukkit.Bukkit.getOnlinePlayers()) {
            org.bukkit.inventory.InventoryView view = player.getOpenInventory();
            if (view != null && view.getTopInventory().getHolder() != null) {
                String holderName = view.getTopInventory().getHolder().getClass().getName();
                if (holderName.startsWith("com.rootssky.market.gui")) {
                    player.closeInventory();
                }
            }
        }

        if (transactionManager != null) {
            transactionManager.shutdown();
        }

        if (databaseManager != null) {
            databaseManager.shutdown();
        }

        if (marketCache != null) {
            marketCache.clear();
        }

        getLogger().info("RootsSkyMarket desativado com sucesso.");
    }



    private void seedDefaultItems() {
        Map<String, MarketItem> items = new HashMap<>();

        // Minerais - preços base
        items.put("DIAMOND", new MarketItem("DIAMOND", BigDecimal.valueOf(25.00), BigDecimal.valueOf(0.15), "MINERALS"));
        items.put("EMERALD", new MarketItem("EMERALD", BigDecimal.valueOf(20.00), BigDecimal.valueOf(0.15), "MINERALS"));
        items.put("GOLD_INGOT", new MarketItem("GOLD_INGOT", BigDecimal.valueOf(8.00), BigDecimal.valueOf(0.15), "MINERALS"));
        items.put("IRON_INGOT", new MarketItem("IRON_INGOT", BigDecimal.valueOf(5.00), BigDecimal.valueOf(0.15), "MINERALS"));
        items.put("COAL", new MarketItem("COAL", BigDecimal.valueOf(2.00), BigDecimal.valueOf(0.15), "MINERALS"));
        items.put("QUARTZ", new MarketItem("QUARTZ", BigDecimal.valueOf(4.00), BigDecimal.valueOf(0.15), "MINERALS"));
        items.put("LAPIS_LAZULI", new MarketItem("LAPIS_LAZULI", BigDecimal.valueOf(3.00), BigDecimal.valueOf(0.15), "MINERALS"));
        items.put("REDSTONE", new MarketItem("REDSTONE", BigDecimal.valueOf(2.50), BigDecimal.valueOf(0.15), "MINERALS"));
        items.put("NETHERITE_INGOT", new MarketItem("NETHERITE_INGOT", BigDecimal.valueOf(100.00), BigDecimal.valueOf(0.15), "MINERALS"));
        items.put("COPPER_INGOT", new MarketItem("COPPER_INGOT", BigDecimal.valueOf(1.80), BigDecimal.valueOf(0.15), "MINERALS"));

        // Farming
        items.put("WHEAT", new MarketItem("WHEAT", BigDecimal.valueOf(1.50), BigDecimal.valueOf(0.15), "FARMING"));
        items.put("CARROT", new MarketItem("CARROT", BigDecimal.valueOf(1.20), BigDecimal.valueOf(0.15), "FARMING"));
        items.put("POTATO", new MarketItem("POTATO", BigDecimal.valueOf(1.30), BigDecimal.valueOf(0.15), "FARMING"));
        items.put("BEETROOT", new MarketItem("BEETROOT", BigDecimal.valueOf(1.10), BigDecimal.valueOf(0.15), "FARMING"));
        items.put("MELON_SLICE", new MarketItem("MELON_SLICE", BigDecimal.valueOf(0.80), BigDecimal.valueOf(0.15), "FARMING"));
        items.put("PUMPKIN", new MarketItem("PUMPKIN", BigDecimal.valueOf(1.60), BigDecimal.valueOf(0.15), "FARMING"));
        items.put("SUGAR_CANE", new MarketItem("SUGAR_CANE", BigDecimal.valueOf(0.90), BigDecimal.valueOf(0.15), "FARMING"));
        items.put("CACTUS", new MarketItem("CACTUS", BigDecimal.valueOf(1.40), BigDecimal.valueOf(0.15), "FARMING"));
        items.put("BAMBOO", new MarketItem("BAMBOO", BigDecimal.valueOf(0.70), BigDecimal.valueOf(0.15), "FARMING"));
        items.put("KELP", new MarketItem("KELP", BigDecimal.valueOf(0.60), BigDecimal.valueOf(0.15), "FARMING"));

        // Blocos
        items.put("STONE", new MarketItem("STONE", BigDecimal.valueOf(1.00), BigDecimal.valueOf(0.15), "BLOCKS"));
        items.put("COBBLESTONE", new MarketItem("COBBLESTONE", BigDecimal.valueOf(0.80), BigDecimal.valueOf(0.15), "BLOCKS"));
        items.put("OBSIDIAN", new MarketItem("OBSIDIAN", BigDecimal.valueOf(8.00), BigDecimal.valueOf(0.15), "BLOCKS"));

        // Mob Drops
        items.put("STRING", new MarketItem("STRING", BigDecimal.valueOf(1.00), BigDecimal.valueOf(0.15), "MOB_DROPS"));
        items.put("BONE", new MarketItem("BONE", BigDecimal.valueOf(1.20), BigDecimal.valueOf(0.15), "MOB_DROPS"));
        items.put("ENDER_PEARL", new MarketItem("ENDER_PEARL", BigDecimal.valueOf(3.00), BigDecimal.valueOf(0.15), "MOB_DROPS"));
        items.put("SLIME_BALL", new MarketItem("SLIME_BALL", BigDecimal.valueOf(2.00), BigDecimal.valueOf(0.15), "MOB_DROPS"));
        items.put("GUNPOWDER", new MarketItem("GUNPOWDER", BigDecimal.valueOf(2.50), BigDecimal.valueOf(0.15), "MOB_DROPS"));
        items.put("ROTTEN_FLESH", new MarketItem("ROTTEN_FLESH", BigDecimal.valueOf(0.50), BigDecimal.valueOf(0.15), "MOB_DROPS"));
        items.put("SPIDER_EYE", new MarketItem("SPIDER_EYE", BigDecimal.valueOf(1.10), BigDecimal.valueOf(0.15), "MOB_DROPS"));

        // Simulação de variações para teste
        // Baixas (currentPrice = basePrice * 0.95 = -5%)
        simulatePriceVariation(items, "DIAMOND", 0.95);
        simulatePriceVariation(items, "EMERALD", 0.95);
        simulatePriceVariation(items, "GOLD_INGOT", 0.95);

        // Altas (currentPrice = basePrice * 1.10 = +10%)
        simulatePriceVariation(items, "IRON_INGOT", 1.10);
        simulatePriceVariation(items, "COAL", 1.10);
        simulatePriceVariation(items, "QUARTZ", 1.10);

        // Volumes simulados
        setVolume(items, "DIAMOND", 150);
        setVolume(items, "EMERALD", 120);
        setVolume(items, "GOLD_INGOT", 200);
        setVolume(items, "IRON_INGOT", 350);
        setVolume(items, "COAL", 500);
        setVolume(items, "QUARTZ", 180);
        setVolume(items, "WHEAT", 420);
        setVolume(items, "CARROT", 310);
        setVolume(items, "POTATO", 280);
        setVolume(items, "NETHERITE_INGOT", 45);
        setVolume(items, "ENDER_PEARL", 90);
        setVolume(items, "STONE", 600);

        // SEMPRE carrega no cache local imediatamente (funciona mesmo sem DB)
        for (var entry : items.entrySet()) {
            marketCache.updateItem(entry.getKey(), entry.getValue());
        }
        getLogger().info("[RootsSkyMarket] Cache local carregado com " + items.size() + " itens.");

        // Se o banco estiver conectado, salva e depois recarrega do banco
        if (databaseManager.isConnected()) {
            databaseManager.seedDefaultItems(items).thenRun(() -> {
                getLogger().info("Seeded " + items.size() + " default items into database.");
                // Só recarrega do banco DEPOIS do seed terminar
                marketCache.loadFromDatabase(databaseManager);
            });
        }
    }

    private void simulatePriceVariation(Map<String, MarketItem> items, String itemId, double multiplier) {
        MarketItem item = items.get(itemId);
        if (item != null) {
            item.setCurrentPrice(item.getBasePrice().multiply(BigDecimal.valueOf(multiplier))
                    .setScale(2, RoundingMode.HALF_UP));
        }
    }

    private void setVolume(Map<String, MarketItem> items, String itemId, int volume) {
        MarketItem item = items.get(itemId);
        if (item != null) {
            item.setVolume24h(volume);
        }
    }

    private void registerCommands() {
        BolsaCommand bolsaCommand = new BolsaCommand(this);
        var bolsaCmd = getCommand("bolsa");
        if (bolsaCmd != null) {
            bolsaCmd.setExecutor(bolsaCommand);
            bolsaCmd.setTabCompleter(bolsaCommand);
        }

        var lojaCmd = getCommand("loja");
        if (lojaCmd != null) {
            lojaCmd.setExecutor(new com.rootssky.market.commands.LojaCommand(this));
        }
        
        var sellCmd = new com.rootssky.market.commands.SellCommand(this);
        String[] sellAliases = {"vender", "sell", "sellall", "sellhand"};
        for (String alias : sellAliases) {
            var cmd = getCommand(alias);
            if (cmd != null) {
                cmd.setExecutor(sellCmd);
            }
        }

        getLogger().info("[RootsSkyMarket] Comandos registrados.");
        getCommand("bolsaadmin").setExecutor((sender, command, label, args) -> {
            if (!sender.hasPermission("rootssky.market.admin")) {
                sender.sendMessage("§cVocê não tem permissão para usar este comando.");
                return true;
            }

            if (args.length == 0) {
                sender.sendMessage("§e§l=== Admin da Bolsa ===");
                sender.sendMessage("§7/bolsaadmin reload §f- Recarrega a configuração");
                sender.sendMessage("§7/bolsaadmin reset <item> §f- Reseta preço para o valor base");
                sender.sendMessage("§7/bolsaadmin forceevent <bull|bear|normal> §f- Força um estado de mercado");
                sender.sendMessage("§7/bolsaadmin forcehotstock [item] §f- Força o queridinho do dia");
                sender.sendMessage("§7/bolsaadmin dumpapi §f- Debug da API");
                return true;
            }

            if (args[0].equalsIgnoreCase("forceevent")) {
                if (args.length < 2) {
                    sender.sendMessage("§cUse: /bolsaadmin forceevent <bull|bear|normal>");
                    return true;
                }
                String state = args[1].toUpperCase();
                try {
                    com.rootssky.market.engine.MarketIndexManager.MarketState marketState = com.rootssky.market.engine.MarketIndexManager.MarketState.valueOf(state);
                    marketIndexManager.setMarketState(marketState);
                    sender.sendMessage("§aEstado do mercado alterado para " + marketState.name() + "!");
                } catch (IllegalArgumentException e) {
                    sender.sendMessage("§cEstado inválido. Use BULL, BEAR ou NORMAL.");
                }
                return true;
            }

            if (args[0].equalsIgnoreCase("forcehotstock")) {
                String item = args.length > 1 ? args[1].toUpperCase() : null;
                hotStockManager.pickHotStock(item);
                sender.sendMessage("§aQueridinho do Dia atualizado!");
                return true;
            }

            if (args[0].equalsIgnoreCase("dumpapi")) {
                try {
                    Class<?> hookClass = Class.forName("me.gypopo.economyshopgui.api.EconomyShopGUIHook");
                    sender.sendMessage("§eMethods of EconomyShopGUIHook:");
                    for (java.lang.reflect.Method m : hookClass.getDeclaredMethods()) {
                        sender.sendMessage("§7- " + m.getName() + " -> " + m.getReturnType().getSimpleName());
                    }
                    sender.sendMessage("§eMethods of ShopManager (if accessible):");
                    Class<?> mainClass = Class.forName("me.gypopo.economyshopgui.EconomyShopGUI");
                    Object instance = mainClass.getMethod("getInstance").invoke(null);
                    Object shopManager = mainClass.getMethod("getShopManager").invoke(instance);
                    for (java.lang.reflect.Method m : shopManager.getClass().getDeclaredMethods()) {
                        if (m.getReturnType().getSimpleName().contains("List") || m.getReturnType().getSimpleName().contains("Collection") || m.getReturnType().getSimpleName().contains("Map")) {
                            sender.sendMessage("§7- " + m.getName() + " -> " + m.getReturnType().getSimpleName());
                        }
                    }
                } catch (Exception e) {
                    sender.sendMessage("§cFailed to dump API: " + e.getMessage());
                }
                return true;
            }

            switch (args[0].toLowerCase()) {
                case "reload" -> {
                    reloadConfig();
                    sender.sendMessage("§aConfiguração recarregada com sucesso.");
                }
                case "reset" -> {
                    if (args.length < 2) {
                        sender.sendMessage("§cUso: /bolsaadmin reset <item>");
                        return true;
                    }
                    String itemId = args[1].toUpperCase();
                    MarketItem item = marketCache.getItem(itemId);
                    if (item == null) {
                        sender.sendMessage("§cItem '" + itemId + "' não encontrado no mercado.");
                        return true;
                    }
                    item.setCurrentPrice(item.getBasePrice());
                    item.setVolume24h(0);
                    marketCache.updateItem(itemId, item);
                    databaseManager.savePrice(item);
                    sender.sendMessage("§aPreço de " + itemId + " resetado para o valor base (" +
                            vaultBridge.format(item.getBasePrice().doubleValue()) + ").");
                }
                default -> sender.sendMessage("§cSubcomando desconhecido. Use /bolsaadmin para ajuda.");
            }

            return true;
        });
    }

    public static RootsSkyMarket getInstance() {
        return instance;
    }

    public DatabaseManager getDatabaseManager() {
        return databaseManager;
    }

    public MarketCache getMarketCache() {
        return marketCache;
    }

    public VaultBridge getVaultBridge() {
        return vaultBridge;
    }

    public EconomyEngine getEconomyEngine() {
        return economyEngine;
    }

    public TransactionManager getTransactionManager() {
        return transactionManager;
    }
    
    public PlayerSharesManager getPlayerSharesManager() {
        return playerSharesManager;
    }
    
    public PlayerLimitManager getPlayerLimitManager() {
        return playerLimitManager;
    }
    
    public TopInvestorsEngine getTopInvestorsEngine() {
        return topInvestorsEngine;
    }

    public com.rootssky.market.engine.ShopManager getShopManager() {
        return shopManager;
    }

    public EventEngine getEventEngine() {
        return eventEngine;
    }
    
    public HotStockManager getHotStockManager() {
        return hotStockManager;
    }
    
    public MarketIndexManager getMarketIndexManager() {
        return marketIndexManager;
    }
}