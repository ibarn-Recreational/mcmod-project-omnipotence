package com.ibarnstormer.projectomnipotence.mixin;

import com.ibarnstormer.projectomnipotence.utils.POUtils;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.server.command.KillCommand;
import net.minecraft.server.command.ServerCommandSource;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import org.spongepowered.asm.mixin.injection.callback.LocalCapture;

import java.util.Collection;
import java.util.Iterator;

@Mixin(KillCommand.class)
public class KillCommandMixin {

    // Against bad actors
    @Inject(method = "execute", at = @At(value = "INVOKE", target = "Lnet/minecraft/entity/Entity;kill(Lnet/minecraft/server/world/ServerWorld;)V"), locals = LocalCapture.CAPTURE_FAILHARD, cancellable = true)
    private static void killCommand$execute(ServerCommandSource source, Collection<? extends Entity> targets, CallbackInfoReturnable<Integer> cir, Iterator var2, Entity entity) {
        if(POUtils.isInHarmony(entity) && entity instanceof LivingEntity && entity.isAlive()) {
            cir.cancel();
        }
    }

}
