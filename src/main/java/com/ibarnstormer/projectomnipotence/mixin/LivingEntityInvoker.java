package com.ibarnstormer.projectomnipotence.mixin;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

import javax.annotation.Nullable;

@Mixin(LivingEntity.class)
public interface LivingEntityInvoker {

    @Invoker("dropExperience")
    void dropMobExperience(@Nullable Entity entity);

    @Invoker("dropFromLootTable")
    void dropMobLoot(DamageSource src, boolean b);

}
