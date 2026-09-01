package com.moulberry.flashback.keyframe.impl;

import com.google.gson.*;
import com.moulberry.flashback.editor.ui.ImGuiHelper;
import com.moulberry.flashback.keyframe.Keyframe;
import com.moulberry.flashback.keyframe.KeyframeType;
import com.moulberry.flashback.keyframe.handler.KeyframeHandler;
import com.moulberry.flashback.keyframe.interpolation.InterpolationType;
import com.moulberry.flashback.keyframe.types.FreezeKeyframeType;
import imgui.ImGui;

import java.lang.reflect.Type;
import java.util.function.Consumer;

public class FreezeKeyframe extends Keyframe {

    private boolean frozen;
    private int frozenDelay;

    public FreezeKeyframe(boolean frozen, int frozenDelay) {
        this(frozen, frozenDelay, InterpolationType.HOLD);
    }

    public FreezeKeyframe(boolean frozen, int frozenDelay, InterpolationType interpolationType) {
        this.frozen = frozen;
        this.frozenDelay = frozenDelay;
        this.interpolationType(interpolationType);
    }

    @Override
    public KeyframeType<?> keyframeType() {
        return FreezeKeyframeType.INSTANCE;
    }

    @Override
    public Keyframe copy() {
        return new FreezeKeyframe(this.frozen, this.frozenDelay, this.interpolationType());
    }

    @Override
    public void renderEditKeyframe(Consumer<Consumer<Keyframe>> update) {
        ImGui.setNextItemWidth(160);
        if (ImGui.checkbox("Frozen", this.frozen)) {
            boolean newFrozen = !this.frozen;
            update.accept(keyframe -> ((FreezeKeyframe) keyframe).frozen = newFrozen);
        }

        int[] delay = new int[]{this.frozenDelay};
        ImGui.setNextItemWidth(160);
        if (ImGui.sliderInt("Delay", delay, 0, 10)) {
            int clamped = Math.max(0, Math.min(10, delay[0]));
            update.accept(keyframe -> ((FreezeKeyframe) keyframe).frozenDelay = clamped);
        }
        ImGuiHelper.tooltip("Number of ticks to wait before freezing, to let motion settle first");
    }

    @Override
    public void apply(KeyframeHandler keyframeHandler) {
        keyframeHandler.applyFreeze(this.frozen, this.frozenDelay);
    }

    @Override
    public void applyInterpolated(KeyframeHandler keyframeHandler, Keyframe other, float amount) {
        keyframeHandler.applyFreeze(this.frozen, this.frozenDelay);
    }

    @Override
    public void applyInterpolatedSmooth(KeyframeHandler keyframeHandler, Keyframe p1, Keyframe p2, Keyframe p3, float t0, float t1, float t2, float t3, float amount,
                                         float lerpAmount, boolean lerpFromRight) {
        keyframeHandler.applyFreeze(this.frozen, this.frozenDelay);
    }

    public static class TypeAdapter implements JsonSerializer<FreezeKeyframe>, JsonDeserializer<FreezeKeyframe> {
        @Override
        public FreezeKeyframe deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException {
            JsonObject jsonObject = json.getAsJsonObject();
            boolean frozen = jsonObject.get("freeze").getAsBoolean();
            int frozenDelay = jsonObject.has("freezeDelay") ? jsonObject.get("freezeDelay").getAsInt() : 0;
            InterpolationType interpolationType = context.deserialize(jsonObject.get("interpolation_type"), InterpolationType.class);
            return new FreezeKeyframe(frozen, frozenDelay, interpolationType);
        }

        @Override
        public JsonElement serialize(FreezeKeyframe src, Type typeOfSrc, JsonSerializationContext context) {
            JsonObject jsonObject = new JsonObject();
            jsonObject.addProperty("freeze", src.frozen);
            jsonObject.addProperty("freezeDelay", src.frozenDelay);
            jsonObject.addProperty("type", "freeze");
            jsonObject.add("interpolation_type", context.serialize(src.interpolationType()));
            return jsonObject;
        }
    }
}
