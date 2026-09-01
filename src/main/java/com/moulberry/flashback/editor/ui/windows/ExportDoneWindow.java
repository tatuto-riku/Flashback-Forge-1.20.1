package com.moulberry.flashback.editor.ui.windows;

import com.moulberry.flashback.editor.ui.ImGuiHelper;
import com.moulberry.flashback.exporting.ExportSettings;
import imgui.ImGui;
import imgui.flag.ImGuiCond;
import imgui.flag.ImGuiWindowFlags;
import imgui.type.ImBoolean;
import net.minecraft.Util;

import java.nio.file.Files;
import java.nio.file.Path;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.List;

public class ExportDoneWindow {

    private static final List<FinishedExportEntry> entries = new ArrayList<>();

    public static void addFinishedExportEntry(FinishedExportEntry entry) {
        entries.add(entry);
    }

    public static boolean isDone() {
        return !entries.isEmpty();
    }

    public static void render() {
        if (entries.isEmpty()) {
            return;
        }

        var viewport = ImGui.getMainViewport();
        ImGui.setNextWindowPos(viewport.getCenterX(), viewport.getCenterY(),
            ImGuiCond.Appearing, 0.5f, 0.5f);
        ImGui.setNextWindowSizeConstraints(250, 50, 5000, 500);
        ImBoolean modalOpen = new ImBoolean(true);
        ImGui.openPopup("###ExportDone");
        if (ImGui.beginPopupModal("Export Done###ExportDone", modalOpen,
            ImGuiWindowFlags.AlwaysAutoResize | ImGuiWindowFlags.NoSavedSettings | ImGuiWindowFlags.NoDocking)) {
            int index = 0;
            for (FinishedExportEntry entry : entries) {
                ImGui.pushID(index++);

                Path output = entry.settings.output();
                String name = entry.settings.name();
                if (name == null) {
                    name = output.getFileName().toString();
                }

                ImGuiHelper.separatorWithText(name);

                if (entry.fileSize > 0) {
                    String bytesString;
                    if (entry.fileSize < 1000L) {
                        bytesString = NumberFormat.getInstance().format(entry.fileSize) + " bytes";
                    } else if (entry.fileSize < 1000000L) {
                        bytesString = NumberFormat.getInstance().format(entry.fileSize / 1000L) + " KB";
                    } else {
                        bytesString = NumberFormat.getInstance().format(entry.fileSize / 1000L / 1000L) + " MB";
                    }
                    ImGui.text("Size: " + bytesString);
                }

                ImGui.text("Duration: " + formatDuration(entry.duration));
                ImGui.textWrapped("Output: " + output.toAbsolutePath().normalize());

                if (entry.outputIsFolder) {
                    if (ImGui.button("Open Folder")) {
                        openPath(output);
                    }
                } else {
                    if (ImGui.button("Open File")) {
                        openPath(output);
                    }
                    ImGui.sameLine();
                    if (ImGui.button("Open Folder")) {
                        openPath(output.getParent());
                    }
                }

                ImGui.popID();
            }

            ImGui.endPopup();
        }

        if (!modalOpen.get()) {
            entries.clear();
        }
    }

    private static String formatDuration(double seconds) {
        int total = (int) seconds;
        int minutes = total / 60;
        int secs = total % 60;
        return String.format("%d:%02d", minutes, secs);
    }

    private static void openPath(Path path) {
        try {
            Util.getPlatform().openFile(path.toFile());
        } catch (Exception ignored) {
        }
    }

    public static final class FinishedExportEntry {
        private final ExportSettings settings;
        private final double duration;
        private final long fileSize;
        private final boolean outputIsFolder;

        public FinishedExportEntry(ExportSettings settings, double duration, long fileSize) {
            this.settings = settings;
            this.duration = duration;
            this.fileSize = fileSize;
            this.outputIsFolder = Files.isDirectory(settings.output());
        }
    }
}
