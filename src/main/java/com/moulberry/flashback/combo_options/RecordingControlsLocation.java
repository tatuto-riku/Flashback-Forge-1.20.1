package com.moulberry.flashback.combo_options;

import com.mojang.serialization.Codec;
import net.minecraft.util.OptionEnum;

public enum RecordingControlsLocation implements OptionEnum {
    RIGHT(0, "flashback.recording_controls_location.right"),
    BELOW(1, "flashback.recording_controls_location.below"),
    LEFT(2, "flashback.recording_controls_location.left"),
    HIDDEN(3, "flashback.recording_controls_location.hidden");

    public static final Codec<RecordingControlsLocation> CODEC = Codec.STRING.xmap(
        name -> RecordingControlsLocation.valueOf(name.toUpperCase()),
        value -> value.name().toLowerCase()
    );

    private final int id;
    private final String translationKey;

    RecordingControlsLocation(int id, String translationKey) {
        this.id = id;
        this.translationKey = translationKey;
    }

    @Override
    public int getId() {
        return id;
    }

    @Override
    public String getKey() {
        return translationKey;
    }
}
