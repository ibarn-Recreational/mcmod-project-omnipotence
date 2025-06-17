package com.ibarnstormer.projectomnipotence.network;

import com.ibarnstormer.projectomnipotence.entity.IPOPlayerEntity;
import com.ibarnstormer.projectomnipotence.network.payload.SyncSSDHDataPayload;
import com.mojang.authlib.GameProfile;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.network.ClientPlayerEntity;

public class POClientPlayNetworkHandler {

    public static void handle(SyncSSDHDataPayload payload, ClientPlayNetworking.Context context) {
        if(context.player() != null) {
            ClientPlayerEntity player = context.player();

            GameProfile profile = payload.profile();
            boolean isOmnipotent = payload.isOmnipotent();
            int entitiesEnlightened = payload.entitiesEnlightened();

            if(player.getUuid().equals(profile.getId())) {

                ((IPOPlayerEntity) player).setOmnipotent(isOmnipotent);
                ((IPOPlayerEntity) player).setEntitiesEnlightened(entitiesEnlightened);
            }

        }
    }
}
