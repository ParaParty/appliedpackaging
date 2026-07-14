package com.warmthdawn.appliedpackaging.part;

import appeng.api.config.AccessRestriction;
import appeng.api.config.Settings;
import appeng.api.parts.IPartCollisionHelper;
import appeng.api.parts.IPartItem;
import appeng.api.parts.IPartModel;
import appeng.api.parts.PartModels;
import appeng.api.stacks.AEItemKey;
import appeng.api.stacks.GenericStack;
import appeng.api.storage.IStorageMounts;
import appeng.api.storage.IStorageProvider;
import appeng.parts.PartModel;
import com.warmthdawn.appliedpackaging.AppliedPackaging;
import com.warmthdawn.appliedpackaging.core.ae2.PackageItemStorage;
import com.warmthdawn.appliedpackaging.core.package_data.PackageDataStorage;
import com.warmthdawn.appliedpackaging.item.PackageItem;
import java.util.HashSet;
import java.util.Set;
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
        getConfigManager().registerSetting(Settings.ACCESS, AccessRestriction.READ_WRITE);
        getConfigManager().registerSetting(Settings.STORAGE_FILTER, appeng.api.config.StorageFilter.EXTRACTABLE_ONLY);
        getConfigManager().registerSetting(Settings.FILTER_ON_EXTRACT, appeng.api.config.YesNo.YES);
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

    /**
     * Rebuilds the visible package rules from packages currently present in the attached inventory.
     * One distinct package fills one enabled rule row, in target-slot order.
     */
    public void partitionFromTarget() {
        var target = findTargetItemHandler();
        if (target.isEmpty()) {
            return;
        }

        clearFilters();
        Set<AEItemKey> seenPackages = new HashSet<>();
        int row = 0;
        for (int slot = 0; slot < target.get().getSlots() && row < enabledRows(); slot++) {
            var stack = target.get().getStackInSlot(slot);
            if (!PackageItemStorage.isLegalPackageStack(stack) || !(stack.getItem() instanceof PackageItem packageItem)) {
                continue;
            }
            AEItemKey packageKey = AEItemKey.of(stack);
            if (!seenPackages.add(packageKey)) {
                continue;
            }
            var data = PackageDataStorage.read(stack);
            if (data.isEmpty()) {
                continue;
            }

            int currentRow = row;
            setRowColor(currentRow, packageItem.color());
            data.get().marker().ifPresent(marker -> markerFilters().setStack(currentRow, marker.stack()));

            var itemContents = data.get().contents().stream()
                    .filter(content -> content.what() instanceof AEItemKey)
                    .toList();
            // A row has six content slots. If the sampled package cannot be represented completely,
            // retain its color/marker partition instead of creating a rule that rejects the sample itself.
            if (itemContents.size() == data.get().contents().size()
                    && itemContents.size() <= CONTENTS_PER_ROW) {
                for (int column = 0; column < itemContents.size(); column++) {
                    contentFilters().setStack(
                            currentRow * CONTENTS_PER_ROW + column,
                            new GenericStack(itemContents.get(column).what(), 1));
                }
            }
            row++;
        }
        configurationChanged();
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
