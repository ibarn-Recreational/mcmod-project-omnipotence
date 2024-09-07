package com.ibarnstormer.projectomnipotence.mixin;

import com.ibarnstormer.projectomnipotence.block.entity.EnlighteningBeacon;
import com.ibarnstormer.projectomnipotence.utils.POUtils;
import net.minecraft.block.entity.BeaconBlockEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.BlockItem;
import net.minecraft.item.ItemPlacementContext;
import net.minecraft.util.ActionResult;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(BlockItem.class)
public class BlockItemMixin {

    @Inject(method = "place(Lnet/minecraft/item/ItemPlacementContext;)Lnet/minecraft/util/ActionResult;", at = @At("RETURN"))
    public void blockItem$place(ItemPlacementContext context, CallbackInfoReturnable<ActionResult> cir) {
        PlayerEntity player = context.getPlayer();
        if(POUtils.isOmnipotent(player) && context.getWorld().getBlockEntity(context.getBlockPos()) instanceof BeaconBlockEntity beacon) {
            ((EnlighteningBeacon) beacon).setAsEnlightening(player);
        }
    }

}
