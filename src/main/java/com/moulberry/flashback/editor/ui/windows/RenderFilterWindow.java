package com.moulberry.flashback.editor.ui.windows;

import com.moulberry.flashback.Flashback;
import com.moulberry.flashback.configuration.FlashbackConfig;
import com.moulberry.flashback.editor.ui.ImGuiHelper;
import com.moulberry.flashback.editor.ui.WindowOpenState;
import com.moulberry.flashback.state.EditorState;
import com.moulberry.flashback.state.EditorStateManager;
import imgui.ImGui;
import imgui.ImGuiListClipper;
import imgui.ImGuiViewport;
import imgui.callback.ImListClipperCallback;
import imgui.flag.ImGuiCond;
import imgui.flag.ImGuiWindowFlags;
import imgui.type.ImString;
import imgui.type.ImBoolean;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.core.particles.ParticleType;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EntityType;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class RenderFilterWindow {

    private static WindowOpenState wasOpen = WindowOpenState.UNKNOWN;

    private static final ImString entitySearch = ImGuiHelper.createResizableImString("");
    private static String lastEntitySearch = null;
    private static List<EntityType<?>> searchedEntityTypes = new ArrayList<>();

    private static final ImString particleSearch = ImGuiHelper.createResizableImString("");
    private static String lastParticleSearch = null;
    private static List<ResourceLocation> searchedParticleTypes = new ArrayList<>();

    public static void render() {
        if (!Flashback.getConfig().openedWindows.contains("render_filter")) {
            wasOpen = WindowOpenState.CLOSED;
            return;
        }

        ImGuiViewport viewport = ImGui.getMainViewport();
        if (wasOpen == WindowOpenState.CLOSED) {
            ImGui.setNextWindowPos(viewport.getCenterX(), viewport.getCenterY(), ImGuiCond.Appearing, 0.5f, 0.5f);
        }
        wasOpen = WindowOpenState.OPEN;

        ImGui.setNextWindowSizeConstraints(250, 50, 5000, viewport.getSizeY() / 2.0f);
        FlashbackConfig config = Flashback.getConfig();
        ImBoolean open = new ImBoolean(true);
        if (ImGui.begin("Render Filter###RenderFilter", open,
            ImGuiWindowFlags.AlwaysAutoResize | ImGuiWindowFlags.NoFocusOnAppearing)) {
            if (!config.signedRenderFilter) {
                String name = Minecraft.getInstance().getUser().getName();
                ImGui.pushTextWrapPos(300);
                ImGui.textWrapped("I, " + name + ", do solemnly swear (or affirm) that I will not toggle something off in the 'Render Filter' menu and then later ask in Flashback support why the entity is not being rendered.");
                if (ImGui.checkbox("Signed, " + name + ".", false)) {
                    config.signedRenderFilter = true;
                    config.saveToDefaultFolder();
                }
                ImGui.popTextWrapPos();
                ImGui.end();
                closeIfRequested(config, open);
                return;
            }

            EditorState editorState = EditorStateManager.getCurrent();
            if (editorState == null) {
                ImGui.end();
                closeIfRequested(config, open);
                return;
            }

            if (ImGui.beginTabBar("##Select")) {
                if (ImGui.beginTabItem("Entities")) {
                    renderEntitiesTab(editorState);
                    ImGui.endTabItem();
                }
                if (ImGui.beginTabItem("Particles")) {
                    renderParticlesTab(editorState);
                    ImGui.endTabItem();
                }
                ImGui.endTabBar();
            }
        }
        ImGui.end();
        closeIfRequested(config, open);
    }

    private static void closeIfRequested(FlashbackConfig config, ImBoolean open) {
        if (!open.get()) {
            config.openedWindows.remove("render_filter");
            config.saveToDefaultFolder();
            wasOpen = WindowOpenState.CLOSED;
        }
    }

    private static void renderEntitiesTab(EditorState editorState) {
        ImGui.inputText("Search", entitySearch);
        String searchString = ImGuiHelper.getString(entitySearch).trim().toLowerCase(Locale.ROOT);
        if (!searchString.equals(lastEntitySearch)) {
            lastEntitySearch = searchString;
            List<EntityType<?>> starts = new ArrayList<>();
            List<EntityType<?>> contains = new ArrayList<>();

            for (EntityType<?> entityType : BuiltInRegistries.ENTITY_TYPE) {
                String name = I18n.get(entityType.getDescriptionId()).trim().toLowerCase(Locale.ROOT);
                if (name.startsWith(lastEntitySearch)) {
                    starts.add(entityType);
                } else if (name.contains(lastEntitySearch)) {
                    contains.add(entityType);
                }
            }

            starts.addAll(contains);
            searchedEntityTypes = starts;
        }

        if (searchedEntityTypes.isEmpty()) {
            ImGui.textUnformatted("No entities found");
            return;
        }

        if (ImGui.smallButton("Enable All")) {
            editorState.filteredEntities.clear();
        }
        ImGui.sameLine();
        if (ImGui.smallButton("Disable All")) {
            for (EntityType<?> entityType : BuiltInRegistries.ENTITY_TYPE) {
                editorState.filteredEntities.add(BuiltInRegistries.ENTITY_TYPE.getKey(entityType).toString());
            }
        }

        if (ImGui.beginChild("##Scroller", 0, 300)) {
            ImGuiListClipper.forEach(searchedEntityTypes.size(), new ImListClipperCallback() {
                @Override
                public void accept(int i) {
                    EntityType<?> entityType = searchedEntityTypes.get(i);
                    ResourceLocation resourceLocation = BuiltInRegistries.ENTITY_TYPE.getKey(entityType);
                    boolean filtered = editorState.filteredEntities.contains(resourceLocation.toString());
                    String name = I18n.get(entityType.getDescriptionId());
                    if (ImGui.checkbox(name + "###" + resourceLocation, !filtered)) {
                        if (filtered) {
                            editorState.filteredEntities.remove(resourceLocation.toString());
                        } else {
                            editorState.filteredEntities.add(resourceLocation.toString());
                        }
                    }
                }
            });
        }
        ImGui.endChild();
    }

    private static void renderParticlesTab(EditorState editorState) {
        ImGui.inputText("Search", particleSearch);
        String searchString = ImGuiHelper.getString(particleSearch).trim().toLowerCase(Locale.ROOT);
        if (!searchString.equals(lastParticleSearch)) {
            lastParticleSearch = searchString;
            List<ResourceLocation> starts = new ArrayList<>();
            List<ResourceLocation> contains = new ArrayList<>();

            for (ParticleType<?> particleType : BuiltInRegistries.PARTICLE_TYPE) {
                ResourceLocation resourceLocation = BuiltInRegistries.PARTICLE_TYPE.getKey(particleType);
                if (resourceLocation != null) {
                    String name = resourceLocation.getNamespace().equals("minecraft")
                        ? resourceLocation.getPath().toLowerCase(Locale.ROOT)
                        : resourceLocation.toString().toLowerCase(Locale.ROOT);
                    if (name.startsWith(lastParticleSearch)) {
                        starts.add(resourceLocation);
                    } else if (name.contains(lastParticleSearch)) {
                        contains.add(resourceLocation);
                    }
                }
            }

            starts.addAll(contains);
            searchedParticleTypes = starts;
        }

        if (searchedParticleTypes.isEmpty()) {
            ImGui.textUnformatted("No particles found");
            return;
        }

        if (ImGui.smallButton("Enable All")) {
            editorState.filteredParticles.clear();
        }
        ImGui.sameLine();
        if (ImGui.smallButton("Disable All")) {
            for (ParticleType<?> particleType : BuiltInRegistries.PARTICLE_TYPE) {
                ResourceLocation resourceLocation = BuiltInRegistries.PARTICLE_TYPE.getKey(particleType);
                if (resourceLocation != null) {
                    editorState.filteredParticles.add(resourceLocation.toString());
                }
            }
        }

        if (ImGui.beginChild("##Scroller", 0, 300)) {
            ImGuiListClipper.forEach(searchedParticleTypes.size(), new ImListClipperCallback() {
                @Override
                public void accept(int i) {
                    ResourceLocation particleType = searchedParticleTypes.get(i);
                    boolean filtered = editorState.filteredParticles.contains(particleType.toString());
                    String name = particleType.getNamespace().equals("minecraft")
                        ? particleType.getPath().toLowerCase(Locale.ROOT)
                        : particleType.toString().toLowerCase(Locale.ROOT);
                    if (ImGui.checkbox(name, !filtered)) {
                        if (filtered) {
                            editorState.filteredParticles.remove(particleType.toString());
                        } else {
                            editorState.filteredParticles.add(particleType.toString());
                        }
                    }
                }
            });
        }
        ImGui.endChild();

        ImGui.pushTextWrapPos(300);
        ImGui.textWrapped("Note: Due to technical limitations, will only prevent new particles from spawning. Existing particles won't be affected.");
        ImGui.popTextWrapPos();
    }
}
