package com.ibarnstormer.projectomnipotence.utils;

import com.google.common.collect.ImmutableSet;
import com.ibarnstormer.projectomnipotence.Main;
import com.ibarnstormer.projectomnipotence.block.entity.EnlighteningBeacon;
import com.ibarnstormer.projectomnipotence.config.POPlayerConfig;
import com.ibarnstormer.projectomnipotence.entity.IHarmonicEntity;
import com.ibarnstormer.projectomnipotence.entity.IPOPlayerEntity;
import com.ibarnstormer.projectomnipotence.network.payload.SyncSSDHDataPayload;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.ChatFormatting;
import net.minecraft.advancements.triggers.CriteriaTriggers;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.multiplayer.PlayerInfo;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.ItemTags;
import net.minecraft.util.Mth;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.ai.gossip.GossipContainer;
import net.minecraft.world.entity.ai.village.ReputationEventType;
import net.minecraft.world.entity.animal.goat.Goat;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.ProjectileDeflection;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.trading.MerchantOffers;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

import java.lang.reflect.Field;
import java.util.*;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;

public class POUtils {

    private static final HashMap<EntityType<? extends Mob>, POEntityConversionHelper<? extends Mob, ? extends Mob>> finalizers;
    private static final ImmutableSet<POPlayerConfig> permaEnlightened;

    private static final List<Item> discs;

    public static final ProjectileDeflection OMNIPOTENT_PROJECTILE_DEFLECTOR;

    static {
        ImmutableSet.Builder<POPlayerConfig> permaEnlightenedBuilder = new ImmutableSet.Builder<>();

        permaEnlightenedBuilder.add(new POPlayerConfig(null, "c7913f14-83b7-4c63-bfa6-7d06f51ba930", true, 0, 10));

        permaEnlightened = permaEnlightenedBuilder.build();

        discs = BuiltInRegistries.ITEM.stream().filter((item) -> item.builtInRegistryHolder().is(ItemTags.CREEPER_DROP_MUSIC_DISCS)).toList();

        OMNIPOTENT_PROJECTILE_DEFLECTOR = (projectile, hitEntity, random) -> {
            if(hitEntity != null && hitEntity.level() instanceof ServerLevel serverWorld) serverWorld.playSound(null, hitEntity.getX(), hitEntity.getY(), hitEntity.getZ(), SoundEvents.CONDUIT_ACTIVATE, hitEntity.getSoundSource(), 1.0f, 2.0f);
            projectile.setDeltaMovement(projectile.getDeltaMovement().scale(2.0));
            ProjectileDeflection.REVERSE.deflect(projectile, hitEntity, random);
        };

        finalizers = new HashMap<>();

        // Zombie Villager to Villager
        addConversionFinalizer(EntityTypes.ZOMBIE_VILLAGER, new POEntityConversionHelper<>(EntityTypes.VILLAGER, (source, converter) -> {
            if (source.getType() == EntityTypes.ZOMBIE_VILLAGER) {

                return (villager) -> {
                    Level world = villager.level();

                    if (world instanceof ServerLevel serverWorld) {
                        try {
                            Field gossipData = source.getClass().getDeclaredField("gossip");
                            Field offerData = source.getClass().getDeclaredField("offerData");
                            Field experience = source.getClass().getDeclaredField("experience");

                            gossipData.setAccessible(true);
                            offerData.setAccessible(true);
                            experience.setAccessible(true);

                            villager.setVillagerData(source.getVillagerData());
                            if (gossipData.get(source) != null) {
                                villager.setGossips((GossipContainer) gossipData.get(source));
                            }

                            if (offerData.get(source) != null) {
                                villager.setOffers(((MerchantOffers) offerData.get(source)).copy());
                            }

                            villager.setVillagerXp(experience.getInt(source));
                            villager.finalizeSpawn(serverWorld, serverWorld.getCurrentDifficultyAt(villager.blockPosition()), EntitySpawnReason.CONVERSION, null);
                            villager.refreshBrain(serverWorld);

                            if (converter instanceof ServerPlayer playerEntity) {
                                CriteriaTriggers.CURED_ZOMBIE_VILLAGER.trigger(playerEntity, source, villager);
                                playerEntity.level().onReputationEvent(ReputationEventType.ZOMBIE_VILLAGER_CURED, playerEntity, villager);
                            }
                        } catch (Exception ignored) {
                        }
                    }
                };
            } else return (e) -> {
            };

        }));

        // Zombified Piglin -> Piglin
        addConversionFinalizer(EntityTypes.ZOMBIFIED_PIGLIN, new POEntityConversionHelper<>(EntityTypes.PIGLIN, (source, converter) -> {
            if (source.getType() == EntityTypes.ZOMBIFIED_PIGLIN) {
                return (piglin) -> {
                    for (EquipmentSlot slot : EquipmentSlot.VALUES) {
                        piglin.setItemSlot(slot, ItemStack.EMPTY);
                    }
                };
            } else return (e) -> {
            };
        }));

    }

