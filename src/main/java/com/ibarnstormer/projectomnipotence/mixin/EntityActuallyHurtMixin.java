package com.ibarnstormer.projectomnipotence.mixin;

import com.ibarnstormer.projectomnipotence.Main;
import com.ibarnstormer.projectomnipotence.capability.ModCapabilityProvider;
import com.mega.uom.util.entity.EntityActuallyHurt;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = EntityActuallyHurt.class, remap = false)
public class EntityActuallyHurtMixin {

    @Shadow
    public LivingEntity entity;

    @Inject(method = "actuallyHurt(Lnet/minecraft/world/damagesource/DamageSource;FZ)V", at = @At("HEAD"), cancellable = true)
    public void entityActuallyHurt$actuallyHurt(DamageSource source, float amount, boolean special, CallbackInfo ci) {
        entity.getCapability(ModCapabilityProvider.OMNIPOTENCE_CAPABILITY).ifPresent((cap) -> {
            if(cap.isOmnipotent() && cap.getEnlightenedEntities() >= Main.CONFIG.invulnerabilityEntityGoal && Main.CONFIG.omnipotentPlayersCanBecomeInvulnerable) {
                ci.cancel();
            }
        });
    }


}
