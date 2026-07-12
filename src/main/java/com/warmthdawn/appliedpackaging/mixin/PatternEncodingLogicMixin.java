package com.warmthdawn.appliedpackaging.mixin;

import appeng.api.crafting.PatternDetailsHelper;
import appeng.api.inventories.InternalInventory;
import appeng.api.stacks.AEItemKey;
import appeng.api.stacks.GenericStack;
import appeng.core.definitions.AEItems;
import appeng.parts.encoding.EncodingMode;
import appeng.parts.encoding.PatternEncodingLogic;
import appeng.util.ConfigInventory;
import appeng.util.inv.AppEngInternalInventory;
import appeng.util.inv.InternalInventoryHost;
import appeng.util.inv.filter.IAEItemFilter;
import com.warmthdawn.appliedpackaging.core.package_data.MarkerSpec;
import com.warmthdawn.appliedpackaging.core.package_data.AdvancedProcessingPatternDataStorage;
import com.warmthdawn.appliedpackaging.core.package_data.PackageCraftingPatternDataStorage;
import com.warmthdawn.appliedpackaging.item.PackageColor;
import com.warmthdawn.appliedpackaging.item.PackageItem;
import com.warmthdawn.appliedpackaging.mixinbridge.PackageCraftingPatternLogicBridge;
import com.warmthdawn.appliedpackaging.part.AdvancedPatternEncodingTerminalHost;
import appeng.helpers.IPatternTerminalLogicHost;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = PatternEncodingLogic.class, remap = false)
public abstract class PatternEncodingLogicMixin implements PackageCraftingPatternLogicBridge {
    @Shadow
    @Final
    private IPatternTerminalLogicHost host;

    @Shadow
    @Final
    private ConfigInventory encodedInputInv;
    @Shadow
    @Final
    private ConfigInventory encodedOutputInv;

    @Shadow
    public abstract void setMode(EncodingMode mode);

    @Shadow
    public abstract void saveChanges();

    @Unique
    private boolean appliedpackaging$packageCraftingMode;
    @Unique
    private PackageColor appliedpackaging$packageCraftingColor = PackageColor.FLUIX;
    @Unique
    private final AppEngInternalInventory appliedpackaging$markerInv =
            new AppEngInternalInventory((InternalInventoryHost) (Object) this, 1, 1);

    @Inject(method = "<init>", at = @At("TAIL"))
    private void appliedpackaging$initMarkerInventory(CallbackInfo ci) {
        appliedpackaging$markerInv.setFilter(new MarkerFilter());
    }

    @Inject(method = "loadEncodedPattern", at = @At("HEAD"), cancellable = true)
    private void appliedpackaging$loadPackageCraftingPattern(ItemStack pattern, CallbackInfo ci) {
        if (pattern.isEmpty()) {
            return;
        }
        if (host instanceof AdvancedPatternEncodingTerminalHost advancedHost) {
            advancedHost.getAdvancedPatternState().loadFromPattern(pattern);
            if (AdvancedProcessingPatternDataStorage.hasData(pattern)) {
                setMode(EncodingMode.PROCESSING);
                encodedInputInv.clear();
                var outputs = AdvancedProcessingPatternDataStorage.readSparseOutputs(pattern);
                encodedOutputInv.beginBatch();
                try {
                    encodedOutputInv.clear();
                    for (int slot = 0; slot < Math.min(outputs.size(), encodedOutputInv.size()); slot++) {
                        encodedOutputInv.setStack(slot, outputs.get(slot));
                    }
                } finally {
                    encodedOutputInv.endBatch();
                }
                saveChanges();
                ci.cancel();
                return;
            }
        }
        var encoded = PackageCraftingPatternDataStorage.read(pattern);
        if (encoded.isEmpty()) {
            appliedpackaging$setPackageCraftingMode(false);
            return;
        }
        setMode(EncodingMode.CRAFTING);
        appliedpackaging$setPackageCraftingMode(true);
        appliedpackaging$setPackageCraftingColor(encoded.get().color());
        appliedpackaging$loadSparseInputs(encoded.get().sparseInputs());
        appliedpackaging$loadMarker(encoded.get().data().marker());
        saveChanges();
        ci.cancel();
    }

