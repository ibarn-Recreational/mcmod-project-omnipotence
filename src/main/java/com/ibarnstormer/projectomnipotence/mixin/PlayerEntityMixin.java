package com.ibarnstormer.projectomnipotence.mixin;

import com.google.common.collect.Maps;
import com.ibarnstormer.projectomnipotence.Main;
import com.ibarnstormer.projectomnipotence.entity.data.ServersideDataTracker;
import com.ibarnstormer.projectomnipotence.utils.POUtils;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.attribute.EntityAttributeInstance;
import net.minecraft.entity.attribute.EntityAttributeModifier;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.effect.StatusEffect;
import net.minecraft.entity.effect.StatusEffectCategory;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.registry.Registries;
import net.minecraft.registry.tag.DamageTypeTags;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Style;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Identifier;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.*;

@Mixin(PlayerEntity.class)
public abstract class PlayerEntityMixin extends EntityMixin {

    @Unique
    private static final Identifier OMNIPOTENT_LUCK = Identifier.of(Main.MODID, "omnipotent_luck");

    @Unique
    private int eeDelta;

    @Override
    @Unique
    public void initServersideDataTracker(ServersideDataTracker.Builder builder) {
        POUtils.initPlayerData(builder);
    }

    @Inject(method = "readCustomDataFromNbt", at = @At("TAIL"))
    public void playerEntity$readCustomDataFromNbt(NbtCompound nbt, CallbackInfo ci) {
        PlayerEntity player = (PlayerEntity) (Object) this;
        POUtils.readPlayerNbt(player, nbt);
    }

   @Inject(method = "writeCustomDataToNbt", at = @At("TAIL"))
    public void playerEntity$writeCustomDataToNbt(NbtCompound nbt, CallbackInfo ci) {
        PlayerEntity player = (PlayerEntity) (Object) this;
        POUtils.writePlayerNbt(player, nbt);
    }

    @Inject(method = "damage", at = @At("HEAD"), cancellable = true)
    public void playerEntity$damage(DamageSource source, float amount, CallbackInfoReturnable<Boolean> cir) {
        PlayerEntity player = (PlayerEntity) (Object) this;
        World world = player.getWorld();
        if (POUtils.isOmnipotent(player)) {
            if(source.isIn(DamageTypeTags.BYPASSES_INVULNERABILITY) && !world.isClient() && !player.getAbilities().allowFlying && player.getY() <= world.getBottomY()) {
                MinecraftServer server = player.getServer();
                if(server != null) {
                    world.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.ENTITY_ENDERMAN_TELEPORT, SoundCategory.PLAYERS, 1.0f, 1);
                    POUtils.respawnPlayer((ServerPlayerEntity) player);
                    world.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.ENTITY_ENDERMAN_TELEPORT, SoundCategory.PLAYERS, 1.0f, 1);
                    cir.setReturnValue(false);
                }
            }

