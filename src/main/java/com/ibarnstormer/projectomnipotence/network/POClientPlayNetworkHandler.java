package com.ibarnstormer.projectomnipotence.network;

import com.ibarnstormer.projectomnipotence.network.payload.SyncSSDHDataPayload;
import com.ibarnstormer.projectomnipotence.utils.POUtils;
import com.mojang.authlib.GameProfile;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.nbt.NbtCompound;

public class POClientPlayNetworkHandler {

    public static void handle(SyncSSDHDataPayload payload, ClientPlayNetworking.Context context) {
        if(context.player() != null) {
            ClientPlayerEntity player = context.player();

            GameProfile profile = payload.profile();
            boolean isOmnipotent = payload.isOmnipotent();
            int entitiesEnlightened = payload.entitiesEnlightened();

            if(player.getUuid().equals(profile.getId())) {
                NbtCompound nbt = new NbtCompound();

                nbt.putBoolean("isOmnipotent", isOmnipotent);
                nbt.putInt("EntitiesEnlightened", entitiesEnlightened);

                POUtils.readPlayerNbt(player, nbt);
            }

        }
    }
}
