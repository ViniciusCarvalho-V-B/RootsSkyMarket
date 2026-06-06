package com.rootssky.market.managers;

import com.rootssky.market.RootsSkyMarket;
import com.rootssky.market.model.MarketItem;
import com.rootssky.market.model.TransactionData;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import com.rootssky.market.RootsSkyMarket;

import java.sql.*;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.logging.Level;
import java.util.logging.Logger;

public class DatabaseManager {

    private final Logger logger;
    private HikariDataSource dataSource;
    private final ExecutorService executor;
    private volatile boolean connected = false;
    private int reconnectAttempts = 0;
    private static final int MAX_RECONNECT_ATTEMPTS = 10;
    private static final long RECONNECT_DELAY_MS = 5000;

    public DatabaseManager(RootsSkyMarket plugin) {
        this.logger = plugin.getLogger();
        this.executor = Executors.newFixedThreadPool(3, r -> {
            Thread t = new Thread(r, "RootsSkyMarket-DB");
            t.setDaemon(true);
            return t;
        });
    }

    private String dbType = "SQLITE";

    public void initialize(RootsSkyMarket plugin) {
        try {
            dbType = plugin.getConfig().getString("database.type", "SQLITE").toUpperCase();
            
            HikariConfig hikariConfig = new HikariConfig();
            
            if (dbType.equals("SQLITE")) {
                Class.forName("org.sqlite.JDBC");
                hikariConfig.setDriverClassName("org.sqlite.JDBC");
                java.io.File dbFile = new java.io.File(plugin.getDataFolder(), "database.db");
                hikariConfig.setJdbcUrl("jdbc:sqlite:" + dbFile.getAbsolutePath());
                hikariConfig.setMaximumPoolSize(1); // SQLite is file-based, keep pool size to 1 to avoid locking
            } else {
                String host = plugin.getConfig().getString("database.host", "localhost");
                int port = plugin.getConfig().getInt("database.port", 5432);
                String database = plugin.getConfig().getString("database.database", "postgres");
                String user = plugin.getConfig().getString("database.user", "postgres");
                String password = plugin.getConfig().getString("database.password", "");
                int poolSize = plugin.getConfig().getInt("database.pool-size", 5);

                if (dbType.equals("MYSQL")) {
                    Class.forName("com.mysql.cj.jdbc.Driver");
                    hikariConfig.setDriverClassName("com.mysql.cj.jdbc.Driver");
                    hikariConfig.setJdbcUrl(String.format("jdbc:mysql://%s:%d/%s?useSSL=false&autoReconnect=true", host, port, database));
                } else {
                    Class.forName("org.postgresql.Driver");
                    hikariConfig.setDriverClassName("org.postgresql.Driver");
                    hikariConfig.setJdbcUrl(String.format("jdbc:postgresql://%s:%d/%s?sslmode=require", host, port, database));
                }
                
                hikariConfig.setUsername(user);
                hikariConfig.setPassword(password);
                hikariConfig.setMaximumPoolSize(poolSize);
            }

            hikariConfig.setMinimumIdle(1);
            hikariConfig.setConnectionTimeout(10000);
            hikariConfig.setIdleTimeout(300000);
            hikariConfig.setMaxLifetime(600000);
            hikariConfig.setPoolName("RootsSkyMarket-Pool");

            dataSource = new HikariDataSource(hikariConfig);
            testConnection();
            createTablesIfNotExists();
            connected = true;
            logger.info("Database connection (" + dbType + ") established successfully.");
        } catch (Exception e) {
            logger.severe("Failed to initialize database: " + e.getMessage());
            connected = false;
            scheduleReconnect();
        }
    }

    private void testConnection() throws SQLException {
        try (Connection conn = dataSource.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT 1")) {
            if (rs.next()) {
                logger.fine("Database connection test passed.");
            }
        }
    }

