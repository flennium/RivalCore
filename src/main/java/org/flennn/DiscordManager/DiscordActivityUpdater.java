package org.flennn.DiscordManager;

import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.connection.PostLoginEvent;
import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.ProxyServer;
import net.dv8tion.jda.api.events.session.ReadyEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.entities.Activity;
import org.flennn.Config.ConfigManager;
import org.flennn.CoreDatabase.DatabaseManager;
import org.flennn.CoreManager.Utils;
import org.flennn.RivalCore;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

public class DiscordActivityUpdater extends ListenerAdapter {

    private final ProxyServer proxyServer;
    private final ConfigManager configManager;
    private final Random random = new Random();
    private final Map<UUID, Long> lastLoginTimes = new ConcurrentHashMap<>();
    private final DatabaseManager databaseManager;

    public DiscordActivityUpdater(ProxyServer proxyServer, ConfigManager configManager, DatabaseManager databaseManager) {
        this.proxyServer = proxyServer;
        this.configManager = configManager;
        this.databaseManager = databaseManager;
    }

    @Override
    public void onReady(ReadyEvent event) {
        updateActivity(event);

        proxyServer.getScheduler().buildTask(RivalCore.getInstance(), () -> updateActivity(event))
                .repeat(30, TimeUnit.SECONDS)
                .schedule();
    }

    private void updateActivity(ReadyEvent event) {
        List<String> statuses = List.of(
                "👥 Online Players: " + proxyServer.getPlayerCount() + " | Join now: RivalMc.net",
                "🌍 Active GameMode: " + getMostPopulatedWorld() + " | Join now: RivalMc.net",
                "🎉 Latest Join: " + getLastJoinedPlayer() + " | Join now: RivalMc.net",
                "⚔️ Deadliest Player: " + getTopKiller() + " | Join now: RivalMc.net"
        );

        String newActivity = statuses.get(random.nextInt(statuses.size()));
        event.getJDA().getPresence().setActivity(Activity.of(Activity.ActivityType.CUSTOM_STATUS, newActivity));
    }

    private String getMostPopulatedWorld() {
        return proxyServer.getAllServers().stream()
                .max(Comparator.comparingInt(s -> s.getPlayersConnected().size()))
                .map(s -> s.getServerInfo().getName())
                .orElse("Unknown");
    }

    private String getTopKiller() {
        String tableName = getRandomServerTable();
        if (tableName == null) {
            return "flennnium";
        }

        String query = "SELECT username FROM " + tableName + " ORDER BY kills DESC LIMIT 1";

        try (Connection conn = databaseManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(query);
             ResultSet rs = stmt.executeQuery()) {

            if (rs.next()) {
                return rs.getString("username");
            }
        } catch (SQLException e) {
            Utils.Log.severe("❌ Failed to fetch top killer from table " + tableName + ": " + e.getMessage());
        }

        return "flennnium";
    }

    private String getRandomServerTable() {
        String query = "SELECT name FROM servers ORDER BY RAND() LIMIT 1";

        try (Connection conn = databaseManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(query);
             ResultSet rs = stmt.executeQuery()) {

            if (rs.next()) {
                return ("stats_" + rs.getString("name"));
            }
        } catch (SQLException e) {
            Utils.Log.severe("❌ Failed to fetch random server table: " + e.getMessage());
        }

        return null;
    }


    @Subscribe
    public void onPlayerJoin(PostLoginEvent event) {
        Player player = event.getPlayer();
        lastLoginTimes.put(player.getUniqueId(), System.currentTimeMillis());
    }

    public String getLastJoinedPlayer() {
        return lastLoginTimes.entrySet().stream()
                .max(Comparator.comparingLong(Map.Entry::getValue))
                .map(entry -> proxyServer.getPlayer(entry.getKey()).map(Player::getUsername).orElse("TechnoKsa"))
                .orElse("TechnoKsa");
    }
}
