package com.ibarnstormer.projectomnipotence.utils;

import com.google.common.collect.ImmutableSet;
import com.ibarnstormer.projectomnipotence.Main;
import com.ibarnstormer.projectomnipotence.entity.ServerTrackedData;
import com.ibarnstormer.projectomnipotence.entity.data.ServersideDataTracker;
import com.ibarnstormer.projectomnipotence.network.payload.SyncSSDHDataPayload;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.block.BedBlock;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.RespawnAnchorBlock;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.network.PlayerListEntry;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.entity.*;
import net.minecraft.entity.ai.goal.ActiveTargetGoal;
import net.minecraft.entity.ai.goal.RevengeGoal;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.conversion.EntityConversionContext;
import net.minecraft.entity.conversion.EntityConversionType;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.data.TrackedData;
import net.minecraft.entity.data.TrackedDataHandlerRegistry;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.entity.mob.PiglinEntity;
import net.minecraft.entity.mob.ZombieVillagerEntity;
import net.minecraft.entity.mob.ZombifiedPiglinEntity;
import net.minecraft.entity.passive.GoatEntity;
import net.minecraft.entity.passive.VillagerEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.*;
import net.minecraft.loot.LootTable;
import net.minecraft.loot.context.LootContextParameters;
import net.minecraft.loot.context.LootContextTypes;
import net.minecraft.loot.context.LootWorldContext;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.network.packet.s2c.play.PositionFlag;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.registry.Registries;
import net.minecraft.registry.RegistryKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Style;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.math.random.Random;
import net.minecraft.village.TradeOfferList;
import net.minecraft.village.VillagerGossips;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;

import java.lang.reflect.Field;
import java.util.*;
import java.util.function.Consumer;

public class POUtils {

    private static final PlayerTrackedData playerData = new PlayerTrackedData();

    // Create a tracked data instance for each respective class so that the ids of other data don't clash
    private static final HashMap<Class<? extends LivingEntity>, TrackedData<Boolean>> livingEntityDataSet = new HashMap<>();
    private static final HashMap<EntityType<? extends MobEntity>, POEntityConversionHelper<? extends MobEntity, ? extends MobEntity>> finalizers;

    private static final ImmutableSet<UUID> trueEnlightened;

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
        ImmutableSet.Builder<UUID> trueEnlightenedBuilder = new ImmutableSet.Builder<>();

        trueEnlightenedBuilder.add(UUID.fromString("c7913f14-83b7-4c63-bfa6-7d06f51ba930"));

        trueEnlightened = trueEnlightenedBuilder.build();

        OMNIPOTENT_PROJECTILE_DEFLECTOR = (projectile, hitEntity, random) -> {
            if(hitEntity != null && hitEntity.getWorld() instanceof ServerWorld serverWorld) serverWorld.playSound(null, hitEntity.getX(), hitEntity.getY(), hitEntity.getZ(), SoundEvents.BLOCK_CONDUIT_ACTIVATE, hitEntity.getSoundCategory(), 1.0f, 2.0f);
            ProjectileDeflection.SIMPLE.deflect(projectile, hitEntity, random);
        };

        finalizers = new HashMap<>();

        // Zombie Villager to Villager
        addConversionFinalizer(EntityType.ZOMBIE_VILLAGER, new POEntityConversionHelper<ZombieVillagerEntity, VillagerEntity>(EntityType.VILLAGER, (source) -> {
            if(source.getType() == EntityType.ZOMBIE_VILLAGER) {

                return (villager) -> {
                    World world = villager.getWorld();

                    if (world instanceof ServerWorld serverWorld) {
                        try {
                            Field gossipData = source.getClass().getDeclaredField("gossip");
                            Field offerData = source.getClass().getDeclaredField("offerData");
                            Field experience = source.getClass().getDeclaredField("experience");

                            gossipData.setAccessible(true);
                            offerData.setAccessible(true);
                            experience.setAccessible(true);

                            villager.setVillagerData(source.getVillagerData());
                            if (gossipData.get(source) != null) {
                                villager.readGossipData((VillagerGossips) gossipData.get(source));
                            }

                            if (offerData.get(source) != null) {
                                villager.setOffers(((TradeOfferList) offerData.get(source)).copy());
                            }

                            villager.setExperience(experience.getInt(source));
                            villager.initialize(serverWorld, serverWorld.getLocalDifficulty(villager.getBlockPos()), SpawnReason.CONVERSION, null);
                            villager.reinitializeBrain(serverWorld);
                        } catch (Exception ignored) {
                        }
                    }
                };
            }
            else return (e) -> {};

        }));

