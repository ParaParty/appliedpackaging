package com.warmthdawn.appliedpackaging.client.renderer;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import com.warmthdawn.appliedpackaging.world.entity.PackageEntity;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.ItemRenderer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;

public class PackageEntityRenderer extends EntityRenderer<PackageEntity> {
    private final ItemRenderer itemRenderer;

    public PackageEntityRenderer(EntityRendererProvider.Context context) {
        super(context);
        this.itemRenderer = context.getItemRenderer();
        this.shadowRadius = 0.3F;
    }

    @Override
    public void render(
            PackageEntity entity,
            float entityYaw,
            float partialTick,
            PoseStack poseStack,
            MultiBufferSource buffer,
            int packedLight) {
        ItemStack stack = entity.getPackageStack();
        if (!stack.isEmpty()) {
            poseStack.pushPose();
            poseStack.mulPose(Axis.YP.rotationDegrees(180.0F - entity.getYRot()));
            poseStack.translate(0.0F, 7.0F / 16.0F, 0.0F);
            itemRenderer.render(
                    stack,
                    ItemDisplayContext.NONE,
                    false,
                    poseStack,
                    buffer,
                    packedLight,
                    OverlayTexture.NO_OVERLAY,
                    itemRenderer.getModel(stack, entity.level(), null, entity.getId()));
            poseStack.popPose();
        }
        super.render(entity, entityYaw, partialTick, poseStack, buffer, packedLight);
    }

    @Override
    public ResourceLocation getTextureLocation(PackageEntity entity) {
        return TextureAtlas.LOCATION_BLOCKS;
    }
}
