package me.nologic.firesparks.gameplay;

import dev.geco.gsit.api.GSitAPI;
import me.nologic.firesparks.Firesparks;
import me.nologic.firesparks.utilities.Configuration;
import net.kyori.adventure.bossbar.BossBar;
import net.kyori.adventure.text.Component;
import org.bukkit.*;
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
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.Map;

public class ComfortDataHandler extends Thread implements Listener {

    private final Firesparks plugin;
    private final Map<Player, ComfortData> comfortDataMap;
    private boolean cancelled;
    private final int comfortGrowthAmount;
    
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
        final Component name = Component.text(Configuration.getComfortBarName());
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

    @EventHandler
    private void whenPlayerRespawns(final PlayerRespawnEvent event) {

        if(!Configuration.getColdness()) return;

        final Player player = event.getPlayer();
        if(player.getFreezeTicks() > 0) {
            new BukkitRunnable() {

                @Override
                public void run() {
                    player.setFreezeTicks(0);
                }

            }.runTask(plugin);
        }
    }
    
    public ComfortDataHandler(final Firesparks plugin, final Map<Player, ComfortData> comfortDataMap) {
        this.plugin = plugin;
        this.comfortDataMap = comfortDataMap;
        this.comfortGrowthAmount = Configuration.getComfortGrowthAmount();
    }
    
    public void setCancelled(final boolean cancelled) {
        this.cancelled = cancelled;
    }
    
    public final class ComfortData {

        private Coldness coldness = Coldness.NORMAL;
        private Boolean coldnessTask = false;

        private final Player player;
        private int level;
        private Level state;
        private boolean musicPlaying;
        private boolean nearFire;
        private boolean hasArmor;
        private boolean sitting;
        private final BossBar bar;
        
        public void update() {

            final boolean wellFed = this.player.getFoodLevel() > 17;
            this.nearFire = this.isPlayerNearWarmBlock();
            this.sitting = GSitAPI.isSitting(this.player);
            this.hasArmor = this.isPlayerHasArmor();

            if (this.nearFire) {
                this.coldness = Coldness.WARM;
                if (this.sitting && wellFed) {
                    this.increment();
                }
            }
            else {
                if (Configuration.getColdness()) {
                    if (this.isPlayerInColdBiome()) {
                        decrementColdness();
                    }
                    else {
                        resetColdness();
                    }
                }
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
                    this.player.sendMessage(Configuration.getMessage("delightfulness"));
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
                if (this.level + comfortGrowthAmount > 1000) {
                    this.level = 1000;
                }
                else {
                    this.level += comfortGrowthAmount;
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

        private void resetColdness() {

            if (this.coldness == Coldness.COLD) {
                this.coldness = Coldness.NORMAL;
            }
        }
        private void decrementColdness() {

            if (
                player.getGameMode().equals(GameMode.SPECTATOR) ||
                player.getGameMode().equals(GameMode.CREATIVE)
            ) return;

            if (this.hasArmor) return;

            if (this.coldness == Coldness.WARM) {
                this.coldness = Coldness.NORMAL;
            }
            else if (this.coldness == Coldness.NORMAL) {

                if (player.getFreezeTicks() > 0) return;
                if(this.coldnessTask) return;

                new BukkitRunnable() {
                    int freezeTime = 0;

                    @Override
                    public void run() {
                        coldnessTask = true;

                        if (
                            coldness == Coldness.COLD ||

                            !isPlayerInColdBiome() ||
                            isPlayerHasArmor() ||
                            isPlayerNearWarmBlock()
                        ) {
                            this.cancel();
                            coldnessTask = false;
                        }

                        if (
                            player.getGameMode().equals(GameMode.SPECTATOR) ||
                            player.getGameMode().equals(GameMode.CREATIVE)
                        ) {
                            this.cancel();
                            resetColdness();
                            coldnessTask = false;
                        }

                        if (freezeTime < 120) {
                            freezeTime++;

                            player.setFreezeTicks(freezeTime);
                        }
                        else {
                            this.cancel();
                            coldnessTask = false;
                            coldness = Coldness.COLD;

                            player.setFreezeTicks(200);

                            player.sendMessage(Configuration.getMessage("getting-cold"));
                            player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_HURT_FREEZE, 1f, 0f);
                        }
                    }

                }.runTaskTimer(plugin, 0L, 1L);
            }
            else if (this.coldness == Coldness.COLD) {
                player.setFreezeTicks(200);
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
        private boolean isPlayerHasArmor() {
            return this.player.getInventory().getBoots() != null &&
                   this.player.getInventory().getLeggings() != null &&
                   this.player.getInventory().getChestplate() != null &&
                   this.player.getInventory().getHelmet() != null;
        }
        private boolean isPlayerInColdBiome() {
            return this.player.getWorld().getBlockAt(this.player.getLocation()).getTemperature() <= 0.05;
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
            COZY
        }
        public enum Coldness {
            COLD,
            NORMAL,
            WARM
        }
    }

}
