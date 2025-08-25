package com.ibarnstormer.projectomnipotence.mixin.compat;

import com.github.alexthe668.domesticationinnovation.server.entity.ModifedToBeTameable;
import com.github.alexthe668.domesticationinnovation.server.entity.TameableUtils;
import com.ibarnstormer.projectomnipotence.utils.Utils;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value = TameableUtils.class, remap = false)
public class TameableUtilsMixin {

    @Unique
    private static boolean hasSameOwnerAsOneWay(Entity tameable, Entity target) {
        try {
            TamableAnimal tamed = (TamableAnimal) tameable;
            ModifedToBeTameable otherPet;
            if (tameable instanceof TamableAnimal) {
                if (tamed.getOwner() != null) {
                    if (target instanceof ModifedToBeTameable) {
                        otherPet = (ModifedToBeTameable) target;
                        if (otherPet.getTameOwner() != null && tamed.getOwner().equals(otherPet.getTameOwner())) {
                            return true;
                        }
                    }

                    if (target instanceof TamableAnimal) {
                        tamed = (TamableAnimal) target;
                        if (tamed.getOwner() != null && tamed.getOwner().equals(tamed.getOwner())) {
                            return true;
                        }
                    }

                    return tamed.getOwner().equals(target);
                }
            }

            if (tameable instanceof ModifedToBeTameable axolotl) {
                if (axolotl.getTameOwner() != null) {
                    if (tamed.getOwner() != null && tamed.getOwner().equals(axolotl.getTameOwner())) {
                        return true;
                    }

                    if (target instanceof ModifedToBeTameable) {
                        otherPet = (ModifedToBeTameable) target;
                        if (otherPet.getTameOwner() != null && axolotl.getTameOwner().equals(otherPet.getTameOwner())) {
                            return true;
                        }
                    }

                    return axolotl.getTameOwner().equals(target);
                }
            }

            return false;
        }
        catch (Exception ignored) {
            return false;
        }
    }

    @Inject(method = "isPetOf", at = @At("RETURN"), cancellable = true)
    private static void tameableUtils$isPetOf(Player player, Entity entity, CallbackInfoReturnable<Boolean> cir) {
        boolean flag = Utils.isEnlightenedOrOmnipotent(player) || Utils.isEnlightenedOrOmnipotent(entity);
        cir.setReturnValue(entity != null && ((!flag && entity.isAlliedTo(player)) || hasSameOwnerAsOneWay(entity, player)));
    }

}
