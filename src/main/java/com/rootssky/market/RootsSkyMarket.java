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
            getLogger().info("[Market] Preços carregados com sucesso. Sincronizando com o config.yml...");
            syncItemsFromConfig();
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

    public void reloadPlugin() {
        getLogger().info("[RootsSkyMarket] Iniciando processo de reload...");

        // 1. Fechar GUIs de bolsa/loja abertas para evitar duplicação ou bugs de inventário
        for (org.bukkit.entity.Player player : org.bukkit.Bukkit.getOnlinePlayers()) {
            org.bukkit.inventory.InventoryView view = player.getOpenInventory();
            if (view != null && view.getTopInventory().getHolder() != null) {
                String holderName = view.getTopInventory().getHolder().getClass().getName();
                if (holderName.startsWith("com.rootssky.market.gui")) {
                    player.closeInventory();
                }
            }
        }

        // 2. Cancelar todas as tarefas ativas do Bukkit scheduler para este plugin
        getServer().getScheduler().cancelTasks(this);

        // 3. Desregistrar todos os listeners deste plugin para evitar duplicação
        org.bukkit.event.HandlerList.unregisterAll(this);

        // 4. Parar e limpar managers antigos
        if (transactionManager != null) {
            transactionManager.shutdown();
        }
        if (databaseManager != null) {
            databaseManager.shutdown();
        }
        if (marketCache != null) {
            marketCache.clear();
        }

        // 5. Recarregar configurações do disco
        reloadConfig();
        
        if (shopManager != null) {
            shopManager.loadShops();
        } else {
            shopManager = new com.rootssky.market.engine.ShopManager(this);
            shopManager.loadShops();
        }

        // 6. Re-instanciar componentes centrais
        databaseManager = new DatabaseManager(this);
        databaseManager.initialize(this);
        
        marketCache = new MarketCache();
        
        transactionManager = new TransactionManager(this, databaseManager, marketCache);
        
        // 7. Re-instanciar e preparar listeners/engines
        playerSharesManager = new PlayerSharesManager(this);
        playerLimitManager = new PlayerLimitManager(this);
        eventEngine = new EventEngine(this);
        hotStockManager = new HotStockManager(this);
        marketIndexManager = new MarketIndexManager(this);
        dividendsEngine = new DividendsEngine(this);
        topInvestorsEngine = new TopInvestorsEngine(this);
        economyEngine = new EconomyEngine(this, databaseManager, marketCache);

        // 8. Carregar dados do banco de dados e sincronizar os itens com o config.yml
        marketCache.loadFromDatabase(databaseManager).thenRun(() -> {
            getLogger().info("[Market] Preços carregados com sucesso após reload. Sincronizando config...");
            syncItemsFromConfig();
        });

        // 9. Registrar novamente os listeners básicos
        getServer().getPluginManager().registerEvents(new StockGUIListener(), this);
        getServer().getPluginManager().registerEvents(new com.rootssky.market.gui.CustomAmountListener(), this);
        getServer().getPluginManager().registerEvents(new com.rootssky.market.gui.SellGUIListener(this), this);

        // 10. Agendar a inicialização das tarefas dos engines com delay
        getServer().getScheduler().runTaskLater(this, () -> {
            economyEngine.startPriceUpdateTask();
            playerSharesManager.initialize();
            playerLimitManager.initialize();
            eventEngine.start();
            hotStockManager.start();
            marketIndexManager.start();
            dividendsEngine.start();
            topInvestorsEngine.start();
            getLogger().info("[RootsSkyMarket] Todos os sistemas e tarefas reiniciados com sucesso.");
        }, 40L);
    }




    public void syncItemsFromConfig() {
        try {
            if (getConfig().getConfigurationSection("items") == null) {
                getLogger().warning("Nenhuma seção 'items' encontrada no config.yml!");
                return;
            }

            Map<String, MarketItem> configItems = new HashMap<>();
            for (String key : getConfig().getConfigurationSection("items").getKeys(false)) {
                String path = "items." + key;
                BigDecimal basePrice = BigDecimal.valueOf(getConfig().getDouble(path + ".base_price", 1.0));
                BigDecimal alpha = BigDecimal.valueOf(getConfig().getDouble(path + ".alpha", 0.15));
                
                // Determinar a categoria com base no shops.yml
                String category = "UNCATEGORIZED";
                if (shopManager != null) {
                    for (com.rootssky.market.engine.ShopManager.ShopCategory cat : shopManager.getCategories().values()) {
                        if (cat.getItems().contains(key)) {
                            category = cat.getId().toUpperCase();
                            break;
                        }
                    }
                }

                // Opcionais de floor e ceiling
                BigDecimal floorPrice = getConfig().contains(path + ".floor_price") ? 
                        BigDecimal.valueOf(getConfig().getDouble(path + ".floor_price")) : 
                        basePrice.multiply(BigDecimal.valueOf(0.2));
                BigDecimal ceilingPrice = getConfig().contains(path + ".ceiling_price") ? 
                        BigDecimal.valueOf(getConfig().getDouble(path + ".ceiling_price")) : 
                        basePrice.multiply(BigDecimal.valueOf(5.0));

                MarketItem item = new MarketItem(key, basePrice, basePrice, alpha, category, floorPrice, ceilingPrice, 0, java.time.Instant.now());
                configItems.put(key, item);
            }

            java.util.List<MarketItem> toSave = new java.util.ArrayList<>();

            // Primeiro atualiza o cache local com todos os itens do config.yml
            for (var entry : configItems.entrySet()) {
                MarketItem cached = marketCache.getItem(entry.getKey());
                if (cached != null) {
                    BigDecimal oldBasePrice = cached.getBasePrice();
                    BigDecimal newBasePrice = entry.getValue().getBasePrice();

                    // Item já existe: atualiza parâmetros estáticos e limites
                    cached.setBasePrice(newBasePrice);
                    cached.setAlpha(entry.getValue().getAlpha());
                    cached.setFloorPrice(entry.getValue().getFloorPrice());
                    cached.setCeilingPrice(entry.getValue().getCeilingPrice());
                    cached.setCategory(entry.getValue().getCategory());
                    
                    // Se o preço base mudou no config.yml, redefinimos o preço atual para o novo preço base
                    if (oldBasePrice != null && oldBasePrice.compareTo(newBasePrice) != 0) {
                        cached.setCurrentPrice(newBasePrice);
                        getLogger().info("[Market] Preço base de " + entry.getKey() + " alterado no config.yml (" + oldBasePrice + " -> " + newBasePrice + "). Preço atual redefinido.");
                    } else {
                        // Garante que o preço atual está nos limites
                        BigDecimal adjusted = cached.getCurrentPrice().max(cached.getFloorPrice()).min(cached.getCeilingPrice());
                        cached.setCurrentPrice(adjusted);
                    }
                    
                    marketCache.updateItem(entry.getKey(), cached);
                    toSave.add(cached);
                } else {
                    // Item novo: insere no cache local
                    marketCache.updateItem(entry.getKey(), entry.getValue());
                    toSave.add(entry.getValue());
                }
            }

            // Salva todos os itens de forma atômica no banco de dados em lote (evita deadlocks e saturação do Supabase)
            if (databaseManager.isConnected() && !toSave.isEmpty()) {
                databaseManager.savePricesBatch(toSave).thenRun(() -> {
                    getLogger().info("[RootsSkyMarket] Sincronização de itens e preços em lote concluída com sucesso.");
                }).exceptionally(ex -> {
                    getLogger().log(java.util.logging.Level.SEVERE, "Erro ao salvar itens em lote no banco:", ex);
                    return null;
                });
            } else {
                getLogger().info("[RootsSkyMarket] Sincronização concluída apenas em cache local (banco não conectado).");
            }
        } catch (Throwable t) {
            getLogger().log(java.util.logging.Level.SEVERE, "Erro crítico na sincronização de itens da configuração:", t);
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
                sender.sendMessage("§7/bolsaadmin resetall §f- Limpa todas as tabelas e re-semeia");
                sender.sendMessage("§7/bolsaadmin forceevent <bull|bear|normal> §f- Força um estado de mercado");
                sender.sendMessage("§7/bolsaadmin forcehotstock [item] §f- Força o queridinho do dia");
                sender.sendMessage("§7/bolsaadmin dumpapi §f- Debug da API");
                return true;
            }

            if (args[0].equalsIgnoreCase("resetall")) {
                sender.sendMessage("§eLimpando banco de dados e reiniciando a economia... Aguarde.");
                databaseManager.resetAllPricesAndTransactions().thenRun(() -> {
                    marketCache.clear();
                    if (marketIndexManager != null) {
                        marketIndexManager.setMarketState(com.rootssky.market.engine.MarketIndexManager.MarketState.NORMAL);
                    }
                    syncItemsFromConfig();
                    sender.sendMessage("§aBanco de dados resetado com sucesso! Preços iniciais do config.yml aplicados.");
                }).exceptionally(ex -> {
                    sender.sendMessage("§cErro ao resetar o banco de dados: " + ex.getMessage());
                    return null;
                });
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
                    reloadPlugin();
                    sender.sendMessage("§aConfiguração, shops, banco de dados e tarefas recarregados com sucesso.");
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