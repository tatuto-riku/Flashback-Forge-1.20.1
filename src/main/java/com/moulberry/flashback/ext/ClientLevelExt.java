package com.moulberry.flashback.ext;

public interface ClientLevelExt {

    int flashback$clearQueuedLightUpdates();

    int flashback$drainQueuedLightUpdates();

    void flashback$queueLightRechecks(long[] positions);

    int flashback$applyQueuedLightRechecks();
}
