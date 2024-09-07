package com.ibarnstormer.projectomnipotence;

import com.ibarnstormer.projectomnipotence.network.POClientPlayNetworkHandler;
import com.ibarnstormer.projectomnipotence.network.payload.SyncSSDHDataPayload;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;

@Environment(EnvType.CLIENT)
public class ClientMain implements ClientModInitializer {

    @Override
    public void onInitializeClient() {

        // Custom S2C Network handlers
        ClientPlayNetworking.registerGlobalReceiver(SyncSSDHDataPayload.ID, POClientPlayNetworkHandler::handle);

    }
}
