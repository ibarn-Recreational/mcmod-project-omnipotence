package com.ibarnstormer.projectomnipotence.network.payload;

import com.ibarnstormer.projectomnipotence.Main;
import com.mojang.authlib.GameProfile;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

public record SyncSSDHDataPayload(GameProfile profile, boolean isOmnipotent, int entitiesEnlightened) implements CustomPacketPayload {
    public static final CustomPacketPayload.Type<SyncSSDHDataPayload> ID = new CustomPacketPayload.Type<>(Identifier.fromNamespaceAndPath(Main.MODID, "sync_serverside_data"));
    public static final StreamCodec<FriendlyByteBuf, SyncSSDHDataPayload> CODEC = StreamCodec.composite(
            ByteBufCodecs.GAME_PROFILE, SyncSSDHDataPayload::profile,
            ByteBufCodecs.BOOL, SyncSSDHDataPayload::isOmnipotent,
            ByteBufCodecs.INT, SyncSSDHDataPayload::entitiesEnlightened,
            SyncSSDHDataPayload::new
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return ID;
    }
}
