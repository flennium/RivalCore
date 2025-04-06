package org.flennn.CoreManager;

import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.player.PlayerChatEvent;
import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.ProxyServer;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.flennn.Config.ConfigManager;
import org.flennn.CoreDatabase.DatabaseManager;

public class StaffChatListener {

    private final ProxyServer proxy;
    private final DatabaseManager databaseManager;
    private final MiniMessage miniMessage = MiniMessage.miniMessage();
    private final ConfigManager configManager;

    public StaffChatListener(ProxyServer proxy, DatabaseManager databaseManager, ConfigManager configManager) {
        this.proxy = proxy;
        this.databaseManager = databaseManager;
        this.configManager = configManager;
    }

    @Subscribe
    public void onPlayerChat(PlayerChatEvent event) {
        Player player = event.getPlayer();

        if (databaseManager.isStaffChatEnabled(player.getUniqueId().toString())) {
            event.setResult(PlayerChatEvent.ChatResult.denied());

            Component chatMessage = miniMessage.deserialize(
                    configManager.getStaffChatPrefix() + Utils.getFormattedName(player) + " " + "<reset>" + event.getMessage()
            );

            proxy.getAllPlayers().stream()
                    .filter(p -> p.hasPermission("rivalcore.staff"))
                    .forEach(p -> p.sendMessage(chatMessage));
        }
    }
}
