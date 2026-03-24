package com.ibarnstormer.projectomnipotence.mixin;

import com.google.common.collect.Maps;
import com.ibarnstormer.projectomnipotence.Main;
import com.ibarnstormer.projectomnipotence.config.POPlayerConfig;
import com.ibarnstormer.projectomnipotence.entity.IPOPlayerEntity;
import com.ibarnstormer.projectomnipotence.network.payload.SyncSSDHDataPayload;
import com.ibarnstormer.projectomnipotence.utils.POUtils;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.ChatFormatting;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.util.Mth;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.*;

@Mixin(Player.class)
public abstract class PlayerMixin extends EntityMixin implements IPOPlayerEntity {

    @Shadow public abstract boolean isAlwaysTicking();

    @Unique
    boolean isOmnipotent;
    @Unique
    int entitiesEnlightened;

    @Unique
    private static final Identifier OMNIPOTENT_LUCK = Identifier.fromNamespaceAndPath(Main.MODID, "omnipotent_luck");

    @Unique
    private int eeDelta;

    @Unique
    private Player getPlayer() {
        return (Player) (Object) this;
    }

    @Inject(method = "readAdditionalSaveData", at = @At("TAIL"))
    public void playerEntity$readCustomData(ValueInput view, CallbackInfo ci) {
        Player player = this.getPlayer();
        POUtils.readPlayerData(player, view);
    }

   @Inject(method = "addAdditionalSaveData", at = @At("TAIL"))
    public void playerEntity$writeCustomData(ValueOutput view, CallbackInfo ci) {
       Player player = this.getPlayer();
        POUtils.writePlayerData(player, view);
    }

    @Inject(method = "hurtServer", at = @At("HEAD"), cancellable = true)
    public void playerEntity$damage(ServerLevel world, DamageSource source, float amount, CallbackInfoReturnable<Boolean> cir) {
        Player player = this.getPlayer();
        if (POUtils.isOmnipotent(player)) {
            if(source.is(DamageTypeTags.BYPASSES_INVULNERABILITY) && !world.isClientSide() && !player.getAbilities().mayfly && player.getY() <= world.getMinY()) {
                world.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.ENDERMAN_TELEPORT, SoundSource.PLAYERS, 1.0f, 1);
                POUtils.respawnPlayer((ServerPlayer) player);
                world.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.ENDERMAN_TELEPORT, SoundSource.PLAYERS, 1.0f, 1);
                cir.setReturnValue(false);
            }