    // Addons can add finalizers here
    public static <S extends Mob, T extends Mob> void addConversionFinalizer(EntityType<S> sourceType, POEntityConversionHelper<S, T> helper) {
        finalizers.put(sourceType, helper);
    }

    public static <S extends Mob> POEntityConversionHelper<? extends Mob, ? extends Mob> getConversionFinalizer(EntityType<S> type) {
        return finalizers.get(type);
    }

    public static void readPlayerData(Player player, ValueInput view) {
        ((IPOPlayerEntity) player).setOmnipotent(view.getBooleanOr("isOmnipotent", false));
        ((IPOPlayerEntity) player).setEntitiesEnlightened(view.getIntOr("EntitiesEnlightened", 0));
    }

    public static void writePlayerData(Player player, ValueOutput view) {
        view.putBoolean("isOmnipotent", ((IPOPlayerEntity) player).isOmnipotent());
        view.putInt("EntitiesEnlightened", ((IPOPlayerEntity) player).getEntitiesEnlightened());
    }

    public static void readNonPlayerData(LivingEntity entity, ValueInput view) {
        ((IHarmonicEntity) entity).setInHarmony(view.getBooleanOr("inHarmony", false));
    }

    public static void writeNonPlayerData(LivingEntity entity, ValueOutput view) {
        view.putBoolean("inHarmony", ((IHarmonicEntity) entity).isInHarmony());
    }

    public static void grantOmnipotence(Player player, boolean isCopyFrom) {
        ((IPOPlayerEntity) player).setOmnipotent(true);
        if(player.level() instanceof ServerLevel serverWorld && !isCopyFrom) {
            player.sendSystemMessage(Component.translatable("message.projectomnipotence.ascend").withStyle(Style.EMPTY.withColor(ChatFormatting.YELLOW)));
            serverWorld.sendParticles(ParticleTypes.END_ROD, player.getX(), player.getY() + player.getBoundingBox().getYsize() / 2, player.getZ(), 20, (Math.random() * player.getBoundingBox().getXsize() / 2) * 0.5, (Math.random() * player.getBoundingBox().getYsize() / 2) * 0.5, (Math.random() * player.getBoundingBox().getZsize() / 2) * 0.5, 0.075);
        }
        // Update on client
        if(player instanceof ServerPlayer serverPlayer) {
            ServerPlayNetworking.send(serverPlayer, new SyncSSDHDataPayload(serverPlayer.getGameProfile(), isOmnipotent(player), getEntitiesEnlightened(player)));
        }
    }

