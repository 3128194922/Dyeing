package com.example.dyeing.client;

import com.example.dyeing.DyeingMod;
import com.example.dyeing.data.PaintData;
import com.example.dyeing.data.PaintRenderState;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;

import java.util.Map;

public class PaintRenderLayer<T extends LivingEntity, M extends EntityModel<T>> extends RenderLayer<T, M> {
    private static final ResourceLocation WHITE_TEXTURE = ResourceLocation.fromNamespaceAndPath(DyeingMod.MODID, "textures/misc/white.png");

    public PaintRenderLayer(RenderLayerParent<T, M> renderer) {
        super(renderer);
    }

    @Override
    public void render(
            PoseStack poseStack,
            MultiBufferSource buffer,
            int packedLight,
            T entity,
            float limbSwing,
            float limbSwingAmount,
            float partialTick,
            float ageInTicks,
            float netHeadYaw,
            float headPitch
    ) {
        if (entity.isInvisible()) {
            return;
        }

        Map<String, PaintData> paints = ClientPaintManager.getAllPaints(entity.getUUID());
        if (paints.isEmpty()) {
            return;
        }

        for (PaintData paintData : paints.values()) {
            PaintRenderState renderState = paintData.resolve(entity.level().getGameTime() + partialTick);
            if (renderState.alpha() <= 0.0F) {
                continue;
            }

            poseStack.pushPose();
            if (renderState.offsetX() != 0.0F || renderState.offsetY() != 0.0F || renderState.offsetZ() != 0.0F) {
                poseStack.translate(renderState.offsetX(), renderState.offsetY(), renderState.offsetZ());
            }
            if (renderState.scale() != 1.0F) {
                poseStack.scale(renderState.scale(), renderState.scale(), renderState.scale());
            }

            this.getParentModel().renderToBuffer(
                    poseStack,
                    buffer.getBuffer(RenderType.entityTranslucent(WHITE_TEXTURE)),
                    packedLight,
                    LivingEntityRenderer.getOverlayCoords(entity, 0.0F),
                    renderState.red(),
                    renderState.green(),
                    renderState.blue(),
                    renderState.alpha()
            );
            poseStack.popPose();
        }
    }
}
