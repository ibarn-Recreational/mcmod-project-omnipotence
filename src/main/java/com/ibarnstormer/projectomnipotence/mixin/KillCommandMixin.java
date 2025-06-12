package com.ibarnstormer.projectomnipotence.mixin;

import com.ibarnstormer.projectomnipotence.Main;
import com.ibarnstormer.projectomnipotence.utils.POUtils;
import com.llamalad7.mixinextras.sugar.Local;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
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


    @Inject(method = "execute", at = @At(value = "INVOKE", target = "Lnet/minecraft/entity/Entity;kill(Lnet/minecraft/server/world/ServerWorld;)V"), cancellable = true)
    private static void killCommand$execute(ServerCommandSource source, Collection<? extends Entity> targets, CallbackInfoReturnable<Integer> cir, @Local Entity entity) {
        if(entity instanceof PlayerEntity player && POUtils.isOmnipotent(player) && entity.isAlive() && POUtils.getEntitiesEnlightened(player) >= Main.CONFIG.invulnerabilityEntityGoal && Main.CONFIG.omnipotentPlayersCanBecomeInvulnerable) {
            cir.cancel();
        }
    }

}
