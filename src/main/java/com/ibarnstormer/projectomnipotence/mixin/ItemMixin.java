package com.ibarnstormer.projectomnipotence.mixin;

import com.ibarnstormer.projectomnipotence.Main;
import com.ibarnstormer.projectomnipotence.utils.POUtils;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.EnchantmentEffectComponentTypes;
import net.minecraft.component.type.NbtComponent;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.*;
import net.minecraft.registry.tag.EnchantmentTags;
import net.minecraft.text.Style;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Hand;
import net.minecraft.util.TypedActionResult;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Item.class)
public class ItemMixin {

    @Inject(method = "use", at = @At("RETURN"), cancellable = true)
    public void item$use(World world, PlayerEntity user, Hand hand, CallbackInfoReturnable<TypedActionResult<ItemStack>> cir) {
        ItemStack stack = user.getStackInHand(hand);
        if(stack.getItem() == Items.BOOK) {
            NbtComponent nbt = stack.getOrDefault(DataComponentTypes.CUSTOM_DATA, NbtComponent.DEFAULT);
            if (nbt.getNbt().getBoolean("isPOTome") && !POUtils.isOmnipotent(user)) {
                POUtils.grantOmnipotence(user, false);
                stack.decrement(1);
                cir.setReturnValue(TypedActionResult.consume(stack));
            } else if (!nbt.getNbt().getBoolean("isPOTome") && POUtils.isOmnipotent(user)) {
                if (Main.CONFIG.permaOmnipotents.containsKey(user.getNameForScoreboard()) || Main.CONFIG.permaOmnipotents.containsKey("*") || POUtils.isTrueEnlightened(user)) {
                    if (!world.isClient)
                        user.sendMessage(Text.translatable("message.projectomnipotence.failed_descend").fillStyle(Style.EMPTY.withColor(Formatting.YELLOW)), false);
                    cir.setReturnValue(TypedActionResult.fail(stack));
                } else {
                    POUtils.revokeOmnipotence(user);
                    stack.decrement(1);
                    cir.setReturnValue(TypedActionResult.consume(stack));
                }
            }
        }
    }

    @Inject(method = "inventoryTick", at = @At("HEAD"))
    public void item$inventoryTick_addAdditionalFortuneLevels(ItemStack stack, World world, Entity entity, int slot, boolean selected, CallbackInfo ci) {
        if(stack.getItem() instanceof ToolItem || stack.getItem() instanceof FishingRodItem) {
            if(entity instanceof PlayerEntity player) {
                NbtComponent nbt = stack.getOrDefault(DataComponentTypes.CUSTOM_DATA, NbtComponent.DEFAULT);
                if (POUtils.isOmnipotent(player)) {
                    int extraLevels = (int) POUtils.getLuckLevel(player);
                    if (!nbt.contains("eelevel") || (nbt.contains("eelevel") && nbt.getNbt().getInt("eelevel") != extraLevels)) {
                        nbt = nbt.apply(newNbt -> newNbt.putInt("eelevel", extraLevels));
                        stack.set(DataComponentTypes.CUSTOM_DATA, nbt);
                    }
                }
                else if (!POUtils.isOmnipotent(player) && nbt.contains("eelevel")) {
                    nbt = nbt.apply(newNbt -> newNbt.remove("eelevel"));
                    stack.set(DataComponentTypes.CUSTOM_DATA, nbt);
                }
            }
        }
    }

    @Inject(method = "inventoryTick", at = @At("HEAD"))
    public void item$inventoryTick_removeCurses(ItemStack stack, World world, Entity entity, int slot, boolean selected, CallbackInfo ci) {
        if(entity instanceof PlayerEntity player && POUtils.isOmnipotent(player) && Main.CONFIG.omnipotentPlayersRemoveCurses) {
            EnchantmentHelper.apply(stack, c -> c.remove(e -> e.isIn(EnchantmentTags.CURSE)));
        }
    }

}