            if(POUtils.getEntitiesEnlightened(player) >= Main.CONFIG.invulnerabilityEntityGoal && Main.CONFIG.omnipotentPlayersCanBecomeInvulnerable) cir.setReturnValue(false);
            if (source.getEntity() != null) {
                if ((!(source.getEntity() instanceof Player) || ((source.getEntity() instanceof Player playerAttacker) && !POUtils.isOmnipotent(playerAttacker))) && Main.CONFIG.omnipotentPlayersReflectDamage) {
                    if(Main.CONFIG.damageReflectionBlackList.contains(BuiltInRegistries.ENTITY_TYPE.getKey(source.getEntity().getType()).toString()) || Main.CONFIG.damageReflectionBlackList.contains("*")) {
                        source.getEntity().hurtServer(world, source.getEntity().damageSources().generic(), amount);
                    }
                    else source.getEntity().hurtServer(world, source, amount);
                }
            }
        }
    }

    @Inject(method = "hurtServer", at = @At("TAIL"))
    public void playerEntity$damage_onDeath(ServerLevel world, DamageSource source, float amount, CallbackInfoReturnable<Boolean> cir) {
        Player player = this.getPlayer();
        if(POUtils.isOmnipotent(player) && player.isDeadOrDying() && Main.CONFIG.omnipotentPlayersReflectDamage) {
            Entity attacker = source.getEntity();
            if(attacker != null) attacker.kill(world);
        }
    }

    @Inject(method = "attack", at = @At("HEAD"))
    public void playerEntity$attack(Entity target, CallbackInfo ci) {
        Player player = this.getPlayer();
        if(POUtils.isOmnipotent(player) && !POUtils.enlightenedPlayerInCreative(player)) {
            float f = (float) player.getAttributeValue(Attributes.SWEEPING_DAMAGE_RATIO);

            List<LivingEntity> list;

            if(f > 0) {
                list = player.level().getEntitiesOfClass(LivingEntity.class, target.getBoundingBox().inflate(1.0D, 0.25D, 1.0D));
                if(player.level() instanceof ServerLevel serverWorld) {
                    double d = -Mth.sin(player.getYRot() * 0.017453292F);
                    double e = Mth.cos(player.getYRot() * 0.017453292F);
                    serverWorld.sendParticles(ParticleTypes.SWEEP_ATTACK, player.getX() + d, player.getY(0.5), player.getZ() + e, 0, d, 0.0, e, 0.0);
                }
            }
            else if (target instanceof LivingEntity le) list = List.of(le);
            else list = new ArrayList<>();

            for(LivingEntity le : list) {
                POUtils.handleEnlightenment(le, player, null);
            }
        }
    }


    @Inject(method = "tick", at = @At("TAIL"))
    public void playerEntity$tick(CallbackInfo ci) {
        Player player = this.getPlayer();
        Level world = player.level();
        if(world instanceof ServerLevel serverWorld) {

            POPlayerConfig config = POUtils.getConfigForPlayer(player);

            if(config != null) {
                if(!POUtils.isOmnipotent(player) && config.enlightenedOnStart()) POUtils.grantOmnipotence(player, false);
                int score = config.eeHandicap();
                POUtils.setEntitiesEnlightened(player, Math.max(score, POUtils.getEntitiesEnlightened(player)));
            }

            AttributeInstance playerLuck = player.getAttribute(Attributes.LUCK);
            assert playerLuck != null;

            if (POUtils.isOmnipotent(player)) {

                if (player.tickCount % 5 == 0 && Main.CONFIG.omnipotentPlayerParticles && !player.isSpectator()) {
                    POUtils.spawnEnlightenmentParticles(player, serverWorld);
                }

                if(Main.CONFIG.omnipotentPlayersGlow && !player.hasEffect(MobEffects.GLOWING)) {
                    player.addEffect(new MobEffectInstance(MobEffects.GLOWING, -1, 0, true, false, false));
                }

                Map<MobEffect, MobEffectInstance> localSEICollection = Maps.newHashMap();

                for (MobEffectInstance statusEffect : player.getActiveEffects()) {
                    if (statusEffect.getEffect().value().getCategory() == MobEffectCategory.HARMFUL)
                        localSEICollection.put(statusEffect.getEffect().value(), statusEffect);
                }
                for (MobEffectInstance statusEffect : localSEICollection.values()) {
                    player.removeEffect(statusEffect.getEffect());
                }

                int score = POUtils.getEntitiesEnlightened(player);

                AttributeModifier luckModifier = playerLuck.getModifier(OMNIPOTENT_LUCK);
                if (luckModifier == null && score >= Main.CONFIG.luckLevelEntityGoal) {
                    playerLuck.addPermanentModifier(new AttributeModifier(OMNIPOTENT_LUCK, POUtils.getLuckLevel(player), AttributeModifier.Operation.ADD_VALUE));
                }
                else if (luckModifier != null) {
                    double currentLevel = POUtils.getLuckLevel(player);
                    if(luckModifier.amount() != currentLevel) {
                        playerLuck.removeModifier(OMNIPOTENT_LUCK);
                        playerLuck.addPermanentModifier(new AttributeModifier(OMNIPOTENT_LUCK, currentLevel, AttributeModifier.Operation.ADD_VALUE));
                    }
                }

                /* eeDelta isn't persistent so set it to score upon class load
                 * to prevent message spam each time we load a world
                 */
                if(score > 0 && eeDelta == 0) eeDelta = score;

                if (score > this.eeDelta && Math.ceil((double) score / Main.CONFIG.luckLevelEntityGoal) > Math.ceil((double) this.eeDelta / Main.CONFIG.luckLevelEntityGoal) && score < (Main.CONFIG.totalLuckLevels + 1) * Main.CONFIG.luckLevelEntityGoal && score > Main.CONFIG.luckLevelEntityGoal) {
                    player.sendSystemMessage(Component.translatable("message.projectomnipotence.attunement").withStyle(Style.EMPTY.withColor(ChatFormatting.YELLOW)));
                }
                if (score > this.eeDelta && score >= Main.CONFIG.invulnerabilityEntityGoal && Main.CONFIG.omnipotentPlayersCanBecomeInvulnerable && eeDelta < Main.CONFIG.invulnerabilityEntityGoal) {
                    player.sendSystemMessage(Component.translatable("message.projectomnipotence.invulnerability").withStyle(Style.EMPTY.withColor(ChatFormatting.YELLOW)));
                }
                if (score > this.eeDelta && score >= Main.CONFIG.flightEntityGoal && Main.CONFIG.omnipotentPlayersCanGainFlight && eeDelta < Main.CONFIG.flightEntityGoal) {
                    player.sendSystemMessage(Component.translatable("message.projectomnipotence.flight").withStyle(Style.EMPTY.withColor(ChatFormatting.YELLOW)));
                }

                this.eeDelta = score;

                if(score >= Main.CONFIG.invulnerabilityEntityGoal && Main.CONFIG.omnipotentPlayersCanBecomeInvulnerable) {
                    player.getEntityData().set(LivingEntity.DATA_HEALTH_ID, Math.max(player.getMaxHealth(), 20.0F));
                    if(player.getTicksFrozen() > 0) player.setTicksFrozen(0);
                }

                if(score >= Main.CONFIG.flightEntityGoal && Main.CONFIG.omnipotentPlayersCanGainFlight && !player.getAbilities().mayfly) {
                    player.getAbilities().mayfly = true;
                    player.onUpdateAbilities();
                }

                // Update nbt on client, send update each second
                if(player instanceof ServerPlayer serverPlayer && serverPlayer.tickCount % 20 == 0) ServerPlayNetworking.send(serverPlayer, new SyncSSDHDataPayload(serverPlayer.getGameProfile(), POUtils.isOmnipotent(serverPlayer), POUtils.getEntitiesEnlightened(serverPlayer)));

            } else {
                if (playerLuck.getModifier(OMNIPOTENT_LUCK) != null) {
                    playerLuck.removeModifier(OMNIPOTENT_LUCK);
                }
            }
        }
    }

    @Inject(method = "die", at = @At("HEAD"), cancellable = true)
    public void playerEntity$onDeath(DamageSource cause, CallbackInfo ci) {
        Player player = this.getPlayer();
        if(POUtils.isOmnipotent(player) && POUtils.getEntitiesEnlightened(player) >= Main.CONFIG.invulnerabilityEntityGoal && Main.CONFIG.omnipotentPlayersCanBecomeInvulnerable) {
            ci.cancel();
        }
    }

    @Inject(method = "isCreative", at = @At("RETURN"), cancellable = true)
    public void playerEntity$isCreative(CallbackInfoReturnable<Boolean> cir) {
        Player player = this.getPlayer();
        if(POUtils.isOmnipotent(player) && Main.CONFIG.carryOnCompat) {
            // Carry-on compat
            StackTraceElement[] stackTrace = Thread.currentThread().getStackTrace();
            for (StackTraceElement e : stackTrace) {
                if(e.getClassName().contains("tschipp.carryon.common.carry")) cir.setReturnValue(true);
            }
        }

        if(POUtils.isOmnipotent(player) && Main.CONFIG.omnipotentPlayersCanBecomeInvulnerable && POUtils.getEntitiesEnlightened(player) >= Main.CONFIG.invulnerabilityEntityGoal && !cir.getReturnValue()) {

            // Just assume that we are in creative if check gets called from FE (prevents UOM's final explosion from killing invulnerable omnipotents and timestop)
            StackTraceElement[] stackTrace = Thread.currentThread().getStackTrace();
            for (StackTraceElement e : stackTrace) {
                if(e.getClassName().contains("com.mega.uom")) cir.setReturnValue(true);
            }
        }
        if(POUtils.isOmnipotent(player) && Main.CONFIG.omnipotentPlayersCanGainFlight && POUtils.getEntitiesEnlightened(player) >= Main.CONFIG.flightEntityGoal && !cir.getReturnValue()) {

            // Prevent the Apostle from Goety from not allowing us to fly
            StackTraceElement[] stackTrace = Thread.currentThread().getStackTrace();
            for (StackTraceElement e : stackTrace) {
                List<String> splitClass = List.of(e.getClassName().toLowerCase().split("\\."));
                if(splitClass.contains("com") && splitClass.contains("polarice3") && splitClass.contains("goety") && splitClass.contains("boss")) {
                    cir.setReturnValue(true);
                }
            }
        }
    }

    @Override
    public boolean isOmnipotent() {
        return this.isOmnipotent;
    }

    @Override
    public int getEntitiesEnlightened() {
        return this.entitiesEnlightened;
    }

    @Override
    public void setOmnipotent(boolean b) {
        this.isOmnipotent = b;
    }

    @Override
    public void setEntitiesEnlightened(int i) {
        this.entitiesEnlightened = i;
    }

}
