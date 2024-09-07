package com.ibarnstormer.projectomnipotence.registry;

import com.ibarnstormer.projectomnipotence.Main;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.RegistryObject;

public class ModCreativeTab {

    public static final DeferredRegister<CreativeModeTab> MOD_TAB_REGISTRY = DeferredRegister.create(Registries.CREATIVE_MODE_TAB, Main.MODID);

    public static final RegistryObject<CreativeModeTab> MOD_TAB;
    public static final ItemStack TOME_OF_TRUTH, TOME_OF_LIES;


    public static void init(IEventBus bus) {
        MOD_TAB_REGISTRY.register(bus);
    }

    static {
        ListTag list = new ListTag();
        list.add(new CompoundTag());

        TOME_OF_TRUTH = new ItemStack(Items.BOOK);
        CompoundTag truthTomeNbt = new CompoundTag();
        truthTomeNbt.put("Enchantments", list);
        truthTomeNbt.putBoolean("isPOTome", true);
        TOME_OF_TRUTH.setTag(truthTomeNbt);
        TOME_OF_TRUTH.setHoverName(Component.translatable("item.projectomnipotence.tome_of_truth").setStyle(Style.EMPTY.withItalic(false)));

        TOME_OF_LIES = new ItemStack(Items.BOOK);
        CompoundTag liesTomeNbt = new CompoundTag();
        liesTomeNbt.put("Enchantments", list);
        liesTomeNbt.putBoolean("isPOTomeReverse", true);
        TOME_OF_LIES.setTag(liesTomeNbt);
        TOME_OF_LIES.setHoverName(Component.translatable("item.projectomnipotence.tome_of_lies").setStyle(Style.EMPTY.withItalic(false)));

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
