package com.warmthdawn.appliedpackaging.part;

import appeng.api.parts.IPartItem;
import appeng.api.parts.IPartModel;
import appeng.api.parts.PartModels;
import appeng.parts.PartModel;
import appeng.parts.encoding.PatternEncodingTerminalPart;
import com.warmthdawn.appliedpackaging.AppliedPackaging;
import com.warmthdawn.appliedpackaging.registry.APMenus;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.MenuType;

public class AdvancedPatternEncodingTerminalPart extends PatternEncodingTerminalPart
        implements AdvancedPatternEncodingTerminalHost {
    private static final String SPECIALIZED_MODE_TAG = "appliedpackagingSpecializedPatternMode";
    private static final IPartModel MODELS_OFF = new PartModel(
            AppliedPackaging.id("part/advanced_pattern_encoding_terminal_base"),
            AppliedPackaging.id("part/advanced_pattern_encoding_terminal_off"),
            AppliedPackaging.id("part/advanced_pattern_encoding_terminal_status_off"));
    private static final IPartModel MODELS_ON = new PartModel(
            AppliedPackaging.id("part/advanced_pattern_encoding_terminal_base"),
            AppliedPackaging.id("part/advanced_pattern_encoding_terminal_on"),
            AppliedPackaging.id("part/advanced_pattern_encoding_terminal_status_on"));
    private static final IPartModel MODELS_HAS_CHANNEL = new PartModel(
            AppliedPackaging.id("part/advanced_pattern_encoding_terminal_base"),
            AppliedPackaging.id("part/advanced_pattern_encoding_terminal_on"),
            AppliedPackaging.id("part/advanced_pattern_encoding_terminal_status_has_channel"));

    private final AdvancedPatternEncodingState advancedPatternState =
            new AdvancedPatternEncodingState(this::markForSave);
    private final PackagePatternEncodingState packagePatternState = new PackagePatternEncodingState(
            this::markForSave,
            () -> getLevel() != null && getLevel().isClientSide());
    private SpecializedPatternMode specializedPatternMode = SpecializedPatternMode.ADVANCED;

    public AdvancedPatternEncodingTerminalPart(IPartItem<?> partItem) {
        super(partItem);
    }

    public static void registerModels() {
        PartModels.registerModels(
                AppliedPackaging.id("part/advanced_pattern_encoding_terminal_base"),
                AppliedPackaging.id("part/advanced_pattern_encoding_terminal_off"),
                AppliedPackaging.id("part/advanced_pattern_encoding_terminal_on"),
                AppliedPackaging.id("part/advanced_pattern_encoding_terminal_status_off"),
                AppliedPackaging.id("part/advanced_pattern_encoding_terminal_status_on"),
                AppliedPackaging.id("part/advanced_pattern_encoding_terminal_status_has_channel"));
    }

    @Override
    public IPartModel getStaticModels() {
        return selectModel(MODELS_OFF, MODELS_ON, MODELS_HAS_CHANNEL);
    }

    @Override
    public MenuType<?> getMenuType(Player player) {
        return APMenus.ADVANCED_PATTERN_ENCODING_TERMINAL.get();
    }

    @Override
    public AdvancedPatternEncodingState getAdvancedPatternState() {
        return advancedPatternState;
    }

    @Override
    public PackagePatternEncodingState getPackagePatternState() {
        return packagePatternState;
    }

    @Override
    public SpecializedPatternMode getSpecializedPatternMode() {
        return specializedPatternMode;
    }

    @Override
    public void setSpecializedPatternMode(SpecializedPatternMode mode) {
        SpecializedPatternMode value = mode == null ? SpecializedPatternMode.ADVANCED : mode;
        if (specializedPatternMode != value) {
            specializedPatternMode = value;
            markForSave();
        }
    }

    @Override
    public void readFromNBT(CompoundTag data) {
        super.readFromNBT(data);
        advancedPatternState.readFromNBT(data);
        packagePatternState.readFromNBT(data);
        try {
            specializedPatternMode = SpecializedPatternMode.valueOf(data.getString(SPECIALIZED_MODE_TAG));
        } catch (IllegalArgumentException ignored) {
            specializedPatternMode = SpecializedPatternMode.ADVANCED;
        }
    }

    @Override
    public void writeToNBT(CompoundTag data) {
        super.writeToNBT(data);
        advancedPatternState.writeToNBT(data);
        packagePatternState.writeToNBT(data);
        data.putString(SPECIALIZED_MODE_TAG, specializedPatternMode.name());
    }

    @Override
    public void addAdditionalDrops(java.util.List<net.minecraft.world.item.ItemStack> drops, boolean wrenched) {
        super.addAdditionalDrops(drops, wrenched);
        for (var stack : packagePatternState.markerInventory()) {
            drops.add(stack);
        }
    }

    @Override
    public void clearContent() {
        super.clearContent();
        packagePatternState.markerInventory().clear();
    }
}
