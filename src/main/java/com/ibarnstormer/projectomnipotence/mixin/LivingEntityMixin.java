package com.ibarnstormer.projectomnipotence.mixin;

import com.ibarnstormer.projectomnipotence.Main;

import com.ibarnstormer.projectomnipotence.entity.HarmonicEntity;
import com.ibarnstormer.projectomnipotence.utils.POUtils;
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
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.effect.MobEffectInstance;
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

    @Unique
    private LivingEntity getLivingEntity() {
        return (LivingEntity) (Object) this;
    }
    
    protected LivingEntityMixin(EntityType<? extends LivingEntity> type, Level level) {
        super(type, level);
    }

    @Inject(method = "defineSynchedData", at = @At("HEAD"))
    public void onDefineSyncedData(SynchedEntityData.Builder builder, CallbackInfo ci) {
        builder.define(IN_HARMONY, false);
    }

    @Inject(method = "readAdditionalSaveData", at = @At("TAIL"))
    public void onReadNbtData(CompoundTag compound, CallbackInfo ci) {
        entityData.set(IN_HARMONY, compound.getBoolean("inHarmony"));
    }

    @Inject(method = "addAdditionalSaveData", at = @At("TAIL"))
    public void onWriteNbtData(CompoundTag compound, CallbackInfo ci) {
        compound.putBoolean("inHarmony", entityData.get(IN_HARMONY));
    }

    @Inject(method = "canAttack(Lnet/minecraft/world/entity/LivingEntity;)Z", at = @At("RETURN"), cancellable = true)
    public void revokeTarget(LivingEntity livingEntity, CallbackInfoReturnable<Boolean> cir) {
        if(livingEntity instanceof Player player) {
            if(POUtils.isOmnipotent(player)) cir.setReturnValue(false);
        }
        else if(livingEntity.getEntityData().get(IN_HARMONY)) cir.setReturnValue(false);
    }

    @Inject(method = "hurt", at = @At("HEAD"), cancellable = true)
    public void modulateDamage(DamageSource p_21016_, float p_21017_, CallbackInfoReturnable<Boolean> cir) {
        LivingEntity thisEntity = this.getLivingEntity();

        if(p_21016_.getEntity() instanceof Player playerAttacker) {
            if(POUtils.isOmnipotent(playerAttacker) && !POUtils.enlightenedPlayerInCreative(playerAttacker) && thisEntity.getType() != EntityType.PLAYER) {
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
                        POUtils.handleEnlightenment(thisEntity, playerAttacker, p_21016_);
                    }
                }
                cir.setReturnValue(false);
            }
            if(POUtils.isOmnipotent(playerAttacker) && thisEntity.getType() == EntityType.PLAYER && !POUtils.enlightenedPlayerInCreative(playerAttacker)) {
                cir.setReturnValue(false);
            }
        }
    }
    
    @Inject(method = "setHealth", at = @At("HEAD"), cancellable = true)
    public void omniInvulnerability(float value, CallbackInfo ci) {
        LivingEntity thisEntity = this.getLivingEntity();
        if(thisEntity instanceof Player player && POUtils.isOmnipotent(player) && Main.CONFIG.omnipotentPlayersCanBecomeInvulnerable && POUtils.getEnlightenedEntities(player) >= Main.CONFIG.invulnerabilityEntityGoal && value < Math.max(thisEntity.getMaxHealth(), 20.0F)) {
            ci.cancel();
        }
    }

    @Inject(method = "tick", at = @At("HEAD"))
    public void harmonicTick(CallbackInfo ci) {
        LivingEntity thisEntity = this.getLivingEntity();
        if(getHarmonicState()) {
            if(level() instanceof ServerLevel server && thisEntity.tickCount % 5 == 0) {
                POUtils.spawnEnlightenmentParticles(thisEntity, server);
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
                        aB.set(!POUtils.isOmnipotent(player));
                        if (!aB.get()) neutralMob.stopBeingAngry();
                    }
                    else if (target instanceof HarmonicEntity harmonicEntity && harmonicEntity.getHarmonicState()) neutralMob.stopBeingAngry();
                }
            }
        }
    }

    @Inject(method = "dropAllDeathLoot", at = @At("HEAD"), cancellable = true)
    public void preventMobDrops(ServerLevel p_level, DamageSource damageSource, CallbackInfo ci) {
        if(getHarmonicState() && this.getType() != EntityType.PLAYER) ci.cancel();
    }

    @Inject(method = "addEffect(Lnet/minecraft/world/effect/MobEffectInstance;Lnet/minecraft/world/entity/Entity;)Z", at = @At("HEAD"), cancellable = true)
    public void livingEntity$canHaveStatusEffect(MobEffectInstance effect, Entity entity, CallbackInfoReturnable<Boolean> cir) {
        if(entity != null) {
            if (entity instanceof Player player && POUtils.isOmnipotent(player) && effect.getEffect().value().getCategory() == MobEffectCategory.HARMFUL) {
                cir.setReturnValue(false);
            }
        }
    }

    public void setHarmonicState(boolean val) {
        entityData.set(IN_HARMONY, val);
    }

    public boolean getHarmonicState() {
        return entityData.get(IN_HARMONY);
    }
}
