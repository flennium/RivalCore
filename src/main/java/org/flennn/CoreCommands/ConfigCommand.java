package org.flennn.CoreCommands;

import com.velocitypowered.api.command.CommandManager;
import com.velocitypowered.api.command.CommandSource;
import com.velocitypowered.api.command.SimpleCommand;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.flennn.Config.ConfigManager;

public class ConfigCommand implements SimpleCommand {
    private final ConfigManager configManager;

    public ConfigCommand(ConfigManager configManager) {
        this.configManager = configManager;
    }

    @Override
    public void execute(Invocation invocation) {
        CommandSource source = invocation.source();

        if (!source.hasPermission("rivalcore.*")) {
            source.sendMessage(Component.text("❌ You do not have permission to use this command.", NamedTextColor.RED));
            return;
        }

        source.sendMessage(MiniMessage.miniMessage().deserialize(buildConfigInfo()));
    }

    private String buildConfigInfo() {
        return """
                <gray>┏━━━━━━━━ <green><bold>CONFIG INFO</bold></green> ━━━━━━━━┓</gray>
                <gray>┃</gray> <gold>Logger Enabled:</gold> <yellow>%s</yellow>
                <gray>┃</gray> <gold>Leaderboard Enabled:</gold> <yellow>%s</yellow>
                <gray>┃</gray> <gold>Hub Lobby Name:</gold> <yellow>%s</yellow>
                <gray>┃</gray> <gold>Maintenance Mode:</gold> <yellow>%s</yellow>
                <gray>┗━━━━━━━━━━━━━━━━━━━━━━━━━━━┛</gray>
                """.formatted(
                configManager.IsLogger() ? "✅" : "❌",
                configManager.IsLeaderboard() ? "✅" : "❌",
                configManager.getHubLobbyName(),
                configManager.isMaintenanceMode() ? "✅" : "❌"
        );
    }


    public void register(CommandManager commandManager, Object plugin) {
        commandManager.register(
                commandManager.metaBuilder("configinfo")
                        .plugin(plugin)
                        .build(),
                this
        );
    }

}
