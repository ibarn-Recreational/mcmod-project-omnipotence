package com.ibarnstormer.projectomnipotence.mixin;

import com.ibarnstormer.projectomnipotence.capability.ModCapabilityProvider;
import com.ibarnstormer.projectomnipotence.utils.Utils;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentCategory;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.LootBonusEnchantment;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(EnchantmentHelper.class)
public class EnchantmentHelperMixin {

    @Inject(method = "getMobLooting", at = @At("RETURN"), cancellable = true)
    private static void getLootingLevel(LivingEntity p_44931_, CallbackInfoReturnable<Integer> cir) {
        if(p_44931_ instanceof Player player) {
            player.getCapability(ModCapabilityProvider.OMNIPOTENCE_CAPABILITY).ifPresent((cap) -> {
                if(cap.isOmnipotent()) {
                    cir.setReturnValue((int) (cir.getReturnValue() + Utils.getLuckLevel(player)));
                }
            });
        }
    }

    @Inject(method = "getItemEnchantmentLevel", at = @At("RETURN"), cancellable = true)
    private static void getItemEnchantLevel(Enchantment enchantment, ItemStack stack, CallbackInfoReturnable<Integer> cir) {
        if(enchantment instanceof LootBonusEnchantment && stack.getTag() != null && enchantment.category != EnchantmentCategory.WEAPON) {
            if (stack.getTag().contains("ee_level")) {
                cir.setReturnValue(cir.getReturnValueI() + stack.getTag().getInt("ee_level"));
            }
        }
    }

}
