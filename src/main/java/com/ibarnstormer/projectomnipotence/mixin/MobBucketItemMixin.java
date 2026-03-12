package com.ibarnstormer.projectomnipotence.mixin;

import com.ibarnstormer.projectomnipotence.utils.POUtils;
import com.llamalad7.mixinextras.sugar.Local;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.concurrent.atomic.AtomicBoolean;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.animal.Bucketable;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.MobBucketItem;
import net.minecraft.world.item.component.CustomData;

@Mixin(MobBucketItem.class)
public class MobBucketItemMixin {

    @Inject(method = "spawn", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/animal/Bucketable;loadFromBucketTag(Lnet/minecraft/nbt/CompoundTag;)V", shift = At.Shift.BEFORE))
    private void mobBucketItem$spawn(ServerLevel world, ItemStack stack, BlockPos pos, CallbackInfo ci, @Local Bucketable entity, @Local CustomData nbtComponent) {
        AtomicBoolean inHarmony = new AtomicBoolean(false);
        nbtComponent.update(nbt -> inHarmony.set(nbt.getBoolean("is_enlightened").orElse(false)));
        if(entity instanceof LivingEntity le) POUtils.setInHarmony(le, inHarmony.get());
    }

}
