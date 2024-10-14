package com.ibarnstormer.projectomnipotence.network;

import net.minecraft.network.FriendlyByteBuf;
import net.neoforged.neoforge.network.NetworkEvent;

import java.util.function.Supplier;

public interface IModPacket {

    void send(FriendlyByteBuf buf);

    static void handle(IModPacket packet, NetworkEvent.Context ctx) {}

}