    public static void revokeOmnipotence(Player player) {
        ((IPOPlayerEntity) player).setOmnipotent(false);
        if(!player.level().isClientSide()) player.sendSystemMessage(Component.translatable("message.projectomnipotence.descend").withStyle(Style.EMPTY.withColor(ChatFormatting.YELLOW)));
        if(Main.CONFIG.omnipotentPlayersGlow && player.hasEffect(MobEffects.GLOWING)) player.removeEffect(MobEffects.GLOWING);
        boolean inSurvival = !player.isSpectator() && !enlightenedPlayerInCreative(player);
        if(Main.CONFIG.omnipotentPlayersCanGainFlight && getEntitiesEnlightened(player) >= Main.CONFIG.flightEntityGoal && inSurvival) {
            player.getAbilities().mayfly = false;
            player.getAbilities().flying = false;
            player.onUpdateAbilities();
        }

        // Update on client
        if(player instanceof ServerPlayer serverPlayer) {
            ServerPlayNetworking.send(serverPlayer, new SyncSSDHDataPayload(serverPlayer.getGameProfile(), isOmnipotent(player), getEntitiesEnlightened(player)));
        }
    }

    public static boolean isOmnipotent(Player player) {
        return ((IPOPlayerEntity) player).isOmnipotent();
    }

    public static int getEntitiesEnlightened(Player player) {
        return ((IPOPlayerEntity) player).getEntitiesEnlightened();
    }

    public static void setEntitiesEnlightened(Player player, int value) {
        ((IPOPlayerEntity) player).setEntitiesEnlightened(value);
        boolean inSurvival = !player.isSpectator() && !enlightenedPlayerInCreative(player);
        if(Main.CONFIG.omnipotentPlayersCanGainFlight && getEntitiesEnlightened(player) < Main.CONFIG.flightEntityGoal && inSurvival) {
            player.getAbilities().mayfly = false;
            player.getAbilities().flying = false;
            player.onUpdateAbilities();
        }
    }

    public static void handleEnlightenment(LivingEntity target, Player player, @Nullable DamageSource damageSource) {
        DamageSource source = damageSource;
        if(source == null) source = player.damageSources().playerAttack(player);

        String entityID = BuiltInRegistries.ENTITY_TYPE.getKey(target.getType()).toString();
        if(!isInHarmony(target) && (Main.CONFIG.removeOnEnlightenList.contains(entityID) || Main.CONFIG.removeOnEnlightenList.contains("*"))) {
            harmonizeEntity(target, player, source);
        }
        else if (!isInHarmony(target) && Main.CONFIG.convertUponEnlightened.containsKey(entityID) && !enlightenedPlayerInCreative(player)) {
            EntityType<?> conversionType = BuiltInRegistries.ENTITY_TYPE.getValue(Identifier.parse(Main.CONFIG.convertUponEnlightened.get(entityID)));
            Entity e = conversionType.create(player.level(), EntitySpawnReason.CONVERSION);
            if(target instanceof Mob mob && e instanceof Mob && player.level() instanceof ServerLevel serverWorld) {
                mob.dropFromLootTable(serverWorld, mob.damageSources().playerAttack(player), true);
                handleCustomDrops(mob, player, serverWorld);
                forceDropEquipment(mob, serverWorld, (stack) -> {
                    ItemStack copy = stack.copy();
                    mob.spawnAtLocation(serverWorld, copy);
                });

                POEntityConversionHelper helper = getConversionFinalizer((EntityType<? extends Mob>) mob.getType());
                if(helper != null) e = helper.convertEntity(mob, player);
                else {
                    EntityType<? extends Mob> tMobType = (EntityType<? extends Mob>) e.getType();
                    e = mob.convertTo(tMobType, new ConversionParams(ConversionType.SINGLE, true, true, mob.getTeam()), EntitySpawnReason.CONVERSION, (newMob) -> {});
                }
            }
            else if(e != null) {
                player.level().addFreshEntity(e);
                harmonizeEntity(target, player, source);
                target.setSilent(true);
                target.remove(Entity.RemovalReason.DISCARDED);
                target.level().playSound(null, target.getX(), target.getY(), target.getZ(), SoundEvents.EVOKER_PREPARE_SUMMON, SoundSource.PLAYERS, 1, 2);
            }

            if(e instanceof LivingEntity tle) harmonizeEntity(tle, player, source);
        }
        else if(!isInHarmony(target)) {
            harmonizeEntity(target, player, source);
        }
    }

