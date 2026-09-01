package com.moulberry.flashback.packet;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.item.ItemStack;

public record FlashbackRemoteSetSlot(int entityId, int slot, ItemStack itemStack) implements FlashbackPacket {
    public static FlashbackRemoteSetSlot decode(FriendlyByteBuf friendlyByteBuf) {
        int entityId = friendlyByteBuf.readVarInt();
        int slot = friendlyByteBuf.readByte();
        ItemStack itemStack = friendlyByteBuf.readItem();
        return new FlashbackRemoteSetSlot(entityId, slot, itemStack);
    }

    @Override
    public void write(FriendlyByteBuf friendlyByteBuf) {
        friendlyByteBuf.writeVarInt(this.entityId);
        friendlyByteBuf.writeByte(this.slot);
        friendlyByteBuf.writeItem(this.itemStack);
    }

}
