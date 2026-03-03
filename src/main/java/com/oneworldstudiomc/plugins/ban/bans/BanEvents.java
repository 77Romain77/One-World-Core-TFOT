package com.oneworldstudiomc.plugins.ban.bans;

import com.oneworldstudiomc.OneWorldCoreConfig;

/**
 * @author Mgazul by OneWorldCore
 * @date 2023/8/9 20:09:51
 */
public class BanEvents {

    public static boolean banFireTick() {
        return OneWorldCoreConfig.doFireTick;
    }
}

