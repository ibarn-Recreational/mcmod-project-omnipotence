package com.ibarnstormer.projectomnipotence.mixin;

import com.ibarnstormer.projectomnipotence.Main;
import com.ibarnstormer.projectomnipotence.utils.POUtils;
import net.minecraft.client.player.AbstractClientPlayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.List;

@Mixin(AbstractClientPlayer.class)
public class AbstractClientPlayerMixin {

    @Unique
    private AbstractClientPlayer getClientPlayer() {
        return (AbstractClientPlayer) (Object) this;
    }

    @Inject(method = "isCreative", at = @At("RETURN"), cancellable = true)
    public void abstractClientPlayer$isCreative(CallbackInfoReturnable<Boolean> cir) {
        AbstractClientPlayer player = this.getClientPlayer();
        if(POUtils.isOmnipotent(player) && Main.CONFIG.carryOnCompat) {
            // Carry-on compat
            StackTraceElement[] stackTrace = Thread.currentThread().getStackTrace();
            for (StackTraceElement e : stackTrace) {
                if(e.getClassName().contains("tschipp.carryon.common.carry")) cir.setReturnValue(true);
            }
        }

        if(POUtils.isOmnipotent(player) && Main.CONFIG.omnipotentPlayersCanBecomeInvulnerable && POUtils.getEnlightenedEntities(player) >= Main.CONFIG.invulnerabilityEntityGoal && !cir.getReturnValue()) {

            // Just assume that we are in creative if check gets called from FE (prevents UOM's final explosion from killing invulnerable omnipotents and timestop)
            StackTraceElement[] stackTrace = Thread.currentThread().getStackTrace();
            for (StackTraceElement e : stackTrace) {
                if(e.getClassName().contains("com.mega.uom")) cir.setReturnValue(true);
            }
        }
        if(POUtils.isOmnipotent(player) && Main.CONFIG.omnipotentPlayersCanGainFlight && POUtils.getEnlightenedEntities(player) >= Main.CONFIG.flightEntityGoal && !cir.getReturnValue()) {

            // Prevent the Apostle from Goety from not allowing us to fly
            StackTraceElement[] stackTrace = Thread.currentThread().getStackTrace();
            for (StackTraceElement e : stackTrace) {
                List<String> splitClass = List.of(e.getClassName().toLowerCase().split("\\."));
                if(splitClass.contains("com") && splitClass.contains("polarice3") && splitClass.contains("goety") && splitClass.contains("boss")) {
                    cir.setReturnValue(true);
                }
            }
        }
    }

}
