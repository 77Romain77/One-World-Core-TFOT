package com.oneworldstudiomc.paper.event.player;

import org.bukkit.entity.Player;
import org.bukkit.event.HandlerList;
import org.bukkit.event.player.PlayerEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Fired when a player stops using an item (e.g. releases a bow, stops blocking).
 * <p>
 * This event exists to provide a Paper-compatible event surface for plugins.
 */
public class PlayerStopUsingItemEvent extends PlayerEvent {
    private static final HandlerList HANDLER_LIST = new HandlerList();
    private final @Nullable ItemStack item;
    private final @NotNull EquipmentSlot hand;
    private final int remainingUseTicks;

    public PlayerStopUsingItemEvent(
            @NotNull final Player player,
            @Nullable final ItemStack item,
            @NotNull final EquipmentSlot hand,
            final int remainingUseTicks
    ) {
        super(player);
        this.item = item;
        this.hand = hand;
        this.remainingUseTicks = remainingUseTicks;
    }

    /**
     * @return the item the player was using, or null if unknown
     */
    public @Nullable ItemStack getItem() {
        return this.item;
    }

    /**
     * @return which hand was used
     */
    public @NotNull EquipmentSlot getHand() {
        return this.hand;
    }

    /**
     * @return remaining ticks of use when the stop happened
     */
    public int getRemainingUseTicks() {
        return this.remainingUseTicks;
    }

    @NotNull
    @Override
    public HandlerList getHandlers() {
        return HANDLER_LIST;
    }

    @NotNull
    public static HandlerList getHandlerList() {
        return HANDLER_LIST;
    }
}
