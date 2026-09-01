package com.moulberry.flashback.editor;

import org.joml.Vector3d;

import java.util.ArrayList;
import java.util.List;

/**
 * Clipboard payload used by newer Flashback versions. The optional origin
 * values allow camera keyframes to be pasted relative to the current player.
 */
public class CopiedKeyframes {

    public Vector3d relativePosition;
    public Float relativeYaw;
    public Float relativePitch;
    public List<SavedTrack> savedTracks = new ArrayList<>();

}
