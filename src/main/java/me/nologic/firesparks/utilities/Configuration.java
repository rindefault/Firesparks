package me.nologic.firesparks.utilities;

import me.nologic.firesparks.Firesparks;
import me.nologic.firesparks.utilities.items.BurnableItemList;
import org.bukkit.ChatColor;
import org.bukkit.configuration.file.FileConfiguration;

import java.util.List;

public class Configuration {

    private static final FileConfiguration config = Firesparks.getInstance().getConfig();
    private static final BurnableItemList burnableItemList = BurnableItemList.build();

    public static String getMessage(String message) {

        if (message.equals("item-burn") || message.equals("message-of-the-day")) {

            List<String> stringList = config.getStringList("messages." + message);

            return ChatColor.translateAlternateColorCodes('&', stringList.get((int) (Math.random() * stringList.size())));
        } else {
            return ChatColor.translateAlternateColorCodes('&', config.getString("messages." + message));
        }

    }

    public static int getComfortGrowthAmount() {
        return config.getInt("comfort.growth-amount");
    }
    public static String getComfortBarName() {
        return ChatColor.translateAlternateColorCodes('&', config.getString("comfort.progressbar-name"));
    }
    public static boolean getColdness() {
        return config.getBoolean("coldness.enable");
    }
    public static boolean getItemBurn() {
        return config.getBoolean("item-burn.enable");
    }
    public static BurnableItemList getItemList() {
        return burnableItemList;
    }

    public static FileConfiguration getConfig() {
        return config;
    }
}
