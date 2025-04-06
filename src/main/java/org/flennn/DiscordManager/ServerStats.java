package org.flennn.DiscordManager;

import com.velocitypowered.api.proxy.ProxyServer;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import org.flennn.Config.ConfigManager;

import java.awt.*;
import java.text.NumberFormat;
import java.util.Locale;
import java.util.stream.Collectors;

public class ServerStats extends ListenerAdapter {

    private final ProxyServer server;
    private final ConfigManager configManager;
    NumberFormat numberFormat = NumberFormat.getInstance(Locale.US);

    public ServerStats(ProxyServer server, ConfigManager configManager) {
        this.server = server;
        this.configManager = configManager;
    }

    @Override
    public void onMessageReceived(MessageReceivedEvent event) {
        if (event.getAuthor().isBot()) return;

        if (!event.getGuild().getId().equals(configManager.getRivalGuildID()) &&
                !event.getGuild().getId().equals(configManager.getRivalStaffGuildID()) &&
                !event.getGuild().getId().equals(configManager.getLogsGuildID()))
            return;

        if (!event.getMessage().getContentRaw().equalsIgnoreCase("mc") ||
                (!event.getChannel().getName().contains("commands") &&
                        !event.getChannel().getName().contains("cmd")))
            return;


        EmbedBuilder embed = new EmbedBuilder()
                .setColor(new Color(255, 140, 0))
                .setDescription(
                        "- **Java IP:**\n```RivalMc.net```\n" +
                                "- **Bedrock IP:**\n```RivalMc.net:19132```\n" +
                                "- **RivalMC <:top1:1113722386789957663>**\n" +
                                "╰ <:online:1098540778311139379> Online \n" +
                                "╰ <:player:1165296512826355793> ``" + numberFormat.format(server.getPlayerCount()) + "``\n\n" +
                                getServerList()
                )
                .setFooter("Requested by: " + event.getAuthor().getName(), event.getAuthor().getAvatarUrl());

        event.getChannel().sendMessageEmbeds(embed.build()).queue();
    }


    private String getServerList() {
        String serverInfo = server.getAllServers().stream()
                .map(s -> String.format("- <:online:1098540778311139379> **%s**: ``%d``", s.getServerInfo().getName(), s.getPlayersConnected().size()))
                .collect(Collectors.joining("\n"));

        return serverInfo.isEmpty() ? "No active servers" : serverInfo;
    }
}
