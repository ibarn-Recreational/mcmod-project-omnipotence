package com.ibarnstormer.projectomnipotence.mixin;

import com.ibarnstormer.projectomnipotence.utils.POUtils;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.goal.AvoidEntityGoal;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(AvoidEntityGoal.class)
public class AvoidEntityGoalMixin {

    @Shadow @Final protected PathfinderMob mob;

    @Inject(method = "start", at = @At("HEAD"), cancellable = true)
    private void avoidEntityGoal$start(CallbackInfo ci) {
        if(POUtils.isInHarmony(mob)) ci.cancel();
    }

}
