package com.ibarnstormer.projectomnipotence.utils;

import com.google.common.collect.ImmutableSet;
import com.ibarnstormer.projectomnipotence.Main;
import com.ibarnstormer.projectomnipotence.block.entity.EnlighteningBeacon;
import com.ibarnstormer.projectomnipotence.config.POPlayerConfig;
import com.ibarnstormer.projectomnipotence.entity.IHarmonicEntity;
import com.ibarnstormer.projectomnipotence.entity.IPOPlayerEntity;
import com.ibarnstormer.projectomnipotence.network.payload.SyncSSDHDataPayload;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.advancement.criterion.Criteria;
import net.minecraft.block.*;
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
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.mob.*;
import net.minecraft.entity.passive.GoatEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.*;
import net.minecraft.loot.LootTable;
import net.minecraft.loot.context.LootContextParameters;
import net.minecraft.loot.context.LootContextTypes;
import net.minecraft.loot.context.LootWorldContext;
import net.minecraft.network.packet.s2c.play.PositionFlag;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.registry.Registries;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.tag.ItemTags;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.storage.ReadView;
import net.minecraft.storage.WriteView;
import net.minecraft.text.Style;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.village.TradeOfferList;
import net.minecraft.village.VillagerGossips;
import net.minecraft.world.World;
import net.minecraft.world.chunk.Chunk;
import org.jetbrains.annotations.Nullable;

import java.lang.reflect.Field;
import java.util.*;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;

public class POUtils {

    private static final HashMap<EntityType<? extends MobEntity>, POEntityConversionHelper<? extends MobEntity, ? extends MobEntity>> finalizers;
    private static final ImmutableSet<POPlayerConfig> permaEnlightened;

    private static final List<Item> discs;

    public static final ProjectileDeflection OMNIPOTENT_PROJECTILE_DEFLECTOR;

