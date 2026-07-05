package com.warmthdawn.appliedpackaging.client.renderer;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import com.warmthdawn.appliedpackaging.AppliedPackaging;
import com.warmthdawn.appliedpackaging.world.block.entity.MePackagerBlockEntity;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.block.BlockRenderDispatcher;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.resources.model.ModelManager;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.client.model.data.ModelData;

public class MePackagerRenderer implements BlockEntityRenderer<MePackagerBlockEntity> {
    public static final ResourceLocation TRAY_MODEL =
            new ResourceLocation(AppliedPackaging.MOD_ID, "block/me_packager_create/tray");
    public static final ResourceLocation HATCH_CLOSED_MODEL =
            new ResourceLocation(AppliedPackaging.MOD_ID, "block/me_packager_create/hatch_closed");
    public static final ResourceLocation HATCH_OPEN_MODEL =
            new ResourceLocation(AppliedPackaging.MOD_ID, "block/me_packager_create/hatch_open");

    private final BlockRenderDispatcher blockRenderer;
    private final ModelManager modelManager;

    public MePackagerRenderer(BlockEntityRendererProvider.Context context) {
        this.blockRenderer = context.getBlockRenderDispatcher();
        this.modelManager = context.getBlockRenderDispatcher().getBlockModelShaper().getModelManager();
    }

    @Override
    public void render(
            MePackagerBlockEntity packager,
            float partialTick,
            PoseStack poseStack,
            MultiBufferSource bufferSource,
            int packedLight,
            int packedOverlay) {
        Direction side = packager.networkSide();
        float trayOffset = packager.getTrayOffset(partialTick);

        renderPartial(
                packager,
                packager.isHatchOpen() ? HATCH_OPEN_MODEL : HATCH_CLOSED_MODEL,
                side,
                0.49999F,
                RenderType.solid(),
                true,
                poseStack,
                bufferSource,
                packedLight,
                packedOverlay);
        renderPartial(
                packager,
                TRAY_MODEL,
                side,
                trayOffset,
                RenderType.cutoutMipped(),
                false,
                poseStack,
                bufferSource,
                packedLight,
                packedOverlay);
        renderPackage(packager, side, trayOffset, partialTick, poseStack, bufferSource, packedLight, packedOverlay);
    }

    private void renderPartial(
            MePackagerBlockEntity packager,
            ResourceLocation modelLocation,
            Direction side,
            float offset,
            RenderType renderType,
            boolean hatch,
            PoseStack poseStack,
            MultiBufferSource bufferSource,
            int packedLight,
            int packedOverlay) {
        var model = modelManager.getModel(modelLocation);
        var blockState = packager.getBlockState();
        var buffer = bufferSource.getBuffer(renderType);

        poseStack.pushPose();
        poseStack.translate(
                side.getStepX() * offset,
                side.getStepY() * offset,
                side.getStepZ() * offset);
        if (hatch) {
            rotateHatchToSide(poseStack, side);
        } else {
            rotateHorizontalToSide(poseStack, side);
        }
        blockRenderer.getModelRenderer().renderModel(
                poseStack.last(),
                buffer,
                blockState,
                model,
                1.0F,
                1.0F,
                1.0F,
                packedLight,
                packedOverlay,
                ModelData.EMPTY,
                renderType);
        poseStack.popPose();
    }

    private void renderPackage(
            MePackagerBlockEntity packager,
            Direction side,
            float trayOffset,
            float partialTick,
            PoseStack poseStack,
            MultiBufferSource bufferSource,
            int packedLight,
            int packedOverlay) {
        ItemStack renderedBox = packager.getRenderedBox();
        if (renderedBox.isEmpty()) {
            return;
        }

        poseStack.pushPose();
        poseStack.translate(
                side.getStepX() * trayOffset,
                side.getStepY() * trayOffset,
                side.getStepZ() * trayOffset);
        poseStack.translate(0.5F, 0.5F, 0.5F);
        rotateHorizontalAroundCenter(poseStack, side);
        poseStack.translate(0.0F, 2.0F / 16.0F, 0.0F);
        poseStack.scale(1.49F, 1.49F, 1.49F);
        Minecraft.getInstance().getItemRenderer().renderStatic(
                renderedBox,
                ItemDisplayContext.FIXED,
                packedLight,
                packedOverlay,
                poseStack,
                bufferSource,
                packager.getLevel(),
                0);
        poseStack.popPose();
    }

    private static void rotateHatchToSide(PoseStack poseStack, Direction side) {
        poseStack.translate(0.5F, 0.5F, 0.5F);
        poseStack.mulPose(Axis.YP.rotationDegrees(horizontalAngle(side)));
        poseStack.mulPose(Axis.XP.rotationDegrees(verticalAngle(side)));
        poseStack.translate(-0.5F, -0.5F, -0.5F);
    }

    private static void rotateHorizontalToSide(PoseStack poseStack, Direction side) {
        poseStack.translate(0.5F, 0.5F, 0.5F);
        rotateHorizontalAroundCenter(poseStack, side);
        poseStack.translate(-0.5F, -0.5F, -0.5F);
    }

    private static void rotateHorizontalAroundCenter(PoseStack poseStack, Direction side) {
        poseStack.mulPose(Axis.YP.rotationDegrees(horizontalAngle(side)));
    }

    private static float horizontalAngle(Direction side) {
        return side.getAxis().isVertical() ? 0.0F : side.toYRot();
    }

    private static float verticalAngle(Direction side) {
        if (side == Direction.UP) {
            return -90.0F;
        }
        if (side == Direction.DOWN) {
            return 90.0F;
        }
        return 0.0F;
    }
}
