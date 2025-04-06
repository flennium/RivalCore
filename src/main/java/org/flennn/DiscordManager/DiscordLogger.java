package org.flennn.DiscordManager;

import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.proxy.server.ServerRegisteredEvent;
import com.velocitypowered.api.proxy.ProxyServer;
import com.velocitypowered.api.proxy.server.RegisteredServer;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.entities.channel.concrete.Category;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import org.flennn.CoreManager.Utils;

import java.awt.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;

public class DiscordLogger {
    private final ProxyServer proxy;
    private final Guild guild;
    private final Map<String, ServerChannels> serverChannels = new ConcurrentHashMap<>();
    private final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();
    private final Logger logger;

    public DiscordLogger(ProxyServer proxy, JDA jda, String guildId, Logger logger) {
        this.proxy = proxy;
        this.logger = logger;
        this.guild = jda.getGuildById(guildId);

        if (guild == null) {
            Utils.Log.severe("Invalid logger guild ID provided");
        }

        initialize();
    }

    private void initialize() {
        proxy.getAllServers().forEach(this::setupServerChannels);

        scheduler.scheduleAtFixedRate(this::verifyChannels, 0, 2, TimeUnit.MINUTES);
    }

    @Subscribe
    public void onServerRegistered(ServerRegisteredEvent event) {
        scheduler.execute(() -> setupServerChannels(event.registeredServer()));
    }


    private void verifyChannels() {
        proxy.getAllServers().forEach(this::setupServerChannels);
    }

    private void setupServerChannels(RegisteredServer server) {
        if (server == null || guild == null) return;

        final String serverName = server.getServerInfo().getName();
        List<Category> categories = guild.getCategoriesByName(serverName, true);

        Category existingCategory = categories.stream()
                .filter(category -> category.getName().equalsIgnoreCase(serverName))
                .findFirst()
                .orElse(null);

        if (existingCategory == null) {
            createNewCategory(serverName);
        } else {
            updateExistingCategory(existingCategory, serverName);
        }
    }

    private void createNewCategory(String serverName) {
        guild.createCategory(serverName).queue(category -> {
            TextChannel chatLogs = getOrCreateChannel(category, "chat-logs");
            TextChannel commands = getOrCreateChannel(category, "commands");
            TextChannel joinLeave = getOrCreateChannel(category, "join-leave");

            serverChannels.put(serverName, new ServerChannels(chatLogs, commands, joinLeave));
        }, error -> Utils.Log.warning("Failed to create category for " + serverName));
    }

    private void updateExistingCategory(Category category, String serverName) {
        TextChannel chatLogs = getOrCreateChannel(category, "chat-logs");
        TextChannel commands = getOrCreateChannel(category, "commands");
        TextChannel joinLeave = getOrCreateChannel(category, "join-leave");

        serverChannels.put(serverName, new ServerChannels(chatLogs, commands, joinLeave));
    }

    private TextChannel getOrCreateChannel(Category category, String channelName) {
        return category.getTextChannels().stream()
                .filter(channel -> channel.getName().equalsIgnoreCase(channelName))
                .findFirst()
                .orElseGet(() -> createChannel(category, channelName));
    }

    private TextChannel createChannel(Category category, String name) {
        try {
            return category.createTextChannel(name)
                    .setTopic("Automatically created by RivalMC Logger")
                    .complete();
        } catch (Exception e) {
            Utils.Log.warning("Failed to create channel " + name);
            return null;
        }
    }

    public void log(String serverName, LogType type, String message) {
        ServerChannels channels = serverChannels.get(serverName);
        if (channels == null) {
            Utils.Log.warning("No channels found for server: " + serverName);
            return;
        }

        try {
            TextChannel target = switch (type) {
                case CHAT -> channels.chat;
                case COMMAND -> channels.commands;
                case JOIN_LEAVE -> channels.joinLeave;
            };

            if (target != null) {
                MessageEmbed embed = createEmbed(type, message, serverName);
                target.sendMessageEmbeds(embed).queue();
            }
        } catch (Exception e) {
            Utils.Log.warning("Failed to send Discord log: " + serverName);
        }
    }

    private MessageEmbed createEmbed(LogType type, String message, String serverName) {
        EmbedBuilder builder = new EmbedBuilder();
        String[] parts = message.split("\n", 2);
        String title = parts[0];
        String description = parts.length > 1 ? parts[1] : "";

        String decoratedDescription = "\n✨ " + description.replace("```diff\n", "📜 **Log Details:**\n```diff\n")
                + "\n" + getRandomCreativeSymbol();

        switch (type) {
            case CHAT:
                builder.setTitle("💬 " + title)
                        .setColor(Color.BLUE)
                        .setDescription(decoratedDescription);
                break;

            case COMMAND:
                builder.setTitle("⚡ " + title)
                        .setColor(Color.ORANGE)
                        .setDescription("🔧 Command Executed:\n" + decoratedDescription);
                break;

            case JOIN_LEAVE:
                boolean isJoin = title.contains("joined");
                builder.setTitle(isJoin ? "🚪 " + title : "🚶 " + title)
                        .setColor(isJoin ? Color.GREEN : Color.RED)
                        .setDescription(decoratedDescription);
                break;
        }
        builder.setFooter("🏰 Server: " + serverName + " • ⏰ " +
                        LocalDateTime.now().format(DateTimeFormatter.ofPattern("MMM dd yyyy HH:mm")),
                guild.getIconUrl());

        return builder.build();
    }

    private String getRandomCreativeSymbol() {
        String[] symbols = {
                "▃▃▃▃▃▃▃▃▃▃▃▃▃▃▃▃▃▃▃▃▃▃▃▃▃▃▃",
                "✧･ﾟ: *✧･ﾟ:*  *:･ﾟ✧*:･ﾟ✧ﾟ✧*ﾟ",
                "✦ ✦ ✦ ✦ ✦ ✦ ✦ ✦ ✦ ✦ ✦ ✦",
                "・ﾟ✧ ・ﾟ✧ ・ﾟ✧ ・ﾟ✧ ・ﾟ✧・ﾟ✧",
                "♡ ♡ ♡ ♡ ♡ ♡ ♡ ♡ ♡ ♡ ♡ ♡ ♡ "
        };
        return symbols[new Random().nextInt(symbols.length)];
    }

    public enum LogType {
        CHAT, COMMAND, JOIN_LEAVE
    }

    private record ServerChannels(TextChannel chat, TextChannel commands, TextChannel joinLeave) {
    }
}
