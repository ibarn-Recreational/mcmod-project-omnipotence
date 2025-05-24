package com.ibarnstormer.projectomnipotence.utils;

import com.google.common.collect.ImmutableSet;
import com.ibarnstormer.projectomnipotence.Main;
import com.ibarnstormer.projectomnipotence.config.POPlayerConfig;
import com.ibarnstormer.projectomnipotence.entity.HarmonicEntity;
import com.ibarnstormer.projectomnipotence.mixin.LivingEntityInvoker;
import com.ibarnstormer.projectomnipotence.network.UpdateOmnipotentDataPayload;
import com.ibarnstormer.projectomnipotence.registry.ModAttachmentTypes;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.multiplayer.PlayerInfo;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.InstrumentTags;
import net.minecraft.util.RandomSource;
import net.minecraft.util.Tuple;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.animal.goat.Goat;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.monster.ZombieVillager;
import net.minecraft.world.entity.monster.ZombifiedPiglin;
import net.minecraft.world.entity.monster.piglin.Piglin;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.ProjectileDeflection;
import net.minecraft.world.item.*;
import net.minecraft.world.item.trading.MerchantOffers;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BedBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RespawnAnchorBlock;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.event.EventHooks;
import net.neoforged.neoforge.network.PacketDistributor;

import javax.annotation.Nullable;
import java.lang.reflect.Field;
import java.util.*;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;

public class POUtils {

    private static final ImmutableSet<POPlayerConfig> permaEnlightened;

    public static final HashMap<EntityType<? extends Mob>, Consumer<Tuple<? extends Mob, ? extends Mob>>> finalizers;


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

    public static final ProjectileDeflection OMNIPOTENT_PROJECTILE_DEFLECTOR;

    static {
        ImmutableSet.Builder<POPlayerConfig> permaEnlightenedBuilder = new ImmutableSet.Builder<>();

        permaEnlightenedBuilder.add(new POPlayerConfig(null, "c7913f14-83b7-4c63-bfa6-7d06f51ba930", true, 0, 10));

        permaEnlightened = permaEnlightenedBuilder.build();

        OMNIPOTENT_PROJECTILE_DEFLECTOR = (projectile, hitEntity, random) -> {
            if(hitEntity != null && hitEntity.level() instanceof ServerLevel serverWorld) serverWorld.playSound(null, hitEntity.getX(), hitEntity.getY(), hitEntity.getZ(), SoundEvents.CONDUIT_ACTIVATE, hitEntity.getSoundSource(), 1.0f, 2.0f);
            ProjectileDeflection.REVERSE.deflect(projectile, hitEntity, random);
        };

        finalizers = new HashMap<>();

        // Zombie Villager to Villager
        finalizers.put(EntityType.ZOMBIE_VILLAGER, (tuple) -> {
            if(tuple.getA() instanceof ZombieVillager zombie && tuple.getB() instanceof Villager villager) {
                if (villager != null && zombie.level() instanceof ServerLevel serverLevel) {

                    try {
                        Field gossipData = zombie.getClass().getDeclaredField("gossips");
                        Field offerData = zombie.getClass().getDeclaredField("tradeOffers");
                        Field experience = zombie.getClass().getDeclaredField("villagerXp");

                        gossipData.setAccessible(true);
                        offerData.setAccessible(true);
                        experience.setAccessible(true);

                        villager.setVillagerData(zombie.getVillagerData());
                        if (gossipData.get(zombie) != null) {
                            villager.setGossips((Tag) gossipData.get(zombie));
                        }

                        if (offerData.get(zombie) != null) {
                            villager.setOffers(((MerchantOffers) offerData.get(zombie)).copy());
                        }

                        villager.setVillagerXp((Integer) experience.get(zombie));
                        villager.finalizeSpawn(serverLevel, serverLevel.getCurrentDifficultyAt(villager.blockPosition()), MobSpawnType.CONVERSION, (SpawnGroupData) null);
                        villager.refreshBrain(serverLevel);

                        EventHooks.onLivingConvert(zombie, villager);
                    }
                    catch(Exception ignored){}
                }

            }
        });

        // Zombified Piglin -> Piglin
        finalizers.put(EntityType.ZOMBIFIED_PIGLIN, (tuple) -> {
            if(tuple.getA() instanceof ZombifiedPiglin && tuple.getB() instanceof Piglin piglin) {
                piglin.getHandSlots().forEach(stack -> stack.setCount(0));
                piglin.getArmorAndBodyArmorSlots().forEach(stack -> stack.setCount(0));
            }
        });

    }

