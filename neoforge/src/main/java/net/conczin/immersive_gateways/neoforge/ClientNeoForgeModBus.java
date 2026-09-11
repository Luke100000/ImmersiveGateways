package net.conczin.immersive_gateways.neoforge;

import net.conczin.immersive_gateways.BlockEntityTypes;
import net.conczin.immersive_gateways.Common;
import net.conczin.immersive_gateways.block.GatewayBlockEntityRenderer;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;

@EventBusSubscriber(modid = Common.MOD_ID, value = Dist.CLIENT)
public class ClientNeoForgeModBus {
    @SubscribeEvent
    public static void registerBlockEntityRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerBlockEntityRenderer(BlockEntityTypes.GATEWAY, GatewayBlockEntityRenderer::new);
    }
}
