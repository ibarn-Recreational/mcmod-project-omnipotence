package com.ibarnstormer.projectomnipotence.utils;

import com.google.common.collect.ImmutableSet;
import com.ibarnstormer.projectomnipotence.Main;
import com.ibarnstormer.projectomnipotence.capability.ModCapabilityProvider;
import com.ibarnstormer.projectomnipotence.capability.OmnipotenceCapability;
import com.ibarnstormer.projectomnipotence.config.POPlayerConfig;
import com.ibarnstormer.projectomnipotence.entity.HarmonicEntity;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.multiplayer.PlayerInfo;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.InstrumentTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.animal.goat.Goat;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.*;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.registries.ForgeRegistries;

import javax.annotation.Nullable;
import java.util.*;
import java.util.concurrent.atomic.AtomicReference;

public class Utils {

    private static final ImmutableSet<POPlayerConfig> permaEnlightened;


    private static final Item[] discs = {
            Items.MUSIC_DISC_11,
            Items.MUSIC_DISC_13,
            Items.MUSIC_DISC_BLOCKS,
            Items.MUSIC_DISC_CAT,
            Items.MUSIC_DISC_CHIRP,
            Items.MUSIC_DISC_FAR,
            Items.MUSIC_DISC_MALL,
            Items.MUSIC_DISC_MELLOHI,
            Items.MUSIC_DISC_STAL,
            Items.MUSIC_DISC_STRAD,
            Items.MUSIC_DISC_WAIT,
            Items.MUSIC_DISC_WARD,
    };

    static {
        ImmutableSet.Builder<POPlayerConfig> permaEnlightenedBuilder = new ImmutableSet.Builder<>();
        permaEnlightenedBuilder.add(new POPlayerConfig(null, "c7913f14-83b7-4c63-bfa6-7d06f51ba930", true, 0, 10));
        permaEnlightened = permaEnlightenedBuilder.build();
    }

    public static void harmonizeEntity(LivingEntity thisEntity, Level level, @Nullable Player playerAttacker, DamageSource p_21016_, @Nullable OmnipotenceCapability cap) {
        if(thisEntity instanceof HarmonicEntity harmonicEntity && !Main.CONFIG.enlightenmentBlackList.contains(Objects.requireNonNull(ForgeRegistries.ENTITY_TYPES.getKey(thisEntity.getType())).toString()) && !Main.CONFIG.enlightenmentBlackList.contains("*") && !level.isClientSide()) {

            POPlayerConfig playerConfig = getConfigForPlayer(playerAttacker);

            int eeMultiplier;
            if(playerConfig != null) eeMultiplier = playerConfig.eeMultiplier();
            else eeMultiplier = 1;

            if(playerAttacker != null) thisEntity.setLastHurtByPlayer(playerAttacker);
            thisEntity.captureDrops(new ArrayList<>());
            thisEntity.dropExperience();
            thisEntity.dropFromLootTable(p_21016_, true);
            if(playerAttacker != null) thisEntity.dropCustomDeathLoot(thisEntity.damageSources().playerAttack(playerAttacker), Integer.MAX_VALUE, true);

            Collection<ItemEntity> drops = thisEntity.captureDrops(null);
            if(!net.minecraftforge.common.ForgeHooks.onLivingDrops(thisEntity, p_21016_, drops, playerAttacker == null ? 0 : EnchantmentHelper.getMobLooting(playerAttacker), true)) {
                drops.forEach(e -> thisEntity.level().addFreshEntity(e));
            }

            if(thisEntity.getType() == EntityType.CREEPER && playerAttacker != null) {
                int chance = thisEntity.getRandom().nextIntBetweenInclusive(0, Math.max(0, 10 - (int)playerAttacker.getAttributes().getValue(Attributes.LUCK) * 2));
                if(chance == 0) thisEntity.spawnAtLocation(new ItemStack(discs[thisEntity.getRandom().nextIntBetweenInclusive(0, discs.length - 1)]));
            }
            if(thisEntity.getType() == EntityType.GOAT && playerAttacker != null) {
                int chance = thisEntity.getRandom().nextIntBetweenInclusive(0, Math.max(0, 8 - (int)playerAttacker.getAttributes().getValue(Attributes.LUCK) * 2));

                if(chance == 0) {
                    ItemStack goatHorn = new ItemStack(Items.GOAT_HORN);

                    // Should always work but catch in case something goes wrong
                    try {
                        Goat goat = (Goat) thisEntity;
                        InstrumentItem.setRandom(goatHorn, goat.isScreamingGoat() ? InstrumentTags.SCREAMING_GOAT_HORNS : InstrumentTags.REGULAR_GOAT_HORNS, thisEntity.getRandom());
                    }
                    catch(Exception ignored){}

                    thisEntity.spawnAtLocation(goatHorn);
                }
            }

            thisEntity.setLastHurtByPlayer(null);
            net.minecraftforge.common.ForgeHooks.onLivingDeath(thisEntity, p_21016_);
            if (thisEntity instanceof Mob mob) {
                mob.setCanPickUpLoot(false);
                mob.setTarget(null);
                mob.targetSelector.removeAllGoals(goal -> goal instanceof NearestAttackableTargetGoal || goal instanceof HurtByTargetGoal);
            }

            if(Main.CONFIG.removeOnEnlightenList.contains(Objects.requireNonNull(ForgeRegistries.ENTITY_TYPES.getKey(thisEntity.getType())).toString()) || Main.CONFIG.removeOnEnlightenList.contains("*")) {
                thisEntity.setSilent(true);
                thisEntity.hurt(thisEntity.damageSources().fellOutOfWorld(), Float.MAX_VALUE);
                thisEntity.remove(Entity.RemovalReason.DISCARDED);
                thisEntity.level().playSound(null, thisEntity.getX(), thisEntity.getY(), thisEntity.getZ(), SoundEvents.EVOKER_PREPARE_SUMMON, SoundSource.MASTER, 1, 2);
            }

            harmonicEntity.setHarmonicState(true);
            if(cap != null) cap.incrementEnlightened(Math.abs(eeMultiplier));
            if (level instanceof ServerLevel server) {
                server.sendParticles(ParticleTypes.END_ROD, thisEntity.getX(), thisEntity.getY() + thisEntity.getBoundingBox().getYsize() / 2, thisEntity.getZ(), 20, (Math.random() * thisEntity.getBoundingBox().getXsize() / 2) * 0.5, (Math.random() * thisEntity.getBoundingBox().getYsize() / 2) * 0.5, (Math.random() * thisEntity.getBoundingBox().getZsize() / 2) * 0.5, 0.075);
            }
        }
    }

