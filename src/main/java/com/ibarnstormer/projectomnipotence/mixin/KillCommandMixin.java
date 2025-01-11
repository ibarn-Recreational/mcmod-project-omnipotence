package com.ibarnstormer.projectomnipotence.mixin;

import com.ibarnstormer.projectomnipotence.utils.POUtils;
import com.llamalad7.mixinextras.sugar.Local;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.server.commands.KillCommand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.Collection;

@Mixin(KillCommand.class)
public class KillCommandMixin {

    // Against bad actors
    @Inject(method = "kill", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/Entity;kill()V"), cancellable = true)
    private static void killCommand$kill(CommandSourceStack p_137814_, Collection<? extends Entity> p_137815_, CallbackInfoReturnable<Integer> cir, @Local Entity entity) {
        if(entity instanceof Player player && POUtils.isOmnipotent(player) && player.isAlive()) {
            cir.cancel();
        }
    }

}
