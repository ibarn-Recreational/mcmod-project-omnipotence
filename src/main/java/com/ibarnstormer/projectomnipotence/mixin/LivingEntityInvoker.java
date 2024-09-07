package com.ibarnstormer.projectomnipotence.mixin;

import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.server.world.ServerWorld;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(LivingEntity.class)
public interface LivingEntityInvoker {

    @Invoker("dropXp")
    void dropMobExperience(@Nullable Entity attacker);

    @Invoker("dropLoot")
    void dropLootTableLoot(DamageSource source, boolean causedByPlayer);

    @Invoker("dropEquipment")
    void dropEntityEquipment(ServerWorld world, DamageSource source, boolean causedByPlayer);

}
