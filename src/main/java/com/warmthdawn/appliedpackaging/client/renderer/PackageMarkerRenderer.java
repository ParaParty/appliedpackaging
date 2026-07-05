package com.warmthdawn.appliedpackaging.client.renderer;

import appeng.api.stacks.AEItemKey;
import appeng.api.stacks.AEKey;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import com.warmthdawn.appliedpackaging.AppliedPackaging;
import com.warmthdawn.appliedpackaging.core.package_data.PackageDataStorage;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.ItemRenderer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

public final class PackageMarkerRenderer {
    public static final ResourceLocation HAS_MARKER_PROPERTY =
            new ResourceLocation(AppliedPackaging.MOD_ID, "has_marker");

    private static final float PX = 1.0F / 16.0F;
    private static final float MARKER_CENTER_X = 6.0F * PX;
    private static final float MARKER_CENTER_Y = 4.0F * PX;
    private static final float FRONT_Z = 3.0F * PX;
    private static final float MARKER_SIZE = 3.0F * PX;

    private PackageMarkerRenderer() {
    }

    public static ItemStack markerItem(ItemStack packageStack) {
        return PackageDataStorage.read(packageStack)
                .flatMap(data -> data.marker())
                .map(marker -> marker.stack().what())
                .filter(AEItemKey.class::isInstance)
                .map(AEItemKey.class::cast)
                .map(AEItemKey::toStack)
                .orElse(ItemStack.EMPTY);
    }

    public static void renderMarkerOnPackageFront(
            ItemStack packageStack,
            PoseStack poseStack,
            MultiBufferSource bufferSource,
            int packedLight,
            Level level,
            ItemRenderer itemRenderer) {
        ItemStack marker = markerItem(packageStack);
        if (marker.isEmpty()) {
            return;
        }

        poseStack.pushPose();
        poseStack.translate(-0.5F, -0.5F, -0.5F);
        poseStack.translate(MARKER_CENTER_X, MARKER_CENTER_Y, FRONT_Z - 1.0F / 512.0F);
        poseStack.mulPose(Axis.YP.rotationDegrees(180.0F));
        poseStack.scale(MARKER_SIZE, MARKER_SIZE, 1.0F / 1024.0F);
        itemRenderer.renderStatic(
                marker,
                ItemDisplayContext.GUI,
                packedLight,
                OverlayTexture.NO_OVERLAY,
                poseStack,
                bufferSource,
                level,
                0);
        poseStack.popPose();
    }

    public static boolean hasItemMarker(ItemStack packageStack) {
        AEKey key = PackageDataStorage.read(packageStack)
                .flatMap(data -> data.marker())
                .map(marker -> marker.stack().what())
                .orElse(null);
        return key instanceof AEItemKey;
    }
}
