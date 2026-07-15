package com.warmthdawn.appliedpackaging.client.renderer;

import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.BufferUploader;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.blaze3d.vertex.VertexFormat;
import com.mojang.math.Axis;
import com.warmthdawn.appliedpackaging.AppliedPackaging;
import com.warmthdawn.appliedpackaging.world.block.AbstractHorizontalMachineBlock;
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
import net.minecraft.util.Mth;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.client.model.data.ModelData;
import org.joml.Matrix4f;
import org.lwjgl.opengl.GL11;

public class MePackagerRenderer implements BlockEntityRenderer<MePackagerBlockEntity> {
    public static final ResourceLocation BELT_MODEL =
            AppliedPackaging.id("block/me_packager/belt");
    public static final ResourceLocation CURTAIN_FLAP_MODEL =
            AppliedPackaging.id("block/me_packager/curtain_flap");
    private static final ResourceLocation BELT_TEXTURE =
            AppliedPackaging.id("block/me_packager/belt_scroll");

    private static final int CURTAIN_FLAP_COUNT = 4;
    private static final float CURTAIN_FLAP_STEP = 3.0F / 16.0F;
    private static final float CURTAIN_PIVOT_X = 4.0F / 16.0F;
    private static final float CURTAIN_PIVOT_Y = 14.0F / 16.0F;
    private static final float CURTAIN_PIVOT_Z = 3.5F / 16.0F;
    private static final float CURTAIN_MAX_ANGLE = 28.0F;
    private static final float BELT_TEXTURE_WIDTH_PIXELS = 32.0F;
    private static final float PACKAGE_INSIDE_X = 1.0F / 16.0F;
    private static final float PACKAGE_FRONT_CENTER_X = 10.0F / 16.0F;
    private static final float PACKAGE_RENDER_SCALE = 1.49F;
    private static final float PACKAGE_FRONT_ROTATION_Y = 90.0F;
    private static final float PACKAGE_FIXED_SCALE = 0.5F;
    private static final float PACKAGE_MODEL_MIN_Y = 1.0F / 16.0F;
    private static final float BELT_TOP_Y = 2.0F / 16.0F;
    private static final float PACKAGE_RENDER_Y = BELT_TOP_Y
            + PACKAGE_RENDER_SCALE * PACKAGE_FIXED_SCALE * (0.5F - PACKAGE_MODEL_MIN_Y);

    private static final int STENCIL_REF = 1;
    private static final int STENCIL_MASK = 0xFF;
    private static final float CLIP_LOCAL_MIN_X = 1.0F / 16.0F;
    private static final float CLIP_LOCAL_MAX_X = 1.0F;
    private static final float CLIP_LOCAL_MIN_YZ = 0.0F;
    private static final float CLIP_LOCAL_MAX_YZ = 1.0F;

    private final BlockRenderDispatcher blockRenderer;
    private final ModelManager modelManager;
    // BufferBuilder owns unmanaged LWJGL memory and has no release API in 1.20.1.
    // Keep the stencil buffers for the renderer lifetime instead of leaking new buffers every frame.
    private final MultiBufferSource.BufferSource clippedAnimationBuffer;
    private final BufferBuilder clipMaskBuffer;

    public MePackagerRenderer(BlockEntityRendererProvider.Context context) {
        this.blockRenderer = context.getBlockRenderDispatcher();
        this.modelManager = context.getBlockRenderDispatcher().getBlockModelShaper().getModelManager();
        this.clippedAnimationBuffer =
                MultiBufferSource.immediate(new BufferBuilder(RenderType.TRANSIENT_BUFFER_SIZE));
        this.clipMaskBuffer = new BufferBuilder(256);
    }

    @Override
    public void render(
            MePackagerBlockEntity packager,
            float partialTick,
            PoseStack poseStack,
            MultiBufferSource bufferSource,
            int packedLight,
            int packedOverlay) {
        Direction facing = packager.getBlockState().getValue(AbstractHorizontalMachineBlock.FACING);

        renderBelt(packager, facing, partialTick, poseStack, bufferSource, packedLight, packedOverlay);
        if (!renderClippedCurtains(packager, facing, partialTick, poseStack, packedLight, packedOverlay)) {
            renderCurtains(packager, facing, partialTick, poseStack, bufferSource, packedLight, packedOverlay);
        }

        if (renderClippedPackage(packager, facing, partialTick, poseStack, packedLight, packedOverlay)) {
            return;
        }
        renderPackage(packager, facing, partialTick, poseStack, bufferSource, packedLight, packedOverlay);
    }

