package com.moulberry.flashback.compat;

import com.moulberry.flashback.Flashback;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundCustomPayloadPacket;
import net.minecraft.world.entity.Entity;
import net.minecraftforge.network.NetworkDirection;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.List;

/** Captures the client-only state used by Immersive Vehicles' entity handshake. */
public final class ImmersiveVehiclesReplayCompat {

    private static volatile Reflection reflection;
    private static volatile boolean unavailable;

    private ImmersiveVehiclesReplayCompat() {
    }

    public static void appendSnapshotPackets(
            ClientLevel level, List<Packet<? super ClientGamePacketListener>> packets) {
        Reflection api = reflection();
        if (api == null) {
            return;
        }

        for (Entity entity : level.entitiesForRendering()) {
            if (entity.isRemoved() || !api.builderEntity().isInstance(entity)) {
                continue;
            }
            try {
                CompoundTag tag = entity.saveWithoutId(new CompoundTag());
                Object wrappedNbt = api.wrapperNbt().newInstance(tag.copy());
                Object handshake = api.handshake().newInstance(entity, wrappedNbt);
                Object wrapper = api.wrapperPacket().newInstance(handshake);
                Object vanillaPacket = api.toVanillaPacket().invoke(
                        api.network(), wrapper, NetworkDirection.PLAY_TO_CLIENT);
                if (vanillaPacket instanceof ClientboundCustomPayloadPacket customPayload) {
                    packets.add(customPayload);
                }
            } catch (ReflectiveOperationException | RuntimeException exception) {
                Flashback.LOGGER.error(
                        "Failed to capture Immersive Vehicles replay snapshot for entity {} ({})",
                        entity.getId(), entity.getType(), exception);
            }
        }
    }

    private static Reflection reflection() {
        Reflection cached = reflection;
        if (cached != null || unavailable) {
            return cached;
        }
        synchronized (ImmersiveVehiclesReplayCompat.class) {
            if (reflection != null || unavailable) {
                return reflection;
            }
            try {
                ClassLoader loader = ImmersiveVehiclesReplayCompat.class.getClassLoader();
                Class<?> builderEntity = Class.forName("mcinterface1201.ABuilderEntityBase", false, loader);
                Class<?> wrapperNbtClass = Class.forName("mcinterface1201.WrapperNBT", false, loader);
                Class<?> wrapperNbtInterface = Class.forName(
                        "minecrafttransportsimulator.mcinterface.IWrapperNBT", false, loader);
                Class<?> handshakeClass = Class.forName(
                        "mcinterface1201.PacketEntityCSHandshakeServer", false, loader);
                Class<?> packetBase = Class.forName(
                        "minecrafttransportsimulator.packets.components.APacketBase", false, loader);
                Class<?> wrapperPacketClass = Class.forName(
                        "mcinterface1201.InterfacePacket$WrapperPacket", false, loader);
                Class<?> interfacePacketClass = Class.forName("mcinterface1201.InterfacePacket", false, loader);

                Constructor<?> wrapperNbt = wrapperNbtClass.getDeclaredConstructor(CompoundTag.class);
                wrapperNbt.setAccessible(true);
                Constructor<?> handshake = handshakeClass.getConstructor(builderEntity, wrapperNbtInterface);
                Constructor<?> wrapperPacket = wrapperPacketClass.getConstructor(packetBase);
                Field networkField = interfacePacketClass.getDeclaredField("network");
                networkField.setAccessible(true);
                Object network = networkField.get(null);
                Method toVanillaPacket = network.getClass().getMethod(
                        "toVanillaPacket", Object.class, NetworkDirection.class);

                reflection = new Reflection(builderEntity, wrapperNbt, handshake, wrapperPacket,
                        network, toVanillaPacket);
            } catch (ClassNotFoundException exception) {
                unavailable = true;
            } catch (ReflectiveOperationException | RuntimeException exception) {
                unavailable = true;
                Flashback.LOGGER.error(
                        "Immersive Vehicles replay compatibility could not initialize", exception);
            }
            return reflection;
        }
    }

    private record Reflection(
            Class<?> builderEntity,
            Constructor<?> wrapperNbt,
            Constructor<?> handshake,
            Constructor<?> wrapperPacket,
            Object network,
            Method toVanillaPacket) {
    }
}
