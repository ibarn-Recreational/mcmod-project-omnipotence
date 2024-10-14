package com.ibarnstormer.projectomnipotence.event;

import com.ibarnstormer.projectomnipotence.Main;
import com.ibarnstormer.projectomnipotence.capability.ModCapabilityProvider;
import com.ibarnstormer.projectomnipotence.capability.OmnipotenceCapability;
import com.ibarnstormer.projectomnipotence.network.ModNetwork;
import com.ibarnstormer.projectomnipotence.network.UpdateModCapabilitiesPacket;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.common.capabilities.RegisterCapabilitiesEvent;
import net.neoforged.neoforge.event.AttachCapabilitiesEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;


@Mod.EventBusSubscriber(modid = Main.MODID)
public class ModEvents {

    @SubscribeEvent
    public static void onAttachCapabilities(AttachCapabilitiesEvent<Entity> event) {
        if(event.getObject() instanceof Player player) {
            if(!player.getCapability(ModCapabilityProvider.OMNIPOTENCE_CAPABILITY).isPresent()) {
                event.addCapability(new ResourceLocation(Main.MODID, "persistent_player_data"), new ModCapabilityProvider());
            }
        }
    }

    @SubscribeEvent
    public static void onPlayerCloned(PlayerEvent.Clone event) {
        event.getOriginal().reviveCaps();
        event.getEntity().getCapability(ModCapabilityProvider.OMNIPOTENCE_CAPABILITY).ifPresent(newPlayer -> {
            event.getOriginal().getCapability(ModCapabilityProvider.OMNIPOTENCE_CAPABILITY).ifPresent(oldPlayer -> {
                newPlayer.copyFrom(oldPlayer);
                ModNetwork.sendToPlayer((ServerPlayer) event.getEntity(), new UpdateModCapabilitiesPacket(newPlayer.isOmnipotent(), newPlayer.getEnlightenedEntities()));
            });
        });
        event.getOriginal().invalidateCaps();
    }

    @SubscribeEvent
    public static void onPlayerLoggedIn(PlayerEvent.StartTracking event) {
        syncPlayerData(event.getEntity());
    }

    @SubscribeEvent
    public static void onPlayerChangeDimension(PlayerEvent.PlayerChangedDimensionEvent event) {
        syncPlayerData(event.getEntity());
    }

    public static void syncPlayerData(Player player) {
        player.getCapability(ModCapabilityProvider.OMNIPOTENCE_CAPABILITY).ifPresent((cap) -> {
            cap.setOmnipotent(cap.isOmnipotent(), player.level(), player, false);
            cap.setEnlightenedEntities(cap.getEnlightenedEntities(), player);
            ModNetwork.sendToPlayer((ServerPlayer) player, new UpdateModCapabilitiesPacket(cap.isOmnipotent(), cap.getEnlightenedEntities()));
        });
    }

    @SubscribeEvent
    public static void onRegisterCapability(RegisterCapabilitiesEvent event) {
        event.register(OmnipotenceCapability.class);
    }

}
