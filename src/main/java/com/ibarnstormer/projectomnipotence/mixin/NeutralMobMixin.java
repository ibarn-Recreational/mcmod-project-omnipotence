package com.ibarnstormer.projectomnipotence.mixin;

import com.ibarnstormer.projectomnipotence.utils.POUtils;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.NeutralMob;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value = NeutralMob.class)
public interface NeutralMobMixin {

    @Inject(method = "isAngryAt", at = @At("RETURN"), cancellable = true)
    default void angerable$shouldAngerAt(LivingEntity entity, ServerLevel world, CallbackInfoReturnable<Boolean> cir) {
        if(POUtils.isInHarmony(entity)) cir.setReturnValue(false);
    }

}
