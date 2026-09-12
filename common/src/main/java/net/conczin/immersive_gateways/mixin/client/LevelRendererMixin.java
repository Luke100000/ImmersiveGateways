package net.conczin.immersive_gateways.mixin.client;

import com.mojang.blaze3d.vertex.PoseStack;
import net.conczin.immersive_gateways.block.GatewayBlock;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.state.level.BlockOutlineRenderState;
import net.minecraft.client.renderer.state.level.LevelRenderState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LevelRenderer.class)
public class LevelRendererMixin {
    @Inject(method = "submitBlockOutline", at = @At("HEAD"), cancellable = true)
    private void immersiveGateways$submitBlockOutline(PoseStack poseStack, SubmitNodeCollector submitNodeCollector, LevelRenderState levelRenderState, CallbackInfo ci) {
        BlockOutlineRenderState state = levelRenderState.blockOutlineRenderState;
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level != null && state != null && minecraft.level.getBlockState(state.pos()).getBlock() instanceof GatewayBlock) {
            ci.cancel();
        }
    }
}
