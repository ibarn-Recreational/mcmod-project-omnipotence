package com.ibarnstormer.projectomnipotence.mixin;

import com.ibarnstormer.projectomnipotence.config.POPlayerConfig;
import com.ibarnstormer.projectomnipotence.utils.POUtils;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Objects;
import net.minecraft.commands.CommandBuildContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.permissions.PermissionCheck;
import net.minecraft.server.permissions.Permissions;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;

import static net.minecraft.commands.Commands.*;

@Mixin(Commands.class)
public abstract class CommandsMixin {

    @Shadow
    @Final
    private CommandDispatcher<CommandSourceStack> dispatcher;

    @Unique
    private static final PermissionCheck PERMISSION_CHECK = new PermissionCheck.Require(Permissions.COMMANDS_GAMEMASTER);

    @Inject(at = @At(value = "INVOKE", target = "Lcom/mojang/brigadier/CommandDispatcher;setConsumer(Lcom/mojang/brigadier/ResultConsumer;)V"), method = "<init>")
    private void commandManager$init(Commands.CommandSelection environment, CommandBuildContext registryAccess, CallbackInfo ci) {
        this.dispatcher.register(literal("projectOmnipotence")
                .then(literal("checkEntitiesEnlightened")
                        .executes(this::getEEStatistics))
                .then(literal("clearEnlightened")
                        .requires(Commands.hasPermission(PERMISSION_CHECK))
                        .then(argument("target", EntityArgument.entity())
                            .executes(this::clearEnlightened)))
                .then(literal("entitiesEnlightened")
                        .requires(Commands.hasPermission(PERMISSION_CHECK))
                        .then(literal("get")
                                .then(argument("target", EntityArgument.player())
                                    .executes(this::outputEntitiesEnlightened)))
                        .then(literal("set")
                            .then(argument("target", EntityArgument.player())
                            .then(argument("amount", IntegerArgumentType.integer())
                                .executes(this::setEntitiesEnlightened)))))
                .then(literal("setEnlightened")
                        .requires(Commands.hasPermission(PERMISSION_CHECK))
                        .then(argument("target", EntityArgument.entity())
                            .executes(this::setEnlightened))));
    }

    @Unique
    private int getEEStatistics(CommandContext<CommandSourceStack> context) {
        int score = POUtils.getEntitiesEnlightened(Objects.requireNonNull(context.getSource().getPlayer()));
        context.getSource().sendSuccess(() -> Component.literal("§eYou've enlightened " + score + " entities."), false);
        return 1;
    }

    @Unique
    private int outputEntitiesEnlightened(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        Player target = EntityArgument.getPlayer(context, "target");
        int score = POUtils.getEntitiesEnlightened(target);
        context.getSource().sendSuccess(() -> Component.literal(target.getScoreboardName() + " enlightened " + score + " entities."), false);
        return 1;
    }

    @Unique
    private int setEntitiesEnlightened(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        Player player = EntityArgument.getPlayer(context, "target");
        POUtils.setEntitiesEnlightened(player, context.getArgument("amount", Integer.class));
        context.getSource().sendSuccess(() -> Component.literal("Set entities enlightened for " + player.getScoreboardName() + " to " + POUtils.getEntitiesEnlightened(player) + "."), true);
        return 1;
    }

    @Unique
    private int setEnlightened(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        Entity target = EntityArgument.getEntity(context, "target");
        boolean isPlayer = target instanceof Player;
        if(!isPlayer) {
            if(target instanceof LivingEntity livingEntity) {
                if (!POUtils.isInHarmony(target)) {
                    POUtils.harmonizeEntity(livingEntity, context.getSource().getPlayer(), target.damageSources().playerAttack(context.getSource().getPlayer()));

                    if(POUtils.isInHarmony(target)) context.getSource().sendSuccess(() -> Component.literal(target.getName().getString() + " is now " + "enlightened."), true);
                    else context.getSource().sendSuccess(() -> Component.literal(target.getName().getString() + " cannot be " + "enlightened."), false);
                }
                else context.getSource().sendSuccess(() -> Component.literal(target.getName().getString() + " is already " + "enlightened."), false);
            }
            else context.getSource().sendSuccess(() -> Component.literal(target.getName().getString() + " cannot be " + "enlightened."), false);
        }
        else {
            Player player = (Player) target;
            if(!POUtils.isOmnipotent(player)) {
                POUtils.grantOmnipotence(player, false);
                context.getSource().sendSuccess(() -> Component.literal(target.getName().getString() + " is now " + "an Omnipotent."), true);
            }
            else context.getSource().sendSuccess(() -> Component.literal(target.getName().getString() + " is already " + "an Omnipotent."), false);
        }
        return 1;
    }

    @Unique
    private int clearEnlightened(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        Entity target = EntityArgument.getEntity(context, "target");
        boolean isPlayer = target instanceof Player;
        boolean failure = false;

        boolean cannotLoseEnlightenment = false;
        if(isPlayer) {
            POPlayerConfig config = POUtils.getConfigForPlayer((Player) target);
            if(config != null) cannotLoseEnlightenment = config.enlightenedOnStart();
        }

        if(cannotLoseEnlightenment) {
            context.getSource().sendSuccess(() -> Component.literal(target.getName().getString() + "'s omnipotence cannot be removed."), false);
            return 1;
        }

        if(!isPlayer) {
            if (POUtils.isInHarmony(target) && target instanceof LivingEntity livingEntity) POUtils.setInHarmony(livingEntity, false);
            else failure = true;

            if (!failure) context.getSource().sendSuccess(() -> Component.literal(target.getName().getString() + " is no longer " + "enlightened."), true);
            else context.getSource().sendSuccess(() -> Component.literal(target.getName().getString() + " is already not " + "enlightened."), false);
        }
        else {
            Player player = (Player) target;
            if(POUtils.isOmnipotent(player)) {
                POUtils.revokeOmnipotence(player);
                context.getSource().sendSuccess(() -> Component.literal(target.getName().getString() + " is no longer " + "an Omnipotent."), true);
            }
            else context.getSource().sendSuccess(() -> Component.literal(target.getName().getString() + " is already not " + "an Omnipotent."), false);
        }
        return 1;
    }

}
