package com.warmthdawn.appliedpackaging.part;

import appeng.helpers.IPatternTerminalMenuHost;

public interface AdvancedPatternEncodingTerminalHost extends IPatternTerminalMenuHost {
    AdvancedPatternEncodingState getAdvancedPatternState();

    PackagePatternEncodingState getPackagePatternState();

    SpecializedPatternMode getSpecializedPatternMode();

    void setSpecializedPatternMode(SpecializedPatternMode mode);
}
