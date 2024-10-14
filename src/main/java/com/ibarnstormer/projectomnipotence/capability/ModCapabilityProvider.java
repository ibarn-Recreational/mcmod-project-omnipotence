package com.ibarnstormer.projectomnipotence.capability;

import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.neoforged.neoforge.common.capabilities.Capability;
import net.neoforged.neoforge.common.capabilities.CapabilityManager;
import net.neoforged.neoforge.common.capabilities.CapabilityToken;
import net.neoforged.neoforge.common.capabilities.ICapabilityProvider;
import net.neoforged.neoforge.common.util.INBTSerializable;
import net.neoforged.neoforge.common.util.LazyOptional;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class ModCapabilityProvider implements ICapabilityProvider, INBTSerializable<CompoundTag> {

    public static Capability<OmnipotenceCapability> OMNIPOTENCE_CAPABILITY = CapabilityManager.get(new CapabilityToken<>() {});

    public final OmnipotenceCapability omnipotenceCapability = new OmnipotenceCapability();
    public final LazyOptional<OmnipotenceCapability> lazyOptional = LazyOptional.of(() -> omnipotenceCapability);

    @Override
    public @NotNull <T> LazyOptional<T> getCapability(@NotNull Capability<T> cap, @Nullable Direction side) {
        if(cap == OMNIPOTENCE_CAPABILITY) return lazyOptional.cast();
        else return LazyOptional.empty();
    }

    @Override
    public CompoundTag serializeNBT() {
        CompoundTag nbt = new CompoundTag();
        this.omnipotenceCapability.saveNbt(nbt);
        return nbt;
    }

    @Override
    public void deserializeNBT(CompoundTag nbt) {
        this.omnipotenceCapability.writeNbt(nbt);
    }
}
