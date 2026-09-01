package com.moulberry.flashback.editor.ui.windows;

import com.mojang.authlib.GameProfile;
import com.moulberry.flashback.editor.ui.ImGuiHelper;
import com.moulberry.flashback.editor.ui.WindowOpenState;
import com.moulberry.flashback.configuration.FlashbackConfig;
import com.moulberry.flashback.state.EditorState;
import com.moulberry.flashback.state.EditorStateManager;
import imgui.ImGui;
import imgui.flag.ImGuiHoveredFlags;
import imgui.flag.ImGuiCond;
import imgui.flag.ImGuiWindowFlags;
import imgui.ImGuiViewport;
import imgui.type.ImBoolean;
import imgui.type.ImString;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.multiplayer.PlayerInfo;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.GameType;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

public class PlayerListWindow {

    private static WindowOpenState wasOpen = WindowOpenState.UNKNOWN;
    private static String lastSearch = null;
    private static long lastUpdate = 0L;
    private static boolean includeNpcs = false;
    private static boolean hasNpcs = false;
    private static final ImString search = ImGuiHelper.createResizableImString("");
    private static final List<PlayerInfo> searchedPlayers = new ArrayList<>();

    private record SearchEntry(double distanceSq, PlayerInfo info) {}

    private static void updatePlayerList() {
        searchedPlayers.clear();
        hasNpcs = false;

        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.getConnection() == null) {
            return;
        }

        LocalPlayer localPlayer = minecraft.player;

        ClientLevel level = minecraft.level;
        Entity camera = minecraft.getCameraEntity();

        List<SearchEntry> searchEntries = new ArrayList<>();
        List<SearchEntry> searchEntriesLowPriority = new ArrayList<>();
        String searchLower = lastSearch.toLowerCase(Locale.ROOT);
        boolean blankSearch = lastSearch.isBlank();

        for (PlayerInfo playerInfo : minecraft.getConnection().getOnlinePlayers()) {
            GameProfile profile = playerInfo.getProfile();

            // Don't list the replay viewer itself
            if (localPlayer != null && profile.getId().equals(localPlayer.getUUID())) {
                continue;
            }

            Entity playerEntity = level == null ? null : level.getPlayerByUUID(profile.getId());
            // Offline-mode and development players commonly use v3 UUIDs. If
            // an actual player entity exists in the replay, it is not an NPC.
            if (profile.getId().version() != 4 && playerEntity == null) {
                hasNpcs = true;
                if (!includeNpcs) {
                    continue;
                }
            }

            double distanceSq = Double.MAX_VALUE;
            if (level != null && camera != null) {
                if (playerEntity != null) {
                    distanceSq = playerEntity.distanceToSqr(camera);
                }
            }

            SearchEntry searchEntry = new SearchEntry(distanceSq, playerInfo);
            if (blankSearch) {
                searchEntries.add(searchEntry);
            } else {
                String nameLower = profile.getName().toLowerCase(Locale.ROOT);
                if (nameLower.startsWith(searchLower)) {
                    searchEntries.add(searchEntry);
                } else if (nameLower.contains(searchLower)) {
                    searchEntriesLowPriority.add(searchEntry);
                }
            }
        }

        searchEntries.sort(Comparator.comparingDouble(SearchEntry::distanceSq));
        searchEntriesLowPriority.sort(Comparator.comparingDouble(SearchEntry::distanceSq));
        if (searchEntries.size() < 16) {
            searchEntries.addAll(searchEntriesLowPriority);
        }

