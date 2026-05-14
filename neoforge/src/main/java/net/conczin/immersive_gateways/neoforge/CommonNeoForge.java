package net.conczin.immersive_gateways.neoforge;

import net.conczin.immersive_gateways.Common;
import net.conczin.immersive_gateways.GatewayDebugCommands;
import net.conczin.immersive_gateways.block.GatewayExecutorController;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.neoforge.event.server.ServerAboutToStartEvent;
import net.neoforged.neoforge.event.server.ServerStoppingEvent;

@Mod(Common.MOD_ID)
@EventBusSubscriber(modid = Common.MOD_ID, bus = EventBusSubscriber.Bus.GAME)
public class CommonNeoForge {
    @SubscribeEvent
    public static void handleServerAboutToStart(ServerAboutToStartEvent event) {
        GatewayExecutorController.reset();
        GatewayDebugCommands.reset();
    }

    @SubscribeEvent
    public static void registerCommands(RegisterCommandsEvent event) {
        GatewayDebugCommands.register(event.getDispatcher());
    }

    @SubscribeEvent
    public static void handleServerStopping(ServerStoppingEvent event) {
        GatewayExecutorController.shutdown();
        GatewayDebugCommands.reset();
    }
}
