package com.ibarnstormer.projectomnipotence.utils;

import net.minecraft.entity.EntityType;
import net.minecraft.entity.SpawnReason;
import net.minecraft.entity.conversion.EntityConversionContext;
import net.minecraft.entity.conversion.EntityConversionType;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.entity.player.PlayerEntity;
import org.jetbrains.annotations.Nullable;

import java.util.function.BiFunction;
import java.util.function.Function;

public class POEntityConversionHelper<S extends MobEntity, T extends MobEntity> {

    private final EntityType<T> targetType;

    private final BiFunction<S, PlayerEntity, EntityConversionContext.Finalizer<T>> callback;

    public POEntityConversionHelper(EntityType<T> targetType, BiFunction<S, PlayerEntity, EntityConversionContext.Finalizer<T>> callback) {
        this.targetType = targetType;
        this.callback = callback;
    }

    public T convertEntity(S source, @Nullable PlayerEntity converter) {
        return source.convertTo(targetType, new EntityConversionContext(EntityConversionType.SINGLE, true, true, source.getScoreboardTeam()), SpawnReason.CONVERSION, callback.apply(source, converter));
    }

}
