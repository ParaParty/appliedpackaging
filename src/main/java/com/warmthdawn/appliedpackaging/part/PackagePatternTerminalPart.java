package com.warmthdawn.appliedpackaging.part;

import appeng.api.parts.IPartItem;
import appeng.api.parts.IPartModel;
import appeng.api.parts.PartModels;
import appeng.api.stacks.GenericStack;
import appeng.parts.PartModel;
import appeng.parts.reporting.AbstractDisplayPart;
import com.warmthdawn.appliedpackaging.AppliedPackaging;
import com.warmthdawn.appliedpackaging.item.PackageColor;
import com.warmthdawn.appliedpackaging.registry.APBlocks;
import com.warmthdawn.appliedpackaging.world.block.entity.terminal.PackagePatternTerminalBlockEntity;
import com.warmthdawn.appliedpackaging.world.block.entity.terminal.PackagePatternTerminalHost;
import com.warmthdawn.appliedpackaging.world.menu.PackagePatternTerminalMenu;
import java.util.List;
import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.ItemStackHandler;
import net.minecraftforge.network.NetworkHooks;

public class PackagePatternTerminalPart extends AbstractDisplayPart implements MenuProvider, PackagePatternTerminalHost {
    private static final String TERMINAL_TAG = "terminal";

    public static final ResourceLocation MODEL_OFF =
            new ResourceLocation(AppliedPackaging.MOD_ID, "part/package_pattern_terminal_off");
    public static final ResourceLocation MODEL_ON =
            new ResourceLocation(AppliedPackaging.MOD_ID, "part/package_pattern_terminal_on");

    public static final IPartModel MODELS_OFF = new PartModel(MODEL_BASE, MODEL_OFF, MODEL_STATUS_OFF);
    public static final IPartModel MODELS_ON = new PartModel(MODEL_BASE, MODEL_ON, MODEL_STATUS_ON);
    public static final IPartModel MODELS_HAS_CHANNEL =
            new PartModel(MODEL_BASE, MODEL_ON, MODEL_STATUS_HAS_CHANNEL);

    private final PackagePatternTerminalBlockEntity terminal =
            new PackagePatternTerminalBlockEntity(
                    BlockPos.ZERO,
                    APBlocks.PACKAGE_PATTERN_TERMINAL.get().defaultBlockState()) {
                @Override
                public void setChanged() {
                    PackagePatternTerminalPart.this.markTerminalChanged();
                }
            };
    private final LazyOptional<IItemHandler> itemHandler = LazyOptional.of(terminal::getItems);
    private boolean loading;

    public PackagePatternTerminalPart(IPartItem<?> partItem) {
        super(partItem, true);
    }

    public static void registerModels() {
        PartModels.registerModels(MODEL_OFF, MODEL_ON);
    }

    @Override
    public boolean onPartActivate(Player player, InteractionHand hand, Vec3 pos) {
        if (super.onPartActivate(player, hand, pos)) {
            return true;
        }
        if (!player.level().isClientSide && player instanceof ServerPlayer serverPlayer) {
            NetworkHooks.openScreen(serverPlayer, this,
                    buffer -> PackagePatternTerminalMenu.writePartHost(buffer, getTerminalPos(), getSide()));
        }
        return true;
    }

    @Override
    public void readFromNBT(CompoundTag data) {
        super.readFromNBT(data);
        if (data.contains(TERMINAL_TAG, net.minecraft.nbt.Tag.TAG_COMPOUND)) {
            loading = true;
            try {
                terminal.load(data.getCompound(TERMINAL_TAG));
            } finally {
                loading = false;
            }
        }
    }

    @Override
    public void writeToNBT(CompoundTag data) {
        super.writeToNBT(data);
        data.put(TERMINAL_TAG, terminal.saveWithoutMetadata());
    }

