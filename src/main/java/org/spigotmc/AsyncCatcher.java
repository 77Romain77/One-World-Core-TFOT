package org.spigotmc;

import com.oneworldstudiomc.OneWorldCore;
import net.minecraft.server.MinecraftServer;

public class AsyncCatcher
{

    public static boolean enabled = true;

    public static void catchOp(String reason)
    {
        if ( enabled && Thread.currentThread() != MinecraftServer.getServer().serverThread )
        {
            throw new IllegalStateException(OneWorldCore.i18n.as("mohist.i18n.63", reason));
        }
    }

    public static boolean catchAsync()
    {
        if ( enabled && Thread.currentThread() != MinecraftServer.getServer().serverThread )
        {
            return true;
        }
        return false;
    }
}

