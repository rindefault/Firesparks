package me.nologic.firesparks;

import me.nologic.firesparks.gameplay.CampfireItemBurnListener;
import me.nologic.firesparks.gameplay.ComfortDataHandler;
import me.nologic.firesparks.utilities.Configuration;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.concurrent.ConcurrentHashMap;

public class Firesparks extends JavaPlugin {

    private ComfortDataHandler comfort;
    private static Firesparks plugin;

    public static Firesparks getPlugin() {
        return plugin;
    }
    
    public void onEnable() {

        plugin = this;
        this.saveDefaultConfig();

        (this.comfort = new ComfortDataHandler(this, new ConcurrentHashMap<>())).start();
        super.getServer().getPluginManager().registerEvents(this.comfort, this);

        if (Configuration.getItemBurn()) {
            super.getServer().getPluginManager().registerEvents(new CampfireItemBurnListener(), this);
        }

        // PLugin startup message

        Component header = MiniMessage.miniMessage().deserialize(
                " <dark_gray>~ <#e5a470>\uD83D\uDD25 <#efbd7a>Fire<#f7da82>sparks <dark_gray>~ "
        );
        String footer = Configuration.getMessage("message-of-the-day");

        Bukkit.getConsoleSender().sendMessage(" ");
        Bukkit.getConsoleSender().sendMessage(header);

        if (super.getServer().getPluginManager().getPlugin("GSit") != null) { // GSit dependency check
            Bukkit.getConsoleSender().sendMessage(footer);
        }
        else {
            Bukkit.getConsoleSender().sendMessage("§cGSit not found! Disabling...");
            super.getServer().getPluginManager().disablePlugin(this);
        }

        Bukkit.getConsoleSender().sendMessage(" ");

    }
    
    public void onDisable() {
        this.comfort.setCancelled(true);
    }
}
