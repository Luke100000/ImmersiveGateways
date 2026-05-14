package net.conczin.immersive_gateways.block;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.conczin.immersive_gateways.Common;
import net.conczin.immersive_gateways.IrisCompat;
import net.conczin.immersive_gateways.Utils;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.blockentity.TheEndPortalRenderer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.FastColor;
import org.joml.*;
import org.joml.Math;

public class GatewayBlockEntityRenderer<T extends GatewayBlockEntity> implements BlockEntityRenderer<T> {
    public static final ResourceLocation BLANK_LOCATION = Common.locate("textures/entity/white.png");

    public static final Vector3f[] NORMALS = new Vector3f[]{
            new Vector3f(0.0f, 0.0f, -1.0f),
            new Vector3f(0.0f, 0.0f, 1.0f),
            new Vector3f(0.0f, -1.0f, 0.0f),
            new Vector3f(0.0f, 1.0f, 0.0f),
            new Vector3f(-1.0f, 0.0f, 0.0f),
            new Vector3f(1.0f, 0.0f, 0.0f)
    };

    @SuppressWarnings("unused")
    public GatewayBlockEntityRenderer(BlockEntityRendererProvider.Context context) {
        // NO-OP
    }

    public void render(T blockEntity, float partialTick, PoseStack poseStack, MultiBufferSource buffer, int packedLight, int packedOverlay) {
        renderRuneCube(blockEntity, partialTick, poseStack, buffer, packedLight, packedOverlay, 0);
        renderRuneCube(blockEntity, partialTick, poseStack, buffer, packedLight, packedOverlay, 1);
        renderRuneCube(blockEntity, partialTick, poseStack, buffer, packedLight, packedOverlay, 2);
        renderRuneCube(blockEntity, partialTick, poseStack, buffer, packedLight, packedOverlay, 3);
    }

    private void renderRuneCube(T blockEntity, float partialTick, PoseStack poseStack, MultiBufferSource buffer, int packedLight, int packedOverlay, int face) {
        BlockPos blockPos = blockEntity.getBlockPos();
        Vector3d position = blockEntity.getPosition(blockPos, blockEntity.getBlockState(), face);

        float blinkDuration = 0.2f;
        float f = blockEntity.lastTime[face] * (1.0f - partialTick) + blockEntity.time[face] * partialTick;
        float f2 = 1.0f - Math.min(1.0f, f / (1.0f - blinkDuration));

        if (f <= 0.0) {
            return;
        }

        // Smooth position
        Vector3f offset = Utils.calculateQuadraticBezier(
                new Vector3f(0.0f, 0.0f, 0.0f),
                blockEntity.offsets1[face],
                blockEntity.offsets2[face],
                f2
        );

        // Rotation
        Quaternionf rotation = new Quaternionf();
        rotation.slerp(blockEntity.rotations[face], f2);

        float size = 0.25f * Math.sqrt(f);
        float brightness = Math.max(0.0f, 1.0f - Math.abs(1.0f - blinkDuration - f) / blinkDuration);

        poseStack.pushPose();
        poseStack.translate(
                position.x - blockPos.getX() + offset.x,
                position.y - blockPos.getY() + offset.y,
                position.z - blockPos.getZ() + offset.z
        );
        poseStack.mulPose(rotation);
        poseStack.scale(size, size, size);

        if (IrisCompat.isShaderPackInUse()) {
            // Iris dies of cringe with the inbuilt end gateway render type and the translucent emissive overlay trick.
            renderCubeIris(blockEntity, partialTick, poseStack, buffer, packedLight, packedOverlay, blockEntity.getColor());
        } else {
            this.renderCube(poseStack.last(), buffer.getBuffer(RenderType.endGateway()));
            this.renderCube(poseStack.last(), buffer.getBuffer(RenderType.entityTranslucentEmissive(BLANK_LOCATION)), packedLight, packedOverlay, brightness, blockEntity.getColor());
        }

        poseStack.popPose();
    }

