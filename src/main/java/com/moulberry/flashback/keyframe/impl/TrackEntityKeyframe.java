package com.moulberry.flashback.keyframe.impl;

import com.google.gson.*;
import com.moulberry.flashback.Interpolation;
import com.moulberry.flashback.combo_options.TrackingBodyPart;
import com.moulberry.flashback.editor.ui.ImGuiHelper;
import com.moulberry.flashback.keyframe.Keyframe;
import com.moulberry.flashback.keyframe.KeyframeType;
import com.moulberry.flashback.keyframe.handler.KeyframeHandler;
import com.moulberry.flashback.keyframe.interpolation.InterpolationType;
import com.moulberry.flashback.keyframe.types.TrackEntityKeyframeType;
import com.moulberry.flashback.spline.CatmullRom;
import imgui.ImGui;
import imgui.type.ImString;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3d;

import java.lang.reflect.Type;
import java.util.UUID;
import java.util.function.Consumer;

public class TrackEntityKeyframe extends Keyframe {

    public UUID target;
    public TrackingBodyPart trackingBodyPart;
    public float yawOffset;
    public float pitchOffset;
    public final Vector3d positionOffset;
    public final Vector3d viewOffset;
    public float roll;

    public TrackEntityKeyframe(UUID target, TrackingBodyPart trackingBodyPart, float yawOffset, float pitchOffset,
                                Vector3d positionOffset, Vector3d viewOffset, float roll) {
        this(target, trackingBodyPart, yawOffset, pitchOffset, positionOffset, viewOffset, roll, InterpolationType.getDefault());
    }

    public TrackEntityKeyframe(UUID target, TrackingBodyPart trackingBodyPart, float yawOffset, float pitchOffset,
                                Vector3d positionOffset, Vector3d viewOffset, float roll, InterpolationType interpolationType) {
        this.target = target;
        this.trackingBodyPart = trackingBodyPart;
        this.yawOffset = yawOffset;
        this.pitchOffset = pitchOffset;
        this.positionOffset = positionOffset;
        this.viewOffset = viewOffset;
        this.roll = roll;
        this.interpolationType(interpolationType);
    }

    @Override
    public KeyframeType<?> keyframeType() {
        return TrackEntityKeyframeType.INSTANCE;
    }

    @Override
    public Keyframe copy() {
        return new TrackEntityKeyframe(this.target, this.trackingBodyPart, this.yawOffset, this.pitchOffset,
            new Vector3d(this.positionOffset), new Vector3d(this.viewOffset), this.roll, this.interpolationType());
    }

    @Override
    public void renderEditKeyframe(Consumer<Consumer<Keyframe>> update) {
        ImString target = new ImString(this.target.toString(), 64);
        if (ImGui.inputText("Entity UUID", target)) {
            try {
                UUID uuid = UUID.fromString(ImGuiHelper.getString(target));
                ClientLevel level = Minecraft.getInstance().level;
                if (level != null) {
                    Entity entity = level.getEntities().get(uuid);
                    if (entity != null && entity != Minecraft.getInstance().player) {
                        update.accept(keyframe -> ((TrackEntityKeyframe) keyframe).target = uuid);
                    }
                }
            } catch (Exception ignored) {
            }
        }

        TrackingBodyPart trackingBodyPart = ImGuiHelper.enumCombo("Body Part", this.trackingBodyPart, TrackingBodyPart.values());
        if (trackingBodyPart != null && trackingBodyPart != this.trackingBodyPart) {
            update.accept(keyframe -> ((TrackEntityKeyframe) keyframe).trackingBodyPart = trackingBodyPart);
        }

        float[] input = new float[]{this.yawOffset};
        if (ImGuiHelper.inputFloat("Yaw Offset", input) && input[0] != this.yawOffset) {
            float yawOffset = input[0];
            update.accept(keyframe -> ((TrackEntityKeyframe) keyframe).yawOffset = yawOffset);
        }

        input[0] = this.pitchOffset;
        if (ImGuiHelper.inputFloat("Pitch Offset", input) && input[0] != this.pitchOffset) {
            float pitchOffset = input[0];
            update.accept(keyframe -> ((TrackEntityKeyframe) keyframe).pitchOffset = pitchOffset);
        }

        float[] positionOffset = new float[]{(float) this.positionOffset.x, (float) this.positionOffset.y, (float) this.positionOffset.z};
        if (ImGuiHelper.inputFloat("Position Offset", positionOffset)) {
            if (positionOffset[0] != this.positionOffset.x) {
                update.accept(keyframe -> ((TrackEntityKeyframe) keyframe).positionOffset.x = positionOffset[0]);
            }
            if (positionOffset[1] != this.positionOffset.y) {
                update.accept(keyframe -> ((TrackEntityKeyframe) keyframe).positionOffset.y = positionOffset[1]);
            }
            if (positionOffset[2] != this.positionOffset.z) {
                update.accept(keyframe -> ((TrackEntityKeyframe) keyframe).positionOffset.z = positionOffset[2]);
            }
        }

        float[] viewOffset = new float[]{(float) this.viewOffset.x, (float) this.viewOffset.y, (float) this.viewOffset.z};
        if (ImGuiHelper.inputFloat("View Offset", viewOffset)) {
            if (viewOffset[0] != this.viewOffset.x) {
                update.accept(keyframe -> ((TrackEntityKeyframe) keyframe).viewOffset.x = viewOffset[0]);
            }
            if (viewOffset[1] != this.viewOffset.y) {
                update.accept(keyframe -> ((TrackEntityKeyframe) keyframe).viewOffset.y = viewOffset[1]);
            }
            if (viewOffset[2] != this.viewOffset.z) {
                update.accept(keyframe -> ((TrackEntityKeyframe) keyframe).viewOffset.z = viewOffset[2]);
            }
        }

        input[0] = this.roll;
        if (ImGuiHelper.inputFloat("Roll", input) && input[0] != this.roll) {
            float roll = input[0];
            update.accept(keyframe -> ((TrackEntityKeyframe) keyframe).roll = roll);
        }
    }