    private void createTablesIfNotExists() throws SQLException {
        try (Connection conn = dataSource.getConnection()) {
            conn.createStatement().executeUpdate(
                    "CREATE TABLE IF NOT EXISTS market_prices (" +
                            "item_id VARCHAR(64) PRIMARY KEY, " +
                            "base_price DECIMAL(12,4) NOT NULL, " +
                            "current_price DECIMAL(12,4) NOT NULL, " +
                            "alpha DECIMAL(5,3) NOT NULL, " +
                            "category VARCHAR(32) DEFAULT 'UNCATEGORIZED', " +
                            "floor_price DECIMAL(12,4) NOT NULL, " +
                            "ceiling_price DECIMAL(12,4) NOT NULL, " +
                            "volume_24h INT DEFAULT 0, " +
                            "last_update TIMESTAMP DEFAULT CURRENT_TIMESTAMP" +
                            ")");

            String autoInc = dbType.equals("SQLITE") ? "INTEGER PRIMARY KEY AUTOINCREMENT" :
                             (dbType.equals("MYSQL") ? "BIGINT AUTO_INCREMENT PRIMARY KEY" : "BIGSERIAL PRIMARY KEY");

            conn.createStatement().executeUpdate(
                    "CREATE TABLE IF NOT EXISTS market_transactions (" +
                            "id " + autoInc + ", " +
                            "player_uuid CHAR(36) NOT NULL, " +
                            "player_name VARCHAR(48) NOT NULL DEFAULT 'Unknown', " +
                            "item_id VARCHAR(64) NOT NULL, " +
                            "type VARCHAR(4) CHECK (type IN ('BUY', 'SELL')) NOT NULL, " +
                            "amount INT NOT NULL, " +
                            "unit_price DECIMAL(12,4) NOT NULL, " +
                            "total_price DECIMAL(14,4) NOT NULL, " +
                            "tax_applied DECIMAL(14,4) NOT NULL, " +
                            "timestamp TIMESTAMP DEFAULT CURRENT_TIMESTAMP" +
                            ")");
            conn.createStatement().executeUpdate("CREATE INDEX IF NOT EXISTS idx_player ON market_transactions(player_uuid)");
            conn.createStatement().executeUpdate("CREATE INDEX IF NOT EXISTS idx_item ON market_transactions(item_id)");
            conn.createStatement().executeUpdate("CREATE INDEX IF NOT EXISTS idx_timestamp ON market_transactions(timestamp)");

            conn.createStatement().executeUpdate(
                    "CREATE TABLE IF NOT EXISTS player_shares (" +
                            "player_uuid CHAR(36) NOT NULL, " +
                            "item_id VARCHAR(64) NOT NULL, " +
                            "amount INT NOT NULL DEFAULT 0, " +
                            "PRIMARY KEY (player_uuid, item_id)" +
                            ")");

            logger.fine("Database tables verified/created.");
        }
    }

    private void scheduleReconnect() {
        if (reconnectAttempts >= MAX_RECONNECT_ATTEMPTS) {
            logger.severe("Max reconnection attempts reached. Database unavailable.");
            return;
        }
        reconnectAttempts++;
        logger.info(String.format("Attempting to reconnect in %d seconds (attempt %d/%d)...",
                RECONNECT_DELAY_MS / 1000, reconnectAttempts, MAX_RECONNECT_ATTEMPTS));

        CompletableFuture.delayedExecutor(RECONNECT_DELAY_MS, java.util.concurrent.TimeUnit.MILLISECONDS)
                .execute(() -> {
                    try {
                        initialize(RootsSkyMarket.getInstance());
                        if (connected) {
                            reconnectAttempts = 0;
                        }
                    } catch (Exception e) {
                        scheduleReconnect();
                    }
                });
    }

