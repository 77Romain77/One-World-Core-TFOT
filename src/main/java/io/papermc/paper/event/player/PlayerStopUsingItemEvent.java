package io.papermc.paper.event.player;

import org.bukkit.entity.Player;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Paper API compatibility wrapper for {@link com.oneworldstudiomc.paper.event.player.PlayerStopUsingItemEvent}.
 */
public class PlayerStopUsingItemEvent extends com.oneworldstudiomc.paper.event.player.PlayerStopUsingItemEvent {
    public PlayerStopUsingItemEvent(
            @NotNull final Player player,
            @Nullable final ItemStack item,
            @NotNull final EquipmentSlot hand,
            final int remainingUseTicks
    ) {
        super(player, item, hand, remainingUseTicks);
    }
}