    @Override
    public void apply(KeyframeHandler keyframeHandler) {
        applyTracking(keyframeHandler, this.target, this.trackingBodyPart, this.yawOffset, this.pitchOffset, this.positionOffset, this.viewOffset, this.roll);
    }

    @Override
    public void applyInterpolated(KeyframeHandler keyframeHandler, Keyframe genericOther, float amount) {
        if (!(genericOther instanceof TrackEntityKeyframe other)) {
            this.apply(keyframeHandler);
            return;
        }

        UUID target = amount < 0.5f ? this.target : other.target;
        TrackingBodyPart trackingBodyPart = amount < 0.5f ? this.trackingBodyPart : other.trackingBodyPart;
        float yawOffset = Interpolation.linear(this.yawOffset, other.yawOffset, amount);
        float pitchOffset = Interpolation.linear(this.pitchOffset, other.pitchOffset, amount);
        Vector3d positionOffset = this.positionOffset.lerp(other.positionOffset, amount, new Vector3d());
        Vector3d viewOffset = this.viewOffset.lerp(other.viewOffset, amount, new Vector3d());
        float roll = Interpolation.linearAngle(this.roll, other.roll, amount);

        applyTracking(keyframeHandler, target, trackingBodyPart, yawOffset, pitchOffset, positionOffset, viewOffset, roll);
    }

    @Override
    public void applyInterpolatedSmooth(KeyframeHandler keyframeHandler, Keyframe p1, Keyframe p2, Keyframe p3, float t0, float t1, float t2, float t3, float amount,
                                         float lerpAmount, boolean lerpFromRight) {
        float time1 = t1 - t0;
        float time2 = t2 - t0;
        float time3 = t3 - t0;

        UUID target = amount < 0.5f ? ((TrackEntityKeyframe) p1).target : ((TrackEntityKeyframe) p2).target;
        TrackingBodyPart trackingBodyPart = amount < 0.5f ? ((TrackEntityKeyframe) p1).trackingBodyPart : ((TrackEntityKeyframe) p2).trackingBodyPart;

        float yawOffset = CatmullRom.value(this.yawOffset, ((TrackEntityKeyframe) p1).yawOffset,
            ((TrackEntityKeyframe) p2).yawOffset, ((TrackEntityKeyframe) p3).yawOffset, time1, time2, time3, amount);
        float pitchOffset = CatmullRom.value(this.pitchOffset, ((TrackEntityKeyframe) p1).pitchOffset,
            ((TrackEntityKeyframe) p2).pitchOffset, ((TrackEntityKeyframe) p3).pitchOffset, time1, time2, time3, amount);
        Vector3d positionOffset = CatmullRom.position(this.positionOffset, ((TrackEntityKeyframe) p1).positionOffset,
            ((TrackEntityKeyframe) p2).positionOffset, ((TrackEntityKeyframe) p3).positionOffset, time1, time2, time3, amount);
        Vector3d viewOffset = CatmullRom.position(this.viewOffset, ((TrackEntityKeyframe) p1).viewOffset,
            ((TrackEntityKeyframe) p2).viewOffset, ((TrackEntityKeyframe) p3).viewOffset, time1, time2, time3, amount);
        float roll = CatmullRom.degrees(this.roll, ((TrackEntityKeyframe) p1).roll,
            ((TrackEntityKeyframe) p2).roll, ((TrackEntityKeyframe) p3).roll, time1, time2, time3, amount);

        applyTracking(keyframeHandler, target, trackingBodyPart, yawOffset, pitchOffset, positionOffset, viewOffset, roll);
    }