    public CompletableFuture<Map<String, MarketItem>> loadAllPrices() {
        return CompletableFuture.supplyAsync(() -> {
            Map<String, MarketItem> items = new HashMap<>();
            if (!ensureConnected()) return items;

            String sql = "SELECT item_id, base_price, current_price, alpha, category, floor_price, ceiling_price, volume_24h, last_update FROM market_prices";
            try (Connection conn = dataSource.getConnection();
                 PreparedStatement stmt = conn.prepareStatement(sql);
                 ResultSet rs = stmt.executeQuery()) {

                while (rs.next()) {
                    MarketItem item = new MarketItem(
                            rs.getString("item_id"),
                            rs.getBigDecimal("base_price"),
                            rs.getBigDecimal("current_price"),
                            rs.getBigDecimal("alpha"),
                            rs.getString("category"),
                            rs.getBigDecimal("floor_price"),
                            rs.getBigDecimal("ceiling_price"),
                            rs.getInt("volume_24h"),
                            rs.getTimestamp("last_update").toInstant()
                    );
                    items.put(item.getItemId(), item);
                }
                logger.info("Loaded " + items.size() + " items from database.");
            } catch (SQLException e) {
                logger.log(Level.SEVERE, "Failed to load prices from database", e);
            }
            return items;
        }, executor);
    }

    public CompletableFuture<Void> savePrice(MarketItem item) {
        return CompletableFuture.runAsync(() -> {
            if (!ensureConnected()) return;

            String sql;
            if (dbType.equals("MYSQL")) {
                sql = "INSERT INTO market_prices (item_id, base_price, current_price, alpha, category, floor_price, ceiling_price, volume_24h, last_update) " +
                      "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?) " +
                      "ON DUPLICATE KEY UPDATE base_price=VALUES(base_price), current_price=VALUES(current_price), " +
                      "alpha=VALUES(alpha), category=VALUES(category), floor_price=VALUES(floor_price), " +
                      "ceiling_price=VALUES(ceiling_price), volume_24h=VALUES(volume_24h), last_update=VALUES(last_update)";
            } else {
                sql = "INSERT INTO market_prices (item_id, base_price, current_price, alpha, category, floor_price, ceiling_price, volume_24h, last_update) " +
                      "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?) " +
                      "ON CONFLICT (item_id) DO UPDATE SET base_price=EXCLUDED.base_price, current_price=EXCLUDED.current_price, " +
                      "alpha=EXCLUDED.alpha, category=EXCLUDED.category, floor_price=EXCLUDED.floor_price, " +
                      "ceiling_price=EXCLUDED.ceiling_price, volume_24h=EXCLUDED.volume_24h, last_update=EXCLUDED.last_update";
            }
            try (Connection conn = dataSource.getConnection();
                 PreparedStatement stmt = conn.prepareStatement(sql)) {

                Timestamp now = Timestamp.from(Instant.now());
                int idx = 1;
                stmt.setString(idx++, item.getItemId());
                stmt.setBigDecimal(idx++, item.getBasePrice());
                stmt.setBigDecimal(idx++, item.getCurrentPrice());
                stmt.setBigDecimal(idx++, item.getAlpha());
                stmt.setString(idx++, item.getCategory());
                stmt.setBigDecimal(idx++, item.getFloorPrice());
                stmt.setBigDecimal(idx++, item.getCeilingPrice());
                stmt.setInt(idx++, item.getVolume24h());
                stmt.setTimestamp(idx++, now);
                stmt.executeUpdate();
            } catch (SQLException e) {
                logger.log(Level.SEVERE, "Failed to save price for item: " + item.getItemId(), e);
            }
        }, executor);
    }

    public CompletableFuture<Void> logTransaction(TransactionData data) {
        return CompletableFuture.runAsync(() -> {
            if (!ensureConnected()) return;

            String sql = "INSERT INTO market_transactions (player_uuid, player_name, item_id, type, amount, unit_price, total_price, tax_applied, timestamp) " +
                    "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)";
            try (Connection conn = dataSource.getConnection();
                 PreparedStatement stmt = conn.prepareStatement(sql)) {

                stmt.setString(1, data.getPlayerUuid().toString());
                stmt.setString(2, data.getPlayerName() != null ? data.getPlayerName() : "Unknown");
                stmt.setString(3, data.getItemId());
                stmt.setString(4, data.getType().name());
                stmt.setInt(5, data.getAmount());
                stmt.setBigDecimal(6, data.getUnitPrice());
                stmt.setBigDecimal(7, data.getTotalPrice());
                stmt.setBigDecimal(8, data.getTaxApplied());
                stmt.setTimestamp(9, Timestamp.from(data.getTimestamp()));
                stmt.executeUpdate();
            } catch (SQLException e) {
                logger.log(Level.SEVERE, "Failed to log transaction: " + data, e);
            }
        }, executor);
    }

