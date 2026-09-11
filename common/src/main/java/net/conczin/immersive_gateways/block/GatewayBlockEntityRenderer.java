package net.conczin.immersive_gateways.block;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.conczin.immersive_gateways.Common;
import net.conczin.immersive_gateways.IrisCompat;
import net.conczin.immersive_gateways.Utils;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.blockentity.AbstractEndPortalRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.Identifier;
import net.minecraft.util.ARGB;
import net.minecraft.world.phys.Vec3;
import org.joml.*;
import org.joml.Math;
import org.jspecify.annotations.Nullable;

public class GatewayBlockEntityRenderer implements BlockEntityRenderer<GatewayBlockEntity, GatewayBlockEntityRenderer.GatewayRenderState> {
    public static final Identifier BLANK_LOCATION = Common.locate("textures/entity/white.png");

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

    @Override
    public GatewayRenderState createRenderState() {
        return new GatewayRenderState();
    }

    @Override
    public void extractRenderState(
            GatewayBlockEntity blockEntity,
            GatewayRenderState state,
            float partialTick,
            Vec3 cameraPosition,
            ModelFeatureRenderer.@Nullable CrumblingOverlay breakProgress
    ) {
        BlockEntityRenderer.super.extractRenderState(blockEntity, state, partialTick, cameraPosition, breakProgress);
        state.partialTick = partialTick;
        state.color = blockEntity.getColor();
        state.defaultClockTime = blockEntity.getLevel() == null ? 0L : blockEntity.getLevel().getDefaultClockTime();

        for (int i = 0; i < 4; i++) {
            state.positions[i].set(blockEntity.getPosition(blockEntity.getBlockPos(), blockEntity.getBlockState(), i));
            state.lastTime[i] = blockEntity.lastTime[i];
            state.time[i] = blockEntity.time[i];
            state.offsets1[i].set(blockEntity.offsets1[i]);
            state.offsets2[i].set(blockEntity.offsets2[i]);
            state.rotations[i].set(blockEntity.rotations[i]);
        }
    }

    @Override
    public void submit(GatewayRenderState state, PoseStack poseStack, SubmitNodeCollector submitNodeCollector, CameraRenderState camera) {
        renderRuneCube(state, poseStack, submitNodeCollector, 0);
        renderRuneCube(state, poseStack, submitNodeCollector, 1);
        renderRuneCube(state, poseStack, submitNodeCollector, 2);
        renderRuneCube(state, poseStack, submitNodeCollector, 3);
    }

    private void renderRuneCube(GatewayRenderState state, PoseStack poseStack, SubmitNodeCollector submitNodeCollector, int face) {
        BlockPos blockPos = state.blockPos;
        Vector3d position = state.positions[face];

        float blinkDuration = 0.2f;
        float f = state.lastTime[face] * (1.0f - state.partialTick) + state.time[face] * state.partialTick;
        float f2 = 1.0f - Math.min(1.0f, f / (1.0f - blinkDuration));

        if (f <= 0.0) {
            return;
        }

        // Smooth position
        Vector3f offset = Utils.calculateQuadraticBezier(
                new Vector3f(0.0f, 0.0f, 0.0f),
                state.offsets1[face],
                state.offsets2[face],
                f2
        );

        // Rotation
        Quaternionf rotation = new Quaternionf();
        rotation.slerp(state.rotations[face], f2);

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
            submitNodeCollector.submitCustomGeometry(
                    poseStack,
                    RenderTypes.entitySolid(AbstractEndPortalRenderer.END_PORTAL_LOCATION),
                    (pose, buffer) -> renderCubeIris(state.defaultClockTime, state.partialTick, pose, buffer, state.lightCoords, OverlayTexture.NO_OVERLAY, state.color)
            );
        } else {
            submitNodeCollector.submitCustomGeometry(poseStack, RenderTypes.endGateway(), this::renderCube);
            submitNodeCollector.submitCustomGeometry(
                    poseStack,
                    RenderTypes.entityTranslucentEmissive(BLANK_LOCATION),
                    (pose, buffer) -> renderCube(pose, buffer, state.lightCoords, OverlayTexture.NO_OVERLAY, brightness, state.color)
            );
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
        float r = ARGB.red(color) / 255.0f;
        float g = ARGB.green(color) / 255.0f;
        float b = ARGB.blue(color) / 255.0f;
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

    public void renderCubeIris(long defaultClockTime, float tickDelta, PoseStack.Pose poseState, VertexConsumer vertexConsumer, int light, int overlay, int color) {
        float r = ARGB.red(color) / 255.0f;
        float g = ARGB.green(color) / 255.0f;
        float b = ARGB.blue(color) / 255.0f;

        Matrix4f pose = poseState.pose();
        Matrix3f normal = poseState.normal();

        // animation with a period of 100 seconds.
        // note that texture coordinates are wrapping, not clamping.
        float progress = ((defaultClockTime + tickDelta) * 0.05f * 0.01f) % 1f;
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

    public static class GatewayRenderState extends BlockEntityRenderState {
        final Vector3d[] positions = createVector3dArray();
        final Vector3f[] offsets1 = createVector3fArray();
        final Vector3f[] offsets2 = createVector3fArray();
        final Quaternionf[] rotations = createQuaternionArray();
        final float[] lastTime = new float[4];
        final float[] time = new float[4];
        float partialTick;
        int color;
        long defaultClockTime;

        private static Vector3d[] createVector3dArray() {
            return new Vector3d[]{new Vector3d(), new Vector3d(), new Vector3d(), new Vector3d()};
        }

        private static Vector3f[] createVector3fArray() {
            return new Vector3f[]{new Vector3f(), new Vector3f(), new Vector3f(), new Vector3f()};
        }

        private static Quaternionf[] createQuaternionArray() {
            return new Quaternionf[]{new Quaternionf(), new Quaternionf(), new Quaternionf(), new Quaternionf()};
        }
    }
}
