package org.flennn.CoreCommands;

import com.velocitypowered.api.command.CommandManager;
import com.velocitypowered.api.command.CommandSource;
import com.velocitypowered.api.command.SimpleCommand;
import com.velocitypowered.api.proxy.ProxyServer;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.flennn.CoreDatabase.DatabaseManager;
import org.flennn.CoreManager.Utils;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

import static org.flennn.CoreManager.Utils.isSimilar;

public class ServerStatsCommand implements SimpleCommand {

    private final ProxyServer proxyServer;
    private final DatabaseManager databaseManager;

    public ServerStatsCommand(ProxyServer proxyServer, DatabaseManager databaseManager) {
        this.proxyServer = proxyServer;
        this.databaseManager = databaseManager;
    }

    @Override
    public void execute(Invocation invocation) {
        CommandSource source = invocation.source();
        String[] args = invocation.arguments();

        if (args.length == 0) {
            showAllServers(source);
        } else {
            if (!source.hasPermission("rivalcore.admin")) {
                source.sendMessage(Component.text("❌ You do not have permission to use this command.", NamedTextColor.RED));
                return;
            }
            toggleStats(source, args[0]);
        }
    }

    private void showAllServers(CommandSource source) {
        CompletableFuture.runAsync(() -> {
            String query = "SELECT name, stats_enabled FROM servers";

            try (Connection connection = databaseManager.getConnection();
                 PreparedStatement stmt = connection.prepareStatement(query);
                 ResultSet rs = stmt.executeQuery()) {

                StringBuilder message = new StringBuilder("<gold>✪ Server Stats ✪</gold>\n");

                while (rs.next()) {
                    String name = rs.getString("name");
                    boolean statsEnabled = rs.getBoolean("stats_enabled");
                    message.append("<yellow>- ").append(name)
                            .append(": ").append(statsEnabled ? "<green>Enabled" : "<red>Disabled")
                            .append("</red>\n");
                }

                source.sendMessage(MiniMessage.miniMessage().deserialize(message.toString()));

            } catch (Exception e) {
                source.sendMessage(MiniMessage.miniMessage().deserialize("<red>❌ Failed to retrieve stats.</red>"));
                e.printStackTrace();
            }
        });
    }

    private void toggleStats(CommandSource source, String serverName) {
        CompletableFuture.runAsync(() -> {
            String queryCheck = "SELECT stats_enabled FROM servers WHERE name = ?";
            String queryUpdate = "UPDATE servers SET stats_enabled = ? WHERE name = ?";

            try (Connection connection = databaseManager.getConnection();
                 PreparedStatement stmtCheck = connection.prepareStatement(queryCheck);
                 PreparedStatement stmtUpdate = connection.prepareStatement(queryUpdate)) {

                stmtCheck.setString(1, serverName);
                ResultSet rs = stmtCheck.executeQuery();

                if (rs.next()) {
                    boolean currentStatus = rs.getBoolean("stats_enabled");
                    boolean newStatus = !currentStatus;

                    stmtUpdate.setBoolean(1, newStatus);
                    stmtUpdate.setString(2, serverName);
                    stmtUpdate.executeUpdate();

                    source.sendMessage(Utils.mm("<green>✅</green> Stats for <yellow>" + serverName +
                            "</yellow> have been " + (newStatus ? "<green>enabled</green>" : "<red>disabled</red>") + "."));

                } else {
                    List<String> serverSuggestions = getServerSuggestions(serverName);
                    String suggestionMsg = serverSuggestions.isEmpty()
                            ? "<red>No known servers found.</red>"
                            : "<yellow>Did you mean:</yellow> " + String.join(", ", serverSuggestions);

                    source.sendMessage(Utils.mm("<red>❌</red> Server <yellow>" + serverName + "</yellow> not found.\n" + suggestionMsg));
                }

            } catch (Exception e) {
                source.sendMessage(Utils.mm("<red>❌</red> Error updating server stats settings."));
                e.printStackTrace();
            }
        });
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
                commandManager.metaBuilder("sstats")
                        .plugin(plugin)
                        .build(),
                this
        );
    }
}
