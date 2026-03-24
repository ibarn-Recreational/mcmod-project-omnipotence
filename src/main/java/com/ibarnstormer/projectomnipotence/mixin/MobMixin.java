package com.ibarnstormer.projectomnipotence.mixin;

import com.ibarnstormer.projectomnipotence.utils.POUtils;
import net.minecraft.world.entity.ConversionParams;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Mob.class)
public class MobMixin<T extends Mob> {

    @Shadow
    private LivingEntity target;

    @Unique
    private Mob getMob() {
        return (Mob) (Object) this;
    }

    @Inject(method = "canAttack", at = @At("RETURN"), cancellable = true)
    public void mobEntity$canTarget(LivingEntity target, CallbackInfoReturnable<Boolean> cir) {
        if(POUtils.isInHarmony((Mob) (Object) this)) cir.setReturnValue(false);
    }

    @Inject(method = "setTarget", at = @At("TAIL"))
    public void mobEntity$setTarget(LivingEntity target, CallbackInfo ci) {
        if(POUtils.isInHarmony(target) && this.target == target) this.target = null;
    }

    @Inject(method = "convertTo(Lnet/minecraft/world/entity/EntityType;Lnet/minecraft/world/entity/ConversionParams;Lnet/minecraft/world/entity/EntitySpawnReason;Lnet/minecraft/world/entity/ConversionParams$AfterConversion;)Lnet/minecraft/world/entity/Mob;", at = @At("RETURN"), cancellable = true)
    public void mobEntity$convertTo(EntityType<T> entityType, ConversionParams context, EntitySpawnReason reason, ConversionParams.AfterConversion<T> finalizer, CallbackInfoReturnable<T> cir) {
        Mob thisMob = this.getMob();
        if(POUtils.isInHarmony(thisMob)) {
            T converionResult = cir.getReturnValue();
            POUtils.setInHarmony(converionResult, true);
            cir.setReturnValue(converionResult);
        }
    }

}
