package com.ibarnstormer.projectomnipotence.utils;

import com.ibarnstormer.projectomnipotence.Main;
import com.ibarnstormer.projectomnipotence.capability.ModCapabilityProvider;
import com.ibarnstormer.projectomnipotence.capability.OmnipotenceCapability;
import com.ibarnstormer.projectomnipotence.entity.HarmonicEntity;
import com.ibarnstormer.projectomnipotence.mixin.LivingEntityInvoker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Registry;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
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
import net.minecraft.world.damagesource.DamageType;
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
import net.neoforged.neoforge.registries.NeoForgeRegistries;

import javax.annotation.Nullable;
import java.util.*;
import java.util.concurrent.atomic.AtomicReference;

public class Utils {

    private static final Set<UUID> trueEnlightened;

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
        trueEnlightened = new HashSet<>();

        trueEnlightened.add(UUID.fromString("c7913f14-83b7-4c63-bfa6-7d06f51ba930"));
    }

    public static void harmonizeEntity(LivingEntity thisEntity, Level level, @Nullable Player playerAttacker, DamageSource p_21016_, @Nullable OmnipotenceCapability cap) {
        if(thisEntity instanceof HarmonicEntity harmonicEntity && !Main.CONFIG.enlightenmentBlackList.contains(Objects.requireNonNull(BuiltInRegistries.ENTITY_TYPE.getKey(thisEntity.getType())).toString()) && !Main.CONFIG.enlightenmentBlackList.contains("*") && !level.isClientSide()) {
            if(playerAttacker != null) thisEntity.setLastHurtByPlayer(playerAttacker);
            thisEntity.captureDrops(new ArrayList<>());
            ((LivingEntityInvoker) thisEntity).dropMobExperience();
            ((LivingEntityInvoker) thisEntity).dropMobLoot(p_21016_, true);
            if(playerAttacker != null) ((LivingEntityInvoker) thisEntity).dropEntityEquipment(thisEntity.damageSources().playerAttack(playerAttacker), Integer.MAX_VALUE, true);

            Collection<ItemEntity> drops = thisEntity.captureDrops(null);
            if(!net.neoforged.neoforge.common.CommonHooks.onLivingDrops(thisEntity, p_21016_, drops, playerAttacker == null ? 0 : EnchantmentHelper.getMobLooting(playerAttacker), true)) {
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
            net.neoforged.neoforge.common.CommonHooks.onLivingDeath(thisEntity, p_21016_);
            if (thisEntity instanceof Mob mob) {
                mob.setCanPickUpLoot(false);
                mob.setTarget(null);
                mob.targetSelector.removeAllGoals(goal -> goal instanceof NearestAttackableTargetGoal || goal instanceof HurtByTargetGoal);
            }

            if(Main.CONFIG.removeOnEnlightenList.contains(Objects.requireNonNull(BuiltInRegistries.ENTITY_TYPE.getKey(thisEntity.getType())).toString()) || Main.CONFIG.removeOnEnlightenList.contains("*")) {
                thisEntity.setSilent(true);
                thisEntity.hurt(thisEntity.damageSources().fellOutOfWorld(), Float.MAX_VALUE);
                thisEntity.remove(Entity.RemovalReason.DISCARDED);
                thisEntity.level().playSound(null, thisEntity.getX(), thisEntity.getY(), thisEntity.getZ(), SoundEvents.EVOKER_PREPARE_SUMMON, SoundSource.MASTER, 1, 2);
            }

            harmonicEntity.setHarmonicState(true);
            if(cap != null) cap.incrementEnlightened(1);
            if (level instanceof ServerLevel server) {
                server.sendParticles(ParticleTypes.END_ROD, thisEntity.getX(), thisEntity.getY() + thisEntity.getBoundingBox().getYsize() / 2, thisEntity.getZ(), 20, (Math.random() * thisEntity.getBoundingBox().getXsize() / 2) * 0.5, (Math.random() * thisEntity.getBoundingBox().getYsize() / 2) * 0.5, (Math.random() * thisEntity.getBoundingBox().getZsize() / 2) * 0.5, 0.075);
            }
        }
    }

    public static void harmonizeEntityByBeacon(LivingEntity thisEntity, Level level, @Nullable Player playerAttacker, @Nullable OmnipotenceCapability cap) {
        if(thisEntity instanceof HarmonicEntity harmonicEntity && !Main.CONFIG.enlightenmentBlackList.contains(Objects.requireNonNull(BuiltInRegistries.ENTITY_TYPE.getKey(thisEntity.getType())).toString()) && !Main.CONFIG.enlightenmentBlackList.contains("*") && !level.isClientSide()) {
            ((LivingEntityInvoker) thisEntity).dropEntityEquipment(thisEntity.damageSources().playerAttack(playerAttacker), Integer.MAX_VALUE, true);
            if(playerAttacker != null) playerAttacker.giveExperiencePoints(thisEntity.getExperienceReward());

            if (thisEntity instanceof Mob mob) {
                mob.setCanPickUpLoot(false);
                mob.setTarget(null);
                mob.targetSelector.removeAllGoals(goal -> goal instanceof NearestAttackableTargetGoal || goal instanceof HurtByTargetGoal);
            }
            String entityID = Objects.requireNonNull(BuiltInRegistries.ENTITY_TYPE.getKey(thisEntity.getType())).toString();

            if(Main.CONFIG.removeOnEnlightenList.contains(entityID) || Main.CONFIG.removeOnEnlightenList.contains("*")) {
                thisEntity.setSilent(true);
                thisEntity.hurt(thisEntity.damageSources().fellOutOfWorld(), Float.MAX_VALUE);
                thisEntity.remove(Entity.RemovalReason.DISCARDED);
                thisEntity.level().playSound(null, thisEntity.getX(), thisEntity.getY(), thisEntity.getZ(), SoundEvents.EVOKER_PREPARE_SUMMON, SoundSource.MASTER, 1, 2);
            }

            if(Main.CONFIG.convertUponEnlightened.containsKey(entityID)) {
                EntityType<?> conversionType = BuiltInRegistries.ENTITY_TYPE.get(new ResourceLocation(Main.CONFIG.convertUponEnlightened.get(entityID)));
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
            if(cap != null) cap.incrementEnlightened(1);
            if (level instanceof ServerLevel server) {
                server.sendParticles(ParticleTypes.END_ROD, thisEntity.getX(), thisEntity.getY() + thisEntity.getBoundingBox().getYsize() / 2, thisEntity.getZ(), 20, (Math.random() * thisEntity.getBoundingBox().getXsize() / 2) * 0.5, (Math.random() * thisEntity.getBoundingBox().getYsize() / 2) * 0.5, (Math.random() * thisEntity.getBoundingBox().getZsize() / 2) * 0.5, 0.075);
            }
        }
    }

    public static boolean isTrueEnlightened(Player player) {
        return trueEnlightened.contains(player.getUUID());
    }

    public static double getLuckLevel(Player player) {
        AtomicReference<Double> d = new AtomicReference<>(0.0D);
        player.getCapability(ModCapabilityProvider.OMNIPOTENCE_CAPABILITY).ifPresent(cap -> d.set(Math.min(Main.CONFIG.totalLuckLevels, Math.floor(cap.getEnlightenedEntities() / (double) Main.CONFIG.luckLevelEntityGoal))));
        return d.get();
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
        double deltaX = Math.min(entity.getBoundingBox().getXsize(), Math.max(-entity.getBoundingBox().getXsize(), (Math.random() * entity.getBoundingBox().getXsize() / 2) * 1.25));
        double deltaY = Math.min(entity.getBoundingBox().getYsize() / 3, Math.max(-entity.getBoundingBox().getYsize() / 3, (Math.random() * entity.getBoundingBox().getYsize() / 3) * 1.25));
        double deltaZ = Math.min(entity.getBoundingBox().getZsize(), Math.max(-entity.getBoundingBox().getZsize(), (Math.random() * entity.getBoundingBox().getZsize() / 2) * 1.25));

        for(ServerPlayer player : server.players()) {
            if(entity.getUUID() != player.getUUID() || Main.CONFIG.omnipotentPlayerParticlesLocal)
                server.sendParticles(player, ParticleTypes.END_ROD, false, entity.getX(), entity.getY() + entity.getBoundingBox().getYsize() / 2, entity.getZ(), 1, deltaX, deltaY, deltaZ, 0);
        }
    }

    public static void spawnEnlightenmentParticlesClient(LocalPlayer player, ClientLevel world) {

        double deltaX = Math.min(player.getBoundingBox().getXsize(), Math.max(-player.getBoundingBox().getXsize(), (Math.random() * player.getBoundingBox().getXsize() / 2) * 1.25));
        double deltaY = Math.min(player.getBoundingBox().getYsize() / 3, Math.max(-player.getBoundingBox().getYsize() / 3, (Math.random() * player.getBoundingBox().getYsize() / 3) * 1.25));
        double deltaZ = Math.min(player.getBoundingBox().getZsize(), Math.max(-player.getBoundingBox().getZsize(), (Math.random() * player.getBoundingBox().getZsize() / 2) * 1.25));

        RandomSource random = world.random;

        double g = random.nextGaussian() * deltaX;
        double h = random.nextGaussian() * deltaY;
        double j = random.nextGaussian() * deltaZ;

        if(Minecraft.getInstance().gameRenderer.getMainCamera().isDetached() || Minecraft.getInstance().cameraEntity != player) {
            world.addParticle(ParticleTypes.END_ROD, false, player.getX() + g, player.getY() + player.getBoundingBox().getYsize() / 2 + h, player.getZ() + j, 0, 0, 0);
        }
    }


}
