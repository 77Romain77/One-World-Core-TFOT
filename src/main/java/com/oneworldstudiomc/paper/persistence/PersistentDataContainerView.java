package com.oneworldstudiomc.paper.persistence;

/**
 * Paper-compatible read-only persistent data container view.
 * <p>
 * On this core the backing implementation is the regular Bukkit persistent
 * data container, so this interface extends it for binary compatibility.
 */
public interface PersistentDataContainerView extends org.bukkit.persistence.PersistentDataContainer {
}
