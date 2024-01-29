package me.nologic.firesparks.gameplay.events;

import me.nologic.firesparks.utilities.items.BurnableItem;
import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;

public class ItemBurnEvent extends Event implements Cancellable {

    private static final HandlerList handlers = new HandlerList();

    private final BurnableItem burnableItem;
    private final Player player;
    private boolean isCancelled;

    public ItemBurnEvent(final BurnableItem burnableItem, final Player player) {
        this.burnableItem = burnableItem;
        this.player = player;
        this.isCancelled = false;
    }

    public Player getPlayer() {
        return player;
    }
    public BurnableItem getBurnableItem() {
        return burnableItem;
    }

    @Override
    public boolean isCancelled() {
        return false;
    }

    @Override
    public void setCancelled(boolean cancelled) {
        this.isCancelled = cancelled;
    }

    @Override
    public @NotNull HandlerList getHandlers() {
        return handlers;
    }
    public static HandlerList getHandlerList() {
        return handlers;
    }
}
