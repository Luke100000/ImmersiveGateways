package net.conczin.immersive_gateways.fabric;

import net.conczin.immersive_gateways.*;
import net.conczin.immersive_gateways.block.GatewayExecutorController;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.level.block.entity.BlockEntityType;

import java.util.function.Consumer;

public class CommonFabric implements ModInitializer {
    private static <T> void registerHelper(Registry<T> register, Consumer<Common.RegisterHelper<T>> consumer) {
        consumer.accept((name, value) -> Registry.register(register, name, value));
    }

    @Override
    public void onInitialize() {
        ServerLifecycleEvents.SERVER_STARTING.register(server -> {
            GatewayExecutorController.reset();
            GatewayDebugCommands.reset();
        });
        ServerLifecycleEvents.SERVER_STOPPED.register(server -> {
            GatewayExecutorController.shutdown();
            GatewayDebugCommands.reset();
        });
        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> GatewayDebugCommands.register(dispatcher));

        registerHelper(BuiltInRegistries.ITEM, Items::registerItems);
        registerHelper(BuiltInRegistries.BLOCK, Blocks::registerBlocks);
        registerHelper(BuiltInRegistries.SOUND_EVENT, Sounds::registerSounds);

        //noinspection DataFlowIssue
        BlockEntityTypes.register((name, factory, block) ->
                Registry.register(BuiltInRegistries.BLOCK_ENTITY_TYPE, name, BlockEntityType.Builder.of(factory::create, block).build(null)));

        Common.init();
    }
}
