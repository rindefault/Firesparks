package me.nologic.firesparks.gameplay;

import dev.geco.gsit.api.GSitAPI;
import me.nologic.firesparks.Firesparks;
import net.kyori.adventure.bossbar.BossBar;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.data.type.Campfire;
import org.bukkit.block.data.type.Furnace;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.util.Map;

public class ComfortDataHandler extends Thread implements Listener {

    private final Firesparks plugin;
    private final Map<Player, ComfortData> comfortDataMap;
    private boolean cancelled;
    
    @Override
    public void run() {
        while (!this.cancelled) {
            this.tick();
            try { Thread.sleep(2000L); }
            catch (Exception ignored) {}
        }
    }
    
    private void tick() {
        this.comfortDataMap.values().forEach(data -> {
            data.update();
            if (data.isDelightfulness()) {
                Player player = data.getPlayer();
                Bukkit.getServer().getScheduler().runTask(this.plugin, () -> {
                    player.addPotionEffect(new PotionEffect(PotionEffectType.REGENERATION, 50, 0, true, true, true));
                    player.setSaturation(player.getSaturation() - 1.0f);
                    if (player.getSaturation() == 0.0f) {
                        player.setFoodLevel(player.getFoodLevel() - 1);
                    }
                    if (!data.isMusicPlaying()) {
                        player.playSound(player.getLocation(), Sound.MUSIC_DISC_CAT, 1.0f, 1.0f);
                        data.setMusicPlaying(true);
                    }
                });
            }
        });
    }
    
    @EventHandler
    private void whenPlayerJoin(final PlayerJoinEvent event) {
        final Component name = Component.text("§l\u041a\u043e\u043c\u0444\u043e\u0440\u0442");
        final BossBar personalPlayerBar = BossBar.bossBar(name, 0.0f, BossBar.Color.RED, BossBar.Overlay.NOTCHED_20);
        final ComfortData data = new ComfortData(event.getPlayer(), personalPlayerBar);
        this.comfortDataMap.put(event.getPlayer(), data);
    }
    
    @EventHandler
    private void whenPlayerQuit(final PlayerQuitEvent event) {
        this.comfortDataMap.remove(event.getPlayer());
    }
    
    @EventHandler
    private void whenPlayerTakesDamage(final EntityDamageEvent event) {
        final Entity entity = event.getEntity();
        if (entity instanceof Player player) {
            final ComfortData data = this.comfortDataMap.get(player);
            if (data != null) {
                data.setLevel((int)(data.getLevel() - event.getFinalDamage() * 50.0));
                data.getBar().progress((float)(data.getLevel() / 1000));
            }
        }
    }
    
    public ComfortDataHandler(final Firesparks plugin, final Map<Player, ComfortData> comfortDataMap) {
        this.plugin = plugin;
        this.comfortDataMap = comfortDataMap;
    }
    
    public void setCancelled(final boolean cancelled) {
        this.cancelled = cancelled;
    }
    
    public final class ComfortData {

        private final Player player;
        private int level;
        private Level state;
        private boolean musicPlaying;
        private boolean nearFire;
        private boolean sitting;
        private final BossBar bar;
        
        public void update() {
            final boolean wellFed = this.player.getFoodLevel() > 17;
            this.nearFire = this.isPlayerNearWarmBlock();
            this.sitting = GSitAPI.isSitting(this.player);
            if (this.nearFire && wellFed && this.sitting) {
                this.increment();
            }
            else {
                this.decrement();
            }
        }
        
        private void restate() {
            if (this.level < 100) {
                this.state = Level.LOW;
            }
            else if (this.level < 600) {
                this.state = Level.NORMAL;
                this.musicPlaying = false;
            }
            else if (this.level < 800) {
                if (this.state == Level.COZY) {
                    this.player.sendMessage("§7\u0412\u044b \u0447\u0443\u0432\u0441\u0442\u0432\u0443\u0435\u0442\u0435 \u0441\u0435\u0431\u044f \u0445\u043e\u0440\u043e\u0448\u043e \u043e\u0442\u0434\u043e\u0445\u043d\u0443\u0432\u0448\u0438\u043c...");
                }
                this.state = Level.NICE;
            }
            else {
                this.state = Level.COZY;
            }
        }
        
        public boolean isDelightfulness() {
            return this.state == Level.COZY && this.nearFire && this.sitting;
        }
        
        public void setLevel(int value) {
            if (value < 0) {
                value = 0;
            }
            else if (value > 1000) {
                value = 1000;
            }
            this.level = value;
        }
        
        public void showBossBar(final float level) {

            switch (this.state) {
                case LOW -> this.bar.color(BossBar.Color.RED);
                case NORMAL -> this.bar.color(BossBar.Color.YELLOW);
                case NICE -> this.bar.color(BossBar.Color.BLUE);
                case COZY -> this.bar.color(BossBar.Color.GREEN);
            }

            this.bar.progress(level / 1000.0f);
            this.player.showBossBar(this.bar);
            Bukkit.getServer().getScheduler().runTaskLater(ComfortDataHandler.this.plugin, () -> {
                if (level >= this.level) {
                    this.player.hideBossBar(this.bar);
                }
            }, 100L);
        }
        
        private void increment() {
            if (this.level != 1000) {
                if (this.level + 6 > 1000) {
                    this.level = 1000;
                }
                else {
                    this.level += 6;
                }
                this.restate();
                this.showBossBar((float)this.level);
            }
        }
        
        private void decrement() {
            if (this.level != 0) {
                if (this.level - 2 < 0) {
                    this.level = 0;
                }
                else {
                    this.level -= 2;
                }
                this.restate();
            }
        }
        
        private boolean isPlayerNearWarmBlock() {
            final Material[] targets = { Material.CAMPFIRE, Material.SOUL_CAMPFIRE, Material.FURNACE, Material.BLAST_FURNACE, Material.SMOKER };
            final Location base = this.player.getLocation();
            for (int y = -2; y < 2; ++y) {
                final Location loc = new Location(base.getWorld(), base.getX(), base.getY() + y, base.getZ());
                for (int distance = 1; distance < 5; ++distance) {
                    for (final BlockFace face : BlockFace.values()) {
                        final Block block = loc.getBlock().getRelative(face, distance);
                        for (final Material target : targets) {
                            if (block.getType() == target) {

                                return switch (target) {
                                    case CAMPFIRE, SOUL_CAMPFIRE -> ((Campfire) block.getBlockData()).isLit();
                                    case FURNACE, BLAST_FURNACE, SMOKER -> ((Furnace) block.getBlockData()).isLit();
                                    default -> false;
                                };

                            }
                        }
                    }
                }
            }
            return false;
        }
        
        public ComfortData(final Player player, final BossBar bar) {
            this.player = player;
            this.bar = bar;
        }
        
        public Player getPlayer() {
            return this.player;
        }
        
        public int getLevel() {
            return this.level;
        }

        public boolean isMusicPlaying() {
            return this.musicPlaying;
        }
        
        public void setMusicPlaying(final boolean musicPlaying) {
            this.musicPlaying = musicPlaying;
        }
        
        public BossBar getBar() {
            return this.bar;
        }
        
        public enum Level {

            LOW, 
            NORMAL, 
            NICE, 
            COZY;

        }
    }

}