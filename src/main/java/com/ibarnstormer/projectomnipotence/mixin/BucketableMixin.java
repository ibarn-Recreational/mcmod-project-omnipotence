package com.ibarnstormer.projectomnipotence.mixin;

import com.ibarnstormer.projectomnipotence.utils.POUtils;
import com.llamalad7.mixinextras.sugar.Local;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.NbtComponent;
import net.minecraft.entity.Bucketable;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.Optional;

@Mixin(Bucketable.class)
public interface BucketableMixin {


    @Inject(method = "tryBucket", at = @At(value = "INVOKE", target = "Lnet/minecraft/entity/Bucketable;copyDataToStack(Lnet/minecraft/item/ItemStack;)V"))
    private static <T extends LivingEntity> void bucketable$tryBucket(PlayerEntity player, Hand hand, T entity, CallbackInfoReturnable<Optional<ActionResult>> cir, @Local(ordinal = 1) ItemStack itemStack2) {
        NbtComponent.set(DataComponentTypes.BUCKET_ENTITY_DATA, itemStack2, nbt -> {
            nbt.putBoolean("is_enlightened", POUtils.isInHarmony(entity));
        });
    }

}
