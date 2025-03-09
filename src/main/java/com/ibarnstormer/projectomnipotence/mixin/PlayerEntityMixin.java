package com.ibarnstormer.projectomnipotence.mixin;

import com.google.common.collect.Maps;
import com.ibarnstormer.projectomnipotence.Main;

import com.ibarnstormer.projectomnipotence.entity.HarmonicEntity;
import com.ibarnstormer.projectomnipotence.network.UpdateOmnipotentDataPayload;
import com.ibarnstormer.projectomnipotence.utils.POUtils;
import net.minecraft.ChatFormatting;
import net.minecraft.core.registries.BuiltInRegistries;
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
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.network.PacketDistributor;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.*;

@Mixin(Player.class)
public abstract class PlayerEntityMixin extends LivingEntity {

    @Unique
    private static final ResourceLocation OMNIPOTENT_LUCK = ResourceLocation.fromNamespaceAndPath(Main.MODID, "omnipotent_luck");

    @Unique
    private int eeDelta;

    protected PlayerEntityMixin(EntityType<? extends LivingEntity> p_20966_, Level p_20967_) {
        super(p_20966_, p_20967_);
    }

    @Inject(method = "tick", at = @At("HEAD"))
    public void omniTick(CallbackInfo ci) {
        Player player = (Player) (Object) this;
        if ((Main.CONFIG.permaOmnipotents.containsKey(player.getScoreboardName()) || Main.CONFIG.permaOmnipotents.containsKey("*")) && !POUtils.isOmnipotent(player)) {
            POUtils.setOmnipotent(true, level(), player, true);
            Integer score = Main.CONFIG.permaOmnipotents.get(player.getScoreboardName());
            POUtils.setEnlightenedEntities(Math.max((score == null ? Main.CONFIG.permaOmnipotents.get("*") : score.intValue()), POUtils.getEnlightenedEntities(player)), player);
        }

        if (POUtils.isTrueEnlightened(player) && !POUtils.isOmnipotent(player)) {
            POUtils.setOmnipotent(true, level(), player, true);
            POUtils.setEnlightenedEntities(Math.max((Math.min(10, Main.CONFIG.totalLuckLevels) * Main.CONFIG.luckLevelEntityGoal) + 1, POUtils.getEnlightenedEntities(player)), player);
        }

        AttributeInstance playerLuck = player.getAttribute(Attributes.LUCK);
        assert playerLuck != null;

        if (POUtils.isOmnipotent(player)) {
            if (level() instanceof ServerLevel server && player.tickCount % 5 == 0 && !player.isSpectator() && Main.CONFIG.omnipotentPlayerParticles) {
                POUtils.spawnEnlightenmentParticles(player, server);
            }

            if (Main.CONFIG.omnipotentPlayersGlow && !player.hasEffect(MobEffects.GLOWING)) {
                player.addEffect(new MobEffectInstance(MobEffects.GLOWING, -1, 0, true, false, false));
            }

            Map<MobEffect, MobEffectInstance> localMEICollection = Maps.newHashMap();
            for (MobEffectInstance effect : player.getActiveEffects()) {
                if (effect.getEffect().value().getCategory() == MobEffectCategory.HARMFUL)
                    localMEICollection.put(effect.getEffect().value(), effect);
            }
            for (MobEffectInstance effect : localMEICollection.values()) {
                player.removeEffect(effect.getEffect());
            }

            int score = POUtils.getEnlightenedEntities(player);

            AttributeModifier luckModifier = playerLuck.getModifier(OMNIPOTENT_LUCK);
            if (luckModifier == null && score >= Main.CONFIG.luckLevelEntityGoal) {
                playerLuck.addPermanentModifier(new AttributeModifier(OMNIPOTENT_LUCK, POUtils.getLuckLevel(player), AttributeModifier.Operation.ADD_VALUE));
            } else if (luckModifier != null) {
                double currentLevel = POUtils.getLuckLevel(player);
                if (luckModifier.amount() != currentLevel) {
                    playerLuck.removeModifier(luckModifier);
                    playerLuck.addPermanentModifier(new AttributeModifier(OMNIPOTENT_LUCK, currentLevel, AttributeModifier.Operation.ADD_VALUE));
                }
            }

            /* eeDelta isn't persistent so set it to score upon class load
             * to prevent message spam each time we load a world
             */
            if (score > 0 && eeDelta == 0) eeDelta = score;

            if (score > this.eeDelta && Math.ceil((double) score / Main.CONFIG.luckLevelEntityGoal) > Math.ceil((double) this.eeDelta / Main.CONFIG.luckLevelEntityGoal) && score < (Main.CONFIG.totalLuckLevels + 1) * Main.CONFIG.luckLevelEntityGoal && score > Main.CONFIG.luckLevelEntityGoal) {
                if (!level().isClientSide)
                    player.displayClientMessage(Component.translatable("message.projectomnipotence.attunement").withStyle(Style.EMPTY.withColor(ChatFormatting.YELLOW)), false);
            }
            if (score > this.eeDelta && score >= Main.CONFIG.invulnerabilityEntityGoal && Main.CONFIG.omnipotentPlayersCanBecomeInvulnerable && eeDelta < Main.CONFIG.invulnerabilityEntityGoal) {
                if (!level().isClientSide)
                    player.displayClientMessage(Component.translatable("message.projectomnipotence.invulnerability").withStyle(Style.EMPTY.withColor(ChatFormatting.YELLOW)), false);
            }
            if (score > this.eeDelta && score >= Main.CONFIG.flightEntityGoal && Main.CONFIG.omnipotentPlayersCanGainFlight && eeDelta < Main.CONFIG.flightEntityGoal) {
                if (!level().isClientSide)
                    player.displayClientMessage(Component.translatable("message.projectomnipotence.flight").withStyle(Style.EMPTY.withColor(ChatFormatting.YELLOW)), false);
            }

            this.eeDelta = score;

            if (score >= Main.CONFIG.invulnerabilityEntityGoal && Main.CONFIG.omnipotentPlayersCanBecomeInvulnerable) {
                player.entityData.set(LivingEntity.DATA_HEALTH_ID, Math.max(player.getMaxHealth(), 20.0F));
                if (player.getTicksFrozen() > 0) player.setTicksFrozen(0);
            }

            if (score >= Main.CONFIG.flightEntityGoal && Main.CONFIG.omnipotentPlayersCanGainFlight && !player.getAbilities().mayfly) {
                player.getAbilities().mayfly = true;
                player.onUpdateAbilities();
            }

            if(player instanceof ServerPlayer serverPlayer && serverPlayer.tickCount % 20 == 0) {
                PacketDistributor.sendToPlayer(serverPlayer, new UpdateOmnipotentDataPayload(serverPlayer.getGameProfile(), POUtils.isOmnipotent(serverPlayer), POUtils.getEnlightenedEntities(serverPlayer)));
            }

        } else {
            if (playerLuck.getModifier(OMNIPOTENT_LUCK) != null) {
                playerLuck.removeModifier(OMNIPOTENT_LUCK);
            }
        }
    }

