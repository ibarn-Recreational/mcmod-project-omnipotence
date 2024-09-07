package com.ibarnstormer.projectomnipotence.capability;

import com.ibarnstormer.projectomnipotence.Main;
import net.minecraft.ChatFormatting;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;

public class OmnipotenceCapability {


    private boolean isOmnipotent;
    private int enlightenedEntities;

    public boolean isOmnipotent() {
        return isOmnipotent;
    }

    public void setOmnipotent(boolean val) {
        this.isOmnipotent = val;
    }

    public void setOmnipotent(boolean val, Level level, Player player, boolean showVisuals) {
        this.isOmnipotent = val;
        if (level instanceof ServerLevel server) {
            if(this.isOmnipotent && showVisuals) {
                server.sendParticles(ParticleTypes.END_ROD, player.getX(), player.getY() + player.getBoundingBox().getYsize() / 2, player.getZ(), 20, (Math.random() * player.getBoundingBox().getXsize() / 2) * 0.5, (Math.random() * player.getBoundingBox().getYsize() / 2) * 0.5, (Math.random() * player.getBoundingBox().getZsize() / 2) * 0.5, 0.075);
                player.displayClientMessage(Component.translatable("message.projectomnipotence.ascend").withStyle(Style.EMPTY.withColor(ChatFormatting.YELLOW)), false);
            }
            else if(!this.isOmnipotent) {
                if(showVisuals) player.displayClientMessage(Component.translatable("message.projectomnipotence.descend").withStyle(Style.EMPTY.withColor(ChatFormatting.YELLOW)), false);
                if(Main.CONFIG.omnipotentPlayersGlow && player.hasEffect(MobEffects.GLOWING)) player.removeEffect(MobEffects.GLOWING);
                boolean inSurvival = !player.isSpectator() && !player.isCreative();
                if(Main.CONFIG.omnipotentPlayersCanGainFlight && enlightenedEntities >= Main.CONFIG.flightEntityGoal && inSurvival) {
                    player.getAbilities().mayfly = false;
                    player.getAbilities().flying = false;
                    player.onUpdateAbilities();
                }
            }
        }
    }

    public int getEnlightenedEntities() {
        return enlightenedEntities;
    }

    public void incrementEnlightened(int val) {
        enlightenedEntities += val;
    }

    public void decrementEnlightened(int val) {
        if(enlightenedEntities - val < 0) enlightenedEntities = 0;
        else enlightenedEntities -= val;
    }

    public void setEnlightenedEntities(int val) {
        this.enlightenedEntities = Math.max(val, 0);
    }

    public void setEnlightenedEntities(int val, Player player) {
        enlightenedEntities = Math.max(val, 0);
        boolean inSurvival = !player.isSpectator() && !player.isCreative();
        if(Main.CONFIG.omnipotentPlayersCanGainFlight && enlightenedEntities < Main.CONFIG.flightEntityGoal && inSurvival) {
            player.getAbilities().mayfly = false;
            player.getAbilities().flying = false;
            player.onUpdateAbilities();
        }
    }

    public void copyFrom(OmnipotenceCapability cap) {
        this.isOmnipotent = cap.isOmnipotent;
        this.enlightenedEntities = cap.enlightenedEntities;
    }

    public void saveNbt(CompoundTag nbt) {
        nbt.putBoolean("isOmnipotent", isOmnipotent);
        nbt.putInt("enlightenedEntities", enlightenedEntities);
    }

    public void writeNbt(CompoundTag nbt) {
        isOmnipotent = nbt.getBoolean("isOmnipotent");
        enlightenedEntities = nbt.getInt("enlightenedEntities");
    }
}
