package com.ibarnstormer.projectomnipotence.mixin;

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

}
