package com.ibarnstormer.projectomnipotence.utils;

import org.jetbrains.annotations.Nullable;

import java.util.function.BiFunction;
import java.util.function.Function;
import net.minecraft.world.entity.ConversionParams;
import net.minecraft.world.entity.ConversionType;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.player.Player;

public class POEntityConversionHelper<S extends Mob, T extends Mob> {

    private final EntityType<T> targetType;

    private final BiFunction<S, Player, ConversionParams.AfterConversion<T>> callback;

    public POEntityConversionHelper(EntityType<T> targetType, BiFunction<S, Player, ConversionParams.AfterConversion<T>> callback) {
        this.targetType = targetType;
        this.callback = callback;
    }

    public T convertEntity(S source, @Nullable Player converter) {
        return source.convertTo(targetType, new ConversionParams(ConversionType.SINGLE, true, true, source.getTeam()), EntitySpawnReason.CONVERSION, callback.apply(source, converter));
    }

}
