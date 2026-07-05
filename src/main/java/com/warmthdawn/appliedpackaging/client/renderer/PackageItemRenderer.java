package com.warmthdawn.appliedpackaging.client.renderer;

import com.mojang.blaze3d.vertex.PoseStack;
import com.warmthdawn.appliedpackaging.core.package_data.PackageDataStorage;
import com.warmthdawn.appliedpackaging.item.PackageItem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.ItemRenderer;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public final class PackageItemRenderer extends BlockEntityWithoutLevelRenderer {
    private static final PackageItemRenderer INSTANCE = new PackageItemRenderer();

    private PackageItemRenderer() {
        super(null, null);
    }

    public static PackageItemRenderer instance() {
        return INSTANCE;
    }

    @Override
    public void renderByItem(
            ItemStack stack,
            ItemDisplayContext displayContext,
            PoseStack poseStack,
            MultiBufferSource bufferSource,
            int packedLight,
            int packedOverlay) {
        if (!(stack.getItem() instanceof PackageItem)) {
            return;
        }

        Minecraft minecraft = Minecraft.getInstance();
        ItemRenderer itemRenderer = minecraft.getItemRenderer();
        ItemStack baseStack = stack.copy();
        baseStack.removeTagKey(PackageDataStorage.PACKAGE_TAG);
        BakedModel baseModel = itemRenderer.getModel(baseStack, minecraft.level, null, 0);

        poseStack.pushPose();
        poseStack.translate(0.5F, 0.5F, 0.5F);
        itemRenderer.render(
                stack,
                ItemDisplayContext.NONE,
                false,
                poseStack,
                bufferSource,
                packedLight,
                packedOverlay,
                baseModel);
        PackageMarkerRenderer.renderMarkerOnPackageFront(
                stack,
                poseStack,
                bufferSource,
                packedLight,
                minecraft.level,
                itemRenderer);
        poseStack.popPose();
    }
}
