package com.ibarnstormer.projectomnipotence.mixin;

import com.ibarnstormer.projectomnipotence.utils.POUtils;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import net.minecraft.core.Holder;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemInstance;
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

    @WrapOperation(method = "run", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/item/enchantment/EnchantmentHelper;getItemEnchantmentLevel(Lnet/minecraft/core/Holder;Lnet/minecraft/world/item/ItemInstance;)I"))
    private int applyBonusLootFunction$process(Holder<Enchantment> enchantment, ItemInstance piece, Operation<Integer> original, @Local(argsOnly = true) LootContext context) {
        if(context.hasParameter(LootContextParams.THIS_ENTITY)) {
            Entity entity = context.getOptionalParameter(LootContextParams.THIS_ENTITY);
            if(entity instanceof Player player && POUtils.isOmnipotent(player)) {
                int eeLevel = POUtils.getLuckLevel(player);
                return original.call(enchantment, piece) + eeLevel;
            }
        }
        return original.call(enchantment, piece);
    }

}