        // Zombified Piglin -> Piglin
        addConversionFinalizer(EntityType.ZOMBIFIED_PIGLIN, new POEntityConversionHelper<ZombifiedPiglinEntity, PiglinEntity>(EntityType.PIGLIN, (source) -> {
            if(source.getType() == EntityType.ZOMBIFIED_PIGLIN) {
                return (piglin) -> {
                    for (EquipmentSlot slot : EquipmentSlot.VALUES) {
                        piglin.equipStack(slot, ItemStack.EMPTY);
                    }
                };
            }
            else return (e) -> {};
        }));

    }

    // Addons can add finalizers here
    public static void addConversionFinalizer(EntityType<? extends MobEntity> sourceType, POEntityConversionHelper helper) {
        finalizers.put(sourceType, helper);
    }

    public static POEntityConversionHelper getConversionFinalizer(EntityType<? extends MobEntity> type) {
        return finalizers.get(type);
    }

    public static void initPlayerData(ServersideDataTracker.Builder builder) {
        builder.add(playerData.IS_OMNIPOTENT(), false);
        builder.add(playerData.ENTITIES_ENLIGHTENED(), 0);
    }

    public static void readPlayerNbt(PlayerEntity player, NbtCompound nbt) {
        ((ServerTrackedData) player).getServersideDataTracker().set(playerData.IS_OMNIPOTENT(), nbt.getBoolean("isOmnipotent").orElse(false));
        ((ServerTrackedData) player).getServersideDataTracker().set(playerData.ENTITIES_ENLIGHTENED(), nbt.getInt("EntitiesEnlightened").orElse(0));
    }

    public static void writePlayerNbt(PlayerEntity player, NbtCompound nbt) {
        boolean isOmnipotent = ((ServerTrackedData) player).getServersideDataTracker().get(playerData.IS_OMNIPOTENT());
        int entitiesEnlightened = ((ServerTrackedData) player).getServersideDataTracker().get(playerData.ENTITIES_ENLIGHTENED());
        nbt.putBoolean("isOmnipotent", isOmnipotent);
        nbt.putInt("EntitiesEnlightened", entitiesEnlightened);
    }

    private static TrackedData<Boolean> assignedEntityData(LivingEntity entity) {
        TrackedData<Boolean> inHarmony = ServersideDataTracker.registerData(entity.getClass(), TrackedDataHandlerRegistry.BOOLEAN);
        if(!livingEntityDataSet.containsKey(entity.getClass())) livingEntityDataSet.put(entity.getClass(), inHarmony);
        return inHarmony;
    }
    
    public static void initNonPlayerData(LivingEntity entity, ServersideDataTracker.Builder builder) {
        TrackedData<Boolean> inHarmony = livingEntityDataSet.get(entity.getClass());
        if(inHarmony == null) inHarmony = assignedEntityData(entity);
        builder.add(inHarmony, false);
    }

    public static void readNonPlayerData(LivingEntity entity, NbtCompound nbt) {
        TrackedData<Boolean> inHarmony = livingEntityDataSet.get(entity.getClass());
        ((ServerTrackedData) entity).getServersideDataTracker().set(inHarmony, nbt.getBoolean("inHarmony").orElse(false));
    }

    public static void writeNonPlayerData(LivingEntity entity, NbtCompound nbt) {
        TrackedData<Boolean> inHarmony = livingEntityDataSet.get(entity.getClass());
        nbt.putBoolean("inHarmony", ((ServerTrackedData) entity).getServersideDataTracker().get(inHarmony));
    }

    public static void grantOmnipotence(PlayerEntity player, boolean isCopyFrom) {
        ((ServerTrackedData) player).getServersideDataTracker().set(playerData.IS_OMNIPOTENT(), true);
        if(player.getWorld() instanceof ServerWorld serverWorld && !isCopyFrom) {
            player.sendMessage(Text.translatable("message.projectomnipotence.ascend").fillStyle(Style.EMPTY.withColor(Formatting.YELLOW)), false);
            serverWorld.spawnParticles(ParticleTypes.END_ROD, player.getX(), player.getY() + player.getBoundingBox().getLengthY() / 2, player.getZ(), 20, (Math.random() * player.getBoundingBox().getLengthX() / 2) * 0.5, (Math.random() * player.getBoundingBox().getLengthY() / 2) * 0.5, (Math.random() * player.getBoundingBox().getLengthZ() / 2) * 0.5, 0.075);
        }
        // Update on client
        if(player instanceof ServerPlayerEntity serverPlayer) {
            ServerPlayNetworking.send(serverPlayer, new SyncSSDHDataPayload(serverPlayer.getGameProfile(), isOmnipotent(player), getEntitiesEnlightened(player)));
        }
    }

    public static void revokeOmnipotence(PlayerEntity player) {
        ((ServerTrackedData) player).getServersideDataTracker().set(playerData.IS_OMNIPOTENT(), false);
        if(!player.getWorld().isClient()) player.sendMessage(Text.translatable("message.projectomnipotence.descend").fillStyle(Style.EMPTY.withColor(Formatting.YELLOW)), false);
        if(Main.CONFIG.omnipotentPlayersGlow && player.hasStatusEffect(StatusEffects.GLOWING)) player.removeStatusEffect(StatusEffects.GLOWING);
        boolean inSurvival = !player.isSpectator() && !enlightenedPlayerInCreative(player);
        if(Main.CONFIG.omnipotentPlayersCanGainFlight && getEntitiesEnlightened(player) >= Main.CONFIG.flightEntityGoal && inSurvival) {
            player.getAbilities().allowFlying = false;
            player.getAbilities().flying = false;
            player.sendAbilitiesUpdate();
        }

        // Update on client
        if(player instanceof ServerPlayerEntity serverPlayer) {
            ServerPlayNetworking.send(serverPlayer, new SyncSSDHDataPayload(serverPlayer.getGameProfile(), isOmnipotent(player), getEntitiesEnlightened(player)));
        }
    }

    public static boolean isOmnipotent(PlayerEntity player) {
        return ((ServerTrackedData) player).getServersideDataTracker().get(playerData.IS_OMNIPOTENT());
    }

    public static boolean isOmnipotentClient(PlayerEntity player) {
        NbtCompound nbt = new NbtCompound();
        player.writeNbt(nbt);
        return nbt.getBoolean("isOmnipotent").orElse(false);
    }

    public static int getEntitiesEnlightened(PlayerEntity player) {
        return ((ServerTrackedData) player).getServersideDataTracker().get(playerData.ENTITIES_ENLIGHTENED());
    }

    public static int getEntitiesEnlightenedClient(PlayerEntity player) {
        NbtCompound nbt = new NbtCompound();
        player.writeNbt(nbt);
        return nbt.getInt("EntitiesEnlightened").orElse(0);
    }

    public static void setEntitiesEnlightened(PlayerEntity player, int value) {
        ((ServerTrackedData) player).getServersideDataTracker().set(playerData.ENTITIES_ENLIGHTENED(), value);
        boolean inSurvival = !player.isSpectator() && !enlightenedPlayerInCreative(player);
        if(Main.CONFIG.omnipotentPlayersCanGainFlight && getEntitiesEnlightened(player) < Main.CONFIG.flightEntityGoal && inSurvival) {
            player.getAbilities().allowFlying = false;
            player.getAbilities().flying = false;
            player.sendAbilitiesUpdate();
        }
    }

    public static void harmonizeEntity(LivingEntity livingEntity, @Nullable PlayerEntity playerAttacker, DamageSource source) {
        if (livingEntity.getWorld() instanceof ServerWorld serverWorld && !Main.CONFIG.enlightenmentBlackList.contains(Registries.ENTITY_TYPE.getId(livingEntity.getType()).toString()) && !Main.CONFIG.enlightenmentBlackList.contains("*")) {
            livingEntity.setAttacking(playerAttacker, 100);
            livingEntity.dropExperience(serverWorld, playerAttacker);
            livingEntity.dropLoot(serverWorld, source, true);
            forceDropEquipment(livingEntity, serverWorld, livingEntity instanceof MobEntity mob ? (stack) -> {
                ItemStack copy = stack.copy();
                mob.dropStack(serverWorld, copy);
            } : (stack) -> {});

            if(livingEntity.getType() == EntityType.CREEPER && playerAttacker != null) {
                int chance = livingEntity.getRandom().nextBetween(0, Math.max(0, 10 - (int)playerAttacker.getAttributes().getValue(EntityAttributes.LUCK) * 2));
                if(chance == 0) livingEntity.dropStack(serverWorld, new ItemStack(discs[livingEntity.getRandom().nextBetween(0, discs.length - 1)]));
            }

            if(livingEntity.getType() == EntityType.GOAT && playerAttacker != null) {
                int chance = livingEntity.getRandom().nextBetween(0, Math.max(0, 8 - (int)playerAttacker.getAttributes().getValue(EntityAttributes.LUCK) * 2));
                if(chance == 0) {
                    ItemStack goatHorn = new ItemStack(Items.GOAT_HORN);

                    // Should always work but catch just in case something goes wrong
                    try {
                        GoatEntity goat = (GoatEntity) livingEntity;
                        goatHorn = goat.getGoatHornStack();
                    }
                    catch (Exception ignored) {}

                    livingEntity.dropStack(serverWorld, goatHorn);
                }
            }

            livingEntity.setAttacking((PlayerEntity) null, 0);
            if(livingEntity instanceof MobEntity mob) {
                mob.setCanPickUpLoot(false);
                mob.setTarget(null);
                mob.targetSelector.clear(goal -> goal instanceof ActiveTargetGoal<?> || goal instanceof RevengeGoal);
            }

            if(Main.CONFIG.removeOnEnlightenList.contains(Registries.ENTITY_TYPE.getId(livingEntity.getType()).toString()) || Main.CONFIG.removeOnEnlightenList.contains("*")) {
                livingEntity.setSilent(true);
                livingEntity.damage(serverWorld, livingEntity.getDamageSources().outOfWorld(), Float.MAX_VALUE);
                livingEntity.remove(Entity.RemovalReason.DISCARDED);
                livingEntity.getWorld().playSound(null, livingEntity.getX(), livingEntity.getY(), livingEntity.getZ(), SoundEvents.ENTITY_EVOKER_PREPARE_SUMMON, SoundCategory.MASTER, 1, 2);
            }

            setInHarmony(livingEntity, true);
            if(playerAttacker != null) setEntitiesEnlightened(playerAttacker, getEntitiesEnlightened(playerAttacker) + 1);
            serverWorld.spawnParticles(ParticleTypes.END_ROD, livingEntity.getX(), livingEntity.getY() + livingEntity.getBoundingBox().getLengthY() / 2, livingEntity.getZ(), 20, (Math.random() * livingEntity.getBoundingBox().getLengthX() / 2) * 0.5, (Math.random() * livingEntity.getBoundingBox().getLengthY() / 2) * 0.5, (Math.random() * livingEntity.getBoundingBox().getLengthZ() / 2) * 0.5, 0.075);
        }
    }


    public static void harmonizeEntityByBeacon(LivingEntity livingEntity, @Nullable PlayerEntity playerAttacker, BlockPos beaconPos) {
        if (livingEntity.getWorld() instanceof ServerWorld serverWorld && !Main.CONFIG.enlightenmentBlackList.contains(Registries.ENTITY_TYPE.getId(livingEntity.getType()).toString()) && !Main.CONFIG.enlightenmentBlackList.contains("*")) {
            forceDropEquipment(livingEntity, serverWorld, (stack) -> {
                ItemStack copy = stack.copy();
                ItemEntity itemEntity = new ItemEntity(serverWorld, beaconPos.getX() + 0.5, beaconPos.up().getY(), beaconPos.getZ() + 0.5, copy);
                itemEntity.setToDefaultPickupDelay();
                itemEntity.setVelocity(0 ,0 ,0);
                serverWorld.spawnEntity(itemEntity);
            });
            if(playerAttacker != null) {
                playerAttacker.addExperience(livingEntity.getExperienceToDrop(serverWorld, playerAttacker));
                processLootTableDrops(livingEntity, serverWorld, serverWorld.getDamageSources().playerAttack(playerAttacker), playerAttacker, (stack) -> {
                    ItemEntity itemEntity = new ItemEntity(serverWorld, beaconPos.getX() + 0.5, beaconPos.up().getY(), beaconPos.getZ() + 0.5, stack);
                    itemEntity.setToDefaultPickupDelay();
                    itemEntity.setVelocity(0 ,0 ,0);
                    serverWorld.spawnEntity(itemEntity);
                });
            }

            livingEntity.setAttacking((PlayerEntity) null, 0);
            if(livingEntity instanceof MobEntity mob) {
                mob.setCanPickUpLoot(false);
                mob.setTarget(null);
                mob.targetSelector.clear(goal -> goal instanceof ActiveTargetGoal<?> || goal instanceof RevengeGoal);
            }

            String entityID = Registries.ENTITY_TYPE.getId(livingEntity.getType()).toString();

            if(Main.CONFIG.removeOnEnlightenList.contains(entityID) || Main.CONFIG.removeOnEnlightenList.contains("*")) {
                livingEntity.setSilent(true);
                livingEntity.damage(serverWorld, livingEntity.getDamageSources().outOfWorld(), Float.MAX_VALUE);
                livingEntity.remove(Entity.RemovalReason.DISCARDED);
                livingEntity.getWorld().playSound(null, livingEntity.getX(), livingEntity.getY(), livingEntity.getZ(), SoundEvents.ENTITY_EVOKER_PREPARE_SUMMON, SoundCategory.MASTER, 1, 2);
            }

            if(Main.CONFIG.convertUponEnlightened.containsKey(entityID)) {
                EntityType<?> conversionType = Registries.ENTITY_TYPE.get(Identifier.of(Main.CONFIG.convertUponEnlightened.get(entityID)));
                Entity e = conversionType.create(serverWorld, SpawnReason.CONVERSION);

                if(livingEntity instanceof MobEntity mob && e instanceof MobEntity) {
                    POEntityConversionHelper helper = finalizers.get(mob.getType());
                    if(helper != null) e = helper.convertEntity(mob);
                    else {
                        EntityType<? extends MobEntity> tMobType = (EntityType<? extends MobEntity>) e.getType();
                        e = mob.convertTo(tMobType, new EntityConversionContext(EntityConversionType.SINGLE, true, true, mob.getScoreboardTeam()), SpawnReason.CONVERSION, (newMob) -> {});
                    }
                    if(e instanceof LivingEntity tle) setInHarmony(tle, true);
                }
                else if(e != null) {
                    serverWorld.spawnEntity(e);
                    livingEntity.setSilent(true);
                    livingEntity.remove(Entity.RemovalReason.DISCARDED);
                    serverWorld.playSound(null, livingEntity.getX(), livingEntity.getY(), livingEntity.getZ(), SoundEvents.ENTITY_EVOKER_PREPARE_SUMMON, SoundCategory.MASTER, 1, 2);
                }
            }

            setInHarmony(livingEntity, true);
            if(playerAttacker != null) setEntitiesEnlightened(playerAttacker, getEntitiesEnlightened(playerAttacker) + 1);
            serverWorld.spawnParticles(ParticleTypes.END_ROD, livingEntity.getX(), livingEntity.getY() + livingEntity.getBoundingBox().getLengthY() / 2, livingEntity.getZ(), 20, (Math.random() * livingEntity.getBoundingBox().getLengthX() / 2) * 0.5, (Math.random() * livingEntity.getBoundingBox().getLengthY() / 2) * 0.5, (Math.random() * livingEntity.getBoundingBox().getLengthZ() / 2) * 0.5, 0.075);
        }
    }

    public static boolean isInHarmony(Entity entity) {
        if(entity instanceof PlayerEntity player) return isOmnipotent(player);
        else if(entity instanceof LivingEntity) {
            try {
                return ((ServerTrackedData) entity).getServersideDataTracker().get(livingEntityDataSet.get(entity.getClass()));
            }
            catch (Exception e) {
                // If for some reason the tracked data is not initialized for the entity
                e.printStackTrace();
                return false;
            }
        }
        else return false;
    }

    public static void setInHarmony(LivingEntity entity, boolean value) {
        if(entity.getType() != EntityType.PLAYER) {
            try {
                ((ServerTrackedData) entity).getServersideDataTracker().set(livingEntityDataSet.get(entity.getClass()), value);
            }
            catch (Exception e) {
                // If for some reason the tracked data is not initialized for the entity
                e.printStackTrace();
            }
        }
    }

    private static void processLootTableDrops(LivingEntity target, ServerWorld world, DamageSource damageSource, @Nullable PlayerEntity playerAttacker, Consumer<ItemStack> callback) {
        Optional<RegistryKey<LootTable>> optional = target.getLootTableKey();
        if (optional.isPresent()) {
            LootTable lootTable = world.getServer().getReloadableRegistries().getLootTable(optional.get());
            LootWorldContext.Builder builder = (new LootWorldContext.Builder(world)).add(LootContextParameters.THIS_ENTITY, target).add(LootContextParameters.ORIGIN, target.getPos()).add(LootContextParameters.DAMAGE_SOURCE, damageSource).addOptional(LootContextParameters.ATTACKING_ENTITY, damageSource.getAttacker()).addOptional(LootContextParameters.DIRECT_ATTACKING_ENTITY, damageSource.getSource());
            if (playerAttacker != null) {
                builder = builder.add(LootContextParameters.LAST_DAMAGE_PLAYER, playerAttacker).luck(playerAttacker.getLuck());
            }

            LootWorldContext lootWorldContext = builder.build(LootContextTypes.ENTITY);
            lootTable.generateLoot(lootWorldContext, target.getLootTableSeed(), callback);
        }
    }

    public static boolean isTrueEnlightened(PlayerEntity player) {
        return trueEnlightened.contains(player.getUuid());
    }

    public static void spawnEnlightenmentParticles(Entity entity, ServerWorld server) {
        double deltaX = Math.min(entity.getBoundingBox().getLengthX(), Math.max(-entity.getBoundingBox().getLengthX(), (Math.random() * entity.getBoundingBox().getLengthX() / 2) * 1.25));
        double deltaY = Math.min(entity.getBoundingBox().getLengthY() / 3, Math.max(-entity.getBoundingBox().getLengthY() / 3, (Math.random() * entity.getBoundingBox().getLengthY() / 3) * 1.25));
        double deltaZ = Math.min(entity.getBoundingBox().getLengthZ(), Math.max(-entity.getBoundingBox().getLengthZ(), (Math.random() * entity.getBoundingBox().getLengthZ() / 2) * 1.25));

        for(ServerPlayerEntity player : server.getPlayers()) {
            if(entity.getUuid() != player.getUuid() || Main.CONFIG.omnipotentPlayerParticlesLocal)
                server.spawnParticles(player, ParticleTypes.END_ROD, false, false, entity.getX(), entity.getY() + entity.getBoundingBox().getLengthY() / 2, entity.getZ(), 1, deltaX, deltaY, deltaZ, 0);
        }
    }

    public static void spawnEnlightenmentParticlesClient(ClientPlayerEntity player, ClientWorld world) {

        double deltaX = Math.min(player.getBoundingBox().getLengthX(), Math.max(-player.getBoundingBox().getLengthX(), (Math.random() * player.getBoundingBox().getLengthX() / 2) * 1.25));
        double deltaY = Math.min(player.getBoundingBox().getLengthY() / 3, Math.max(-player.getBoundingBox().getLengthY() / 3, (Math.random() * player.getBoundingBox().getLengthY() / 3) * 1.25));
        double deltaZ = Math.min(player.getBoundingBox().getLengthZ(), Math.max(-player.getBoundingBox().getLengthZ(), (Math.random() * player.getBoundingBox().getLengthZ() / 2) * 1.25));

        Random random = world.random;

        double g = random.nextGaussian() * deltaX;
        double h = random.nextGaussian() * deltaY;
        double j = random.nextGaussian() * deltaZ;

        if(MinecraftClient.getInstance().gameRenderer.getCamera().isThirdPerson() || MinecraftClient.getInstance().cameraEntity != player) {
            world.addParticleClient(ParticleTypes.END_ROD, false, true, player.getX() + g, player.getY() + player.getBoundingBox().getLengthY() / 2 + h, player.getZ() + j, 0, 0, 0);
        }
    }

    public static boolean enlightenedPlayerInCreative(PlayerEntity player) {
        if(!player.getWorld().isClient() && player instanceof ServerPlayerEntity serverPlayer) {
            return serverPlayer.interactionManager.isCreative();
        }
        else if(player.getWorld().isClient() && player instanceof AbstractClientPlayerEntity clientPlayer) {
            PlayerListEntry playerListEntry = clientPlayer.getPlayerListEntry();
            return playerListEntry != null && playerListEntry.getGameMode().isCreative();
        }
        else return false;
    }

    public static int getLuckLevel(PlayerEntity player) {
        return (int) Math.min(Main.CONFIG.totalLuckLevels, Math.floor(getEntitiesEnlightened(player) / (double) Main.CONFIG.luckLevelEntityGoal));
    }

    public static void forceDropEquipment(LivingEntity entity, World world, Consumer<ItemStack> callback) {
        if(world instanceof ServerWorld && entity.getType() != EntityType.PLAYER) {
            if(entity instanceof MobEntity mob) {
                for (EquipmentSlot slot : EquipmentSlot.VALUES) {
                    ItemStack stack = mob.getEquippedStack(slot);
                    callback.accept(stack);
                    mob.getEquippedStack(slot).setCount(0);
                }
            }
        }
    }

    public static void respawnPlayer(ServerPlayerEntity player) {
        // Sanity check
        if (!player.getWorld().isClient()) {
            ServerPlayerEntity.Respawn respawn = player.getRespawn();


            BlockPos pos = respawn != null ? respawn.pos() : player.getServerWorld().getSpawnPos();
            RegistryKey<World> key = respawn != null ? respawn.dimension() : player.getServerWorld().getRegistryKey();

            MinecraftServer server = player.getServer();
            if (server != null) {
                ServerWorld world = player.getServer().getWorld(key);

                if (world != null) {
                    if (pos == null) pos = world.getSpawnPos();

                    Optional<Vec3d> finalPos = findRespawnPosition(world, pos, respawn != null ? respawn.angle() : 0.0F, true, true);
                    BlockPos finalPos1 = pos;

                    player.fallDistance = 0.0F;
                    finalPos.ifPresentOrElse(vec3d -> player.teleport(world, vec3d.x, vec3d.y, vec3d.z, PositionFlag.ROT, player.getYaw(), player.getPitch(), false), () -> player.teleport(world, finalPos1.getX(), finalPos1.getY() + 1, finalPos1.getZ(), PositionFlag.ROT, player.getYaw(), player.getPitch(), false));
                }
            }
        }
    }

    private static Optional<Vec3d> findRespawnPosition(ServerWorld world, BlockPos pos, float spawnAngle, boolean spawnForced, boolean alive) {
        BlockState blockState = world.getBlockState(pos);
        Block block = blockState.getBlock();
        if (block instanceof RespawnAnchorBlock && (spawnForced || blockState.get(RespawnAnchorBlock.CHARGES) > 0) && RespawnAnchorBlock.isNether(world)) {
            Optional<Vec3d> optional = RespawnAnchorBlock.findRespawnPosition(EntityType.PLAYER, world, pos);
            if (!spawnForced && !alive && optional.isPresent()) {
                world.setBlockState(pos, blockState.with(RespawnAnchorBlock.CHARGES, blockState.get(RespawnAnchorBlock.CHARGES) - 1), Block.NOTIFY_ALL);
            }
            return optional;
        }
        if (block instanceof BedBlock && BedBlock.isBedWorking(world)) {
            return BedBlock.findWakeUpPosition(EntityType.PLAYER, world, pos, blockState.get(BedBlock.FACING), spawnAngle);
        }
        if (!spawnForced) {
            return Optional.empty();
        }
        boolean bl = block.canMobSpawnInside(blockState);
        BlockState blockState2 = world.getBlockState(pos.up());
        boolean bl2 = blockState2.getBlock().canMobSpawnInside(blockState2);
        if (bl && bl2) {
            return Optional.of(new Vec3d((double)pos.getX() + 0.5, (double)pos.getY() + 0.1, (double)pos.getZ() + 0.5));
        }
        return Optional.empty();
    }

}
