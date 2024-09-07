package com.ibarnstormer.projectomnipotence.utils;

import com.ibarnstormer.projectomnipotence.Main;
import net.fabricmc.fabric.api.itemgroup.v1.FabricItemGroup;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.NbtComponent;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.text.Style;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.util.Rarity;

public class POCreativeTab {

    public static final ItemStack TOME_OF_TRUTH, TOME_OF_LIES;

    public static void init() {
        Registry.register(Registries.ITEM_GROUP, Identifier.of(Main.MODID, "tab"), FabricItemGroup.builder()
                .icon(() -> TOME_OF_TRUTH)
                .displayName(Text.literal("Project Omnipotence"))
                .entries((ctx, entries) -> {
                    entries.add(TOME_OF_TRUTH);
                    entries.add(TOME_OF_LIES);
                }).build());
    }

    static {
        TOME_OF_TRUTH = new ItemStack(Items.BOOK);

        NbtComponent totNbt = TOME_OF_TRUTH.getOrDefault(DataComponentTypes.CUSTOM_DATA, NbtComponent.DEFAULT);
        totNbt = totNbt.apply(nbt -> nbt.putBoolean("isPOTome", true));
        TOME_OF_TRUTH.set(DataComponentTypes.CUSTOM_DATA, totNbt);
        TOME_OF_TRUTH.set(DataComponentTypes.ENCHANTMENT_GLINT_OVERRIDE, true);
        TOME_OF_TRUTH.set(DataComponentTypes.RARITY, Rarity.EPIC);
        TOME_OF_TRUTH.set(DataComponentTypes.CUSTOM_NAME, Text.literal("Tome of the Truth").fillStyle(Style.EMPTY.withItalic(false)));

        TOME_OF_LIES = new ItemStack(Items.BOOK);

        NbtComponent tolNbt = TOME_OF_LIES.getOrDefault(DataComponentTypes.CUSTOM_DATA, NbtComponent.DEFAULT);
        tolNbt = tolNbt.apply(nbt -> nbt.putBoolean("isPOTome", false));
        TOME_OF_LIES.set(DataComponentTypes.CUSTOM_DATA, tolNbt);
        TOME_OF_LIES.set(DataComponentTypes.ENCHANTMENT_GLINT_OVERRIDE, true);
        TOME_OF_LIES.set(DataComponentTypes.RARITY, Rarity.EPIC);
        TOME_OF_LIES.set(DataComponentTypes.CUSTOM_NAME, Text.literal("Tome of Lies").fillStyle(Style.EMPTY.withItalic(false)));

    }

}