    private void renderBelt(
            MePackagerBlockEntity packager,
            Direction facing,
            float partialTick,
            PoseStack poseStack,
            MultiBufferSource bufferSource,
            int packedLight,
            int packedOverlay) {
        float phasePixels = packager.beltScrollPixels();
        if (packager.animationTicks() > 0) {
            float direction = packager.animationInward() ? 1.0F : -1.0F;
            float tickTravel = packager.getPackageTravelProgress(0.0F);
            float partialTravel = packager.getPackageTravelProgress(partialTick);
            phasePixels = Mth.positiveModulo(
                    phasePixels
                            + direction
                                    * (partialTravel - tickTravel)
                                    * MePackagerBlockEntity.BELT_PACKAGE_TRAVEL_PIXELS,
                    MePackagerBlockEntity.BELT_SCROLL_PERIOD_PIXELS);
        }

        var sprite = Minecraft.getInstance().getTextureAtlas(InventoryMenu.BLOCK_ATLAS).apply(BELT_TEXTURE);
        float modelUvOffset = phasePixels * 16.0F / BELT_TEXTURE_WIDTH_PIXELS;
        float atlasUOffset = sprite.getU(modelUvOffset) - sprite.getU(0.0F);
        VertexConsumer offsetBuffer = new UvOffsetVertexConsumer(
                bufferSource.getBuffer(RenderType.cutoutMipped()), atlasUOffset);
        renderModel(
                packager,
                BELT_MODEL,
                facing,
                RenderType.cutoutMipped(),
                offsetBuffer,
                poseStack,
                packedLight,
                packedOverlay);
    }

