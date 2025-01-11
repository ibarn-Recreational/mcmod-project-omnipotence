package com.ibarnstormer.projectomnipotence.mixin;

import com.ibarnstormer.projectomnipotence.entity.HarmonicEntity;
import com.ibarnstormer.projectomnipotence.utils.POUtils;
import net.minecraft.core.Holder;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(EnchantmentHelper.class)
public class EnchantmentHelperMixin {

    @Inject(method = "processEquipmentDropChance", at = @At("RETURN"), cancellable = true)
    private static void enchantmentHelper$getEquipmentDropChance(ServerLevel level, LivingEntity entity, DamageSource damageSource, float equipmentDropChance, CallbackInfoReturnable<Float> cir) {
        if(entity instanceof HarmonicEntity he && he.getHarmonicState() || (entity instanceof Player player && POUtils.isOmnipotent(player))) cir.setReturnValue(1.0F);
    }

    @Inject(method = "getEnchantmentLevel", at = @At("RETURN"), cancellable = true)
    private static void getItemEnchantLevel(Holder<Enchantment> enchantment, LivingEntity entity, CallbackInfoReturnable<Integer> cir) {
        if(entity instanceof Player player && POUtils.isOmnipotent(player)) {
            int eeLevel = POUtils.getLuckLevel(player);
            cir.setReturnValue(cir.getReturnValueI() + eeLevel);
        }
    }

}