    @Override
    public void addAdditionalDrops(List<ItemStack> drops, boolean wrenched) {
        super.addAdditionalDrops(drops, wrenched);
        terminal.addContentDrops(drops);
    }

    @Override
    public void clearContent() {
        super.clearContent();
        terminal.clearTerminalContent();
    }

    @Override
    public IPartModel getStaticModels() {
        return selectModel(MODELS_OFF, MODELS_ON, MODELS_HAS_CHANNEL);
    }

    @Override
    public ItemStackHandler getItems() {
        return terminal.getItems();
    }

    @Override
    public Level getTerminalLevel() {
        return getLevel();
    }

    @Override
    public BlockPos getTerminalPos() {
        return getBlockEntity().getBlockPos();
    }

    @Override
    public boolean isTerminalMenuValid(Player player) {
        if (getHost() == null || !getHost().isInWorld() || getBlockEntity() == null || getSide() == null) {
            return false;
        }
        if (player.level() != getLevel() || getHost().getPart(getSide()) != this) {
            return false;
        }
        BlockPos pos = getTerminalPos();
        return player.distanceToSqr(
                pos.getX() + 0.5D,
                pos.getY() + 0.5D,
                pos.getZ() + 0.5D) <= 64.0D;
    }

    @Override
    public PackageColor selectedColor() {
        return terminal.selectedColor();
    }

    @Override
    public void setSelectedColor(PackageColor selectedColor) {
        terminal.setSelectedColor(selectedColor);
    }

    @Override
    public Optional<PackageColor> inputSlotColor(int slot) {
        return terminal.inputSlotColor(slot);
    }

    @Override
    public int inputSlotColorOrdinal(int slot) {
        return terminal.inputSlotColorOrdinal(slot);
    }

    @Override
    public void setInputSlotColor(int slot, PackageColor color) {
        terminal.setInputSlotColor(slot, color);
    }

    @Override
    public void setInputSlotColorOrdinal(int slot, int color) {
        terminal.setInputSlotColorOrdinal(slot, color);
    }

    @Override
    public void clearInputSlotColor(int slot) {
        terminal.clearInputSlotColor(slot);
    }

    @Override
    public ItemStack processingOutput(int slot) {
        return terminal.processingOutput(slot);
    }

    @Override
    public void setProcessingOutputFromGhostStack(int slot, ItemStack stack, boolean singleContainerOrItem) {
        terminal.setProcessingOutputFromGhostStack(slot, stack, singleContainerOrItem);
    }

    @Override
    public GenericStack processingOutputKey(int slot) {
        return terminal.processingOutputKey(slot);
    }

    @Override
    public int processingOutputAmountForDisplay(int slot) {
        return terminal.processingOutputAmountForDisplay(slot);
    }

    @Override
    public void adjustProcessingOutputAmount(int slot, boolean increase) {
        terminal.adjustProcessingOutputAmount(slot, increase);
    }

    @Override
    public void clearProcessingOutput(int slot) {
        terminal.clearProcessingOutput(slot);
    }

    @Override
    public PackagePatternTerminalBlockEntity.EncodeResult encodeOnce() {
        return terminal.encodeOnce();
    }

    @Override
    public PackagePatternTerminalBlockEntity.SplitResult splitOnce() {
        return terminal.splitOnce();
    }

    @Override
    public Component getDisplayName() {
        return Component.translatable("block.appliedpackaging.package_pattern_terminal");
    }

    @Override
    public AbstractContainerMenu createMenu(int containerId, Inventory playerInventory, Player player) {
        return new PackagePatternTerminalMenu(containerId, playerInventory, this);
    }

    @Override
    public <T> LazyOptional<T> getCapability(Capability<T> cap) {
        if (cap == ForgeCapabilities.ITEM_HANDLER) {
            return itemHandler.cast();
        }
        return super.getCapability(cap);
    }

    private void markTerminalChanged() {
        if (!loading && getHost() != null) {
            getHost().markForSave();
        }
    }
}