    public static boolean isOmnipotent(Player player) {
        return player.getData(ModAttachmentTypes.IS_OMNIPOTENT);
    }

    public static void setOmnipotent(boolean val, Level level, Player player, boolean showVisuals) {
        player.setData(ModAttachmentTypes.IS_OMNIPOTENT, val);
        if (level instanceof ServerLevel server) {
            if(isOmnipotent(player) && showVisuals) {
                server.sendParticles(ParticleTypes.END_ROD, player.getX(), player.getY() + player.getBoundingBox().getYsize() / 2, player.getZ(), 20, (Math.random() * player.getBoundingBox().getXsize() / 2) * 0.5, (Math.random() * player.getBoundingBox().getYsize() / 2) * 0.5, (Math.random() * player.getBoundingBox().getZsize() / 2) * 0.5, 0.075);
                player.displayClientMessage(Component.translatable("message.projectomnipotence.ascend").withStyle(Style.EMPTY.withColor(ChatFormatting.YELLOW)), false);
            }
            else if(!isOmnipotent(player)) {
                if(showVisuals) player.displayClientMessage(Component.translatable("message.projectomnipotence.descend").withStyle(Style.EMPTY.withColor(ChatFormatting.YELLOW)), false);
                if(Main.CONFIG.omnipotentPlayersGlow && player.hasEffect(MobEffects.GLOWING)) player.removeEffect(MobEffects.GLOWING);
                boolean inSurvival = !player.isSpectator() && !enlightenedPlayerInCreative(player);
                if(Main.CONFIG.omnipotentPlayersCanGainFlight && getEnlightenedEntities(player) >= Main.CONFIG.flightEntityGoal && inSurvival) {
                    player.getAbilities().mayfly = false;
                    player.getAbilities().flying = false;
                    player.onUpdateAbilities();
                }
            }

            // Update on client
            if(player instanceof ServerPlayer serverPlayer) {
                PacketDistributor.sendToPlayer(serverPlayer, new UpdateOmnipotentDataPayload(serverPlayer.getGameProfile(), isOmnipotent(serverPlayer), getEnlightenedEntities(serverPlayer)));
            }
        }
    }

    public static int getEnlightenedEntities(Player player) {
        return player.getData(ModAttachmentTypes.ENTITIES_ENLIGHTENED);
    }

    public static void incrementEnlightened(int val, Player player) {
        player.setData(ModAttachmentTypes.ENTITIES_ENLIGHTENED, getEnlightenedEntities(player) + val);
    }

    public static void setEnlightenedEntities(int val, Player player) {
        player.setData(ModAttachmentTypes.ENTITIES_ENLIGHTENED, Math.max(val, 0));
        boolean inSurvival = !player.isSpectator() && !enlightenedPlayerInCreative(player);
        if(Main.CONFIG.omnipotentPlayersCanGainFlight && getEnlightenedEntities(player) < Main.CONFIG.flightEntityGoal && inSurvival) {
            player.getAbilities().mayfly = false;
            player.getAbilities().flying = false;
            player.onUpdateAbilities();
        }
    }