        int count = Math.min(16, searchEntries.size());
        for (int i = 0; i < count; i++) {
            searchedPlayers.add(searchEntries.get(i).info());
        }
    }

    public static void render() {
        FlashbackConfig config = com.moulberry.flashback.Flashback.getConfig();
        if (!config.openedWindows.contains("player_list")) {
            wasOpen = WindowOpenState.CLOSED;
            return;
        }
        if (wasOpen == WindowOpenState.CLOSED) {
            ImGuiViewport viewport = ImGui.getMainViewport();
            ImGui.setNextWindowPos(viewport.getCenterX(), viewport.getCenterY(), ImGuiCond.Appearing, 0.5f, 0.5f);
        }
        wasOpen = WindowOpenState.OPEN;

        ImBoolean open = new ImBoolean(true);
        boolean visible = ImGui.begin("Player List###PlayerList", open,
            ImGuiWindowFlags.NoFocusOnAppearing | ImGuiWindowFlags.AlwaysAutoResize);
        if (!visible) {
            ImGui.end();
            closeIfRequested(config, open);
            return;
        }

        String searchStr = "Search";
        ImGui.setNextItemWidth(ImGui.getContentRegionAvailX() - ImGuiHelper.calcTextWidth(searchStr) - 32.0F);
        if (ImGui.inputText(searchStr, search)) {
            lastSearch = null;
        }

        long currentTime = System.currentTimeMillis();
        if (lastSearch == null || currentTime > lastUpdate + 3000L || currentTime < lastUpdate) {
            lastSearch = ImGuiHelper.getString(search);
            lastUpdate = currentTime;
            updatePlayerList();
        }

        if ((hasNpcs || includeNpcs) && ImGui.checkbox("Include NPCs", includeNpcs)) {
            includeNpcs = !includeNpcs;
            lastSearch = null;
        }

        EditorState editorState = EditorStateManager.getCurrent();
        ImGui.separator();

        for (int i = 0; i < searchedPlayers.size(); i++) {
            ImGui.pushID(i);
            PlayerInfo playerInfo = searchedPlayers.get(i);
            GameProfile profile = playerInfo.getProfile();

            ImGui.textUnformatted(profile.getName());

            ImGui.sameLine();
            if (ImGui.smallButton("TP")) {
                Minecraft.getInstance().player.connection.sendUnsignedCommand("teleport " + profile.getId());
                lastUpdate = currentTime;
            }

            if (editorState != null) {
                ImGui.sameLine();
                if (editorState.hideAllSpectators && playerInfo.getGameMode() == GameType.SPECTATOR) {
                    ImGui.beginDisabled();
                    ImGui.smallButton("Show");
                    ImGui.endDisabled();
                    ImGuiHelper.tooltip("You cannot show a player while Hide All Spectators is enabled",
                        ImGuiHoveredFlags.AllowWhenDisabled);
                } else if (editorState.hideDuringExport.contains(profile.getId())) {
                    if (ImGui.smallButton("Show")) {
                        editorState.hideDuringExport.remove(profile.getId());
                        editorState.markDirty();
                        lastUpdate = currentTime;
                    }
                } else if (ImGui.smallButton("Hide")) {
                    editorState.hideDuringExport.add(profile.getId());
                    editorState.markDirty();
                    lastUpdate = currentTime;
                }
            }

            ImGui.sameLine();
            if (ImGui.smallButton("Spectate")) {
                Minecraft.getInstance().player.connection.sendUnsignedCommand("spectate " + profile.getId());
                lastUpdate = currentTime;
            }

            ImGui.popID();
        }

        ImGui.separator();
        if (editorState != null) {
            if (ImGui.button("Hide All")) {
                changeVisibilityOfAll(editorState, false);
                lastUpdate = currentTime;
            }

            ImGui.sameLine();
            if (ImGui.button("Show All")) {
                changeVisibilityOfAll(editorState, true);
                lastUpdate = currentTime;
            }

            if (ImGui.checkbox("Hide All Spectators", editorState.hideAllSpectators)) {
                editorState.hideAllSpectators = !editorState.hideAllSpectators;
                editorState.markDirty();
            }
        }

        ImGui.end();
        closeIfRequested(config, open);
    }

    private static void closeIfRequested(FlashbackConfig config, ImBoolean open) {
        if (!open.get()) {
            config.openedWindows.remove("player_list");
            config.saveToDefaultFolder();
            wasOpen = WindowOpenState.CLOSED;
        }
    }

    private static void changeVisibilityOfAll(EditorState editorState, boolean visible) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.getConnection() == null) {
            return;
        }

        LocalPlayer localPlayer = minecraft.player;
        String searchLower = lastSearch.toLowerCase(Locale.ROOT);
        boolean blankSearch = lastSearch.isBlank();

        for (PlayerInfo playerInfo : minecraft.getConnection().getOnlinePlayers()) {
            GameProfile profile = playerInfo.getProfile();

            if (localPlayer != null && profile.getId().equals(localPlayer.getUUID())) {
                continue;
            }
            Entity playerEntity = minecraft.level == null ? null : minecraft.level.getPlayerByUUID(profile.getId());
            if (profile.getId().version() != 4 && playerEntity == null && !includeNpcs) {
                continue;
            }

            if (blankSearch) {
                setVisibility(editorState, profile.getId(), visible);
            } else {
                String nameLower = profile.getName().toLowerCase(Locale.ROOT);
                if (nameLower.contains(searchLower)) {
                    setVisibility(editorState, profile.getId(), visible);
                }
            }
        }
        editorState.markDirty();
    }

    private static void setVisibility(EditorState editorState, java.util.UUID uuid, boolean visible) {
        if (visible) {
            editorState.hideDuringExport.remove(uuid);
        } else {
            editorState.hideDuringExport.add(uuid);
        }
    }

}
