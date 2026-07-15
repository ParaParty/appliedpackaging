package com.warmthdawn.appliedpackaging.part;

import appeng.api.crafting.PatternDetailsHelper;
import appeng.api.inventories.InternalInventory;
import appeng.core.definitions.AEItems;
import appeng.util.inv.AppEngInternalInventory;
import appeng.util.inv.InternalInventoryHost;
import appeng.util.inv.filter.IAEItemFilter;
import appeng.util.ConfigInventory;
import com.warmthdawn.appliedpackaging.core.package_data.MarkerSpec;
import com.warmthdawn.appliedpackaging.core.package_data.PackageCraftingPatternDataStorage;
import com.warmthdawn.appliedpackaging.item.PackageColor;
import com.warmthdawn.appliedpackaging.item.PackageItem;
import java.util.Optional;
import java.util.function.BooleanSupplier;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;

public final class PackagePatternEncodingState implements InternalInventoryHost {
    private static final String STATE_TAG = "appliedpackagingPackagePatternTerminal";
    private static final String COLOR_TAG = "color";
    private static final String MARKER_TAG = "marker";
    private static final String INPUTS_TAG = "inputs";

    private final Runnable changeListener;
    private final BooleanSupplier clientSide;
    private final AppEngInternalInventory markerInventory = new AppEngInternalInventory(this, 1, 1);
    private final ConfigInventory inputs = ConfigInventory.configStacks(
            null,
            PackageCraftingPatternDataStorage.INPUT_SLOT_COUNT,
            this::saveChanges,
            true);
    private PackageColor color = PackageColor.FLUIX;
    private boolean loading;

    public PackagePatternEncodingState(Runnable changeListener, BooleanSupplier clientSide) {
        this.changeListener = changeListener;
        this.clientSide = clientSide;
        markerInventory.setFilter(new MarkerFilter());
    }

    public PackageColor color() {
        return color;
    }

    public void setColor(PackageColor color) {
        PackageColor value = color == null ? PackageColor.FLUIX : color;
        if (this.color != value) {
            this.color = value;
            saveChanges();
        }
    }

    public InternalInventory markerInventory() {
        return markerInventory;
    }

    public ConfigInventory inputs() {
        return inputs;
    }

    public void loadMarker(Optional<MarkerSpec> marker) {
        ItemStack markerStack = marker
                .filter(spec -> spec.stack().what() instanceof appeng.api.stacks.AEItemKey)
                .map(spec -> ((appeng.api.stacks.AEItemKey) spec.stack().what()).toStack())
                .orElse(ItemStack.EMPTY);
        loading = true;
        try {
            markerInventory.setItemDirect(0, markerStack);
        } finally {
            loading = false;
        }
        saveChanges();
    }

    public void readFromNBT(CompoundTag data) {
        loading = true;
        try {
            CompoundTag state = data.getCompound(STATE_TAG);
            color = PackageColor.byId(state.getString(COLOR_TAG)).orElse(PackageColor.FLUIX);
            markerInventory.readFromNBT(state, MARKER_TAG);
            inputs.readFromChildTag(state, INPUTS_TAG);
        } finally {
            loading = false;
        }
    }

    public void writeToNBT(CompoundTag data) {
        CompoundTag state = new CompoundTag();
        state.putString(COLOR_TAG, color.id());
        markerInventory.writeToNBT(state, MARKER_TAG);
        inputs.writeToChildTag(state, INPUTS_TAG);
        data.put(STATE_TAG, state);
    }

    @Override
    public void saveChanges() {
        if (!loading) {
            changeListener.run();
        }
    }

    @Override
    public void onChangeInventory(InternalInventory inventory, int slot) {
        saveChanges();
    }

    @Override
    public boolean isClientSide() {
        return clientSide.getAsBoolean();
    }

    private static final class MarkerFilter implements IAEItemFilter {
        @Override
        public boolean allowInsert(InternalInventory inventory, int slot, ItemStack stack) {
            return !stack.isEmpty()
                    && !(stack.getItem() instanceof PackageItem)
                    && !AEItems.BLANK_PATTERN.isSameAs(stack)
                    && !PatternDetailsHelper.isEncodedPattern(stack)
                    && !PackageCraftingPatternDataStorage.hasData(stack);
        }
    }
}
