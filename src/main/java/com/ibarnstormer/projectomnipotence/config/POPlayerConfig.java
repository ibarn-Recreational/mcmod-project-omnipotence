package com.ibarnstormer.projectomnipotence.config;

import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;

import java.util.Optional;
import java.util.UUID;

public record POPlayerConfig(String username, String stringUUID, boolean enlightenedOnStart, int eeHandicap, int eeMultiplier) {

    public @Nullable PlayerEntity getPlayer(World world) {
        Optional<? extends PlayerEntity> playerFromUName = world.getPlayers().stream().filter(p -> p.getNameForScoreboard().equals(username)).findFirst();

        UUID uuid;
        PlayerEntity playerFromUUID;
        try {
            uuid = UUID.fromString(stringUUID);
            playerFromUUID = world.getPlayerByUuid(uuid);
        }
        catch(Exception ex) {
            playerFromUUID = null;
        }

        if(playerFromUName.isPresent() && playerFromUUID == null) return playerFromUName.get();
        else return playerFromUUID;
    }

    public boolean isWildcard() {
        return username.equals("*");
    }

}
