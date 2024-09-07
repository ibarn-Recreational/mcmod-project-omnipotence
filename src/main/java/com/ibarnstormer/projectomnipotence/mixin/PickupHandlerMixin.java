package com.ibarnstormer.projectomnipotence.mixin;

import com.ibarnstormer.projectomnipotence.Main;
import com.ibarnstormer.projectomnipotence.capability.ModCapabilityProvider;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.AgeableMob;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import tschipp.carryon.Constants;
import tschipp.carryon.common.carry.CarryOnData;
import tschipp.carryon.common.carry.CarryOnDataManager;
import tschipp.carryon.common.carry.PickupHandler;
import tschipp.carryon.common.config.ListHandler;
import tschipp.carryon.common.pickupcondition.PickupCondition;
import tschipp.carryon.common.pickupcondition.PickupConditionHandler;
import tschipp.carryon.common.scripting.CarryOnScript;
import tschipp.carryon.common.scripting.ScriptManager;
import tschipp.carryon.networking.clientbound.ClientboundStartRidingPacket;
import tschipp.carryon.platform.Services;

import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Function;

@Mixin(PickupHandler.class)
public class PickupHandlerMixin {

    // Redirects don't work with external libraries so this is the only way
    @Inject(method = "tryPickupEntity", at = @At("HEAD"), remap = false, cancellable = true)
    private static void pickupHandler$tryPickupEntity(ServerPlayer player, Entity entity, Function<Entity, Boolean> pickupCallback, CallbackInfoReturnable<Boolean> cir) {
        player.getCapability(ModCapabilityProvider.OMNIPOTENCE_CAPABILITY).ifPresent(cap -> {
            cir.setReturnValue(PickupHandlerMixin.tryPickupEntityPO(player, entity, pickupCallback, cap.isOmnipotent()));
        });

    }

    @Unique
    private static boolean tryPickupEntityPO(ServerPlayer player, Entity entity, Function<Entity, Boolean> pickupCallback, boolean enlightened) {
        if (!PickupHandler.canCarryGeneral(player, entity.position())) {
            return false;
        } else if (entity.invulnerableTime != 0) {
            return false;
        } else if (entity.isRemoved()) {
            return false;
        } else {
            if (entity instanceof TamableAnimal tame) {
                UUID owner = tame.getOwnerUUID();
                UUID playerID = player.getGameProfile().getId();
                if (owner != null && !owner.equals(playerID)) {
                    return false;
                }
            }

            if (!ListHandler.isPermitted(entity)) {
                if (!(entity instanceof AgeableMob ageableMob)) {
                    return false;
                }

                if (!Constants.COMMON_CONFIG.settings.allowBabies || ageableMob.getAge() >= 0 && !ageableMob.isBaby()) {
                    return false;
                }
            }

            if (!(player.isCreative() || (enlightened && Main.CONFIG.carryOnCompat))) {
                if (!Constants.COMMON_CONFIG.settings.pickupHostileMobs && entity.getType().getCategory() == MobCategory.MONSTER) {
                    return false;
                }

                if (Constants.COMMON_CONFIG.settings.maxEntityHeight < (double)entity.getBbHeight() || Constants.COMMON_CONFIG.settings.maxEntityWidth < (double)entity.getBbWidth()) {
                    return false;
                }
            }

            Optional<PickupCondition> cond = PickupConditionHandler.getPickupCondition(entity);
            if (cond.isPresent() && !cond.get().isFulfilled(player)) {
                return false;
            } else {
                boolean doPickup = pickupCallback == null || pickupCallback.apply(entity);
                if (!doPickup) {
                    return false;
                } else {
                    CarryOnData carry = CarryOnDataManager.getCarryData(player);
                    Optional<CarryOnScript> result = ScriptManager.inspectEntity(entity);
                    if (result.isPresent()) {
                        CarryOnScript script = result.get();
                        if (!script.fulfillsConditions(player)) {
                            return false;
                        }

                        carry.setActiveScript(script);
                    }

                    Commands var10000;
                    CommandSourceStack var10001;
                    String var10002;
                    if (entity instanceof Player otherPlayer) {
                        if (!Constants.COMMON_CONFIG.settings.pickupPlayers) {
                            return false;
                        } else if (!player.isCreative() && otherPlayer.isCreative()) {
                            return false;
                        } else {
                            otherPlayer.ejectPassengers();
                            otherPlayer.stopRiding();
                            if (result.isPresent()) {
                                String cmd = result.get().scriptEffects().commandInit();
                                if (!cmd.isEmpty()) {
                                    var10000 = Objects.requireNonNull(player.getServer()).getCommands();
                                    var10001 = player.getServer().createCommandSourceStack();
                                    var10002 = player.getGameProfile().getName();
                                    var10000.performPrefixedCommand(var10001, "/execute as " + var10002 + " run " + cmd);
                                }
                            }

                            otherPlayer.startRiding(player);
                            Services.PLATFORM.sendPacketToPlayer(Constants.PACKET_ID_START_RIDING, new ClientboundStartRidingPacket(otherPlayer.getId(), true), player);
                            carry.setCarryingPlayer();
                            player.swing(InteractionHand.MAIN_HAND, true);
                            player.level().playSound((Player)null, player.getOnPos(), SoundEvents.ARMOR_EQUIP_GENERIC, SoundSource.AMBIENT, 1.0F, 0.5F);
                            CarryOnDataManager.setCarryData(player, carry);
                            return true;
                        }
                    } else {
                        entity.ejectPassengers();
                        entity.stopRiding();
                        if (entity instanceof Animal animal) {
                            animal.dropLeash(true, true);
                        }

                        if (result.isPresent()) {
                            String cmd = result.get().scriptEffects().commandInit();
                            if (!cmd.isEmpty()) {
                                var10000 = Objects.requireNonNull(player.getServer()).getCommands();
                                var10001 = player.getServer().createCommandSourceStack();
                                var10002 = player.getGameProfile().getName();
                                var10000.performPrefixedCommand(var10001, "/execute as " + var10002 + " run " + cmd);
                            }
                        }

                        carry.setEntity(entity);
                        entity.remove(Entity.RemovalReason.UNLOADED_WITH_PLAYER);
                        player.level().playSound(null, player.getOnPos(), SoundEvents.ARMOR_EQUIP_GENERIC, SoundSource.AMBIENT, 1.0F, 0.5F);
                        CarryOnDataManager.setCarryData(player, carry);
                        player.swing(InteractionHand.MAIN_HAND, true);
                        return true;
                    }
                }
            }
        }
    }

}
