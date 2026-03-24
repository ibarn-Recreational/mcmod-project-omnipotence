package com.ibarnstormer.projectomnipotence.mixin;

import com.ibarnstormer.projectomnipotence.Main;
import com.ibarnstormer.projectomnipotence.utils.POUtils;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.entity.projectile.ProjectileDeflection;
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

    @Inject(method = "isAlliedTo(Lnet/minecraft/world/entity/Entity;)Z", at = @At("RETURN"), cancellable = true)
    public void entity$isTeammate(Entity other, CallbackInfoReturnable<Boolean> cir) {
        Entity thisEntity = this.getEntity();
        if(POUtils.isInHarmony(thisEntity) || POUtils.isInHarmony(other)) cir.setReturnValue(true);
    }

    @Inject(method = "fireImmune", at = @At("RETURN"), cancellable = true)
    public void entity$isFireImmune(CallbackInfoReturnable<Boolean> cir) {
        Entity thisEntity = this.getEntity();
        if(POUtils.isInHarmony(thisEntity) && thisEntity instanceof Player player) {
            if(POUtils.getEntitiesEnlightened(player) >= Main.CONFIG.invulnerabilityEntityGoal && Main.CONFIG.omnipotentPlayersCanBecomeInvulnerable)
                cir.setReturnValue(true);
        }
    }

    @Inject(method = "deflection", at = @At("RETURN"), cancellable = true)
    public void entity$getProjectileDeflection(Projectile projectile, CallbackInfoReturnable<ProjectileDeflection> cir) {
        Entity thisEntity = this.getEntity();
        if(this.getType() == EntityType.PLAYER && POUtils.isInHarmony(thisEntity)) {
            cir.setReturnValue(POUtils.OMNIPOTENT_PROJECTILE_DEFLECTOR);
        }
    }

    @Inject(method = "setRemoved", at = @At("HEAD"), cancellable = true)
    public void entity$setRemoved(Entity.RemovalReason reason, CallbackInfo ci) {
        Entity thisEntity = this.getEntity();
        if(thisEntity instanceof Player player && POUtils.isOmnipotent(player) && POUtils.getEntitiesEnlightened(player) >= Main.CONFIG.invulnerabilityEntityGoal && Main.CONFIG.omnipotentPlayersCanBecomeInvulnerable && reason == Entity.RemovalReason.KILLED) {
            ci.cancel();
        }
    }

    @Inject(method = "remove", at = @At("HEAD"), cancellable = true)
    public void entity$remove(Entity.RemovalReason reason, CallbackInfo ci) {
        Entity thisEntity = this.getEntity();
        if(thisEntity instanceof Player player && POUtils.isOmnipotent(player) && POUtils.getEntitiesEnlightened(player) >= Main.CONFIG.invulnerabilityEntityGoal && Main.CONFIG.omnipotentPlayersCanBecomeInvulnerable && reason == Entity.RemovalReason.KILLED) {
            ci.cancel();
        }
    }
}
