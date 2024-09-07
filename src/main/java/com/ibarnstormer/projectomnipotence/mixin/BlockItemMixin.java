package com.ibarnstormer.projectomnipotence.mixin;

import com.ibarnstormer.projectomnipotence.block.entity.EnlighteningBeacon;
import com.ibarnstormer.projectomnipotence.capability.ModCapabilityProvider;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.block.entity.BeaconBlockEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(BlockItem.class)
public class BlockItemMixin {

    @Inject(method = "place", at = @At("RETURN"))
    public void blockItem$place(BlockPlaceContext p_40577_, CallbackInfoReturnable<InteractionResult> cir) {
        Player player = p_40577_.getPlayer();
        if(player != null) {
            player.getCapability(ModCapabilityProvider.OMNIPOTENCE_CAPABILITY).ifPresent((cap) -> {
                if (cap.isOmnipotent() && p_40577_.getLevel().getBlockEntity(p_40577_.getClickedPos()) instanceof BeaconBlockEntity beacon){
                    ((EnlighteningBeacon) beacon).setAsEnlightening(player);
                }
            });
        }
    }

}
