package com.ibarnstormer.projectomnipotence.block.entity;

import net.minecraft.world.entity.player.Player;
import org.jetbrains.annotations.Nullable;

public interface EnlighteningBeacon {

    void setAsEnlightening(@Nullable Player player);
    void setEnlightenedCache(int i);
    int getEnlightenedCache();
    boolean isEnlightening();
    @Nullable Player getOmnipotentOwner();

}
