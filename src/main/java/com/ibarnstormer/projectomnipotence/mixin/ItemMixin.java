package com.ibarnstormer.projectomnipotence.mixin;

import com.ibarnstormer.projectomnipotence.Main;

import com.ibarnstormer.projectomnipotence.utils.POUtils;
import net.minecraft.ChatFormatting;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.tags.EnchantmentTags;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.*;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.ItemEnchantments;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Item.class)
public class ItemMixin {

    @Inject(method = "use", at = @At("RETURN"), cancellable = true)
    public void onUse(Level level, Player user, InteractionHand hand, CallbackInfoReturnable<InteractionResultHolder<ItemStack>> cir) {
        ItemStack stack = user.getItemInHand(hand);
        if (stack.getItem() == Items.BOOK) {
            CustomData customData = stack.get(DataComponents.CUSTOM_DATA);
            if (customData != null && customData.contains("isPOTome") && !POUtils.isOmnipotent(user)) {
                POUtils.setOmnipotent(true, level, user, true);
                stack.shrink(1);
                cir.setReturnValue(InteractionResultHolder.consume(stack));
            } else if (customData != null && customData.contains("isPOTomeReverse") && POUtils.isOmnipotent(user)) {
                if(Main.CONFIG.permaOmnipotents.containsKey(user.getScoreboardName()) || Main.CONFIG.permaOmnipotents.containsKey("*") || POUtils.isTrueEnlightened(user)) {
                    if (!level.isClientSide)
                        user.displayClientMessage(Component.translatable("message.projectomnipotence.failed_descend").withStyle(Style.EMPTY.withColor(ChatFormatting.YELLOW)), false);
                    cir.setReturnValue(InteractionResultHolder.fail(stack));
                }
                else {
                    POUtils.setOmnipotent(false, level, user, true);
                    stack.shrink(1);
                    cir.setReturnValue(InteractionResultHolder.consume(stack));
                }
            }
        }
    }

    @Inject(method = "inventoryTick", at = @At("HEAD"))
    public void item$inventoryTick_removeCurses(ItemStack stack, Level level, Entity entity, int p_41407_, boolean p_41408_, CallbackInfo ci) {
        if(entity instanceof Player player && POUtils.isOmnipotent(player) && Main.CONFIG.omnipotentPlayersRemoveCurses) {
            ItemEnchantments enchantments = stack.get(DataComponents.ENCHANTMENTS);
            if(enchantments != null && enchantments.keySet().stream().anyMatch(e -> e.is(EnchantmentTags.CURSE))) {
                EnchantmentHelper.updateEnchantments(stack, c -> c.removeIf(e -> e.is(EnchantmentTags.CURSE)));
            }
        }
    }
}