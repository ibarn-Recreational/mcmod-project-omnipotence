package com.ibarnstormer.projectomnipotence.mixin;

import com.ibarnstormer.projectomnipotence.Main;
import com.ibarnstormer.projectomnipotence.network.payload.SyncSSDHDataPayload;
import com.ibarnstormer.projectomnipotence.utils.POUtils;
import com.mojang.authlib.GameProfile;
import com.mojang.datafixers.util.Either;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.advancement.criterion.Criteria;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.stat.Stats;
import net.minecraft.text.Text;
import net.minecraft.util.Unit;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.List;

@Mixin(ServerPlayerEntity.class)
public abstract class ServerPlayerEntityMixin extends PlayerEntity {

    public ServerPlayerEntityMixin(World world, BlockPos pos, float yaw, GameProfile gameProfile) {
        super(world, pos, yaw, gameProfile);
    }

    @Inject(method = "copyFrom", at = @At("TAIL"))
    public void serverPlayerEntity$copyFrom(ServerPlayerEntity oldPlayer, boolean alive, CallbackInfo ci) {
        ServerPlayerEntity newPlayer = (ServerPlayerEntity) (Object) this;
        if(POUtils.isOmnipotent(oldPlayer)) POUtils.grantOmnipotence(newPlayer, true);
        POUtils.setEntitiesEnlightened(newPlayer, POUtils.getEntitiesEnlightened(oldPlayer));
    }

    @Inject(method = "trySleep", at = @At("RETURN"), cancellable = true)
    public void serverPlayerEntity$trySleep(BlockPos pos, CallbackInfoReturnable<Either<PlayerEntity.SleepFailureReason, Unit>> cir) {
        cir.getReturnValue().ifLeft((reason) -> {
           if(reason == PlayerEntity.SleepFailureReason.NOT_SAFE) {
                ServerPlayerEntity player = (ServerPlayerEntity) (Object) this;
                if(POUtils.isOmnipotent(player)) {
                    cir.setReturnValue(super.trySleep(pos).ifRight((unit) -> {
                        player.incrementStat(Stats.SLEEP_IN_BED);
                        Criteria.SLEPT_IN_BED.trigger(player);
                    }));

                    if (!player.getServerWorld().isSleepingEnabled()) {
                        player.sendMessage(Text.translatable("sleep.not_possible"), true);
                    }

                    if(player.getWorld() instanceof ServerWorld server) server.updateSleepingPlayers();
                }
           }
        });
    }

    @Inject(method = "isCreative", at = @At("RETURN"), cancellable = true)
    public void serverPlayerEntity$isCreative(CallbackInfoReturnable<Boolean> cir) {
        ServerPlayerEntity player = (ServerPlayerEntity) (Object) this;
        if(POUtils.isOmnipotent(player) && Main.CONFIG.carryOnCompat) {
            // Carry-on compat
            StackTraceElement[] stackTrace = Thread.currentThread().getStackTrace();
            for (StackTraceElement e : stackTrace) {
                if(e.getClassName().contains("tschipp.carryon.common.carry")) cir.setReturnValue(true);
            }
        }

        if(POUtils.isOmnipotent(player) && Main.CONFIG.omnipotentPlayersCanBecomeInvulnerable && POUtils.getEntitiesEnlightened(player) >= Main.CONFIG.invulnerabilityEntityGoal && !cir.getReturnValue()) {

            // Just assume that we are in creative if check gets called from FE (prevents UOM's final explosion from killing invulnerable omnipotents and timestop)
            StackTraceElement[] stackTrace = Thread.currentThread().getStackTrace();
            for (StackTraceElement e : stackTrace) {
                if(e.getClassName().contains("com.mega.uom")) cir.setReturnValue(true);
            }
        }
        if(POUtils.isOmnipotent(player) && Main.CONFIG.omnipotentPlayersCanGainFlight && POUtils.getEntitiesEnlightened(player) >= Main.CONFIG.flightEntityGoal && !cir.getReturnValue()) {

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