    private static void applyTracking(KeyframeHandler keyframeHandler, UUID target, TrackingBodyPart trackingBodyPart, float yawOffset, float pitchOffset,
                                       Vector3d positionOffset, Vector3d viewOffset, float roll) {
        Minecraft minecraft = Minecraft.getInstance();
        ClientLevel level = minecraft.level;
        if (level == null) {
            return;
        }

        Entity entity = level.getEntities().get(target);
        if (entity == null) {
            return;
        }

        float partialTick = minecraft.getFrameTime();

        Vec3 limbPosition;
        float yaw;
        float pitch;
        switch (trackingBodyPart) {
            case HEAD -> {
                limbPosition = entity.getEyePosition(partialTick);
                if (entity instanceof LivingEntity livingEntity) {
                    yaw = Mth.rotLerp(partialTick, livingEntity.yHeadRotO, livingEntity.yHeadRot);
                } else {
                    yaw = entity.getViewYRot(partialTick);
                }
                pitch = entity.getViewXRot(partialTick);
            }
            case BODY -> {
                limbPosition = entity.getPosition(partialTick).add(0, entity.getBbHeight() * 0.5f, 0);
                if (entity instanceof LivingEntity livingEntity) {
                    yaw = Mth.rotLerp(partialTick, livingEntity.yBodyRotO, livingEntity.yBodyRot);
                } else {
                    yaw = entity.getViewYRot(partialTick);
                }
                pitch = 0.0f;
            }
            default -> {
                limbPosition = entity.getPosition(partialTick);
                yaw = 0.0f;
                pitch = 0.0f;
            }
        }

        pitch = pitch * (float) Math.cos(Math.toRadians(yawOffset));
        yaw += yawOffset;
        pitch += pitchOffset;

        Vector3d offset = new Vector3d(viewOffset);
        offset.rotateX(Math.toRadians(-pitch));
        offset.rotateY(Math.toRadians(180.0f - yaw));

        Vector3d position = new Vector3d(
            limbPosition.x + positionOffset.x + offset.x,
            limbPosition.y + positionOffset.y + offset.y,
            limbPosition.z + positionOffset.z + offset.z
        );

        LocalPlayer player = minecraft.player;
        if (player != null) {
            position.y -= player.getEyeHeight();
        }

        keyframeHandler.applyCameraPosition(position, yaw, pitch, roll);
    }

    public static class TypeAdapter implements JsonSerializer<TrackEntityKeyframe>, JsonDeserializer<TrackEntityKeyframe> {
        @Override
        public TrackEntityKeyframe deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException {
            JsonObject jsonObject = json.getAsJsonObject();
            UUID target = context.deserialize(jsonObject.get("target"), UUID.class);
            TrackingBodyPart bodyPart = context.deserialize(jsonObject.get("bodyPart"), TrackingBodyPart.class);
            float yawOffset = jsonObject.get("yawOffset").getAsFloat();
            float pitchOffset = jsonObject.get("pitchOffset").getAsFloat();
            Vector3d positionOffset = context.deserialize(jsonObject.get("positionOffset"), Vector3d.class);
            Vector3d viewOffset = context.deserialize(jsonObject.get("viewOffset"), Vector3d.class);
            float roll = jsonObject.has("roll") ? jsonObject.get("roll").getAsFloat() : 0.0f;
            InterpolationType interpolationType = context.deserialize(jsonObject.get("interpolation_type"), InterpolationType.class);
            return new TrackEntityKeyframe(target, bodyPart, yawOffset, pitchOffset,
                positionOffset != null ? positionOffset : new Vector3d(),
                viewOffset != null ? viewOffset : new Vector3d(), roll, interpolationType);
        }

        @Override
        public JsonElement serialize(TrackEntityKeyframe src, Type typeOfSrc, JsonSerializationContext context) {
            JsonObject jsonObject = new JsonObject();
            jsonObject.add("target", context.serialize(src.target));
            jsonObject.add("bodyPart", context.serialize(src.trackingBodyPart));
            jsonObject.addProperty("yawOffset", src.yawOffset);
            jsonObject.addProperty("pitchOffset", src.pitchOffset);
            jsonObject.add("positionOffset", context.serialize(src.positionOffset));
            jsonObject.add("viewOffset", context.serialize(src.viewOffset));
            jsonObject.addProperty("roll", src.roll);
            jsonObject.addProperty("type", "track_entity");
            jsonObject.add("interpolation_type", context.serialize(src.interpolationType()));
            return jsonObject;
        }
    }
}
