param()

$ErrorActionPreference = "Stop"

$repoRoot = (Resolve-Path (Join-Path $PSScriptRoot "..")).Path
Set-Location $repoRoot

$verifyAssetsScript = Join-Path $repoRoot "scripts/verify-assets.ps1"
$sourceAssetsRoot = Join-Path $repoRoot "src/main/resources/assets/appliedpackaging"
$sourceAe2AssetsRoot = Join-Path $repoRoot "src/main/resources/assets/ae2"
$tempRoot = Join-Path ([System.IO.Path]::GetTempPath()) ("appliedpackaging-assets-audit-" + [System.Guid]::NewGuid().ToString("N"))
$tinyPngBytes = [byte[]]@(
    137, 80, 78, 71, 13, 10, 26, 10,
    0, 0, 0, 13, 73, 72, 68, 82,
    0, 0, 0, 1, 0, 0, 0, 1,
    8, 6, 0, 0, 0, 31, 21, 196, 137,
    0, 0, 0, 13, 73, 68, 65, 84,
    120, 156, 99, 96, 96, 96, 0, 0,
    0, 4, 0, 1, 243, 255, 97, 212,
    0, 0, 0, 0, 73, 69, 78, 68,
    174, 66, 96, 130
)
$transparent32PngBytes = [Convert]::FromBase64String("iVBORw0KGgoAAAANSUhEUgAAACAAAAAgCAYAAABzenr0AAAAGklEQVR42u3BAQEAAACCIP+vbkhAAQAAAO8GECAAAcm1w7EAAAAASUVORK5CYII=")
$solid32PngBytes = [Convert]::FromBase64String("iVBORw0KGgoAAAANSUhEUgAAACAAAAAgCAYAAABzenr0AAAAMElEQVR42u3OIQEAAAgDMAKQghT0LwYxbibmVz17SSUgICAgICAgICAgICAgIJAOPBSKlEzX+oz1AAAAAElFTkSuQmCC")

function New-AssetsFixture {
    param([string] $CaseName)

    $caseRoot = Join-Path $tempRoot $CaseName
    $caseAssetsRoot = Join-Path $caseRoot "src/main/resources/assets"
    New-Item -ItemType Directory -Force -Path $caseAssetsRoot | Out-Null
    Copy-Item -LiteralPath $sourceAssetsRoot -Destination $caseAssetsRoot -Recurse
    Copy-Item -LiteralPath $sourceAe2AssetsRoot -Destination $caseAssetsRoot -Recurse
    return $caseRoot
}

function Invoke-AssetsCase {
    param(
        [string] $Name,
        [string] $RootPath,
        [int] $ExpectedExitCode,
        [string] $ExpectedText = "",
        [switch] $RunPngVisualContentAudit
    )

    $verifyArguments = @(
        "-NoProfile",
        "-ExecutionPolicy", "Bypass",
        "-File", $verifyAssetsScript,
        "-RootPath", $RootPath
    )
    if (-not $RunPngVisualContentAudit) {
        $verifyArguments += "-SkipPngVisualContent"
    }
    $output = & pwsh @verifyArguments 2>&1 | Out-String
    $exitCode = $LASTEXITCODE

    if ($exitCode -ne $ExpectedExitCode) {
        Write-Host "[FAIL] $Name expected exit $ExpectedExitCode but got $exitCode" -ForegroundColor Red
        Write-Host $output
        exit 1
    }

    if ($ExpectedText -ne "" -and -not $output.Contains($ExpectedText)) {
        Write-Host "[FAIL] $Name missing expected text: $ExpectedText" -ForegroundColor Red
        Write-Host $output
        exit 1
    }

    Write-Host "[PASS] $Name exited $ExpectedExitCode" -ForegroundColor Green
}

