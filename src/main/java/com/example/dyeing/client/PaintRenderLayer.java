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
        PaintData paintData = ClientPaintManager.get(entity.getUUID());
        if (paintData == null || entity.isInvisible()) {
            return;
        }

        PaintRenderState renderState = paintData.resolve(entity.tickCount + partialTick);
        if (renderState.alpha() <= 0.0F) {
            return;
        }

        poseStack.pushPose();
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
