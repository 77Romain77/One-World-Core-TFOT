package org.bukkit.inventory.meta;

import org.bukkit.NamespacedKey;
import org.bukkit.persistence.PersistentDataType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Represents an item that has durability and can take damage.
 */
public interface Damageable extends ItemMeta {

    /**
     * Checks to see if this item has damage.
     *
     * @return true if this has damage
     */
    boolean hasDamage();

    /**
     * Gets the damage.
     *
     * @return the damage
     */
    int getDamage();

    /**
     * Sets the damage.
     *
     * @param damage item damage
     */
    void setDamage(int damage);

    /**
     * Checks whether this item meta has a custom maximum damage value.
     *
     * <p>The 1.20.1 backport stores the value in the item's persistent data
     * only when a plugin explicitly calls {@link #setMaxDamage(Integer)}.
     * Ordinary items therefore receive no additional tag.</p>
     *
     * @return true when a custom maximum damage value is present
     */
    default boolean hasMaxDamage() {
        return this.getPersistentDataContainer().has(MaxDamageStorage.KEY, PersistentDataType.INTEGER);
    }

    /**
     * Gets the custom maximum damage value.
     *
     * @return custom maximum damage
     * @throws IllegalStateException when no custom value is present
     */
    default int getMaxDamage() {
        Integer maxDamage = this.getPersistentDataContainer().get(MaxDamageStorage.KEY, PersistentDataType.INTEGER);
        if (maxDamage == null) {
            throw new IllegalStateException("We don't have max_damage! Check hasMaxDamage first!");
        }
        return maxDamage;
    }

    /**
     * Sets or removes the custom maximum damage value.
     *
     * @param maxDamage a strictly positive maximum damage value, or null to
     *                  remove the custom value
     */
    default void setMaxDamage(@Nullable Integer maxDamage) {
        if (maxDamage == null) {
            this.getPersistentDataContainer().remove(MaxDamageStorage.KEY);
            return;
        }
        if (maxDamage <= 0) {
            throw new IllegalArgumentException("maxDamage must be greater than 0");
        }
        this.getPersistentDataContainer().set(MaxDamageStorage.KEY, PersistentDataType.INTEGER, maxDamage);
    }

    @NotNull
    @Override
    Damageable clone();

    /**
     * Internal storage holder for the 1.20.1 Paper API backport.
     */
    final class MaxDamageStorage {
        private static final NamespacedKey KEY = new NamespacedKey("oneworldcore", "max_damage");

        private MaxDamageStorage() {
        }
    }
}