try {
    New-Item -ItemType Directory -Force -Path $tempRoot | Out-Null

    $validFixture = New-AssetsFixture "valid"
    Invoke-AssetsCase `
        -Name "valid assets fixture" `
        -RootPath $validFixture `
        -ExpectedExitCode 0 `
        -RunPngVisualContentAudit

    $badDimensionFixture = New-AssetsFixture "bad-dimension"
    $badDimensionPath = Join-Path $badDimensionFixture "src/main/resources/assets/appliedpackaging/textures/item/package_pattern.png"
    [System.IO.File]::WriteAllBytes($badDimensionPath, $tinyPngBytes)
    Invoke-AssetsCase `
        -Name "bad user pattern item dimension fixture" `
        -RootPath $badDimensionFixture `
        -ExpectedExitCode 1 `
        -ExpectedText "expected 16x16 user pattern item texture"

    $badSequenceBufferDimensionFixture = New-AssetsFixture "bad-sequence-buffer-dimension"
    $badSequenceBufferDimensionPath = Join-Path $badSequenceBufferDimensionFixture "src/main/resources/assets/appliedpackaging/textures/block/sequence_buffer/faces/undirected_unformed.png"
    [System.IO.File]::WriteAllBytes($badSequenceBufferDimensionPath, $tinyPngBytes)
    Invoke-AssetsCase `
        -Name "bad Sequence Buffer texture dimension fixture" `
        -RootPath $badSequenceBufferDimensionFixture `
        -ExpectedExitCode 1 `
        -ExpectedText "expected 16x16 Sequence Buffer face texture"

    $badSequenceBufferGuiDimensionFixture = New-AssetsFixture "bad-sequence-buffer-gui-dimension"
    $badSequenceBufferGuiDimensionPath = Join-Path $badSequenceBufferGuiDimensionFixture "src/main/resources/assets/appliedpackaging/textures/gui/sequence_buffer.png"
    [System.IO.File]::WriteAllBytes($badSequenceBufferGuiDimensionPath, $tinyPngBytes)
    Invoke-AssetsCase `
        -Name "bad Sequence Buffer GUI atlas dimension fixture" `
        -RootPath $badSequenceBufferGuiDimensionFixture `
        -ExpectedExitCode 1 `
        -ExpectedText "expected 256x256 Sequence Buffer GUI atlas"

    $replacedSequenceBufferGuiFixture = New-AssetsFixture "replaced-sequence-buffer-gui"
    $replacedSequenceBufferGuiPath = Join-Path $replacedSequenceBufferGuiFixture "src/main/resources/assets/appliedpackaging/textures/gui/sequence_buffer.png"
    Copy-Item -LiteralPath (Join-Path $replacedSequenceBufferGuiFixture "src/main/resources/assets/appliedpackaging/textures/gui/package-storagebus.png") -Destination $replacedSequenceBufferGuiPath -Force
    Invoke-AssetsCase `
        -Name "replaced Sequence Buffer user GUI fixture" `
        -RootPath $replacedSequenceBufferGuiFixture `
        -ExpectedExitCode 1 `
        -ExpectedText "Byte-preserved PNG keeps its source hash: src/main/resources/assets/appliedpackaging/textures/gui/sequence_buffer.png"

    $badGuiAtlasDimensionFixture = New-AssetsFixture "bad-gui-atlas-dimension"
    $badGuiAtlasDimensionPath = Join-Path $badGuiAtlasDimensionFixture "src/main/resources/assets/appliedpackaging/textures/gui/mepackageassembler.png"
    [System.IO.File]::WriteAllBytes($badGuiAtlasDimensionPath, $tinyPngBytes)
    Invoke-AssetsCase `
        -Name "bad package assembler GUI atlas dimension fixture" `
        -RootPath $badGuiAtlasDimensionFixture `
        -ExpectedExitCode 1 `
        -ExpectedText "expected 256x256 ME Package Assembler GUI atlas"

    $replacedPackageAssemblerGuiFixture = New-AssetsFixture "replaced-package-assembler-gui"
    $replacedPackageAssemblerGuiPath = Join-Path $replacedPackageAssemblerGuiFixture "src/main/resources/assets/appliedpackaging/textures/gui/mepackageassembler.png"
    Copy-Item `
        -LiteralPath (Join-Path $replacedPackageAssemblerGuiFixture "src/main/resources/assets/appliedpackaging/textures/gui/package-storagebus.png") `
        -Destination $replacedPackageAssemblerGuiPath `
        -Force
    Invoke-AssetsCase `
        -Name "replaced Package Assembler user GUI fixture" `
        -RootPath $replacedPackageAssemblerGuiFixture `
        -ExpectedExitCode 1 `
        -ExpectedText "Byte-preserved PNG keeps its source hash: src/main/resources/assets/appliedpackaging/textures/gui/mepackageassembler.png"

    $badAdvancedTerminalGuiFixture = New-AssetsFixture "bad-advanced-terminal-gui-dimension"
    $badAdvancedTerminalGuiPath = Join-Path $badAdvancedTerminalGuiFixture "src/main/resources/assets/appliedpackaging/textures/gui/advanced_pattern_encoding_terminal.png"
    [System.IO.File]::WriteAllBytes($badAdvancedTerminalGuiPath, $tinyPngBytes)
    Invoke-AssetsCase `
        -Name "bad advanced terminal GUI dimension fixture" `
        -RootPath $badAdvancedTerminalGuiFixture `
        -ExpectedExitCode 1 `
        -ExpectedText "expected 256x256 advanced pattern encoding terminal GUI atlas"

    $badAdvancedTerminalSpriteFixture = New-AssetsFixture "bad-advanced-terminal-sprite-dimension"
    $badAdvancedTerminalSpritePath = Join-Path $badAdvancedTerminalSpriteFixture "src/main/resources/assets/appliedpackaging/textures/gui/advanced_pattern_encoding_terminal_sprites.png"
    [System.IO.File]::WriteAllBytes($badAdvancedTerminalSpritePath, $tinyPngBytes)
    Invoke-AssetsCase `
        -Name "bad advanced terminal sprite atlas dimension fixture" `
        -RootPath $badAdvancedTerminalSpriteFixture `
        -ExpectedExitCode 1 `
        -ExpectedText "expected 256x256 advanced pattern encoding terminal sprite atlas"

    $badAdvancedTerminalStatesFixture = New-AssetsFixture "bad-advanced-terminal-states-dimension"
    $badAdvancedTerminalStatesPath = Join-Path $badAdvancedTerminalStatesFixture "src/main/resources/assets/appliedpackaging/textures/gui/advanced_pattern_encoding_terminal_states.png"
    [System.IO.File]::WriteAllBytes($badAdvancedTerminalStatesPath, $tinyPngBytes)
    Invoke-AssetsCase `
        -Name "bad advanced terminal AE2 states atlas dimension fixture" `
        -RootPath $badAdvancedTerminalStatesFixture `
        -ExpectedExitCode 1 `
        -ExpectedText "expected 256x256 advanced pattern encoding terminal AE2 states atlas"

    $badAdvancedTerminalMiddleRowFixture = New-AssetsFixture "bad-advanced-terminal-middle-row-dimension"
    $badAdvancedTerminalMiddleRowPath = Join-Path $badAdvancedTerminalMiddleRowFixture "src/main/resources/assets/appliedpackaging/textures/gui/advanced_pattern_encoding_terminal_middle_row.png"
    [System.IO.File]::WriteAllBytes($badAdvancedTerminalMiddleRowPath, $tinyPngBytes)
    Invoke-AssetsCase `
        -Name "bad advanced terminal middle row dimension fixture" `
        -RootPath $badAdvancedTerminalMiddleRowFixture `
        -ExpectedExitCode 1 `
        -ExpectedText "expected 195x18 advanced pattern encoding terminal middle row"

    $badAdvancedTerminalScrollbarFixture = New-AssetsFixture "bad-advanced-terminal-scrollbar-dimension"
    $badAdvancedTerminalScrollbarPath = Join-Path $badAdvancedTerminalScrollbarFixture "src/main/resources/assets/appliedpackaging/textures/gui/advanced_pattern_encoding_terminal_scrollbar.png"
    [System.IO.File]::WriteAllBytes($badAdvancedTerminalScrollbarPath, $tinyPngBytes)
    Invoke-AssetsCase `
        -Name "bad advanced terminal scrollbar dimension fixture" `
        -RootPath $badAdvancedTerminalScrollbarFixture `
        -ExpectedExitCode 1 `
        -ExpectedText "expected 256x256 advanced pattern encoding terminal AE2 scrollbar atlas"

    $badPackageModeFixture = New-AssetsFixture "bad-package-pattern-mode-dimension"
    $badPackageModePath = Join-Path $badPackageModeFixture "src/main/resources/assets/appliedpackaging/textures/gui/pattern_mode_packaging.png"
    [System.IO.File]::WriteAllBytes($badPackageModePath, $tinyPngBytes)
    Invoke-AssetsCase `
        -Name "bad package pattern mode GUI atlas dimension fixture" `
        -RootPath $badPackageModeFixture `
        -ExpectedExitCode 1 `
        -ExpectedText "expected 256x256 package pattern mode GUI atlas"

    $badPatternTerminalBaseFixture = New-AssetsFixture "bad-pattern-terminal-base-dimension"
    $badPatternTerminalBasePath = Join-Path $badPatternTerminalBaseFixture "src/main/resources/assets/appliedpackaging/textures/gui/pattern_encoding_terminal.png"
    [System.IO.File]::WriteAllBytes($badPatternTerminalBasePath, $tinyPngBytes)
    Invoke-AssetsCase `
        -Name "bad package-mode full-screen terminal base dimension fixture" `
        -RootPath $badPatternTerminalBaseFixture `
        -ExpectedExitCode 1 `
        -ExpectedText "expected 256x256 AE2 v19 package-mode full-screen terminal base GUI"

    $modifiedPatternTerminalBaseFixture = New-AssetsFixture "modified-pattern-terminal-base"
    $modifiedPatternTerminalBasePath = Join-Path $modifiedPatternTerminalBaseFixture "src/main/resources/assets/appliedpackaging/textures/gui/pattern_encoding_terminal.png"
    $alternatePatternTerminalBasePath = Join-Path $modifiedPatternTerminalBaseFixture "src/main/resources/assets/appliedpackaging/textures/gui/pattern_mode_packaging.png"
    Copy-Item -LiteralPath $alternatePatternTerminalBasePath -Destination $modifiedPatternTerminalBasePath -Force
    Invoke-AssetsCase `
        -Name "modified byte-preserved package-mode full-screen terminal base fixture" `
        -RootPath $modifiedPatternTerminalBaseFixture `
        -ExpectedExitCode 1 `
        -ExpectedText "Byte-preserved PNG keeps its source hash: src/main/resources/assets/appliedpackaging/textures/gui/pattern_encoding_terminal.png"

    $modifiedAdvancedTerminalGuiFixture = New-AssetsFixture "modified-advanced-terminal-gui"
    $modifiedAdvancedTerminalGuiPath = Join-Path $modifiedAdvancedTerminalGuiFixture "src/main/resources/assets/appliedpackaging/textures/gui/advanced_pattern_encoding_terminal.png"
    $alternateAdvancedTerminalGuiPath = Join-Path $modifiedAdvancedTerminalGuiFixture "src/main/resources/assets/appliedpackaging/textures/gui/pattern_encoding_terminal.png"
    Copy-Item -LiteralPath $alternateAdvancedTerminalGuiPath -Destination $modifiedAdvancedTerminalGuiPath -Force
    Invoke-AssetsCase `
        -Name "modified byte-preserved combined terminal base fixture" `
        -RootPath $modifiedAdvancedTerminalGuiFixture `
        -ExpectedExitCode 1 `
        -ExpectedText "Byte-preserved PNG keeps its source hash: src/main/resources/assets/appliedpackaging/textures/gui/advanced_pattern_encoding_terminal.png"

    $modifiedPackageModeFixture = New-AssetsFixture "modified-package-mode-atlas"
    $modifiedPackageModePath = Join-Path $modifiedPackageModeFixture "src/main/resources/assets/appliedpackaging/textures/gui/pattern_mode_packaging.png"
    $alternatePackageModePath = Join-Path $modifiedPackageModeFixture "src/main/resources/assets/appliedpackaging/textures/gui/advanced_pattern_encoding_terminal.png"
    Copy-Item -LiteralPath $alternatePackageModePath -Destination $modifiedPackageModePath -Force
    Invoke-AssetsCase `
        -Name "modified byte-preserved package mode atlas fixture" `
        -RootPath $modifiedPackageModeFixture `
        -ExpectedExitCode 1 `
        -ExpectedText "Byte-preserved PNG keeps its source hash: src/main/resources/assets/appliedpackaging/textures/gui/pattern_mode_packaging.png"

    $modifiedPackageBusSourceFixture = New-AssetsFixture "modified-package-bus-source"
    $modifiedPackageBusSourcePath = Join-Path $modifiedPackageBusSourceFixture "src/main/resources/assets/appliedpackaging/textures/gui/package-storagebus.png"
    $alternatePackageBusSourcePath = Join-Path $modifiedPackageBusSourceFixture "src/main/resources/assets/appliedpackaging/textures/gui/package-storagebus-sprites.png"
    Copy-Item -LiteralPath $alternatePackageBusSourcePath -Destination $modifiedPackageBusSourcePath -Force
    Invoke-AssetsCase `
        -Name "modified byte-preserved Package Bus texture fixture" `
        -RootPath $modifiedPackageBusSourceFixture `
        -ExpectedExitCode 1 `
        -ExpectedText "Byte-preserved PNG keeps its source hash: src/main/resources/assets/appliedpackaging/textures/gui/package-storagebus.png"

    $modifiedAdvancedTerminalMaskFixture = New-AssetsFixture "modified-advanced-terminal-mask"
    $modifiedAdvancedTerminalDarkPath = Join-Path $modifiedAdvancedTerminalMaskFixture "src/main/resources/assets/appliedpackaging/textures/part/advanced_pattern_encoding_terminal_dark.png"
    $alternateAdvancedTerminalMaskPath = Join-Path $modifiedAdvancedTerminalMaskFixture "src/main/resources/assets/appliedpackaging/textures/part/advanced_pattern_encoding_terminal_medium.png"
    Copy-Item -LiteralPath $alternateAdvancedTerminalMaskPath -Destination $modifiedAdvancedTerminalDarkPath -Force
    Invoke-AssetsCase `
        -Name "modified byte-preserved Advanced Terminal mask fixture" `
        -RootPath $modifiedAdvancedTerminalMaskFixture `
        -ExpectedExitCode 1 `
        -ExpectedText "Byte-preserved PNG keeps its source hash: src/main/resources/assets/appliedpackaging/textures/part/advanced_pattern_encoding_terminal_dark.png"

    $badHeaderFixture = New-AssetsFixture "bad-header"
    $badHeaderPath = Join-Path $badHeaderFixture "src/main/resources/assets/appliedpackaging/textures/gui/icons/color_select.png"
    [System.IO.File]::WriteAllBytes($badHeaderPath, [byte[]]@(1, 2, 3, 4))
    Invoke-AssetsCase `
        -Name "bad PNG header fixture" `
        -RootPath $badHeaderFixture `
        -ExpectedExitCode 1 `
        -ExpectedText "Invalid PNG headers"

    $transparentFixture = New-AssetsFixture "transparent-png"
    $transparentPath = Join-Path $transparentFixture "src/main/resources/assets/appliedpackaging/textures/item/package_pattern.png"
    [System.IO.File]::WriteAllBytes($transparentPath, $transparent32PngBytes)
    Invoke-AssetsCase `
        -Name "transparent PNG fixture" `
        -RootPath $transparentFixture `
        -ExpectedExitCode 1 `
        -ExpectedText "fully transparent" `
        -RunPngVisualContentAudit

    $solidFixture = New-AssetsFixture "solid-png"
    $solidPath = Join-Path $solidFixture "src/main/resources/assets/appliedpackaging/textures/item/package_pattern.png"
    [System.IO.File]::WriteAllBytes($solidPath, $solid32PngBytes)
    Invoke-AssetsCase `
        -Name "single-color PNG fixture" `
        -RootPath $solidFixture `
        -ExpectedExitCode 1 `
        -ExpectedText "single-color placeholder" `
        -RunPngVisualContentAudit

    $missingRequiredFixture = New-AssetsFixture "missing-required"
    Remove-Item -LiteralPath (Join-Path $missingRequiredFixture "src/main/resources/assets/appliedpackaging/logo.png") -Force
    Invoke-AssetsCase `
        -Name "missing required PNG fixture" `
        -RootPath $missingRequiredFixture `
        -ExpectedExitCode 1 `
        -ExpectedText "Required PNG exists: src/main/resources/assets/appliedpackaging/logo.png"

    $nativeStyleOverrideFixture = New-AssetsFixture "native-pattern-style-override"
    $nativeStyleOverridePath = Join-Path $nativeStyleOverrideFixture "src/main/resources/assets/ae2/screens/terminals/pattern_encoding_terminal.json"
    New-Item -ItemType Directory -Force -Path (Split-Path -Parent $nativeStyleOverridePath) | Out-Null
    Copy-Item `
        -LiteralPath (Join-Path $nativeStyleOverrideFixture "src/main/resources/assets/ae2/screens/appliedpackaging/advanced_pattern_encoding_terminal.json") `
        -Destination $nativeStyleOverridePath
    Invoke-AssetsCase `
        -Name "forbidden AE2 native pattern ScreenStyle override fixture" `
        -RootPath $nativeStyleOverrideFixture `
        -ExpectedExitCode 1 `
        -ExpectedText "AE2 native pattern terminal ScreenStyle is not overridden"

    $badPackageProfileFixture = New-AssetsFixture "bad-specialized-terminal-package-profile"
    $badPackageProfilePath = Join-Path $badPackageProfileFixture "src/main/resources/assets/ae2/screens/appliedpackaging/advanced_pattern_encoding_terminal.json"
    $badPackageProfile = Get-Content -Raw -LiteralPath $badPackageProfilePath | ConvertFrom-Json
    $badPackageProfile.widgets.packagePatternModeScrollbar.bottom = 177
    $badPackageProfile | ConvertTo-Json -Depth 20 | Set-Content -LiteralPath $badPackageProfilePath -Encoding UTF8
    Invoke-AssetsCase `
        -Name "bad specialized terminal package profile fixture" `
        -RootPath $badPackageProfileFixture `
        -ExpectedExitCode 1 `
        -ExpectedText "Combined specialized pattern terminal declares the package-mode scrollbar geometry"

    $overlappingAdvancedHeaderFixture = New-AssetsFixture "overlapping-specialized-terminal-header-controls"
    $overlappingAdvancedHeaderPath = Join-Path $overlappingAdvancedHeaderFixture "src/main/resources/assets/ae2/screens/appliedpackaging/advanced_pattern_encoding_terminal.json"
    $overlappingAdvancedHeader = Get-Content -Raw -LiteralPath $overlappingAdvancedHeaderPath | ConvertFrom-Json
    $overlappingAdvancedHeader.widgets.processingCycleOutput.bottom = 172
    $overlappingAdvancedHeader | ConvertTo-Json -Depth 20 | Set-Content -LiteralPath $overlappingAdvancedHeaderPath -Encoding UTF8
    Invoke-AssetsCase `
        -Name "overlapping specialized terminal header controls fixture" `
        -RootPath $overlappingAdvancedHeaderFixture `
        -ExpectedExitCode 1 `
        -ExpectedText "Combined specialized pattern terminal keeps advanced header controls above the input-grid border"

    $badSequenceBufferBlockstateFixture = New-AssetsFixture "bad-sequence-buffer-blockstate"
    $badSequenceBufferBlockstatePath = Join-Path $badSequenceBufferBlockstateFixture "src/main/resources/assets/appliedpackaging/blockstates/sequence_buffer.json"
    $badSequenceBufferBlockstate = Get-Content -Raw -LiteralPath $badSequenceBufferBlockstatePath | ConvertFrom-Json
    $badSequenceBufferBlockstate.multipart = @($badSequenceBufferBlockstate.multipart | Where-Object { $_.when.state -ne "endpoint" })
    $badSequenceBufferBlockstate | ConvertTo-Json -Depth 20 | Set-Content -LiteralPath $badSequenceBufferBlockstatePath -Encoding UTF8
    Invoke-AssetsCase `
        -Name "missing Sequence Buffer endpoint state fixture" `
        -RootPath $badSequenceBufferBlockstateFixture `
        -ExpectedExitCode 1 `
        -ExpectedText "Sequence Buffer blockstate renders all five visual states"

    $badSequenceBufferMainStyleFixture = New-AssetsFixture "bad-sequence-buffer-main-style"
    $badSequenceBufferMainStylePath = Join-Path $badSequenceBufferMainStyleFixture "src/main/resources/assets/ae2/screens/appliedpackaging/sequence_buffer_main.json"
    $badSequenceBufferMainStyle = Get-Content -Raw -LiteralPath $badSequenceBufferMainStylePath | ConvertFrom-Json
    $badSequenceBufferMainStyle.widgets.sequenceBufferScrollbar.height = 72
    $badSequenceBufferMainStyle | ConvertTo-Json -Depth 20 | Set-Content -LiteralPath $badSequenceBufferMainStylePath -Encoding UTF8
    Invoke-AssetsCase `
        -Name "bad Sequence Buffer main scrollbar fixture" `
        -RootPath $badSequenceBufferMainStyleFixture `
        -ExpectedExitCode 1 `
        -ExpectedText "Sequence Buffer main screen keeps the high-version three-row scrollbar geometry"

    $legacyOptionalSlotFixture = New-AssetsFixture "legacy-optional-slot-renderer"
    $packageAssemblerMenuRelativePath = "src/main/java/com/warmthdawn/appliedpackaging/world/menu/PackageAssemblerMenu.java"
    $packageAssemblerMenuSourcePath = Join-Path $repoRoot $packageAssemblerMenuRelativePath
    $legacyOptionalSlotPath = Join-Path $legacyOptionalSlotFixture $packageAssemblerMenuRelativePath
    New-Item -ItemType Directory -Force -Path (Split-Path -Parent $legacyOptionalSlotPath) | Out-Null
    $legacyOptionalSlotText = (Get-Content -Raw -LiteralPath $packageAssemblerMenuSourcePath).Replace(
        "return false;",
        "return true;")
    Set-Content -LiteralPath $legacyOptionalSlotPath -Value $legacyOptionalSlotText -Encoding UTF8
    Invoke-AssetsCase `
        -Name "legacy AE2 optional-slot renderer fixture" `
        -RootPath $legacyOptionalSlotFixture `
        -ExpectedExitCode 1 `
        -ExpectedText "Applied Packaging optional slots never enable AE2 15's legacy slot-background renderer"

    $badSequenceBufferUpgradeSideFixture = New-AssetsFixture "bad-sequence-buffer-upgrade-side"
    $badSequenceBufferUpgradeSidePath = Join-Path $badSequenceBufferUpgradeSideFixture "src/main/resources/assets/ae2/screens/appliedpackaging/sequence_buffer_main.json"
    $badSequenceBufferUpgradeSide = Get-Content -Raw -LiteralPath $badSequenceBufferUpgradeSidePath | ConvertFrom-Json
    $badSequenceBufferUpgradeSide.widgets.upgrades.PSObject.Properties.Remove("right")
    $badSequenceBufferUpgradeSide.widgets.upgrades | Add-Member -NotePropertyName left -NotePropertyValue -28
    $badSequenceBufferUpgradeSide | ConvertTo-Json -Depth 20 | Set-Content -LiteralPath $badSequenceBufferUpgradeSidePath -Encoding UTF8
    Invoke-AssetsCase `
        -Name "bad Sequence Buffer upgrade side fixture" `
        -RootPath $badSequenceBufferUpgradeSideFixture `
        -ExpectedExitCode 1 `
        -ExpectedText "Sequence Buffer main upgrade panel stays attached to the right side"

    $badSequenceBufferDeferredConfigFixture = New-AssetsFixture "bad-sequence-buffer-deferred-config"
    $badSequenceBufferDeferredConfigPath = Join-Path $badSequenceBufferDeferredConfigFixture "src/main/resources/assets/ae2/screens/appliedpackaging/sequence_buffer_side.json"
    $badSequenceBufferDeferredConfig = Get-Content -Raw -LiteralPath $badSequenceBufferDeferredConfigPath | ConvertFrom-Json
    $badSequenceBufferDeferredConfig.slots | Add-Member `
        -NotePropertyName CONFIG `
        -NotePropertyValue ([pscustomobject]@{ left = -60; top = 33; grid = "BREAK_AFTER_3COLS" })
    $badSequenceBufferDeferredConfig | ConvertTo-Json -Depth 20 | Set-Content -LiteralPath $badSequenceBufferDeferredConfigPath -Encoding UTF8
    Invoke-AssetsCase `
        -Name "bad Sequence Buffer deferred configuration fixture" `
        -RootPath $badSequenceBufferDeferredConfigFixture `
        -ExpectedExitCode 1 `
        -ExpectedText "Sequence Buffer side screen leaves the deferred input-filter panel hidden"

    $missingSequenceBufferSettingFixture = New-AssetsFixture "missing-sequence-buffer-setting"
    $sequenceBufferSharedScreenRelativePath = "src/main/java/com/warmthdawn/appliedpackaging/client/screen/AbstractSequenceBufferScreen.java"
    $sequenceBufferSharedScreenSourcePath = Join-Path $repoRoot $sequenceBufferSharedScreenRelativePath
    $missingSequenceBufferSettingPath = Join-Path $missingSequenceBufferSettingFixture $sequenceBufferSharedScreenRelativePath
    New-Item -ItemType Directory -Force -Path (Split-Path -Parent $missingSequenceBufferSettingPath) | Out-Null
    $missingSequenceBufferSettingText = (Get-Content -Raw -LiteralPath $sequenceBufferSharedScreenSourcePath).Replace(
        "menu::togglePatternMode",
        "menu::toggleAutoOutput")
    Set-Content -LiteralPath $missingSequenceBufferSettingPath -Value $missingSequenceBufferSettingText -Encoding UTF8
    Invoke-AssetsCase `
        -Name "missing Sequence Buffer setting fixture" `
        -RootPath $missingSequenceBufferSettingFixture `
        -ExpectedExitCode 1 `
        -ExpectedText "Sequence Buffer main and side screens expose all five endpoint settings in the shared AE2 toolbar"

    $badSequenceBufferSideStyleFixture = New-AssetsFixture "bad-sequence-buffer-side-style"
    $badSequenceBufferSideStylePath = Join-Path $badSequenceBufferSideStyleFixture "src/main/resources/assets/ae2/screens/appliedpackaging/sequence_buffer_side.json"
    $badSequenceBufferSideStyle = Get-Content -Raw -LiteralPath $badSequenceBufferSideStylePath | ConvertFrom-Json
    $badSequenceBufferSideStyle.slots.APPLIEDPACKAGING_SEQUENCE_BUFFER_CONTENTS.left = 79
    $badSequenceBufferSideStyle | ConvertTo-Json -Depth 20 | Set-Content -LiteralPath $badSequenceBufferSideStylePath -Encoding UTF8
    Invoke-AssetsCase `
        -Name "bad Sequence Buffer side storage slot fixture" `
        -RootPath $badSequenceBufferSideStyleFixture `
        -ExpectedExitCode 1 `
        -ExpectedText "Sequence Buffer side screen keeps the single central storage slot"

    $missingVerticalSequenceBufferModelFixture = New-AssetsFixture "missing-vertical-sequence-buffer-model"
    $missingVerticalSequenceBufferModelPath = Join-Path $missingVerticalSequenceBufferModelFixture "src/main/resources/assets/appliedpackaging/models/block/sequence_buffer/generated/member/y.json"
    Remove-Item -LiteralPath $missingVerticalSequenceBufferModelPath -Force
    Invoke-AssetsCase `
        -Name "missing vertical Sequence Buffer member model fixture" `
        -RootPath $missingVerticalSequenceBufferModelFixture `
        -ExpectedExitCode 1 `
        -ExpectedText "Sequence Buffer model asset exists: src/main/resources/assets/appliedpackaging/models/block/sequence_buffer/generated/member/y.json"

    $reversedSequenceBufferTailFixture = New-AssetsFixture "reversed-sequence-buffer-tail-edge"
    $reversedSequenceBufferTailPath = Join-Path $reversedSequenceBufferTailFixture "src/main/resources/assets/appliedpackaging/models/block/sequence_buffer/generated/tail/east.json"
    $reversedSequenceBufferTail = Get-Content -Raw -LiteralPath $reversedSequenceBufferTailPath | ConvertFrom-Json
    $reversedSequenceBufferTail.elements[0].faces.up.rotation = 270
    $reversedSequenceBufferTail | ConvertTo-Json -Depth 20 | Set-Content -LiteralPath $reversedSequenceBufferTailPath -Encoding UTF8
    Invoke-AssetsCase `
        -Name "reversed Sequence Buffer tail edge fixture" `
        -RootPath $reversedSequenceBufferTailFixture `
        -ExpectedExitCode 1 `
        -ExpectedText "Sequence Buffer east tail keeps its cap outward and its open edge toward the controller"

    $flatSequenceBufferItemFixture = New-AssetsFixture "flat-sequence-buffer-item"
    $flatSequenceBufferShellPath = Join-Path $flatSequenceBufferItemFixture "src/main/resources/assets/appliedpackaging/models/block/sequence_buffer/shell.json"
    $flatSequenceBufferShell = Get-Content -Raw -LiteralPath $flatSequenceBufferShellPath | ConvertFrom-Json
    $flatSequenceBufferShell.PSObject.Properties.Remove("parent")
    $flatSequenceBufferShell | ConvertTo-Json -Depth 20 | Set-Content -LiteralPath $flatSequenceBufferShellPath -Encoding UTF8
    Invoke-AssetsCase `
        -Name "flat Sequence Buffer inventory item fixture" `
        -RootPath $flatSequenceBufferItemFixture `
        -ExpectedExitCode 1 `
        -ExpectedText "Sequence Buffer shell inherits the standard 3D block item transforms"

    $badPackageModelFixture = New-AssetsFixture "bad-package-box-cropped-uv"
    $badPackageModelPath = Join-Path $badPackageModelFixture "src/main/resources/assets/appliedpackaging/models/item/package_box/fluix.json"
    $badPackageModel = Get-Content -Raw -LiteralPath $badPackageModelPath | ConvertFrom-Json
    $badPackageModel.elements[0].faces.north.uv = @(3, 1, 13, 9)
    $badPackageModel | ConvertTo-Json -Depth 20 | Set-Content -LiteralPath $badPackageModelPath -Encoding UTF8
    Invoke-AssetsCase `
        -Name "bad package_box cropped UV fixture" `
        -RootPath $badPackageModelFixture `
        -ExpectedExitCode 1 `
        -ExpectedText "uses full-face uv"

    $missingMarkerOverrideFixture = New-AssetsFixture "missing-package-marker-override"
    $missingMarkerOverridePath = Join-Path $missingMarkerOverrideFixture "src/main/resources/assets/appliedpackaging/models/item/fluix_package.json"
    $missingMarkerOverride = Get-Content -Raw -LiteralPath $missingMarkerOverridePath | ConvertFrom-Json
    $missingMarkerOverride.PSObject.Properties.Remove("overrides")
    $missingMarkerOverride | ConvertTo-Json -Depth 20 | Set-Content -LiteralPath $missingMarkerOverridePath -Encoding UTF8
    Invoke-AssetsCase `
        -Name "missing package marker override fixture" `
        -RootPath $missingMarkerOverrideFixture `
        -ExpectedExitCode 1 `
        -ExpectedText "declares the marker custom-render override"

    $badPackageGuiTransformFixture = New-AssetsFixture "bad-package-gui-transform"
    $badPackageGuiTransformPath = Join-Path $badPackageGuiTransformFixture "src/main/resources/assets/appliedpackaging/models/item/package_box/_transforms.json"
    $badPackageGuiTransform = Get-Content -Raw -LiteralPath $badPackageGuiTransformPath | ConvertFrom-Json
    $badPackageGuiTransform.display.gui.translation = @(0, 3, 0)
    $badPackageGuiTransform | ConvertTo-Json -Depth 20 | Set-Content -LiteralPath $badPackageGuiTransformPath -Encoding UTF8
    Invoke-AssetsCase `
        -Name "bad package GUI transform fixture" `
        -RootPath $badPackageGuiTransformFixture `
        -ExpectedExitCode 1 `
        -ExpectedText "centers the transformed cuboid in the GUI"

    $badOpaqueModelFixture = New-AssetsFixture "bad-opaque-model-render-type"
    $badOpaqueModelPath = Join-Path $badOpaqueModelFixture "src/main/resources/assets/appliedpackaging/models/part/package_storage_bus_base.json"
    $badOpaqueModel = Get-Content -Raw -LiteralPath $badOpaqueModelPath | ConvertFrom-Json
    $badOpaqueModel | Add-Member -NotePropertyName "render_type" -NotePropertyValue "minecraft:cutout_mipped"
    $badOpaqueModel | ConvertTo-Json -Depth 20 | Set-Content -LiteralPath $badOpaqueModelPath -Encoding UTF8
    Invoke-AssetsCase `
        -Name "bad opaque model render_type fixture" `
        -RootPath $badOpaqueModelFixture `
        -ExpectedExitCode 1 `
        -ExpectedText "Opaque block/part model must use the default solid render type"

    $badAdvancedTerminalTintFixture = New-AssetsFixture "bad-advanced-terminal-tint-order"
    $badAdvancedTerminalTintPath = Join-Path $badAdvancedTerminalTintFixture "src/main/resources/assets/appliedpackaging/models/part/advanced_pattern_encoding_terminal_on.json"
    $badAdvancedTerminalTint = Get-Content -Raw -LiteralPath $badAdvancedTerminalTintPath | ConvertFrom-Json
    $badAdvancedTerminalTint.elements[0].faces.north.tintindex = 1
    $badAdvancedTerminalTint | ConvertTo-Json -Depth 20 | Set-Content -LiteralPath $badAdvancedTerminalTintPath -Encoding UTF8
    Invoke-AssetsCase `
        -Name "bad Advanced Terminal tint order fixture" `
        -RootPath $badAdvancedTerminalTintFixture `
        -ExpectedExitCode 1 `
        -ExpectedText "uses v19 dark/medium/bright tint order [3,2,1]"

    Write-Host ""
    Write-Host "Asset audit self-test passed." -ForegroundColor Green
} finally {
    if (Test-Path -LiteralPath $tempRoot) {
        $resolvedTempRoot = (Resolve-Path -LiteralPath $tempRoot).Path
        $resolvedTempParent = (Resolve-Path -LiteralPath ([System.IO.Path]::GetTempPath())).Path
        $tempLeaf = Split-Path -Leaf $resolvedTempRoot
        if (-not $resolvedTempRoot.StartsWith($resolvedTempParent, [System.StringComparison]::OrdinalIgnoreCase) -or
            -not $tempLeaf.StartsWith("appliedpackaging-assets-audit-", [System.StringComparison]::Ordinal)) {
            throw "Refusing to remove unexpected asset audit self-test path: $resolvedTempRoot"
        }

        Remove-Item -LiteralPath $resolvedTempRoot -Recurse -Force
    }
}
