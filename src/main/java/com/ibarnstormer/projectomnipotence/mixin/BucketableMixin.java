package com.ibarnstormer.projectomnipotence.mixin;

import com.ibarnstormer.projectomnipotence.utils.POUtils;
import com.llamalad7.mixinextras.sugar.Local;
import net.minecraft.world.entity.Bucketable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.Optional;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;

@Mixin(Bucketable.class)
public interface BucketableMixin {


    @Inject(method = "bucketMobPickup", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/Bucketable;saveToBucketTag(Lnet/minecraft/world/item/ItemStack;)V"))
    private static <T extends LivingEntity> void bucketable$tryBucket(Player player, InteractionHand hand, T entity, CallbackInfoReturnable<Optional<InteractionResult>> cir, @Local(ordinal = 1) ItemStack itemStack2) {
        CustomData.update(DataComponents.BUCKET_ENTITY_DATA, itemStack2, nbt -> {
            nbt.putBoolean("is_enlightened", POUtils.isInHarmony(entity));
        });
    }

}
