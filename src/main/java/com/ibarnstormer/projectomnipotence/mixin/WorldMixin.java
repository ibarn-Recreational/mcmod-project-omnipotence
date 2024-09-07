package com.ibarnstormer.projectomnipotence.mixin;

import com.ibarnstormer.projectomnipotence.Main;
import com.ibarnstormer.projectomnipotence.utils.POUtils;
import net.minecraft.entity.Entity;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.particle.ParticleEffect;
import net.minecraft.sound.SoundEvent;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.world.World;
import net.minecraft.world.explosion.ExplosionBehavior;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

import java.util.*;

//@Mixin(World.class)
public class WorldMixin {

    //@ModifyVariable(method = "createExplosion(Lnet/minecraft/entity/Entity;Lnet/minecraft/entity/damage/DamageSource;Lnet/minecraft/world/explosion/ExplosionBehavior;DDDFZLnet/minecraft/world/World$ExplosionSourceType;ZLnet/minecraft/particle/ParticleEffect;Lnet/minecraft/particle/ParticleEffect;Lnet/minecraft/sound/SoundEvent;)Lnet/minecraft/world/explosion/Explosion;", at = @At("HEAD"), argsOnly = true)
    public World.ExplosionSourceType world$createExplosion(World.ExplosionSourceType value, @Nullable Entity entity, @Nullable DamageSource damageSource, @Nullable ExplosionBehavior behavior, double x, double y, double z, float power, boolean createFire, World.ExplosionSourceType explosionSourceType, boolean particles, ParticleEffect particle, ParticleEffect emitterParticle, SoundEvent soundEvent) {
        World thisWorld = (World) (Object) this;
        BlockPos pos = BlockPos.ofFloored(x, y, z);
        List<PlayerEntity> players = thisWorld.getNonSpectatingEntities(PlayerEntity.class, new Box(pos).expand(100));
        for(PlayerEntity player : players) {
            //if(POUtils.isOmnipotent(player) && Main.CONFIG.omnipotentPlayersDampenExplosions) return World.ExplosionSourceType.NONE;
        }
        return value;
    }

}
