package org.flennn.CoreCommands;

import com.velocitypowered.api.command.CommandManager;
import com.velocitypowered.api.command.CommandSource;
import com.velocitypowered.api.command.SimpleCommand;
import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.ProxyServer;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.flennn.Config.ConfigManager;
import org.flennn.CoreDatabase.DatabaseManager;
import org.flennn.CoreManager.Utils;

import java.util.UUID;

public class StaffChatCommand implements SimpleCommand {
    private final ProxyServer proxy;
    private final DatabaseManager databaseManager;
    private final MiniMessage miniMessage = MiniMessage.miniMessage();
    private final ConfigManager configManager;


    public StaffChatCommand(ProxyServer proxy, DatabaseManager databaseManager, ConfigManager configManager) {
        this.proxy = proxy;
        this.databaseManager = databaseManager;
        this.configManager = configManager;
    }

    @Override
    public void execute(Invocation invocation) {
        CommandSource source = invocation.source();
        String[] args = invocation.arguments();

        if (!source.hasPermission("rivalcore.staff")) {
            source.sendMessage(Utils.mm("<red>❌ You do not have permission to use this command.</red>"));
            return;
        }

        if (!(source instanceof Player player)) {
            source.sendMessage(Utils.mm("<red>❌ Only players can use this command.</red>"));
            return;
        }

        UUID playerUUID = player.getUniqueId();

        if (args.length == 0) {
            databaseManager.toggleStaffChat(playerUUID.toString());
            boolean isEnabled = databaseManager.isStaffChatEnabled(playerUUID.toString());
            source.sendMessage(Utils.mm("<gold>Staff chat " + (isEnabled ? "enabled ✅" : "disabled ❌") + "</gold>"));
            return;
        }

        String message = String.join(" ", args);
        sendStaffChatMessage(player, message);
    }

    private void sendStaffChatMessage(Player player, String message) {
        Component chatMessage = miniMessage.deserialize(configManager.getStaffChatPrefix() + Utils.getFormattedName(player) + " " + "<reset>" + message);

        proxy.getAllPlayers().stream()
                .filter(p -> p.hasPermission("rivalcore.staff"))
                .forEach(p -> p.sendMessage(chatMessage));

    }

    public void register(CommandManager commandManager, Object plugin) {
        commandManager.register(
                commandManager.metaBuilder("sc")
                        .aliases("staffchat")
                        .plugin(plugin)
                        .build(),
                this
        );
    }
}
