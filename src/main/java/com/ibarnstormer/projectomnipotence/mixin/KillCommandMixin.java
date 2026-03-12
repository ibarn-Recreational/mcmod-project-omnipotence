package com.ibarnstormer.projectomnipotence.mixin;

import com.ibarnstormer.projectomnipotence.Main;
import com.ibarnstormer.projectomnipotence.utils.POUtils;
import com.llamalad7.mixinextras.sugar.Local;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.Collection;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.server.commands.KillCommand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;

@Mixin(KillCommand.class)
public class KillCommandMixin {


    @Inject(method = "kill", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/Entity;kill(Lnet/minecraft/server/level/ServerLevel;)V"), cancellable = true)
    private static void killCommand$execute(CommandSourceStack source, Collection<? extends Entity> targets, CallbackInfoReturnable<Integer> cir, @Local Entity entity) {
        if(entity instanceof Player player && POUtils.isOmnipotent(player) && entity.isAlive() && POUtils.getEntitiesEnlightened(player) >= Main.CONFIG.invulnerabilityEntityGoal && Main.CONFIG.omnipotentPlayersCanBecomeInvulnerable) {
            cir.cancel();
        }
    }

}
