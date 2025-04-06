package org.flennn.DiscordManager;

import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.command.CommandExecuteEvent;
import com.velocitypowered.api.event.connection.DisconnectEvent;
import com.velocitypowered.api.event.player.PlayerChatEvent;
import com.velocitypowered.api.event.player.ServerConnectedEvent;
import com.velocitypowered.api.proxy.Player;
import org.flennn.Config.ConfigManager;

import java.net.InetAddress;
import java.time.Duration;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class ActivityListeners {

    private final DiscordLogger discordLogger;
    private final Map<UUID, Instant> joinTimes = new HashMap<>();
    private final ConfigManager configManager;

    public ActivityListeners(DiscordLogger discordLogger, ConfigManager configManager) {
        this.discordLogger = discordLogger;
        this.configManager = configManager;
    }

    @Subscribe
    public void onPlayerJoin(ServerConnectedEvent event) {
        Player player = event.getPlayer();

        if (!configManager.IsLogger()) return;

        joinTimes.put(player.getUniqueId(), Instant.now());

        String serverName = event.getServer().getServerInfo().getName();
        String logMessage = String.format(
                "**%s** joined\n```diff\n" +
                        "+ UUID: %s\n" +
                        "+ IP: %s\n" +
                        "+ Client: %s\n" +
                        "```",
                player.getUsername(),
                player.getUniqueId(),
                getIP(player),
                getClientVersion(player)
        );

        discordLogger.log(serverName, DiscordLogger.LogType.JOIN_LEAVE, logMessage);
    }

    @Subscribe
    public void onCommandExecute(CommandExecuteEvent event) {
        if (!configManager.IsLogger()) return;

        if (event.getCommandSource() instanceof Player player) {
            player.getCurrentServer().ifPresent(serverConnection -> {
                String logMessage = String.format(
                        "**%s** executed command\n```diff\n" +
                                "+ Command: /%s\n" +
                                "+ Server: %s\n" +
                                "+ IP: %s\n" +
                                "+ UUID: %s\n" +
                                "+ Client: %s\n" +
                                "```",
                        player.getUsername(),
                        event.getCommand(),
                        serverConnection.getServerInfo().getName(),
                        getIP(player),
                        player.getUniqueId(),
                        getClientVersion(player)
                );

                discordLogger.log(player.getCurrentServer().get().getServerInfo().getName(), DiscordLogger.LogType.COMMAND, logMessage);
            });
        }
    }

    @Subscribe
    public void onPlayerChat(PlayerChatEvent event) {
        if (!configManager.IsLogger()) return;

        Player player = event.getPlayer();
        player.getCurrentServer().ifPresent(serverConnection -> {
            String logMessage = String.format(
                    "**%s** in %s\n```diff\n" +
                            "+ Message: %s\n" +
                            "+ UUID: %s\n" +
                            "+ IP: %s\n" +
                            "+ Client: %s\n" +
                            "```",
                    player.getUsername(),
                    serverConnection.getServerInfo().getName(),
                    event.getMessage(),
                    player.getUniqueId(),
                    getIP(player),
                    getClientVersion(player)
            );

            discordLogger.log(player.getCurrentServer().get().getServerInfo().getName(), DiscordLogger.LogType.CHAT, logMessage);
        });
    }

    @Subscribe
    public void onPlayerDisconnect(DisconnectEvent event) {
        if (!configManager.IsLogger()) return;

        Player player = event.getPlayer();
        player.getCurrentServer().ifPresent(serverConnection -> {
            Duration duration = Duration.between(
                    joinTimes.getOrDefault(player.getUniqueId(), Instant.now()),
                    Instant.now()
            );

            String logMessage = String.format(
                    "**%s** left\n```diff\n" +
                            "+ Time Connected: %dh %dm %ds\n" +
                            "+ Last Server: %s\n" +
                            "+ UUID: %s\n" +
                            "+ IP: %s\n" +
                            "+ Client: %s\n" +
                            "```",
                    player.getUsername(),
                    duration.toHoursPart(),
                    duration.toMinutesPart(),
                    duration.toSecondsPart(),
                    serverConnection.getServerInfo().getName(),
                    player.getUniqueId(),
                    getIP(player),
                    getClientVersion(player)
            );

            discordLogger.log(player.getCurrentServer().get().getServerInfo().getName(), DiscordLogger.LogType.JOIN_LEAVE, logMessage);
            joinTimes.remove(player.getUniqueId());
        });
    }

    private String getIP(Player player) {
        InetAddress address = player.getRemoteAddress().getAddress();
        return address != null ? address.getHostName() : "Unknown";
    }

    private String getClientVersion(Player player) {
        String version = player.getProtocolVersion().getMostRecentSupportedVersion();
        String brand = player.getGameProfile().getName();

        return "Version: " + (version != null ? version : "Unknown") + ", Brand: " + (brand != null ? brand : "Unknown");
    }

    private String getVPNStatus(String ip) {
        return "Unknown";
    }

}