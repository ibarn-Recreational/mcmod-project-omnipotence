package com.ibarnstormer.projectomnipotence.network;

import com.ibarnstormer.projectomnipotence.Main;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.network.simple.SimpleChannel;

public class ModNetwork {

    public static SimpleChannel ModChannel;

    public static void initNetwork() {
        ModChannel = NetworkRegistry.newSimpleChannel(new ResourceLocation(Main.MODID, "network"), () -> "1.0", s -> true, s -> true);
        ModChannel.registerMessage(0, UpdateModCapabilitiesPacket.class, UpdateModCapabilitiesPacket::send, UpdateModCapabilitiesPacket::new, UpdateModCapabilitiesPacket::handle);
    }

    public static void sendToPlayer(ServerPlayer sender, IModPacket packet) {
        ModChannel.send(PacketDistributor.PLAYER.with(() -> sender), packet);
    }
}
