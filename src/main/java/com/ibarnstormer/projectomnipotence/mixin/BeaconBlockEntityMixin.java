package com.ibarnstormer.projectomnipotence.mixin;

import com.ibarnstormer.projectomnipotence.block.entity.EnlighteningBeacon;
import com.ibarnstormer.projectomnipotence.utils.POUtils;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.List;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.UUIDUtil;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BeaconBlockEntity;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.AABB;

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
    private void beaconBlockEntity$readData(ValueInput view, CallbackInfo ci) {
        try {
            this.isEnlightening = view.getBooleanOr("isEnlightening", false);
            view.read("omnipotentOwnerUUID", UUIDUtil.CODEC);
            this.cachedEnlightenedAmount = view.getIntOr("cachedEnlightenedAmount", 0);
        }
        catch(Exception ignored){}
    }

    @Inject(method = "saveAdditional", at = @At("TAIL"))
    private void beaconBlockEntity$writeData(ValueOutput view, CallbackInfo ci) {
        try {
            view.putBoolean("isEnlightening", this.isEnlightening);
            view.store("omnipotentOwnerUUID", UUIDUtil.CODEC, this.omnipotentOwner);
            view.putInt("cachedEnlightenedAmount", this.cachedEnlightenedAmount);
        }
        catch(Exception ignored){}
    }

    @Inject(method = "applyEffects", at = @At(value = "HEAD"))
    private static void beaconBlockEntity$applyPlayerEffects(Level world, BlockPos pos, int beaconLevel, @Nullable Holder<MobEffect> primaryEffect, @Nullable Holder<MobEffect> secondaryEffect, CallbackInfo ci) {
        if(world.getBlockEntity(pos) instanceof BeaconBlockEntity beacon && ((EnlighteningBeacon) beacon).isEnlightening()) {
            double d = beaconLevel * 10 + 10;
            AABB box = new AABB(pos).inflate(d).expandTowards(0.0, world.getHeight(), 0.0);

            Player player = ((EnlighteningBeacon) beacon).getOmnipotentOwner();
            List<LivingEntity> entities = world.getEntitiesOfClass(LivingEntity.class, box, e -> e.getType() != EntityType.PLAYER && !POUtils.isInHarmony(e));

            for(LivingEntity entity : entities) POUtils.harmonizeEntityByBeacon(entity, player, pos);

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