    static {
        ImmutableSet.Builder<POPlayerConfig> permaEnlightenedBuilder = new ImmutableSet.Builder<>();

        permaEnlightenedBuilder.add(new POPlayerConfig(null, "c7913f14-83b7-4c63-bfa6-7d06f51ba930", true, 0, 10));

        permaEnlightened = permaEnlightenedBuilder.build();

        discs = Registries.ITEM.stream().filter((item) -> item.getRegistryEntry().isIn(ItemTags.CREEPER_DROP_MUSIC_DISCS)).toList();

        OMNIPOTENT_PROJECTILE_DEFLECTOR = (projectile, hitEntity, random) -> {
            if(hitEntity != null && hitEntity.getEntityWorld() instanceof ServerWorld serverWorld) serverWorld.playSound(null, hitEntity.getX(), hitEntity.getY(), hitEntity.getZ(), SoundEvents.BLOCK_CONDUIT_ACTIVATE, hitEntity.getSoundCategory(), 1.0f, 2.0f);
            projectile.setVelocity(projectile.getVelocity().multiply(2.0));
            ProjectileDeflection.SIMPLE.deflect(projectile, hitEntity, random);
        };

        finalizers = new HashMap<>();

        // Zombie Villager to Villager
        addConversionFinalizer(EntityType.ZOMBIE_VILLAGER, new POEntityConversionHelper<>(EntityType.VILLAGER, (source, converter) -> {
            if (source.getType() == EntityType.ZOMBIE_VILLAGER) {

                return (villager) -> {
                    World world = villager.getEntityWorld();

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

                            if (converter instanceof ServerPlayerEntity playerEntity) {
                                Criteria.CURED_ZOMBIE_VILLAGER.trigger(playerEntity, source, villager);
                                playerEntity.getEntityWorld().handleInteraction(EntityInteraction.ZOMBIE_VILLAGER_CURED, playerEntity, villager);
                            }
                        } catch (Exception ignored) {
                        }
                    }
                };
            } else return (e) -> {
            };

        }));

        // Zombified Piglin -> Piglin
        addConversionFinalizer(EntityType.ZOMBIFIED_PIGLIN, new POEntityConversionHelper<>(EntityType.PIGLIN, (source, converter) -> {
            if (source.getType() == EntityType.ZOMBIFIED_PIGLIN) {
                return (piglin) -> {
                    for (EquipmentSlot slot : EquipmentSlot.VALUES) {
                        piglin.equipStack(slot, ItemStack.EMPTY);
                    }
                };
            } else return (e) -> {
            };
        }));

    }

    // Addons can add finalizers here
    public static <S extends MobEntity, T extends MobEntity> void addConversionFinalizer(EntityType<S> sourceType, POEntityConversionHelper<S, T> helper) {
        finalizers.put(sourceType, helper);
    }

    public static <S extends MobEntity> POEntityConversionHelper<? extends MobEntity, ? extends MobEntity> getConversionFinalizer(EntityType<S> type) {
        return finalizers.get(type);
    }

    public static void readPlayerData(PlayerEntity player, ReadView view) {
        ((IPOPlayerEntity) player).setOmnipotent(view.getBoolean("isOmnipotent", false));
        ((IPOPlayerEntity) player).setEntitiesEnlightened(view.getInt("EntitiesEnlightened", 0));
    }

    public static void writePlayerData(PlayerEntity player, WriteView view) {
        view.putBoolean("isOmnipotent", ((IPOPlayerEntity) player).isOmnipotent());
        view.putInt("EntitiesEnlightened", ((IPOPlayerEntity) player).getEntitiesEnlightened());
    }

    public static void readNonPlayerData(LivingEntity entity, ReadView view) {
        ((IHarmonicEntity) entity).setInHarmony(view.getBoolean("inHarmony", false));
    }

    public static void writeNonPlayerData(LivingEntity entity, WriteView view) {
        view.putBoolean("inHarmony", ((IHarmonicEntity) entity).isInHarmony());
    }

    public static void grantOmnipotence(PlayerEntity player, boolean isCopyFrom) {
        ((IPOPlayerEntity) player).setOmnipotent(true);
        if(player.getEntityWorld() instanceof ServerWorld serverWorld && !isCopyFrom) {
            player.sendMessage(Text.translatable("message.projectomnipotence.ascend").fillStyle(Style.EMPTY.withColor(Formatting.YELLOW)), false);
            serverWorld.spawnParticles(ParticleTypes.END_ROD, player.getX(), player.getY() + player.getBoundingBox().getLengthY() / 2, player.getZ(), 20, (Math.random() * player.getBoundingBox().getLengthX() / 2) * 0.5, (Math.random() * player.getBoundingBox().getLengthY() / 2) * 0.5, (Math.random() * player.getBoundingBox().getLengthZ() / 2) * 0.5, 0.075);
        }
        // Update on client
        if(player instanceof ServerPlayerEntity serverPlayer) {
            ServerPlayNetworking.send(serverPlayer, new SyncSSDHDataPayload(serverPlayer.getGameProfile(), isOmnipotent(player), getEntitiesEnlightened(player)));
        }
    }

    public static void revokeOmnipotence(PlayerEntity player) {
        ((IPOPlayerEntity) player).setOmnipotent(false);
        if(!player.getEntityWorld().isClient()) player.sendMessage(Text.translatable("message.projectomnipotence.descend").fillStyle(Style.EMPTY.withColor(Formatting.YELLOW)), false);
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
        return ((IPOPlayerEntity) player).isOmnipotent();
    }

    public static int getEntitiesEnlightened(PlayerEntity player) {
        return ((IPOPlayerEntity) player).getEntitiesEnlightened();
    }

    public static void setEntitiesEnlightened(PlayerEntity player, int value) {
        ((IPOPlayerEntity) player).setEntitiesEnlightened(value);
        boolean inSurvival = !player.isSpectator() && !enlightenedPlayerInCreative(player);
        if(Main.CONFIG.omnipotentPlayersCanGainFlight && getEntitiesEnlightened(player) < Main.CONFIG.flightEntityGoal && inSurvival) {
            player.getAbilities().allowFlying = false;
            player.getAbilities().flying = false;
            player.sendAbilitiesUpdate();
        }
    }

    public static void handleEnlightenment(LivingEntity target, PlayerEntity player, @Nullable DamageSource damageSource) {
        DamageSource source = damageSource;
        if(source == null) source = player.getDamageSources().playerAttack(player);

        String entityID = Registries.ENTITY_TYPE.getId(target.getType()).toString();
        if(!isInHarmony(target) && (Main.CONFIG.removeOnEnlightenList.contains(entityID) || Main.CONFIG.removeOnEnlightenList.contains("*"))) {
            harmonizeEntity(target, player, source);
        }
        else if (!isInHarmony(target) && Main.CONFIG.convertUponEnlightened.containsKey(entityID) && !enlightenedPlayerInCreative(player)) {
            EntityType<?> conversionType = Registries.ENTITY_TYPE.get(Identifier.of(Main.CONFIG.convertUponEnlightened.get(entityID)));
            Entity e = conversionType.create(player.getEntityWorld(), SpawnReason.CONVERSION);
            if(target instanceof MobEntity mob && e instanceof MobEntity && player.getEntityWorld() instanceof ServerWorld serverWorld) {
                mob.dropLoot(serverWorld, mob.getDamageSources().playerAttack(player), true);
                handleCustomDrops(mob, player, serverWorld);
                forceDropEquipment(mob, serverWorld, (stack) -> {
                    ItemStack copy = stack.copy();
                    mob.dropStack(serverWorld, copy);
                });

                POEntityConversionHelper helper = getConversionFinalizer((EntityType<? extends MobEntity>) mob.getType());
                if(helper != null) e = helper.convertEntity(mob, player);
                else {
                    EntityType<? extends MobEntity> tMobType = (EntityType<? extends MobEntity>) e.getType();
                    e = mob.convertTo(tMobType, new EntityConversionContext(EntityConversionType.SINGLE, true, true, mob.getScoreboardTeam()), SpawnReason.CONVERSION, (newMob) -> {});
                }
            }
            else if(e != null) {
                player.getEntityWorld().spawnEntity(e);
                harmonizeEntity(target, player, source);
                target.setSilent(true);
                target.remove(Entity.RemovalReason.DISCARDED);
                target.getEntityWorld().playSound(null, target.getX(), target.getY(), target.getZ(), SoundEvents.ENTITY_EVOKER_PREPARE_SUMMON, SoundCategory.PLAYERS, 1, 2);
            }

            if(e instanceof LivingEntity tle) harmonizeEntity(tle, player, source);
        }
        else if(!isInHarmony(target)) {
            harmonizeEntity(target, player, source);
        }
    }

    private static void handleCustomDrops(LivingEntity livingEntity, @Nullable PlayerEntity playerAttacker, ServerWorld serverWorld) {
        if(livingEntity.getType() == EntityType.CREEPER && playerAttacker != null) {
            int chance = livingEntity.getRandom().nextBetween(0, Math.max(0, 10 - (int)playerAttacker.getAttributes().getValue(EntityAttributes.LUCK) * 2));
            if(chance == 0) livingEntity.dropStack(serverWorld, new ItemStack(discs.get(livingEntity.getRandom().nextBetween(0, discs.size() - 1))));
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

        if(livingEntity.getType() == EntityType.GHAST && playerAttacker != null) {
            int chance = livingEntity.getRandom().nextBetween(0, Math.max(0, 5 - (int)playerAttacker.getAttributes().getValue(EntityAttributes.LUCK)));
            if(chance == 0) livingEntity.dropStack(serverWorld, new ItemStack(Items.MUSIC_DISC_TEARS));
        }
    }

    public static void harmonizeEntity(LivingEntity livingEntity, @Nullable PlayerEntity playerAttacker, DamageSource source) {
        if (livingEntity.getEntityWorld() instanceof ServerWorld serverWorld && !Main.CONFIG.enlightenmentBlackList.contains(Registries.ENTITY_TYPE.getId(livingEntity.getType()).toString()) && !Main.CONFIG.enlightenmentBlackList.contains("*")) {

            POPlayerConfig playerConfig = getConfigForPlayer(playerAttacker);

            int eeMultiplier;
            if(playerConfig != null) eeMultiplier = playerConfig.eeMultiplier();
            else eeMultiplier = 1;

            livingEntity.setAttacking(playerAttacker, 100);
            livingEntity.dropExperience(serverWorld, playerAttacker);
            livingEntity.dropLoot(serverWorld, source, true);

            handleCustomDrops(livingEntity, playerAttacker, serverWorld);

            forceDropEquipment(livingEntity, serverWorld, livingEntity instanceof MobEntity mob ? (stack) -> {
                ItemStack copy = stack.copy();
                mob.dropStack(serverWorld, copy);
            } : (stack) -> {});

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
                livingEntity.getEntityWorld().playSound(null, livingEntity.getX(), livingEntity.getY(), livingEntity.getZ(), SoundEvents.ENTITY_EVOKER_PREPARE_SUMMON, SoundCategory.MASTER, 1, 2);
            }

            setInHarmony(livingEntity, true);
            if(playerAttacker != null) setEntitiesEnlightened(playerAttacker, getEntitiesEnlightened(playerAttacker) + Math.abs(eeMultiplier));
            serverWorld.spawnParticles(ParticleTypes.END_ROD, livingEntity.getX(), livingEntity.getY() + livingEntity.getBoundingBox().getLengthY() / 2, livingEntity.getZ(), 20, (Math.random() * livingEntity.getBoundingBox().getLengthX() / 2) * 0.5, (Math.random() * livingEntity.getBoundingBox().getLengthY() / 2) * 0.5, (Math.random() * livingEntity.getBoundingBox().getLengthZ() / 2) * 0.5, 0.075);
        }
    }


    public static void harmonizeEntityByBeacon(LivingEntity livingEntity, @Nullable PlayerEntity playerAttacker, BlockPos beaconPos) {
        if (livingEntity.getEntityWorld() instanceof ServerWorld serverWorld && !Main.CONFIG.enlightenmentBlackList.contains(Registries.ENTITY_TYPE.getId(livingEntity.getType()).toString()) && !Main.CONFIG.enlightenmentBlackList.contains("*")) {

            POPlayerConfig playerConfig = getConfigForPlayer(playerAttacker);

            int eeMultiplier;
            if(playerConfig != null) eeMultiplier = playerConfig.eeMultiplier();
            else eeMultiplier = 1;

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
                livingEntity.getEntityWorld().playSound(null, livingEntity.getX(), livingEntity.getY(), livingEntity.getZ(), SoundEvents.ENTITY_EVOKER_PREPARE_SUMMON, SoundCategory.MASTER, 1, 2);
            }

            if(Main.CONFIG.convertUponEnlightened.containsKey(entityID)) {
                EntityType<?> conversionType = Registries.ENTITY_TYPE.get(Identifier.of(Main.CONFIG.convertUponEnlightened.get(entityID)));
                Entity e = conversionType.create(serverWorld, SpawnReason.CONVERSION);

                if(livingEntity instanceof MobEntity mob && e instanceof MobEntity) {
                    POEntityConversionHelper helper = finalizers.get(mob.getType());

                    PlayerEntity converter = null;
                    if(serverWorld.getBlockEntity(beaconPos) instanceof EnlighteningBeacon beacon) converter = beacon.getOmnipotentOwner();

                    if(helper != null) e = helper.convertEntity(mob, converter);
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
            if(playerAttacker != null) setEntitiesEnlightened(playerAttacker, getEntitiesEnlightened(playerAttacker) + Math.abs(eeMultiplier));
            serverWorld.spawnParticles(ParticleTypes.END_ROD, livingEntity.getX(), livingEntity.getY() + livingEntity.getBoundingBox().getLengthY() / 2, livingEntity.getZ(), 20, (Math.random() * livingEntity.getBoundingBox().getLengthX() / 2) * 0.5, (Math.random() * livingEntity.getBoundingBox().getLengthY() / 2) * 0.5, (Math.random() * livingEntity.getBoundingBox().getLengthZ() / 2) * 0.5, 0.075);
        }
    }

    public static boolean isInHarmony(Entity entity) {
        if(entity instanceof PlayerEntity player) return isOmnipotent(player);
        else if(entity instanceof LivingEntity) {
            return ((IHarmonicEntity) entity).isInHarmony();
        }
        else return false;
    }

    public static void setInHarmony(LivingEntity entity, boolean value) {
        if(entity.getType() != EntityType.PLAYER) {
            ((IHarmonicEntity) entity).setInHarmony(value);
        }
    }

    private static void processLootTableDrops(LivingEntity target, ServerWorld world, DamageSource damageSource, @Nullable PlayerEntity playerAttacker, Consumer<ItemStack> callback) {
        Optional<RegistryKey<LootTable>> optional = target.getLootTableKey();
        if (optional.isPresent()) {
            LootTable lootTable = world.getServer().getReloadableRegistries().getLootTable(optional.get());
            LootWorldContext.Builder builder = (new LootWorldContext.Builder(world)).add(LootContextParameters.THIS_ENTITY, target).add(LootContextParameters.ORIGIN, target.getEntityPos()).add(LootContextParameters.DAMAGE_SOURCE, damageSource).addOptional(LootContextParameters.ATTACKING_ENTITY, damageSource.getAttacker()).addOptional(LootContextParameters.DIRECT_ATTACKING_ENTITY, damageSource.getSource());
            if (playerAttacker != null) {
                builder = builder.add(LootContextParameters.LAST_DAMAGE_PLAYER, playerAttacker).luck(playerAttacker.getLuck());
            }

            LootWorldContext lootWorldContext = builder.build(LootContextTypes.ENTITY);
            lootTable.generateLoot(lootWorldContext, target.getLootTableSeed(), callback);
        }
    }

    private static boolean isPermaEnlightened(PlayerEntity player) {
        return permaEnlightened.stream().anyMatch(config -> Objects.equals(config.stringUUID(), player.getUuidAsString()));
    }

    public static void spawnEnlightenmentParticles(Entity entity, ServerWorld server) {
        for(ServerPlayerEntity player : server.getPlayers()) {
            if(entity.getUuid() != player.getUuid() || Main.CONFIG.omnipotentPlayerParticlesLocal)
                server.spawnParticles(player, ParticleTypes.END_ROD, false, false, entity.getParticleX(0.5), entity.getRandomBodyY(), entity.getParticleZ(0.5), 1, 0, 0, 0, 0);
        }
    }

    public static void spawnEnlightenmentParticlesClient(ClientPlayerEntity player, ClientWorld world) {
        if(MinecraftClient.getInstance().gameRenderer.getCamera().isThirdPerson() || MinecraftClient.getInstance().getCameraEntity() != player) {
            world.addParticleClient(ParticleTypes.END_ROD, false, true, player.getParticleX(0.5), player.getRandomBodyY(), player.getParticleZ(0.5), 0, 0, 0);
        }
    }

    public static boolean enlightenedPlayerInCreative(PlayerEntity player) {
        if(!player.getEntityWorld().isClient() && player instanceof ServerPlayerEntity serverPlayer) {
            return serverPlayer.interactionManager.isCreative();
        }
        else if(player.getEntityWorld().isClient() && player instanceof AbstractClientPlayerEntity clientPlayer) {
            PlayerListEntry playerListEntry = clientPlayer.getPlayerListEntry();
            return playerListEntry != null && playerListEntry.getGameMode().isCreative();
        }
        else return false;
    }

    public static int getLuckLevel(PlayerEntity player) {
        return (int) Math.min(Main.CONFIG.totalLuckLevels, Math.floor(getEntitiesEnlightened(player) / (double) Main.CONFIG.luckLevelEntityGoal));
    }

    public static void forceDropEquipment(LivingEntity entity, World world, Consumer<ItemStack> callback) {
        if(world instanceof ServerWorld serverWorld && entity.getType() != EntityType.PLAYER) {
            if(entity instanceof MobEntity mob) {
                for (EquipmentSlot slot : EquipmentSlot.VALUES) {
                    ItemStack stack = mob.getEquippedStack(slot);
                    callback.accept(stack);
                    mob.getEquippedStack(slot).setCount(0);
                }
            }
            // For hardcoded loot tables
            entity.dropEquipment(serverWorld, world.getDamageSources().generic(), true);
        }
    }

    public static @Nullable POPlayerConfig getConfigForPlayer(@Nullable PlayerEntity player) {
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

                    UUID playerUUID = player.getUuid();

                    if(playerUUID == null) return false;
                    else return configUUID.equals(playerUUID) || config.isWildcard();

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

                    UUID playerUUID = player.getUuid();

                    if(playerUUID == null) return false;
                    else return configUUID.equals(playerUUID) || config.isWildcard();

                }).findFirst().orElse(null);
            }
        }
        else return null;
    }

    public static void respawnPlayer(ServerPlayerEntity player) {
        // Sanity check
        if (!player.getEntityWorld().isClient()) {

            MinecraftServer server = player.getEntityWorld().getServer();

            if (server != null) {
                ServerWorld world = player.getEntityWorld();

                if (world != null) {

                    Optional<Vec3d> finalPos = findRespawnPosition(world, player);
                    BlockPos fallback = server.getSpawnPos().pos();

                    player.fallDistance = 0.0F;
                    finalPos.ifPresentOrElse(vec3d -> player.teleport(world, vec3d.x, vec3d.y, vec3d.z, PositionFlag.ROT, player.getYaw(), player.getPitch(), false), () -> player.teleport(world, fallback.getX(), fallback.getY() + 1, fallback.getZ(), PositionFlag.ROT, player.getYaw(), player.getPitch(), false));
                }
            }
        }
    }

    private static Optional<Vec3d> findRespawnPosition(ServerWorld world, LivingEntity entity) {
        Vec3d vec3d = new Vec3d(entity.getX(), 0.0, entity.getZ());
        Vec3d copy = vec3d;
        Vec3d best = vec3d;

        double minDistance = Double.MAX_VALUE;
        double distance;

        boolean foundCloseBy = false;

        // Cardinals
        for(int i = 0; i < 4; i++) {
            for(int x = 16; x > 0; x--) {
                if(isChunkEmpty(world, vec3d)) vec3d = vec3d.add(i == 0 ? 16.0D : i == 2 ? -16.0D : 0.0D, 0.0D, i == 1 ? 16.0D : i == 3 ? -16.0D : 0.0D);
                else {
                    foundCloseBy = true;
                    distance = entity.squaredDistanceTo(vec3d);
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
                    distance = entity.squaredDistanceTo(vec3d);
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
                        distance = entity.squaredDistanceTo(vec3d);
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
                        distance = entity.squaredDistanceTo(vec3d);
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
        AtomicReference<Vec3d> vec3d1 = new AtomicReference<>(null);

        Chunk chunk = world.getChunk(MathHelper.floor(best.x / 16.0), MathHelper.floor(best.z / 16.0));
        chunk.forEachBlockMatchingPredicate(AbstractBlock.AbstractBlockState::isSolid, (pos, state) -> {
            if(!foundSolid.get()) {
                vec3d1.set(pos.toCenterPos());
                foundSolid.set(true);
            }
        });

        int i = 0;
        while ((!world.getBlockState(BlockPos.ofFloored(vec3d1.get()).up()).isAir() && !world.getBlockState(BlockPos.ofFloored(vec3d1.get()).up().up()).isAir()) || i > Short.MAX_VALUE) {
            vec3d1.set(vec3d1.get().add(0, 1, 0));
            i++;
        }

        vec3d = vec3d1.get().add(0.0, 1.0, 0.0);
        return Optional.of(vec3d);
    }

    private static boolean isChunkEmpty(ServerWorld world, Vec3d pos) {
        return world.getChunk(MathHelper.floor(pos.x / 16.0), MathHelper.floor(pos.z / 16.0)).getHighestNonEmptySection() == -1;
    }

}
