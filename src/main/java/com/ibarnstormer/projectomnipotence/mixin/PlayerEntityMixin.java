package com.ibarnstormer.projectomnipotence.mixin;

import com.google.common.collect.Maps;
import com.ibarnstormer.projectomnipotence.Main;
import com.ibarnstormer.projectomnipotence.capability.ModCapabilityProvider;
import com.ibarnstormer.projectomnipotence.entity.HarmonicEntity;
import com.ibarnstormer.projectomnipotence.utils.Utils;
import net.minecraft.ChatFormatting;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.level.Level;
import net.minecraftforge.registries.ForgeRegistries;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;

@Mixin(Player.class)
public abstract class PlayerEntityMixin extends LivingEntity {

    @Unique
    private int eeDelta;

    protected PlayerEntityMixin(EntityType<? extends LivingEntity> p_20966_, Level p_20967_) {
        super(p_20966_, p_20967_);
    }

    @Inject(method = "tick", at = @At("HEAD"))
    public void omniTick(CallbackInfo ci) {
        Player player = (Player) (Object) this;
        player.getCapability(ModCapabilityProvider.OMNIPOTENCE_CAPABILITY).ifPresent((cap) -> {
            if((Main.CONFIG.permaOmnipotents.containsKey(player.getScoreboardName()) || Main.CONFIG.permaOmnipotents.containsKey("*")) && !cap.isOmnipotent()) {
                cap.setOmnipotent(true, level(), player, true);
                Integer score = Main.CONFIG.permaOmnipotents.get(player.getScoreboardName());
                cap.setEnlightenedEntities(Math.max((score == null ? Main.CONFIG.permaOmnipotents.get("*") : score.intValue()), cap.getEnlightenedEntities()));
            }

            if(Utils.isTrueEnlightened(player) && !cap.isOmnipotent()) {
                cap.setOmnipotent(true, level(), player, true);
                cap.setEnlightenedEntities(Math.max((Math.min(10, Main.CONFIG.totalLuckLevels) * Main.CONFIG.luckLevelEntityGoal) + 1, cap.getEnlightenedEntities()));
            }

            AttributeInstance playerLuck = player.getAttribute(Attributes.LUCK);
            assert playerLuck != null;

            if(cap.isOmnipotent()) {
                if(level() instanceof ServerLevel server && player.tickCount % 5 == 0 && !player.isSpectator() && Main.CONFIG.omnipotentPlayerParticles) {
                    Utils.spawnEnlightenmentParticles(player, server);
                }

                if(Main.CONFIG.omnipotentPlayersGlow && !player.hasEffect(MobEffects.GLOWING)) {
                    player.addEffect(new MobEffectInstance(MobEffects.GLOWING, -1, 0, true, false, false));
                }

                Map<MobEffect, MobEffectInstance> localMEICollection = Maps.newHashMap();
                for(MobEffectInstance effect : player.getActiveEffects()) {
                    if(effect.getEffect().getCategory() == MobEffectCategory.HARMFUL) localMEICollection.put(effect.getEffect(), effect);
                }
                for(MobEffectInstance effect : localMEICollection.values()) {
                    player.removeEffect(effect.getEffect());
                }

                int score = cap.getEnlightenedEntities();

                AttributeModifier luckModifier = playerLuck.getModifier(UUID.fromString("784e3cf6-9e69-11ed-a8fc-0242ac120002"));
                if (luckModifier == null && score >= Main.CONFIG.luckLevelEntityGoal) {
                    playerLuck.addPermanentModifier(new AttributeModifier(UUID.fromString("784e3cf6-9e69-11ed-a8fc-0242ac120002"), "Omnipotent Luck", Utils.getLuckLevel(player), AttributeModifier.Operation.ADDITION));
                }
                else if (luckModifier != null) {
                    double currentLevel = Utils.getLuckLevel(player);
                    if(luckModifier.getAmount() != currentLevel) {
                        playerLuck.removeModifier(luckModifier);
                        playerLuck.addPermanentModifier(new AttributeModifier(UUID.fromString("784e3cf6-9e69-11ed-a8fc-0242ac120002"), "Omnipotent Luck", currentLevel, AttributeModifier.Operation.ADDITION));
                    }
                }

                /* eeDelta isn't persistent so set it to score upon class load
                 * to prevent message spam each time we load a world
                 */
                if(score > 0 && eeDelta == 0) eeDelta = score;

                if(score > this.eeDelta && Math.ceil((double) score / Main.CONFIG.luckLevelEntityGoal) > Math.ceil((double) this.eeDelta / Main.CONFIG.luckLevelEntityGoal) && score < (Main.CONFIG.totalLuckLevels + 1) * Main.CONFIG.luckLevelEntityGoal && score > Main.CONFIG.luckLevelEntityGoal) {
                    if(!level().isClientSide) player.displayClientMessage(Component.translatable("message.projectomnipotence.attunement").withStyle(Style.EMPTY.withColor(ChatFormatting.YELLOW)), false);
                }
                if(score > this.eeDelta && score >= Main.CONFIG.invulnerabilityEntityGoal && Main.CONFIG.omnipotentPlayersCanBecomeInvulnerable && eeDelta < Main.CONFIG.invulnerabilityEntityGoal) {
                    if(!level().isClientSide) player.displayClientMessage(Component.translatable("message.projectomnipotence.invulnerability").withStyle(Style.EMPTY.withColor(ChatFormatting.YELLOW)), false);
                }
                if(score > this.eeDelta && score >= Main.CONFIG.flightEntityGoal && Main.CONFIG.omnipotentPlayersCanGainFlight && eeDelta < Main.CONFIG.flightEntityGoal) {
                    if(!level().isClientSide) player.displayClientMessage(Component.translatable("message.projectomnipotence.flight").withStyle(Style.EMPTY.withColor(ChatFormatting.YELLOW)), false);
                }

                this.eeDelta = score;

                if(score >= Main.CONFIG.invulnerabilityEntityGoal && Main.CONFIG.omnipotentPlayersCanBecomeInvulnerable) {
                    if(player.getTicksFrozen() > 0) player.setTicksFrozen(0);
                }

                if(score >= Main.CONFIG.flightEntityGoal && Main.CONFIG.omnipotentPlayersCanGainFlight && !player.getAbilities().mayfly) {
                    player.getAbilities().mayfly = true;
                    player.onUpdateAbilities();
                }

            }
            else {
                if(playerLuck.getModifier(UUID.fromString("784e3cf6-9e69-11ed-a8fc-0242ac120002")) != null) {
                    playerLuck.removePermanentModifier(UUID.fromString("784e3cf6-9e69-11ed-a8fc-0242ac120002"));
                }
            }
        });
    }

