package com.moulberry.flashback.mixin.visuals;

import com.llamalad7.mixinextras.injector.v2.WrapWithCondition;
import com.moulberry.flashback.state.EditorState;
import com.moulberry.flashback.state.EditorStateManager;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.layers.ItemInHandLayer;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import java.util.EnumSet;

@Mixin(ItemInHandLayer.class)
public class MixinItemInHandLayerRenderer {

    @WrapWithCondition(method = "render(Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;ILnet/minecraft/world/entity/LivingEntity;FFFFFF)V",
        at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/entity/layers/ItemInHandLayer;renderArmWithItem(Lnet/minecraft/world/entity/LivingEntity;Lnet/minecraft/world/item/ItemStack;Lnet/minecraft/world/item/ItemDisplayContext;Lnet/minecraft/world/entity/HumanoidArm;Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;I)V"))
    public boolean renderArmWithItem(ItemInHandLayer<?, ?> layer, LivingEntity livingEntity, ItemStack itemStack, ItemDisplayContext itemDisplayContext,
            HumanoidArm humanoidArm, PoseStack poseStack, MultiBufferSource multiBufferSource, int i) {
        if (!(livingEntity instanceof Player player)) {
            return true;
        }

        EditorState editorState = EditorStateManager.getCurrent();
        if (editorState == null) {
            return true;
        }

        EnumSet<EquipmentSlot> hidden = editorState.hiddenEquipment.get(player.getUUID());
        if (hidden == null) {
            return true;
        }

        boolean rightIsMain = player.getMainArm() == HumanoidArm.RIGHT;
        EquipmentSlot slot;
        if (humanoidArm == HumanoidArm.RIGHT) {
            slot = rightIsMain ? EquipmentSlot.MAINHAND : EquipmentSlot.OFFHAND;
        } else {
            slot = rightIsMain ? EquipmentSlot.OFFHAND : EquipmentSlot.MAINHAND;
        }

        return !hidden.contains(slot);
    }

}
