package com.oneworldstudiomc.paper.util;

import java.util.concurrent.Executor;
import net.minecraft.server.MinecraftServer;

/**
 * Paper API compatibility shim used by plugins that were compiled against Paper internals.
 */
public final class MCUtil {

    public static final Executor MAIN_EXECUTOR = runnable -> {
        MinecraftServer server = MinecraftServer.getServer();
        if (server != null) {
            server.executeIfPossible(runnable);
        } else {
            runnable.run();
        }
    };

    private MCUtil() {
    }
}
