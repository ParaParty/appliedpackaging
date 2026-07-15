# License

Copyright (c) 2026 WarmthDawn.

All rights reserved.

No permission is granted to copy, modify, redistribute, sublicense, publish, or use this project or its assets except where the copyright holder provides separate written permission.

This license statement applies to the Applied Packaging source code, documentation, generated resources, textures, models, and other project assets unless a file explicitly states a different license.

## Third-Party Asset and Source Exception

The following GUI assets and AE2-derived source portions contain or adapt Applied Energistics 2 high-version work and are distributed under `LGPL-3.0-or-later`, not under the Applied Packaging All Rights Reserved terms above:

```text
src/main/resources/assets/appliedpackaging/textures/gui/advanced_pattern_encoding_terminal.png
src/main/resources/assets/appliedpackaging/textures/gui/advanced_pattern_encoding_terminal_sprites.png
src/main/resources/assets/appliedpackaging/textures/gui/advanced_pattern_encoding_terminal_states.png
src/main/resources/assets/appliedpackaging/textures/gui/advanced_pattern_encoding_terminal_middle_row.png
src/main/resources/assets/appliedpackaging/textures/gui/advanced_pattern_encoding_terminal_scrollbar.png
src/main/resources/assets/appliedpackaging/textures/gui/pattern_encoding_terminal.png
src/main/resources/assets/appliedpackaging/textures/gui/pattern_mode_packaging.png
src/main/resources/assets/appliedpackaging/textures/gui/ae2-states.png
src/main/resources/assets/appliedpackaging/textures/gui/package_bus_extra_panels.png
src/main/resources/assets/appliedpackaging/textures/gui/package_bus_vertical_buttons_bg.png
src/main/java/com/warmthdawn/appliedpackaging/client/widget/ModernUpgradesPanel.java
src/main/java/com/warmthdawn/appliedpackaging/client/screen/ModernUpgradeableScreen.java
AE2-derived portions of src/main/java/com/warmthdawn/appliedpackaging/client/widget/ModernSlotRendering.java
portions of src/main/java/com/warmthdawn/appliedpackaging/client/screen/PackageBusScreen.java
AE2-derived portions of src/main/java/com/warmthdawn/appliedpackaging/client/screen/AdvancedPatternEncodingTermScreen.java
AE2-derived portions of src/main/java/com/warmthdawn/appliedpackaging/client/screen/AdvancedSetPatternAmountScreen.java
src/main/resources/assets/ae2/screens/appliedpackaging/advanced_pattern_encoding_terminal.json
src/main/resources/assets/appliedpackaging/models/item/advanced_pattern_encoding_terminal.json
src/main/resources/assets/appliedpackaging/models/part/advanced_pattern_encoding_terminal_base.json
src/main/resources/assets/appliedpackaging/models/part/advanced_pattern_encoding_terminal_off.json
src/main/resources/assets/appliedpackaging/models/part/advanced_pattern_encoding_terminal_on.json
src/main/resources/assets/appliedpackaging/models/part/advanced_pattern_encoding_terminal_status_off.json
src/main/resources/assets/appliedpackaging/models/part/advanced_pattern_encoding_terminal_status_on.json
src/main/resources/assets/appliedpackaging/models/part/advanced_pattern_encoding_terminal_status_has_channel.json
src/main/resources/assets/appliedpackaging/textures/part/advanced_pattern_encoding_terminal_back.png
src/main/resources/assets/appliedpackaging/textures/part/advanced_pattern_encoding_terminal_colored.png
src/main/resources/assets/appliedpackaging/textures/part/advanced_pattern_encoding_terminal_front.png
src/main/resources/assets/appliedpackaging/textures/part/advanced_pattern_encoding_terminal_sides.png
src/main/resources/assets/appliedpackaging/textures/part/advanced_pattern_encoding_terminal_sides_status.png
src/main/resources/assets/appliedpackaging/textures/part/advanced_pattern_encoding_terminal_status_off.png
src/main/resources/assets/appliedpackaging/textures/part/advanced_pattern_encoding_terminal_status_on.png
src/main/resources/assets/appliedpackaging/textures/part/advanced_pattern_encoding_terminal_status_has_channel.png
```

The three Package Bus AE2 textures are unmodified copies from Applied Energistics 2 current-main commit `45f315517ea346efc0babd02c85c6b9d32dc8acf`; their bytes also match tag `neoforge/v19.2.17`:

```text
repository: https://github.com/AppliedEnergistics/Applied-Energistics-2
commit: 45f315517ea346efc0babd02c85c6b9d32dc8acf
matching tag: neoforge/v19.2.17
states.png SHA-256: 0996B0084C7BF37F65A97A745982AB681EBD86F142FADE526F14C823C4727E55
extra_panels.png SHA-256: C67FED0F98C9CA67A0602B5589A5191D59D5DD2BD3848C62DE0E209E0E44B8B0
vertical_buttons_bg.png SHA-256: 62150F9869EE17CBD15BDA963542287BF798482CEED1F18F0E24DD82381F7715
```

The supplied Package Bus background and sprite are preserved byte-for-byte as separate textures. No AE2 pixels are baked into either file. In particular, the marker empty-slot icon at sprite rectangle `(32,16,16,16)` is user-authored Applied Packaging artwork and is not part of the AE2 third-party exception. Exact source paths, hashes, and runtime-use notes are recorded in:

```text
src/main/resources/META-INF/licenses/ae2-states-source.txt
src/main/resources/META-INF/licenses/ae2-pattern-screen-source.txt
src/main/resources/META-INF/licenses/ae2-terminal-part-source.txt
```

The three `advanced_pattern_encoding_terminal_dark/medium/bright.png` tint masks are user-provided Applied Packaging artwork and remain under the project terms; they are not copied AE2 textures.

Applied Energistics 2 copyright belongs to its respective contributors. The corresponding LGPL license text is included at:

```text
src/main/resources/META-INF/licenses/ae2-LGPL-3.0-or-later.txt
```
