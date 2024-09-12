package com.ibarnstormer.projectomnipotence.mixin;

import com.ibarnstormer.projectomnipotence.capability.ModCapabilityProvider;
import com.ibarnstormer.projectomnipotence.utils.Utils;

import net.minecraft.commands.CommandSourceStack;
import net.minecraft.server.commands.KillCommand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
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
    @Inject(method = "kill", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/Entity;kill()V"), locals = LocalCapture.CAPTURE_FAILHARD, cancellable = true)
    private static void killCommand$kill(CommandSourceStack p_137814_, Collection<? extends Entity> p_137815_, CallbackInfoReturnable<Integer> cir, Iterator var2, Entity entity) {
        entity.getCapability(ModCapabilityProvider.OMNIPOTENCE_CAPABILITY).ifPresent(cap -> {
            if(cap.isOmnipotent() && entity instanceof LivingEntity && entity.isAlive()) {
                cir.cancel();
            }
        });
    }

}
