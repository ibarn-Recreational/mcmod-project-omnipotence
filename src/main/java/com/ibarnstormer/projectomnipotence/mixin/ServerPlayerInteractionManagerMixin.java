package com.ibarnstormer.projectomnipotence.mixin;

import com.ibarnstormer.projectomnipotence.Main;
import com.ibarnstormer.projectomnipotence.utils.POUtils;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.registry.Registries;
import net.minecraft.registry.tag.BlockTags;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.network.ServerPlayerInteractionManager;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.Hand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.random.Random;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import org.spongepowered.asm.mixin.injection.callback.LocalCapture;

@Mixin(ServerPlayerInteractionManager.class)
public class ServerPlayerInteractionManagerMixin {

    @Shadow
    @Final
    protected ServerPlayerEntity player;

    @Shadow
    protected ServerWorld world;

    @Inject(method = "tryBreakBlock", at = @At(value = "INVOKE", target = "Lnet/minecraft/block/Block;onBreak(Lnet/minecraft/world/World;Lnet/minecraft/util/math/BlockPos;Lnet/minecraft/block/BlockState;Lnet/minecraft/entity/player/PlayerEntity;)Lnet/minecraft/block/BlockState;"), locals = LocalCapture.CAPTURE_FAILHARD, cancellable = true)
    public void serverPlayerInteractionManager$tryBreakBlock(BlockPos pos, CallbackInfoReturnable<Boolean> cir, BlockEntity blockEntity, Block block, BlockState blockState) {
        if(POUtils.isOmnipotent(player) && !player.getAbilities().creativeMode && !player.isSneaking() && Main.CONFIG.omnipotentPlayersDontGriefTrees) {
            BlockState bs = world.getBlockState(pos);
            if(Registries.BLOCK.getEntry(bs.getBlock()).isIn(BlockTags.LOGS) || Registries.BLOCK.getEntry(bs.getBlock()).isIn(BlockTags.LEAVES)) {
                Block.dropStacks(bs, world, BlockPos.ofFloored(player.getX(), player.getY(), player.getZ()), blockEntity, player, player.getStackInHand(Hand.MAIN_HAND));
                Random random = world.getRandom();
                for(int i = 0; i < 10; i++) {
                    double x = ((pos.getX() + 0.5) + ((double) random.nextBetween(60, 80) / 100) * random.nextBetween(-1, 1));
                    double y = ((pos.getY() + 0.5) + ((double) random.nextBetween(60, 80) / 100) * random.nextBetween(-1, 1));
                    double z = ((pos.getZ() + 0.5) + ((double) random.nextBetween(60, 80) / 100) * random.nextBetween(-1, 1));
                    this.world.spawnParticles(ParticleTypes.HAPPY_VILLAGER, x, y, z, 1, 0.1, 0.1, 0.1, 0);
                }
                cir.cancel();
            }
        }
    }


}
