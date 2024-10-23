package com.ibarnstormer.projectomnipotence.mixin;

import com.ibarnstormer.projectomnipotence.utils.POUtils;
import com.llamalad7.mixinextras.sugar.Local;
import net.minecraft.enchantment.Enchantment;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.loot.context.LootContext;
import net.minecraft.loot.context.LootContextParameters;
import net.minecraft.loot.function.ApplyBonusLootFunction;
import net.minecraft.registry.entry.RegistryEntry;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(ApplyBonusLootFunction.class)
public class ApplyBonusLootFunctionMixin {

    @Redirect(method = "process", at = @At(value = "INVOKE", target = "Lnet/minecraft/enchantment/EnchantmentHelper;getLevel(Lnet/minecraft/registry/entry/RegistryEntry;Lnet/minecraft/item/ItemStack;)I"))
    private int applyBonusLootFunction$process(RegistryEntry<Enchantment> enchantment, ItemStack stack, @Local(argsOnly = true) LootContext context) {
        if(context.hasParameter(LootContextParameters.THIS_ENTITY)) {
            Entity entity = context.get(LootContextParameters.THIS_ENTITY);
            if(entity instanceof PlayerEntity player && POUtils.isOmnipotent(player)) {
                int eeLevel = POUtils.getLuckLevel(player);
                return EnchantmentHelper.getLevel(enchantment, stack) + eeLevel;
            }
        }
        return EnchantmentHelper.getLevel(enchantment, stack);
    }

}