    private void renderCube(PoseStack.Pose pose, VertexConsumer consumer) {
        this.renderFace(pose, consumer, -1.0f, 1.0f, -1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f);
        this.renderFace(pose, consumer, -1.0f, 1.0f, 1.0f, -1.0f, -1.0f, -1.0f, -1.0f, -1.0f);
        this.renderFace(pose, consumer, 1.0f, 1.0f, 1.0f, -1.0f, -1.0f, 1.0f, 1.0f, -1.0f);
        this.renderFace(pose, consumer, -1.0f, -1.0f, -1.0f, 1.0f, -1.0f, 1.0f, 1.0f, -1.0f);
        this.renderFace(pose, consumer, -1.0f, 1.0f, -1.0f, -1.0f, -1.0f, -1.0f, 1.0f, 1.0f);
        this.renderFace(pose, consumer, -1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, -1.0f, -1.0f);
    }

    private void renderCube(PoseStack.Pose pose, VertexConsumer consumer, int light, int overlay, float brightness, int color) {
        this.renderFace(pose, consumer, -1.0f, 1.0f, -1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 0, light, overlay, brightness, color);
        this.renderFace(pose, consumer, -1.0f, 1.0f, 1.0f, -1.0f, -1.0f, -1.0f, -1.0f, -1.0f, 1, light, overlay, brightness, color);
        this.renderFace(pose, consumer, 1.0f, 1.0f, 1.0f, -1.0f, -1.0f, 1.0f, 1.0f, -1.0f, 2, light, overlay, brightness, color);
        this.renderFace(pose, consumer, -1.0f, -1.0f, -1.0f, 1.0f, -1.0f, 1.0f, 1.0f, -1.0f, 3, light, overlay, brightness, color);
        this.renderFace(pose, consumer, -1.0f, 1.0f, -1.0f, -1.0f, -1.0f, -1.0f, 1.0f, 1.0f, 4, light, overlay, brightness, color);
        this.renderFace(pose, consumer, -1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, -1.0f, -1.0f, 5, light, overlay, brightness, color);
    }

    private void renderFace(PoseStack.Pose pose, VertexConsumer consumer, float x0, float x1, float y0, float y1, float z0, float z1, float z2, float z3) {
        consumer.addVertex(pose.pose(), x0, y0, z0);
        consumer.addVertex(pose.pose(), x1, y0, z1);
        consumer.addVertex(pose.pose(), x1, y1, z2);
        consumer.addVertex(pose.pose(), x0, y1, z3);
    }

    private void renderFace(PoseStack.Pose pose, VertexConsumer consumer, float x0, float x1, float y0, float y1, float z0, float z1, float z2, float z3, int face, int light, int overlay, float brightness, int color) {
        float r = FastColor.ARGB32.red(color) / 255.0f;
        float g = FastColor.ARGB32.green(color) / 255.0f;
        float b = FastColor.ARGB32.blue(color) / 255.0f;
        float a = 0.15f + 0.25f * brightness;

        float u = Math.floor(face / 2.0f) * 6.0f;
        float v = (face % 2.0f) * 6.0f;

        Vector4f p = new Vector4f();
        Vector3f n = pose.normal().transform(new Vector3f(NORMALS[face]));

        pose.pose().transform(x0, y0, z0, 1.0f, p);
        consumer.addVertex(p.x(), p.y(), p.z()).setColor(r, g, b, a).setUv(u / 32.0f, v / 32.0f).setOverlay(overlay).setLight(light).setNormal(n.x(), n.y(), n.z());

        pose.pose().transform(x1, y0, z1, 1.0f, p);
        consumer.addVertex(p.x(), p.y(), p.z()).setColor(r, g, b, a).setUv((u + 6.0f) / 32.0f, v / 32.0f).setOverlay(overlay).setLight(light).setNormal(n.x(), n.y(), n.z());

        pose.pose().transform(x1, y1, z2, 1.0f, p);
        consumer.addVertex(p.x(), p.y(), p.z()).setColor(r, g, b, a).setUv((u + 6.0f) / 32.0f, (v + 6.0f) / 32.0f).setOverlay(overlay).setLight(light).setNormal(n.x(), n.y(), n.z());

        pose.pose().transform(x0, y1, z3, 1.0f, p);
        consumer.addVertex(p.x(), p.y(), p.z()).setColor(r, g, b, a).setUv(u / 32.0f, (v + 6.0f) / 32.0f).setOverlay(overlay).setLight(light).setNormal(n.x(), n.y(), n.z());
    }

    ///  Iris support

