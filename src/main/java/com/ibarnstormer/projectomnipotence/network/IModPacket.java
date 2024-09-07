package com.ibarnstormer.projectomnipotence.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public interface IModPacket {

    void send(FriendlyByteBuf buf);
    void handle(Supplier<NetworkEvent.Context> ctx);

}
