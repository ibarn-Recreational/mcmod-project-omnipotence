package com.ibarnstormer.projectomnipotence.mixin;

import com.ibarnstormer.projectomnipotence.Main;
import com.ibarnstormer.projectomnipotence.entity.ServerTrackedData;
import com.ibarnstormer.projectomnipotence.entity.data.ServersideDataTracker;
import com.ibarnstormer.projectomnipotence.utils.POUtils;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.ProjectileDeflection;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.projectile.ProjectileEntity;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Entity.class)
public abstract class EntityMixin implements ServerTrackedData {

    @Shadow public abstract EntityType<?> getType();

    @Unique private ServersideDataTracker serversideDataTracker;

    @Inject(method = "<init>", at = @At("TAIL"))
    public void entity$init(EntityType type, World world, CallbackInfo ci) {
        ServersideDataTracker.Builder builder = new ServersideDataTracker.Builder((Entity) (Object) this);
        this.initServersideDataTracker(builder);
        this.serversideDataTracker = builder.build();
    }

    @Inject(method = "isTeammate", at = @At("RETURN"), cancellable = true)
    public void entity$isTeammate(Entity other, CallbackInfoReturnable<Boolean> cir) {
        Entity thisEntity = (Entity) (Object) this;
        if(POUtils.isInHarmony(thisEntity) || POUtils.isInHarmony(other)) cir.setReturnValue(true);
    }

    @Inject(method = "isFireImmune", at = @At("RETURN"), cancellable = true)
    public void entity$isFireImmune(CallbackInfoReturnable<Boolean> cir) {
        Entity thisEntity = (Entity) (Object) this;
        if(POUtils.isInHarmony(thisEntity) && thisEntity instanceof PlayerEntity player) {
            if(POUtils.getEntitiesEnlightened(player) >= Main.CONFIG.invulnerabilityEntityGoal && Main.CONFIG.omnipotentPlayersCanBecomeInvulnerable)
                cir.setReturnValue(true);
        }
    }

    @Inject(method = "getProjectileDeflection", at = @At("RETURN"), cancellable = true)
    public void entity$getProjectileDeflection(ProjectileEntity projectile, CallbackInfoReturnable<ProjectileDeflection> cir) {
        Entity thisEntity = (Entity) (Object) this;
        if(this.getType() == EntityType.PLAYER && POUtils.isInHarmony(thisEntity)) {
            cir.setReturnValue(POUtils.OMNIPOTENT_PROJECTILE_DEFLECTOR);
        }
    }

    @Override
    public void initServersideDataTracker(ServersideDataTracker.Builder builder) {}

    @Override
    public ServersideDataTracker getServersideDataTracker() {
        return serversideDataTracker;
    }
}
