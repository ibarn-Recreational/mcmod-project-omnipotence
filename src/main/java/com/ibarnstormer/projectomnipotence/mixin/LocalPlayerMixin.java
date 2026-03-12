package com.ibarnstormer.projectomnipotence.mixin;

import com.ibarnstormer.projectomnipotence.Main;
import com.ibarnstormer.projectomnipotence.utils.POUtils;
import com.mojang.authlib.GameProfile;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.player.LocalPlayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LocalPlayer.class)
public abstract class LocalPlayerMixin extends AbstractClientPlayer {

    @Unique
    private LocalPlayer getClientPlayer() {
        return (LocalPlayer) (Object) this;
    }

    public LocalPlayerMixin(ClientLevel world, GameProfile profile) {
        super(world, profile);
    }

    @Inject(method = "tick", at = @At("HEAD"))
    public void clientPlayerEntity$tick(CallbackInfo ci) {
        LocalPlayer player = this.getClientPlayer();
        if (player.tickCount % 5 == 0 && POUtils.isOmnipotent(player) && Main.CONFIG.omnipotentPlayerRenderParticlesClient && !Main.CONFIG.omnipotentPlayerParticlesLocal) {
            POUtils.spawnEnlightenmentParticlesClient(player, (ClientLevel) player.level());
        }
    }

}
