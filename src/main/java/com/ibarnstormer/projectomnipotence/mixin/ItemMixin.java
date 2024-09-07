package com.ibarnstormer.projectomnipotence.mixin;

import com.ibarnstormer.projectomnipotence.Main;
import com.ibarnstormer.projectomnipotence.capability.ModCapabilityProvider;
import com.ibarnstormer.projectomnipotence.utils.Utils;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.*;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.Map;

@Mixin(Item.class)
public class ItemMixin {

    @Inject(method = "use", at = @At("RETURN"), cancellable = true)
    public void onUse(Level level, Player user, InteractionHand hand, CallbackInfoReturnable<InteractionResultHolder<ItemStack>> cir) {
        ItemStack stack = user.getItemInHand(hand);
        if (stack.getItem() == Items.BOOK && stack.getTag() != null) {
            user.getCapability(ModCapabilityProvider.OMNIPOTENCE_CAPABILITY).ifPresent((cap) -> {
                if (stack.getTag().contains("isPOTome") && !cap.isOmnipotent()) {
                    cap.setOmnipotent(true, level, user, true);
                    stack.shrink(1);
                    cir.setReturnValue(InteractionResultHolder.consume(stack));
                } else if (stack.getTag().contains("isPOTomeReverse") && cap.isOmnipotent()) {
                    if(Main.CONFIG.permaOmnipotents.containsKey(user.getScoreboardName()) || Main.CONFIG.permaOmnipotents.containsKey("*") || Utils.isTrueEnlightened(user)) {
                        if (!level.isClientSide)
                            user.displayClientMessage(Component.translatable("message.projectomnipotence.failed_descend").withStyle(Style.EMPTY.withColor(ChatFormatting.YELLOW)), false);
                        cir.setReturnValue(InteractionResultHolder.fail(stack));
                    }
                    else {
                        cap.setOmnipotent(false, level, user, true);
                        stack.shrink(1);
                        cir.setReturnValue(InteractionResultHolder.consume(stack));
                    }
                }
            });
        }
    }

    @Inject(method = "inventoryTick", at = @At("HEAD"))
    public void item$inventoryTick(ItemStack stack, Level world, Entity entity, int slot, boolean selected, CallbackInfo ci) {
        if(stack.getTag() != null && (stack.getItem() instanceof TieredItem || stack.getItem() instanceof FishingRodItem)) {
            entity.getCapability(ModCapabilityProvider.OMNIPOTENCE_CAPABILITY).ifPresent((cap) -> {
                int extraLevels = entity instanceof Player player ? (int) Utils.getLuckLevel(player) : 0;
                if(cap.isOmnipotent() && !stack.getTag().contains("ee_level") || (stack.getTag().contains("ee_level") && stack.getTag().getInt("ee_level") != extraLevels))
                    stack.getTag().putInt("ee_level", extraLevels);
                else if(!cap.isOmnipotent() && stack.getTag().contains("ee_level")) stack.getTag().remove("ee_level");
            });
        }
    }

    @Inject(method = "inventoryTick", at = @At("HEAD"))
    public void item$inventoryTick_removeCurses(ItemStack stack, Level level, Entity entity, int p_41407_, boolean p_41408_, CallbackInfo ci) {
        entity.getCapability(ModCapabilityProvider.OMNIPOTENCE_CAPABILITY).ifPresent((cap) -> {
            if(cap.isOmnipotent() && Main.CONFIG.omnipotentPlayersRemoveCurses) {
                Map<Enchantment, Integer> enchantments = EnchantmentHelper.deserializeEnchantments(stack.getEnchantmentTags());
                boolean hadCurses = enchantments.keySet().removeIf(Enchantment::isCurse);
                if(hadCurses) EnchantmentHelper.setEnchantments(enchantments, stack);
            }
        });
    }
}