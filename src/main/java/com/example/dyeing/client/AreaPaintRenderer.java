package com.example.dyeing.client;

import com.example.dyeing.DyeingMod;
import com.example.dyeing.data.AreaPaintData;
import com.example.dyeing.data.PaintRenderState;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.client.event.RenderLevelStageEvent;
import org.joml.Matrix3f;
import org.joml.Matrix4f;

public final class AreaPaintRenderer {
    private static final ResourceLocation WHITE_TEXTURE = ResourceLocation.fromNamespaceAndPath(DyeingMod.MODID, "textures/misc/white.png");
    private static final double SURFACE_EPSILON = 0.001D;

    private AreaPaintRenderer() {
    }

    public static void render(RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_TRANSLUCENT_BLOCKS) {
            return;
        }

        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level == null || ClientPaintManager.getAreaEntries().isEmpty()) {
            return;
        }

        PoseStack poseStack = event.getPoseStack();
        Vec3 cameraPos = event.getCamera().getPosition();
        MultiBufferSource.BufferSource buffer = minecraft.renderBuffers().bufferSource();
        VertexConsumer consumer = buffer.getBuffer(RenderType.entityTranslucent(WHITE_TEXTURE));

        poseStack.pushPose();
        poseStack.translate(-cameraPos.x, -cameraPos.y, -cameraPos.z);

        float partialTick = event.getPartialTick();
        for (AreaPaintData areaPaintData : ClientPaintManager.getAreaEntries().values()) {
            Entity entity = findEntity(minecraft, areaPaintData.entityUuid());
            if (entity == null || !entity.isAlive()) {
                continue;
            }

            PaintRenderState renderState = areaPaintData.paintData().resolve(entity.tickCount + partialTick);
            if (renderState.alpha() <= 0.0F) {
                continue;
            }

            double originX = Mth.lerp(partialTick, entity.xo, entity.getX()) + renderState.offsetX();
            double originY = Mth.lerp(partialTick, entity.yo, entity.getY()) + renderState.offsetY();
            double originZ = Mth.lerp(partialTick, entity.zo, entity.getZ()) + renderState.offsetZ();

            float scale = renderState.scale();
            double x1 = originX + areaPaintData.fromX() * scale;
            double y1 = originY + areaPaintData.fromY() * scale;
            double z1 = originZ + areaPaintData.fromZ() * scale;
            double x2 = originX + areaPaintData.toX() * scale;
            double y2 = originY + areaPaintData.toY() * scale;
            double z2 = originZ + areaPaintData.toZ() * scale;

            drawCuboidSurface(
                    poseStack,
                    consumer,
                    Math.min(x1, x2),
                    Math.min(y1, y2),
                    Math.min(z1, z2),
                    Math.max(x1, x2),
                    Math.max(y1, y2),
                    Math.max(z1, z2),
                    renderState
            );
        }

        poseStack.popPose();
        buffer.endBatch(RenderType.entityTranslucent(WHITE_TEXTURE));
    }

    private static Entity findEntity(Minecraft minecraft, java.util.UUID entityUuid) {
        for (Entity entity : minecraft.level.entitiesForRendering()) {
            if (entity.getUUID().equals(entityUuid)) {
                return entity;
            }
        }
        return null;
    }

    private static void drawCuboidSurface(
            PoseStack poseStack,
            VertexConsumer consumer,
            double minX,
            double minY,
            double minZ,
            double maxX,
            double maxY,
            double maxZ,
            PaintRenderState state
    ) {
        addQuad(poseStack, consumer, minX - SURFACE_EPSILON, minY, minZ, minX - SURFACE_EPSILON, maxY, minZ, minX - SURFACE_EPSILON, maxY, maxZ, minX - SURFACE_EPSILON, minY, maxZ, -1.0F, 0.0F, 0.0F, state);
        addQuad(poseStack, consumer, maxX + SURFACE_EPSILON, minY, maxZ, maxX + SURFACE_EPSILON, maxY, maxZ, maxX + SURFACE_EPSILON, maxY, minZ, maxX + SURFACE_EPSILON, minY, minZ, 1.0F, 0.0F, 0.0F, state);
        addQuad(poseStack, consumer, minX, minY - SURFACE_EPSILON, maxZ, maxX, minY - SURFACE_EPSILON, maxZ, maxX, minY - SURFACE_EPSILON, minZ, minX, minY - SURFACE_EPSILON, minZ, 0.0F, -1.0F, 0.0F, state);
        addQuad(poseStack, consumer, minX, maxY + SURFACE_EPSILON, minZ, maxX, maxY + SURFACE_EPSILON, minZ, maxX, maxY + SURFACE_EPSILON, maxZ, minX, maxY + SURFACE_EPSILON, maxZ, 0.0F, 1.0F, 0.0F, state);
        addQuad(poseStack, consumer, maxX, minY, minZ - SURFACE_EPSILON, maxX, maxY, minZ - SURFACE_EPSILON, minX, maxY, minZ - SURFACE_EPSILON, minX, minY, minZ - SURFACE_EPSILON, 0.0F, 0.0F, -1.0F, state);
        addQuad(poseStack, consumer, minX, minY, maxZ + SURFACE_EPSILON, minX, maxY, maxZ + SURFACE_EPSILON, maxX, maxY, maxZ + SURFACE_EPSILON, maxX, minY, maxZ + SURFACE_EPSILON, 0.0F, 0.0F, 1.0F, state);
    }

    private static void addQuad(
            PoseStack poseStack,
            VertexConsumer consumer,
            double x1,
            double y1,
            double z1,
            double x2,
            double y2,
            double z2,
            double x3,
            double y3,
            double z3,
            double x4,
            double y4,
            double z4,
            float normalX,
            float normalY,
            float normalZ,
            PaintRenderState state
    ) {
        addVertex(poseStack, consumer, x1, y1, z1, 0.0F, 0.0F, normalX, normalY, normalZ, state);
        addVertex(poseStack, consumer, x2, y2, z2, 0.0F, 1.0F, normalX, normalY, normalZ, state);
        addVertex(poseStack, consumer, x3, y3, z3, 1.0F, 1.0F, normalX, normalY, normalZ, state);
        addVertex(poseStack, consumer, x4, y4, z4, 1.0F, 0.0F, normalX, normalY, normalZ, state);
    }

    private static void addVertex(
            PoseStack poseStack,
            VertexConsumer consumer,
            double x,
            double y,
            double z,
            float u,
            float v,
            float normalX,
            float normalY,
            float normalZ,
            PaintRenderState state
    ) {
        Matrix4f pose = poseStack.last().pose();
        Matrix3f normal = poseStack.last().normal();
        consumer.vertex(pose, (float) x, (float) y, (float) z)
                .color(state.red(), state.green(), state.blue(), state.alpha())
                .uv(u, v)
                .overlayCoords(OverlayTexture.NO_OVERLAY)
                .uv2(LightTexture.FULL_BRIGHT)
                .normal(normal, normalX, normalY, normalZ)
                .endVertex();
    }
}