    public CompletableFuture<Void> savePricesBatch(java.util.Collection<MarketItem> items) {
        return CompletableFuture.runAsync(() -> {
            if (items.isEmpty()) return;
            if (!ensureConnected()) return;

            String sql;
            if (dbType.equals("MYSQL")) {
                sql = "INSERT INTO market_prices (item_id, base_price, current_price, alpha, category, floor_price, ceiling_price, volume_24h, last_update) " +
                      "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?) " +
                      "ON DUPLICATE KEY UPDATE base_price=VALUES(base_price), current_price=VALUES(current_price), " +
                      "alpha=VALUES(alpha), category=VALUES(category), floor_price=VALUES(floor_price), " +
                      "ceiling_price=VALUES(ceiling_price), volume_24h=VALUES(volume_24h), last_update=VALUES(last_update)";
            } else {
                sql = "INSERT INTO market_prices (item_id, base_price, current_price, alpha, category, floor_price, ceiling_price, volume_24h, last_update) " +
                      "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?) " +
                      "ON CONFLICT (item_id) DO UPDATE SET base_price=EXCLUDED.base_price, current_price=EXCLUDED.current_price, " +
                      "alpha=EXCLUDED.alpha, category=EXCLUDED.category, floor_price=EXCLUDED.floor_price, " +
                      "ceiling_price=EXCLUDED.ceiling_price, volume_24h=EXCLUDED.volume_24h, last_update=EXCLUDED.last_update";
            }
            try (Connection conn = dataSource.getConnection();
                 PreparedStatement stmt = conn.prepareStatement(sql)) {

                conn.setAutoCommit(false);
                Timestamp now = Timestamp.from(Instant.now());

                for (MarketItem item : items) {
                    int idx = 1;
                    stmt.setString(idx++, item.getItemId());
                    stmt.setBigDecimal(idx++, item.getBasePrice());
                    stmt.setBigDecimal(idx++, item.getCurrentPrice());
                    stmt.setBigDecimal(idx++, item.getAlpha());
                    stmt.setString(idx++, item.getCategory());
                    stmt.setBigDecimal(idx++, item.getFloorPrice());
                    stmt.setBigDecimal(idx++, item.getCeilingPrice());
                    stmt.setInt(idx++, item.getVolume24h());
                    stmt.setTimestamp(idx++, now);
                    stmt.addBatch();
                }
                stmt.executeBatch();
                conn.commit();
                logger.info("[DatabaseManager] Saved/updated " + items.size() + " prices in batch.");
            } catch (SQLException e) {
                logger.log(Level.SEVERE, "Failed to save prices in batch", e);
            }
        }, executor);
    }

    public CompletableFuture<Map<String, Integer>> getShares(String playerUuid) {
        return CompletableFuture.supplyAsync(() -> {
            Map<String, Integer> shares = new HashMap<>();
            if (!ensureConnected()) return shares;

            String sql = "SELECT item_id, amount FROM player_shares WHERE player_uuid = ?";
            try (Connection conn = dataSource.getConnection();
                 PreparedStatement stmt = conn.prepareStatement(sql)) {
                stmt.setString(1, playerUuid);
                try (ResultSet rs = stmt.executeQuery()) {
                    while (rs.next()) {
                        shares.put(rs.getString("item_id"), rs.getInt("amount"));
                    }
                }
            } catch (SQLException e) {
                logger.log(Level.SEVERE, "Failed to get shares for player: " + playerUuid, e);
            }
            return shares;
        }, executor);
    }

