package com.ibarnstormer.projectomnipotence.mixin;
import com.ibarnstormer.projectomnipotence.Main;
import com.ibarnstormer.projectomnipotence.entity.HarmonicEntity;
import com.ibarnstormer.projectomnipotence.utils.POUtils;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.entity.projectile.ProjectileDeflection;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Entity.class)
public class EntityMixin {


    @Inject(method = "isAlliedTo(Lnet/minecraft/world/entity/Entity;)Z", at = @At("RETURN"), cancellable = true)
    public void teamMate(Entity entity, CallbackInfoReturnable<Boolean> cir) {
        Entity thisEntity = (Entity) (Object) this;
        if(thisEntity instanceof HarmonicEntity harmonicEntity) {
            if(harmonicEntity.getHarmonicState()) {
                cir.setReturnValue(true);
            }
            if(entity instanceof Player player && POUtils.isOmnipotent(player)) {
                cir.setReturnValue(true);
            }
        }
        if(entity instanceof HarmonicEntity harmonicEntity) {
            if(harmonicEntity.getHarmonicState()) cir.setReturnValue(true);
        }
    }

    @Inject(method = "fireImmune", at = @At("RETURN"), cancellable = true)
    public void entity$fireImmune(CallbackInfoReturnable<Boolean> cir) {
        Entity thisEntity = (Entity) (Object) this;
        if(thisEntity instanceof Player player && POUtils.isOmnipotent(player) && POUtils.getEnlightenedEntities(player) >= Main.CONFIG.invulnerabilityEntityGoal && Main.CONFIG.omnipotentPlayersCanBecomeInvulnerable) {
            cir.setReturnValue(true);
        }
    }

    @Inject(method = "deflection", at = @At("RETURN"), cancellable = true)
    public void entity$getProjectileDeflection(Projectile projectile, CallbackInfoReturnable<ProjectileDeflection> cir) {
        Entity thisEntity = (Entity) (Object) this;
        if(thisEntity instanceof Player player && POUtils.isOmnipotent(player)) {
            cir.setReturnValue(POUtils.OMNIPOTENT_PROJECTILE_DEFLECTOR);
        }
    }


}
