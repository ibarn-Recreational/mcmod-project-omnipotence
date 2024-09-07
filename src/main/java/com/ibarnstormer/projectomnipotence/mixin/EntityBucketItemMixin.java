package com.ibarnstormer.projectomnipotence.mixin;

import com.ibarnstormer.projectomnipotence.utils.POUtils;
import com.llamalad7.mixinextras.sugar.Local;
import net.minecraft.component.type.NbtComponent;
import net.minecraft.entity.Bucketable;
import net.minecraft.entity.LivingEntity;
import net.minecraft.item.EntityBucketItem;
import net.minecraft.item.ItemStack;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.concurrent.atomic.AtomicBoolean;

@Mixin(EntityBucketItem.class)
public class EntityBucketItemMixin {

    @Inject(method = "spawnEntity", at = @At(value = "INVOKE", target = "Lnet/minecraft/entity/Bucketable;copyDataFromNbt(Lnet/minecraft/nbt/NbtCompound;)V", shift = At.Shift.BEFORE))
    private void entityBucketItem$spawnEntity(ServerWorld world, ItemStack stack, BlockPos pos, CallbackInfo ci, @Local Bucketable entity, @Local NbtComponent nbtComponent) {
        AtomicBoolean inHarmony = new AtomicBoolean(false);
        nbtComponent.apply(nbt -> inHarmony.set(nbt.getBoolean("is_enlightened")));
        if(entity instanceof LivingEntity le) POUtils.setInHarmony(le, inHarmony.get());
    }

}
