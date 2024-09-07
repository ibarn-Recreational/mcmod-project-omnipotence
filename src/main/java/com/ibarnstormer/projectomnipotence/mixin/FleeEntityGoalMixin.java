package com.ibarnstormer.projectomnipotence.mixin;

import com.ibarnstormer.projectomnipotence.utils.POUtils;
import net.minecraft.entity.ai.goal.FleeEntityGoal;
import net.minecraft.entity.mob.PathAwareEntity;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(FleeEntityGoal.class)
public class FleeEntityGoalMixin {

    @Shadow @Final protected PathAwareEntity mob;

    @Inject(method = "start", at = @At("HEAD"), cancellable = true)
    private void fleeEntityGoal$start(CallbackInfo ci) {
        if(POUtils.isInHarmony(mob)) ci.cancel();
    }

}
