package com.moulberry.flashback.compat;

import com.moulberry.flashback.Flashback;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.UUID;

/** Reconciles IV's internal rider link with the authoritative vanilla passenger relation. */
public final class ImmersiveVehiclesRiderCompat {

    private static volatile Reflection reflection;
    private static volatile boolean unavailable;

    private ImmersiveVehiclesRiderCompat() {
    }

    public static void reconcile(Entity vehicle, Entity passenger) {
        if (!Flashback.isInReplay() || vehicle == null || passenger == null) {
            return;
        }

        Reflection api = reflection();
        if (api == null || !api.linkedSeat().isInstance(vehicle)) {
            return;
        }

        try {
            Object internalEntity = api.entity().get(vehicle);
            if (internalEntity == null) {
                UUID entityUuid = (UUID) api.entityUuid().get(vehicle);
                if (entityUuid != null) {
                    Object world = api.getWrapperFor().invoke(null, vehicle.level());
                    internalEntity = api.getEntity().invoke(world, entityUuid);
                    if (internalEntity != null) {
                        api.entity().set(vehicle, internalEntity);
                    }
                }
            }

            if (internalEntity == null) {
                return;
            }

            Object wrapper = api.getWrapperForEntity().invoke(null, passenger);
            Object builderRider = api.rider().get(vehicle);
            Object internalRider = api.internalRider().get(internalEntity);
            // BuilderEntityLinkedSeat marks itself permanently dismounted whenever vanilla calls
            // its passenger-removed callback. Replay packet duplication used to trigger that hook
            // during an immediate eject/re-add. If the authoritative vanilla relation is mounted,
            // keep IV's removal countdown cleared as well.
            api.dismountedRider().setBoolean(vehicle, false);
            api.ticksWithoutRider().setInt(vehicle, 0);
            if (builderRider != wrapper) {
                api.rider().set(vehicle, wrapper);
            }
            if (internalRider != wrapper) {
                api.setRider().invoke(internalEntity, wrapper, true);
            }

        } catch (ReflectiveOperationException | RuntimeException exception) {
            unavailable = true;
            Flashback.LOGGER.error("Failed to reconcile an Immersive Vehicles replay rider", exception);
        }
    }

    private static Reflection reflection() {
        Reflection cached = reflection;
        if (cached != null || unavailable) {
            return cached;
        }
        synchronized (ImmersiveVehiclesRiderCompat.class) {
            if (reflection != null || unavailable) {
                return reflection;
            }
            try {
                ClassLoader loader = ImmersiveVehiclesRiderCompat.class.getClassLoader();
                Class<?> linkedSeat = Class.forName(
                        "mcinterface1201.BuilderEntityLinkedSeat", false, loader);
                Class<?> wrapperEntity = Class.forName(
                        "mcinterface1201.WrapperEntity", false, loader);
                Class<?> wrapperWorld = Class.forName(
                        "mcinterface1201.WrapperWorld", false, loader);
                Class<?> existingEntity = Class.forName(
                        "minecrafttransportsimulator.entities.components.AEntityB_Existing",
                        false, loader);
                Class<?> wrapperEntityInterface = Class.forName(
                        "minecrafttransportsimulator.mcinterface.IWrapperEntity", false, loader);

                reflection = new Reflection(
                        linkedSeat,
                        accessibleField(linkedSeat, "entity"),
                        accessibleField(linkedSeat, "entityUUID"),
                        accessibleField(linkedSeat, "rider"),
                        accessibleField(linkedSeat, "dismountedRider"),
                        accessibleField(linkedSeat, "ticksWithoutRider"),
                        accessibleField(existingEntity, "rider"),
                        wrapperEntity.getMethod("getWrapperFor", Entity.class),
                        wrapperWorld.getMethod("getWrapperFor", Level.class),
                        wrapperWorld.getMethod("getEntity", UUID.class),
                        existingEntity.getMethod("setRider", wrapperEntityInterface, boolean.class));
            } catch (ClassNotFoundException exception) {
                unavailable = true;
            } catch (ReflectiveOperationException | RuntimeException exception) {
                unavailable = true;
                Flashback.LOGGER.error(
                        "Immersive Vehicles replay rider compatibility could not initialize",
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
            Class<?> linkedSeat, Field entity, Field entityUuid, Field rider,
            Field dismountedRider, Field ticksWithoutRider, Field internalRider,
            Method getWrapperForEntity, Method getWrapperFor,
            Method getEntity, Method setRider) {
    }
}
