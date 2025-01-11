package com.ibarnstormer.projectomnipotence.registry;

import com.ibarnstormer.projectomnipotence.Main;
import com.mojang.serialization.Codec;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.attachment.AttachmentType;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;

import java.util.function.Supplier;

public class ModAttachmentTypes {

    private static final DeferredRegister<AttachmentType<?>> ATTACHMENT_TYPES = DeferredRegister.create(NeoForgeRegistries.Keys.ATTACHMENT_TYPES, Main.MODID);

    public static final Supplier<AttachmentType<Boolean>> IS_OMNIPOTENT = ATTACHMENT_TYPES.register(
            "is_omnipotent", () -> AttachmentType.builder(() -> false).serialize(Codec.BOOL).copyOnDeath().build());

    public static final Supplier<AttachmentType<Integer>> ENTITIES_ENLIGHTENED = ATTACHMENT_TYPES.register(
            "entities_enlightened", () -> AttachmentType.builder(() -> 0).serialize(Codec.INT).copyOnDeath().build());

    public static void init(IEventBus bus) {
        ATTACHMENT_TYPES.register(bus);
    }


}
