package com.warmthdawn.appliedpackaging.client.renderer;

import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.BufferUploader;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexFormat;
import com.mojang.math.Axis;
import com.warmthdawn.appliedpackaging.AppliedPackaging;
import com.warmthdawn.appliedpackaging.world.block.entity.MePackagerBlockEntity;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GameRenderer;
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
import org.joml.Matrix4f;
import org.lwjgl.opengl.GL11;

public class MePackagerRenderer implements BlockEntityRenderer<MePackagerBlockEntity> {
    public static final ResourceLocation TRAY_MODEL =
            new ResourceLocation(AppliedPackaging.MOD_ID, "block/me_packager_create/tray");
    public static final ResourceLocation HATCH_CLOSED_MODEL =
            new ResourceLocation(AppliedPackaging.MOD_ID, "block/me_packager_create/hatch_closed");
    public static final ResourceLocation HATCH_OPEN_MODEL =
            new ResourceLocation(AppliedPackaging.MOD_ID, "block/me_packager_create/hatch_open");
    private static final int STENCIL_REF = 1;
    private static final int STENCIL_MASK = 0xFF;
    private static final float CLIP_BOX_MIN = -0.001F;
    private static final float CLIP_BOX_MAX = 1.001F;

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
        Direction side = packageAnimationSide(packager);
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

        if (packager.animationTicks() > 0 && renderClippedTrayAndPackage(
                packager,
                side,
                trayOffset,
                partialTick,
                poseStack,
                packedLight,
                packedOverlay)) {
            return;
        }

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

    private boolean renderClippedTrayAndPackage(
            MePackagerBlockEntity packager,
            Direction side,
            float trayOffset,
            float partialTick,
            PoseStack poseStack,
            int packedLight,
            int packedOverlay) {
        RenderTarget target = Minecraft.getInstance().getMainRenderTarget();
        if (!target.isStencilEnabled()) {
            return false;
        }

        beginBlockStencilClip(poseStack);
        MultiBufferSource.BufferSource animationBuffer =
                MultiBufferSource.immediate(new BufferBuilder(RenderType.TRANSIENT_BUFFER_SIZE));
        try {
            renderPartial(
                    packager,
                    TRAY_MODEL,
                    side,
                    trayOffset,
                    RenderType.cutoutMipped(),
                    false,
                    poseStack,
                    animationBuffer,
                    packedLight,
                    packedOverlay);
            renderPackage(packager, side, trayOffset, partialTick, poseStack, animationBuffer, packedLight, packedOverlay);
            animationBuffer.endBatch();
        } finally {
            endBlockStencilClip();
        }
        return true;
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

    private static void beginBlockStencilClip(PoseStack poseStack) {
        // Clip rasterized animation pixels to the block volume without changing the Create partial geometry.
        GL11.glEnable(GL11.GL_STENCIL_TEST);
        RenderSystem.stencilMask(STENCIL_MASK);
        RenderSystem.clearStencil(0);
        RenderSystem.clear(GL11.GL_STENCIL_BUFFER_BIT, Minecraft.ON_OSX);

        RenderSystem.enableDepthTest();
        RenderSystem.depthFunc(GL11.GL_LEQUAL);
        RenderSystem.depthMask(false);
        RenderSystem.colorMask(false, false, false, false);
        RenderSystem.disableCull();
        RenderSystem.stencilFunc(GL11.GL_ALWAYS, STENCIL_REF, STENCIL_MASK);
        RenderSystem.stencilOp(GL11.GL_KEEP, GL11.GL_KEEP, GL11.GL_REPLACE);
        RenderSystem.setShader(GameRenderer::getPositionShader);
        drawClipBox(poseStack);

        RenderSystem.enableCull();
        RenderSystem.colorMask(true, true, true, true);
        RenderSystem.depthMask(true);
        RenderSystem.stencilMask(0);
        RenderSystem.stencilFunc(GL11.GL_EQUAL, STENCIL_REF, STENCIL_MASK);
        RenderSystem.stencilOp(GL11.GL_KEEP, GL11.GL_KEEP, GL11.GL_KEEP);
    }

    private static void endBlockStencilClip() {
        RenderSystem.stencilMask(STENCIL_MASK);
        RenderSystem.clearStencil(0);
        RenderSystem.clear(GL11.GL_STENCIL_BUFFER_BIT, Minecraft.ON_OSX);
        RenderSystem.stencilFunc(GL11.GL_ALWAYS, 0, STENCIL_MASK);
        RenderSystem.stencilOp(GL11.GL_KEEP, GL11.GL_KEEP, GL11.GL_KEEP);
        RenderSystem.stencilMask(STENCIL_MASK);
        GL11.glDisable(GL11.GL_STENCIL_TEST);
        RenderSystem.colorMask(true, true, true, true);
        RenderSystem.depthMask(true);
        RenderSystem.depthFunc(GL11.GL_LEQUAL);
        RenderSystem.enableDepthTest();
        RenderSystem.enableCull();
    }

    private static void drawClipBox(PoseStack poseStack) {
        Matrix4f pose = poseStack.last().pose();
        BufferBuilder builder = new BufferBuilder(256);
        builder.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION);
        float minX = CLIP_BOX_MIN;
        float minY = CLIP_BOX_MIN;
        float minZ = CLIP_BOX_MIN;
        float maxX = CLIP_BOX_MAX;
        float maxY = CLIP_BOX_MAX;
        float maxZ = CLIP_BOX_MAX;

        quad(builder, pose, minX, minY, minZ, maxX, minY, minZ, maxX, maxY, minZ, minX, maxY, minZ);
        quad(builder, pose, maxX, minY, maxZ, minX, minY, maxZ, minX, maxY, maxZ, maxX, maxY, maxZ);
        quad(builder, pose, minX, minY, maxZ, minX, minY, minZ, minX, maxY, minZ, minX, maxY, maxZ);
        quad(builder, pose, maxX, minY, minZ, maxX, minY, maxZ, maxX, maxY, maxZ, maxX, maxY, minZ);
        quad(builder, pose, minX, maxY, minZ, maxX, maxY, minZ, maxX, maxY, maxZ, minX, maxY, maxZ);
        quad(builder, pose, minX, minY, maxZ, maxX, minY, maxZ, maxX, minY, minZ, minX, minY, minZ);

        BufferUploader.drawWithShader(builder.end());
    }

    private static void quad(
            BufferBuilder builder,
            Matrix4f pose,
            float x1,
            float y1,
            float z1,
            float x2,
            float y2,
            float z2,
            float x3,
            float y3,
            float z3,
            float x4,
            float y4,
            float z4) {
        vertex(builder, pose, x1, y1, z1);
        vertex(builder, pose, x2, y2, z2);
        vertex(builder, pose, x3, y3, z3);
        vertex(builder, pose, x4, y4, z4);
    }

    private static void vertex(BufferBuilder builder, Matrix4f pose, float x, float y, float z) {
        builder.vertex(pose, x, y, z).endVertex();
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

    private static Direction packageAnimationSide(MePackagerBlockEntity packager) {
        return packager.networkSide();
    }
}