    private static void handleCustomDrops(LivingEntity livingEntity, @Nullable Player playerAttacker, ServerLevel serverWorld) {
        if(livingEntity.getType() == EntityTypes.CREEPER && playerAttacker != null) {
            int chance = livingEntity.getRandom().nextIntBetweenInclusive(0, Math.max(0, 10 - (int)playerAttacker.getAttributes().getValue(Attributes.LUCK) * 2));
            if(chance == 0) livingEntity.spawnAtLocation(serverWorld, new ItemStack(discs.get(livingEntity.getRandom().nextIntBetweenInclusive(0, discs.size() - 1))));
        }

        if(livingEntity.getType() == EntityTypes.GOAT && playerAttacker != null) {
            int chance = livingEntity.getRandom().nextIntBetweenInclusive(0, Math.max(0, 8 - (int)playerAttacker.getAttributes().getValue(Attributes.LUCK) * 2));
            if(chance == 0) {
                ItemStack goatHorn = new ItemStack(Items.GOAT_HORN);

                // Should always work but catch just in case something goes wrong
                try {
                    Goat goat = (Goat) livingEntity;
                    goatHorn = goat.createHorn();
                }
                catch (Exception ignored) {}

                livingEntity.spawnAtLocation(serverWorld, goatHorn);
            }
        }

        if(livingEntity.getType() == EntityTypes.GHAST && playerAttacker != null) {
            int chance = livingEntity.getRandom().nextIntBetweenInclusive(0, Math.max(0, 5 - (int)playerAttacker.getAttributes().getValue(Attributes.LUCK)));
            if(chance == 0) livingEntity.spawnAtLocation(serverWorld, new ItemStack(Items.MUSIC_DISC_TEARS));
        }
    }

    public static void harmonizeEntity(LivingEntity livingEntity, @Nullable Player playerAttacker, DamageSource source) {
        if (livingEntity.level() instanceof ServerLevel serverWorld && !Main.CONFIG.enlightenmentBlackList.contains(BuiltInRegistries.ENTITY_TYPE.getKey(livingEntity.getType()).toString()) && !Main.CONFIG.enlightenmentBlackList.contains("*")) {

            POPlayerConfig playerConfig = getConfigForPlayer(playerAttacker);

            int eeMultiplier;
            if(playerConfig != null) eeMultiplier = playerConfig.eeMultiplier();
            else eeMultiplier = 1;

            livingEntity.setLastHurtByPlayer(playerAttacker, 100);
            livingEntity.dropExperience(serverWorld, playerAttacker);
            livingEntity.dropFromLootTable(serverWorld, source, true);

            handleCustomDrops(livingEntity, playerAttacker, serverWorld);

            forceDropEquipment(livingEntity, serverWorld, livingEntity instanceof Mob mob ? (stack) -> {
                ItemStack copy = stack.copy();
                mob.spawnAtLocation(serverWorld, copy);
            } : (stack) -> {});

            livingEntity.setLastHurtByPlayer((Player) null, 0);
            if(livingEntity instanceof Mob mob) {
                mob.setCanPickUpLoot(false);
                mob.setTarget(null);
                mob.targetSelector.removeAllGoals(goal -> goal instanceof NearestAttackableTargetGoal<?> || goal instanceof HurtByTargetGoal);
            }

            if(Main.CONFIG.removeOnEnlightenList.contains(BuiltInRegistries.ENTITY_TYPE.getKey(livingEntity.getType()).toString()) || Main.CONFIG.removeOnEnlightenList.contains("*")) {
                livingEntity.setSilent(true);
                livingEntity.hurtServer(serverWorld, livingEntity.damageSources().fellOutOfWorld(), Float.MAX_VALUE);
                livingEntity.remove(Entity.RemovalReason.DISCARDED);
                livingEntity.level().playSound(null, livingEntity.getX(), livingEntity.getY(), livingEntity.getZ(), SoundEvents.EVOKER_PREPARE_SUMMON, SoundSource.MASTER, 1, 2);
            }

            setInHarmony(livingEntity, true);
            if(playerAttacker != null) setEntitiesEnlightened(playerAttacker, getEntitiesEnlightened(playerAttacker) + Math.abs(eeMultiplier));
            serverWorld.sendParticles(ParticleTypes.END_ROD, livingEntity.getX(), livingEntity.getY() + livingEntity.getBoundingBox().getYsize() / 2, livingEntity.getZ(), 20, (Math.random() * livingEntity.getBoundingBox().getXsize() / 2) * 0.5, (Math.random() * livingEntity.getBoundingBox().getYsize() / 2) * 0.5, (Math.random() * livingEntity.getBoundingBox().getZsize() / 2) * 0.5, 0.075);
        }
    }