    public void renderCubeIris(GatewayBlockEntity entity, float tickDelta, PoseStack poseStack, MultiBufferSource multiBufferSource, int light, int overlay, int color) {
        float r = FastColor.ARGB32.red(color) / 255.0f;
        float g = FastColor.ARGB32.green(color) / 255.0f;
        float b = FastColor.ARGB32.blue(color) / 255.0f;

        // POSITION_COLOR_TEXTURE_OVERLAY_LIGHT_NORMAL
        VertexConsumer vertexConsumer = multiBufferSource.getBuffer(RenderType.entitySolid(TheEndPortalRenderer.END_PORTAL_LOCATION));

        Matrix4f pose = poseStack.last().pose();
        Matrix3f normal = poseStack.last().normal();

        // animation with a period of 100 seconds.
        // note that texture coordinates are wrapping, not clamping.
        float progress = ((entity.getLevel() == null ? 0 : entity.getLevel().getDayTime() + tickDelta) * 0.05f * 0.01f) % 1f;
        float topHeight = 1.0f;
        float bottomHeight = -1.0f;

        quad(vertexConsumer, pose, normal, Direction.UP, progress, overlay, light,
                -1.0f, topHeight, 1.0f,
                1.0f, topHeight, 1.0f,
                1.0f, topHeight, -1.0f,
                -1.0f, topHeight, -1.0f,
                r, g, b);

        quad(vertexConsumer, pose, normal, Direction.DOWN, progress, overlay, light,
                -1.0f, bottomHeight, 1.0f,
                -1.0f, bottomHeight, -1.0f,
                1.0f, bottomHeight, -1.0f,
                1.0f, bottomHeight, 1.0f,
                r, g, b);

        quad(vertexConsumer, pose, normal, Direction.NORTH, progress, overlay, light,
                -1.0f, topHeight, -1.0f,
                1.0f, topHeight, -1.0f,
                1.0f, bottomHeight, -1.0f,
                -1.0f, bottomHeight, -1.0f,
                r, g, b);

        quad(vertexConsumer, pose, normal, Direction.WEST, progress, overlay, light,
                -1.0f, topHeight, 1.0f,
                -1.0f, topHeight, -1.0f,
                -1.0f, bottomHeight, -1.0f,
                -1.0f, bottomHeight, 1.0f,
                r, g, b);

        quad(vertexConsumer, pose, normal, Direction.SOUTH, progress, overlay, light,
                -1.0f, topHeight, 1.0f,
                -1.0f, bottomHeight, 1.0f,
                1.0f, bottomHeight, 1.0f,
                1.0f, topHeight, 1.0f,
                r, g, b);

        quad(vertexConsumer, pose, normal, Direction.EAST, progress, overlay, light,
                1.0f, topHeight, 1.0f,
                1.0f, bottomHeight, 1.0f,
                1.0f, bottomHeight, -1.0f,
                1.0f, topHeight, -1.0f,
                r, g, b);
    }

    private void quad(VertexConsumer vertexConsumer, Matrix4f pose, Matrix3f normal,
                      Direction direction, float progress, int overlay, int light,
                      float x1, float y1, float z1,
                      float x2, float y2, float z2,
                      float x3, float y3, float z3,
                      float x4, float y4, float z4,
                      float r, float g, float b
    ) {

        float nx = direction.getStepX();
        float ny = direction.getStepY();
        float nz = direction.getStepZ();

        vertexConsumer.addVertex(pose, x1, y1, z1).setColor(r, g, b, 1.0f)
                .setUv(0.0F + progress, 0.0F + progress).setOverlay(overlay).setLight(light)
                .setNormal(nx, ny, nz);

        vertexConsumer.addVertex(pose, x2, y2, z2).setColor(r, g, b, 1.0f)
                .setUv(0.0F + progress, 0.1F + progress).setOverlay(overlay).setLight(light)
                .setNormal(nx, ny, nz);

        vertexConsumer.addVertex(pose, x3, y3, z3).setColor(r, g, b, 1.0f)
                .setUv(0.1F + progress, 0.1F + progress).setOverlay(overlay).setLight(light)
                .setNormal(nx, ny, nz);

        vertexConsumer.addVertex(pose, x4, y4, z4).setColor(r, g, b, 1.0f)
                .setUv(0.1F + progress, 0.0F + progress).setOverlay(overlay).setLight(light)
                .setNormal(nx, ny, nz);
    }
}
