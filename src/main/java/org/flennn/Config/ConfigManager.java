package org.flennn.Config;

import com.velocitypowered.api.plugin.annotation.DataDirectory;
import com.velocitypowered.api.proxy.ProxyServer;
import org.flennn.CoreManager.Utils;
import org.yaml.snakeyaml.DumperOptions;
import org.yaml.snakeyaml.Yaml;

import javax.inject.Inject;
import java.io.IOException;
import java.io.InputStream;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;

public class ConfigManager {
    private final Logger logger;
    private final Path configPath;
    private final Yaml yaml;
    private final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();
    private Map<String, Object> config;

    @Inject
    public ConfigManager(ProxyServer proxyServer, Logger logger, @DataDirectory Path dataFolder) {
        this.logger = logger;
        this.configPath = dataFolder.resolve("config.yml");

        DumperOptions options = new DumperOptions();
        options.setIndent(2);
        options.setPrettyFlow(true);
        options.setDefaultFlowStyle(DumperOptions.FlowStyle.BLOCK);

        this.yaml = new Yaml(options);
        initialize();
    }

    private void initialize() {
        try {
            loadConfig();
            startAutoReload();
        } catch (Exception e) {
            Utils.Log.severe("❌ Failed to initialize configuration: " + e.getMessage());

            handleCriticalError();
        }
    }

    private synchronized void loadConfig() throws IOException {
        Files.createDirectories(configPath.getParent());

        if (!Files.exists(configPath)) {
            try (InputStream is = getClass().getResourceAsStream("/config.yml")) {
                if (is != null) {
                    Files.copy(is, configPath, StandardCopyOption.REPLACE_EXISTING);
                }
            }
        }

        try (InputStream inputStream = Files.newInputStream(configPath)) {
            config = yaml.load(inputStream);
        }

        if (config == null) {
            config = new HashMap<>();
        }

        validateConfig();
        Utils.Log.info("✅ Configuration loaded successfully.");
    }

    public synchronized void saveConfig() {
        try (Writer writer = Files.newBufferedWriter(configPath)) {
            yaml.dump(config, writer);
            Utils.Log.info("✅ Configuration saved successfully.");
        } catch (IOException e) {
            Utils.Log.severe("❌ Failed to save config: " + e.getMessage());
        }
    }


    public synchronized void reloadConfig() {
        try {
            loadConfig();
            Utils.Log.info("✅ Configuration reloaded");
        } catch (IOException e) {
            Utils.Log.severe("❌ Config Reload failed: " + e.getMessage());
        }
    }

    private void validateConfig() {
        Map<String, Object> core = getSection("core");
        if (core == null) {
            Utils.Log.severe("❌ Missing 'core' section in config.yml");
        }

        Map<String, Object> discord = getSection("core.discord");
        if (discord == null || !discord.containsKey("bot-token") || discord.get("bot-token").toString().isEmpty()) {
            Utils.Log.severe("❌ Discord bot token is required");
        }
    }

    public void startAutoReload() {
        scheduler.scheduleAtFixedRate(this::reloadConfig, 30, 30, TimeUnit.MINUTES);
    }

