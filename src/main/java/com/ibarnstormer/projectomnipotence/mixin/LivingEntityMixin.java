package com.ibarnstormer.projectomnipotence.mixin;

import com.ibarnstormer.projectomnipotence.Main;
import com.ibarnstormer.projectomnipotence.capability.ModCapabilityProvider;
import com.ibarnstormer.projectomnipotence.entity.HarmonicEntity;
import com.ibarnstormer.projectomnipotence.utils.Utils;
import net.minecraft.advancements.CriteriaTriggers;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.Objects;
import java.util.concurrent.atomic.AtomicBoolean;

@Mixin(LivingEntity.class)
public abstract class LivingEntityMixin extends Entity implements HarmonicEntity {

    @Unique
    private static final EntityDataAccessor<Boolean> IN_HARMONY = SynchedEntityData.defineId(LivingEntity.class, EntityDataSerializers.BOOLEAN);

    protected LivingEntityMixin(EntityType<? extends LivingEntity> p_20966_, Level p_20967_) {
        super(p_20966_, p_20967_);
    }

    @Inject(method = "defineSynchedData", at = @At("HEAD"))
    public void onDefineSyncedData(CallbackInfo ci) {
        entityData.define(IN_HARMONY, false);
    }

    @Inject(method = "readAdditionalSaveData", at = @At("TAIL"))
    public void onReadNbtData(CompoundTag p_21096_, CallbackInfo ci) {
        entityData.set(IN_HARMONY, p_21096_.getBoolean("inHarmony"));
    }

    @Inject(method = "addAdditionalSaveData", at = @At("TAIL"))
    public void onWriteNbtData(CompoundTag p_21145_, CallbackInfo ci) {
        p_21145_.putBoolean("inHarmony", entityData.get(IN_HARMONY));
    }

    @Inject(method = "canAttack(Lnet/minecraft/world/entity/LivingEntity;)Z", at = @At("RETURN"), cancellable = true)
    public void revokeTarget(LivingEntity p_21171_, CallbackInfoReturnable<Boolean> cir) {
        if(p_21171_ instanceof Player player) {
            player.getCapability(ModCapabilityProvider.OMNIPOTENCE_CAPABILITY).ifPresent((cap) -> {
                if(cap.isOmnipotent()) cir.setReturnValue(false);
            });
        }
        else if(p_21171_.getEntityData().get(IN_HARMONY)) cir.setReturnValue(false);
    }

    @Inject(method = "hurt", at = @At("HEAD"), cancellable = true)
    public void modulateDamage(DamageSource p_21016_, float p_21017_, CallbackInfoReturnable<Boolean> cir) {
        LivingEntity thisEntity = (LivingEntity)(Object) this;

        if(p_21016_.getEntity() instanceof Player playerAttacker) {
            playerAttacker.getCapability(ModCapabilityProvider.OMNIPOTENCE_CAPABILITY).ifPresent((cap) -> {
                if(cap.isOmnipotent() && !playerAttacker.isCreative() && thisEntity.getType() != EntityType.PLAYER) {
                    if(thisEntity.getType() == EntityType.ENDER_DRAGON) {
                        if(playerAttacker instanceof ServerPlayer serverPlayer) CriteriaTriggers.PLAYER_KILLED_ENTITY.trigger(serverPlayer, thisEntity, p_21016_);
                        if (level() instanceof ServerLevel serverWorld) {
                            if(serverWorld.getDragonFight() != null) playerAttacker.giveExperiencePoints(Objects.requireNonNull(serverWorld.getDragonFight()).hasPreviouslyKilledDragon() ? 1000 : 24000);
                            for(ServerPlayer serverPlayer : serverWorld.players()) {
                                serverWorld.sendParticles(serverPlayer, ParticleTypes.END_ROD, true, thisEntity.getX(), thisEntity.getY() + thisEntity.getBoundingBox().getYsize() / 2, thisEntity.getZ(), 50, Math.random() * 0.5, Math.random() * 0.5, Math.random() * 0.5, 0.5);
                            }
                        }
                    }
                    if(!level().isClientSide) {
                        HarmonicEntity harmonicEntity = (HarmonicEntity) thisEntity;
                        if (!harmonicEntity.getHarmonicState()) {
                            Utils.harmonizeEntity(thisEntity, level(), playerAttacker, p_21016_, cap);
                        }
                    }
                    cir.setReturnValue(false);
                }
                if(cap.isOmnipotent() && thisEntity.getType() == EntityType.PLAYER && !playerAttacker.isCreative()) {
                    cir.setReturnValue(false);
                }
            });
        }
    }
    
    @Inject(method = "setHealth", at = @At("HEAD"), cancellable = true)
    public void omniInvulnerability(float p_21154_, CallbackInfo ci) {
        LivingEntity thisEntity = (LivingEntity)(Object) this;
        thisEntity.getCapability(ModCapabilityProvider.OMNIPOTENCE_CAPABILITY).ifPresent(cap -> {
            if(cap.isOmnipotent() && Main.CONFIG.omnipotentPlayersCanBecomeInvulnerable && cap.getEnlightenedEntities() >= Main.CONFIG.invulnerabilityEntityGoal && p_21154_ < thisEntity.getMaxHealth()) {
                ci.cancel();
            }
        });
    }

    @Inject(method = "tick", at = @At("HEAD"))
    public void harmonicTick(CallbackInfo ci) {
        LivingEntity thisEntity = (LivingEntity)(Object) this;
        if(getHarmonicState()) {
            if(level() instanceof ServerLevel server && thisEntity.tickCount % 5 == 0) {
                Utils.spawnEnlightenmentParticles(thisEntity, server);
            }
            thisEntity.skipDropExperience();
            if(thisEntity.getType() == EntityType.ENDER_DRAGON) {
                thisEntity.setDeltaMovement(thisEntity.getDeltaMovement().x, 2.0D, thisEntity.getDeltaMovement().z);
                if(thisEntity.getY() > thisEntity.level().getHeight() && thisEntity.level() instanceof ServerLevel serverWorld) {
                    serverWorld.playSound(null, thisEntity.getX(), thisEntity.getY(), thisEntity.getZ(), SoundEvents.END_PORTAL_SPAWN, SoundSource.MASTER, 500, 1);
                    thisEntity.kill();
                }
            }

            // Workaround for mixingradle not supporting injectors into interfaces
            if(thisEntity instanceof NeutralMob neutralMob) {
                LivingEntity target = neutralMob.getTarget();
                if(target != null) {
                    if (target instanceof Player player) {
                        AtomicBoolean aB = new AtomicBoolean(true);
                        player.getCapability(ModCapabilityProvider.OMNIPOTENCE_CAPABILITY).ifPresent((cap) -> aB.set(!cap.isOmnipotent()));
                        if (!aB.get()) neutralMob.stopBeingAngry();
                    }
                    else if (target instanceof HarmonicEntity harmonicEntity && harmonicEntity.getHarmonicState()) neutralMob.stopBeingAngry();
                }
            }
        }
    }

    @Inject(method = "dropAllDeathLoot", at = @At("HEAD"), cancellable = true)
    public void preventMobDrops(DamageSource p_21192_, CallbackInfo ci) {
        if(getHarmonicState() && this.getType() != EntityType.PLAYER) ci.cancel();
    }

    public void setHarmonicState(boolean val) {
        entityData.set(IN_HARMONY, val);
    }

    public boolean getHarmonicState() {
        return entityData.get(IN_HARMONY);
    }
}
