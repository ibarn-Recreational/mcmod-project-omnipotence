package com.ibarnstormer.projectomnipotence.mixin;

import com.ibarnstormer.projectomnipotence.Main;
import com.ibarnstormer.projectomnipotence.entity.IHarmonicEntity;
import com.ibarnstormer.projectomnipotence.utils.POUtils;
import net.minecraft.advancements.triggers.CriteriaTriggers;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(LivingEntity.class)
public abstract class LivingEntityMixin extends EntityMixin implements IHarmonicEntity {

    @Unique
    boolean inHarmony;

    @Unique
    private LivingEntity getEntity() {
        return (LivingEntity) (Object) this;
    }

    @Inject(method = "readAdditionalSaveData", at = @At("TAIL"))
    public void livingEntity$readCustomData(ValueInput view, CallbackInfo ci) {
        LivingEntity thisEntity = this.getEntity();
        if(thisEntity.getType() != EntityTypes.PLAYER) {
            POUtils.readNonPlayerData(thisEntity, view);
        }
    }

    @Inject(method = "addAdditionalSaveData", at = @At("TAIL"))
    public void livingEntity$writeCustomData(ValueOutput view, CallbackInfo ci) {
        LivingEntity thisEntity = this.getEntity();
        if(thisEntity.getType() != EntityTypes.PLAYER) {
            POUtils.writeNonPlayerData(thisEntity, view);
        }
    }

    @Inject(method = "canAttack(Lnet/minecraft/world/entity/LivingEntity;)Z", at = @At("RETURN"), cancellable = true)
    public void livingEntity$canTarget(LivingEntity target, CallbackInfoReturnable<Boolean> cir) {
        if (target instanceof Player player && POUtils.isOmnipotent(player)) {
            cir.setReturnValue(false);
        }
        else if (POUtils.isInHarmony(target)) cir.setReturnValue(false);
    }

    @Inject(method = "hurtServer", at = @At("HEAD"), cancellable = true)
    public void livingEntity$damage(ServerLevel world, DamageSource source, float amount, CallbackInfoReturnable<Boolean> cir) {
        LivingEntity thisEntity = this.getEntity();

        if(source.getEntity() instanceof Player playerAttacker) {
            if(POUtils.isOmnipotent(playerAttacker) && !POUtils.enlightenedPlayerInCreative(playerAttacker) && thisEntity.getType() != EntityTypes.PLAYER) {
                if (thisEntity.getType() == EntityTypes.ENDER_DRAGON) {
                    if(playerAttacker instanceof ServerPlayer serverPlayer) CriteriaTriggers.PLAYER_KILLED_ENTITY.trigger(serverPlayer, thisEntity, source);
                    if (thisEntity.level() instanceof ServerLevel serverWorld) {
                        if(serverWorld.getDragonFight() != null) playerAttacker.giveExperiencePoints(serverWorld.getDragonFight().hasPreviouslyKilledDragon() ? 1000 : 24000);
                        for(ServerPlayer serverPlayer : serverWorld.players()) {
                            serverWorld.sendParticles(serverPlayer, ParticleTypes.END_ROD, false, true, thisEntity.getX(), thisEntity.getY() + thisEntity.getBoundingBox().getYsize() / 2, thisEntity.getZ(), 50, Math.random() * 0.5, Math.random() * 0.5, Math.random() * 0.5, 0.5);
                        }
                    }
                }
                if (!POUtils.isInHarmony(thisEntity)) {
                    POUtils.handleEnlightenment(thisEntity, playerAttacker, source);
                }
                cir.setReturnValue(false);
            }
            if(POUtils.isOmnipotent(playerAttacker) && thisEntity.getType() == EntityTypes.PLAYER && !POUtils.enlightenedPlayerInCreative(playerAttacker)) {
                cir.setReturnValue(false);
            }
        }
    }

    @Inject(method = "setHealth", at = @At("HEAD"), cancellable = true)
    public void livingEntity$setHealth(float health, CallbackInfo ci) {
        LivingEntity thisEntity = this.getEntity();
        if(thisEntity instanceof Player player) {
            if(POUtils.isOmnipotent(player) && POUtils.getEntitiesEnlightened(player) >= Main.CONFIG.invulnerabilityEntityGoal && Main.CONFIG.omnipotentPlayersCanBecomeInvulnerable && health < Math.max(thisEntity.getMaxHealth(), 20.0F)) {
                ci.cancel();
            }
        }
    }

    @Inject(method = "tick", at = @At("TAIL"))
    public void livingEntity$tick(CallbackInfo ci) {
        LivingEntity thisEntity = this.getEntity();

        if(POUtils.isInHarmony(thisEntity) && thisEntity.getType() != EntityTypes.PLAYER) {
            if (thisEntity.level() instanceof ServerLevel serverWorld && thisEntity.tickCount % 5 == 0) {
                POUtils.spawnEnlightenmentParticles(thisEntity, serverWorld);
            }
            thisEntity.skipDropExperience();
            if(thisEntity.getType() == EntityTypes.ENDER_DRAGON) {
                thisEntity.setDeltaMovement(thisEntity.getDeltaMovement().x, 2.0D, thisEntity.getDeltaMovement().z);
                if(thisEntity.getY() > thisEntity.level().getHeight() && thisEntity.level() instanceof ServerLevel serverWorld) {
                    serverWorld.playSound(null, thisEntity.getX(), thisEntity.getY(), thisEntity.getZ(), SoundEvents.END_PORTAL_SPAWN, SoundSource.MASTER, 500, 1);
                    thisEntity.kill(serverWorld);
                }
            }
        }
    }

    @Inject(method = "dropAllDeathLoot", at = @At("HEAD"), cancellable = true)
    public void livingEntity$drop(ServerLevel world, DamageSource damageSource, CallbackInfo ci) {
        LivingEntity thisEntity = this.getEntity();
        if(POUtils.isInHarmony(thisEntity) && thisEntity.getType() != EntityTypes.PLAYER) ci.cancel();
    }

    @Inject(method = "isDeadOrDying", at = @At("RETURN"), cancellable = true)
    public void livingEntity$isDead(CallbackInfoReturnable<Boolean> cir) {
        LivingEntity thisEntity = this.getEntity();
        if(thisEntity instanceof Player player && POUtils.isOmnipotent(player) && POUtils.getEntitiesEnlightened(player) >= Main.CONFIG.invulnerabilityEntityGoal && Main.CONFIG.omnipotentPlayersCanBecomeInvulnerable) {
            cir.setReturnValue(false);
        }
    }

    @Inject(method = "canBeAffected", at = @At("HEAD"), cancellable = true)
    public void livingEntity$canHaveStatusEffect(MobEffectInstance effect, CallbackInfoReturnable<Boolean> cir) {
        LivingEntity thisEntity = this.getEntity();
        if(thisEntity instanceof Player player && POUtils.isOmnipotent(player) && effect.getEffect().value().getCategory() == MobEffectCategory.HARMFUL) {
            cir.setReturnValue(false);
        }
    }

    @Override
    public boolean isInHarmony() {
        return this.inHarmony;
    }

    @Override
    public void setInHarmony(boolean b) {
        this.inHarmony = b;
    }
}
