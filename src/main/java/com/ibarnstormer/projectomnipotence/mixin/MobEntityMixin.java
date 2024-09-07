package com.ibarnstormer.projectomnipotence.mixin;

import com.ibarnstormer.projectomnipotence.utils.POUtils;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.mob.MobEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(MobEntity.class)
public class MobEntityMixin<T extends MobEntity> {

    @Shadow
    private LivingEntity target;

    @Inject(method = "canTarget", at = @At("RETURN"), cancellable = true)
    public void mobEntity$canTarget(EntityType<?> type, CallbackInfoReturnable<Boolean> cir) {
        if(POUtils.isInHarmony((MobEntity) (Object) this)) cir.setReturnValue(false);
    }

    @Inject(method = "setTarget", at = @At("TAIL"))
    public void mobEntity$setTarget(LivingEntity target, CallbackInfo ci) {
        if(POUtils.isInHarmony(target) && this.target == target) this.target = null;
    }

    @Inject(method = "convertTo", at = @At("RETURN"), cancellable = true)
    public void mobEntity$convertTo(EntityType<T> entityType, boolean keepEquipment, CallbackInfoReturnable<T> cir) {
        MobEntity thisMob = (MobEntity) (Object) this;
        if(POUtils.isInHarmony(thisMob)) {
            T converionResult = cir.getReturnValue();
            POUtils.setInHarmony(converionResult, true);
            cir.setReturnValue(converionResult);
        }
    }

}
