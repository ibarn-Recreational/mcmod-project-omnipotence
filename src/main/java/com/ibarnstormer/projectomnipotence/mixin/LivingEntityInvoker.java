package com.ibarnstormer.projectomnipotence.mixin;

import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.data.TrackedData;
import net.minecraft.server.world.ServerWorld;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(LivingEntity.class)
public interface LivingEntityInvoker {

    @Accessor("HEALTH")
    static TrackedData<Float> getHealthID() {
        throw new IllegalStateException();
    }

    @Invoker("dropXp")
    void dropMobExperience(ServerWorld world, @Nullable Entity attacker);

    @Invoker("dropLoot")
    void dropLootTableLoot(ServerWorld world, DamageSource source, boolean causedByPlayer);

    @Invoker("dropEquipment")
    void dropEntityEquipment(ServerWorld world, DamageSource source, boolean causedByPlayer);

}