    @Inject(method = "readFromNBT", at = @At("TAIL"))
    private void appliedpackaging$readPackageCraftingState(CompoundTag data, CallbackInfo ci) {
        CompoundTag tag = data.getCompound("appliedpackagingPackageCrafting");
        appliedpackaging$packageCraftingMode = tag.getBoolean("mode");
        appliedpackaging$packageCraftingColor = PackageColor.byId(tag.getString("color")).orElse(PackageColor.FLUIX);
        appliedpackaging$markerInv.readFromNBT(tag, "marker");
    }

    @Inject(method = "writeToNBT", at = @At("TAIL"))
    private void appliedpackaging$writePackageCraftingState(CompoundTag data, CallbackInfo ci) {
        CompoundTag tag = new CompoundTag();
        tag.putBoolean("mode", appliedpackaging$packageCraftingMode);
        tag.putString("color", appliedpackaging$packageCraftingColor.id());
        appliedpackaging$markerInv.writeToNBT(tag, "marker");
        data.put("appliedpackagingPackageCrafting", tag);
    }

    @Inject(method = "fixCraftingRecipes", at = @At("HEAD"), cancellable = true)
    private void appliedpackaging$allowPackageCraftingAmounts(CallbackInfo ci) {
        if (appliedpackaging$packageCraftingMode) {
            ci.cancel();
        }
    }

    @Override
    public boolean appliedpackaging$isPackageCraftingMode() {
        return appliedpackaging$packageCraftingMode;
    }

    @Override
    public void appliedpackaging$setPackageCraftingMode(boolean packageMode) {
        if (this.appliedpackaging$packageCraftingMode != packageMode) {
            this.appliedpackaging$packageCraftingMode = packageMode;
            saveChanges();
        }
    }

    @Override
    public PackageColor appliedpackaging$getPackageCraftingColor() {
        return appliedpackaging$packageCraftingColor;
    }

    @Override
    public void appliedpackaging$setPackageCraftingColor(PackageColor color) {
        PackageColor value = color == null ? PackageColor.FLUIX : color;
        if (this.appliedpackaging$packageCraftingColor != value) {
            this.appliedpackaging$packageCraftingColor = value;
            saveChanges();
        }
    }

    @Override
    public InternalInventory appliedpackaging$getPackageCraftingMarkerInv() {
        return appliedpackaging$markerInv;
    }

    @Unique
    private void appliedpackaging$loadSparseInputs(GenericStack[] inputs) {
        encodedInputInv.beginBatch();
        try {
            encodedInputInv.clear();
            for (int slot = 0; slot < Math.min(inputs.length, PackageCraftingPatternDataStorage.INPUT_SLOT_COUNT); slot++) {
                encodedInputInv.setStack(slot, inputs[slot]);
            }
        } finally {
            encodedInputInv.endBatch();
        }
    }

    @Unique
    private void appliedpackaging$loadMarker(java.util.Optional<MarkerSpec> marker) {
        if (marker.isPresent() && marker.get().stack().what() instanceof AEItemKey itemKey) {
            appliedpackaging$markerInv.setItemDirect(0, itemKey.toStack());
        } else {
            appliedpackaging$markerInv.setItemDirect(0, ItemStack.EMPTY);
        }
    }

    private static final class MarkerFilter implements IAEItemFilter {
        @Override
        public boolean allowInsert(InternalInventory inv, int slot, ItemStack stack) {
            return !stack.isEmpty()
                    && !(stack.getItem() instanceof PackageItem)
                    && !AEItems.BLANK_PATTERN.isSameAs(stack)
                    && !PatternDetailsHelper.isEncodedPattern(stack)
                    && !PackageCraftingPatternDataStorage.hasData(stack);
        }
    }
}
