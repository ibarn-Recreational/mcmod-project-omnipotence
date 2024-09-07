package com.ibarnstormer.projectomnipotence.block.entity;

import net.minecraft.entity.player.PlayerEntity;
import org.jetbrains.annotations.Nullable;

public interface EnlighteningBeacon {

    void setAsEnlightening(@Nullable PlayerEntity player);
    void setEnlightenedCache(int i);
    int getEnlightenedCache();
    boolean isEnlightening();
    @Nullable PlayerEntity getOmnipotentOwner();

}
