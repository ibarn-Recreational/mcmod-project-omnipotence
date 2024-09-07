package com.ibarnstormer.projectomnipotence.mixin;

import com.ibarnstormer.projectomnipotence.block.entity.EnlighteningBeacon;
import com.ibarnstormer.projectomnipotence.utils.POUtils;
import com.llamalad7.mixinextras.sugar.Local;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BeaconBlockEntity;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.effect.StatusEffect;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.registry.RegistryWrapper;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.world.World;
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

    @Inject(method = "readNbt", at = @At("TAIL"))
    private void beaconBlockEntity$readNbt(NbtCompound nbt, RegistryWrapper.WrapperLookup registryLookup, CallbackInfo ci) {
        try {
            this.isEnlightening = nbt.getBoolean("isEnlightening");
            this.omnipotentOwner = nbt.getUuid("omnipotentOwnerUUID");
            this.cachedEnlightenedAmount = nbt.getInt("cachedEnlightenedAmount");
        }
        catch(Exception ignored){}
    }

    @Inject(method = "writeNbt", at = @At("TAIL"))
    private void beaconBlockEntity$writeNbt(NbtCompound nbt, RegistryWrapper.WrapperLookup registryLookup, CallbackInfo ci) {
        try {
            nbt.putBoolean("isEnlightening", this.isEnlightening);
            nbt.putUuid("omnipotentOwnerUUID", this.omnipotentOwner);
            nbt.putInt("cachedEnlightenedAmount", this.cachedEnlightenedAmount);
        }
        catch(Exception ignored){}
    }

    @Inject(method = "applyPlayerEffects", at = @At(value = "HEAD"))
    private static void beaconBlockEntity$applyPlayerEffects(World world, BlockPos pos, int beaconLevel, @Nullable RegistryEntry<StatusEffect> primaryEffect, @Nullable RegistryEntry<StatusEffect> secondaryEffect, CallbackInfo ci) {
        if(world.getBlockEntity(pos) instanceof BeaconBlockEntity beacon && ((EnlighteningBeacon) beacon).isEnlightening()) {
            double d = beaconLevel * 10 + 10;
            Box box = new Box(pos).expand(d).stretch(0.0, world.getHeight(), 0.0);

            PlayerEntity player = ((EnlighteningBeacon) beacon).getOmnipotentOwner();
            List<LivingEntity> entities = world.getEntitiesByClass(LivingEntity.class, box, e -> e.getType() != EntityType.PLAYER && !POUtils.isInHarmony(e));

            for(LivingEntity entity : entities) POUtils.harmonizeEntityByBeacon(entity, player);

            if(player == null) ((EnlighteningBeacon) beacon).setEnlightenedCache(((EnlighteningBeacon) beacon).getEnlightenedCache() + entities.size());
            else {
                POUtils.setEntitiesEnlightened(player, POUtils.getEntitiesEnlightened(player) + ((EnlighteningBeacon) beacon).getEnlightenedCache());
                ((EnlighteningBeacon) beacon).setEnlightenedCache(0);
            }
        }
    }

    @Override
    public int getEnlightenedCache() {
        return this.cachedEnlightenedAmount;
    }

    @Override
    public @Nullable PlayerEntity getOmnipotentOwner() {
        if(this.world != null && omnipotentOwner != null)
            return this.world.getPlayerByUuid(omnipotentOwner);
        else return null;
    }

    @Override
    public void setEnlightenedCache(int i) {
        this.cachedEnlightenedAmount = i;
    }

    @Override
    public void setAsEnlightening(@Nullable PlayerEntity player) {
        this.isEnlightening = true;
        this.omnipotentOwner = player != null ? player.getUuid() : new UUID(0L, 0L);
        this.cachedEnlightenedAmount = 0;
    }

    @Override
    public boolean isEnlightening() {
        return this.isEnlightening;
    }
}