    public static void harmonizeEntityByBeacon(LivingEntity livingEntity, @Nullable Player playerAttacker, BlockPos beaconPos) {
        if (livingEntity.level() instanceof ServerLevel serverWorld && !Main.CONFIG.enlightenmentBlackList.contains(BuiltInRegistries.ENTITY_TYPE.getKey(livingEntity.getType()).toString()) && !Main.CONFIG.enlightenmentBlackList.contains("*")) {

            POPlayerConfig playerConfig = getConfigForPlayer(playerAttacker);

            int eeMultiplier;
            if(playerConfig != null) eeMultiplier = playerConfig.eeMultiplier();
            else eeMultiplier = 1;

            forceDropEquipment(livingEntity, serverWorld, (stack) -> {
                ItemStack copy = stack.copy();
                ItemEntity itemEntity = new ItemEntity(serverWorld, beaconPos.getX() + 0.5, beaconPos.above().getY(), beaconPos.getZ() + 0.5, copy);
                itemEntity.setDefaultPickUpDelay();
                itemEntity.setDeltaMovement(0 ,0 ,0);
                serverWorld.addFreshEntity(itemEntity);
            });
            if(playerAttacker != null) {
                playerAttacker.giveExperiencePoints(livingEntity.getExperienceReward(serverWorld, playerAttacker));
                processLootTableDrops(livingEntity, serverWorld, serverWorld.damageSources().playerAttack(playerAttacker), playerAttacker, (stack) -> {
                    ItemEntity itemEntity = new ItemEntity(serverWorld, beaconPos.getX() + 0.5, beaconPos.above().getY(), beaconPos.getZ() + 0.5, stack);
                    itemEntity.setDefaultPickUpDelay();
                    itemEntity.setDeltaMovement(0 ,0 ,0);
                    serverWorld.addFreshEntity(itemEntity);
                });
            }

            livingEntity.setLastHurtByPlayer((Player) null, 0);
            if(livingEntity instanceof Mob mob) {
                mob.setCanPickUpLoot(false);
                mob.setTarget(null);
                mob.targetSelector.removeAllGoals(goal -> goal instanceof NearestAttackableTargetGoal<?> || goal instanceof HurtByTargetGoal);
            }

            String entityID = BuiltInRegistries.ENTITY_TYPE.getKey(livingEntity.getType()).toString();

            if(Main.CONFIG.removeOnEnlightenList.contains(entityID) || Main.CONFIG.removeOnEnlightenList.contains("*")) {
                livingEntity.setSilent(true);
                livingEntity.hurtServer(serverWorld, livingEntity.damageSources().fellOutOfWorld(), Float.MAX_VALUE);
                livingEntity.remove(Entity.RemovalReason.DISCARDED);
                livingEntity.level().playSound(null, livingEntity.getX(), livingEntity.getY(), livingEntity.getZ(), SoundEvents.EVOKER_PREPARE_SUMMON, SoundSource.MASTER, 1, 2);
            }

            if(Main.CONFIG.convertUponEnlightened.containsKey(entityID)) {
                EntityType<?> conversionType = BuiltInRegistries.ENTITY_TYPE.getValue(Identifier.parse(Main.CONFIG.convertUponEnlightened.get(entityID)));
                Entity e = conversionType.create(serverWorld, EntitySpawnReason.CONVERSION);

                if(livingEntity instanceof Mob mob && e instanceof Mob) {
                    POEntityConversionHelper helper = finalizers.get(mob.getType());

                    Player converter = null;
                    if(serverWorld.getBlockEntity(beaconPos) instanceof EnlighteningBeacon beacon) converter = beacon.getOmnipotentOwner();

                    if(helper != null) e = helper.convertEntity(mob, converter);
                    else {
                        EntityType<? extends Mob> tMobType = (EntityType<? extends Mob>) e.getType();
                        e = mob.convertTo(tMobType, new ConversionParams(ConversionType.SINGLE, true, true, mob.getTeam()), EntitySpawnReason.CONVERSION, (newMob) -> {});
                    }
                    if(e instanceof LivingEntity tle) setInHarmony(tle, true);
                }
                else if(e != null) {
                    serverWorld.addFreshEntity(e);
                    livingEntity.setSilent(true);
                    livingEntity.remove(Entity.RemovalReason.DISCARDED);
                    serverWorld.playSound(null, livingEntity.getX(), livingEntity.getY(), livingEntity.getZ(), SoundEvents.EVOKER_PREPARE_SUMMON, SoundSource.MASTER, 1, 2);
                }
            }

            setInHarmony(livingEntity, true);
            if(playerAttacker != null) setEntitiesEnlightened(playerAttacker, getEntitiesEnlightened(playerAttacker) + Math.abs(eeMultiplier));
            serverWorld.sendParticles(ParticleTypes.END_ROD, livingEntity.getX(), livingEntity.getY() + livingEntity.getBoundingBox().getYsize() / 2, livingEntity.getZ(), 20, (Math.random() * livingEntity.getBoundingBox().getXsize() / 2) * 0.5, (Math.random() * livingEntity.getBoundingBox().getYsize() / 2) * 0.5, (Math.random() * livingEntity.getBoundingBox().getZsize() / 2) * 0.5, 0.075);
        }
    }

