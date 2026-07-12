package com.warmthdawn.appliedpackaging.client.renderer;

import com.mojang.blaze3d.vertex.PoseStack;
import com.warmthdawn.appliedpackaging.AppliedPackaging;
import com.warmthdawn.appliedpackaging.world.block.entity.PackageAssemblerBlockEntity;
import appeng.client.render.effects.ParticleTypes;
import java.util.HashMap;
import java.util.Map;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.client.model.data.ModelData;

/**
 * Forge 1.20.1 adaptation of AE2 v19.2.17's molecular assembler renderer.
 * It renders the animated light chamber, the active output item and inward-flying crafting particles.
 */
public class PackageAssemblerRenderer implements BlockEntityRenderer<PackageAssemblerBlockEntity> {
    public static final ResourceLocation LIGHTS_MODEL =
            AppliedPackaging.id("block/package_assembler_lights");

    private final Map<BlockPos, Long> lastParticleTick = new HashMap<>();

    public PackageAssemblerRenderer(BlockEntityRendererProvider.Context context) {
    }

    @Override
    public void render(
            PackageAssemblerBlockEntity assembler,
            float partialTick,
            PoseStack poseStack,
            MultiBufferSource bufferSource,
            int packedLight,
            int packedOverlay) {
        if (!assembler.isCrafting()) {
            return;
        }

        renderLights(assembler, poseStack, bufferSource, packedOverlay);
        renderActivePackage(assembler, poseStack, bufferSource, packedLight, packedOverlay);
        spawnCraftingParticle(assembler);
    }

    private static void renderLights(
            PackageAssemblerBlockEntity assembler,
            PoseStack poseStack,
            MultiBufferSource bufferSource,
            int packedOverlay) {
        Minecraft minecraft = Minecraft.getInstance();
        var model = minecraft.getModelManager().getModel(LIGHTS_MODEL);
        var buffer = bufferSource.getBuffer(RenderType.tripwire());
        minecraft.getBlockRenderer().getModelRenderer().renderModel(
                poseStack.last(),
                buffer,
                assembler.getBlockState(),
                model,
                1.0F,
                1.0F,
                1.0F,
                LightTexture.FULL_BRIGHT,
                packedOverlay,
                ModelData.EMPTY,
                RenderType.tripwire());
    }

    private static void renderActivePackage(
            PackageAssemblerBlockEntity assembler,
            PoseStack poseStack,
            MultiBufferSource bufferSource,
            int packedLight,
            int packedOverlay) {
        ItemStack stack = assembler.activePackageDisplayStack();
        if (stack.isEmpty()) {
            return;
        }
        poseStack.pushPose();
        poseStack.translate(0.5F, 0.42F, 0.5F);
        poseStack.scale(1.35F, 1.35F, 1.35F);
        Minecraft.getInstance().getItemRenderer().renderStatic(
                stack,
                ItemDisplayContext.FIXED,
                packedLight,
                packedOverlay,
                poseStack,
                bufferSource,
                assembler.getLevel(),
                0);
        poseStack.popPose();
    }

    private void spawnCraftingParticle(PackageAssemblerBlockEntity assembler) {
        if (assembler.getLevel() == null || Minecraft.getInstance().isPaused()) {
            return;
        }
        long gameTime = assembler.getLevel().getGameTime();
        BlockPos pos = assembler.getBlockPos();
        Long previousTick = lastParticleTick.put(pos.immutable(), gameTime);
        if (previousTick != null && previousTick.longValue() == gameTime) {
            return;
        }
        if ((gameTime & 3L) != 0L) {
            return;
        }

        double x = pos.getX() + 0.2D + assembler.getLevel().random.nextDouble() * 0.6D;
        double y = pos.getY() + 0.2D + assembler.getLevel().random.nextDouble() * 0.6D;
        double z = pos.getZ() + 0.2D + assembler.getLevel().random.nextDouble() * 0.6D;
        Minecraft.getInstance().particleEngine.createParticle(
                ParticleTypes.CRAFTING,
                x,
                y,
                z,
                (pos.getX() + 0.5D - x) * 0.08D,
                (pos.getY() + 0.5D - y) * 0.08D,
                (pos.getZ() + 0.5D - z) * 0.08D);
    }
}
