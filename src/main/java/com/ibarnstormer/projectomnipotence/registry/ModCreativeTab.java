package com.ibarnstormer.projectomnipotence.registry;

import com.ibarnstormer.projectomnipotence.Main;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.component.CustomData;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public class ModCreativeTab {

    public static final DeferredRegister<CreativeModeTab> MOD_TAB_REGISTRY = DeferredRegister.create(Registries.CREATIVE_MODE_TAB, Main.MODID);

    public static final DeferredHolder<CreativeModeTab, CreativeModeTab> MOD_TAB;
    public static final ItemStack TOME_OF_TRUTH, TOME_OF_LIES;


    public static void init(IEventBus bus) {
        MOD_TAB_REGISTRY.register(bus);
    }

    static {
        TOME_OF_TRUTH = new ItemStack(Items.BOOK);

        CustomData totNbt = TOME_OF_TRUTH.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY);
        totNbt = totNbt.update(nbt -> nbt.putBoolean("isPOTome", true));
        TOME_OF_TRUTH.set(DataComponents.CUSTOM_DATA, totNbt);
        TOME_OF_TRUTH.set(DataComponents.ENCHANTMENT_GLINT_OVERRIDE, true);
        TOME_OF_TRUTH.set(DataComponents.RARITY, Rarity.EPIC);
        TOME_OF_TRUTH.set(DataComponents.CUSTOM_NAME, Component.translatable("item.projectomnipotence.tome_of_truth").setStyle(Style.EMPTY.withItalic(false)));

        TOME_OF_LIES = new ItemStack(Items.BOOK);

        CustomData tolNbt = TOME_OF_LIES.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY);
        tolNbt = tolNbt.update(nbt -> nbt.putBoolean("isPOTomeReverse", true));
        TOME_OF_LIES.set(DataComponents.CUSTOM_DATA, tolNbt);
        TOME_OF_LIES.set(DataComponents.ENCHANTMENT_GLINT_OVERRIDE, true);
        TOME_OF_LIES.set(DataComponents.RARITY, Rarity.EPIC);
        TOME_OF_LIES.set(DataComponents.CUSTOM_NAME, Component.translatable("item.projectomnipotence.tome_of_lies").setStyle(Style.EMPTY.withItalic(false)));

        MOD_TAB = MOD_TAB_REGISTRY.register("tab", () ->
            CreativeModeTab.builder()
                    .icon(() -> TOME_OF_TRUTH)
                    .title(Component.translatable("itemGroup.projectomnipotence.tab"))
                    .displayItems((params, output) -> {
                        output.accept(TOME_OF_TRUTH);
                        output.accept(TOME_OF_LIES);
                    }).build());
    }

}
