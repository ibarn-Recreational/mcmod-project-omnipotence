package com.ibarnstormer.projectomnipotence.network.payload;

import com.ibarnstormer.projectomnipotence.Main;
import com.mojang.authlib.GameProfile;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;

public record SyncSSDHDataPayload(GameProfile profile, boolean isOmnipotent, int entitiesEnlightened) implements CustomPayload {
    public static final CustomPayload.Id<SyncSSDHDataPayload> ID = new CustomPayload.Id<>(Identifier.of(Main.MODID, "sync_serverside_data"));
    public static final PacketCodec<PacketByteBuf, SyncSSDHDataPayload> CODEC = PacketCodec.tuple(
            PacketCodecs.GAME_PROFILE, SyncSSDHDataPayload::profile,
            PacketCodecs.BOOL, SyncSSDHDataPayload::isOmnipotent,
            PacketCodecs.INTEGER, SyncSSDHDataPayload::entitiesEnlightened,
            SyncSSDHDataPayload::new
    );

    @Override
    public Id<? extends CustomPayload> getId() {
        return ID;
    }
}
