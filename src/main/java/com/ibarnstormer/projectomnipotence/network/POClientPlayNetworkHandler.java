package com.ibarnstormer.projectomnipotence.network;

import com.ibarnstormer.projectomnipotence.entity.IPOPlayerEntity;
import com.ibarnstormer.projectomnipotence.network.payload.SyncSSDHDataPayload;
import com.mojang.authlib.GameProfile;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.player.LocalPlayer;

public class POClientPlayNetworkHandler {

    public static void handle(SyncSSDHDataPayload payload, ClientPlayNetworking.Context context) {
        if(context.player() != null) {
            LocalPlayer player = context.player();

            GameProfile profile = payload.profile();
            boolean isOmnipotent = payload.isOmnipotent();
            int entitiesEnlightened = payload.entitiesEnlightened();

            if(player.getUUID().equals(profile.id())) {

                ((IPOPlayerEntity) player).setOmnipotent(isOmnipotent);
                ((IPOPlayerEntity) player).setEntitiesEnlightened(entitiesEnlightened);
            }

        }
    }
}
