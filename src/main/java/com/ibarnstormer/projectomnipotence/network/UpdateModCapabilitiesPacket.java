package com.ibarnstormer.projectomnipotence.network;

import com.ibarnstormer.projectomnipotence.capability.ModCapabilityProvider;
import net.minecraft.client.Minecraft;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public class UpdateModCapabilitiesPacket implements IModPacket {

    boolean isOmnipotent;
    int enlightenedEntities;

    public UpdateModCapabilitiesPacket(boolean isOmni, int eE) {
        isOmnipotent = isOmni;
        enlightenedEntities = eE;
    }

    public UpdateModCapabilitiesPacket(FriendlyByteBuf buf) {
        isOmnipotent = buf.readBoolean();
        enlightenedEntities = buf.readInt();
    }

    public void send(FriendlyByteBuf buf) {
        buf.writeBoolean(isOmnipotent);
        buf.writeInt(enlightenedEntities);
    }

    public void handle(Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            // Client-side updates
            if(Minecraft.getInstance().player != null) {
                Minecraft.getInstance().player.getCapability(ModCapabilityProvider.OMNIPOTENCE_CAPABILITY).ifPresent((cap) -> {
                    cap.setOmnipotent(this.isOmnipotent);
                    cap.setEnlightenedEntities(this.enlightenedEntities);
                });
            }
        });
        ctx.get().setPacketHandled(true);
    }


}
