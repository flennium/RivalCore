package org.flennn.CoreCommands;

import com.velocitypowered.api.command.CommandManager;
import com.velocitypowered.api.command.CommandSource;
import com.velocitypowered.api.command.SimpleCommand;
import com.velocitypowered.api.proxy.ProxyServer;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.title.Title;
import org.flennn.Config.ConfigManager;
import org.flennn.CoreManager.Utils;

import java.time.Duration;
import java.util.List;

public class AlertCommand implements SimpleCommand {

    private final ProxyServer proxyServer;
    private final MiniMessage miniMessage = MiniMessage.miniMessage();
    private final ConfigManager configManager;

    public AlertCommand(ProxyServer proxyServer, ConfigManager configManager) {
        this.proxyServer = proxyServer;
        this.configManager = configManager;
    }

    @Override
    public void execute(Invocation invocation) {
        CommandSource source = invocation.source();
        String[] args = invocation.arguments();

        if (args.length < 1) {
            sendUsageMessage(source);
            return;
        }

        if (!hasPermission(source)) {
            source.sendMessage(Component.text("❌ You do not have permission to use this command.", NamedTextColor.RED));
            return;
        }

        String messageWithoutPrefix = String.join(" ", args);

        Component message = miniMessage.deserialize(configManager.getAlertPrefix() + " " + messageWithoutPrefix);

        Component title = miniMessage.deserialize(configManager.getAlertTitle());

        Component subtitle = miniMessage.deserialize(messageWithoutPrefix);

        sendAlertToAllPlayers(title, subtitle, message);


        source.sendMessage(Utils.mm("<green> ✅ Announcement sent to everyone!</green>"));
    }

    private boolean hasPermission(CommandSource source) {
        return source.hasPermission("rivalcore.admin") || source.hasPermission("rivalcore.alert");
    }

    private void sendUsageMessage(CommandSource source) {
        source.sendMessage(miniMessage.deserialize("<red>Usage: /alert <message></red>"));
    }

    private void sendAlertToAllPlayers(Component title, Component subtitle, Component message) {
        Title alertTitle = Title.title(
                title,
                subtitle,
                Title.Times.times(
                        Duration.ofMillis(500),
                        Duration.ofSeconds(3),
                        Duration.ofMillis(500)
                )
        );

        proxyServer.getAllPlayers().forEach(player -> {
            player.showTitle(alertTitle);
            player.sendMessage(message);
        });
    }

    @Override
    public List<String> suggest(Invocation invocation) {
        return List.of();
    }


    public void register(CommandManager commandManager, Object plugin) {
        commandManager.register(
                commandManager.metaBuilder("alert")
                        .aliases("announce")
                        .plugin(plugin)
                        .build(),
                this
        );
    }

}
