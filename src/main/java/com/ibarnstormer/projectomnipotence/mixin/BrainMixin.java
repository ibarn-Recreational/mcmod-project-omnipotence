package com.ibarnstormer.projectomnipotence.mixin;

import com.google.common.collect.ImmutableSet;
import com.ibarnstormer.projectomnipotence.utils.POUtils;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.Set;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.Brain;
import net.minecraft.world.entity.schedule.Activity;

@Mixin(Brain.class)
public class BrainMixin<E extends LivingEntity> {

    @Unique
    private boolean inHarmony = false;

    @Unique
    private Set<Activity> ignorableIfInHarmony = BrainMixin.initSet();

    // Workaround initializer (technically not needed, works either way)
    @Unique
    private static Set<Activity> initSet() {
        return ImmutableSet.of(Activity.AVOID, Activity.PANIC, Activity.FIGHT);
    }

    @Inject(method = "tick", at = @At("HEAD"))
    public void brain$tick(ServerLevel world, E entity, CallbackInfo ci) {
        this.inHarmony = POUtils.isInHarmony(entity);
    }

    @Inject(method = "activityRequirementsAreMet", at = @At("RETURN"), cancellable = true)
    public void brain$canDoActivity(Activity activity, CallbackInfoReturnable<Boolean> cir) {
        if(ignorableIfInHarmony.contains(activity) && this.inHarmony) cir.setReturnValue(false);
    }

}
