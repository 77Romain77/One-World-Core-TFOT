package org.bukkit.entity;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Represents an instance of a lightning strike. May or may not do damage.
 */
public interface LightningStrike extends Entity {

    /**
     * Returns whether the strike is an effect that does no damage.
     *
     * @return whether the strike is an effect
     */
    public boolean isEffect();

    /**
     * Returns the entity that caused this lightning strike (for example, a player
     * using a channeled trident), if known.
     *
     * @return causing entity, or {@code null} when not available
     */
    @Nullable
    default Entity getCausingEntity() {
        return null;
    }

    // Spigot start
    public class Spigot extends Entity.Spigot {

        /*
         * Returns whether the strike is silent.
         *
         * @return whether the strike is silent.
         */
        public boolean isSilent() {
            throw new UnsupportedOperationException("Not supported yet.");
        }
    }

    @NotNull
    @Override
    Spigot spigot();
    // Spigot end
}
