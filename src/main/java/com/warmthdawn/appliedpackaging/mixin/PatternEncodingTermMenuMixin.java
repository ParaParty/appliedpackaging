package com.warmthdawn.appliedpackaging.mixin;

import appeng.menu.AEBaseMenu;
import appeng.api.inventories.InternalInventory;
import appeng.api.stacks.AEItemKey;
import appeng.api.stacks.GenericStack;
import appeng.menu.SlotSemantics;
import appeng.menu.guisync.GuiSync;
import appeng.menu.me.items.PatternEncodingTermMenu;
import appeng.menu.slot.FakeSlot;
import appeng.menu.slot.PatternTermSlot;
import appeng.parts.encoding.EncodingMode;
import appeng.parts.encoding.PatternEncodingLogic;
import appeng.util.ConfigInventory;
import com.warmthdawn.appliedpackaging.core.package_data.MarkerSpec;
import com.warmthdawn.appliedpackaging.core.package_data.PackageCraftingPatternDataStorage;
import com.warmthdawn.appliedpackaging.item.PackageColor;
import com.warmthdawn.appliedpackaging.item.PackageItem;
import com.warmthdawn.appliedpackaging.mixinbridge.PackageCraftingPatternLogicBridge;
import com.warmthdawn.appliedpackaging.mixinbridge.PackageCraftingPatternMenuBridge;
import com.warmthdawn.appliedpackaging.world.menu.AdvancedPatternEncodingTermMenu;
import java.util.Optional;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value = PatternEncodingTermMenu.class, remap = false)
public abstract class PatternEncodingTermMenuMixin implements PackageCraftingPatternMenuBridge {
    @Unique
    private static final String AP_ACTION_SET_PACKAGE_MODE = "apSetPackageMode";
    @Unique
    private static final String AP_ACTION_SET_PACKAGE_COLOR = "apSetPackageColor";
    @Unique
    private static final String AP_ACTION_SET_PACKAGE_NAME = "apSetPackageName";

    @Shadow
    @Final
    private PatternEncodingLogic encodingLogic;
    @Shadow
    @Final
    private ConfigInventory encodedInputsInv;
    @Shadow
    @Final
    private PatternTermSlot craftOutputSlot;
    @Shadow
    public EncodingMode mode;

    @GuiSync(89)
    @Unique
    public boolean appliedpackaging$packageCraftingMode;
    @GuiSync(88)
    @Unique
    public int appliedpackaging$packageCraftingColor = PackageColor.FLUIX.ordinal();
    @GuiSync(87)
    @Unique
    public String appliedpackaging$packageCraftingName = "";

    @Unique
    private FakeSlot appliedpackaging$markerSlot;

    @Shadow
    public abstract void setMode(EncodingMode mode);

    @Shadow
    private ItemStack getAndUpdateOutput() {
        throw new AssertionError();
    }

    @Inject(method = "<init>(Lnet/minecraft/world/inventory/MenuType;ILnet/minecraft/world/entity/player/Inventory;Lappeng/helpers/IPatternTerminalMenuHost;Z)V",
            at = @At("TAIL"))
    private void appliedpackaging$initPackageCraftingControls(
            MenuType<?> menuType,
            int id,
            Inventory ip,
            appeng.helpers.IPatternTerminalMenuHost host,
            boolean bindInventory,
            CallbackInfo ci) {
        PackageCraftingPatternLogicBridge bridge = (PackageCraftingPatternLogicBridge) encodingLogic;
        FakeSlot markerSlot = new FakeSlot(bridge.appliedpackaging$getPackageCraftingMarkerInv(), 0);
        markerSlot.setHideAmount(true);
        markerSlot.setActive(false);
        this.appliedpackaging$markerSlot = markerSlot;
        ((AEBaseMenuAccessor) this).appliedpackaging$addSlot(markerSlot, SlotSemantics.CONFIG);

        ((AEBaseMenuAccessor) this).appliedpackaging$registerClientAction(
                AP_ACTION_SET_PACKAGE_MODE,
                Boolean.class,
                this::appliedpackaging$applyPackageMode);
        ((AEBaseMenuAccessor) this).appliedpackaging$registerClientAction(
                AP_ACTION_SET_PACKAGE_COLOR,
                String.class,
                this::appliedpackaging$applyPackageColor);
        ((AEBaseMenuAccessor) this).appliedpackaging$registerClientAction(
                AP_ACTION_SET_PACKAGE_NAME,
                String.class,
                this::appliedpackaging$applyPackageName);
    }

