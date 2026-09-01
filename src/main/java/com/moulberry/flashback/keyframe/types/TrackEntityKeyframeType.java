package com.moulberry.flashback.keyframe.types;

import com.moulberry.flashback.combo_options.TrackingBodyPart;
import com.moulberry.flashback.editor.ui.ImGuiHelper;
import com.moulberry.flashback.keyframe.KeyframeType;
import com.moulberry.flashback.keyframe.impl.TrackEntityKeyframe;
import imgui.ImGui;
import imgui.type.ImString;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.Entity;
import net.minecraft.client.multiplayer.ClientLevel;
import org.jetbrains.annotations.Nullable;
import org.joml.Vector3d;

import java.util.UUID;

public class TrackEntityKeyframeType implements KeyframeType<TrackEntityKeyframe> {

    public static TrackEntityKeyframeType INSTANCE = new TrackEntityKeyframeType();

    private TrackEntityKeyframeType() {
    }

    @Override
    public String name() {
        return "Track Entity";
    }

    @Override
    public String id() {
        return "TRACK_ENTITY";
    }

    @Override
    public @Nullable TrackEntityKeyframe createDirect() {
        return null;
    }

    @Override
    public KeyframeCreatePopup<TrackEntityKeyframe> createPopup() {
        CameraTrackTargetData data = new CameraTrackTargetData();

        return () -> {
            if (ImGui.inputText("Entity UUID", data.target)) {
                data.validEntityTarget = null;
                try {
                    UUID uuid = UUID.fromString(ImGuiHelper.getString(data.target));
                    ClientLevel level = Minecraft.getInstance().level;
                    if (level != null) {
                        Entity entity = level.getEntities().get(uuid);
                        if (entity != null && entity != Minecraft.getInstance().player) {
                            data.validEntityTarget = uuid;
                        }
                    }
                } catch (Exception ignored) {
                }
            }

            data.trackingBodyPart = ImGuiHelper.enumCombo("Body Part", data.trackingBodyPart, TrackingBodyPart.values());
            ImGuiHelper.inputFloat("Yaw Offset", data.yawOffset);
            ImGuiHelper.inputFloat("Pitch Offset", data.pitchOffset);
            ImGuiHelper.inputFloat("Position Offset", data.positionOffset);
            ImGuiHelper.inputFloat("View Offset", data.viewOffset);
            ImGuiHelper.inputFloat("Roll", data.roll);

            boolean disabled = data.validEntityTarget == null;
            if (disabled) {
                ImGui.beginDisabled();
            }
            boolean add = ImGui.button("Add");
            if (disabled) {
                ImGui.endDisabled();
            }

            ImGui.sameLine();
            if (ImGui.button("Cancel")) {
                ImGui.closeCurrentPopup();
            }

            if (add && data.validEntityTarget != null) {
                Vector3d positionOffset = new Vector3d(data.positionOffset[0], data.positionOffset[1], data.positionOffset[2]);
                Vector3d viewOffset = new Vector3d(data.viewOffset[0], data.viewOffset[1], data.viewOffset[2]);
                return new TrackEntityKeyframe(data.validEntityTarget, data.trackingBodyPart, data.yawOffset[0], data.pitchOffset[0],
                    positionOffset, viewOffset, data.roll[0]);
            }
            return null;
        };
    }

    private static class CameraTrackTargetData {
        float[] positionOffset = new float[]{0.0f, 0.0f, 0.0f};
        float[] viewOffset = new float[]{0.0f, 0.0f, 3.0f};
        float[] yawOffset = new float[]{0.0f};
        float[] pitchOffset = new float[]{0.0f};
        float[] roll = new float[]{0.0f};
        TrackingBodyPart trackingBodyPart = TrackingBodyPart.HEAD;
        ImString target = new ImString(64);
        UUID validEntityTarget = null;
    }
}
