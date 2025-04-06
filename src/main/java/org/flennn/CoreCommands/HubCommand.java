package org.flennn.CoreCommands;

import com.velocitypowered.api.command.CommandManager;
import com.velocitypowered.api.command.CommandSource;
import com.velocitypowered.api.command.SimpleCommand;
import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.ProxyServer;
import com.velocitypowered.api.proxy.server.RegisteredServer;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.flennn.Config.ConfigManager;
import org.flennn.CoreManager.Utils;

import java.util.List;
import java.util.Optional;

public class HubCommand implements SimpleCommand {

    private final ProxyServer proxyServer;
    private final MiniMessage miniMessage = MiniMessage.miniMessage();
    private final ConfigManager configManager;

    public HubCommand(ProxyServer proxyServer, ConfigManager configManager) {
        this.proxyServer = proxyServer;
        this.configManager = configManager;
    }

    @Override
    public void execute(Invocation invocation) {
        CommandSource source = invocation.source();

        if (!(source instanceof Player player)) {
            source.sendMessage(Component.text("❌ Only players can use this command!", NamedTextColor.RED));
            return;
        }

        String hubServerName = configManager.getHubLobbyName();

        Optional<RegisteredServer> hubServer = proxyServer.getServer(hubServerName);

        if (hubServer.isPresent()) {
            player.createConnectionRequest(hubServer.get()).fireAndForget();
            player.sendMessage(Utils.mm("<green>✅ Sending you to the hub...</green>"));
        }
    }

    @Override
    public List<String> suggest(Invocation invocation) {
        return List.of();
    }

    public void register(CommandManager commandManager, Object plugin) {
        commandManager.register(
                commandManager.metaBuilder("hub")
                        .aliases("lobby")
                        .plugin(plugin)
                        .build(),
                this
        );
    }
}
