package org.flennn.CoreManager;

import com.velocitypowered.api.proxy.Player;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.luckperms.api.LuckPerms;
import net.luckperms.api.model.user.User;
import net.luckperms.api.query.QueryOptions;
import org.flennn.Config.ConfigManager;

import java.util.logging.Logger;

public class Utils {
    private static LuckPerms luckPerms;
    private static ConfigManager config;

    public static void setLuckPerms(LuckPerms lp) {
        luckPerms = lp;
    }

    public static void setConfig(ConfigManager cfg) {
        config = cfg;
    }

    public static Component mm(String message) {
        return MiniMessage.miniMessage().deserialize(config.getPrefix() + message);
    }

    public static String getFormattedName(Player player) {
        User user = luckPerms.getUserManager().getUser(player.getUniqueId());
        if (user == null) return player.getUsername();

        QueryOptions queryOptions = QueryOptions.nonContextual();
        String prefix = user.getCachedData().getMetaData(queryOptions).getPrefix();
        String suffix = user.getCachedData().getMetaData(queryOptions).getSuffix();

        return (prefix != null ? prefix + " " : "") + player.getUsername() + (suffix != null ? " " + suffix : "");
    }

    public static boolean isSimilar(String serverName, String input) {
        int distance = levenshteinDistance(serverName.toLowerCase(), input.toLowerCase());
        return distance <= 3;
    }

    public static int levenshteinDistance(String s1, String s2) {
        int[][] dp = new int[s1.length() + 1][s2.length() + 1];

        for (int i = 0; i <= s1.length(); i++) {
            for (int j = 0; j <= s2.length(); j++) {
                if (i == 0) {
                    dp[i][j] = j;
                } else if (j == 0) {
                    dp[i][j] = i;
                } else {
                    dp[i][j] = Math.min(
                            dp[i - 1][j - 1] + (s1.charAt(i - 1) == s2.charAt(j - 1) ? 0 : 1),
                            Math.min(dp[i - 1][j] + 1, dp[i][j - 1] + 1)
                    );
                }
            }
        }

        return dp[s1.length()][s2.length()];
    }

    public static class Log {
        private static final Logger logger = Logger.getLogger("RivalMCCore");

        private static final String RESET = "\u001B[0m";

        private static final String BLUE = "\u001B[94m";
        private static final String YELLOW = "\u001B[93m";
        private static final String RED = "\u001B[91m";
        private static final String ORANGE = "\u001B[38;5;214m";

        static {
            logger.setUseParentHandlers(false);
        }

        public static void info(String message) {
            System.out.println(ORANGE + "[RivalMCCore] " + BLUE + "[INFO] " + RESET + " " + message);
        }

        public static void warning(String message) {
            System.out.println(ORANGE + "[RivalMCCore] " + YELLOW + "[WARN] " + RESET + " " + message);
        }

        public static void severe(String message) {
            System.out.println(ORANGE + "[RivalMCCore] " + RED + "[ERROR] " + RESET + " " + message);
        }
    }

}
