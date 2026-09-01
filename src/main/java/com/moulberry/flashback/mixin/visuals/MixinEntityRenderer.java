package com.moulberry.flashback.mixin.visuals;

import com.llamalad7.mixinextras.sugar.Local;
import com.mojang.blaze3d.vertex.PoseStack;
import com.moulberry.flashback.state.EditorState;
import com.moulberry.flashback.state.EditorStateManager;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.scores.PlayerTeam;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(EntityRenderer.class)
public class MixinEntityRenderer {

    @Inject(method = "renderNameTag", at = @At("HEAD"), cancellable = true, require = 0)
    public void renderNameTag(Entity entity, Component component, PoseStack poseStack, MultiBufferSource multiBufferSource, int i, CallbackInfo ci) {
        EditorState editorState = EditorStateManager.getCurrent();
        if (editorState != null) {
            if (!editorState.replayVisuals.renderNametags) {
                ci.cancel();
            } else if (editorState.hideNametags.contains(entity.getUUID())) {
                ci.cancel();
            } else if (editorState.isEntityHidden(entity)) {
                ci.cancel();
            }
        }
    }

    /*
     * Entity#getDisplayName builds the nametag as "teamPrefix + name + teamSuffix".
     * Rebuild it here so the prefix and suffix can be dropped individually.
     */
    @ModifyVariable(method = "renderNameTag", at = @At("HEAD"), argsOnly = true, index = 2)
    public Component renderNameTag_stripTeamAffixes(Component component, @Local(argsOnly = true) Entity entity) {
        EditorState editorState = EditorStateManager.getCurrent();
        if (editorState == null) {
            return component;
        }

        boolean hidePrefix = editorState.hideTeamPrefix.contains(entity.getUUID());
        boolean hideSuffix = editorState.hideTeamSuffix.contains(entity.getUUID());
        if (!hidePrefix && !hideSuffix) {
            return component;
        }

        PlayerTeam team = entity.getTeam() instanceof PlayerTeam playerTeam ? playerTeam : null;

        MutableComponent mutable = Component.empty();
        if (team != null && !hidePrefix) {
            mutable.append(team.getPlayerPrefix());
        }
        mutable.append(entity.getName());
        if (team != null && !hideSuffix) {
            mutable.append(team.getPlayerSuffix());
        }
        return mutable;
    }

}
