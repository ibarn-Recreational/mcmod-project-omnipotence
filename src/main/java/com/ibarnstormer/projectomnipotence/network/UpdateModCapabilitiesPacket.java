package com.ibarnstormer.projectomnipotence.network;

import com.ibarnstormer.projectomnipotence.capability.ModCapabilityProvider;
import net.minecraft.client.Minecraft;
import net.minecraft.network.FriendlyByteBuf;
import net.neoforged.neoforge.network.NetworkEvent;

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

    public static void handle(UpdateModCapabilitiesPacket packet, NetworkEvent.Context ctx) {
        ctx.enqueueWork(() -> {
            // Client-side updates
            if(Minecraft.getInstance().player != null) {
                Minecraft.getInstance().player.getCapability(ModCapabilityProvider.OMNIPOTENCE_CAPABILITY).ifPresent((cap) -> {
                    cap.setOmnipotent(packet.isOmnipotent);
                    cap.setEnlightenedEntities(packet.enlightenedEntities);
                });
            }
        });
        ctx.setPacketHandled(true);
    }


}
