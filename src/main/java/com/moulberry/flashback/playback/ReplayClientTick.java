package com.moulberry.flashback.playback;

import com.moulberry.flashback.Flashback;
import com.moulberry.flashback.ext.MinecraftExt;
import net.minecraft.client.Minecraft;

/**
 * Distinguishes a genuinely paused replay from the one client tick requested after a seek.
 *
 * Minecraft 1.21 keeps the client tick manager separate from the replay server. In this 1.20.1
 * port the manager state is shared, so the server has already frozen it again by the time the
 * force-tick packet is handled. The scoped override restores the upstream behaviour without
 * allowing ordinary render frames to advance the replay world.
 */
public final class ReplayClientTick {

    private static int forcedTickDepth;

    private ReplayClientTick() {
    }

    public static void runForced(Runnable action) {
        forcedTickDepth++;
        try {
            action.run();
        } finally {
            forcedTickDepth--;
        }
    }

    public static boolean isForcing() {
        return forcedTickDepth > 0;
    }

    public static boolean runsNormally(Minecraft minecraft) {
        ReplayServer replayServer = Flashback.getReplayServer();
        if (replayServer == null || Flashback.isExporting() || isForcing()) {
            return true;
        }

        ReplayTimer replayTimer = ((MinecraftExt) minecraft).flashback$getReplayTimer();
        return !replayServer.replayPaused && replayTimer.manager.runsNormally();
    }
}
