package com.ibarnstormer.projectomnipotence.mixin;

import com.ibarnstormer.projectomnipotence.block.entity.EnlighteningBeacon;


import com.ibarnstormer.projectomnipotence.entity.HarmonicEntity;
import com.ibarnstormer.projectomnipotence.utils.POUtils;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BeaconBlockEntity;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.List;
import java.util.UUID;

@Mixin(BeaconBlockEntity.class)
public abstract class BeaconBlockEntityMixin extends BlockEntity implements EnlighteningBeacon {

    @Unique
    private boolean isEnlightening;
    @Unique
    private UUID omnipotentOwner;
    @Unique
    private int cachedEnlightenedAmount;

    public BeaconBlockEntityMixin(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
    }

    @Inject(method = "loadAdditional", at = @At("TAIL"))
    private void beaconBlockEntity$load(CompoundTag tag, HolderLookup.Provider registries, CallbackInfo ci) {
        try {
            this.isEnlightening = tag.getBoolean("isEnlightening");
            this.omnipotentOwner = tag.getUUID("omnipotentOwnerUUID");
            this.cachedEnlightenedAmount = tag.getInt("cachedEnlightenedAmount");
        }
        catch(Exception ignored) {}
    }

    @Inject(method = "saveAdditional", at = @At("TAIL"))
    private void beaconBlockEntity$saveAdditional(CompoundTag tag, HolderLookup.Provider registries, CallbackInfo ci) {
        try {
            tag.putBoolean("isEnlightening", this.isEnlightening);
            tag.putUUID("omnipotentOwnerUUID", this.omnipotentOwner);
            tag.putInt("cachedEnlightenedAmount", this.cachedEnlightenedAmount);
        }
        catch(Exception ignored){}
    }

    @Inject(method = "applyEffects", at = @At(value = "HEAD"))
    private static void beaconBlockEntity$applyEffects(Level level, BlockPos pos, int beaconLevel, Holder<MobEffect> primaryEffect, Holder<MobEffect> secondaryEffect, CallbackInfo ci) {
        if(level.getBlockEntity(pos) instanceof BeaconBlockEntity beacon && ((EnlighteningBeacon) beacon).isEnlightening()) {
            double d = beaconLevel * 10 + 10;
            AABB aabb = (new AABB(pos)).inflate(d).expandTowards(0.0, level.getHeight(), 0.0);

            Player player = ((EnlighteningBeacon) beacon).getOmnipotentOwner();
            List<LivingEntity> entities = level.getEntitiesOfClass(LivingEntity.class, aabb, e -> e.getType() != EntityType.PLAYER && !((HarmonicEntity) e).getHarmonicState());

            for(LivingEntity entity : entities)
                if(player != null) POUtils.harmonizeEntityByBeacon(entity, level, player);

            if(player == null) ((EnlighteningBeacon) beacon).setEnlightenedCache(((EnlighteningBeacon) beacon).getEnlightenedCache() + entities.size());
            else {
                POUtils.incrementEnlightened(((EnlighteningBeacon) beacon).getEnlightenedCache(), player);
                ((EnlighteningBeacon) beacon).setEnlightenedCache(0);
            }
        }
    }

    @Override
    public int getEnlightenedCache() {
        return this.cachedEnlightenedAmount;
    }

    @Override
    public @Nullable Player getOmnipotentOwner() {
        if(this.level != null && omnipotentOwner != null)
            return this.level.getPlayerByUUID(omnipotentOwner);
        else return null;
    }

    @Override
    public void setEnlightenedCache(int i) {
        this.cachedEnlightenedAmount = i;
    }

    @Override
    public void setAsEnlightening(@Nullable Player player) {
        this.isEnlightening = true;
        this.omnipotentOwner = player != null ? player.getUUID() : new UUID(0L, 0L);
        this.cachedEnlightenedAmount = 0;
    }

    @Override
    public boolean isEnlightening() {
        return this.isEnlightening;
    }
}
