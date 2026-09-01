package com.moulberry.flashback;

import net.minecraft.client.Minecraft;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

/** Keeps the original split start/end tick callbacks while Forge supplies the event. */
public final class ClientTickHooks {

    public static final Hook START_CLIENT_TICK = new Hook();
    public static final Hook END_CLIENT_TICK = new Hook();

    private ClientTickHooks() {
    }

    public static final class Hook {
        private final List<Consumer<Minecraft>> callbacks = new ArrayList<>();

        public void register(Consumer<Minecraft> callback) {
            callbacks.add(callback);
        }

        public void fire(Minecraft minecraft) {
            for (Consumer<Minecraft> callback : callbacks) {
                callback.accept(minecraft);
            }
        }
    }
}