    @Inject(method = "broadcastChanges", at = @At("TAIL"))
    private void appliedpackaging$syncPackageCraftingFields(CallbackInfo ci) {
        if (((AEBaseMenu) (Object) this).isClientSide()) {
            return;
        }
        PackageCraftingPatternLogicBridge bridge = (PackageCraftingPatternLogicBridge) encodingLogic;
        this.appliedpackaging$packageCraftingMode = bridge.appliedpackaging$isPackageCraftingMode();
        this.appliedpackaging$packageCraftingColor = bridge.appliedpackaging$getPackageCraftingColor().ordinal();
        this.appliedpackaging$packageCraftingName = bridge.appliedpackaging$getPackageCraftingName();
        if (this.appliedpackaging$packageCraftingMode && this.mode != EncodingMode.CRAFTING) {
            this.mode = EncodingMode.CRAFTING;
        }
    }

    @Inject(method = "onServerDataSync", at = @At("TAIL"))
    private void appliedpackaging$updatePackageCraftingSlotVisibility(CallbackInfo ci) {
        if (appliedpackaging$markerSlot != null) {
            appliedpackaging$markerSlot.setActive(appliedpackaging$packageCraftingMode);
        }
        if (appliedpackaging$packageCraftingMode) {
            this.craftOutputSlot.setActive(true);
            this.getAndUpdateOutput();
        }
    }

    @Inject(method = "setMode", at = @At("HEAD"))
    private void appliedpackaging$leavePackageCraftingMode(EncodingMode mode, CallbackInfo ci) {
        if (this.appliedpackaging$packageCraftingMode) {
            appliedpackaging$setPackageCraftingMode(false);
        }
    }

    @Inject(method = "getAndUpdateOutput", at = @At("HEAD"), cancellable = true)
    private void appliedpackaging$getAndUpdatePackageOutput(CallbackInfoReturnable<ItemStack> cir) {
        if (!appliedpackaging$packageCraftingMode) {
            return;
        }
        ItemStack output = appliedpackaging$createPackageCraftingPreview()
                .map(PackageCraftingPatternDataStorage::toPackageStack)
                .orElse(ItemStack.EMPTY);
        this.craftOutputSlot.setDisplayedCraftingOutput(output);
        cir.setReturnValue(output);
    }

    @Inject(method = "encodePattern", at = @At("HEAD"), cancellable = true)
    private void appliedpackaging$encodePackageCraftingPattern(CallbackInfoReturnable<ItemStack> cir) {
        if ((Object) this instanceof AdvancedPatternEncodingTermMenu advancedMenu) {
            cir.setReturnValue(advancedMenu.encodeAdvancedPattern());
            return;
        }
        if (!appliedpackaging$packageCraftingMode) {
            return;
        }
        ItemStack pattern = appliedpackaging$createPackageCraftingPreview()
                .map(PackageCraftingPatternDataStorage::encode)
                .orElse(null);
        cir.setReturnValue(pattern);
    }

    @Override
    public boolean appliedpackaging$isPackageCraftingMode() {
        return appliedpackaging$packageCraftingMode;
    }

    @Override
    public void appliedpackaging$setPackageCraftingMode(boolean packageMode) {
        if (((AEBaseMenu) (Object) this).isClientSide()) {
            ((AEBaseMenuAccessor) this).appliedpackaging$sendClientAction(AP_ACTION_SET_PACKAGE_MODE, packageMode);
        }
        appliedpackaging$applyPackageMode(packageMode);
    }

    @Override
    public PackageColor appliedpackaging$getPackageCraftingColor() {
        PackageColor[] values = PackageColor.values();
        if (appliedpackaging$packageCraftingColor < 0 || appliedpackaging$packageCraftingColor >= values.length) {
            return PackageColor.FLUIX;
        }
        return values[appliedpackaging$packageCraftingColor];
    }

