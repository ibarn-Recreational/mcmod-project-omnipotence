package com.ibarnstormer.projectomnipotence.mixin;

import com.ibarnstormer.projectomnipotence.Main;
import com.ibarnstormer.projectomnipotence.utils.POUtils;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

import java.util.*;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.ExplosionDamageCalculator;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;

//@Mixin(World.class)
public class WorldMixin {

    //@ModifyVariable(method = "createExplosion(Lnet/minecraft/entity/Entity;Lnet/minecraft/entity/damage/DamageSource;Lnet/minecraft/world/explosion/ExplosionBehavior;DDDFZLnet/minecraft/world/World$ExplosionSourceType;ZLnet/minecraft/particle/ParticleEffect;Lnet/minecraft/particle/ParticleEffect;Lnet/minecraft/sound/SoundEvent;)Lnet/minecraft/world/explosion/Explosion;", at = @At("HEAD"), argsOnly = true)
    public Level.ExplosionInteraction world$createExplosion(Level.ExplosionInteraction value, @Nullable Entity entity, @Nullable DamageSource damageSource, @Nullable ExplosionDamageCalculator behavior, double x, double y, double z, float power, boolean createFire, Level.ExplosionInteraction explosionSourceType, boolean particles, ParticleOptions particle, ParticleOptions emitterParticle, SoundEvent soundEvent) {
        Level thisWorld = (Level) (Object) this;
        BlockPos pos = BlockPos.containing(x, y, z);
        List<Player> players = thisWorld.getEntitiesOfClass(Player.class, new AABB(pos).inflate(100));
        for(Player player : players) {
            //if(POUtils.isOmnipotent(player) && Main.CONFIG.omnipotentPlayersDampenExplosions) return World.ExplosionSourceType.NONE;
        }
        return value;
    }

}
