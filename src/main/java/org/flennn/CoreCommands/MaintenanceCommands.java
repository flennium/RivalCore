package org.flennn.CoreCommands;

import com.velocitypowered.api.command.CommandManager;
import com.velocitypowered.api.command.CommandSource;
import com.velocitypowered.api.command.SimpleCommand;
import com.velocitypowered.api.proxy.ProxyServer;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.flennn.Config.ConfigManager;
import org.flennn.CoreManager.Utils;

import java.util.List;

public class MaintenanceCommands implements SimpleCommand {

    private final ProxyServer proxyServer;
    private final ConfigManager configManager;

    public MaintenanceCommands(ProxyServer proxyServer, ConfigManager configManager) {
        this.proxyServer = proxyServer;
        this.configManager = configManager;
    }

    @Override
    public void execute(Invocation invocation) {
        CommandSource source = invocation.source();
        String[] args = invocation.arguments();

        if (!source.hasPermission("rivalcore.admin")) {
            source.sendMessage(Component.text("❌ You do not have permission to use this command.", NamedTextColor.RED));
            return;
        }

        if (args.length == 0) {
            source.sendMessage(Utils.mm("<green>Usage:</green> /maintenance <enable/disable> - To manage maintenance mode."));
            return;
        }

        switch (args[0].toLowerCase()) {
            case "enable":
                enableMaintenanceMode();
                source.sendMessage(Utils.mm("<green>✅</green> Maintenance mode <yellow>enabled</yellow>!"));
                break;

            case "disable":
                disableMaintenanceMode();
                source.sendMessage(Utils.mm("<green>✅</green> Maintenance mode <red>disabled</red>!"));
                break;
            default:
                source.sendMessage(Utils.mm("<red>❌</red> Invalid command. Usage: /maintenance <enable/disable>"));
                break;
        }
    }

    private void enableMaintenanceMode() {
        configManager.setMaintenanceMode(true);
        updateMOTD();
        proxyServer.getAllPlayers().forEach(player -> {
            boolean isBypassed = configManager.getBypassPlayers().stream()
                    .anyMatch(bypassPlayer -> bypassPlayer.equalsIgnoreCase(player.getUsername()));

            if (!isBypassed) {
                player.disconnect(Utils.mm(configManager.getMaintenanceModeMessage()));
            }
        });
    }

    private void disableMaintenanceMode() {
        configManager.setMaintenanceMode(false);
        updateMOTD();
    }

    private void updateMOTD() {
        String maintencemotd = configManager.getMaintenanceMOTD();
        String normalmotd = configManager.getMaintenanceMOTD();
        if (configManager.isMaintenanceMode()) {
            Component motdComponent = Utils.mm(maintencemotd);
            proxyServer.getAllPlayers().forEach(player -> player.sendMessage(motdComponent));
        } else if (!configManager.isMaintenanceMode()) {
            Component motdComponent = Utils.mm(normalmotd);
            proxyServer.getAllPlayers().forEach(player -> player.sendMessage(motdComponent));
        }
    }

    @Override
    public List<String> suggest(Invocation invocation) {
        return List.of("enable", "disable");
    }


    public void register(CommandManager commandManager, Object plugin) {
        commandManager.register(
                commandManager.metaBuilder("maintenance")
                        .plugin(plugin)
                        .build(),
                this
        );
    }
}
