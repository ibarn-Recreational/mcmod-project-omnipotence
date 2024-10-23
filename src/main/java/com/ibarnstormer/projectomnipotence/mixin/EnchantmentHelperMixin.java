package com.ibarnstormer.projectomnipotence.mixin;

import com.ibarnstormer.projectomnipotence.utils.POUtils;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.EnchantmentEffectComponentTypes;
import net.minecraft.component.type.NbtComponent;
import net.minecraft.enchantment.Enchantment;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.registry.tag.ItemTags;
import net.minecraft.server.world.ServerWorld;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(EnchantmentHelper.class)
public class EnchantmentHelperMixin {

    @Inject(method = "getEquipmentDropChance", at = @At("RETURN"), cancellable = true)
    private static void enchantmentHelper$getEquipmentDropChance(ServerWorld world, LivingEntity attacker, DamageSource damageSource, float baseEquipmentDropChance, CallbackInfoReturnable<Float> cir) {
        if(POUtils.isInHarmony(attacker)) cir.setReturnValue(1.0F);
    }

    @Inject(method = "getEquipmentLevel", at = @At("RETURN"), cancellable = true)
    private static void enchantmentHelper$getEquipmentLevel(RegistryEntry<Enchantment> enchantment, LivingEntity entity, CallbackInfoReturnable<Integer> cir) {
        if(entity instanceof PlayerEntity player && POUtils.isOmnipotent(player)) {
            int eeLevel = POUtils.getLuckLevel(player);
            cir.setReturnValue(cir.getReturnValueI() + eeLevel);
        }
    }
}