    @Inject(method = "hurt", at = @At("HEAD"), cancellable = true)
    public void modulateDamage(DamageSource source, float amount, CallbackInfoReturnable<Boolean> cir) {
        Player player = (Player) (Object) this;
        Level world = player.level();
        if (POUtils.isOmnipotent(player)) {
            if(source.is(DamageTypeTags.BYPASSES_INVULNERABILITY) && !world.isClientSide() && !player.getAbilities().mayfly && player.getY() <= world.getMinBuildHeight()) {
                MinecraftServer server = player.getServer();
                if(server != null) {
                    world.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.ENDERMAN_TELEPORT, SoundSource.PLAYERS, 1.0f, 1);
                    POUtils.respawnPlayer((ServerPlayer) player);
                    world.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.ENDERMAN_TELEPORT, SoundSource.PLAYERS, 1.0f, 1);
                    cir.setReturnValue(false);
                }
            }

            if(POUtils.getEnlightenedEntities(player) >= Main.CONFIG.invulnerabilityEntityGoal && Main.CONFIG.omnipotentPlayersCanBecomeInvulnerable) cir.setReturnValue(false);
            if (source.getEntity() != null) {
                if (Main.CONFIG.omnipotentPlayersReflectDamage && source.getEntity() instanceof Player playerAttacker && !POUtils.isOmnipotent(playerAttacker)) {
                    if(Main.CONFIG.damageReflectionBlackList.contains(Objects.requireNonNull(BuiltInRegistries.ENTITY_TYPE.getKey(source.getEntity().getType())).toString()) || Main.CONFIG.damageReflectionBlackList.contains("*")) {
                        source.getEntity().hurt(source.getEntity().damageSources().generic(), amount);
                    }
                    else source.getEntity().hurt(source, amount);
                }
            }
        }
    }

    @Inject(method = "hurt", at = @At("TAIL"))
    public void onDeath(DamageSource p_36154_, float p_36155_, CallbackInfoReturnable<Boolean> cir) {
        Player player = (Player) (Object) this;
        if(POUtils.isOmnipotent(player) && player.isDeadOrDying() && Main.CONFIG.omnipotentPlayersReflectDamage) {
            Entity attacker = p_36154_.getEntity();
            if(attacker != null) attacker.kill();
        }
    }

    @Inject(method = "attack", at = @At("HEAD"))
    public void onAttack(Entity p_36347_, CallbackInfo ci) {
        Player player = (Player) (Object) this;
        if(POUtils.isOmnipotent(player) && !player.level().isClientSide) {
            float f = (float) player.getAttributeValue(Attributes.SWEEPING_DAMAGE_RATIO);

            List<LivingEntity> list;

            if(f > 0) {
                list = player.level().getEntitiesOfClass(LivingEntity.class, p_36347_.getBoundingBox().inflate(1.0D, 0.25D, 1.0D));
                for(LivingEntity entity : list) {
                    if(entity != p_36347_ && entity != player && entity instanceof HarmonicEntity harmonicEntity && !harmonicEntity.getHarmonicState()) POUtils.harmonizeEntity(entity, player.level(), player, entity.damageSources().playerAttack(player));
                }
                player.sweepAttack();
            }
            else if(p_36347_ instanceof LivingEntity le) list = List.of(le);
            else list = new ArrayList<>();

            // If we want to simply remove stubborn entities
            for(LivingEntity le : list) {
                String entityID = Objects.requireNonNull(BuiltInRegistries.ENTITY_TYPE.getKey(le.getType())).toString();
                if(!((HarmonicEntity) le).getHarmonicState() && (Main.CONFIG.removeOnEnlightenList.contains(entityID) || Main.CONFIG.removeOnEnlightenList.contains("*"))) {
                    POUtils.harmonizeEntity(le, player.level(), player, player.damageSources().playerAttack(player));
                }
                else if (!((HarmonicEntity) le).getHarmonicState() && Main.CONFIG.convertUponEnlightened.containsKey(entityID) && !POUtils.enlightenedPlayerInCreative(player)) {
                    EntityType<?> conversionType = BuiltInRegistries.ENTITY_TYPE.get(ResourceLocation.tryParse(Main.CONFIG.convertUponEnlightened.get(entityID)));
                    if(conversionType != null) {
                        Entity e = conversionType.create(player.level());

                        if(le instanceof Mob mob && e instanceof Mob) {
                            POUtils.harmonizeEntity(le, player.level(), player, player.damageSources().playerAttack(player));
                            EntityType<? extends Mob> tMobType = (EntityType<? extends Mob>) e.getType();
                            e = mob.convertTo(tMobType, true);
                        }
                        else if(e != null) {
                            player.level().addFreshEntity(e);
                            POUtils.harmonizeEntity(le, player.level(), player, player.damageSources().playerAttack(player));
                            le.setSilent(true);
                            le.remove(RemovalReason.DISCARDED);
                            le.level().playSound(null, le.getX(), le.getY(), le.getZ(), SoundEvents.EVOKER_PREPARE_SUMMON, SoundSource.MASTER, 1, 2);
                        }

                        if(e instanceof LivingEntity tle) POUtils.harmonizeEntity(tle, player.level(), player, player.damageSources().playerAttack(player));
                    }
                }
            }
        }
    }

    @Inject(method = "die", at = @At("HEAD"), cancellable = true)
    public void playerEntity$die(DamageSource cause, CallbackInfo ci) {
        Player player = ((Player) (Object) this);
        if(POUtils.isOmnipotent(player) && POUtils.getEnlightenedEntities(player) >= Main.CONFIG.invulnerabilityEntityGoal && Main.CONFIG.omnipotentPlayersCanBecomeInvulnerable) {
            ci.cancel();
        }
    }
}
