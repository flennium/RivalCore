package org.flennn.CoreCommands;

import com.velocitypowered.api.command.CommandManager;
import com.velocitypowered.api.command.CommandSource;
import com.velocitypowered.api.command.SimpleCommand;
import com.velocitypowered.api.proxy.ProxyServer;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.flennn.Config.ConfigManager;

import java.util.List;

public class BypassListCommand implements SimpleCommand {

    private final ConfigManager configManager;
    private final ProxyServer proxyServer;

    public BypassListCommand(ConfigManager configManager, ProxyServer proxyServer) {
        this.configManager = configManager;
        this.proxyServer = proxyServer;
    }

    @Override
    public void execute(Invocation invocation) {
        CommandSource source = invocation.source();

        if (!source.hasPermission("rivalcore.admin")) {
            source.sendMessage(Component.text("❌ You do not have permission to use this command.", NamedTextColor.RED));
            return;
        }

        List<String> bypassPlayers = configManager.getBypassPlayers();

        if (bypassPlayers.isEmpty()) {
            source.sendMessage(Component.text("⚠ No players currently bypass maintenance mode.", NamedTextColor.YELLOW));
            return;
        }

        Component message = Component.text("✅ Players who bypass maintenance mode: ", NamedTextColor.GREEN)
                .append(Component.text(String.join(", ", bypassPlayers), NamedTextColor.GOLD));

        source.sendMessage(message);
    }


    public void register(CommandManager commandManager, Object plugin) {
        commandManager.register(
                commandManager.metaBuilder("bypasslist")
                        .aliases("listbypass")
                        .plugin(plugin)
                        .build(),
                this
        );
    }
}