            if(POUtils.getEntitiesEnlightened(player) >= Main.CONFIG.invulnerabilityEntityGoal && Main.CONFIG.omnipotentPlayersCanBecomeInvulnerable) cir.setReturnValue(false);
            if (source.getAttacker() != null) {
                if ((!(source.getAttacker() instanceof PlayerEntity) || ((source.getAttacker() instanceof PlayerEntity playerAttacker) && !POUtils.isOmnipotent(playerAttacker))) && Main.CONFIG.omnipotentPlayersReflectDamage) {
                    if(Main.CONFIG.damageReflectionBlackList.contains(Registries.ENTITY_TYPE.getId(source.getAttacker().getType()).toString()) || Main.CONFIG.damageReflectionBlackList.contains("*")) {
                        source.getAttacker().damage(source.getAttacker().getDamageSources().generic(), amount);
                    }
                    else source.getAttacker().damage(source, amount);
                }
            }
        }
    }

    @Inject(method = "damage", at = @At("TAIL"))
    public void playerEntity$damage_onDeath(DamageSource source, float amount, CallbackInfoReturnable<Boolean> cir) {
        PlayerEntity player = (PlayerEntity) (Object) this;
        if(POUtils.isOmnipotent(player) && player.isDead() && Main.CONFIG.omnipotentPlayersReflectDamage) {
            Entity attacker = source.getAttacker();
            if(attacker != null) attacker.kill();
        }
    }

    @Inject(method = "attack", at = @At("HEAD"))
    public void playerEntity$attack(Entity target, CallbackInfo ci) {
        PlayerEntity player = (PlayerEntity) (Object) this;
        if(POUtils.isOmnipotent(player)) {
            float f = (float) player.getAttributeValue(EntityAttributes.PLAYER_SWEEPING_DAMAGE_RATIO);

            List<LivingEntity> list;

            if(f > 0) {
                list = player.getWorld().getNonSpectatingEntities(LivingEntity.class, target.getBoundingBox().expand(1.0D, 0.25D, 1.0D));
                for(LivingEntity entity : list) {
                    if(entity != target) entity.damage(entity.getDamageSources().playerAttack(player), 0.0F);
                }

                player.spawnSweepAttackParticles();
            }
            else if (target instanceof LivingEntity le) list = List.of(le);
            else list = new ArrayList<>();

            // If we want to simply remove stubborn entities
            for(LivingEntity le : list) {
                String entityID = Registries.ENTITY_TYPE.getId(le.getType()).toString();
                if(!POUtils.isInHarmony(le) && (Main.CONFIG.removeOnEnlightenList.contains(entityID) || Main.CONFIG.removeOnEnlightenList.contains("*"))) {
                    POUtils.harmonizeEntity(le, player, player.getDamageSources().playerAttack(player));
                }
                else if (!POUtils.isInHarmony(le) && Main.CONFIG.convertUponEnlightened.containsKey(entityID) && !player.isCreative()) {
                    EntityType<?> conversionType = Registries.ENTITY_TYPE.get(Identifier.of(Main.CONFIG.convertUponEnlightened.get(entityID)));
                    if(conversionType != null) {
                        Entity e = conversionType.create(player.getWorld());
                        if(le instanceof MobEntity mob && e instanceof MobEntity) {
                            EntityType<? extends MobEntity> tMobType = (EntityType<? extends MobEntity>) e.getType();
                            e = mob.convertTo(tMobType, true);
                        }
                        else if(e != null) {
                            player.getWorld().spawnEntity(e);
                            POUtils.harmonizeEntity(le, player, player.getDamageSources().playerAttack(player));
                            le.setSilent(true);
                            le.remove(Entity.RemovalReason.DISCARDED);
                            le.getWorld().playSound(null, le.getX(), le.getY(), le.getZ(), SoundEvents.ENTITY_EVOKER_PREPARE_SUMMON, SoundCategory.MASTER, 1, 2);
                        }

                        if(e instanceof LivingEntity tle) POUtils.harmonizeEntity(tle, player, player.getDamageSources().playerAttack(player));
                    }
                }
            }
        }
    }


    @Inject(method = "tick", at = @At("TAIL"))
    public void playerEntity$tick(CallbackInfo ci) {
        PlayerEntity player = (PlayerEntity) (Object) this;
        World world = player.getWorld();
        if(world instanceof ServerWorld serverWorld) {
            if ((Main.CONFIG.permaOmnipotents.containsKey(player.getNameForScoreboard()) || Main.CONFIG.permaOmnipotents.containsKey("*")) && !POUtils.isOmnipotent(player)) {
                POUtils.grantOmnipotence(player, false);
                Integer score = Main.CONFIG.permaOmnipotents.get(player.getNameForScoreboard());
                POUtils.setEntitiesEnlightened(player, Math.max((score == null ? Main.CONFIG.permaOmnipotents.get("*") : score), POUtils.getEntitiesEnlightened(player)));
            }

            if (POUtils.isTrueEnlightened(player) && !POUtils.isOmnipotent(player)) {
                POUtils.grantOmnipotence(player, false);
                POUtils.setEntitiesEnlightened(player, Math.max((Math.min(10, Main.CONFIG.totalLuckLevels) * Main.CONFIG.luckLevelEntityGoal) + 1, POUtils.getEntitiesEnlightened(player)));
            }

            EntityAttributeInstance playerLuck = player.getAttributeInstance(EntityAttributes.GENERIC_LUCK);
            assert playerLuck != null;

            if (POUtils.isOmnipotent(player)) {

                if (player.age % 5 == 0 && Main.CONFIG.omnipotentPlayerParticles && !player.isSpectator()) {
                    POUtils.spawnEnlightenmentParticles(player, serverWorld);
                }

                if(Main.CONFIG.omnipotentPlayersGlow && !player.hasStatusEffect(StatusEffects.GLOWING)) {
                    player.addStatusEffect(new StatusEffectInstance(StatusEffects.GLOWING, -1, 0, true, false, false));
                }

                Map<StatusEffect, StatusEffectInstance> localSEICollection = Maps.newHashMap();

                for (StatusEffectInstance statusEffect : player.getStatusEffects()) {
                    if (statusEffect.getEffectType().value().getCategory() == StatusEffectCategory.HARMFUL)
                        localSEICollection.put(statusEffect.getEffectType().value(), statusEffect);
                }
                for (StatusEffectInstance statusEffect : localSEICollection.values()) {
                    player.removeStatusEffect(statusEffect.getEffectType());
                }

                int score = POUtils.getEntitiesEnlightened(player);

                EntityAttributeModifier luckModifier = playerLuck.getModifier(OMNIPOTENT_LUCK);
                if (luckModifier == null && score >= Main.CONFIG.luckLevelEntityGoal) {
                    playerLuck.addPersistentModifier(new EntityAttributeModifier(OMNIPOTENT_LUCK, POUtils.getLuckLevel(player), EntityAttributeModifier.Operation.ADD_VALUE));
                }
                else if (luckModifier != null) {
                    double currentLevel = POUtils.getLuckLevel(player);
                    if(luckModifier.value() != currentLevel) {
                        playerLuck.removeModifier(OMNIPOTENT_LUCK);
                        playerLuck.addPersistentModifier(new EntityAttributeModifier(OMNIPOTENT_LUCK, currentLevel, EntityAttributeModifier.Operation.ADD_VALUE));
                    }
                }

                /* eeDelta isn't persistent so set it to score upon class load
                 * to prevent message spam each time we load a world
                 */
                if(score > 0 && eeDelta == 0) eeDelta = score;

                if (score > this.eeDelta && Math.ceil((double) score / Main.CONFIG.luckLevelEntityGoal) > Math.ceil((double) this.eeDelta / Main.CONFIG.luckLevelEntityGoal) && score < (Main.CONFIG.totalLuckLevels + 1) * Main.CONFIG.luckLevelEntityGoal && score > Main.CONFIG.luckLevelEntityGoal) {
                    player.sendMessage(Text.translatable("message.projectomnipotence.attunement").fillStyle(Style.EMPTY.withColor(Formatting.YELLOW)), false);
                }
                if (score > this.eeDelta && score >= Main.CONFIG.invulnerabilityEntityGoal && Main.CONFIG.omnipotentPlayersCanBecomeInvulnerable && eeDelta < Main.CONFIG.invulnerabilityEntityGoal) {
                    player.sendMessage(Text.translatable("message.projectomnipotence.invulnerability").fillStyle(Style.EMPTY.withColor(Formatting.YELLOW)), false);
                }
                if (score > this.eeDelta && score >= Main.CONFIG.flightEntityGoal && Main.CONFIG.omnipotentPlayersCanGainFlight && eeDelta < Main.CONFIG.flightEntityGoal) {
                    player.sendMessage(Text.translatable("message.projectomnipotence.flight").fillStyle(Style.EMPTY.withColor(Formatting.YELLOW)), false);
                }

                this.eeDelta = score;

                if(score >= Main.CONFIG.invulnerabilityEntityGoal && Main.CONFIG.omnipotentPlayersCanBecomeInvulnerable) {
                    if(player.getFrozenTicks() > 0) player.setFrozenTicks(0);
                }

                if(score >= Main.CONFIG.flightEntityGoal && Main.CONFIG.omnipotentPlayersCanGainFlight && !player.getAbilities().allowFlying) {
                    player.getAbilities().allowFlying = true;
                    player.sendAbilitiesUpdate();
                }

            } else {
                if (playerLuck.getModifier(OMNIPOTENT_LUCK) != null) {
                    playerLuck.removeModifier(OMNIPOTENT_LUCK);
                }
            }
        }
    }
}
