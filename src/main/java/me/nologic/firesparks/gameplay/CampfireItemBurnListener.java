package me.nologic.firesparks.gameplay;

import me.nologic.firesparks.Firesparks;
import me.nologic.firesparks.gameplay.events.ItemBurnEvent;
import me.nologic.firesparks.utilities.Colors;
import me.nologic.firesparks.utilities.Configuration;
import me.nologic.firesparks.utilities.items.BurnableItem;
import me.nologic.firesparks.utilities.items.BurnableItemList;
import net.kyori.adventure.text.Component;
import org.bukkit.*;
import org.bukkit.block.Campfire;
import org.bukkit.entity.AreaEffectCloud;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.ItemDisplay;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import org.jetbrains.annotations.NotNull;

import javax.annotation.Nullable;
import java.util.List;

public class CampfireItemBurnListener implements Listener {

    private final BurnableItemList burnableItemList = Configuration.getItemList();

    @EventHandler
    public void onCampfireInteract(final PlayerInteractEvent event) {

        if (event.getPlayer().getGameMode().equals(GameMode.SPECTATOR)) return;
        if (event.getHand() == null) return;
        if (!event.getHand().equals(EquipmentSlot.HAND)) return;
        if (!event.getAction().isRightClick()) return;
        if (event.getClickedBlock() == null) return;
        if (!event.getClickedBlock().getType().equals(Material.CAMPFIRE)) return;

        Player player = event.getPlayer();
        ItemStack itemInHand = player.getInventory().getItemInMainHand();
        Campfire campfire = (Campfire) event.getClickedBlock().getState();

        for (BurnableItem burnableItem : burnableItemList.getItems()) {

            Material itemMaterial = burnableItem.getMaterial();

            if (itemInHand.getType().equals(itemMaterial)) {

                event.setCancelled(true);

                if (
                    getCampfireItem(event.getClickedBlock().getLocation()) != null ||
                    isCampfireHasFood(campfire)
                ) {
                    player.sendMessage(Configuration.getMessage("campfire-no-capacity"));
                    return;
                }

                if (!player.getGameMode().equals(GameMode.CREATIVE)) {
                    itemInHand.setAmount(itemInHand.getAmount() - 1);
                }

                burn(burnableItem, player, event.getClickedBlock().getLocation());

            }
        }
    }
    @EventHandler
    public void onCampfireBreak(BlockBreakEvent event) {

        Location location = event.getBlock().getLocation();

        if (getCampfireItem(location) != null && event.getBlock().getType().equals(Material.CAMPFIRE)) {
            getCampfireItem(location).remove();
        }

    }

    private void burn(final BurnableItem burnableItem, final Player player, final Location location) {

        ItemDisplay itemDisplay = (ItemDisplay) location.getWorld().spawnEntity(location.clone().add(0.1, 0.5, 0.4), EntityType.ITEM_DISPLAY);
        itemDisplay.setItemStack(new ItemStack(burnableItem.getMaterial(), 1));
        itemDisplay.setRotation(45f, 90f);
        itemDisplay.customName(Component.text("CampfireItem"));

        location.getWorld().playSound(location, Sound.BLOCK_FIRE_EXTINGUISH, 1f, 1f);

        new BukkitRunnable() {

            @Override
            public void run() {

                if (getCampfireItem(location) != null) {

                    int[] colors = Colors.fromString(burnableItem.getPotionColor());

                    location.getWorld().playSound(location, Sound.ENTITY_SHULKER_BULLET_HURT, 1f, 1f);
                    location.getWorld().playSound(location, Sound.ENTITY_SHULKER_SHOOT, 1f, 1f);
                    itemDisplay.remove();

                    AreaEffectCloud areaEffectCloud = (AreaEffectCloud) location.getWorld().spawnEntity(location.toCenterLocation(), EntityType.AREA_EFFECT_CLOUD);

                    PotionEffect potionEffect = new PotionEffect(burnableItem.getPotionEffect(), 20*burnableItem.getDuration(), 0);
                    Particle.DustOptions dustOptions = new Particle.DustOptions(Color.fromRGB(colors[0], colors[1], colors[2]), 1);

                    areaEffectCloud.setParticle(Particle.REDSTONE, dustOptions);
                    areaEffectCloud.setRadius(5f);
                    areaEffectCloud.setRadiusPerTick(-0.005f);
                    areaEffectCloud.addCustomEffect(potionEffect, false);

                    for (Object player : location.getNearbyPlayers(10).toArray()) {
                        ((Player) player).addPotionEffect(PotionEffectType.CONFUSION.createEffect(20 * 25, 0));
                        ((Player) player).sendMessage(Configuration.getMessage("item-burn"));
                    }

                    ItemBurnEvent itemBurnEvent = new ItemBurnEvent(burnableItem, player);
                    Bukkit.getPluginManager().callEvent(itemBurnEvent);
                }

                this.cancel();
            }
        }.runTaskLater(Firesparks.getPlugin(), 20L * burnableItem.getTimeToBurn());
    }

    @Nullable
    private ItemDisplay getCampfireItem(@NotNull final Location location) {

        List<ItemDisplay> itemDisplayList = location
                .getNearbyEntitiesByType(ItemDisplay.class, 1)
                .stream()
                .filter(itemDisplay -> itemDisplay.name().equals(Component.text("CampfireItem")))
                .toList();

        return itemDisplayList.isEmpty() ? null : itemDisplayList.get(0);
    }
    private boolean isCampfireHasFood(@NotNull final Campfire campfire) {
        for (int i = 0; i < campfire.getSize(); i++) {

            if (campfire.getItem(i) != null) {
                return true;
            }

        }
        return false;
    }
}
