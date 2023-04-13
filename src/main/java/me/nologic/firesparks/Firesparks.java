package me.nologic.firesparks;

import me.nologic.firesparks.gameplay.ComfortDataHandler;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.concurrent.ConcurrentHashMap;

public final class Firesparks extends JavaPlugin {

    private ComfortDataHandler comfort;
    
    public void onEnable() {
        (this.comfort = new ComfortDataHandler(this, new ConcurrentHashMap<>())).start();
        super.getServer().getPluginManager().registerEvents(this.comfort, this);
    }
    
    public void onDisable() {
        this.comfort.setCancelled(true);
    }

}