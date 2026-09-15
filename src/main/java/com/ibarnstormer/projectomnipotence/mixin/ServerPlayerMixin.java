package com.ibarnstormer.projectomnipotence.mixin;

import com.ibarnstormer.projectomnipotence.utils.POUtils;
import com.mojang.authlib.GameProfile;
import com.mojang.datafixers.util.Either;
import net.minecraft.advancements.triggers.CriteriaTriggers;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.stats.Stats;
import net.minecraft.util.Unit;
import net.minecraft.world.attribute.BedRule;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.AbstractBedBlock;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ServerPlayer.class)
public abstract class ServerPlayerMixin extends Player {

    @Unique
    private ServerPlayer getServerPlayer() {
        return (ServerPlayer) (Object) this;
    }

    public ServerPlayerMixin(Level world, GameProfile gameProfile) {
        super(world, gameProfile);
    }

    @Inject(method = "restoreFrom", at = @At("TAIL"))
    public void serverPlayerEntity$copyFrom(ServerPlayer oldPlayer, boolean alive, CallbackInfo ci) {
        ServerPlayer newPlayer = this.getServerPlayer();
        if(POUtils.isOmnipotent(oldPlayer)) POUtils.grantOmnipotence(newPlayer, true);
        POUtils.setEntitiesEnlightened(newPlayer, POUtils.getEntitiesEnlightened(oldPlayer));
    }

    @Inject(method = "startSleepInBed", at = @At("RETURN"), cancellable = true)
    public void serverPlayerEntity$trySleep(AbstractBedBlock bedBlock, BlockState bedBlockState, BedRule rule, BlockPos pos, CallbackInfoReturnable<Either<BedSleepingProblem, Unit>> cir) {
        cir.getReturnValue().ifLeft((reason) -> {
           if(reason == Player.BedSleepingProblem.NOT_SAFE) {
                ServerPlayer player = this.getServerPlayer();
                if(POUtils.isOmnipotent(player)) {
                    cir.setReturnValue(super.startSleepInBed(bedBlock, bedBlockState, rule, pos).ifRight((unit) -> {
                        player.awardStat(Stats.SLEEP_IN_BED);
                        CriteriaTriggers.SLEPT_IN_BED.trigger(player);
                    }));

                    if (!player.level().canSleepThroughNights()) {
                        player.sendOverlayMessage(Component.translatable("sleep.not_possible"));
                    }

                    if(player.level() instanceof ServerLevel server) server.updateSleepingPlayerList();
                }
           }
        });
    }

}