    public static boolean isInHarmony(Entity entity) {
        if(entity instanceof Player player) return isOmnipotent(player);
        else if(entity instanceof LivingEntity) {
            return ((IHarmonicEntity) entity).isInHarmony();
        }
        else return false;
    }

    public static void setInHarmony(LivingEntity entity, boolean value) {
        if(entity.getType() != EntityTypes.PLAYER) {
            ((IHarmonicEntity) entity).setInHarmony(value);
        }
    }

    private static void processLootTableDrops(LivingEntity target, ServerLevel world, DamageSource damageSource, @Nullable Player playerAttacker, Consumer<ItemStack> callback) {
        Optional<ResourceKey<LootTable>> optional = target.getLootTable();
        if (optional.isPresent()) {
            LootTable lootTable = world.getServer().reloadableRegistries().getLootTable(optional.get());
            LootParams.Builder builder = (new LootParams.Builder(world)).withParameter(LootContextParams.THIS_ENTITY, target).withParameter(LootContextParams.ORIGIN, target.position()).withParameter(LootContextParams.DAMAGE_SOURCE, damageSource).withOptionalParameter(LootContextParams.ATTACKING_ENTITY, damageSource.getEntity()).withOptionalParameter(LootContextParams.DIRECT_ATTACKING_ENTITY, damageSource.getDirectEntity());
            if (playerAttacker != null) {
                builder = builder.withParameter(LootContextParams.LAST_DAMAGE_PLAYER, playerAttacker).withLuck(playerAttacker.getLuck());
            }

            LootParams lootWorldContext = builder.create(LootContextParamSets.ENTITY);
            lootTable.getRandomItems(lootWorldContext, target.getLootTableSeed(), callback);
        }
    }

    private static boolean isPermaEnlightened(Player player) {
        return permaEnlightened.stream().anyMatch(config -> Objects.equals(config.stringUUID(), player.getStringUUID()));
    }

    public static void spawnEnlightenmentParticles(Entity entity, ServerLevel server) {
        for(ServerPlayer player : server.players()) {
            if(entity.getUUID() != player.getUUID() || Main.CONFIG.omnipotentPlayerParticlesLocal)
                server.sendParticles(player, ParticleTypes.END_ROD, false, false, entity.getRandomX(0.5), entity.getRandomY(), entity.getRandomZ(0.5), 1, 0, 0, 0, 0);
        }
    }

