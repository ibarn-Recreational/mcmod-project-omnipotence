package com.ibarnstormer.projectomnipotence.mixin;

import com.ibarnstormer.projectomnipotence.capability.ModCapabilityProvider;
import com.ibarnstormer.projectomnipotence.entity.HarmonicEntity;
import com.ibarnstormer.projectomnipotence.utils.Utils;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Mob.class)
public class MobMixin<T extends Mob> {

    @Shadow
    private LivingEntity target;

    @Inject(method = "setTarget", at = @At("TAIL"))
    public void mob$setTarget(LivingEntity p_21544_, CallbackInfo ci) {
        if(p_21544_ == this.target) {
            if (p_21544_ instanceof Player player) {
                player.getCapability(ModCapabilityProvider.OMNIPOTENCE_CAPABILITY).ifPresent((cap) -> {
                   if(cap.isOmnipotent()) this.target = null;
                });
            } else if (p_21544_ instanceof HarmonicEntity harmonicEntity && harmonicEntity.getHarmonicState()) this.target = null;
        }
    }

    @Inject(method = "convertTo", at = @At("RETURN"), cancellable = true)
    public void mod$convertTo(EntityType<T> p_21407_, boolean p_21408_, CallbackInfoReturnable<T> cir) {
        Mob thisMob = (Mob) (Object) this;
        if(thisMob instanceof HarmonicEntity harmonicEntity && harmonicEntity.getHarmonicState()) {
            T converionResult = cir.getReturnValue();
            if(converionResult instanceof HarmonicEntity h) {
                h.setHarmonicState(true);
                cir.setReturnValue(converionResult);
            }
        }
    }

}
