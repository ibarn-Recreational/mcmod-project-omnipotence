package com.ibarnstormer.projectomnipotence.mixin;

import com.ibarnstormer.projectomnipotence.Main;
import com.ibarnstormer.projectomnipotence.capability.ModCapabilityProvider;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.level.ServerPlayerGameMode;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;


@Mixin(ServerPlayerGameMode.class)
public class ServerPlayerGameModeMixin {

    @Shadow
    @Final
    protected ServerPlayer player;

    @Shadow
    protected ServerLevel level;

    @Inject(method = "destroyBlock", at = @At("HEAD"), cancellable = true)
    public void serverPlayerGameMode$destroyBlock(BlockPos p_9281_, CallbackInfoReturnable<Boolean> cir) {
        player.getCapability(ModCapabilityProvider.OMNIPOTENCE_CAPABILITY).ifPresent((cap) -> {
            if(cap.isOmnipotent() && !player.getAbilities().instabuild && !player.isCrouching() && Main.CONFIG.omnipotentPlayersDontGriefTrees) {
                BlockState bs = level.getBlockState(p_9281_);
                if(bs.is(BlockTags.LOGS) || bs.is(BlockTags.LEAVES)) {
                    BlockEntity blockEntity = bs.hasBlockEntity() ? level.getBlockEntity(p_9281_) : null;
                    Block.dropResources(bs, level, BlockPos.containing(player.position()), blockEntity, player, player.getItemInHand(InteractionHand.MAIN_HAND));
                    RandomSource random = level.getRandom();
                    for(int i = 0; i < 10; i++) {
                        double x = ((p_9281_.getX() + 0.5) + ((double) random.nextIntBetweenInclusive(60, 80) / 100) * random.nextIntBetweenInclusive(-1, 1));
                        double y = ((p_9281_.getY() + 0.5) + ((double) random.nextIntBetweenInclusive(60, 80) / 100) * random.nextIntBetweenInclusive(-1, 1));
                        double z = ((p_9281_.getZ() + 0.5) + ((double) random.nextIntBetweenInclusive(60, 80) / 100) * random.nextIntBetweenInclusive(-1, 1));
                        level.sendParticles(ParticleTypes.HAPPY_VILLAGER, x, y, z, 1, 0.1, 0.1, 0.1, 0);
                    }
                    cir.cancel();
                }
            }
        });
    }

}
