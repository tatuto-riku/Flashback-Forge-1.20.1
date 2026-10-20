package com.moulberry.flashback.compat;

import com.moulberry.flashback.Flashback;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.Entity;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;

/** Repairs IV handshake-to-wrapper pairing after a replay snapshot is reconstructed. */
public final class ImmersiveVehiclesHandshakeCompat {

    private static volatile Reflection reflection;
    private static volatile boolean unavailable;
    private static ClientLevel pendingLevel;
    private static final List<PendingHandshake> pendingHandshakes = new ArrayList<>();

    private ImmersiveVehiclesHandshakeCompat() {
    }

    public static void finishReplayHandshake(Object handshakePacket) {
        if (!Flashback.isInReplay()) {
            return;
        }
        ClientLevel level = Minecraft.getInstance().level;
        Reflection api = reflection();
        if (level == null || api == null) {
            return;
        }

        try {
            String builderId = (String) api.builderId().get(handshakePacket);
            Object wrappedNbt = api.data().get(handshakePacket);
            if (builderId == null || wrappedNbt == null
                    || !api.wrapperNbt().isInstance(wrappedNbt)) {
                return;
            }
            CompoundTag tag = ((CompoundTag) api.tag().get(wrappedNbt)).copy();
            String expectedBuilder = expectedBuilder(tag);
            if (expectedBuilder == null) {
                return;
            }

            Entity exact = null;
            for (Entity entity : level.entitiesForRendering()) {
                if (!entity.isRemoved() && api.builderBase().isInstance(entity)
                        && entity.getClass().getSimpleName().equals(expectedBuilder)
                        && entity.getStringUUID().equals(builderId)) {
                    exact = entity;
                    break;
                }
            }
            if (exact == null) {
                queue(level, new PendingHandshake(builderId, expectedBuilder, tag,
                        System.nanoTime()));
            } else {
                apply(api, exact, tag);
                // Snapshot payloads are delivered after vanilla entity pairing but before later
                // IV timeline packets. Initialize the internal IV object now, so a following part
                // or rider packet has a real target instead of being silently discarded.
                exact.tick();
            }
        } catch (IllegalAccessException | RuntimeException exception) {
            Flashback.LOGGER.error("Failed to finish an Immersive Vehicles replay handshake", exception);
        }
    }

    public static void applyPendingHandshake(Entity builder) {
        if (!Flashback.isInReplay() || builder == null || builder.isRemoved()) {
            return;
        }
        Reflection api = reflection();
        if (api == null || !api.builderBase().isInstance(builder)) {
            return;
        }

        PendingHandshake selected = null;
        synchronized (pendingHandshakes) {
            if (pendingLevel != builder.level()) {
                pendingHandshakes.clear();
                pendingLevel = builder.level() instanceof ClientLevel clientLevel ? clientLevel : null;
                return;
            }
            String kind = builder.getClass().getSimpleName();
            for (PendingHandshake pending : pendingHandshakes) {
                if (pending.expectedBuilder().equals(kind)
                        && pending.builderId().equals(builder.getStringUUID())) {
                    selected = pending;
                    break;
                }
            }
            if (selected == null) {
                long now = System.nanoTime();
                double bestDistance = Double.POSITIVE_INFINITY;
                for (PendingHandshake pending : pendingHandshakes) {
                    if (!pending.expectedBuilder().equals(kind)
                            || now - pending.queuedAtNanos() < 1_000_000_000L) {
                        continue;
                    }
                    CompoundTag tag = pending.tag();
                    boolean positioned = tag.contains("positionx")
                            || tag.contains("positiony") || tag.contains("positionz");
                    double distance = positioned
                            ? builder.distanceToSqr(tag.getDouble("positionx"),
                                    tag.getDouble("positiony"), tag.getDouble("positionz"))
                            : builder.getId();
                    if (distance < bestDistance) {
                        bestDistance = distance;
                        selected = pending;
                    }
                }
            }
            if (selected != null) {
                pendingHandshakes.remove(selected);
            }
        }

        if (selected != null) {
            try {
                apply(api, builder, selected.tag());
            } catch (IllegalAccessException | RuntimeException exception) {
                Flashback.LOGGER.error(
                        "Failed to apply a deferred Immersive Vehicles replay handshake", exception);
            }
        }
    }

    private static void queue(ClientLevel level, PendingHandshake pending) {
        synchronized (pendingHandshakes) {
            if (pendingLevel != level) {
                pendingHandshakes.clear();
                pendingLevel = level;
            }
            pendingHandshakes.removeIf(existing -> existing.builderId().equals(pending.builderId()));
            pendingHandshakes.add(pending);
        }
    }

    private static void apply(Reflection api, Entity target, CompoundTag tag)
            throws IllegalAccessException {
        api.lastLoadedNbt().set(target, tag.copy());
        api.loadedFromSavedNbt().setBoolean(target, false);
        api.loadFromSavedNbt().setBoolean(target, true);
    }

    private static String expectedBuilder(CompoundTag tag) {
        if (tag.contains("entityid")) {
            return "BuilderEntityExisting";
        }
        if (tag.contains("playerFollowing")) {
            return "BuilderEntityRenderForwarder";
        }
        if (tag.contains("entityUUID")) {
            return "BuilderEntityLinkedSeat";
        }
        return null;
    }

    private static Reflection reflection() {
        Reflection cached = reflection;
        if (cached != null || unavailable) {
            return cached;
        }
        synchronized (ImmersiveVehiclesHandshakeCompat.class) {
            if (reflection != null || unavailable) {
                return reflection;
            }
            try {
                ClassLoader loader = ImmersiveVehiclesHandshakeCompat.class.getClassLoader();
                Class<?> packet = Class.forName(
                        "mcinterface1201.PacketEntityCSHandshakeServer", false, loader);
                Class<?> builderBase = Class.forName("mcinterface1201.ABuilderEntityBase", false, loader);
                Class<?> wrapperNbt = Class.forName("mcinterface1201.WrapperNBT", false, loader);
                reflection = new Reflection(
                        builderBase, wrapperNbt,
                        accessibleField(packet, "builderID"), accessibleField(packet, "data"),
                        accessibleField(wrapperNbt, "tag"),
                        accessibleField(builderBase, "lastLoadedNBT"),
                        accessibleField(builderBase, "loadFromSavedNBT"),
                        accessibleField(builderBase, "loadedFromSavedNBT"));
            } catch (ClassNotFoundException exception) {
                unavailable = true;
            } catch (ReflectiveOperationException | RuntimeException exception) {
                unavailable = true;
                Flashback.LOGGER.error(
                        "Immersive Vehicles replay handshake compatibility could not initialize",
                        exception);
            }
            return reflection;
        }
    }

    private static Field accessibleField(Class<?> owner, String name) throws NoSuchFieldException {
        Field field = owner.getDeclaredField(name);
        field.setAccessible(true);
        return field;
    }

    private record Reflection(
            Class<?> builderBase, Class<?> wrapperNbt, Field builderId, Field data, Field tag,
            Field lastLoadedNbt, Field loadFromSavedNbt, Field loadedFromSavedNbt) {
    }

    private record PendingHandshake(
            String builderId, String expectedBuilder, CompoundTag tag, long queuedAtNanos) {
    }
}
