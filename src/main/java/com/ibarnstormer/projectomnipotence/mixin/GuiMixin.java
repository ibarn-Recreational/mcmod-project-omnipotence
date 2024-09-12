package com.ibarnstormer.projectomnipotence.mixin;

import com.ibarnstormer.projectomnipotence.Main;
import com.ibarnstormer.projectomnipotence.capability.ModCapabilityProvider;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Gui.class)
public class GuiMixin {

    @Inject(method = "renderHearts", at = @At("HEAD"), cancellable = true)
    private void gui$renderHearts(GuiGraphics p_282497_, Player player, int p_168691_, int p_168692_, int p_168693_, int p_168694_, float p_168695_, int p_168696_, int p_168697_, int p_168698_, boolean p_168699_, CallbackInfo ci) {
        player.getCapability(ModCapabilityProvider.OMNIPOTENCE_CAPABILITY).ifPresent(cap -> {
            if(cap.isOmnipotent() && Main.CONFIG.omnipotentPlayersCanBecomeInvulnerable && cap.getEnlightenedEntities() >= Main.CONFIG.invulnerabilityEntityGoal) {
                ci.cancel();
            }
        });
    }


}
