package com.ibarnstormer.projectomnipotence.mixin;

import com.ibarnstormer.projectomnipotence.utils.POUtils;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.EnchantmentEffectComponentTypes;
import net.minecraft.component.type.NbtComponent;
import net.minecraft.enchantment.Enchantment;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.damage.DamageSource;
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

    @Inject(method = "getLevel", at = @At("RETURN"), cancellable = true)
    private static void enchantmentHelper$getLevel(RegistryEntry<Enchantment> enchantment, ItemStack stack, CallbackInfoReturnable<Integer> cir) {
        if(enchantment.value().definition().supportedItems().stream().anyMatch(i -> i.isIn(ItemTags.MINING_LOOT_ENCHANTABLE)) || enchantment.value().effects().contains(EnchantmentEffectComponentTypes.FISHING_LUCK_BONUS) || enchantment.value().effects().contains(EnchantmentEffectComponentTypes.EQUIPMENT_DROPS)) {
            NbtComponent nbt = stack.getOrDefault(DataComponentTypes.CUSTOM_DATA, NbtComponent.DEFAULT);
            if (nbt.contains("eelevel")) {
                cir.setReturnValue(cir.getReturnValueI() + nbt.getNbt().getInt("eelevel"));
            }
        }
    }
}
