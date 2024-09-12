package com.ibarnstormer.projectomnipotence.mixin;

import com.ibarnstormer.projectomnipotence.Main;
import com.ibarnstormer.projectomnipotence.utils.POUtils;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.command.CommandRegistryAccess;
import net.minecraft.command.argument.EntityArgumentType;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.text.Text;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Objects;

import static net.minecraft.server.command.CommandManager.*;

@Mixin(CommandManager.class)
public abstract class CommandManagerMixin {

    @Shadow
    @Final
    private CommandDispatcher<ServerCommandSource> dispatcher;

    @Inject(at = @At(value = "INVOKE", target = "Lcom/mojang/brigadier/CommandDispatcher;setConsumer(Lcom/mojang/brigadier/ResultConsumer;)V"), method = "<init>")
    private void commandManager$init(CommandManager.RegistrationEnvironment environment, CommandRegistryAccess registryAccess, CallbackInfo ci) {
        this.dispatcher.register(literal("projectOmnipotence")
                .then(literal("checkEntitiesEnlightened")
                        .executes(this::getEEStatistics))
                .then(literal("clearEnlightened")
                        .requires(source -> source.hasPermissionLevel(2))
                        .then(argument("target", EntityArgumentType.entity())
                            .executes(this::clearEnlightened)))
                .then(literal("entitiesEnlightened")
                        .requires(source -> source.hasPermissionLevel(2))
                        .then(literal("get")
                                .then(argument("target", EntityArgumentType.player())
                                    .executes(this::outputEntitiesEnlightened)))
                        .then(literal("set")
                            .then(argument("target", EntityArgumentType.player())
                            .then(argument("amount", IntegerArgumentType.integer())
                                .executes(this::setEntitiesEnlightened)))))
                .then(literal("removeBadActor")
                        .requires(source -> source.hasPermissionLevel(2))
                        .then(argument("target", EntityArgumentType.player())
                            .executes(this::removeBadActor)))
                .then(literal("setEnlightened")
                        .requires(source -> source.hasPermissionLevel(2))
                        .then(argument("target", EntityArgumentType.entity())
                            .executes(this::setEnlightened))));
    }

    @Unique
    private int getEEStatistics(CommandContext<ServerCommandSource> context) {
        int score = POUtils.getEntitiesEnlightened(Objects.requireNonNull(context.getSource().getPlayer()));
        context.getSource().sendFeedback(() -> Text.literal("§eYou've enlightened " + score + " entities."), false);
        return 1;
    }

    @Unique
    private int outputEntitiesEnlightened(CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
        PlayerEntity target = EntityArgumentType.getPlayer(context, "target");
        int score = POUtils.getEntitiesEnlightened(target);
        context.getSource().sendFeedback(() -> Text.literal(target.getNameForScoreboard() + " enlightened " + score + " entities."), false);
        return 1;
    }

    @Unique
    private int setEntitiesEnlightened(CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
        PlayerEntity player = EntityArgumentType.getPlayer(context, "target");
        POUtils.setEntitiesEnlightened(player, context.getArgument("amount", Integer.class));
        context.getSource().sendFeedback(() -> Text.literal("Set entities enlightened for " + player.getNameForScoreboard() + " to " + POUtils.getEntitiesEnlightened(player) + "."), true);
        return 1;
    }

    @Unique
    private int setEnlightened(CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
        Entity target = EntityArgumentType.getEntity(context, "target");
        boolean isPlayer = target instanceof PlayerEntity;
        if(!isPlayer) {
            if(target instanceof LivingEntity livingEntity) {
                if (!POUtils.isInHarmony(target)) {
                    POUtils.harmonizeEntity(livingEntity, context.getSource().getPlayer(), target.getDamageSources().playerAttack(context.getSource().getPlayer()));

                    if(POUtils.isInHarmony(target)) context.getSource().sendFeedback(() -> Text.literal(target.getName().getString() + " is now " + "enlightened."), true);
                    else context.getSource().sendFeedback(() -> Text.literal(target.getName().getString() + " cannot be " + "enlightened."), false);
                }
                else context.getSource().sendFeedback(() -> Text.literal(target.getName().getString() + " is already " + "enlightened."), false);
            }
            else context.getSource().sendFeedback(() -> Text.literal(target.getName().getString() + " cannot be " + "enlightened."), false);
        }
        else {
            PlayerEntity player = (PlayerEntity) target;
            if(!POUtils.isOmnipotent(player)) {
                POUtils.grantOmnipotence(player, false);
                context.getSource().sendFeedback(() -> Text.literal(target.getName().getString() + " is now " + "an Omnipotent."), true);
            }
            else context.getSource().sendFeedback(() -> Text.literal(target.getName().getString() + " is already " + "an Omnipotent."), false);
        }
        return 1;
    }

    @Unique
    private int clearEnlightened(CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
        Entity target = EntityArgumentType.getEntity(context, "target");
        boolean isPlayer = target instanceof PlayerEntity;
        boolean failure = false;

        if(target instanceof PlayerEntity playerTarget && (Main.CONFIG.permaOmnipotents.containsKey(playerTarget.getNameForScoreboard()) || POUtils.isTrueEnlightened(playerTarget))) {
            context.getSource().sendFeedback(() -> Text.literal(target.getName().getString() + "'s omnipotence cannot be removed."), false);
            return 1;
        }

        if(!isPlayer) {
            if (POUtils.isInHarmony(target) && target instanceof LivingEntity livingEntity) POUtils.setInHarmony(livingEntity, false);
            else failure = true;

            if (!failure) context.getSource().sendFeedback(() -> Text.literal(target.getName().getString() + " is no longer " + "enlightened."), true);
            else context.getSource().sendFeedback(() -> Text.literal(target.getName().getString() + " is already not " + "enlightened."), false);
        }
        else {
            PlayerEntity player = (PlayerEntity) target;
            if(POUtils.isOmnipotent(player)) {
                POUtils.revokeOmnipotence(player);
                context.getSource().sendFeedback(() -> Text.literal(target.getName().getString() + " is no longer " + "an Omnipotent."), true);
            }
            else context.getSource().sendFeedback(() -> Text.literal(target.getName().getString() + " is already not " + "an Omnipotent."), false);
        }
        return 1;
    }

    @Unique
    private int removeBadActor(CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
        PlayerEntity player = EntityArgumentType.getPlayer(context, "target");

        if(POUtils.isOmnipotent(player)) {
            ((EntityAccessor) player).getDataTracker().set(LivingEntityInvoker.getHealthID(), 0.0F);
            context.getSource().sendFeedback(() -> Text.literal("Removed " + player.getNameForScoreboard() + "."), true);
        }
        else {
            context.getSource().sendError(Text.literal("Player is not an omnipotent, use /kill."));
        }

        return 1;
    }

}
