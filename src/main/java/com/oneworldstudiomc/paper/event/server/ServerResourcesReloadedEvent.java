package com.oneworldstudiomc.paper.event.server;

import org.bukkit.event.HandlerList;
import org.bukkit.event.server.ServerEvent;

/**
 * Called after server resources such as datapacks have been reloaded.
 *
 * <p>This compatibility event mirrors Paper's
 * {@code io.papermc.paper.event.server.ServerResourcesReloadedEvent} API under
 * OneWorldCore's remapped Paper namespace.</p>
 */
public class ServerResourcesReloadedEvent extends ServerEvent {

    public static final HandlerList HANDLER_LIST = new HandlerList();

    private final Cause cause;

    public ServerResourcesReloadedEvent(Cause cause) {
        this.cause = cause;
    }

    public Cause getCause() {
        return this.cause;
    }

    public static HandlerList getHandlerList() {
        return HANDLER_LIST;
    }

    @Override
    public HandlerList getHandlers() {
        return HANDLER_LIST;
    }

    public enum Cause {
        COMMAND,
        PLUGIN
    }
}
