package org.flennn.DiscordManager;

import com.velocitypowered.api.proxy.ProxyServer;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.channel.concrete.Category;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import org.flennn.Config.ConfigManager;
import org.flennn.CoreDatabase.DatabaseManager;
import org.flennn.CoreManager.Utils;

import java.awt.*;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.text.NumberFormat;
import java.time.Instant;
import java.util.List;
import java.util.Locale;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

public class LeaderboardListener {

    private final JDA jda;
    private final DatabaseManager databaseManager;
    private final String guildId;
    private final String categoryName;
    private final boolean leaderboardEnabled;
    private final ProxyServer proxyServer;
    private final Timer timer;

    public LeaderboardListener(JDA jda, DatabaseManager databaseManager, ConfigManager configManager, ProxyServer proxyServer) {
        this.jda = jda;
        this.databaseManager = databaseManager;
        this.proxyServer = proxyServer;
        this.guildId = configManager.getRivalGuildID();

        this.leaderboardEnabled = configManager.IsLeaderboard();
        this.categoryName = configManager.getLeaderboardCategoryName();

        this.timer = new Timer();


        if (leaderboardEnabled) {
            setupLeaderboard();
            scheduleLeaderboardUpdate();
        } else {
            Utils.Log.info("📛 Leaderboard is disabled in the config.");
        }
    }


    private void setupLeaderboard() {
        Guild guild = jda.getGuildById(guildId);
        if (guild == null) {
            Utils.Log.severe("❌ Guild not found!");
            return;
        }

        Category leaderboardCategory = guild.getCategoriesByName(categoryName, true).stream().findFirst().orElse(null);
        if (leaderboardCategory == null) {
            leaderboardCategory = guild.createCategory(categoryName).complete();
            Utils.Log.info("✅ Created category: " + categoryName);
        }

        for (String serverName : getServerNames()) {
            if (guild.getTextChannelsByName(serverName, true).isEmpty()) {
                guild.createTextChannel(serverName).setParent(leaderboardCategory).complete();
                Utils.Log.info("✅ Created channel: " + serverName);
            }
        }
    }

    private List<String> getServerNames() {
        return proxyServer.getAllServers().stream()
                .map(server -> server.getServerInfo().getName())
                .collect(Collectors.toList());
    }

    private void scheduleLeaderboardUpdate() {
        timer.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                if (leaderboardEnabled) {
                    updateLeaderboards();
                } else {
                    Utils.Log.info("⏸ Leaderboard update skipped (disabled).");
                }
            }
        }, 0, TimeUnit.MINUTES.toMillis(5));
    }

    private void updateLeaderboards() {
        Guild guild = jda.getGuildById(guildId);
        if (guild == null) return;

        for (String serverName : getServerNames()) {
            TextChannel channel = guild.getTextChannelsByName(serverName, true).stream().findFirst().orElse(null);
            if (channel == null) continue;

            String leaderboardContent = fetchLeaderboardData(serverName);
            if (leaderboardContent == null || leaderboardContent.isEmpty()) continue;

            long timestamp = Instant.now().getEpochSecond();

            EmbedBuilder embed = new EmbedBuilder()
                    .setDescription(
                            "**Top 20 Kills:**\n\n" + leaderboardContent +
                                    "\n📊 **Stats updated:** <t:" + timestamp + ":R>"
                    )
                    .setColor(Color.ORANGE)
                    .setTimestamp(Instant.now())
                    .setFooter(serverName);

            String messageId = getStoredMessageId(serverName);
            if (messageId != null) {
                channel.retrieveMessageById(messageId).queue(
                        message -> message.editMessageEmbeds(embed.build()).queue(),
                        failure -> sendNewLeaderboardMessage(channel, embed, serverName)
                );
            } else {
                sendNewLeaderboardMessage(channel, embed, serverName);
            }
        }
    }


    private void sendNewLeaderboardMessage(TextChannel channel, EmbedBuilder embed, String serverName) {
        channel.sendMessageEmbeds(embed.build()).queue(message -> storeMessageId(serverName, message.getId()));
    }

    private void storeMessageId(String serverName, String messageId) {
        String query = "INSERT INTO leaderboard_messages (server_name, message_id) VALUES (?, ?) " +
                "ON DUPLICATE KEY UPDATE message_id = VALUES(message_id)";
        try (Connection conn = databaseManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(query)) {
            stmt.setString(1, serverName);
            stmt.setString(2, messageId);
            stmt.executeUpdate();
        } catch (SQLException e) {
            Utils.Log.severe("❌ Failed to store message ID for " + serverName + ": " + e.getMessage());
        }
    }


    private String getStoredMessageId(String serverName) {
        String query = "SELECT message_id FROM leaderboard_messages WHERE server_name = ?";
        try (Connection conn = databaseManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(query)) {
            stmt.setString(1, serverName);
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                return rs.getString("message_id");
            }
        } catch (SQLException e) {
            Utils.Log.warning("❌ Failed to retrieve message ID for " + serverName + ": " + e.getMessage());
        }
        return null;
    }


    private String fetchLeaderboardData(String serverName) {
        String tableName = "stats_" + serverName;
        String query = "SELECT username, kills FROM " + tableName + " ORDER BY kills DESC LIMIT 20";
        StringBuilder leaderboard = new StringBuilder();

        try (Connection conn = databaseManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(query);
             ResultSet rs = stmt.executeQuery()) {
            NumberFormat formatter = NumberFormat.getInstance(Locale.US);

            int rank = 1;
            while (rs.next()) {
                String rankEmoji = switch (rank) {
                    case 1 -> "🥇";
                    case 2 -> "🥈";
                    case 3 -> "🥉";
                    default -> "#️⃣";
                };
                String formattedKills = formatter.format(rs.getInt("kills"));

                leaderboard.append(rankEmoji)
                        .append(" **").append(rank).append("th** - ")
                        .append(rs.getString("username"))
                        .append(" - `").append(formattedKills).append("`\n");

                rank++;
            }
        } catch (SQLException e) {
            Utils.Log.severe("❌ Failed to fetch leaderboard data from " + tableName + ": " + e.getMessage());
            return null;
        }

        return leaderboard.toString();
    }

}