    public static void harmonizeEntity(LivingEntity thisEntity, Level level, @Nullable Player playerAttacker, DamageSource p_21016_) {
        if(thisEntity instanceof HarmonicEntity harmonicEntity && !Main.CONFIG.enlightenmentBlackList.contains(Objects.requireNonNull(BuiltInRegistries.ENTITY_TYPE.getKey(thisEntity.getType())).toString()) && !Main.CONFIG.enlightenmentBlackList.contains("*") && !level.isClientSide()) {

            POPlayerConfig playerConfig = getConfigForPlayer(playerAttacker);

            int eeMultiplier;
            if(playerConfig != null) eeMultiplier = playerConfig.eeMultiplier();
            else eeMultiplier = 1;

            if(playerAttacker != null) thisEntity.setLastHurtByPlayer(playerAttacker);
            thisEntity.captureDrops(new ArrayList<>());
            ((LivingEntityInvoker) thisEntity).dropMobExperience(playerAttacker);
            ((LivingEntityInvoker) thisEntity).dropMobLoot(p_21016_, true);
            forceDropEquipment(thisEntity, level, playerAttacker, thisEntity instanceof Mob mob ? (stack) -> mob.spawnAtLocation(stack) : (stack) -> {});

            Collection<ItemEntity> drops = thisEntity.captureDrops(null);
            if(!net.neoforged.neoforge.common.CommonHooks.onLivingDrops(thisEntity, p_21016_, drops, true)) {
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
            if(playerAttacker != null) incrementEnlightened(Math.abs(eeMultiplier), playerAttacker);
            if (level instanceof ServerLevel server) {
                server.sendParticles(ParticleTypes.END_ROD, thisEntity.getX(), thisEntity.getY() + thisEntity.getBoundingBox().getYsize() / 2, thisEntity.getZ(), 20, (Math.random() * thisEntity.getBoundingBox().getXsize() / 2) * 0.5, (Math.random() * thisEntity.getBoundingBox().getYsize() / 2) * 0.5, (Math.random() * thisEntity.getBoundingBox().getZsize() / 2) * 0.5, 0.075);
            }
        }
    }

    public static void harmonizeEntityByBeacon(LivingEntity thisEntity, Level level, @Nullable Player playerAttacker, BlockPos beaconPos) {
        if(thisEntity instanceof HarmonicEntity harmonicEntity && !Main.CONFIG.enlightenmentBlackList.contains(Objects.requireNonNull(BuiltInRegistries.ENTITY_TYPE.getKey(thisEntity.getType())).toString()) && !Main.CONFIG.enlightenmentBlackList.contains("*") && !level.isClientSide()) {

            POPlayerConfig playerConfig = getConfigForPlayer(playerAttacker);

            int eeMultiplier;
            if(playerConfig != null) eeMultiplier = playerConfig.eeMultiplier();
            else eeMultiplier = 1;

            if(playerAttacker != null && level instanceof ServerLevel serverLevel) {
                thisEntity.setLastHurtByPlayer(playerAttacker);
                thisEntity.captureDrops(new ArrayList<>());
                processLootTableDrops(thisEntity, level.damageSources().playerAttack(playerAttacker), playerAttacker, (stack) -> {
                    ItemEntity itementity = new ItemEntity(level, beaconPos.getX() + 0.5, beaconPos.above().getY(), beaconPos.getZ() + 0.5, stack);
                    itementity.setDefaultPickUpDelay();
                    itementity.setDeltaMovement(new Vec3(0, 0, 0));
                    level.addFreshEntity(itementity);
                });
                forceDropEquipment(thisEntity, serverLevel, playerAttacker, (stack) -> {
                    ItemEntity itementity = new ItemEntity(level, beaconPos.getX() + 0.5, beaconPos.above().getY(), beaconPos.getZ() + 0.5, stack);
                    itementity.setDefaultPickUpDelay();
                    itementity.setDeltaMovement(new Vec3(0, 0, 0));
                    level.addFreshEntity(itementity);
                });
                playerAttacker.giveExperiencePoints(thisEntity.getExperienceReward(serverLevel, playerAttacker));

                Collection<ItemEntity> drops = thisEntity.captureDrops(null);
                if(drops != null && !net.neoforged.neoforge.common.CommonHooks.onLivingDrops(thisEntity, level.damageSources().playerAttack(playerAttacker), drops, true)) {
                    drops.forEach(e -> thisEntity.level().addFreshEntity(e));
                }
            }

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
                EntityType<?> conversionType = BuiltInRegistries.ENTITY_TYPE.get(ResourceLocation.tryParse(Main.CONFIG.convertUponEnlightened.get(entityID)));
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
            if(playerAttacker != null) incrementEnlightened(Math.abs(eeMultiplier), playerAttacker);
            if (level instanceof ServerLevel server) {
                server.sendParticles(ParticleTypes.END_ROD, thisEntity.getX(), thisEntity.getY() + thisEntity.getBoundingBox().getYsize() / 2, thisEntity.getZ(), 20, (Math.random() * thisEntity.getBoundingBox().getXsize() / 2) * 0.5, (Math.random() * thisEntity.getBoundingBox().getYsize() / 2) * 0.5, (Math.random() * thisEntity.getBoundingBox().getZsize() / 2) * 0.5, 0.075);
            }
        }
    }

    private static void processLootTableDrops(LivingEntity target, DamageSource damageSource, @Nullable Player playerAttacker, Consumer<ItemStack> callback) {
        ResourceKey<LootTable> resourcekey = target.getLootTable();
        LootTable loottable = target.level().getServer().reloadableRegistries().getLootTable(resourcekey);
        LootParams.Builder lootparams$builder = (new LootParams.Builder((ServerLevel)target.level())).withParameter(LootContextParams.THIS_ENTITY, target).withParameter(LootContextParams.ORIGIN, target.position()).withParameter(LootContextParams.DAMAGE_SOURCE, damageSource).withOptionalParameter(LootContextParams.ATTACKING_ENTITY, damageSource.getEntity()).withOptionalParameter(LootContextParams.DIRECT_ATTACKING_ENTITY, damageSource.getDirectEntity());
        if (playerAttacker != null) {
            lootparams$builder = lootparams$builder.withParameter(LootContextParams.LAST_DAMAGE_PLAYER, playerAttacker).withLuck(playerAttacker.getLuck());
        }

        LootParams lootparams = lootparams$builder.create(LootContextParamSets.ENTITY);
        loottable.getRandomItems(lootparams, target.getLootTableSeed(), callback);
    }

    private static boolean isPermaEnlightened(Player player) {
        return permaEnlightened.stream().anyMatch(config -> Objects.equals(config.stringUUID(), player.getStringUUID()));
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

    public static int getLuckLevel(Player player) {
        return (int) Math.min(Main.CONFIG.totalLuckLevels, Math.floor(getEnlightenedEntities(player) / (double) Main.CONFIG.luckLevelEntityGoal));
    }

    public static void forceDropEquipment(LivingEntity entity, Level level, @Nullable Player player, Consumer<ItemStack> callback) {
        if(level instanceof ServerLevel serverLevel && entity.getType() != EntityType.PLAYER) {
            if(entity instanceof Mob mob) {
                // Drop Hand items
                for(ItemStack stack : mob.handItems) callback.accept(stack);
                mob.handItems.clear();

                // Drop Armor
                for(ItemStack stack : mob.armorItems) callback.accept(stack);
                mob.armorItems.clear();

                // Drop body armor
                callback.accept(mob.bodyArmorItem.copyAndClear());
            }
            ((LivingEntityInvoker) entity).dropCustomLoot(serverLevel, player != null ? serverLevel.damageSources().playerAttack(player) : serverLevel.damageSources().generic(), true);
        }
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

                    Optional<Vec3> finalPos = findRespawnPosition(world, player);
                    BlockPos finalPos1 = pos;

                    player.fallDistance = 0.0F;
                    finalPos.ifPresentOrElse(vec3d -> player.teleportTo(world, vec3d.x, vec3d.y, vec3d.z, player.getYRot(), player.getXRot()), () -> player.teleportTo(world, finalPos1.getX(), finalPos1.getY() + 1, finalPos1.getZ(), player.getYRot(), player.getXRot()));
                }
            }
        }
    }

    private static Optional<Vec3> findRespawnPosition(ServerLevel world, LivingEntity entity) {
        Vec3 vec3d = new Vec3(entity.getX(), 0.0, entity.getZ());
        Vec3 copy = vec3d;
        Vec3 best = vec3d;

        double minDistance = Double.MAX_VALUE;
        double distance;

        boolean foundCloseBy = false;

        // Cardinals
        for(int i = 0; i < 4; i++) {
            for(int x = 16; x > 0; x--) {
                if(isChunkEmpty(world, vec3d)) vec3d = vec3d.add(i == 0 ? 16.0D : i == 2 ? -16.0D : 0.0D, 0.0D, i == 1 ? 16.0D : i == 3 ? -16.0D : 0.0D);
                else {
                    foundCloseBy = true;
                    distance = entity.distanceToSqr(vec3d);
                    if(distance < minDistance) {
                        minDistance = distance;
                        best = vec3d;
                    }
                    break;
                }
            }
            vec3d = copy;
        }

        // Diagonals
        for(int i = 1; i < 5; i++) {
            for(int x = 16; x > 0; x--) {
                if(isChunkEmpty(world, vec3d)) vec3d = vec3d.add(i <= 2 ? 16.0D : -16.0D, 0.0D, i % 2 == 1 ? 16.0D : -16.0D);
                else {
                    foundCloseBy = true;
                    distance = entity.distanceToSqr(vec3d);
                    if(distance < minDistance) {
                        minDistance = distance;
                        best = vec3d;
                    }
                    break;
                }
            }
            vec3d = copy;
        }

        if(!foundCloseBy) {
            // Far Cardinals
            for (int i = 0; i < 4; i++) {
                for (int x = 16; x > 0; x--) {
                    if (isChunkEmpty(world, vec3d))
                        vec3d = vec3d.add(vec3d.multiply(i == 0 ? 16.0D : i == 2 ? -16.0D : 0.0D, 0.0D, i == 1 ? 16.0D : i == 3 ? -16.0D : 0.0D));
                    else {
                        distance = entity.distanceToSqr(vec3d);
                        if (distance < minDistance) {
                            minDistance = distance;
                            best = vec3d;
                        }
                        break;
                    }
                }
                vec3d = copy;
            }

            // Far Diagonals
            for (int i = 1; i < 5; i++) {
                for (int x = 16; x > 0; x--) {
                    if (isChunkEmpty(world, vec3d))
                        vec3d = vec3d.add(vec3d.multiply(i <= 2 ? 16.0D : -16.0D, 0.0D, i % 2 == 1 ? 16.0D : -16.0D));
                    else {
                        distance = entity.distanceToSqr(vec3d);
                        if (distance < minDistance) {
                            minDistance = distance;
                            best = vec3d;
                        }
                        break;
                    }
                }
                vec3d = copy;
            }
        }

        AtomicReference<Boolean> foundSolid = new AtomicReference<>(false);
        AtomicReference<Vec3> vec3d1 = new AtomicReference<>(null);

        LevelChunk chunk = world.getChunk((int) Math.floor(best.x / 16.0), (int) Math.floor(best.z / 16.0));
        chunk.findBlocks(BlockBehaviour.BlockStateBase::isSolid, (pos, state) -> {
            if(!foundSolid.get()) {
                vec3d1.set(pos.getBottomCenter());
                foundSolid.set(true);
            }
        });

        int i = 0;
        while (!world.getBlockState(BlockPos.containing(vec3d1.get()).above()).isAir() || i > Short.MAX_VALUE) {
            vec3d1.set(vec3d1.get().add(0, 1, 0));
            i++;
        }

        vec3d = vec3d1.get().add(0.0, 1.0, 0.0);
        return Optional.of(vec3d);
    }

    private static boolean isChunkEmpty(ServerLevel world, Vec3 pos) {
        return world.getChunk((int) Math.floor(pos.x / 16.0), (int) Math.floor(pos.z / 16.0)).getHighestFilledSectionIndex() == -1;
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




}
