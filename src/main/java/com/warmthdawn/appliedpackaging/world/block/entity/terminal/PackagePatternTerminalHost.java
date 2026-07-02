package com.warmthdawn.appliedpackaging.world.block.entity.terminal;

import appeng.api.stacks.GenericStack;
import com.warmthdawn.appliedpackaging.item.PackageColor;
import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraftforge.items.ItemStackHandler;

public interface PackagePatternTerminalHost {
    ItemStackHandler getItems();

    Level getTerminalLevel();

    BlockPos getTerminalPos();

    boolean isTerminalMenuValid(Player player);

    PackageColor selectedColor();

    void setSelectedColor(PackageColor selectedColor);

    Optional<PackageColor> inputSlotColor(int slot);

    int inputSlotColorOrdinal(int slot);

    void setInputSlotColor(int slot, PackageColor color);

    void setInputSlotColorOrdinal(int slot, int color);

    void clearInputSlotColor(int slot);

    ItemStack processingOutput(int slot);

    void setProcessingOutputFromGhostStack(int slot, ItemStack stack, boolean singleContainerOrItem);

    GenericStack processingOutputKey(int slot);

    int processingOutputAmountForDisplay(int slot);

    void adjustProcessingOutputAmount(int slot, boolean increase);

    void clearProcessingOutput(int slot);

    PackagePatternTerminalBlockEntity.EncodeResult encodeOnce();

    PackagePatternTerminalBlockEntity.SplitResult splitOnce();
}
