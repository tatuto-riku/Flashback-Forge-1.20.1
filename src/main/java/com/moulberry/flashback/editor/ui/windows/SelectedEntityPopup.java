package com.moulberry.flashback.editor.ui.windows;

import com.mojang.authlib.GameProfile;
import com.moulberry.flashback.FilePlayerSkin;
import com.moulberry.flashback.Utils;
import com.moulberry.flashback.combo_options.GlowingOverride;
import com.moulberry.flashback.editor.ui.ImGuiHelper;
import com.moulberry.flashback.editor.ui.ReplayUI;
import com.moulberry.flashback.exporting.AsyncFileDialogs;
import com.moulberry.flashback.state.EditorState;
import imgui.ImGui;
import imgui.flag.ImGuiHoveredFlags;
import imgui.type.ImString;
import net.minecraft.client.Minecraft;
import net.minecraft.commands.arguments.EntityAnchorArgument;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.player.PlayerModelPart;
import net.minecraft.world.scores.Objective;
import net.minecraft.world.scores.PlayerTeam;
import net.minecraft.world.scores.Scoreboard;

import java.nio.file.Path;
import java.util.EnumSet;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public class SelectedEntityPopup {

    private static ImString changeSkinInput = ImGuiHelper.createResizableImString("");
    static {
        changeSkinInput.inputData.allowedChars = "0123456789abcdef-";
    }
    private static ImString changeNameInput = ImGuiHelper.createResizableImString("");

    public static void open(Entity entity, EditorState editorState) {
        String nameOverride = editorState.nameOverride.get(entity.getUUID());
        if (nameOverride != null) {
            changeNameInput.set(nameOverride);
        } else {
            changeNameInput.set("");
        }

        GameProfile skinOverride = editorState.skinOverride.get(entity.getUUID());
        if (skinOverride != null) {
            changeSkinInput.set(skinOverride.getId().toString());
        } else {
            changeSkinInput.set("");
        }
    }

    public static void render(Entity entity, EditorState editorState) {
        UUID uuid = entity.getUUID();
        ImGui.textUnformatted("Entity: " + uuid);

        ImGui.separator();

        if (ImGui.button("Look At")) {
            Minecraft.getInstance().cameraEntity.lookAt(EntityAnchorArgument.Anchor.EYES, entity.getEyePosition());
        }

        ImGui.sameLine();
        if (ImGui.button("Spectate")) {
            Minecraft.getInstance().player.connection.sendUnsignedCommand("spectate " + entity.getUUID());
            ImGui.closeCurrentPopup();
        }

        ImGui.sameLine();
        if (ImGui.button("Copy UUID")) {
            Minecraft.getInstance().keyboardHandler.setClipboard(entity.getUUID().toString());
            ReplayUI.setInfoOverlay("Copied '" + entity.getUUID() + "'");
            ImGui.closeCurrentPopup();
        }

        if (uuid.equals(editorState.audioSourceEntity)) {
            if (ImGui.button("Unset Audio Source")) {
                editorState.audioSourceEntity = null;
                editorState.markDirty();
            }
        } else if (ImGui.button("Set Audio Source")) {
            editorState.audioSourceEntity = entity.getUUID();
            editorState.markDirty();
        }

        boolean isHiddenDuringExport;
        if (editorState.hideAllSpectators && entity instanceof Player player && player.isSpectator()) {
            isHiddenDuringExport = true;
            ImGui.beginDisabled();
            ImGui.checkbox("Hide During Export", true);
            ImGui.endDisabled();
            ImGuiHelper.tooltip("You cannot show an entity while Hide All Spectators is enabled",
                ImGuiHoveredFlags.AllowWhenDisabled);
        } else {
            isHiddenDuringExport = editorState.hideDuringExport.contains(entity.getUUID());
            if (ImGui.checkbox("Hide During Export", isHiddenDuringExport)) {
                if (isHiddenDuringExport) {
                    editorState.hideDuringExport.remove(entity.getUUID());
                } else {
                    editorState.hideDuringExport.add(entity.getUUID());
                }
                editorState.markDirty();
            }
        }

        if (!isHiddenDuringExport) {
            if (entity instanceof Player player) {
                if (editorState.hideCape.contains(player.getUUID())) {
                    if (ImGui.checkbox("Hide Cape", true)) {
                        editorState.hideCape.remove(player.getUUID());
                    }
                } else if (player.isModelPartShown(PlayerModelPart.CAPE) && ImGui.checkbox("Hide Cape", false)) {
                    editorState.hideCape.add(player.getUUID());
                }

                boolean hideNametag = editorState.hideNametags.contains(entity.getUUID());
                if (ImGui.checkbox("Render Nametag", !hideNametag)) {
                    if (hideNametag) {
                        editorState.hideNametags.remove(entity.getUUID());
                    } else {
                        editorState.hideNametags.add(entity.getUUID());
                    }
                    editorState.markDirty();
                }

                if (!hideNametag) {
                    boolean changedName = ImGui.inputTextWithHint("Name##SetNameInput", player.getScoreboardName(), changeNameInput);
                    if (changedName) {
                        String string = ImGuiHelper.getString(changeNameInput);
                        if (string.isEmpty()) {
                            editorState.nameOverride.remove(entity.getUUID());
                        } else {
                            editorState.nameOverride.put(entity.getUUID(), string);
                        }
                        editorState.markDirty();
                    }

                    if (editorState.hideTeamPrefix.contains(player.getUUID())) {
                        if (ImGui.checkbox("Hide Team Prefix", true)) {
                            editorState.hideTeamPrefix.remove(player.getUUID());
                        }
                    } else {
                        PlayerTeam team = getPlayerTeam(player);
                        if (team != null && !Utils.isComponentEmpty(team.getPlayerPrefix())
                                && ImGui.checkbox("Hide Team Prefix", false)) {
                            editorState.hideTeamPrefix.add(player.getUUID());
                        }
                    }

                    if (editorState.hideTeamSuffix.contains(player.getUUID())) {
                        if (ImGui.checkbox("Hide Team Suffix", true)) {
                            editorState.hideTeamSuffix.remove(player.getUUID());
                        }
                    } else {
                        PlayerTeam team = getPlayerTeam(player);
                        if (team != null && !Utils.isComponentEmpty(team.getPlayerSuffix())
                                && ImGui.checkbox("Hide Team Suffix", false)) {
                            editorState.hideTeamSuffix.add(player.getUUID());
                        }
                    }

                    if (editorState.hideBelowName.contains(player.getUUID())) {
                        if (ImGui.checkbox("Hide Text Below Name", true)) {
                            editorState.hideBelowName.remove(player.getUUID());
                        }
                    } else {
                        Scoreboard scoreboard = player.getScoreboard();
                        Objective objective = scoreboard.getDisplayObjective(2);
                        if (objective != null && ImGui.checkbox("Hide Text Below Name", false)) {
                            editorState.hideBelowName.add(player.getUUID());
                        }
                    }
                }

                showGlowingDropdown(entity, editorState);

                ImGuiHelper.separatorWithText("Change Skin & Cape");
                ImGui.setNextItemWidth(320);
                ImGui.inputTextWithHint("##SetSkinInput", "e.g. d0e05de7-6067-454d-beae-c6d19d886191", changeSkinInput);

                boolean hasApplySkin = false;
                if (!changeSkinInput.isEmpty()) {
                    String string = ImGuiHelper.getString(changeSkinInput);
                    try {
                        UUID changeSkinUuid = UUID.fromString(string);
                        if (ImGui.button("Apply Skin From UUID")) {
                            GameProfile profile = new GameProfile(changeSkinUuid, null);
                            Minecraft.getInstance().getMinecraftSessionService().fillProfileProperties(profile, true);
                            editorState.skinOverride.put(entity.getUUID(), profile);
                            editorState.skinOverrideFromFile.remove(entity.getUUID());
                            editorState.markDirty();
                        }
                        hasApplySkin = true;
                    } catch (Exception ignored) {}
                }

                if (ImGui.button("Upload Skin From File")) {
                    Path gameDir = Minecraft.getInstance().gameDirectory.toPath();
                    CompletableFuture<String> future = AsyncFileDialogs.openFileDialog(gameDir.toString(), "Skin Texture", "png");
                    future.thenAccept(pathStr -> {
                        if (pathStr != null) {
                            editorState.skinOverride.remove(entity.getUUID());
                            editorState.skinOverrideFromFile.put(entity.getUUID(), new FilePlayerSkin(pathStr));
                            editorState.markDirty();
                        }
                    });
                    if (hasApplySkin) {
                        ImGui.sameLine();
                    }
                }

                if (editorState.skinOverride.containsKey(entity.getUUID())
                        || editorState.skinOverrideFromFile.containsKey(entity.getUUID())) {
                    if (ImGui.button("Reset Skin")) {
                        editorState.skinOverride.remove(entity.getUUID());
                        editorState.skinOverrideFromFile.remove(entity.getUUID());
                        editorState.markDirty();
                        changeSkinInput.set("");
                    }
                }
            } else {
                showGlowingDropdown(entity, editorState);
            }

            if (entity instanceof LivingEntity && ImGui.collapsingHeader("Equipment")) {
                EnumSet<EquipmentSlot> hiddenEquipment = editorState.hiddenEquipment.get(entity.getUUID());
                if (hiddenEquipment == null) {
                    hiddenEquipment = EnumSet.noneOf(EquipmentSlot.class);
                } else {
                    hiddenEquipment = hiddenEquipment.clone();
                }

                boolean changed = false;

                for (EquipmentSlot slot : EquipmentSlot.values()) {
                    boolean hidden = hiddenEquipment.contains(slot);
                    if (ImGui.checkbox(slot.getName(), !hidden)) {
                        if (hidden) {
                            hiddenEquipment.remove(slot);
                        } else {
                            hiddenEquipment.add(slot);
                        }
                        changed = true;
                    }
                }

                if (changed) {
                    if (hiddenEquipment.isEmpty()) {
                        editorState.hiddenEquipment.remove(entity.getUUID());
                    } else {
                        editorState.hiddenEquipment.put(entity.getUUID(), hiddenEquipment);
                    }
                    editorState.markDirty();
                }
            }

            if (entity instanceof Player avatar && ImGui.collapsingHeader("Model Parts")) {
                EnumSet<PlayerModelPart> hiddenModelParts = editorState.hiddenModelParts.get(entity.getUUID());
                boolean changed = false;
                EnumSet<PlayerModelPart> actualHiddenModelParts = EnumSet.noneOf(PlayerModelPart.class);

                for (PlayerModelPart part : PlayerModelPart.values()) {
                    if (!avatar.isModelPartShown(part)) {
                        actualHiddenModelParts.add(part);
                    }
                }

                boolean overriding = hiddenModelParts != null;
                if (ImGui.checkbox("Override", overriding)) {
                    if (overriding) {
                        hiddenModelParts = null;
                    } else {
                        hiddenModelParts = actualHiddenModelParts;
                    }
                    changed = true;
                }

                if (hiddenModelParts != null) {
                    for (PlayerModelPart part : PlayerModelPart.values()) {
                        boolean hidden = hiddenModelParts.contains(part);
                        if (ImGui.checkbox(part.getName().getString(), !hidden)) {
                            if (hidden) {
                                hiddenModelParts.remove(part);
                            } else {
                                hiddenModelParts.add(part);
                            }
                            changed = true;
                        }
                    }
                } else {
                    ImGui.beginDisabled();
                    for (PlayerModelPart part : PlayerModelPart.values()) {
                        ImGui.checkbox(part.getName().getString(), !actualHiddenModelParts.contains(part));
                    }
                    ImGui.endDisabled();
                }

                if (changed) {
                    if (hiddenModelParts == null) {
                        editorState.hiddenModelParts.remove(entity.getUUID());
                    } else {
                        editorState.hiddenModelParts.put(entity.getUUID(), hiddenModelParts);
                    }
                    editorState.markDirty();
                }
            }
        }
    }

    private static PlayerTeam getPlayerTeam(Entity entity) {
        return entity.getTeam() instanceof PlayerTeam playerTeam ? playerTeam : null;
    }

    private static void showGlowingDropdown(Entity entity, EditorState editorState) {
        GlowingOverride glowingOverride = GlowingOverride.DEFAULT;
        if (editorState.glowingOverride.containsKey(entity.getUUID())) {
            glowingOverride = editorState.glowingOverride.get(entity.getUUID());
        }

        GlowingOverride newGlowingOverride = ImGuiHelper.enumCombo("Glowing", glowingOverride);
        if (newGlowingOverride != glowingOverride) {
            if (newGlowingOverride == GlowingOverride.DEFAULT) {
                editorState.glowingOverride.remove(entity.getUUID());
            } else {
                editorState.glowingOverride.put(entity.getUUID(), newGlowingOverride);
            }
            editorState.markDirty();
        }
    }

}
