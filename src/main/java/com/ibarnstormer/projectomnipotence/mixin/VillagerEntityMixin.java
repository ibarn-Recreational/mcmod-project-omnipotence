package com.ibarnstormer.projectomnipotence.mixin;


import com.ibarnstormer.projectomnipotence.utils.POUtils;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.trading.MerchantOffer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Villager.class)
public class VillagerEntityMixin {

    @Inject(method = "updateSpecialPrices", at = @At("HEAD"))
    public void decreasePrices(Player player, CallbackInfo ci) {
        if(POUtils.isOmnipotent(player)) {
            Villager villager = (Villager) (Object) this;
            for(MerchantOffer offer : villager.getOffers()) {
                offer.addToSpecialPriceDiff(-Mth.floor(25 * POUtils.getLuckLevel(player) + offer.getPriceMultiplier()));
            }
        }
    }

}
