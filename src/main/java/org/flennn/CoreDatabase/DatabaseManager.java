package org.flennn.CoreDatabase;

import com.velocitypowered.api.proxy.ProxyServer;
import com.velocitypowered.api.proxy.server.RegisteredServer;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.flennn.Config.ConfigManager;
import org.flennn.CoreManager.Utils;

import java.sql.*;
import java.util.logging.Logger;

public class DatabaseManager {
    private final ConfigManager config;
    private final ProxyServer proxy;
    private HikariDataSource dataSource;

    public DatabaseManager(Logger logger, ConfigManager config, ProxyServer proxy) {
        this.config = config;
        this.proxy = proxy;
        initializeDataSource();
        if (isDatabaseInitialized()) {
            initializeDatabase();
            addAllServersIfNotExists();
        }
    }

    private void initializeDataSource() {
        if (dataSource != null && !dataSource.isClosed()) {
            return;
        }
        
        try {
            HikariConfig hikariConfig = new HikariConfig();
            String jdbcUrl = "jdbc:mysql://" + config.getDbHost() + ":" + config.getDbPort() + "/" + config.getDbName() + "?useSSL=false&autoReconnect=true";

            hikariConfig.setJdbcUrl(jdbcUrl);
            hikariConfig.setUsername(config.getDbUser());
            hikariConfig.setPassword(config.getDbPassword());
            hikariConfig.setDriverClassName("com.mysql.cj.jdbc.Driver");

            hikariConfig.setMaximumPoolSize(20);
            hikariConfig.setMinimumIdle(5);
            hikariConfig.setIdleTimeout(300000);
            hikariConfig.setMaxLifetime(1800000);
            hikariConfig.setConnectionTimeout(3000);
            hikariConfig.setValidationTimeout(1000);

            hikariConfig.setLeakDetectionThreshold(60000);
            hikariConfig.setRegisterMbeans(true);

            hikariConfig.addDataSourceProperty("cachePrepStmts", "true");
            hikariConfig.addDataSourceProperty("prepStmtCacheSize", "500");
            hikariConfig.addDataSourceProperty("prepStmtCacheSqlLimit", "2048");
            hikariConfig.addDataSourceProperty("useServerPrepStmts", "true");
            hikariConfig.addDataSourceProperty("rewriteBatchedStatements", "true");

            this.dataSource = new HikariDataSource(hikariConfig);
            Utils.Log.info("✅ Database connection pool initialized!");

        } catch (Exception e) {
            Utils.Log.severe("❌ Failed to initialize database connection: " + e.getMessage());
            e.printStackTrace();
        }
    }


    private void initializeDatabase() {
        String[] tableStatements = {
                "CREATE TABLE IF NOT EXISTS servers (id INT AUTO_INCREMENT PRIMARY KEY, name VARCHAR(50) UNIQUE NOT NULL, stats_enabled BOOLEAN DEFAULT TRUE);",
                "CREATE TABLE IF NOT EXISTS leaderboard_messages (server_name VARCHAR(50) PRIMARY KEY, message_id TEXT NOT NULL);",
                "CREATE TABLE IF NOT EXISTS staff_chat_toggle (player_name VARCHAR(50) PRIMARY KEY, enabled BOOLEAN DEFAULT FALSE);"
        };

        try (Connection conn = getConnection(); Statement stmt = conn.createStatement()) {
            for (String sql : tableStatements) {
                stmt.executeUpdate(sql);
            }
            Utils.Log.info("✅ Database tables initialized");
        } catch (SQLException e) {
            logSQLException("❌ Failed to initialize database tables", e);
        }
    }

    public boolean isDatabaseInitialized() {
        return dataSource != null && !dataSource.isClosed();
    }

    private boolean serverExists(String serverName) {
        String query = "SELECT 1 FROM servers WHERE name = ? LIMIT 1";
        try (Connection conn = getConnection(); PreparedStatement stmt = conn.prepareStatement(query)) {
            stmt.setString(1, serverName);
            try (ResultSet rs = stmt.executeQuery()) {
                return rs.next();
            }
        } catch (SQLException e) {
            logSQLException("❌ Failed to check server existence", e);
        }
        return false;
    }

    public void insertServer(String serverName, boolean statsEnabled) {
        if (serverExists(serverName)) return;

        String insertSQL = "INSERT INTO servers (name, stats_enabled) VALUES (?, ?)";
        try (Connection conn = getConnection(); PreparedStatement stmt = conn.prepareStatement(insertSQL)) {
            stmt.setString(1, serverName);
            stmt.setBoolean(2, statsEnabled);
            stmt.executeUpdate();
            Utils.Log.info("📁 Server '" + serverName + "' added to the database.");
        } catch (SQLException e) {
            logSQLException("❌ Failed to insert server", e);
        }
    }

    public void addAllServersIfNotExists() {
        if (!isDatabaseInitialized()) {
            Utils.Log.severe("❌ Database is not initialized. Skipping server checks.");
            return;
        }

        proxy.getAllServers().forEach(server -> insertServer(server.getServerInfo().getName(), false));
        Utils.Log.info("✅ Checked and added missing servers.");
    }


    public Connection getConnection() {
        if (dataSource == null) {
            Utils.Log.severe("❌ Database connection pool is not initialized.");
            return null;
        }
        try {
            return dataSource.getConnection();
        } catch (SQLException e) {
            Utils.Log.severe("❌ Failed to obtain a database connection: " + e.getMessage());
            return null;
        }
    }

    public void toggleStaffChat(String playerName) {
        String query = "INSERT INTO staff_chat_toggle (player_name, enabled) VALUES (?, TRUE) ON DUPLICATE KEY UPDATE enabled = NOT enabled";
        try (Connection conn = getConnection(); PreparedStatement stmt = conn.prepareStatement(query)) {
            stmt.setString(1, playerName);
            stmt.executeUpdate();
        } catch (SQLException e) {
            logSQLException("❌ Failed to toggle staff chat", e);
        }
    }

    public Boolean isStaffChatEnabled(String playerName) {
        String query = "SELECT enabled FROM staff_chat_toggle WHERE player_name = ?";
        try (Connection conn = getConnection(); PreparedStatement stmt = conn.prepareStatement(query)) {
            stmt.setString(1, playerName);
            try (ResultSet rs = stmt.executeQuery()) {
                return rs.next() ? rs.getBoolean("enabled") : null;
            }
        } catch (SQLException e) {
            logSQLException("❌ Failed to check staff chat toggle", e);
        }
        return null;
    }

    public void close() {
        if (dataSource != null) {
            dataSource.close();
            Utils.Log.info("✅ Database connection pool closed.");
        }
    }

    private void logSQLException(String message, SQLException e) {
        Utils.Log.severe(message + ": " + e.getMessage());
        for (Throwable t : e) {
            Utils.Log.severe("Caused by: " + t);
        }
    }
}
