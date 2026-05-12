package net.conczin.immersive_gateways.forge;

import net.conczin.immersive_gateways.*;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.registries.RegisterEvent;

import java.util.function.Consumer;

@Mod.EventBusSubscriber(modid = Common.MOD_ID, bus = Mod.EventBusSubscriber.Bus.MOD)
public class CommonForgeModBus {
    private static <T> void registerHelper(RegisterEvent event, Registry<T> register, Consumer<Common.RegisterHelper<T>> consumer) {
        event.register(
                register.key(),
                registry -> consumer.accept(registry::register)
        );
    }

    @SubscribeEvent
    public static void register(RegisterEvent event) {
        registerHelper(event, BuiltInRegistries.ITEM, Items::registerItems);
        registerHelper(event, BuiltInRegistries.BLOCK, Blocks::registerBlocks);
        registerHelper(event, BuiltInRegistries.SOUND_EVENT, Sounds::registerSounds);

        if (event.getRegistryKey() == Registries.BLOCK_ENTITY_TYPE) {
            event.register(Registries.BLOCK_ENTITY_TYPE, helper ->
                    BlockEntityTypes.register((name, factory, block) -> {
                        //noinspection DataFlowIssue
                        BlockEntityType<?> build = BlockEntityType.Builder.of(factory::create, block).build(null);
                        helper.register(name, build);
                        return build;
                    }));
        }
    }

    @SubscribeEvent
    static void onCommonSetup(FMLCommonSetupEvent event) {
        Common.init();
    }
}