package com.ibarnstormer.projectomnipotence.mixin;

import com.ibarnstormer.projectomnipotence.utils.POUtils;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.mob.Angerable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value = Angerable.class)
public interface AngerableMixin {

    @Inject(method = "shouldAngerAt", at = @At("RETURN"), cancellable = true)
    default void angerable$shouldAngerAt(LivingEntity entity, CallbackInfoReturnable<Boolean> cir) {
        if(POUtils.isInHarmony(entity)) cir.setReturnValue(false);
    }

}
