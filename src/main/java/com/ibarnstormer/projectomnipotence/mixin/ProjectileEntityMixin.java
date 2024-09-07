package com.ibarnstormer.projectomnipotence.mixin;

import com.ibarnstormer.projectomnipotence.Main;
import com.ibarnstormer.projectomnipotence.capability.ModCapabilityProvider;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Projectile.class)
public class ProjectileEntityMixin {

    @Inject(method = "onHit", at = @At("HEAD"), cancellable = true)
    private void projectile$onHit(HitResult hitResult, CallbackInfo ci) {
        Projectile projectile = (Projectile) (Object) this;
        if(hitResult instanceof EntityHitResult entityHitResult) {
            Entity entity = entityHitResult.getEntity();
            entity.getCapability(ModCapabilityProvider.OMNIPOTENCE_CAPABILITY).ifPresent((cap) -> {
                if(cap.isOmnipotent() && cap.getEnlightenedEntities() > Main.CONFIG.invulnerabilityEntityGoal) {
                    if(projectile.getOwner() != entity) {
                        if (entity.level() instanceof ServerLevel serverLevel) {
                            serverLevel.sendParticles(ParticleTypes.END_ROD, projectile.getX(), projectile.getY() + projectile.getBoundingBox().getYsize() / 2, projectile.getZ(), 5, (Math.random() * projectile.getBoundingBox().getXsize() / 2) * 0.5, (Math.random() * projectile.getBoundingBox().getYsize() / 2) * 0.5, (Math.random() * projectile.getBoundingBox().getZsize() / 2) * 0.5, 0.025);
                            serverLevel.playSound(null, projectile.getX(), projectile.getY(), projectile.getZ(), SoundEvents.CONDUIT_ACTIVATE, SoundSource.PLAYERS, 0.25f, 1);
                        }
                        projectile.setPos(entity.position().x, Short.MIN_VALUE + 1, entity.position().z);
                        projectile.remove(Entity.RemovalReason.KILLED);
                    }
                    ci.cancel();
                }
            });
        }

    }
}
