package com.ibarnstormer.projectomnipotence.utils;

import com.ibarnstormer.projectomnipotence.Main;
import net.fabricmc.fabric.api.creativetab.v1.FabricCreativeModeTab;
import net.minecraft.core.Registry;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.resources.Identifier;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.item.*;
import net.minecraft.world.item.component.Consumable;
import net.minecraft.world.item.component.CustomData;

public class POCreativeTab {

    public static final ItemStack TOME_OF_TRUTH, TOME_OF_LIES;

    public static void init() {
        Registry.register(BuiltInRegistries.CREATIVE_MODE_TAB, Identifier.fromNamespaceAndPath(Main.MODID, "tab"), FabricCreativeModeTab.builder()
                .icon(() -> TOME_OF_TRUTH)
                .title(Component.translatable("itemGroup.projectomnipotence.tab"))
                .displayItems((ctx, entries) -> {
                    entries.accept(TOME_OF_TRUTH);
                    entries.accept(TOME_OF_LIES);
                }).build());
    }

    static {
        TOME_OF_TRUTH = new ItemStack(Items.BOOK);

        CustomData totNbt = TOME_OF_TRUTH.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY);
        totNbt = totNbt.update(nbt -> nbt.putBoolean("isPOTome", true));
        TOME_OF_TRUTH.set(DataComponents.CUSTOM_DATA, totNbt);
        TOME_OF_TRUTH.set(DataComponents.ENCHANTMENT_GLINT_OVERRIDE, true);
        TOME_OF_TRUTH.set(DataComponents.RARITY, Rarity.EPIC);
        TOME_OF_TRUTH.set(DataComponents.CUSTOM_NAME, Component.translatable("item.projectomnipotence.tome_of_truth").withStyle(Style.EMPTY.withItalic(false)));
        TOME_OF_TRUTH.set(DataComponents.CONSUMABLE, Consumable.builder().consumeSeconds(0).hasConsumeParticles(false).sound(SoundEvents.NOTE_BLOCK_BASEDRUM).animation(ItemUseAnimation.NONE).build());

        TOME_OF_LIES = new ItemStack(Items.BOOK);

        CustomData tolNbt = TOME_OF_LIES.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY);
        tolNbt = tolNbt.update(nbt -> nbt.putBoolean("isPOTome", false));
        TOME_OF_LIES.set(DataComponents.CUSTOM_DATA, tolNbt);
        TOME_OF_LIES.set(DataComponents.ENCHANTMENT_GLINT_OVERRIDE, true);
        TOME_OF_LIES.set(DataComponents.RARITY, Rarity.EPIC);
        TOME_OF_LIES.set(DataComponents.CUSTOM_NAME, Component.translatable("item.projectomnipotence.tome_of_lies").withStyle(Style.EMPTY.withItalic(false)));
        TOME_OF_LIES.set(DataComponents.CONSUMABLE, Consumable.builder().consumeSeconds(0).hasConsumeParticles(false).sound(SoundEvents.NOTE_BLOCK_BASEDRUM).animation(ItemUseAnimation.NONE).build());

    }

}
