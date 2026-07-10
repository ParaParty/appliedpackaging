package com.warmthdawn.appliedpackaging.mixin.client;

import appeng.client.gui.me.items.EncodingModePanel;
import appeng.client.gui.me.items.PatternEncodingTermScreen;
import appeng.client.gui.widgets.TabButton;
import appeng.menu.me.items.PatternEncodingTermMenu;
import appeng.parts.encoding.EncodingMode;
import com.warmthdawn.appliedpackaging.client.widget.PackageColorButton;
import com.warmthdawn.appliedpackaging.item.PackageColor;
import com.warmthdawn.appliedpackaging.mixinbridge.PackageCraftingPanelBridge;
import com.warmthdawn.appliedpackaging.mixinbridge.PackageCraftingPatternMenuBridge;
import com.warmthdawn.appliedpackaging.registry.APItems;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = PatternEncodingTermScreen.class, remap = false)
public abstract class PatternEncodingTermScreenMixin<C extends PatternEncodingTermMenu> extends Screen {
    @Shadow
    @Final
    private Map<EncodingMode, EncodingModePanel> modePanels;
    @Shadow
    @Final
    private Map<EncodingMode, TabButton> modeTabButtons;

    @Unique
    private TabButton appliedpackaging$packageTabButton;
    @Unique
    private EditBox appliedpackaging$nameField;
    @Unique
    private final List<PackageColorButton> appliedpackaging$colorButtons = new ArrayList<>();
    @Unique
    private boolean appliedpackaging$updatingNameField;

    protected PatternEncodingTermScreenMixin(Component title) {
        super(title);
    }

    @Inject(method = "<init>", at = @At("TAIL"))
    private void appliedpackaging$createPackageModeWidgets(
            C menu,
            Inventory playerInventory,
            Component title,
            appeng.client.gui.style.ScreenStyle style,
            CallbackInfo ci) {
        this.appliedpackaging$packageTabButton = new TabButton(
                new ItemStack(APItems.PACKAGE_PATTERN.get()),
                Component.translatable("gui.appliedpackaging.package_pattern.mode"),
                button -> ((PackageCraftingPatternMenuBridge) appliedpackaging$menu())
                        .appliedpackaging$setPackageCraftingMode(true));
        this.appliedpackaging$packageTabButton.setStyle(TabButton.Style.HORIZONTAL);

        this.appliedpackaging$nameField = new EditBox(
                Minecraft.getInstance().font,
                0,
                0,
                64,
                12,
                Component.translatable("gui.appliedpackaging.package_pattern.name"));
        this.appliedpackaging$nameField.setMaxLength(50);
        this.appliedpackaging$nameField.setBordered(true);
        this.appliedpackaging$nameField.setResponder(value -> {
            if (!appliedpackaging$updatingNameField) {
                ((PackageCraftingPatternMenuBridge) appliedpackaging$menu()).appliedpackaging$setPackageCraftingName(value);
            }
        });

        for (PackageColor color : PackageColor.values()) {
            this.appliedpackaging$colorButtons.add(new PackageColorButton(color, button ->
                    ((PackageCraftingPatternMenuBridge) appliedpackaging$menu())
                            .appliedpackaging$setPackageCraftingColor(color)));
        }
    }

    @Inject(method = "updateBeforeRender", at = @At("TAIL"))
    private void appliedpackaging$updatePackageModeWidgets(CallbackInfo ci) {
        appliedpackaging$ensurePackageModeWidgetsAdded();
        PackageCraftingPatternMenuBridge bridge = (PackageCraftingPatternMenuBridge) appliedpackaging$menu();
        boolean visible = bridge.appliedpackaging$isPackageCraftingMode();

        appliedpackaging$layoutPackageModeWidgets();
        appliedpackaging$packageTabButton.visible = true;
        appliedpackaging$packageTabButton.setSelected(visible);
        appliedpackaging$nameField.visible = visible;
        appliedpackaging$nameField.active = visible;

        for (PackageColorButton button : appliedpackaging$colorButtons) {
            button.visible = visible;
            button.active = visible;
            button.setSelected(button.color() == bridge.appliedpackaging$getPackageCraftingColor());
        }
        if (visible) {
            for (TabButton button : modeTabButtons.values()) {
                button.setSelected(false);
            }
            modePanels.get(EncodingMode.CRAFTING).setVisible(true);
        }
        if (modePanels.get(EncodingMode.CRAFTING) instanceof PackageCraftingPanelBridge bridgePanel) {
            bridgePanel.appliedpackaging$setPackageModeControlsHidden(visible);
        }

        String menuName = bridge.appliedpackaging$getPackageCraftingName();
        if (!appliedpackaging$nameField.getValue().equals(menuName)) {
            appliedpackaging$updatingNameField = true;
            try {
                appliedpackaging$nameField.setValue(menuName);
            } finally {
                appliedpackaging$updatingNameField = false;
            }
        }
    }

    @Unique
    private void appliedpackaging$ensurePackageModeWidgetsAdded() {
        if (appliedpackaging$packageTabButton == null || children().contains(appliedpackaging$packageTabButton)) {
            return;
        }
        addRenderableWidget(appliedpackaging$packageTabButton);
        addRenderableWidget(appliedpackaging$nameField);
        for (PackageColorButton button : appliedpackaging$colorButtons) {
            addRenderableWidget(button);
        }
        appliedpackaging$layoutPackageModeWidgets();
    }

    @Unique
    private void appliedpackaging$layoutPackageModeWidgets() {
        if (appliedpackaging$packageTabButton == null) {
            return;
        }
        AbstractContainerScreenAccessor screen = (AbstractContainerScreenAccessor) this;
        int leftPos = screen.appliedpackaging$getLeftPos();
        int topPos = screen.appliedpackaging$getTopPos();
        int imageHeight = screen.appliedpackaging$getImageHeight();

        appliedpackaging$packageTabButton.setX(leftPos + 173);
        appliedpackaging$packageTabButton.setY(topPos + imageHeight - 90);
        appliedpackaging$packageTabButton.setWidth(20);
        appliedpackaging$packageTabButton.setHeight(20);

        appliedpackaging$nameField.setX(leftPos + 74);
        appliedpackaging$nameField.setY(topPos + imageHeight - 161);
        appliedpackaging$nameField.setWidth(65);
        appliedpackaging$nameField.setHeight(12);

        int startX = leftPos + 74;
        int startY = topPos + imageHeight - 121;
        for (int index = 0; index < appliedpackaging$colorButtons.size(); index++) {
            PackageColorButton button = appliedpackaging$colorButtons.get(index);
            button.setX(startX + (index % 9) * 8);
            button.setY(startY + (index / 9) * 8);
            button.setWidth(8);
            button.setHeight(8);
        }

        var markerSlot = ((PackageCraftingPatternMenuBridge) appliedpackaging$menu())
                .appliedpackaging$getPackageCraftingMarkerSlot();
        if (markerSlot != null) {
            ((SlotAccessor) markerSlot).appliedpackaging$setX(84);
            ((SlotAccessor) markerSlot).appliedpackaging$setY(imageHeight - 138);
        }
    }

    @Unique
    @SuppressWarnings("unchecked")
    private C appliedpackaging$menu() {
        return ((PatternEncodingTermScreen<C>) (Object) this).getMenu();
    }
}
