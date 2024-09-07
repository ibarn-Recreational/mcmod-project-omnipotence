package com.ibarnstormer.projectomnipotence.mixin;

import com.google.common.collect.ImmutableSet;
import com.ibarnstormer.projectomnipotence.entity.HarmonicEntity;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.Brain;
import net.minecraft.world.entity.schedule.Activity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.Set;

@Mixin(Brain.class)
public class BrainMixin<E extends LivingEntity> {

    @Unique
    private boolean inHarmony = false;

    @Unique
    private Set<Activity> ignorableIfInHarmony = BrainMixin.initSet();

    // Workaround initializer
    @Unique
    private static Set<Activity> initSet() {
        return ImmutableSet.of(Activity.AVOID, Activity.PANIC);
    }

    @Inject(method = "tick", at = @At("HEAD"))
    public void brain$tick(ServerLevel world, E entity, CallbackInfo ci) {
        if(entity instanceof HarmonicEntity harmonicEntity) this.inHarmony = harmonicEntity.getHarmonicState();
    }

    @Inject(method = "activityRequirementsAreMet", at = @At("RETURN"), cancellable = true)
    public void brain$isMemoryInState(Activity p_21970_, CallbackInfoReturnable<Boolean> cir) {
        if(ignorableIfInHarmony.contains(p_21970_) && this.inHarmony) cir.setReturnValue(false);
    }


}