    private void renderCurtains(
            MePackagerBlockEntity packager,
            Direction facing,
            float partialTick,
            PoseStack poseStack,
            MultiBufferSource bufferSource,
            int packedLight,
            int packedOverlay) {
        float deflection = packager.getCurtainDeflection(partialTick);
        float[] weights = {0.78F, 1.0F, 1.0F, 0.78F};
        var model = modelManager.getModel(CURTAIN_FLAP_MODEL);
        var renderType = RenderType.cutoutMipped();
        var buffer = bufferSource.getBuffer(renderType);

        poseStack.pushPose();
        rotateMachineToFacing(poseStack, facing);
        for (int flap = 0; flap < CURTAIN_FLAP_COUNT; flap++) {
            poseStack.pushPose();
            poseStack.translate(0.0F, 0.0F, flap * CURTAIN_FLAP_STEP);
            poseStack.translate(CURTAIN_PIVOT_X, CURTAIN_PIVOT_Y, CURTAIN_PIVOT_Z);
            poseStack.mulPose(Axis.ZP.rotationDegrees(deflection * CURTAIN_MAX_ANGLE * weights[flap]));
            poseStack.translate(-CURTAIN_PIVOT_X, -CURTAIN_PIVOT_Y, -CURTAIN_PIVOT_Z);
            blockRenderer.getModelRenderer().renderModel(
                    poseStack.last(),
                    buffer,
                    packager.getBlockState(),
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
        poseStack.popPose();
    }

    private boolean renderClippedCurtains(
            MePackagerBlockEntity packager,
            Direction facing,
            float partialTick,
            PoseStack poseStack,
            int packedLight,
            int packedOverlay) {
        RenderTarget target = Minecraft.getInstance().getMainRenderTarget();
        if (!target.isStencilEnabled()) {
            return false;
        }

        beginMachineStencilClip(poseStack, facing);
        try {
            renderCurtains(
                    packager,
                    facing,
                    partialTick,
                    poseStack,
                    clippedAnimationBuffer,
                    packedLight,
                    packedOverlay);
            clippedAnimationBuffer.endBatch();
        } finally {
            endBlockStencilClip();
        }
        return true;
    }

    private boolean renderClippedPackage(
            MePackagerBlockEntity packager,
            Direction facing,
            float partialTick,
            PoseStack poseStack,
            int packedLight,
            int packedOverlay) {
        RenderTarget target = Minecraft.getInstance().getMainRenderTarget();
        if (!target.isStencilEnabled()) {
            return false;
        }

        beginMachineStencilClip(poseStack, facing);
        try {
            renderPackage(
                    packager,
                    facing,
                    partialTick,
                    poseStack,
                    clippedAnimationBuffer,
                    packedLight,
                    packedOverlay);
            clippedAnimationBuffer.endBatch();
        } finally {
            endBlockStencilClip();
        }
        return true;
    }

    private void renderModel(
            MePackagerBlockEntity packager,
            ResourceLocation modelLocation,
            Direction facing,
            RenderType renderType,
            VertexConsumer buffer,
            PoseStack poseStack,
            int packedLight,
        int packedOverlay) {
        poseStack.pushPose();
        rotateMachineToFacing(poseStack, facing);
        blockRenderer.getModelRenderer().renderModel(
                poseStack.last(),
                buffer,
                packager.getBlockState(),
                modelManager.getModel(modelLocation),
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
            Direction facing,
            float partialTick,
            PoseStack poseStack,
            MultiBufferSource bufferSource,
            int packedLight,
            int packedOverlay) {
        ItemStack renderedBox = packager.getRenderedBox();
        if (renderedBox.isEmpty()) {
            return;
        }

        float packageX = packageX(packager, partialTick);
        poseStack.pushPose();
        rotateMachineToFacing(poseStack, facing);
        poseStack.translate(packageX, PACKAGE_RENDER_Y, 0.5F);
        poseStack.mulPose(Axis.YP.rotationDegrees(PACKAGE_FRONT_ROTATION_Y));
        poseStack.scale(PACKAGE_RENDER_SCALE, PACKAGE_RENDER_SCALE, PACKAGE_RENDER_SCALE);
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

    private static float packageX(MePackagerBlockEntity packager, float partialTick) {
        if (packager.animationTicks() <= 0) {
            return PACKAGE_FRONT_CENTER_X;
        }
        float progress = packager.getPackageTravelProgress(partialTick);
        if (packager.animationInward()) {
            return Mth.lerp(progress, PACKAGE_FRONT_CENTER_X, PACKAGE_INSIDE_X);
        }
        return Mth.lerp(progress, PACKAGE_INSIDE_X, PACKAGE_FRONT_CENTER_X);
    }

    private static void rotateMachineToFacing(PoseStack poseStack, Direction facing) {
        poseStack.translate(0.5F, 0.5F, 0.5F);
        switch (facing) {
            case SOUTH -> poseStack.mulPose(Axis.YP.rotationDegrees(270.0F));
            case WEST -> poseStack.mulPose(Axis.YP.rotationDegrees(180.0F));
            case NORTH -> poseStack.mulPose(Axis.YP.rotationDegrees(90.0F));
            default -> {
            }
        }
        poseStack.translate(-0.5F, -0.5F, -0.5F);
    }

    private void beginMachineStencilClip(PoseStack poseStack, Direction facing) {
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
        drawClipBox(poseStack, facing);

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

    private void drawClipBox(PoseStack poseStack, Direction facing) {
        poseStack.pushPose();
        rotateMachineToFacing(poseStack, facing);
        Matrix4f pose = poseStack.last().pose();
        BufferBuilder builder = clipMaskBuffer;
        builder.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION);
        float minX = CLIP_LOCAL_MIN_X;
        float minY = CLIP_LOCAL_MIN_YZ;
        float minZ = CLIP_LOCAL_MIN_YZ;
        float maxX = CLIP_LOCAL_MAX_X;
        float maxY = CLIP_LOCAL_MAX_YZ;
        float maxZ = CLIP_LOCAL_MAX_YZ;

        quad(builder, pose, minX, minY, minZ, maxX, minY, minZ, maxX, maxY, minZ, minX, maxY, minZ);
        quad(builder, pose, maxX, minY, maxZ, minX, minY, maxZ, minX, maxY, maxZ, maxX, maxY, maxZ);
        quad(builder, pose, minX, minY, maxZ, minX, minY, minZ, minX, maxY, minZ, minX, maxY, maxZ);
        quad(builder, pose, maxX, minY, minZ, maxX, minY, maxZ, maxX, maxY, maxZ, maxX, maxY, minZ);
        quad(builder, pose, minX, maxY, minZ, maxX, maxY, minZ, maxX, maxY, maxZ, minX, maxY, maxZ);
        quad(builder, pose, minX, minY, maxZ, maxX, minY, maxZ, maxX, minY, minZ, minX, minY, minZ);

        BufferUploader.drawWithShader(builder.end());
        poseStack.popPose();
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
}
