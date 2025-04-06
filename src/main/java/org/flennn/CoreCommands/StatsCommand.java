package org.flennn.CoreCommands;

import com.velocitypowered.api.command.CommandManager;
import com.velocitypowered.api.command.CommandSource;
import com.velocitypowered.api.command.SimpleCommand;
import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.ProxyServer;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.flennn.CoreDatabase.DatabaseManager;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

import static org.flennn.CoreManager.Utils.isSimilar;

public class StatsCommand implements SimpleCommand {

    private final ProxyServer proxyServer;
    private final DatabaseManager databaseManager;

    public StatsCommand(ProxyServer proxyServer, DatabaseManager databaseManager) {
        this.proxyServer = proxyServer;
        this.databaseManager = databaseManager;
    }

    @Override
    public void execute(Invocation invocation) {
        CommandSource source = invocation.source();
        String[] args = invocation.arguments();

        if (!source.hasPermission("rivalcore.stats")) {
            source.sendMessage(Component.text("❌ You do not have permission to use this command.", NamedTextColor.RED));
            return;
        }

        if (args.length == 0) {
            source.sendMessage(Component.text("Usage: /stats <server> OR /stats <player> <server> (Admins Only)", NamedTextColor.YELLOW));
            return;
        }

        String serverName;
        String playerName;

        if (args.length == 1) {
            serverName = args[0];

            if (source instanceof Player player) {
                playerName = player.getUsername();
            } else {
                source.sendMessage(Component.text("❌ Only players can use this command.", NamedTextColor.RED));
                return;
            }
        } else {
            if (!source.hasPermission("rivalcore.admin")) {
                source.sendMessage(Component.text("❌ You do not have permission to check other players' stats.", NamedTextColor.RED));
                return;
            }
            playerName = args[0];
            serverName = args[1];
        }

        fetchStats(source, playerName, serverName);
    }

    private void fetchStats(CommandSource source, String playerName, String serverName) {
        CompletableFuture.runAsync(() -> {
            if (!isStatsEnabled(serverName)) {
                List<String> serverSuggestions = getServerSuggestions(serverName);
                Component suggestionMsg = serverSuggestions.isEmpty()
                        ? Component.text("No known servers found.", NamedTextColor.RED)
                        : Component.text("Did you mean: ", NamedTextColor.YELLOW)
                        .append(Component.text(String.join(", ", serverSuggestions), NamedTextColor.GOLD));

                source.sendMessage(Component.text("❌ Stats for " + serverName + " are disabled!", NamedTextColor.RED)
                        .append(suggestionMsg));
                return;
            }

            String query = "SELECT kills, deaths, joins, quits, blocks_placed, blocks_broken, " +
                    "items_picked_up, food_eaten, damage_dealt, damage_taken " +
                    "FROM stats_" + serverName + " WHERE username = ?";

            try (Connection connection = databaseManager.getConnection();
                 PreparedStatement stmt = connection.prepareStatement(query)) {

                stmt.setString(1, playerName);
                ResultSet rs = stmt.executeQuery();

                if (rs.next()) {
                    Component statsMessage = Component.text("✪ RivalStats ✪ " + playerName + "'s Stats on " + serverName, NamedTextColor.GOLD)
                            .append(Component.newline())
                            .append(Component.text("- Kills: " + rs.getInt("kills"), NamedTextColor.YELLOW))
                            .append(Component.newline())
                            .append(Component.text("- Deaths: " + rs.getInt("deaths"), NamedTextColor.YELLOW))
                            .append(Component.newline())
                            .append(Component.text("- Blocks Placed: " + rs.getInt("blocks_placed"), NamedTextColor.YELLOW))
                            .append(Component.newline())
                            .append(Component.text("- Blocks Broken: " + rs.getInt("blocks_broken"), NamedTextColor.YELLOW))
                            .append(Component.newline())
                            .append(Component.text("- Items Picked Up: " + rs.getInt("items_picked_up"), NamedTextColor.YELLOW))
                            .append(Component.newline())
                            .append(Component.text("- Food Eaten: " + rs.getInt("food_eaten"), NamedTextColor.YELLOW))
                            .append(Component.newline())
                            .append(Component.text("- Damage Dealt: " + rs.getInt("damage_dealt"), NamedTextColor.YELLOW))
                            .append(Component.newline())
                            .append(Component.text("- Damage Taken: " + rs.getInt("damage_taken"), NamedTextColor.YELLOW));

                    source.sendMessage(statsMessage);
                } else {
                    source.sendMessage(Component.text("❌ No stats found for " + playerName + " on " + serverName, NamedTextColor.RED));
                }

            } catch (Exception e) {
                source.sendMessage(Component.text("❌ Error retrieving stats.", NamedTextColor.RED));
                e.printStackTrace();
            }
        });
    }

    private boolean isStatsEnabled(String serverName) {
        String query = "SELECT stats_enabled FROM servers WHERE name = ?";

        try (Connection connection = databaseManager.getConnection();
             PreparedStatement stmt = connection.prepareStatement(query)) {

            stmt.setString(1, serverName);
            ResultSet rs = stmt.executeQuery();

            return rs.next() && rs.getBoolean("stats_enabled");

        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }


    private List<String> getServerSuggestions(String input) {
        List<String> servers = new ArrayList<>();
        String query = "SELECT name FROM servers";

        try (Connection connection = databaseManager.getConnection();
             PreparedStatement stmt = connection.prepareStatement(query);
             ResultSet rs = stmt.executeQuery()) {

            while (rs.next()) {
                servers.add(rs.getString("name"));
            }

        } catch (Exception e) {
            e.printStackTrace();
        }

        return servers.stream()
                .filter(server -> isSimilar(server, input))
                .limit(8)
                .collect(Collectors.toList());
    }

    public void register(CommandManager commandManager, Object plugin) {
        commandManager.register(
                commandManager.metaBuilder("stats")
                        .aliases("statistics")
                        .plugin(plugin)
                        .build(),
                this
        );
    }
}