    @Inject(method = "hurt", at = @At("HEAD"), cancellable = true)
    public void modulateDamage(DamageSource source, float amount, CallbackInfoReturnable<Boolean> cir) {
        Player player = (Player) (Object) this;
        player.getCapability(ModCapabilityProvider.OMNIPOTENCE_CAPABILITY).ifPresent((cap) -> {
            Level world = player.level();
            if (cap.isOmnipotent() && !source.type().equals(Utils.antiBadActorDamage(world).type())) {
                if(source.is(DamageTypeTags.BYPASSES_INVULNERABILITY) && !world.isClientSide() && !player.getAbilities().mayfly && player.getY() <= world.getMinBuildHeight()) {
                    MinecraftServer server = player.getServer();
                    if(server != null) {
                        world.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.ENDERMAN_TELEPORT, SoundSource.PLAYERS, 1.0f, 1);
                        Utils.respawnPlayer((ServerPlayer) player);
                        world.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.ENDERMAN_TELEPORT, SoundSource.PLAYERS, 1.0f, 1);
                        cir.setReturnValue(false);
                    }
                }

                if(cap.getEnlightenedEntities() >= Main.CONFIG.invulnerabilityEntityGoal) cir.setReturnValue(false);
                if (source.getEntity() != null) {
                    AtomicBoolean attackerIsOmnipotent = new AtomicBoolean(false);
                    source.getEntity().getCapability(ModCapabilityProvider.OMNIPOTENCE_CAPABILITY).ifPresent(c -> attackerIsOmnipotent.set(c.isOmnipotent()));
                    if (Main.CONFIG.omnipotentPlayersReflectDamage && !attackerIsOmnipotent.get()) {
                        if(Main.CONFIG.damageReflectionBlackList.contains(Objects.requireNonNull(ForgeRegistries.ENTITY_TYPES.getKey(source.getEntity().getType())).toString()) || Main.CONFIG.damageReflectionBlackList.contains("*")) {
                            source.getEntity().hurt(source.getEntity().damageSources().generic(), amount);
                        }
                        else source.getEntity().hurt(source, amount);
                    }
                }
            }
        });
    }

    @Inject(method = "hurt", at = @At("TAIL"))
    public void onDeath(DamageSource p_36154_, float p_36155_, CallbackInfoReturnable<Boolean> cir) {
        Player player = (Player) (Object) this;
        player.getCapability(ModCapabilityProvider.OMNIPOTENCE_CAPABILITY).ifPresent((cap) -> {
            if(cap.isOmnipotent() && player.isDeadOrDying() && Main.CONFIG.omnipotentPlayersReflectDamage) {
                Entity attacker = p_36154_.getEntity();
                if(attacker != null) attacker.kill();
            }
        });
    }

    @Inject(method = "attack", at = @At("HEAD"))
    public void onAttack(Entity p_36347_, CallbackInfo ci) {
        Player player = (Player) (Object) this;
        player.getCapability(ModCapabilityProvider.OMNIPOTENCE_CAPABILITY).ifPresent((cap) -> {
            if(cap.isOmnipotent() && !player.level().isClientSide) {
                float f = EnchantmentHelper.getSweepingDamageRatio(player);

                List<LivingEntity> list;

                if(f > 0) {
                    list = player.level().getEntitiesOfClass(LivingEntity.class, p_36347_.getBoundingBox().inflate(1.0D, 0.25D, 1.0D));
                    for(LivingEntity entity : list) {
                        if(entity != p_36347_ && entity != player && entity instanceof HarmonicEntity harmonicEntity && !harmonicEntity.getHarmonicState()) Utils.harmonizeEntity(entity, player.level(), player, entity.damageSources().playerAttack(player), cap);
                    }
                    player.sweepAttack();
                }
                else if(p_36347_ instanceof LivingEntity le) list = List.of(le);
                else list = new ArrayList<>();

                // If we want to simply remove stubborn entities
                for(LivingEntity le : list) {
                    String entityID = Objects.requireNonNull(ForgeRegistries.ENTITY_TYPES.getKey(le.getType())).toString();
                    if(!((HarmonicEntity) le).getHarmonicState() && (Main.CONFIG.removeOnEnlightenList.contains(entityID) || Main.CONFIG.removeOnEnlightenList.contains("*"))) {
                        Utils.harmonizeEntity(le, player.level(), player, player.damageSources().playerAttack(player), cap);
                    }
                    else if (!((HarmonicEntity) le).getHarmonicState() && Main.CONFIG.convertUponEnlightened.containsKey(entityID)) {
                        EntityType<?> conversionType = ForgeRegistries.ENTITY_TYPES.getValue(new ResourceLocation(Main.CONFIG.convertUponEnlightened.get(entityID)));
                        if(conversionType != null) {
                            Entity e = conversionType.create(player.level());
                            if(le instanceof Mob mob && e instanceof Mob) {
                                EntityType<? extends Mob> tMobType = (EntityType<? extends Mob>) e.getType();
                                e = mob.convertTo(tMobType, true);
                            }
                            else if(e != null) {
                                player.level().addFreshEntity(e);
                                Utils.harmonizeEntity(le, player.level(), player, player.damageSources().playerAttack(player), cap);
                                le.setSilent(true);
                                le.remove(RemovalReason.DISCARDED);
                                le.level().playSound(null, le.getX(), le.getY(), le.getZ(), SoundEvents.EVOKER_PREPARE_SUMMON, SoundSource.MASTER, 1, 2);
                            }

                            if(e instanceof LivingEntity tle) Utils.harmonizeEntity(tle, player.level(), player, player.damageSources().playerAttack(player), cap);
                        }
                    }
                }
            }
        });
    }
}
