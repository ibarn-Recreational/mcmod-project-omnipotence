package com.ibarnstormer.projectomnipotence.mixin;

import com.ibarnstormer.projectomnipotence.utils.POUtils;
import net.minecraft.entity.passive.VillagerEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.math.MathHelper;
import net.minecraft.village.TradeOffer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(VillagerEntity.class)
public class VillagerEntityMixin {

    @Inject(method = "prepareOffersFor", at = @At("HEAD"))
    public void villagerEntity$prepareOffersFor(PlayerEntity player, CallbackInfo ci) {
        if(POUtils.isOmnipotent(player)) {
            VillagerEntity villager = (VillagerEntity) (Object) this;
            for (TradeOffer tradeOffer : villager.getOffers()) {
                tradeOffer.increaseSpecialPrice((int) (-MathHelper.floor((float) 25 * POUtils.getLuckLevel(player)) + tradeOffer.getPriceMultiplier()));
            }
        }
    }

}