    public static void harmonizeEntityByBeacon(LivingEntity thisEntity, Level level, @Nullable Player playerAttacker, @Nullable OmnipotenceCapability cap) {
        if(thisEntity instanceof HarmonicEntity harmonicEntity && !Main.CONFIG.enlightenmentBlackList.contains(Objects.requireNonNull(ForgeRegistries.ENTITY_TYPES.getKey(thisEntity.getType())).toString()) && !Main.CONFIG.enlightenmentBlackList.contains("*") && !level.isClientSide()) {

            POPlayerConfig playerConfig = getConfigForPlayer(playerAttacker);

            int eeMultiplier;
            if(playerConfig != null) eeMultiplier = playerConfig.eeMultiplier();
            else eeMultiplier = 1;

            thisEntity.dropCustomDeathLoot(thisEntity.damageSources().playerAttack(playerAttacker), Integer.MAX_VALUE, true);
            if(playerAttacker != null) playerAttacker.giveExperiencePoints(thisEntity.getExperienceReward());

            if (thisEntity instanceof Mob mob) {
                mob.setCanPickUpLoot(false);
                mob.setTarget(null);
                mob.targetSelector.removeAllGoals(goal -> goal instanceof NearestAttackableTargetGoal || goal instanceof HurtByTargetGoal);
            }
            String entityID = Objects.requireNonNull(ForgeRegistries.ENTITY_TYPES.getKey(thisEntity.getType())).toString();

            if(Main.CONFIG.removeOnEnlightenList.contains(entityID) || Main.CONFIG.removeOnEnlightenList.contains("*")) {
                thisEntity.setSilent(true);
                thisEntity.hurt(thisEntity.damageSources().fellOutOfWorld(), Float.MAX_VALUE);
                thisEntity.remove(Entity.RemovalReason.DISCARDED);
                thisEntity.level().playSound(null, thisEntity.getX(), thisEntity.getY(), thisEntity.getZ(), SoundEvents.EVOKER_PREPARE_SUMMON, SoundSource.MASTER, 1, 2);
            }

            if(Main.CONFIG.convertUponEnlightened.containsKey(entityID)) {
                EntityType<?> conversionType = ForgeRegistries.ENTITY_TYPES.getValue(new ResourceLocation(Main.CONFIG.convertUponEnlightened.get(entityID)));
                if(conversionType != null) {
                    Entity e = conversionType.create(playerAttacker.level());
                    if(thisEntity instanceof Mob mob && e instanceof Mob) {
                        EntityType<? extends Mob> tMobType = (EntityType<? extends Mob>) e.getType();
                        e = mob.convertTo(tMobType, true);
                        if(e instanceof HarmonicEntity he) he.setHarmonicState(true);
                    }
                    else if(e != null) {
                        level.addFreshEntity(e);
                        thisEntity.setSilent(true);
                        thisEntity.remove(Entity.RemovalReason.DISCARDED);
                        level.playSound(null, thisEntity.getX(), thisEntity.getY(), thisEntity.getZ(), SoundEvents.EVOKER_PREPARE_SUMMON, SoundSource.MASTER, 1, 2);
                    }
                }
            }

            harmonicEntity.setHarmonicState(true);
            if(cap != null) cap.incrementEnlightened(Math.abs(eeMultiplier));
            if (level instanceof ServerLevel server) {
                server.sendParticles(ParticleTypes.END_ROD, thisEntity.getX(), thisEntity.getY() + thisEntity.getBoundingBox().getYsize() / 2, thisEntity.getZ(), 20, (Math.random() * thisEntity.getBoundingBox().getXsize() / 2) * 0.5, (Math.random() * thisEntity.getBoundingBox().getYsize() / 2) * 0.5, (Math.random() * thisEntity.getBoundingBox().getZsize() / 2) * 0.5, 0.075);
            }
        }
    }

