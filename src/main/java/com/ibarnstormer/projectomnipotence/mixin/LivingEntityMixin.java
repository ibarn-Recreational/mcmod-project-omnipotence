package com.ibarnstormer.projectomnipotence.mixin;

import com.ibarnstormer.projectomnipotence.Main;
import com.ibarnstormer.projectomnipotence.entity.data.ServersideDataTracker;
import com.ibarnstormer.projectomnipotence.utils.POUtils;
import net.minecraft.advancement.criterion.Criteria;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.data.DataTracker;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(LivingEntity.class)
public abstract class LivingEntityMixin extends EntityMixin {

    @Override
    @Unique
    public void initServersideDataTracker(ServersideDataTracker.Builder builder) {
        LivingEntity thisEntity = (LivingEntity) (Object) this;
        if(thisEntity.getType() != EntityType.PLAYER) {
            POUtils.initNonPlayerData(thisEntity, builder);
        }
    }

    @Inject(method = "readCustomDataFromNbt", at = @At("TAIL"))
    public void livingEntity$readCustomDataFromNbt(NbtCompound nbt, CallbackInfo ci) {
        LivingEntity thisEntity = (LivingEntity) (Object) this;
        if(thisEntity.getType() != EntityType.PLAYER) {
            POUtils.readNonPlayerData(thisEntity, nbt);
        }
    }

    @Inject(method = "writeCustomDataToNbt", at = @At("TAIL"))
    public void livingEntity$writeCustomDataToNbt(NbtCompound nbt, CallbackInfo ci) {
        LivingEntity thisEntity = (LivingEntity) (Object) this;
        if(thisEntity.getType() != EntityType.PLAYER) {
            POUtils.writeNonPlayerData(thisEntity, nbt);
        }
    }

    @Inject(method = "canTarget(Lnet/minecraft/entity/LivingEntity;)Z", at = @At("RETURN"), cancellable = true)
    public void livingEntity$canTarget(LivingEntity target, CallbackInfoReturnable<Boolean> cir) {
        if (target instanceof PlayerEntity player && POUtils.isOmnipotent(player)) {
            cir.setReturnValue(false);
        }
        else if (POUtils.isInHarmony(target)) cir.setReturnValue(false);
    }

    @Inject(method = "damage", at = @At("HEAD"), cancellable = true)
    public void livingEntity$damage(DamageSource source, float amount, CallbackInfoReturnable<Boolean> cir) {
        LivingEntity thisEntity = (LivingEntity)(Object) this;

        if(source.getAttacker() instanceof PlayerEntity playerAttacker) {
            if(POUtils.isOmnipotent(playerAttacker) && !playerAttacker.isCreative() && thisEntity.getType() != EntityType.PLAYER) {
                if (thisEntity.getType() == EntityType.ENDER_DRAGON) {
                    if(playerAttacker instanceof ServerPlayerEntity serverPlayer) Criteria.PLAYER_KILLED_ENTITY.trigger(serverPlayer, thisEntity, source);
                    if (thisEntity.getWorld() instanceof ServerWorld serverWorld) {
                        if(serverWorld.getEnderDragonFight() != null) playerAttacker.addExperience(serverWorld.getEnderDragonFight().toData().previouslyKilled() ? 1000 : 24000);
                        for(ServerPlayerEntity serverPlayer : serverWorld.getPlayers()) {
                            serverWorld.spawnParticles(serverPlayer, ParticleTypes.END_ROD, true, thisEntity.getX(), thisEntity.getY() + thisEntity.getBoundingBox().getLengthY() / 2, thisEntity.getZ(), 50, Math.random() * 0.5, Math.random() * 0.5, Math.random() * 0.5, 0.5);
                        }
                    }
                }
                if (!POUtils.isInHarmony(thisEntity)) {
                    POUtils.harmonizeEntity(thisEntity, playerAttacker, source);
                }
                cir.setReturnValue(false);
            }
            if(POUtils.isOmnipotent(playerAttacker) && thisEntity.getType() == EntityType.PLAYER && !playerAttacker.isCreative()) {
                cir.setReturnValue(false);
            }
        }
    }

    @Inject(method = "setHealth", at = @At("HEAD"), cancellable = true)
    public void livingEntity$setHealth(float health, CallbackInfo ci) {
        LivingEntity thisEntity = (LivingEntity) (Object) this;
        if(thisEntity instanceof PlayerEntity player) {
            if(POUtils.isOmnipotent(player) && POUtils.getEntitiesEnlightened(player) >= Main.CONFIG.invulnerabilityEntityGoal && Main.CONFIG.omnipotentPlayersCanBecomeInvulnerable && health < player.getHealth()) {
                ci.cancel();
            }
        }
    }

    @Inject(method = "tick", at = @At("TAIL"))
    public void livingEntity$tick(CallbackInfo ci) {
        LivingEntity thisEntity = (LivingEntity) (Object) this;

        if(POUtils.isInHarmony(thisEntity) && thisEntity.getType() != EntityType.PLAYER) {
            if (thisEntity.getWorld() instanceof ServerWorld serverWorld && thisEntity.age % 5 == 0) {
                POUtils.spawnEnlightenmentParticles(thisEntity, serverWorld);
            }
            thisEntity.disableExperienceDropping();
            if(thisEntity.getType() == EntityType.ENDER_DRAGON) {
                thisEntity.setVelocity(thisEntity.getVelocity().x, 2.0D, thisEntity.getVelocity().z);
                if(thisEntity.getY() > thisEntity.getWorld().getHeight() && thisEntity.getWorld() instanceof ServerWorld serverWorld) {
                    serverWorld.playSound(null, thisEntity.getX(), thisEntity.getY(), thisEntity.getZ(), SoundEvents.BLOCK_END_PORTAL_SPAWN, SoundCategory.MASTER, 500, 1);
                    thisEntity.kill();
                }
            }
        }
    }

    @Inject(method = "drop", at = @At("HEAD"), cancellable = true)
    public void livingEntity$drop(ServerWorld world, DamageSource damageSource, CallbackInfo ci) {
        LivingEntity thisEntity = (LivingEntity) (Object) this;
        if(POUtils.isInHarmony(thisEntity) && thisEntity.getType() != EntityType.PLAYER) ci.cancel();
    }
}
