package com.moulberry.flashback.packet;

import net.minecraft.network.FriendlyByteBuf;

/** Re-evaluates lighting at blocks changed by replay deltas after a snapshot replacement. */
public record FlashbackRecheckBlocks(long[] positions) implements FlashbackPacket {

    public static final int MAX_POSITIONS = 65_536;

    @Override
    public void write(FriendlyByteBuf buffer) {
        buffer.writeVarInt(this.positions.length);
        for (long position : this.positions) {
            buffer.writeLong(position);
        }
    }

    public static FlashbackRecheckBlocks decode(FriendlyByteBuf buffer) {
        int count = buffer.readVarInt();
        if (count < 0 || count > MAX_POSITIONS) {
            throw new IllegalArgumentException("Invalid replay light recheck count: " + count);
        }
        long[] positions = new long[count];
        for (int index = 0; index < count; index++) {
            positions[index] = buffer.readLong();
        }
        return new FlashbackRecheckBlocks(positions);
    }
}
