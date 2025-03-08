package com.ibarnstormer.projectomnipotence.mixin.compat.ars_nouveau;

import com.hollingsworth.arsnouveau.common.spell.effect.EffectBreak;
import com.ibarnstormer.projectomnipotence.utils.POUtils;
import com.llamalad7.mixinextras.sugar.Local;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

@Mixin(value = EffectBreak.class, remap = false)
public class EffectBreakMixin {

    @ModifyVariable(method = "onResolveBlock", at = @At(value = "STORE"), ordinal = 1)
    private int effectBreak$onResolveBlock(int numFortune, @Local(argsOnly = true) LivingEntity shooter) {
        if(shooter instanceof Player player && POUtils.isOmnipotent(player)) {
            int eeLevel = POUtils.getLuckLevel(player);
            return numFortune + eeLevel;
        }

        return numFortune;
    }

}
