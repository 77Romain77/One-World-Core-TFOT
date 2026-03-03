package com.oneworldstudiomc.util;

import com.oneworldstudiomc.OneWorldCoreConfig;
import org.spigotmc.SpigotConfig;

public class ProxyUtils {

    public static boolean is() {
        return OneWorldCoreConfig.velocity_enabled || SpigotConfig.bungee;
    }
}

