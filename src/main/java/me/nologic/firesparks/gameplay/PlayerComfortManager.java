package me.nologic.firesparks.gameplay;

import dev.geco.gsit.api.GSitAPI;
import lombok.Getter;
import lombok.Setter;
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
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class PlayerComfortManager implements Listener {

    private final Firesparks plugin;
    private final Map<Player, ComfortData> comfortDataMap;
    private final NamespacedKey            comfortNamespacedKey;
    private final int                      comfortPerTick;

    public PlayerComfortManager(final Firesparks plugin) {
        this.plugin = plugin;
        this.comfortDataMap = new ConcurrentHashMap<>();
        this.comfortPerTick = Configuration.getComfortGrowthAmount();
        this.comfortNamespacedKey = new NamespacedKey(plugin, "comfort");
        plugin.getServer().getScheduler().runTaskTimerAsynchronously(plugin, this::tick, 0L, 20L);
    }
    
    private void tick() {
        this.comfortDataMap.values().forEach(data -> {

            // The comfort date (block scan and increment/decrement) is calculated asynchronously from the main server thread.
            data.update();

            // In case the comfort level is COZY, the player is near campfire, and he is sitting, we apply the regeneration effect to him.
            if (data.isDelightfulness()) {

                final Player player = data.getPlayer();

                // Of course, interaction with the player is handled in the main thread of the server.
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
        final ComfortData data = new ComfortData(event.getPlayer(), personalPlayerBar, this.loadComfortData(event.getPlayer()));
        this.comfortDataMap.put(event.getPlayer(), data);
    }
    
    @EventHandler
    private void whenPlayerQuit(final PlayerQuitEvent event) {
        this.saveComfortData(event.getPlayer());
    }

    private int loadComfortData(final Player player) {
        return player.getPersistentDataContainer().getOrDefault(comfortNamespacedKey, PersistentDataType.INTEGER, 0);
    }

    private void saveComfortData(final Player player) {
        player.getPersistentDataContainer().set(comfortNamespacedKey, PersistentDataType.INTEGER, comfortDataMap.get(player).getComfortAmount());
        this.comfortDataMap.remove(player);
    }
    
    @EventHandler
    private void whenPlayerTakesDamage(final EntityDamageEvent event) {
        final Entity entity = event.getEntity();
        if (entity instanceof Player player) {
            final ComfortData data = this.comfortDataMap.get(player);
            if (data != null) {
                data.setComfortAmount((int)(data.getComfortAmount() - event.getFinalDamage() * 50.0));
                data.getBar().progress((float)(data.getComfortAmount() / 1000));
            }
        }
    }

    @EventHandler
    private void whenPlayerRespawns(final PlayerRespawnEvent event) {

        if (!Configuration.getColdness()) return;

        final Player player = event.getPlayer();
        if (player.getFreezeTicks() > 0) {
            new BukkitRunnable() {

                @Override
                public void run() {
                    player.setFreezeTicks(0);
                }

            }.runTaskLater(plugin, 1L);
        }
    }

    @Getter
    public final class ComfortData {

        private final Player  player;
        private final BossBar bar;

        private Coldness coldness     = Coldness.NORMAL;
        private Boolean  coldnessTask = false;

        private ComfortLevel comfortLevel;
        private int          comfortAmount;

        private boolean nearFire;
        private boolean hasArmor;
        private boolean sitting;

        @Setter
        private boolean musicPlaying;

        public ComfortData(final Player player, final BossBar bar, final int level) {
            this.player = player;
            this.bar    = bar;
            this.comfortAmount = level;
        }
        
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
            if (this.comfortAmount < 100) {
                this.comfortLevel = ComfortLevel.LOW;
            }
            else if (this.comfortAmount < 600) {
                this.comfortLevel = ComfortLevel.NORMAL;
                this.musicPlaying = false;
            }
            else if (this.comfortAmount < 800) {
                if (this.comfortLevel == ComfortLevel.COZY) {
                    this.player.sendMessage(Configuration.getMessage("delightfulness"));
                }
                this.comfortLevel = ComfortLevel.NICE;
            }
            else {
                this.comfortLevel = ComfortLevel.COZY;
            }
        }
        
        public boolean isDelightfulness() {
            return this.comfortLevel == ComfortLevel.COZY && this.nearFire && this.sitting;
        }
        
        public void setComfortAmount(int value) {
            if (value < 0) {
                value = 0;
            }
            else if (value > 1000) {
                value = 1000;
            }
            this.comfortAmount = value;
        }
        
        public void showBossBar(final float level) {

            switch (this.comfortLevel) {
                case LOW -> this.bar.color(BossBar.Color.RED);
                case NORMAL -> this.bar.color(BossBar.Color.YELLOW);
                case NICE -> this.bar.color(BossBar.Color.BLUE);
                case COZY -> this.bar.color(BossBar.Color.GREEN);
            }

            this.bar.progress(level / 1000.0f);
            this.player.showBossBar(this.bar);
            Bukkit.getServer().getScheduler().runTaskLater(PlayerComfortManager.this.plugin, () -> {
                if (level >= this.comfortAmount) {
                    this.player.hideBossBar(this.bar);
                }
            }, 100L);
        }
        
        private void increment() {
            if (this.comfortAmount != 1000) {
                if (this.comfortAmount + comfortPerTick > 1000) {
                    this.comfortAmount = 1000;
                }
                else {
                    this.comfortAmount += comfortPerTick;
                }
                this.restate();
                this.showBossBar((float)this.comfortAmount);
            }
        }
        
        private void decrement() {
            if (this.comfortAmount != 0) {
                if (this.comfortAmount - 2 < 0) {
                    this.comfortAmount = 0;
                }
                else {
                    this.comfortAmount -= 2;
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
        
        public enum ComfortLevel {
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