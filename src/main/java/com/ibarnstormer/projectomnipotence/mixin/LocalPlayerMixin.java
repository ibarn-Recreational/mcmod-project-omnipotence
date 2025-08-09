package com.ibarnstormer.projectomnipotence.mixin;

import com.ibarnstormer.projectomnipotence.Main;
import com.ibarnstormer.projectomnipotence.capability.ModCapabilityProvider;
import com.ibarnstormer.projectomnipotence.utils.Utils;
import com.mojang.authlib.GameProfile;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.entity.player.ProfilePublicKey;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LocalPlayer.class)
public abstract class LocalPlayerMixin extends AbstractClientPlayer {

    public LocalPlayerMixin(ClientLevel p_234112_, GameProfile p_234113_, @Nullable ProfilePublicKey p_234114_) {
        super(p_234112_, p_234113_, p_234114_);
    }

    @Unique
    private LocalPlayer getLocalPlayer() {
        return (LocalPlayer) (Object) this;
    }

    @Inject(method = "tick", at = @At("HEAD"))
    public void localPlayer$tick(CallbackInfo ci) {
        LocalPlayer player = this.getLocalPlayer();

        player.getCapability(ModCapabilityProvider.OMNIPOTENCE_CAPABILITY).ifPresent(cap -> {

            if (player.tickCount % 5 == 0 && cap.isOmnipotent() && Main.CONFIG.omnipotentPlayerRenderParticlesClient && !Main.CONFIG.omnipotentPlayerParticlesLocal) {
                Utils.spawnEnlightenmentParticlesClient(player, clientLevel);
            }

        });

    }

}
