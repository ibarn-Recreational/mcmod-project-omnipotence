package com.ibarnstormer.projectomnipotence.mixin;

import com.ibarnstormer.projectomnipotence.Main;
import com.ibarnstormer.projectomnipotence.utils.POUtils;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.level.ServerPlayerGameMode;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import org.spongepowered.asm.mixin.injection.callback.LocalCapture;

@Mixin(ServerPlayerGameMode.class)
public class ServerPlayerInteractionManagerMixin {

    @Shadow
    @Final
    protected ServerPlayer player;

    @Shadow
    protected ServerLevel level;

    @Inject(method = "destroyBlock", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/block/Block;playerWillDestroy(Lnet/minecraft/world/level/Level;Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/state/BlockState;Lnet/minecraft/world/entity/player/Player;)Lnet/minecraft/world/level/block/state/BlockState;"), locals = LocalCapture.CAPTURE_FAILHARD, cancellable = true)
    public void serverPlayerInteractionManager$tryBreakBlock(BlockPos pos, CallbackInfoReturnable<Boolean> cir, BlockEntity blockEntity, Block block, BlockState blockState) {
        if(POUtils.isOmnipotent(player) && !player.getAbilities().instabuild && !player.isShiftKeyDown() && Main.CONFIG.omnipotentPlayersDontGriefTrees) {
            BlockState bs = level.getBlockState(pos);
            if(BuiltInRegistries.BLOCK.wrapAsHolder(bs.getBlock()).is(BlockTags.LOGS) || BuiltInRegistries.BLOCK.wrapAsHolder(bs.getBlock()).is(BlockTags.LEAVES)) {
                Block.dropResources(bs, level, BlockPos.containing(player.getX(), player.getY(), player.getZ()), blockEntity, player, player.getItemInHand(InteractionHand.MAIN_HAND));
                RandomSource random = level.getRandom();
                for(int i = 0; i < 10; i++) {
                    double x = ((pos.getX() + 0.5) + ((double) random.nextIntBetweenInclusive(60, 80) / 100) * random.nextIntBetweenInclusive(-1, 1));
                    double y = ((pos.getY() + 0.5) + ((double) random.nextIntBetweenInclusive(60, 80) / 100) * random.nextIntBetweenInclusive(-1, 1));
                    double z = ((pos.getZ() + 0.5) + ((double) random.nextIntBetweenInclusive(60, 80) / 100) * random.nextIntBetweenInclusive(-1, 1));
                    this.level.sendParticles(ParticleTypes.HAPPY_VILLAGER, x, y, z, 1, 0.1, 0.1, 0.1, 0);
                }
                cir.cancel();
            }
        }
    }


}
