package com.ibarnstormer.projectomnipotence.mixin;

import com.ibarnstormer.projectomnipotence.block.entity.EnlighteningBeacon;
import com.ibarnstormer.projectomnipotence.utils.POUtils;
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

    @Inject(method = "place(Lnet/minecraft/world/item/context/BlockPlaceContext;)Lnet/minecraft/world/InteractionResult;", at = @At("RETURN"))
    public void blockItem$place(BlockPlaceContext context, CallbackInfoReturnable<InteractionResult> cir) {
        Player player = context.getPlayer();
        if(POUtils.isOmnipotent(player) && context.getLevel().getBlockEntity(context.getClickedPos()) instanceof BeaconBlockEntity beacon) {
            ((EnlighteningBeacon) beacon).setAsEnlightening(player);
        }
    }

}
