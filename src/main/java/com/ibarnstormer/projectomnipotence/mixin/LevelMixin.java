package com.ibarnstormer.projectomnipotence.mixin;


import com.ibarnstormer.projectomnipotence.Main;
import com.ibarnstormer.projectomnipotence.capability.ModCapabilityProvider;
import net.minecraft.core.BlockPos;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.ExplosionDamageCalculator;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

import javax.annotation.Nullable;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

@Mixin(Level.class)
public class LevelMixin {

    @ModifyVariable(method = "explode(Lnet/minecraft/world/entity/Entity;Lnet/minecraft/world/damagesource/DamageSource;Lnet/minecraft/world/level/ExplosionDamageCalculator;DDDFZLnet/minecraft/world/level/Level$ExplosionInteraction;Z)Lnet/minecraft/world/level/Explosion;", at = @At("HEAD"), argsOnly = true)
    public Level.ExplosionInteraction level$explode(Level.ExplosionInteraction i, @Nullable Entity p_46526_, @Nullable DamageSource p_46527_, @Nullable ExplosionDamageCalculator p_46528_, double p_46529_, double p_46530_, double p_46531_, float p_46532_, boolean p_46533_, Level.ExplosionInteraction p_46534_, boolean p_46535_) {
        Level level = (Level) (Object) this;
        BlockPos pos = BlockPos.containing(p_46529_, p_46530_, p_46531_);
        List<Player> players = level.getEntitiesOfClass(Player.class, new AABB(pos).inflate(100));
        AtomicReference<Level.ExplosionInteraction> interaction = new AtomicReference<>(i);
        AtomicReference<Boolean> foundOmnipotentPlayer = new AtomicReference<>(false);
        for(Player player : players) {
            player.getCapability(ModCapabilityProvider.OMNIPOTENCE_CAPABILITY).ifPresent((cap) -> {
                if(cap.isOmnipotent() && Main.CONFIG.omnipotentPlayersDampenExplosions) {
                    interaction.set(Level.ExplosionInteraction.NONE);
                    foundOmnipotentPlayer.set(true);
                }
            });
            if(foundOmnipotentPlayer.get()) break;
        }
        return interaction.get();
    }


}
