package org.flennn.CoreManager;

import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.connection.LoginEvent;
import com.velocitypowered.api.event.connection.PostLoginEvent;
import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.ProxyServer;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.flennn.Config.ConfigManager;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Arrays;
import java.util.List;

public class PermissionListener {

    private static final String WEBHOOK_NAME = "RivalMC Security";
    private static final String WEBHOOK_AVATAR = "https://cdn.discordapp.com/avatars/1198385836736135340/a0daf842cc4c712c46b5494b9360d10e.png?size=1024";
    private final ConfigManager configManager;

    public PermissionListener(ProxyServer proxyServer, ConfigManager configManager) {
        this.configManager = configManager;
    }

    @Subscribe
    public void onPostLogin(PostLoginEvent event) {
        Player player = event.getPlayer();

        if (configManager.getBypassPlayers().stream()
                .noneMatch(name -> name.equalsIgnoreCase(player.getUsername())) && hasServerDangerousPermissions(player)) {
            sendDiscordAlert(player);

            player.disconnect(Component.text("⛔ Security Alert ⛔\n\n" +
                    "You have been temporarily removed from the server due to security concerns.\n" +
                    "If you believe this is a mistake, please contact a senior staff member for assistance.\n\n" +
                    "📩 Support: discord.gg/rivalmc", NamedTextColor.RED));
        }



    }

    @Subscribe
    public void onLogin(LoginEvent event) {
        Player player = event.getPlayer();

        if (configManager.getBypassPlayers().stream()
                .noneMatch(name -> name.equalsIgnoreCase(player.getUsername())) && hasServerDangerousPermissions(player)) {
            sendDiscordAlert(player);

            player.disconnect(Component.text("⛔ Security Alert ⛔\n\n" +
                    "You have been temporarily removed from the server due to security concerns.\n" +
                    "If you believe this is a mistake, please contact a senior staff member for assistance.\n\n" +
                    "📩 Support: discord.gg/rivalmc", NamedTextColor.RED));
        }
    }


    private boolean hasServerDangerousPermissions(Player player) {
        List<String> dangerousPermissions = Arrays.asList(
                "luckperms",
                "rivalcore.*",
                "rivalcore.admin",
                "luckperms.*",
                "luckperms.applyedit",
                "*",
                "minecraft.command.op",
                "op"
        );

        return dangerousPermissions.stream().anyMatch(player::hasPermission);
    }

    private void sendDiscordAlert(Player player) {
        try {
            String webhookUrl = configManager.getSecurityWebhook();

            if (webhookUrl == null || webhookUrl.isEmpty()) {
                System.err.println("❌ Webhook URL is not set in the config!");
                return;
            }

            URL url = new URL(webhookUrl);
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("POST");
            connection.setDoOutput(true);
            connection.setRequestProperty("Content-Type", "application/json");

            String jsonPayload = "{"
                    + "\"username\": \"" + WEBHOOK_NAME + "\","
                    + "\"avatar_url\": \"" + WEBHOOK_AVATAR + "\","
                    + "\"embeds\": [{"
                    + "\"title\": \" Security Alert \","
                    + "\"color\": 16711680,"
                    + "\"description\": \"A player was **disconnected** due to security concerns.\","
                    + "\"fields\": ["
                    + "{\"name\": \"Player:\", \"value\": \"" + player.getUsername() + "\", \"inline\": true},"
                    + "{\"name\": \"UUID:\", \"value\": \"" + player.getUniqueId() + "\", \"inline\": true},"
                    + "{\"name\": \"Reason:\", \"value\": \"Detected with dangerous permissions.\", \"inline\": false}"
                    + "],"
                    + "\"footer\": { \"text\": \"RivalMC Security System\", \"icon_url\": \"" + WEBHOOK_AVATAR + "\" }"
                    + "}]"
                    + "}";

            try (OutputStream outputStream = connection.getOutputStream()) {
                outputStream.write(jsonPayload.getBytes());
                outputStream.flush();
            }

            int responseCode = connection.getResponseCode();

            if (responseCode != 200 && responseCode != 204) {
                try (BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getErrorStream()))) {
                    StringBuilder response = new StringBuilder();
                    String line;
                    while ((line = reader.readLine()) != null) {
                        response.append(line);
                    }
                }
            }

        } catch (Exception e) {
            Utils.Log.warning("❌ Failed to send Discord alert");
            e.printStackTrace();
        }
    }

}
