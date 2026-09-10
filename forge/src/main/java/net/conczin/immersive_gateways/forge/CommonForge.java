package net.conczin.immersive_gateways.forge;

import net.conczin.immersive_gateways.Common;
import net.conczin.immersive_gateways.GatewayCommands;
import net.conczin.immersive_gateways.block.GatewayExecutorController;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.event.server.ServerAboutToStartEvent;
import net.minecraftforge.event.server.ServerStoppingEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod(Common.MOD_ID)
@Mod.EventBusSubscriber(modid = Common.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class CommonForge {
    @SubscribeEvent
    public static void handleServerAboutToStart(ServerAboutToStartEvent event) {
        GatewayExecutorController.reset();
        GatewayCommands.reset();
    }

    @SubscribeEvent
    public static void registerCommands(RegisterCommandsEvent event) {
        GatewayCommands.register(event.getDispatcher());
    }

    @SubscribeEvent
    public static void handleServerStopping(ServerStoppingEvent event) {
        GatewayExecutorController.shutdown();
        GatewayCommands.reset();
    }
}