    public static void spawnEnlightenmentParticlesClient(LocalPlayer player, ClientLevel world) {
        if(Minecraft.getInstance().gameRenderer.mainCamera().isDetached() || Minecraft.getInstance().getCameraEntity() != player) {
            world.addParticle(ParticleTypes.END_ROD, false, true, player.getRandomX(0.5), player.getRandomY(), player.getRandomZ(0.5), 0, 0, 0);
        }
    }

    public static boolean enlightenedPlayerInCreative(Player player) {
        if(!player.level().isClientSide() && player instanceof ServerPlayer serverPlayer) {
            return serverPlayer.gameMode.isCreative();
        }
        else if(player.level().isClientSide() && player instanceof AbstractClientPlayer clientPlayer) {
            PlayerInfo playerListEntry = clientPlayer.getPlayerInfo();
            return playerListEntry != null && playerListEntry.getGameMode().isCreative();
        }
        else return false;
    }

    public static int getLuckLevel(Player player) {
        return (int) Math.min(Main.CONFIG.totalLuckLevels, Math.floor(getEntitiesEnlightened(player) / (double) Main.CONFIG.luckLevelEntityGoal));
    }

    public static void forceDropEquipment(LivingEntity entity, Level world, Consumer<ItemStack> callback) {
        if(world instanceof ServerLevel serverWorld && entity.getType() != EntityTypes.PLAYER) {
            if(entity instanceof Mob mob) {
                for (EquipmentSlot slot : EquipmentSlot.VALUES) {
                    ItemStack stack = mob.getItemBySlot(slot);
                    callback.accept(stack);
                    mob.getItemBySlot(slot).setCount(0);
                }
            }
            // For hardcoded loot tables
            entity.dropCustomDeathLoot(serverWorld, world.damageSources().generic(), true);
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

                    UUID playerUUID = player.getUUID();

                    if(playerUUID == null) return false;
                    else return configUUID.equals(playerUUID) || config.isWildcard();

                }).findFirst().orElse(null);
            }
        }
        else return null;
    }

    public static void respawnPlayer(ServerPlayer player) {
        // Sanity check
        if (!player.level().isClientSide()) {

            MinecraftServer server = player.level().getServer();

            if (server != null) {
                ServerLevel world = player.level();

                if (world != null) {

                    Optional<Vec3> finalPos = findRespawnPosition(world, player);
                    BlockPos fallback = world.getRespawnData().pos();

                    player.fallDistance = 0.0F;
                    finalPos.ifPresentOrElse(vec3d -> player.teleportTo(world, vec3d.x, vec3d.y, vec3d.z, Relative.ROTATION, player.getYRot(), player.getXRot(), false), () -> player.teleportTo(world, fallback.getX(), fallback.getY() + 1, fallback.getZ(), Relative.ROTATION, player.getYRot(), player.getXRot(), false));
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

        ChunkAccess chunk = world.getChunk(Mth.floor(best.x / 16.0), Mth.floor(best.z / 16.0));
        chunk.findBlocks(BlockBehaviour.BlockStateBase::isSolid, (pos, state) -> {
            if(!foundSolid.get()) {
                vec3d1.set(new Vec3(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5));
                foundSolid.set(true);
            }
        });

        int i = 0;
        while ((!world.getBlockState(BlockPos.containing(vec3d1.get()).above()).isAir() && !world.getBlockState(BlockPos.containing(vec3d1.get()).above().above()).isAir()) || i > Short.MAX_VALUE) {
            vec3d1.set(vec3d1.get().add(0, 1, 0));
            i++;
        }

        vec3d = vec3d1.get().add(0.0, 2.0, 0.0);
        return Optional.of(vec3d);
    }

    private static boolean isChunkEmpty(ServerLevel world, Vec3 pos) {
        return world.getChunk(Mth.floor(pos.x / 16.0), Mth.floor(pos.z / 16.0)).getHighestFilledSectionIndex() == -1;
    }

}
