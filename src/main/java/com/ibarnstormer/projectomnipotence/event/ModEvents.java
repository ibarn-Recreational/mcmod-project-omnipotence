package com.ibarnstormer.projectomnipotence.event;

import com.ibarnstormer.projectomnipotence.Main;
import com.ibarnstormer.projectomnipotence.utils.POUtils;
import net.minecraft.world.entity.player.Player;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;


@EventBusSubscriber(modid = Main.MODID)
public class ModEvents {

    @SubscribeEvent
    public static void onPlayerCloned(PlayerEvent.Clone event) {
        Player newPlayer = event.getEntity();
        Player oldPlayer = event.getOriginal();

        POUtils.setOmnipotent(POUtils.isOmnipotent(oldPlayer), newPlayer.level(), newPlayer, false);
        POUtils.setEnlightenedEntities(POUtils.getEnlightenedEntities(oldPlayer), newPlayer);
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
        POUtils.setOmnipotent(POUtils.isOmnipotent(player), player.level(), player, false);
        POUtils.setEnlightenedEntities(POUtils.getEnlightenedEntities(player), player);
    }

}
