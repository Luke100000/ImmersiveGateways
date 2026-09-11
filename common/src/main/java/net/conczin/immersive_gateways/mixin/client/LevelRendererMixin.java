package net.conczin.immersive_gateways.mixin.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.conczin.immersive_gateways.block.GatewayBlock;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.state.level.BlockOutlineRenderState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LevelRenderer.class)
public class LevelRendererMixin {
    @Inject(method = "renderHitOutline", at = @At("HEAD"), cancellable = true)
    private void immersiveGateways$renderHitOutline(PoseStack poseStack, VertexConsumer vertexConsumer, double cameraX, double cameraY, double cameraZ, BlockOutlineRenderState state, int color, float width, CallbackInfo ci) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level != null && minecraft.level.getBlockState(state.pos()).getBlock() instanceof GatewayBlock) {
            ci.cancel();
        }
    }
}
