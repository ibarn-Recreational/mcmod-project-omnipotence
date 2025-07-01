package com.ibarnstormer.projectomnipotence.mixin.compat.ars_nouveau;

import com.hollingsworth.arsnouveau.api.spell.IDamageEffect;
import com.hollingsworth.arsnouveau.api.spell.SpellContext;
import com.hollingsworth.arsnouveau.api.spell.SpellResolver;
import com.hollingsworth.arsnouveau.api.spell.SpellStats;
import com.ibarnstormer.projectomnipotence.entity.HarmonicEntity;
import com.ibarnstormer.projectomnipotence.utils.POUtils;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(IDamageEffect.class)
public interface IDamageEffectMixin {

    @Inject(method = "attemptDamage", at = @At("RETURN"))
    default void iDamageEffect$attemptDamage(Level world, LivingEntity shooter, SpellStats stats, SpellContext spellContext, SpellResolver resolver, Entity entity, DamageSource source, float baseDamage, CallbackInfoReturnable<Boolean> cir) {
        if(!cir.getReturnValue() && entity instanceof HarmonicEntity harmonic && entity instanceof LivingEntity living && !harmonic.getHarmonicState() && shooter instanceof Player player && !(entity instanceof Player)) {
            POUtils.handleEnlightenment(living, player, source);
        }
    }



}
