package com.ibarnstormer.projectomnipotence.mixin;

import com.ibarnstormer.projectomnipotence.Main;
import com.ibarnstormer.projectomnipotence.config.POPlayerConfig;
import com.ibarnstormer.projectomnipotence.utils.POUtils;
import net.minecraft.ChatFormatting;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.EnchantmentTags;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.Consumable;
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

    @Inject(method = "use", at = @At("HEAD"), cancellable = true)
    public void item$use(Level world, Player user, InteractionHand hand, CallbackInfoReturnable<InteractionResult> cir) {
        ItemStack stack = user.getItemInHand(hand);
        Consumable component = stack.get(DataComponents.CONSUMABLE);
        if(stack.getItem() == Items.BOOK && component != null) {
            CustomData nbt = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY);
            if (nbt.copyTag().getBoolean("isPOTome").orElse(false) && !POUtils.isOmnipotent(user)) {
                POUtils.grantOmnipotence(user, false);
                if(!user.isCreative()) stack.shrink(1);
                cir.setReturnValue(component.startConsuming(user, stack, hand));
            } else if (!nbt.copyTag().getBoolean("isPOTome").orElse(false) && POUtils.isOmnipotent(user)) {

                boolean cannotLoseEnlightenment = false;
                POPlayerConfig config = POUtils.getConfigForPlayer(user);
                if(config != null) cannotLoseEnlightenment = config.enlightenedOnStart();

                if (cannotLoseEnlightenment) {
                    if (!world.isClientSide())
                        user.sendSystemMessage(Component.translatable("message.projectomnipotence.failed_descend").withStyle(Style.EMPTY.withColor(ChatFormatting.YELLOW)));
                    cir.setReturnValue(InteractionResult.FAIL);
                } else {
                    POUtils.revokeOmnipotence(user);
                    if(!user.isCreative()) stack.shrink(1);
                    cir.setReturnValue(component.startConsuming(user, stack, hand));
                }
            }
        }
    }

    @Inject(method = "inventoryTick", at = @At("HEAD"))
    public void item$inventoryTick_removeCurses(ItemStack stack, ServerLevel world, Entity entity, EquipmentSlot slot, CallbackInfo ci) {
        if(entity instanceof Player player && POUtils.isOmnipotent(player) && Main.CONFIG.omnipotentPlayersRemoveCurses) {
            ItemEnchantments enchantments = stack.getEnchantments();
            if(enchantments.keySet().stream().anyMatch(re -> re.is(EnchantmentTags.CURSE))) {
                EnchantmentHelper.updateEnchantments(stack, c -> c.removeIf(e -> e.is(EnchantmentTags.CURSE)));
            }
        }
    }

}
