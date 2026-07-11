package com.warmthdawn.appliedpackaging.client.screen;

import appeng.api.stacks.GenericStack;
import appeng.client.gui.AESubScreen;
import appeng.client.gui.NumberEntryType;
import appeng.client.gui.me.common.ClientDisplaySlot;
import appeng.client.gui.widgets.NumberEntryWidget;
import appeng.client.gui.widgets.TabButton;
import appeng.core.localization.GuiText;
import appeng.menu.SlotSemantics;
import com.google.common.primitives.Longs;
import com.warmthdawn.appliedpackaging.world.menu.AdvancedPatternEncodingTermMenu;
import java.util.function.Consumer;

public final class AdvancedSetPatternAmountScreen
        extends AESubScreen<AdvancedPatternEncodingTermMenu, AdvancedPatternEncodingTermScreen> {
    private final NumberEntryWidget amount;
    private final GenericStack currentStack;
    private final Consumer<GenericStack> setter;

    public AdvancedSetPatternAmountScreen(
            AdvancedPatternEncodingTermScreen parentScreen,
            GenericStack currentStack,
            Consumer<GenericStack> setter) {
        super(parentScreen, "/screens/set_processing_pattern_amount.json");
        this.currentStack = currentStack;
        this.setter = setter;

        widgets.addButton("save", GuiText.Set.text(), this::confirm);

        var icon = getMenu().getHost().getMainMenuIcon();
        var backButton = new TabButton(icon, icon.getHoverName(), button -> returnToParent());
        widgets.add("back", backButton);

        amount = widgets.addNumberEntryWidget("amountToStock", NumberEntryType.of(currentStack.what()));
        amount.setLongValue(currentStack.amount());
        amount.setMaxValue(maxAmount());
        amount.setTextFieldStyle(style.getWidget("amountToStockInput"));
        amount.setMinValue(0);
        amount.setHideValidationIcon(true);
        amount.setOnConfirm(this::confirm);

        addClientSideSlot(new ClientDisplaySlot(currentStack), SlotSemantics.MACHINE_OUTPUT);
    }

    @Override
    protected void init() {
        super.init();
        setSlotsHidden(SlotSemantics.TOOLBOX, true);
    }

    private void confirm() {
        amount.getLongValue().ifPresent(newAmount -> {
            long constrained = Longs.constrainToRange(newAmount, 0, maxAmount());
            setter.accept(constrained <= 0 ? null : new GenericStack(currentStack.what(), constrained));
            returnToParent();
        });
    }

    private long maxAmount() {
        return 999999L * currentStack.what().getAmountPerUnit();
    }
}