    @Override
    public void appliedpackaging$setPackageCraftingColor(PackageColor color) {
        PackageColor value = color == null ? PackageColor.FLUIX : color;
        if (((AEBaseMenu) (Object) this).isClientSide()) {
            ((AEBaseMenuAccessor) this).appliedpackaging$sendClientAction(AP_ACTION_SET_PACKAGE_COLOR, value.id());
        }
        appliedpackaging$applyPackageColor(value.id());
    }

    @Override
    public String appliedpackaging$getPackageCraftingName() {
        return appliedpackaging$packageCraftingName == null ? "" : appliedpackaging$packageCraftingName;
    }

    @Override
    public void appliedpackaging$setPackageCraftingName(String name) {
        String value = PackageCraftingPatternDataStorage.sanitizePackageName(name);
        if (((AEBaseMenu) (Object) this).isClientSide()) {
            ((AEBaseMenuAccessor) this).appliedpackaging$sendClientAction(AP_ACTION_SET_PACKAGE_NAME, value);
        }
        appliedpackaging$applyPackageName(value);
    }

    @Override
    public FakeSlot appliedpackaging$getPackageCraftingMarkerSlot() {
        return appliedpackaging$markerSlot;
    }

    @Unique
    private void appliedpackaging$applyPackageMode(Boolean packageMode) {
        boolean value = Boolean.TRUE.equals(packageMode);
        this.appliedpackaging$packageCraftingMode = value;
        PackageCraftingPatternLogicBridge bridge = (PackageCraftingPatternLogicBridge) encodingLogic;
        bridge.appliedpackaging$setPackageCraftingMode(value);
        if (value) {
            encodingLogic.setMode(EncodingMode.CRAFTING);
            this.mode = EncodingMode.CRAFTING;
        }
        if (appliedpackaging$markerSlot != null) {
            appliedpackaging$markerSlot.setActive(value);
        }
        getAndUpdateOutput();
    }

    @Unique
    private void appliedpackaging$applyPackageColor(String colorId) {
        PackageColor color = PackageColor.byId(colorId).orElse(PackageColor.FLUIX);
        this.appliedpackaging$packageCraftingColor = color.ordinal();
        ((PackageCraftingPatternLogicBridge) encodingLogic).appliedpackaging$setPackageCraftingColor(color);
        getAndUpdateOutput();
    }

    @Unique
    private void appliedpackaging$applyPackageName(String name) {
        String value = PackageCraftingPatternDataStorage.sanitizePackageName(name);
        this.appliedpackaging$packageCraftingName = value;
        ((PackageCraftingPatternLogicBridge) encodingLogic).appliedpackaging$setPackageCraftingName(value);
        getAndUpdateOutput();
    }

    @Unique
    private Optional<PackageCraftingPatternDataStorage.EncodedPackageCraftingPattern> appliedpackaging$createPackageCraftingPreview() {
        GenericStack[] sparseInputs = new GenericStack[PackageCraftingPatternDataStorage.INPUT_SLOT_COUNT];
        for (int slot = 0; slot < sparseInputs.length; slot++) {
            GenericStack stack = encodedInputsInv.getStack(slot);
            if (stack == null) {
                continue;
            }
            if (!AEItemKey.is(stack.what()) || stack.amount() <= 0) {
                return Optional.empty();
            }
            sparseInputs[slot] = stack;
        }
        return PackageCraftingPatternDataStorage.create(
                appliedpackaging$getPackageCraftingColor(),
                sparseInputs,
                appliedpackaging$markerSpec(),
                appliedpackaging$getPackageCraftingName());
    }

    @Unique
    private Optional<MarkerSpec> appliedpackaging$markerSpec() {
        InternalInventory markerInv =
                ((PackageCraftingPatternLogicBridge) encodingLogic).appliedpackaging$getPackageCraftingMarkerInv();
        ItemStack marker = markerInv.getStackInSlot(0);
        if (marker.isEmpty() || marker.getItem() instanceof PackageItem || !AEItemKey.is(AEItemKey.of(marker))) {
            return Optional.empty();
        }
        ItemStack keyStack = marker.copy();
        keyStack.setCount(1);
        return Optional.of(new MarkerSpec(new GenericStack(AEItemKey.of(keyStack), 1)));
    }
}
