package com.ibarnstormer.projectomnipotence.mixin;


import com.ibarnstormer.projectomnipotence.Main;
import com.ibarnstormer.projectomnipotence.utils.POUtils;
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
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.List;

@Mixin(ServerPlayer.class)
public abstract class ServerPlayerMixin extends Player {

    public ServerPlayerMixin(Level p_250508_, BlockPos p_250289_, float p_251702_, GameProfile p_252153_) {
        super(p_250508_, p_250289_, p_251702_, p_252153_);
    }

    @Unique
    private ServerPlayer getServerPlayer() {
        return (ServerPlayer) (Object) this;
    }

    @Inject(method = "startSleepInBed", at = @At("RETURN"), cancellable = true)
    public void serverPlayer$startSleepInBed(BlockPos p_9115_, CallbackInfoReturnable<Either<Player.BedSleepingProblem, Unit>> cir) {
        cir.getReturnValue().ifLeft((reason) -> {
            if(reason == Player.BedSleepingProblem.NOT_SAFE) {
                ServerPlayer player = this.getServerPlayer();
                if(POUtils.isOmnipotent(player)) {
                    cir.setReturnValue(super.startSleepInBed(p_9115_).ifRight((unit) -> {
                        player.awardStat(Stats.SLEEP_IN_BED);
                        CriteriaTriggers.SLEPT_IN_BED.trigger(player);
                    }));

                    if (!player.serverLevel().canSleepThroughNights()) {
                        player.displayClientMessage(Component.translatable("sleep.not_possible"), true);
                    }

                    if (player.level() instanceof ServerLevel server) server.updateSleepingPlayerList();
                }
            }
        });
    }

    @Inject(method = "isCreative", at = @At("RETURN"), cancellable = true)
    public void serverPlayer$isCreative(CallbackInfoReturnable<Boolean> cir) {
        ServerPlayer player = this.getServerPlayer();
        if(POUtils.isOmnipotent(player) && Main.CONFIG.carryOnCompat) {
            // Carry-on compat
            StackTraceElement[] stackTrace = Thread.currentThread().getStackTrace();
            for (StackTraceElement e : stackTrace) {
                if(e.getClassName().contains("tschipp.carryon.common.carry")) cir.setReturnValue(true);
            }
        }

        if(POUtils.isOmnipotent(player) && Main.CONFIG.omnipotentPlayersCanBecomeInvulnerable && POUtils.getEnlightenedEntities(player) >= Main.CONFIG.invulnerabilityEntityGoal && !cir.getReturnValue()) {

            // Just assume that we are in creative if check gets called from FE (prevents UOM's final explosion from killing invulnerable omnipotents and timestop)
            StackTraceElement[] stackTrace = Thread.currentThread().getStackTrace();
            for (StackTraceElement e : stackTrace) {
                if(e.getClassName().contains("com.mega.uom")) cir.setReturnValue(true);
            }
        }
        if(POUtils.isOmnipotent(player) && Main.CONFIG.omnipotentPlayersCanGainFlight && POUtils.getEnlightenedEntities(player) >= Main.CONFIG.flightEntityGoal && !cir.getReturnValue()) {

            // Prevent the Apostle from Goety from not allowing us to fly
            StackTraceElement[] stackTrace = Thread.currentThread().getStackTrace();
            for (StackTraceElement e : stackTrace) {
                List<String> splitClass = List.of(e.getClassName().toLowerCase().split("\\."));
                if(splitClass.contains("com") && splitClass.contains("polarice3") && splitClass.contains("goety") && splitClass.contains("boss")) {
                    cir.setReturnValue(true);
                }
            }
        }
    }
}
