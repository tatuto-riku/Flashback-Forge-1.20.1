package com.moulberry.flashback.editor;

import com.moulberry.flashback.editor.ui.KeyframeRelativeOffsets;
import com.moulberry.flashback.keyframe.Keyframe;
import com.moulberry.flashback.keyframe.KeyframeType;
import com.moulberry.flashback.state.EditorState;
import com.moulberry.flashback.state.EditorStateHistoryAction;
import com.moulberry.flashback.state.EditorStateHistoryEntry;
import com.moulberry.flashback.state.KeyframeTrack;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

public record SavedTrack(KeyframeType<?> type, int track, boolean copiedFromDisabled,
                         TreeMap<Integer, Keyframe> keyframes) {
    public int applyToEditorState(EditorState editorState, int cursorTicks, int totalTicks) {
        return this.applyToEditorState(editorState, cursorTicks, totalTicks, new KeyframeRelativeOffsets());
    }

    public int applyToEditorState(EditorState editorState, int cursorTicks, int totalTicks,
                                  KeyframeRelativeOffsets offsets) {
        if (this.keyframes == null || this.keyframes.isEmpty()) {
            return 0;
        }

        // Try to apply directly to copied track
        if (this.track >= 0 && this.track < editorState.keyframeTracks.size()) {
            KeyframeTrack keyframeTrack = editorState.keyframeTracks.get(this.track);
            if ((keyframeTrack.enabled || this.copiedFromDisabled) && keyframeTrack.keyframeType == this.type) {
                return this.applyTo(editorState, keyframeTrack, this.track, cursorTicks, totalTicks, offsets);
            }
        }

        // Try to find first eligible enabled track
        for (int trackIndex = 0; trackIndex < editorState.keyframeTracks.size(); trackIndex++) {
            KeyframeTrack keyframeTrack = editorState.keyframeTracks.get(trackIndex);
            if (keyframeTrack.enabled && keyframeTrack.keyframeType == this.type) {
                return this.applyTo(editorState, keyframeTrack, trackIndex, cursorTicks, totalTicks, offsets);
            }
        }

        // Create new track
        return this.applyTo(editorState, null, editorState.keyframeTracks.size(), cursorTicks, totalTicks, offsets);
    }

    private int applyTo(EditorState editorState, @Nullable KeyframeTrack existing, int trackIndex,
                        int cursorTicks, int totalTicks, KeyframeRelativeOffsets offsets) {
        List<EditorStateHistoryAction> undo = new ArrayList<>();
        List<EditorStateHistoryAction> redo = new ArrayList<>();

        if (existing == null) {
            undo.add(new EditorStateHistoryAction.RemoveTrack(this.type, trackIndex));
            redo.add(new EditorStateHistoryAction.AddTrack(this.type, trackIndex));
        }

        int count = 0;
        for (Map.Entry<Integer, Keyframe> entry : this.keyframes.entrySet()) {
            int newTick = entry.getKey() + cursorTicks;
            if (newTick >= 0 && newTick <= totalTicks) {
                if (existing != null) {
                    Keyframe old = existing.keyframesByTick.get(newTick);

                    if (old != null) {
                        undo.add(new EditorStateHistoryAction.SetKeyframe(this.type, trackIndex, newTick, old.copy()));
                    } else {
                        undo.add(new EditorStateHistoryAction.RemoveKeyframe(this.type, trackIndex, newTick));
                    }
                }

                Keyframe newKeyframe = entry.getValue().copy();
                offsets.apply(newKeyframe);
                redo.add(new EditorStateHistoryAction.SetKeyframe(this.type, trackIndex, newTick, newKeyframe));

                count += 1;
            }
        }

        editorState.push(new EditorStateHistoryEntry(undo, redo, "Pasted " + count + " keyframe(s)"));

        return count;
    }
}
