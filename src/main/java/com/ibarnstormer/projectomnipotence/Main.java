package com.ibarnstormer.projectomnipotence;

import com.ibarnstormer.projectomnipotence.capability.ModCapabilityProvider;
import com.ibarnstormer.projectomnipotence.config.ModConfig;
import com.ibarnstormer.projectomnipotence.entity.HarmonicEntity;
import com.ibarnstormer.projectomnipotence.event.ModEvents;
import com.ibarnstormer.projectomnipotence.mixin.EntityAccessor;
import com.ibarnstormer.projectomnipotence.mixin.LivingEntityInvoker;
import com.ibarnstormer.projectomnipotence.network.ModNetwork;
import com.ibarnstormer.projectomnipotence.registry.ModCreativeTab;
import com.ibarnstormer.projectomnipotence.utils.Utils;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.logging.LogUtils;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import net.neoforged.fml.javafmlmod.FMLJavaModLoadingContext;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import org.slf4j.Logger;

import java.util.Objects;

@Mod(Main.MODID)
public class Main
{   
    public static final String MODID = "projectomnipotence";
    public static ModConfig CONFIG = ModConfig.initConfig();
    private static final Logger LOGGER = LogUtils.getLogger();
    public static final int CONFIG_VERSION = 2;

    public Main(IEventBus modEventBus)
    {
        ModCreativeTab.init(modEventBus);

        FMLJavaModLoadingContext.get().getModEventBus().addListener(this::setup);
        NeoForge.EVENT_BUS.register(this);
        NeoForge.EVENT_BUS.register(ModEvents.class);
        NeoForge.EVENT_BUS.addListener(this::registerCommands);

        // Check config values
        if(CONFIG.luckLevelEntityGoal <= 0) {
            LOGGER.error("luckLevelEntityGoal has to be greater than 0, resetting to default value: 250.");
            CONFIG.luckLevelEntityGoal = 250;
        }

        if(CONFIG.totalLuckLevels < 0) {
            LOGGER.error("totalLuckLevels has to be greater than or equal to 0, resetting to default value: 3.");
            CONFIG.totalLuckLevels = 3;
        }

        if(CONFIG.invulnerabilityEntityGoal < 0) {
            LOGGER.error("invulnerabilityEntityGoal has to be greater than or equal to 0, resetting to default value: 1000");
            CONFIG.invulnerabilityEntityGoal = 1000;
        }

        if(CONFIG.flightEntityGoal < 0) {
            LOGGER.error("flightEntityGoal has to be greater than or equal to 0, resetting to default value: 10000");
            CONFIG.flightEntityGoal = 10000;
        }
    }

    @SubscribeEvent
    private void setup(final FMLCommonSetupEvent e) {
        ModNetwork.initNetwork();
    }

    @SubscribeEvent
    public void registerCommands(final RegisterCommandsEvent e) {
        e.getDispatcher().register(LiteralArgumentBuilder.<CommandSourceStack>literal("projectOmnipotence")
                .then(LiteralArgumentBuilder.<CommandSourceStack>literal("checkEntitiesEnlightened")
                    .executes(this::checkEntitiesEnlightened))
                .then(LiteralArgumentBuilder.<CommandSourceStack>literal("clearEnlightened")
                    .requires(cx -> cx.hasPermission(2))
                    .then(Commands.argument("target", EntityArgument.entity())
                        .executes(this::clearEnlightened)))
                .then(LiteralArgumentBuilder.<CommandSourceStack>literal("entitiesEnlightened")
                        .requires(cx -> cx.hasPermission(2))
                        .then(Commands.literal("get")
                                .then(Commands.argument("target", EntityArgument.player())
                                    .executes(this::outputEntitiesEnlightened)))
                        .then(Commands.literal("set")
                                .then(Commands.argument("target", EntityArgument.player())
                                .then(Commands.argument("amount", IntegerArgumentType.integer())
                                    .executes(this::setEntitiesEnlightened)))))
                .then(LiteralArgumentBuilder.<CommandSourceStack>literal("removeBadActor")
                        .requires(cx -> cx.hasPermission(2))
                        .then(Commands.argument("target", EntityArgument.player())
                                .executes(this::removeBadActor)))
                .then(LiteralArgumentBuilder.<CommandSourceStack>literal("setEnlightened")
                        .requires(cx -> cx.hasPermission(2))
                        .then(Commands.argument("target", EntityArgument.entity())
                        .executes(this::setEnlightened))));
    }

    private int checkEntitiesEnlightened(CommandContext<CommandSourceStack> context) {
        Objects.requireNonNull(context.getSource().getPlayer()).getCapability(ModCapabilityProvider.OMNIPOTENCE_CAPABILITY).ifPresent((cap) -> {
            context.getSource().sendSuccess(() -> Component.literal("§eYou've enlightened " + cap.getEnlightenedEntities() + " entities."), false);
        });
        return 1;
    }

