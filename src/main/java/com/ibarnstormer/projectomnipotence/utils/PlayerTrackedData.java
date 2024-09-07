package com.ibarnstormer.projectomnipotence.utils;

import com.ibarnstormer.projectomnipotence.entity.data.ServersideDataTracker;
import net.minecraft.entity.data.*;
import net.minecraft.entity.player.PlayerEntity;

public record PlayerTrackedData(TrackedData<Boolean> IS_OMNIPOTENT, TrackedData<Integer> ENTITIES_ENLIGHTENED) {

    public PlayerTrackedData() {
        this(ServersideDataTracker.registerData(PlayerEntity.class, TrackedDataHandlerRegistry.BOOLEAN), ServersideDataTracker.registerData(PlayerEntity.class, TrackedDataHandlerRegistry.INTEGER));
    }

}
