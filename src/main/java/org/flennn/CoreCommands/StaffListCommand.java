package org.flennn.CoreCommands;

import com.velocitypowered.api.command.CommandManager;
import com.velocitypowered.api.command.CommandSource;
import com.velocitypowered.api.command.SimpleCommand;
import com.velocitypowered.api.proxy.ProxyServer;
import org.flennn.CoreManager.Utils;

import java.util.List;
import java.util.stream.Collectors;

public class StaffListCommand implements SimpleCommand {
    private final ProxyServer proxy;

    public StaffListCommand(ProxyServer proxy) {
        this.proxy = proxy;
    }

    @Override
    public void execute(Invocation invocation) {
        CommandSource source = invocation.source();

        List<String> staffList = proxy.getAllPlayers().stream()
                .filter(player -> player.hasPermission("rivalcore.staff"))
                .map(player -> "<yellow>• <green>" + player.getUsername() + " <gray>(Server: <gold>" +
                        player.getCurrentServer().map(server -> server.getServerInfo().getName()).orElse("Unknown") +
                        "</gold>)</gray>")
                .collect(Collectors.toList());

        if (staffList.isEmpty()) {
            source.sendMessage(Utils.mm("<red>No staff members are currently online.</red>"));
            return;
        }

        String staffMessage = "<gold>🛠️ Staff Online:</gold>\n" + String.join("\n", staffList);
        source.sendMessage(Utils.mm(staffMessage));
    }

    public void register(CommandManager commandManager, Object plugin) {
        commandManager.register(
                commandManager.metaBuilder("stafflist")
                        .aliases("sl")
                        .plugin(plugin)
                        .build(),
                this
        );
    }
}
