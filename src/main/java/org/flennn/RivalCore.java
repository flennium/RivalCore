package org.flennn;

import com.velocitypowered.api.command.CommandManager;
import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.proxy.ProxyInitializeEvent;
import com.velocitypowered.api.event.proxy.ProxyShutdownEvent;
import com.velocitypowered.api.plugin.Plugin;
import com.velocitypowered.api.proxy.ProxyServer;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.requests.GatewayIntent;
import net.dv8tion.jda.api.utils.cache.CacheFlag;
import net.luckperms.api.LuckPerms;
import net.luckperms.api.LuckPermsProvider;
import org.flennn.Config.ConfigManager;
import org.flennn.CoreCommands.*;
import org.flennn.CoreDatabase.DatabaseManager;
import org.flennn.CoreManager.*;
import org.flennn.DiscordManager.*;

import javax.inject.Inject;
import java.nio.file.Path;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;

@Plugin(id = "rivalcore", name = "RivalCore", version = "1.0", authors = {"flennn"})
public class RivalCore {

    private final ProxyServer proxyServer;
    private final ConfigManager configManager;
    private final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();
    @Inject
    private Logger logger;
    private LuckPerms luckPerms;
    private JDA jda;
    private DatabaseManager databaseManager;
    private static RivalCore instance;

    private boolean isDatabaseInitialized = false;

    @Inject
    public RivalCore(ProxyServer proxyServer, Logger logger, @com.velocitypowered.api.plugin.annotation.DataDirectory Path dataFolder) {
        this.proxyServer = proxyServer;
        this.logger = logger;

        this.configManager = new ConfigManager(proxyServer, logger, dataFolder);
    }

    @Subscribe
    public void onProxyInitialization(ProxyInitializeEvent event) {
        Utils.Log.info("🔧 Starting RivalMCCore ...");
        instance = this;

        initDatabase();

        Utils.setConfig(configManager);

        try {
            this.luckPerms = LuckPermsProvider.get();
            Utils.setLuckPerms(luckPerms);
        } catch (IllegalStateException e) {
            Utils.Log.severe("❌ Failed to load LuckPerms! Ensure it is installed.");
        }


        if (LaunchDiscord()) {
            jda.addEventListener(new ServerStats(proxyServer, configManager));
            jda.addEventListener(new DiscordActivityUpdater(proxyServer, configManager, databaseManager));
        }


        registerListeners();
        registerCommands();



        Utils.Log.info("✅ Core successfully loaded!");
    }

    public void initDatabase() {
        databaseManager = new DatabaseManager(logger, configManager, proxyServer);
    }

    // Register listeners
    public void registerListeners() {
        proxyServer.getEventManager().register(this, new MaintenanceManager(proxyServer, configManager));
        proxyServer.getEventManager().register(this, new PermissionListener(proxyServer, configManager));
        proxyServer.getEventManager().register(this, new LeaderboardListener(jda, databaseManager, configManager, proxyServer));
        DiscordLogger discordLogger = new DiscordLogger(proxyServer, jda, configManager.getLogsGuildID(), logger);
        proxyServer.getEventManager().register(this, discordLogger);
        proxyServer.getEventManager().register(this, new ActivityListeners(discordLogger, configManager));
        proxyServer.getEventManager().register(this, new StaffChatListener(proxyServer, databaseManager, configManager));
        proxyServer.getEventManager().register(this, new DiscordActivityUpdater(proxyServer, configManager, databaseManager));
        proxyServer.getEventManager().register(this, new ClientBrandListener(proxyServer, configManager));
        proxyServer.getEventManager().register(this, new LevelListener(configManager, databaseManager, proxyServer));
    }

    // Register commands
    public void registerCommands() {
        CommandManager commandManager = proxyServer.getCommandManager();

        new AlertCommand(proxyServer, configManager).register(commandManager, this);
        new MaintenanceCommands(proxyServer, configManager).register(commandManager, this);
        new StatsCommand(proxyServer, databaseManager).register(commandManager, this);
        new ServerStatsCommand(proxyServer, databaseManager).register(commandManager, this);
        new BypassListCommand(configManager, proxyServer).register(commandManager, this);
        new ReloadConfigCommand(this, configManager, proxyServer, databaseManager).register(commandManager, this);
        new StaffChatCommand(proxyServer, databaseManager, configManager).register(commandManager, this);
        new StaffListCommand(proxyServer).register(commandManager, this);
        new HubCommand(proxyServer, configManager).register(commandManager, this);
        new ConfigCommand(configManager).register(commandManager, this);
    }

    public boolean LaunchDiscord() {
        try {
            JDABuilder builder = JDABuilder.create(configManager.getBotToken(), GatewayIntent.getIntents(GatewayIntent.ALL_INTENTS));
            builder.disableCache(CacheFlag.EMOJI, CacheFlag.STICKER, CacheFlag.SCHEDULED_EVENTS);

            jda = builder.build().awaitReady();

            Utils.Log.info("✅ Core Discord-bot successfully loaded!");
            return true;
        } catch (InterruptedException e) {
            Utils.Log.severe("❌ Failed to start Discord bot");
            e.printStackTrace();
        }
        return false;
    }

    public void shutdown() {
        scheduler.shutdown();
        databaseManager.close();
        try {
            if (!scheduler.awaitTermination(5, TimeUnit.SECONDS)) {
                scheduler.shutdownNow();
            }
        } catch (InterruptedException e) {
            scheduler.shutdownNow();
        }
        if (jda != null) {
            jda.shutdown();
        }
    }

    @Subscribe
    public void onProxyShutdown(ProxyShutdownEvent event) {
        shutdown();
    }

    public void restartDiscord() {
        shutdown();
        LaunchDiscord();
    }

    public static RivalCore getInstance() {
        return instance;
    }
}
