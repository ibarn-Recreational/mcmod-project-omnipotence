package com.ibarnstormer.projectomnipotence.mixin;

import com.ibarnstormer.projectomnipotence.utils.POUtils;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.block.entity.trialspawner.TrialSpawner;

@Mixin(TrialSpawner.class)
public class TrialSpawnerMixin {

    @Inject(method = "shouldMobBeUntracked", at = @At("RETURN"), cancellable = true)
    private static void trialSpawnerLogic$shouldMobBeUntracked(ServerLevel world, BlockPos pos, UUID uuid, CallbackInfoReturnable<Boolean> cir) {
        Entity entity = world.getEntity(uuid);
        if(POUtils.isInHarmony(entity)) cir.setReturnValue(true);
    }

}
