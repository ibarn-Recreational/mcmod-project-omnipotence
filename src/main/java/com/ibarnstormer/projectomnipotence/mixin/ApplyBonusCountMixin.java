package com.ibarnstormer.projectomnipotence.mixin;

import com.ibarnstormer.projectomnipotence.utils.POUtils;
import com.llamalad7.mixinextras.sugar.Local;
import net.minecraft.core.Holder;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.functions.ApplyBonusCount;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(ApplyBonusCount.class)
public class ApplyBonusCountMixin {

    @Redirect(method = "run", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/item/enchantment/EnchantmentHelper;getItemEnchantmentLevel(Lnet/minecraft/core/Holder;Lnet/minecraft/world/item/ItemStack;)I"))
    private int applyBonusCount$run(Holder<Enchantment> enchantment, ItemStack stack, @Local(argsOnly = true) LootContext context) {
        if(context.hasParam(LootContextParams.THIS_ENTITY)) {
            Entity entity = context.getParamOrNull(LootContextParams.THIS_ENTITY);
            if(entity instanceof Player player && POUtils.isOmnipotent(player)) {
                int eeLevel = POUtils.getLuckLevel(player);
                return EnchantmentHelper.getItemEnchantmentLevel(enchantment, stack) + eeLevel;
            }
        }
        return EnchantmentHelper.getItemEnchantmentLevel(enchantment, stack);
    }

}
