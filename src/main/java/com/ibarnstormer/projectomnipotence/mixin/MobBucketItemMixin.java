package com.ibarnstormer.projectomnipotence.mixin;

import com.ibarnstormer.projectomnipotence.entity.HarmonicEntity;
import com.llamalad7.mixinextras.sugar.Local;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.animal.Bucketable;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.MobBucketItem;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(MobBucketItem.class)
public class MobBucketItemMixin {

    @Inject(method = "spawn", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/animal/Bucketable;loadFromBucketTag(Lnet/minecraft/nbt/CompoundTag;)V", shift = At.Shift.BEFORE))
    private void mobBucketItem$spawn(ServerLevel p_151142_, ItemStack p_151143_, BlockPos p_151144_, CallbackInfo ci, @Local Bucketable entity) {
        CompoundTag nbt = p_151143_.getTag();
        if(nbt != null && entity instanceof HarmonicEntity harmonicEntity) harmonicEntity.setHarmonicState(nbt.getBoolean("is_enlightened"));
    }

}
