package com.ibarnstormer.projectomnipotence.mixin.compat;

import com.github.alexthe668.domesticationinnovation.server.entity.TameableUtils;
import com.ibarnstormer.projectomnipotence.utils.Utils;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(value = TameableUtils.class, remap = false)
public class TameableUtilsMixin {

    @Redirect(method = "isPetOf", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/Entity;isAlliedTo(Lnet/minecraft/world/entity/Entity;)Z"))
    private static boolean tameableUtils$isPetOf(Entity thisEntity, Entity p_20355_) {
        if(Utils.isEnlightenedOrOmnipotent(thisEntity) || Utils.isEnlightenedOrOmnipotent(p_20355_)) return false;
        else return thisEntity.isAlliedTo(p_20355_);

    }

}