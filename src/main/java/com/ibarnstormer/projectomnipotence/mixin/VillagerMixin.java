package com.ibarnstormer.projectomnipotence.mixin;

import com.ibarnstormer.projectomnipotence.utils.POUtils;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.npc.villager.Villager;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.trading.MerchantOffer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Villager.class)
public class VillagerMixin {

    @Unique
    private Villager getVillager() {
        return (Villager) (Object) this;
    }

    @Inject(method = "updateSpecialPrices", at = @At("HEAD"))
    public void villagerEntity$prepareOffersFor(Player player, CallbackInfo ci) {
        if(POUtils.isOmnipotent(player)) {
            Villager villager = this.getVillager();
            for (MerchantOffer tradeOffer : villager.getOffers()) {
                tradeOffer.addToSpecialPriceDiff((int) (-Mth.floor((float) 25 * POUtils.getLuckLevel(player)) + tradeOffer.getPriceMultiplier()));
            }
        }
    }

}
