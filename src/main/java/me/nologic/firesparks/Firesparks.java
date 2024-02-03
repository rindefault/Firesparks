package me.nologic.firesparks;

import lombok.Getter;
import me.nologic.firesparks.gameplay.CampfireItemBurnListener;
import me.nologic.firesparks.gameplay.PlayerComfortManager;
import me.nologic.firesparks.utilities.Configuration;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;

public class Firesparks extends JavaPlugin {

    @Getter
    private static Firesparks instance;

    @Getter
    private PlayerComfortManager comfortManager;

    public void onEnable() {

        instance = this;
        this.saveDefaultConfig();

        this.comfortManager = new PlayerComfortManager(this);
        super.getServer().getPluginManager().registerEvents(this.comfortManager, this);

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

}