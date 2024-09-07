package com.ibarnstormer.projectomnipotence.mixin;

import com.ibarnstormer.projectomnipotence.capability.ModCapabilityProvider;
import com.mojang.authlib.GameProfile;
import com.mojang.datafixers.util.Either;
import net.minecraft.advancements.CriteriaTriggers;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.stats.Stats;
import net.minecraft.util.Unit;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ServerPlayer.class)
public abstract class ServerPlayerMixin extends Player {

    public ServerPlayerMixin(Level p_250508_, BlockPos p_250289_, float p_251702_, GameProfile p_252153_) {
        super(p_250508_, p_250289_, p_251702_, p_252153_);
    }

    @Inject(method = "startSleepInBed", at = @At("RETURN"), cancellable = true)
    public void serverPlayer$startSleepInBed(BlockPos p_9115_, CallbackInfoReturnable<Either<Player.BedSleepingProblem, Unit>> cir) {
        cir.getReturnValue().ifLeft((reason) -> {
            if(reason == Player.BedSleepingProblem.NOT_SAFE) {
                ServerPlayer player = (ServerPlayer) (Object) this;
                player.getCapability(ModCapabilityProvider.OMNIPOTENCE_CAPABILITY).ifPresent((cap) -> {
                    if(cap.isOmnipotent()) {
                        cir.setReturnValue(super.startSleepInBed(p_9115_).ifRight((unit) -> {
                            player.awardStat(Stats.SLEEP_IN_BED);
                            CriteriaTriggers.SLEPT_IN_BED.trigger(player);
                        }));

                        if (!player.serverLevel().canSleepThroughNights()) {
                            player.displayClientMessage(Component.translatable("sleep.not_possible"), true);
                        }

                        if (player.level() instanceof ServerLevel server) server.updateSleepingPlayerList();
                    }
                });
            }
        });
    }
}
