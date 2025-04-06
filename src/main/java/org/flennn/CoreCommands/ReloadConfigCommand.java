package org.flennn.CoreCommands;

import com.velocitypowered.api.command.CommandManager;
import com.velocitypowered.api.command.CommandSource;
import com.velocitypowered.api.command.SimpleCommand;
import com.velocitypowered.api.proxy.ProxyServer;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.flennn.Config.ConfigManager;
import org.flennn.CoreDatabase.DatabaseManager;
import org.flennn.RivalCore;

public class ReloadConfigCommand implements SimpleCommand {
    private final RivalCore plugin;
    private final ConfigManager configManager;
    private final ProxyServer proxyServer;
    private final DatabaseManager databaseManager;

    public ReloadConfigCommand(RivalCore plugin, ConfigManager configManager, ProxyServer proxyServer, DatabaseManager databaseManager) {
        this.plugin = plugin;
        this.configManager = configManager;
        this.proxyServer = proxyServer;
        this.databaseManager = databaseManager;
    }

    @Override
    public void execute(Invocation invocation) {
        CommandSource source = invocation.source();
        if (!source.hasPermission("rivalcore.*")) {
            source.sendMessage(Component.text("❌ You do not have permission to use this command.", NamedTextColor.RED));
            return;
        }

        source.sendMessage(Component.text("🔄 Reloading Core...", NamedTextColor.YELLOW));

        configManager.reloadConfig();

        plugin.restartDiscord();

        databaseManager.close();
        plugin.initDatabase();

        plugin.registerListeners();
        plugin.registerCommands();

        source.sendMessage(Component.text("✅ Core reloaded successfully!", NamedTextColor.GREEN));
    }

    public void register(CommandManager commandManager, Object plugin) {
        commandManager.register(
                commandManager.metaBuilder("reloadconfig")
                        .plugin(plugin)
                        .build(),
                this
        );
    }
}
