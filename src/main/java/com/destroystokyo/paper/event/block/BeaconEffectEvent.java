package com.destroystokyo.paper.event.block;

import org.bukkit.block.Block;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;

/**
 * Compatibility placeholder for plugins probing legacy Paper classes.
 */
public class BeaconEffectEvent extends Event {

    private static final HandlerList HANDLERS = new HandlerList();
    private final Block block;

    public BeaconEffectEvent(@NotNull Block block) {
        this.block = block;
    }

    @NotNull
    public Block getBlock() {
        return block;
    }

    @Override
    @NotNull
    public HandlerList getHandlers() {
        return HANDLERS;
    }

    @NotNull
    public static HandlerList getHandlerList() {
        return HANDLERS;
    }
}
