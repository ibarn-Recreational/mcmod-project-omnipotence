package com.ibarnstormer.projectomnipotence.network;

import com.ibarnstormer.projectomnipotence.Main;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.NetworkRegistry;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.simple.SimpleChannel;

public class ModNetwork {

    public static SimpleChannel ModChannel;

    public static void initNetwork() {
        ModChannel = NetworkRegistry.newSimpleChannel(new ResourceLocation(Main.MODID, "network"), () -> "1.0", s -> true, s -> true);
        ModChannel.messageBuilder(UpdateModCapabilitiesPacket.class, 0).encoder(UpdateModCapabilitiesPacket::send).decoder(UpdateModCapabilitiesPacket::new).consumerNetworkThread(UpdateModCapabilitiesPacket::handle).add();
    }

    public static void sendToPlayer(ServerPlayer sender, IModPacket packet) {
        ModChannel.send(PacketDistributor.PLAYER.with(() -> sender), packet);
    }
}
