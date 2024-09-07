package com.ibarnstormer.projectomnipotence.mixin;

import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(LivingEntity.class)
public interface LivingEntityInvoker {

    @Invoker("dropExperience")
    void dropMobExperience();

    @Invoker("dropFromLootTable")
    void dropMobLoot(DamageSource src, boolean b);

    @Invoker("dropCustomDeathLoot")
    void dropEntityEquipment(DamageSource source, int lootingMultiplier, boolean allowDrops);

}
