package org.flennn.CoreManager;

import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.connection.PostLoginEvent;
import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.ProxyServer;
import com.velocitypowered.api.scheduler.Scheduler;
import net.luckperms.api.LuckPerms;
import net.luckperms.api.LuckPermsProvider;
import net.luckperms.api.model.user.User;
import net.luckperms.api.node.Node;
import net.luckperms.api.node.types.MetaNode;
import org.flennn.Config.ConfigManager;
import org.flennn.CoreDatabase.DatabaseManager;
import org.flennn.RivalCore;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.UUID;

public class LevelListener {

    private final ConfigManager configManager;
    private final DatabaseManager databaseManager;
    private final ProxyServer proxyServer;

    public LevelListener(ConfigManager configManager, DatabaseManager databaseManager, ProxyServer proxyServer) {
        this.configManager = configManager;
        this.databaseManager = databaseManager;
        this.proxyServer = proxyServer;

        startDatabaseCheckTask();
    }

    @Subscribe
    public void onPostLogin(PostLoginEvent event) {
        Player player = event.getPlayer();

        int playerLevel = getPlayerLevel(player);
        setPlayerLevelMeta(player.getUniqueId(), playerLevel);
    }

    private void startDatabaseCheckTask() {
        proxyServer.getScheduler().buildTask(RivalCore.getInstance(), this::checkForLevelUpdates)
                .delay(5L, java.util.concurrent.TimeUnit.SECONDS)
                .repeat(60L, java.util.concurrent.TimeUnit.SECONDS)
                .schedule();
    }

    private void checkForLevelUpdates() {
        try (Connection connection = databaseManager.getConnection()) {
            if (connection == null) {
                System.err.println("Failed to obtain a database connection.");
                return;
            }

            String query = "SELECT uuid, level FROM levels";
            try (PreparedStatement stmt = connection.prepareStatement(query)) {
                ResultSet rs = stmt.executeQuery();
                while (rs.next()) {
                    UUID uuid = UUID.fromString(rs.getString("uuid"));
                    int currentLevel = rs.getInt("level");
                    updateLuckPermsMetaIfNeeded(uuid, currentLevel);
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private void updateLuckPermsMetaIfNeeded(UUID uuid, int currentLevel) {
        LuckPerms luckPerms = LuckPermsProvider.get();
        User user = luckPerms.getUserManager().getUser(uuid);

        if (user != null) {
            int currentMetaLevel = getCurrentMetaLevel(user);
            if (currentMetaLevel != currentLevel) {
                setPlayerLevelMeta(uuid, currentLevel);
            }
        }
    }

    private int getCurrentMetaLevel(User user) {
        for (Node node : user.getNodes()) {
            if (node instanceof MetaNode && "level".equals(((MetaNode) node).getMetaKey())) {
                return Integer.parseInt(((MetaNode) node).getMetaValue());
            }
        }

        return 0;
    }


    private int getPlayerLevel(Player player) {
        String selectQuery = "SELECT level FROM levels WHERE uuid = ?";
        String insertQuery = "INSERT INTO levels (uuid, username, level, xp) VALUES (?, ?, ?, ?)";

        try (Connection connection = databaseManager.getConnection();
             PreparedStatement selectStmt = connection.prepareStatement(selectQuery)) {

            selectStmt.setString(1, player.getUniqueId().toString());
            ResultSet rs = selectStmt.executeQuery();

            if (rs.next()) {
                return rs.getInt("level");
            } else {
                try (PreparedStatement insertStmt = connection.prepareStatement(insertQuery)) {
                    insertStmt.setString(1, player.getUniqueId().toString());
                    insertStmt.setString(2, player.getUsername());
                    insertStmt.setInt(3, 0);
                    insertStmt.setDouble(4, 0.0);
                    insertStmt.executeUpdate();
                }

                return 0;
            }

        } catch (Exception e) {
            e.printStackTrace();
        }

        return 0;
    }

    private void setPlayerLevelMeta(UUID uuid, int level) {
        LuckPerms luckPerms = LuckPermsProvider.get();
        User user = luckPerms.getUserManager().getUser(uuid);

        if (user != null) {
            user.data().clear(node -> node instanceof MetaNode && ((MetaNode) node).getMetaKey().equals("level"));

            MetaNode metaNode = MetaNode.builder("level", String.valueOf(level)).build();
            user.data().add(metaNode);

            luckPerms.getUserManager().saveUser(user);
        }
    }
}
