package com.ibarnstormer.projectomnipotence.mixin;

import com.ibarnstormer.projectomnipotence.entity.HarmonicEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.block.entity.trialspawner.TrialSpawner;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.UUID;

@Mixin(TrialSpawner.class)
public class TrialSpawnerMixin {

    @Inject(method = "shouldMobBeUntracked", at = @At("RETURN"), cancellable = true)
    private static void trialSpawnerLogic$shouldRemoveMobFromData(ServerLevel level, BlockPos pos, UUID uuid, CallbackInfoReturnable<Boolean> cir) {
        Entity entity = level.getEntity(uuid);
        if(entity instanceof HarmonicEntity harmonicEntity && harmonicEntity.getHarmonicState()) cir.setReturnValue(true);
    }

}