    public void shutdown() {
        scheduler.shutdown();
        try {
            if (!scheduler.awaitTermination(5, TimeUnit.SECONDS)) {
                scheduler.shutdownNow();
            }
            saveConfig();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    // =======================
    // Configuration Getters
    // =======================

    public String getBotToken() {
        return getString("core.discord.bot-token", "");
    }

    public void setBotToken(String token) {
        set("core.discord.bot-token", token);
    }

    public String getLogsGuildID() {
        return getString("core.discord.logger-guildid", "");
    }

    public String getRivalGuildID() {
        return getString("core.discord.rival-guildid", "");
    }

    public String getRivalStaffGuildID() {
        return getString("core.discord.rivalstaff-guildid", "");
    }

    public String getSecurityWebhook() {
        return getString("core.discord.security-webhook", "");
    }

    public String getDbHost() {
        return getString("core.mysql.host", "localhost");
    }

    public String getDbPort() {
        return getString("core.mysql.port", "3306");
    }

    public String getDbName() {
        return getString("core.mysql.database", "rivalcore");
    }

    public String getDbUser() {
        return getString("core.mysql.user", "root");
    }

    public String getDbPassword() {
        return getString("core.mysql.password", "");
    }

    public String getMOTD() {
        List<String> lines = getList("core.motd");
        return String.join("\n", lines);
    }


    public void setMOTD(String motd) {
        set("core.motd", motd);
    }

    public boolean isMaintenanceMode() {
        return getBoolean("core.maintenance-mode.status", false);
    }

    public void setMaintenanceMode(boolean status) {
        set("core.maintenance-mode.status", status);
    }

    public String getMaintenanceMOTD() {
        List<String> lines = getList("core.maintenance-mode.motd");
        return String.join("\n", lines);
    }

    public List<String> getBypassPlayers() {
        return getList("core.maintenance-mode.bypass-players");
    }

    public String getStaffChatPrefix() {
        return getString("core.messages.staff-chat-prefix", "[CoreStaff]");
    }

    public String getPrefix() {
        return getString("core.messages.prefix", "[Core]");
    }

    public String getHubLobbyName() {
        return getString("core.hub-lobby-name", "hub");
    }

    public String getMaintenanceModeMessage() {
        return getString("core.messages.maintenance-mode-message", "Server is in maintenance mode.");
    }

    public String getMaintenanceVersionMessage() {
        return getString("core.messages.maintenance-version-message", "<bold><dark_red>Maintenance</dark_red></bold>");
    }

    public String getAlertPrefix() {
        return getString("core.messages.alert-prefix", "<bold><green>ANNOUNCEMENT</green> <dark_gray>|</dark_gray> <reset>");
    }

    public String getAlertTitle() {
        return getString("core.messages.alert-title", "<bold><green>ANNOUNCEMENT</green><reset>");
    }

    public Boolean IsLeaderboard() {
        return getBoolean("core.discord.leaderboard", false);
    }

    public Boolean IsLogger() {
        return getBoolean("core.discord.logger", false);
    }

    public String getLeaderboardCategoryName() {
        return getString("core.messages.leaderboard-category-name", "◂〣 LEADERBOARDS 〣▸");
    }

    public List<String> getMaintenanceMotdDescription() {
        return getList("core.maintenance-motd-description");
    }


    public List<String> getMotdDescription() {
        return getList("core.motd-description");
    }

    public int getMaxPlayers() {
        return getInt("core.max-players", 100);
    }

    public int getFakeOnlinePlayers() {
        return getInt("core.fake-online-players", 0);
    }


    public void addBypassPlayer(String player) {
        List<String> players = getBypassPlayers();
        if (!players.contains(player)) {
            players.add(player);
            set("core.maintenance-mode.bypass-players", players);
        }
    }

    public void removeBypassPlayer(String player) {
        List<String> players = getBypassPlayers();
        if (players.remove(player)) {
            set("core.maintenance-mode.bypass-players", players);
        }
    }

    // =======================
    // Helper Methods
    // =======================

    @SuppressWarnings("unchecked")
    public Map<String, Object> getSection(String key) {
        Object value = getValue(key);
        return (value instanceof Map) ? (Map<String, Object>) value : new HashMap<>();
    }

    private int getInt(String key, int defaultValue) {
        Object value = getValue(key);
        return value != null ? Integer.parseInt(value.toString()) : defaultValue;
    }

    private String getString(String key, String defaultValue) {
        Object value = getValue(key);
        return value != null ? value.toString() : defaultValue;
    }

    private boolean getBoolean(String key, boolean defaultValue) {
        Object value = getValue(key);
        return value != null ? Boolean.parseBoolean(value.toString()) : defaultValue;
    }

    private List<String> getList(String key) {
        Object value = getValue(key);
        return (value instanceof List) ? (List<String>) value : new ArrayList<>();
    }

    private void set(String key, Object value) {
        setValue(key, value);
        saveConfig();
    }

    private Object getValue(String key) {
        String[] keys = key.split("\\.");
        Map<String, Object> section = config;
        for (int i = 0; i < keys.length - 1; i++) {
            section = (Map<String, Object>) section.computeIfAbsent(keys[i], k -> new HashMap<>());
        }
        return section.get(keys[keys.length - 1]);
    }

    private void setValue(String key, Object value) {
        String[] keys = key.split("\\.");
        Map<String, Object> section = config;
        for (int i = 0; i < keys.length - 1; i++) {
            section = (Map<String, Object>) section.computeIfAbsent(keys[i], k -> new HashMap<>());
        }
        section.put(keys[keys.length - 1], value);
    }

    private void handleCriticalError() {
        logger.severe("CRITICAL CONFIG ERROR - Plugin may not function properly");
    }
}
