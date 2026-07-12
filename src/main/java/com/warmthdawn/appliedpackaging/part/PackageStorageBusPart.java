package com.warmthdawn.appliedpackaging.part;

import appeng.api.config.AccessRestriction;
import appeng.api.config.Settings;
import appeng.api.parts.IPartCollisionHelper;
import appeng.api.parts.IPartItem;
import appeng.api.parts.IPartModel;
import appeng.api.parts.PartModels;
import appeng.api.storage.IStorageMounts;
import appeng.api.storage.IStorageProvider;
import appeng.parts.PartModel;
import com.warmthdawn.appliedpackaging.AppliedPackaging;
import com.warmthdawn.appliedpackaging.core.ae2.PackageItemStorage;
import net.minecraft.network.chat.Component;

public class PackageStorageBusPart extends AbstractPackageBusPart implements IStorageProvider {
    private static final IPartModel MODELS_OFF = new PartModel(
            AppliedPackaging.id("part/package_storage_bus_base"),
            AppliedPackaging.id("part/package_bus_status_off"));
    private static final IPartModel MODELS_ON = new PartModel(
            AppliedPackaging.id("part/package_storage_bus_base"),
            AppliedPackaging.id("part/package_bus_status_on"));
    private static final IPartModel MODELS_HAS_CHANNEL = new PartModel(
            AppliedPackaging.id("part/package_storage_bus_base"),
            AppliedPackaging.id("part/package_bus_status_has_channel"));

    public PackageStorageBusPart(IPartItem<?> partItem) {
        super(partItem);
        getMainNode().addService(IStorageProvider.class, this);
    }

    public static void registerModels() {
        PartModels.registerModels(
                AppliedPackaging.id("part/package_storage_bus_base"),
                AppliedPackaging.id("part/package_bus_status_off"),
                AppliedPackaging.id("part/package_bus_status_on"),
                AppliedPackaging.id("part/package_bus_status_has_channel"));
    }

    @Override
    protected int getUpgradeSlots() {
        return UPGRADE_SLOT_COUNT;
    }

    @Override
    protected void addCollisionBoxes(IPartCollisionHelper helper) {
        helper.addBox(3, 3, 15, 13, 13, 16);
        helper.addBox(2, 2, 14, 14, 14, 15);
        helper.addBox(5, 5, 12, 11, 11, 14);
    }

    @Override
    protected void tickBus() {
        if (advanceWorkTicks() >= 10) {
            resetWorkTicks();
            IStorageProvider.requestUpdate(getMainNode());
        }
    }

    @Override
    protected void configurationChanged() {
        super.configurationChanged();
        IStorageProvider.requestUpdate(getMainNode());
    }

    @Override
    public void mountInventories(IStorageMounts mounts) {
        AccessRestriction access = getConfigManager().getSetting(Settings.ACCESS);
        findTargetItemHandler().ifPresent(target -> mounts.mount(
                new PackageItemStorage(
                        target,
                        Component.translatable("item.appliedpackaging.package_storage_bus"),
                        filterSet()::matches,
                        access.isAllowInsertion(),
                        access.isAllowExtraction()),
                getPriority()));
    }

    @Override
    public IPartModel getStaticModels() {
        if (isActive() && isPowered()) {
            return MODELS_HAS_CHANNEL;
        }
        return isPowered() ? MODELS_ON : MODELS_OFF;
    }
}
