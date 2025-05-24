package com.ibarnstormer.projectomnipotence.mixin;


import com.ibarnstormer.projectomnipotence.Main;
import com.ibarnstormer.projectomnipotence.capability.ModCapabilityProvider;
import com.ibarnstormer.projectomnipotence.entity.HarmonicEntity;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;


@Mixin(Entity.class)
public class EntityMixin {

    @Unique
    private Entity getEntity() {
        return (Entity) (Object) this;
    }

    @Inject(method = "isAlliedTo(Lnet/minecraft/world/entity/Entity;)Z", at = @At("RETURN"), cancellable = true)
    public void teamMate(Entity p_20355_, CallbackInfoReturnable<Boolean> cir) {
        Entity thisEntity = this.getEntity();
        if(thisEntity instanceof HarmonicEntity harmonicEntity) {
            if(harmonicEntity.getHarmonicState()) {
                cir.setReturnValue(true);
            }
            if(p_20355_ instanceof Player player) {
                player.getCapability(ModCapabilityProvider.OMNIPOTENCE_CAPABILITY).ifPresent((cap) -> {
                    if(cap.isOmnipotent()) cir.setReturnValue(true);
                });
            }
        }
        if(p_20355_ instanceof HarmonicEntity harmonicEntity) {
            if(harmonicEntity.getHarmonicState()) cir.setReturnValue(true);
        }
    }

    @Inject(method = "fireImmune", at = @At("RETURN"), cancellable = true)
    public void entity$fireImmune(CallbackInfoReturnable<Boolean> cir) {
        Entity thisEntity = this.getEntity();
        thisEntity.getCapability(ModCapabilityProvider.OMNIPOTENCE_CAPABILITY).ifPresent((cap) -> {
            if(cap.isOmnipotent() && cap.getEnlightenedEntities() >= Main.CONFIG.invulnerabilityEntityGoal && Main.CONFIG.omnipotentPlayersCanBecomeInvulnerable) {
                cir.setReturnValue(true);
            }
        });
    }

    @Inject(method = "setRemoved", at = @At("HEAD"), cancellable = true)
    public void entity$setRemoved(Entity.RemovalReason reason, CallbackInfo ci) {
        Entity thisEntity = this.getEntity();
        thisEntity.getCapability(ModCapabilityProvider.OMNIPOTENCE_CAPABILITY).ifPresent((cap) -> {
            if(cap.isOmnipotent() && cap.getEnlightenedEntities() >= Main.CONFIG.invulnerabilityEntityGoal && Main.CONFIG.omnipotentPlayersCanBecomeInvulnerable && reason == Entity.RemovalReason.KILLED) {
                ci.cancel();
            }
        });
    }

    @Inject(method = "remove", at = @At("HEAD"), cancellable = true)
    public void entity$remove(Entity.RemovalReason reason, CallbackInfo ci) {
        Entity thisEntity = this.getEntity();
        thisEntity.getCapability(ModCapabilityProvider.OMNIPOTENCE_CAPABILITY).ifPresent((cap) -> {
            if(cap.isOmnipotent() && cap.getEnlightenedEntities() >= Main.CONFIG.invulnerabilityEntityGoal && Main.CONFIG.omnipotentPlayersCanBecomeInvulnerable && reason == Entity.RemovalReason.KILLED) {
                ci.cancel();
            }
        });
    }

}
