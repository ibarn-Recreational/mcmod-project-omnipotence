package com.ibarnstormer.projectomnipotence.network;

import com.ibarnstormer.projectomnipotence.Main;
import com.ibarnstormer.projectomnipotence.registry.ModAttachmentTypes;
import com.mojang.authlib.GameProfile;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import org.jetbrains.annotations.NotNull;


public record UpdateOmnipotentDataPayload(GameProfile profile, boolean isOmnipotent, int entitiesEnlightened) implements CustomPacketPayload {
    public static final Type<UpdateOmnipotentDataPayload> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(Main.MODID, "update_omnipotent_data"));

    public static final StreamCodec<FriendlyByteBuf, UpdateOmnipotentDataPayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.GAME_PROFILE,
            UpdateOmnipotentDataPayload::profile,
            ByteBufCodecs.BOOL,
            UpdateOmnipotentDataPayload::isOmnipotent,
            ByteBufCodecs.INT,
            UpdateOmnipotentDataPayload::entitiesEnlightened,
            UpdateOmnipotentDataPayload::new);


    @Override
    public @NotNull Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(UpdateOmnipotentDataPayload payload, IPayloadContext context) {
        try {
            LocalPlayer player = (LocalPlayer) context.player();

            GameProfile profile = payload.profile();
            boolean isOmnipotent = payload.isOmnipotent();
            int entitiesEnlightened = payload.entitiesEnlightened();

            if(player.getUUID().equals(profile.getId())) {
                player.setData(ModAttachmentTypes.IS_OMNIPOTENT, isOmnipotent);
                player.setData(ModAttachmentTypes.ENTITIES_ENLIGHTENED, entitiesEnlightened);
            }
        }
        catch(Exception ignored) {}
    }

}
