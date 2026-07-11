package com.warmthdawn.appliedpackaging.part;

import appeng.api.parts.IPartItem;
import appeng.parts.encoding.PatternEncodingTerminalPart;
import com.warmthdawn.appliedpackaging.registry.APMenus;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.MenuType;

public class AdvancedPatternEncodingTerminalPart extends PatternEncodingTerminalPart
        implements AdvancedPatternEncodingTerminalHost {
    private final AdvancedPatternEncodingState advancedPatternState =
            new AdvancedPatternEncodingState(this::markForSave);

    public AdvancedPatternEncodingTerminalPart(IPartItem<?> partItem) {
        super(partItem);
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
    public void readFromNBT(CompoundTag data) {
        super.readFromNBT(data);
        advancedPatternState.readFromNBT(data);
        advancedPatternState.migrateLegacyInputs(getLogic().getEncodedInputInv());
    }

    @Override
    public void writeToNBT(CompoundTag data) {
        super.writeToNBT(data);
        advancedPatternState.writeToNBT(data);
    }
}
