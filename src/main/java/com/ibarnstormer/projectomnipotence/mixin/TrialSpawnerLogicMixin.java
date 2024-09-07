package com.ibarnstormer.projectomnipotence.mixin;

import com.ibarnstormer.projectomnipotence.utils.POUtils;
import net.minecraft.block.spawner.TrialSpawnerLogic;
import net.minecraft.entity.Entity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.UUID;

@Mixin(TrialSpawnerLogic.class)
public class TrialSpawnerLogicMixin {

    @Inject(method = "shouldRemoveMobFromData", at = @At("RETURN"), cancellable = true)
    private static void trialSpawnerLogic$shouldRemoveMobFromData(ServerWorld world, BlockPos pos, UUID uuid, CallbackInfoReturnable<Boolean> cir) {
        Entity entity = world.getEntity(uuid);
        if(POUtils.isInHarmony(entity)) cir.setReturnValue(true);
    }

}