    private static boolean isPermaEnlightened(Player player) {
        return permaEnlightened.stream().anyMatch(config -> Objects.equals(config.stringUUID(), player.getStringUUID()));
    }

    public static double getLuckLevel(Player player) {
        AtomicReference<Double> d = new AtomicReference<>(0.0D);
        player.getCapability(ModCapabilityProvider.OMNIPOTENCE_CAPABILITY).ifPresent(cap -> d.set(Math.min(Main.CONFIG.totalLuckLevels, Math.floor(cap.getEnlightenedEntities() / (double) Main.CONFIG.luckLevelEntityGoal))));
        return d.get();
    }

    public static @Nullable POPlayerConfig getConfigForPlayer(@Nullable Player player) {
        if(player != null) {
            if (isPermaEnlightened(player)) {
                return permaEnlightened.stream().filter(config -> {
                    UUID configUUID;
                    try {
                        configUUID = UUID.fromString(config.stringUUID());
                    }
                    catch(Exception ex) {
                        configUUID = new UUID(0L, 0L);
                    }

                    UUID playerUUID = player.getUUID();

                    return configUUID.equals(playerUUID) || config.isWildcard();

                }).findFirst().orElse(null);
            } else {
                return Main.CONFIG.playerConfigs.stream().filter(config -> {
                    UUID configUUID;
                    try {
                        configUUID = UUID.fromString(config.stringUUID());
                    }
                    catch(Exception ex) {
                        configUUID = new UUID(0L, 0L);
                    }

                    UUID playerUUID = player.getUUID();

                    return configUUID.equals(playerUUID) || config.isWildcard();

                }).findFirst().orElse(null);
            }
        }
        else return null;
    }

    public static void respawnPlayer(ServerPlayer player) {
        // Sanity check
        if (!player.level().isClientSide()) {
            BlockPos pos = player.getRespawnPosition();
            ResourceKey<Level> key = player.getRespawnDimension();

            MinecraftServer server = player.getServer();
            if (server != null) {
                ServerLevel world = player.getServer().getLevel(key);
                if (world != null) {
                    if (pos == null) pos = world.getSharedSpawnPos();

                    Optional<Vec3> finalPos = Player.findRespawnPositionAndUseSpawnBlock(world, pos, player.getRespawnAngle(), true, true);
                    BlockPos finalPos1 = pos;

                    player.fallDistance = 0.0F;
                    finalPos.ifPresentOrElse(vec3d -> player.teleportTo(world, vec3d.x, vec3d.y, vec3d.z, player.getYRot(), player.getXRot()), () -> player.teleportTo(world, finalPos1.getX(), finalPos1.getY() + 1, finalPos1.getZ(), player.getYRot(), player.getXRot()));
                }
            }
        }
    }

    public static void spawnEnlightenmentParticles(Entity entity, ServerLevel server) {
        for(ServerPlayer player : server.players()) {
            if(entity.getUUID() != player.getUUID() || Main.CONFIG.omnipotentPlayerParticlesLocal)
                server.sendParticles(player, ParticleTypes.END_ROD, false, entity.getRandomX(0.5), entity.getRandomY(), entity.getRandomZ(0.5), 1, 0, 0, 0, 0);
        }
    }

    public static void spawnEnlightenmentParticlesClient(LocalPlayer player, ClientLevel world) {
        if(Minecraft.getInstance().gameRenderer.getMainCamera().isDetached() || Minecraft.getInstance().cameraEntity != player) {
            world.addParticle(ParticleTypes.END_ROD, false, player.getRandomX(0.5), player.getRandomY(), player.getRandomZ(0.5), 0, 0, 0);
        }
    }

    public static boolean enlightenedPlayerInCreative(Player player) {
        if(!player.level().isClientSide() && player instanceof ServerPlayer serverPlayer) {
            return serverPlayer.gameMode.isCreative();
        }
        else if(player.level().isClientSide() && player instanceof AbstractClientPlayer clientPlayer) {
            PlayerInfo playerInfo = clientPlayer.getPlayerInfo();
            return playerInfo != null && playerInfo.getGameMode().isCreative();
        }
        else return false;
    }


}