    private int clearEnlightened(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        Entity target = EntityArgument.getEntity(context, "target");
        if(target instanceof Player player) {
            player.getCapability(ModCapabilityProvider.OMNIPOTENCE_CAPABILITY).ifPresent((cap) -> {
                if(cap.isOmnipotent()) {
                    if (!Main.CONFIG.permaOmnipotents.containsKey(player.getScoreboardName()) && !Utils.isTrueEnlightened(player)) {
                        cap.setOmnipotent(false, player.level(), player, true);
                        context.getSource().sendSuccess(() -> Component.literal(player.getScoreboardName() + " is no longer an omnipotent."), true);
                    }
                    else context.getSource().sendSuccess(() -> Component.literal(player.getScoreboardName() + "'s omnipotence cannot be removed"), false);
                }
                else context.getSource().sendSuccess(() -> Component.literal(player.getScoreboardName() + " is already not an omnipotent."), false);
            });
        }
        else if(target instanceof HarmonicEntity harmonicEntity) {
            if(harmonicEntity.getHarmonicState()) {
                harmonicEntity.setHarmonicState(false);
                context.getSource().sendSuccess(() -> Component.literal(target.getScoreboardName() + " is no longer enlightened."), true);
            }
            else context.getSource().sendSuccess(() -> Component.literal(target.getScoreboardName() + " is already not enlightened."), false);
        }
        return 1;
    }

    private int outputEntitiesEnlightened(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        Player target = EntityArgument.getPlayer(context, "target");
        target.getCapability(ModCapabilityProvider.OMNIPOTENCE_CAPABILITY).ifPresent((cap) -> {
            context.getSource().sendSuccess(() -> Component.literal(target.getScoreboardName() + " enlightened " + cap.getEnlightenedEntities() + " entities."), false);
        });
        return 1;
    }

    private int setEntitiesEnlightened(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        Player target = EntityArgument.getPlayer(context, "target");
        int amount = IntegerArgumentType.getInteger(context, "amount");
        target.getCapability(ModCapabilityProvider.OMNIPOTENCE_CAPABILITY).ifPresent((cap) -> {
            cap.setEnlightenedEntities(amount);
            context.getSource().sendSuccess(() -> Component.literal("Set entities enlightened for " + target.getScoreboardName() + " to " + amount + "."), true);
        });
        return 1;
    }

    private int setEnlightened(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        Entity target = EntityArgument.getEntity(context, "target");
        if(target instanceof Player player) {
            player.getCapability(ModCapabilityProvider.OMNIPOTENCE_CAPABILITY).ifPresent((cap) -> {
                if(!cap.isOmnipotent()) {
                    cap.setOmnipotent(true, player.level(), player, true);
                    context.getSource().sendSuccess(() -> Component.literal(player.getScoreboardName() + " is now an omnipotent."), true);
                }
                else context.getSource().sendSuccess(() -> Component.literal(player.getScoreboardName() + " is already an omnipotent."), false);
            });
        }
        else if(target instanceof HarmonicEntity harmonicEntity) {
            if(target instanceof LivingEntity livingEntity) {
                if (!harmonicEntity.getHarmonicState()) {
                    ServerPlayer player = context.getSource().getPlayer();
                    if(player != null) {
                        player.getCapability(ModCapabilityProvider.OMNIPOTENCE_CAPABILITY).ifPresent((cap) -> Utils.harmonizeEntity(livingEntity, context.getSource().getLevel(), player, target.damageSources().playerAttack(player), cap));
                    }
                    else {
                        Utils.harmonizeEntity(livingEntity, context.getSource().getLevel(), null, target.damageSources().generic(), null);
                    }

                    if(harmonicEntity.getHarmonicState()) context.getSource().sendSuccess(() -> Component.literal(target.getScoreboardName() + " is now enlightened."), true);
                    else context.getSource().sendSuccess(() -> Component.literal(target.getScoreboardName() + " cannot be enlightened."), false);
                }
                else context.getSource().sendSuccess(() -> Component.literal(target.getScoreboardName() + " is already enlightened."), false);
            }
            else context.getSource().sendSuccess(() -> Component.literal(target.getScoreboardName() + " cannot be enlightened."), false);
        }
        return 1;
    }

    private int removeBadActor(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        Player target = EntityArgument.getPlayer(context, "target");
        target.getCapability(ModCapabilityProvider.OMNIPOTENCE_CAPABILITY).ifPresent((cap) -> {
            if(cap.isOmnipotent()) {
                ((EntityAccessor) target).getEntityData().set(LivingEntityInvoker.getHealthID(), 0.0F);
                context.getSource().sendSuccess(() -> Component.literal("Removed " + target.getScoreboardName() + "."), true);
            }
            else {
                context.getSource().sendFailure(Component.literal("Player is not an omnipotent, use /kill."));
            }
        });
        return 1;
    }

}
