package org.flennn.CoreManager;

import com.velocitypowered.api.event.Subscribe;
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
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

public class ClientBrandListener {

    private static final String WEBHOOK_NAME = "RivalMC Security";
    private static final String WEBHOOK_AVATAR = "https://cdn.discordapp.com/avatars/1198385836736135340/a0daf842cc4c712c46b5494b9360d10e.png?size=1024";
    private final ConfigManager configManager;
    private final ProxyServer proxy;
    private final Set<UUID> checkedPlayers = new HashSet<>();

    private static final Set<String> BLACKLISTED_CLIENTS = Set.of(
            "impact", "wurst", "sigma", "future", "aristois",
            "meteor", "inertia", "liquidbounce", "rusherhack",
            "kami", "salhack", "forgehax", "weepcraft", "huzuni",
            "rise", "vape", "novoline", "moon", "pyro",
            "pandora", "kronos", "zeroday", "y-skid", "fate",
            "proton", "skid", "leak", "nivia", "atlas",
            "clicker", "autoclicker", "lambda", "skidder",
            "dortware", "hxcheat", "edge", "catalyst", "omnia",
            "yigd", "aqua", "kraken", "kilo", "blackout", "ares", "doomsday", "jex", "boze",
            "bloody", "mio", "prestige", "shoreline", "lumina",
            "coffee", "alien", "kura", "melon", "3arthh4ck",
            "blackspigot", "spigotunlocked", "directleaks", "firespigot",
            "wizardhax", "minecrafthax", "hovac", "masterof13fps",
            "smarthack", "sulfur", "jessica", "fdp", "ravenb++",
            "akira", "noise", "nightx", "azura", "wurst+",
            "inertia+", "raven n+", "liquidbounce+ reborn", "drip",
            "drip lite", "drip x", "nitr0", "prax", "borion",
            "aoba", "jexclient", "bleachhack"
    );


    public ClientBrandListener(ProxyServer proxy, ConfigManager configManager) {
        this.proxy = proxy;
        this.configManager = configManager;
    }

    @Subscribe
    public void onPostLogin(PostLoginEvent event) {
        Player player = event.getPlayer();

        checkedPlayers.add(event.getPlayer().getUniqueId());

        if (!checkedPlayers.contains(player.getUniqueId())) return;
        checkedPlayers.remove(player.getUniqueId());

        if (BLACKLISTED_CLIENTS.contains(player.getClientBrand())) {
            sendDiscordAlert(player, player.getClientBrand());

            player.disconnect(Component.text("⛔ Security Alert ⛔\n\n" +
                    "You have been temporarily removed from the server due to using a **blacklisted client**.\n" +
                    "If you believe this is a mistake, please contact a senior staff member for assistance.\n\n" +
                    "📩 Support: discord.gg/rivalmc", NamedTextColor.RED));
        }
    }


    private void sendDiscordAlert(Player player, String clientBrand) {
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
                    + "\"title\": \"Security Alert\","
                    + "\"color\": 16711680,"
                    + "\"description\": \"A player was **disconnected** for using a blacklisted client.\","
                    + "\"fields\": ["
                    + "{\"name\": \"Player:\", \"value\": \"" + player.getUsername() + "\", \"inline\": true},"
                    + "{\"name\": \"UUID:\", \"value\": \"" + player.getUniqueId() + "\", \"inline\": true},"
                    + "{\"name\": \"Client:\", \"value\": \"" + clientBrand + "\", \"inline\": false}"
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
            System.err.println("❌ Failed to send Discord alert");
            e.printStackTrace();
        }
    }
}
