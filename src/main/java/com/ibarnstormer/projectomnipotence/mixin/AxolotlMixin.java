package com.ibarnstormer.projectomnipotence.mixin;

import com.ibarnstormer.projectomnipotence.entity.HarmonicEntity;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.animal.axolotl.Axolotl;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Axolotl.class)
public class AxolotlMixin {

    @Unique
    private Axolotl getAxolotl() {
        return (Axolotl) (Object) this;
    }

    @Inject(method = "saveToBucketTag", at = @At("RETURN"))
    public void axolotl$saveToBucketTag(ItemStack p_149187_, CallbackInfo ci) {
        Axolotl entity = this.getAxolotl();
        CompoundTag nbt = p_149187_.getTag();
        if(nbt != null && entity instanceof HarmonicEntity harmonicEntity) nbt.putBoolean("is_enlightened", harmonicEntity.getHarmonicState());
    }

    //@Inject(method = "loadFromBucketTag", at = @At("RETURN"))
    public void axolotl$loadFromBucketTag(CompoundTag nbt, CallbackInfo ci) {
        Axolotl entity = this.getAxolotl();
        if(entity instanceof HarmonicEntity harmonicEntity) harmonicEntity.setHarmonicState(nbt.getBoolean("is_enlightened"));
    }

}
