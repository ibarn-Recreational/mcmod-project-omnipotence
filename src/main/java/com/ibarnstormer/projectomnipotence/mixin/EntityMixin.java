package com.ibarnstormer.projectomnipotence.mixin;

import com.ibarnstormer.projectomnipotence.Main;
import com.ibarnstormer.projectomnipotence.utils.POUtils;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.ProjectileDeflection;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.projectile.ProjectileEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Entity.class)
public abstract class EntityMixin {

    @Shadow public abstract EntityType<?> getType();

    @Unique
    private Entity getEntity() {
        return (Entity) (Object) this;
    }

    @Inject(method = "isTeammate", at = @At("RETURN"), cancellable = true)
    public void entity$isTeammate(Entity other, CallbackInfoReturnable<Boolean> cir) {
        Entity thisEntity = this.getEntity();
        if(POUtils.isInHarmony(thisEntity) || POUtils.isInHarmony(other)) cir.setReturnValue(true);
    }

    @Inject(method = "isFireImmune", at = @At("RETURN"), cancellable = true)
    public void entity$isFireImmune(CallbackInfoReturnable<Boolean> cir) {
        Entity thisEntity = this.getEntity();
        if(POUtils.isInHarmony(thisEntity) && thisEntity instanceof PlayerEntity player) {
            if(POUtils.getEntitiesEnlightened(player) >= Main.CONFIG.invulnerabilityEntityGoal && Main.CONFIG.omnipotentPlayersCanBecomeInvulnerable)
                cir.setReturnValue(true);
        }
    }

    @Inject(method = "getProjectileDeflection", at = @At("RETURN"), cancellable = true)
    public void entity$getProjectileDeflection(ProjectileEntity projectile, CallbackInfoReturnable<ProjectileDeflection> cir) {
        Entity thisEntity = this.getEntity();
        if(this.getType() == EntityType.PLAYER && POUtils.isInHarmony(thisEntity)) {
            cir.setReturnValue(POUtils.OMNIPOTENT_PROJECTILE_DEFLECTOR);
        }
    }

    @Inject(method = "setRemoved", at = @At("HEAD"), cancellable = true)
    public void entity$setRemoved(Entity.RemovalReason reason, CallbackInfo ci) {
        Entity thisEntity = this.getEntity();
        if(thisEntity instanceof PlayerEntity player && POUtils.isOmnipotent(player) && POUtils.getEntitiesEnlightened(player) >= Main.CONFIG.invulnerabilityEntityGoal && Main.CONFIG.omnipotentPlayersCanBecomeInvulnerable && reason == Entity.RemovalReason.KILLED) {
            ci.cancel();
        }
    }

    @Inject(method = "remove", at = @At("HEAD"), cancellable = true)
    public void entity$remove(Entity.RemovalReason reason, CallbackInfo ci) {
        Entity thisEntity = this.getEntity();
        if(thisEntity instanceof PlayerEntity player && POUtils.isOmnipotent(player) && POUtils.getEntitiesEnlightened(player) >= Main.CONFIG.invulnerabilityEntityGoal && Main.CONFIG.omnipotentPlayersCanBecomeInvulnerable && reason == Entity.RemovalReason.KILLED) {
            ci.cancel();
        }
    }
}