    public CompletableFuture<Void> updateShares(String playerUuid, String itemId, int amount) {
        return CompletableFuture.runAsync(() -> {
            if (!ensureConnected()) return;

            String sql;
            if (dbType.equals("MYSQL")) {
                sql = "INSERT INTO player_shares (player_uuid, item_id, amount) VALUES (?, ?, ?) " +
                      "ON DUPLICATE KEY UPDATE amount = VALUES(amount)";
            } else {
                sql = "INSERT INTO player_shares (player_uuid, item_id, amount) VALUES (?, ?, ?) " +
                      "ON CONFLICT (player_uuid, item_id) DO UPDATE SET amount = EXCLUDED.amount";
            }
            try (Connection conn = dataSource.getConnection();
                 PreparedStatement stmt = conn.prepareStatement(sql)) {
                stmt.setString(1, playerUuid);
                stmt.setString(2, itemId);
                stmt.setInt(3, amount);
                stmt.executeUpdate();
            } catch (SQLException e) {
                logger.log(Level.SEVERE, "Failed to update shares for player: " + playerUuid, e);
            }
        }, executor);
    }
    
    public CompletableFuture<Map<String, Map<String, Integer>>> getAllSharesGlobal() {
        return CompletableFuture.supplyAsync(() -> {
            Map<String, Map<String, Integer>> allShares = new HashMap<>();
            if (!ensureConnected()) return allShares;

            String sql = "SELECT player_uuid, item_id, amount FROM player_shares WHERE amount > 0";
            try (Connection conn = dataSource.getConnection();
                 PreparedStatement stmt = conn.prepareStatement(sql);
                 ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    String uuid = rs.getString("player_uuid");
                    String itemId = rs.getString("item_id");
                    int amount = rs.getInt("amount");
                    
                    allShares.computeIfAbsent(uuid, k -> new HashMap<>()).put(itemId, amount);
                }
            } catch (SQLException e) {
                logger.log(Level.SEVERE, "Failed to get all shares globally", e);
            }
            return allShares;
        }, executor);
    }

    public CompletableFuture<Map<String, Integer>> getSoldAmountsToday(String playerUuid) {
        return CompletableFuture.supplyAsync(() -> {
            Map<String, Integer> amounts = new HashMap<>();
            if (!ensureConnected()) return amounts;

            String sql = "SELECT item_id, SUM(amount) as total FROM market_transactions " +
                    "WHERE player_uuid = ? AND type = 'SELL' AND timestamp >= CURRENT_DATE " +
                    "GROUP BY item_id";
            try (Connection conn = dataSource.getConnection();
                 PreparedStatement stmt = conn.prepareStatement(sql)) {
                stmt.setString(1, playerUuid);
                try (ResultSet rs = stmt.executeQuery()) {
                    while (rs.next()) {
                        amounts.put(rs.getString("item_id"), rs.getInt("total"));
                    }
                }
            } catch (SQLException e) {
                logger.log(Level.SEVERE, "Failed to get daily limits for player: " + playerUuid, e);
            }
            return amounts;
        }, executor);
    }

    private boolean ensureConnected() {
        if (connected) return true;
        logger.warning("Database not connected. Attempting reconnect...");
        try {
            initialize(RootsSkyMarket.getInstance());
        } catch (Exception e) {
            logger.severe("Reconnection failed: " + e.getMessage());
        }
        return connected;
    }

    public CompletableFuture<Void> resetAllPricesAndTransactions() {
        return CompletableFuture.runAsync(() -> {
            if (!ensureConnected()) return;
            try (Connection conn = dataSource.getConnection()) {
                conn.setAutoCommit(false);
                try (Statement stmt = conn.createStatement()) {
                    stmt.executeUpdate("DELETE FROM market_transactions");
                    stmt.executeUpdate("DELETE FROM market_prices");
                    stmt.executeUpdate("DELETE FROM player_shares");
                    conn.commit();
                    logger.info("Database reset: All market prices, transactions, and shares deleted.");
                } catch (SQLException e) {
                    conn.rollback();
                    throw e;
                } finally {
                    conn.setAutoCommit(true);
                }
            } catch (SQLException e) {
                logger.log(Level.SEVERE, "Failed to reset database tables", e);
            }
        }, executor);
    }

    public void shutdown() {
        logger.info("Shutting down database manager...");
        if (dataSource != null && !dataSource.isClosed()) {
            dataSource.close();
        }
        executor.shutdownNow();
        connected = false;
    }

    public boolean isConnected() {
        return connected;
    }
}