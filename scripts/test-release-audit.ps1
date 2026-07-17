param()

$ErrorActionPreference = "Stop"

$repoRoot = (Resolve-Path (Join-Path $PSScriptRoot "..")).Path
Set-Location $repoRoot

$verifyReleaseScript = Join-Path $repoRoot "scripts/verify-release.ps1"
$tempRoot = Join-Path ([System.IO.Path]::GetTempPath()) ("appliedpackaging-release-audit-" + [System.Guid]::NewGuid().ToString("N"))
$fixtureJarRelativePath = "build/libs/appliedpackaging-0.1.0-dev.jar"
$pngBytes = [byte[]]@(
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
function Write-Utf8File {
    param(
        [string] $Path,
        [string] $Text
    )

    $parent = Split-Path -Parent $Path
    if (-not [string]::IsNullOrWhiteSpace($parent)) {
        New-Item -ItemType Directory -Force -Path $parent | Out-Null
    }

    [System.IO.File]::WriteAllText($Path, $Text, [System.Text.UTF8Encoding]::new($false))
}

function Add-ZipEntryText {
    param(
        [System.IO.Compression.ZipArchive] $Zip,
        [string] $EntryName,
        [string] $Text
    )

    $entry = $Zip.CreateEntry($EntryName, [System.IO.Compression.CompressionLevel]::Optimal)
    $stream = $entry.Open()
    $writer = [System.IO.StreamWriter]::new($stream, [System.Text.UTF8Encoding]::new($false))
    try {
        $writer.Write($Text)
    } finally {
        $writer.Dispose()
        $stream.Dispose()
    }
}

function Add-ZipEntryBytes {
    param(
        [System.IO.Compression.ZipArchive] $Zip,
        [string] $EntryName,
        [byte[]] $Bytes
    )

    $entry = $Zip.CreateEntry($EntryName, [System.IO.Compression.CompressionLevel]::Optimal)
    $stream = $entry.Open()
    try {
        $stream.Write($Bytes, 0, $Bytes.Length)
    } finally {
        $stream.Dispose()
    }
}

function Update-ZipEntryText {
    param(
        [string] $ZipPath,
        [string] $EntryName,
        [string] $Text
    )

    Add-Type -AssemblyName System.IO.Compression.FileSystem
    $zip = [System.IO.Compression.ZipFile]::Open($ZipPath, [System.IO.Compression.ZipArchiveMode]::Update)
    try {
        $entry = $zip.GetEntry($EntryName)
        if ($null -ne $entry) {
            $entry.Delete()
        }

        Add-ZipEntryText -Zip $zip -EntryName $EntryName -Text $Text
    } finally {
        $zip.Dispose()
    }
}

function Remove-ZipEntry {
    param(
        [string] $ZipPath,
        [string] $EntryName
    )

    Add-Type -AssemblyName System.IO.Compression.FileSystem
    $zip = [System.IO.Compression.ZipFile]::Open($ZipPath, [System.IO.Compression.ZipArchiveMode]::Update)
    try {
        $entry = $zip.GetEntry($EntryName)
        if ($null -eq $entry) {
            throw "Zip entry not found: $EntryName"
        }

        $entry.Delete()
    } finally {
        $zip.Dispose()
    }
}

function Get-GradlePropertiesText {
    @"
mod_id=appliedpackaging
mod_name=Applied Packaging
mod_license=All Rights Reserved
mod_version=0.1.0-dev
mod_authors=WarmthDawn
minecraft_version_range=[1.20.1, 1.21)
forge_version_range=[47.4.10,)
loader_version_range=[47,)
ae2_version_range=[15.4.10,16)
guideme_version_range=[20.1.7,20.2.0)
"@
}

function Get-ModsTomlText {
    @"
modLoader="javafml"
loaderVersion="[47,)"
license="All Rights Reserved"
[[mods]]
modId="appliedpackaging"
version="0.1.0-dev"
displayName="Applied Packaging"
authors="WarmthDawn"
description='''Release audit fixture.'''
[[dependencies.appliedpackaging]]
modId="forge"
mandatory=true
versionRange="[47.4.10,)"
ordering="NONE"
side="BOTH"
[[dependencies.appliedpackaging]]
modId="minecraft"
mandatory=true
versionRange="[1.20.1, 1.21)"
ordering="NONE"
side="BOTH"
[[dependencies.appliedpackaging]]
modId="ae2"
mandatory=true
versionRange="[15.4.10,16)"
ordering="AFTER"
side="BOTH"
[[dependencies.appliedpackaging]]
modId="guideme"
mandatory=true
versionRange="[20.1.7,20.2.0)"
ordering="AFTER"
side="BOTH"
"@
}

function Get-ManifestText {
    @"
Manifest-Version: 1.0
Specification-Title: Applied Packaging
Specification-Version: 0.1.0-dev
Implementation-Title: Applied Packaging
Implementation-Version: 0.1.0-dev
Implementation-Vendor: WarmthDawn

"@
}

function New-ReleaseAuditFixture {
    param([string] $CaseName)

    $caseRoot = Join-Path $tempRoot $CaseName
    New-Item -ItemType Directory -Force -Path $caseRoot | Out-Null

    $fixtureReadmeText = "Release audit fixture readme.`n"
    $fixtureChangelogText = "Release audit fixture changelog.`n"
    $fixtureLicenseText = "Release audit fixture license.`n"
    $fixtureEnLangText = @"
{
  "item.appliedpackaging.release_audit_fixture": "Release Audit Fixture",
  "tooltip.appliedpackaging.release_audit_fixture": "Fixture: %s"
}
"@
    $fixtureZhLangText = @"
{
  "item.appliedpackaging.release_audit_fixture": "Release Audit Fixture",
  "tooltip.appliedpackaging.release_audit_fixture": "Fixture: %s"
}
"@
    $fixtureModelText = @"
{
  "parent": "minecraft:item/generated",
  "textures": {
    "layer0": "appliedpackaging:item/release_audit_fixture"
  }
}
"@
    $fixtureRecipeText = @"
{
  "type": "minecraft:crafting_shapeless",
  "ingredients": [
    {
      "item": "ae2:pattern_encoding_terminal"
    }
  ],
  "result": {
    "item": "appliedpackaging:package_pattern_terminal"
  }
}
"@

    Write-Utf8File -Path (Join-Path $caseRoot "gradle.properties") -Text (Get-GradlePropertiesText)
    Write-Utf8File -Path (Join-Path $caseRoot "README.md") -Text $fixtureReadmeText
    Write-Utf8File -Path (Join-Path $caseRoot "CHANGELOG.md") -Text $fixtureChangelogText
    Write-Utf8File -Path (Join-Path $caseRoot "LICENSE.md") -Text $fixtureLicenseText
    Write-Utf8File -Path (Join-Path $caseRoot "docs/assets/palette.md") -Text "release audit fixture palette`n"
    Write-Utf8File -Path (Join-Path $caseRoot "docs/assets/asset-briefs/ui-and-icons.md") -Text "release audit fixture brief`n"
    Write-Utf8File -Path (Join-Path $caseRoot "docs/assets/contracts/release_audit.yaml") -Text @"
asset_id: release_audit_fixture
asset_kind: item
generation_mode: icon_set
target_game: minecraft_java
target_texture_resolution: 16
style:
  name: ae2_fluix_packaging_ui
  palette_reference: docs/assets/palette.md
asset_brief:
  summary: Release audit fixture icon.
  source_brief: docs/assets/asset-briefs/ui-and-icons.md
materials:
  - dark_metal
required_visual_parts:
  - simple centered test icon
forbidden_visual_parts:
  - text
conversion_requirements:
  transparent_background: true
  readable_at_8x8: true
  icon_ids:
    - release_audit_fixture
harness_assumptions:
  output_namespace: appliedpackaging
  icon_dir: src/main/resources/assets/appliedpackaging/textures/item
  logo_path: src/main/resources/assets/appliedpackaging/logo.png
  report_path: docs/assets/reports/release-audit.md
"@
    Write-Utf8File -Path (Join-Path $caseRoot "src/main/resources/assets/appliedpackaging/lang/en_us.json") -Text $fixtureEnLangText
    Write-Utf8File -Path (Join-Path $caseRoot "src/main/resources/assets/appliedpackaging/lang/zh_cn.json") -Text $fixtureZhLangText
    Write-Utf8File -Path (Join-Path $caseRoot "src/main/resources/assets/appliedpackaging/models/item/release_audit_fixture.json") -Text $fixtureModelText
    Write-Utf8File -Path (Join-Path $caseRoot "src/main/java/com/warmthdawn/appliedpackaging/registry/APCreativeTabs.java") -Text @"
package com.warmthdawn.appliedpackaging.registry;

public final class APCreativeTabs {
    private APCreativeTabs() {
    }

    public static void build(FixtureOutput output) {
        output.accept(APItems.ME_PACKAGER.get());
        output.accept(APItems.PACKAGE_ASSEMBLER.get());
        output.accept(APItems.PACKAGE_STORAGE_BUS.get());
        output.accept(APItems.PACKAGE_UNPACKING_BUS.get());
        output.accept(APItems.ADVANCED_PATTERN_ENCODING_TERMINAL.get());
    }
}
"@
    Write-Utf8File -Path (Join-Path $caseRoot "src/main/java/com/warmthdawn/appliedpackaging/registry/APItems.java") -Text @"
package com.warmthdawn.appliedpackaging.registry;

import appeng.items.parts.PartItem;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;

public final class APItems {
    public static final RegistryObject<Item> PACKAGE_PATTERN = ITEMS.register(
            "package_pattern",
            () -> new Item(new Item.Properties()));

    public static final RegistryObject<Item> ADVANCED_PROCESSING_PATTERN = ITEMS.register(
            "advanced_processing_pattern",
            () -> new AdvancedProcessingPatternItem(new Item.Properties()));

    public static final RegistryObject<Item> PACKAGE_STORAGE_BUS = ITEMS.register(
            "package_storage_bus",
            () -> new PartItem<>(
                    new Item.Properties(),
                    PackageStorageBusPart.class,
                    PackageStorageBusPart::new));

    public static final RegistryObject<Item> PACKAGE_UNPACKING_BUS = ITEMS.register(
            "package_unpacking_bus",
            () -> new PartItem<>(
                    new Item.Properties(),
                    PackageUnpackingBusPart.class,
                    PackageUnpackingBusPart::new));

    public static final RegistryObject<Item> ADVANCED_PATTERN_ENCODING_TERMINAL = ITEMS.register(
            "advanced_pattern_encoding_terminal",
            () -> new PartItem<>(
                    new Item.Properties(),
                    AdvancedPatternEncodingTerminalPart.class,
                    AdvancedPatternEncodingTerminalPart::new));
}
"@

    $texturePath = Join-Path $caseRoot "src/main/resources/assets/appliedpackaging/textures/item/release_audit_fixture.png"
    New-Item -ItemType Directory -Force -Path (Split-Path -Parent $texturePath) | Out-Null
    [System.IO.File]::WriteAllBytes($texturePath, $pngBytes)

    $logoPath = Join-Path $caseRoot "src/main/resources/assets/appliedpackaging/logo.png"
    [System.IO.File]::WriteAllBytes($logoPath, $pngBytes)

    $jarPath = Join-Path $caseRoot $fixtureJarRelativePath
    New-Item -ItemType Directory -Force -Path (Split-Path -Parent $jarPath) | Out-Null

    Add-Type -AssemblyName System.IO.Compression.FileSystem
    $zip = [System.IO.Compression.ZipFile]::Open($jarPath, [System.IO.Compression.ZipArchiveMode]::Create)
    try {
        Add-ZipEntryText -Zip $zip -EntryName "META-INF/mods.toml" -Text (Get-ModsTomlText)
        Add-ZipEntryText -Zip $zip -EntryName "META-INF/MANIFEST.MF" -Text (Get-ManifestText)
        Add-ZipEntryText -Zip $zip -EntryName "LICENSE.md" -Text $fixtureLicenseText
        Add-ZipEntryText -Zip $zip -EntryName "README.md" -Text $fixtureReadmeText
        Add-ZipEntryText -Zip $zip -EntryName "CHANGELOG.md" -Text $fixtureChangelogText
        Add-ZipEntryBytes -Zip $zip -EntryName "assets/appliedpackaging/logo.png" -Bytes $pngBytes
        Add-ZipEntryText -Zip $zip -EntryName "assets/appliedpackaging/lang/en_us.json" -Text $fixtureEnLangText
        Add-ZipEntryText -Zip $zip -EntryName "assets/appliedpackaging/lang/zh_cn.json" -Text $fixtureZhLangText
        Add-ZipEntryText -Zip $zip -EntryName "assets/appliedpackaging/models/item/release_audit_fixture.json" -Text $fixtureModelText
        Add-ZipEntryBytes -Zip $zip -EntryName "assets/appliedpackaging/textures/item/release_audit_fixture.png" -Bytes $pngBytes
    } finally {
        $zip.Dispose()
    }

    return @{
        RootPath = $caseRoot
        JarPath = $jarPath
    }
}

function Invoke-ReleaseAuditCase {
    param(
        [string] $Name,
        [string] $RootPath,
        [string] $JarPath,
        [int] $ExpectedExitCode,
        [string] $ExpectedText = "",
        [switch] $RequireLog
    )

    $arguments = @(
        "-NoProfile",
        "-ExecutionPolicy", "Bypass",
        "-File", $verifyReleaseScript,
        "-RootPath", $RootPath,
        "-JarPath", $JarPath
    )
    if ($RequireLog) {
        $arguments += "-RequireLog"
    }
    $output = & pwsh @arguments 2>&1 | Out-String
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

    $validFixture = New-ReleaseAuditFixture "valid"
    Invoke-ReleaseAuditCase `
        -Name "valid release audit fixture" `
        -RootPath $validFixture.RootPath `
        -JarPath $validFixture.JarPath `
        -ExpectedExitCode 0

    $optionalCompatLogFixture = New-ReleaseAuditFixture "known-optional-compat-log"
    Write-Utf8File -Path (Join-Path $optionalCompatLogFixture.RootPath "run/logs/latest.log") -Text @"
[Render thread/WARN] [mixin/]: Error loading class: com/simibubi/create/foundation/ponder/PonderWorld (java.lang.ClassNotFoundException: com.simibubi.create.foundation.ponder.PonderWorld)
[main/WARN] [mixin/]: Error loading class: xaero/map/gui/GuiMap (java.lang.ClassNotFoundException: xaero.map.gui.GuiMap)
[modloading-worker-0/WARN] [mixin/]: Error loading class: org/embeddedt/modernfix/api/entrypoint/ModernFixClientIntegration (java.lang.ClassNotFoundException: org.embeddedt.modernfix.api.entrypoint.ModernFixClientIntegration)
[Worker-Main-1/INFO] [com.warmthdawn.appliedpackaging.AppliedPackaging/]: Applied Packaging initialized.
"@
    Invoke-ReleaseAuditCase `
        -Name "known third-party optional integration warnings fixture" `
        -RootPath $optionalCompatLogFixture.RootPath `
        -JarPath $optionalCompatLogFixture.JarPath `
        -ExpectedExitCode 0 `
        -ExpectedText "Ignored 3 known third-party optional-integration class warning(s)" `
        -RequireLog

    $unknownMissingClassLogFixture = New-ReleaseAuditFixture "unknown-missing-class-log"
    Write-Utf8File -Path (Join-Path $unknownMissingClassLogFixture.RootPath "run/logs/latest.log") -Text @"
[Render thread/ERROR] [appliedpackaging/]: Error loading class: com/warmthdawn/appliedpackaging/MissingIntegration (java.lang.ClassNotFoundException: com.warmthdawn.appliedpackaging.MissingIntegration)
"@
    Invoke-ReleaseAuditCase `
        -Name "unknown missing class log fixture" `
        -RootPath $unknownMissingClassLogFixture.RootPath `
        -JarPath $unknownMissingClassLogFixture.JarPath `
        -ExpectedExitCode 1 `
        -ExpectedText "Log contains release-blocking diagnostic keywords" `
        -RequireLog

    $missingReadmeFixture = New-ReleaseAuditFixture "missing-readme"
    Remove-ZipEntry -ZipPath $missingReadmeFixture.JarPath -EntryName "README.md"
    Invoke-ReleaseAuditCase `
        -Name "missing jar README fixture" `
        -RootPath $missingReadmeFixture.RootPath `
        -JarPath $missingReadmeFixture.JarPath `
        -ExpectedExitCode 1 `
        -ExpectedText "Jar contains README.md"

    $staleReadmeFixture = New-ReleaseAuditFixture "stale-readme"
    Update-ZipEntryText -ZipPath $staleReadmeFixture.JarPath -EntryName "README.md" -Text "Stale release audit fixture readme.`n"
    Invoke-ReleaseAuditCase `
        -Name "stale jar README fixture" `
        -RootPath $staleReadmeFixture.RootPath `
        -JarPath $staleReadmeFixture.JarPath `
        -ExpectedExitCode 1 `
        -ExpectedText "Jar README.md matches repository README.md"

    $badMetadataFixture = New-ReleaseAuditFixture "bad-metadata"
    $badMetadataToml = [regex]::Replace((Get-ModsTomlText), 'modId="appliedpackaging"', 'modId="wrong_mod_id"', 1)
    Update-ZipEntryText -ZipPath $badMetadataFixture.JarPath -EntryName "META-INF/mods.toml" -Text $badMetadataToml
    Invoke-ReleaseAuditCase `
        -Name "tampered mods.toml metadata fixture" `
        -RootPath $badMetadataFixture.RootPath `
        -JarPath $badMetadataFixture.JarPath `
        -ExpectedExitCode 1 `
        -ExpectedText "mods.toml modId matches gradle.properties"

    $leakedPathFixture = New-ReleaseAuditFixture "leaked-path"
    Update-ZipEntryText -ZipPath $leakedPathFixture.JarPath -EntryName "README.md" -Text "Fixture leaked path E:\secret\asset-reference`n"
    Invoke-ReleaseAuditCase `
        -Name "local path leak fixture" `
        -RootPath $leakedPathFixture.RootPath `
        -JarPath $leakedPathFixture.JarPath `
        -ExpectedExitCode 1 `
        -ExpectedText "Jar text resources leak local/reference paths"

    $langPlaceholderFixture = New-ReleaseAuditFixture "lang-placeholder"
    Write-Utf8File -Path (Join-Path $langPlaceholderFixture.RootPath "src/main/resources/assets/appliedpackaging/lang/zh_cn.json") -Text @"
{
  "item.appliedpackaging.release_audit_fixture": "Release Audit Fixture",
  "tooltip.appliedpackaging.release_audit_fixture": "Fixture"
}
"@
    Invoke-ReleaseAuditCase `
        -Name "language placeholder mismatch fixture" `
        -RootPath $langPlaceholderFixture.RootPath `
        -JarPath $langPlaceholderFixture.JarPath `
        -ExpectedExitCode 1 `
        -ExpectedText "Language placeholder mismatches"

    $staleLangFixture = New-ReleaseAuditFixture "stale-lang"
    Update-ZipEntryText -ZipPath $staleLangFixture.JarPath -EntryName "assets/appliedpackaging/lang/en_us.json" -Text @"
{
  "item.appliedpackaging.release_audit_fixture": "Stale Release Audit Fixture",
  "tooltip.appliedpackaging.release_audit_fixture": "Fixture: %s"
}
"@
    Invoke-ReleaseAuditCase `
        -Name "stale jar language fixture" `
        -RootPath $staleLangFixture.RootPath `
        -JarPath $staleLangFixture.JarPath `
        -ExpectedExitCode 1 `
        -ExpectedText "Jar en_us.json matches source en_us.json"

    $missingResourceFixture = New-ReleaseAuditFixture "missing-resource"
    Remove-ZipEntry -ZipPath $missingResourceFixture.JarPath -EntryName "assets/appliedpackaging/models/item/release_audit_fixture.json"
    Invoke-ReleaseAuditCase `
        -Name "missing jar release resource fixture" `
        -RootPath $missingResourceFixture.RootPath `
        -JarPath $missingResourceFixture.JarPath `
        -ExpectedExitCode 1 `
        -ExpectedText "Missing jar release resources"

    $staleResourceFixture = New-ReleaseAuditFixture "stale-resource"
    Update-ZipEntryText -ZipPath $staleResourceFixture.JarPath -EntryName "assets/appliedpackaging/models/item/release_audit_fixture.json" -Text @"
{
  "parent": "minecraft:item/generated",
  "textures": {
    "layer0": "appliedpackaging:item/stale_release_audit_fixture"
  }
}
"@
    Invoke-ReleaseAuditCase `
        -Name "stale jar release resource fixture" `
        -RootPath $staleResourceFixture.RootPath `
        -JarPath $staleResourceFixture.JarPath `
        -ExpectedExitCode 1 `
        -ExpectedText "Stale jar release resources"

    $localPatternRecipeFixture = New-ReleaseAuditFixture "local-pattern-recipe"
    Write-Utf8File -Path (Join-Path $localPatternRecipeFixture.RootPath "src/main/resources/data/appliedpackaging/recipes/local_package_pattern.json") -Text @"
{
  "type": "minecraft:crafting_shapeless",
  "ingredients": [
    {
      "item": "ae2:blank_pattern"
    }
  ],
  "result": {
    "item": "appliedpackaging:package_pattern"
  }
}
"@
    Invoke-ReleaseAuditCase `
        -Name "local pattern recipe output fixture" `
        -RootPath $localPatternRecipeFixture.RootPath `
        -JarPath $localPatternRecipeFixture.JarPath `
        -ExpectedExitCode 1 `
        -ExpectedText "Encoded local pattern items are recipe outputs"

    $creativeLocalPatternFixture = New-ReleaseAuditFixture "creative-local-pattern"
    Write-Utf8File -Path (Join-Path $creativeLocalPatternFixture.RootPath "src/main/java/com/warmthdawn/appliedpackaging/registry/APCreativeTabs.java") -Text @"
package com.warmthdawn.appliedpackaging.registry;

public final class APCreativeTabs {
    public static void build(FixtureOutput output) {
        output.accept(APItems.PACKAGE_STORAGE_BUS.get());
        output.accept(APItems.PACKAGE_UNPACKING_BUS.get());
        output.accept(APItems.ADVANCED_PATTERN_ENCODING_TERMINAL.get());
        output.accept(APItems.ADVANCED_PROCESSING_PATTERN.get());
    }
}
"@
    Invoke-ReleaseAuditCase `
        -Name "creative local pattern fixture" `
        -RootPath $creativeLocalPatternFixture.RootPath `
        -JarPath $creativeLocalPatternFixture.JarPath `
        -ExpectedExitCode 1 `
        -ExpectedText "Creative tab exposes encoded local pattern items"

    $terminalBlockItemFixture = New-ReleaseAuditFixture "terminal-blockitem"
    Write-Utf8File -Path (Join-Path $terminalBlockItemFixture.RootPath "src/main/java/com/warmthdawn/appliedpackaging/registry/APItems.java") -Text @"
package com.warmthdawn.appliedpackaging.registry;

import appeng.items.parts.PartItem;
import net.minecraft.world.item.Item;

public final class APItems {
    public static final RegistryObject<Item> ADVANCED_PROCESSING_PATTERN = ITEMS.register(
            "advanced_processing_pattern",
            () -> new AdvancedProcessingPatternItem(new Item.Properties()));

    public static final RegistryObject<Item> PACKAGE_STORAGE_BUS = ITEMS.register(
            "package_storage_bus",
            () -> new PartItem<>(new Item.Properties(), PackageStorageBusPart.class, PackageStorageBusPart::new));

    public static final RegistryObject<Item> PACKAGE_UNPACKING_BUS = ITEMS.register(
            "package_unpacking_bus",
            () -> new PartItem<>(new Item.Properties(), PackageUnpackingBusPart.class, PackageUnpackingBusPart::new));

    public static final RegistryObject<Item> PACKAGE_PATTERN_TERMINAL = ITEMS.register(
            "package_pattern_terminal",
            () -> new Item(new Item.Properties()));

    public static final RegistryObject<Item> ADVANCED_PATTERN_ENCODING_TERMINAL = ITEMS.register(
            "advanced_pattern_encoding_terminal",
            () -> new PartItem<>(new Item.Properties(), AdvancedPatternEncodingTerminalPart.class, AdvancedPatternEncodingTerminalPart::new));
}
"@
    Invoke-ReleaseAuditCase `
        -Name "terminal block item fixture" `
        -RootPath $terminalBlockItemFixture.RootPath `
        -JarPath $terminalBlockItemFixture.JarPath `
        -ExpectedExitCode 1 `
        -ExpectedText "Canceled package pattern terminal and export bus items are not registered"

    $removedPatternItemFixture = New-ReleaseAuditFixture "removed-pattern-item-registration"
    Add-Content -LiteralPath (Join-Path $removedPatternItemFixture.RootPath "src/main/java/com/warmthdawn/appliedpackaging/registry/APItems.java") -Value @"

// Deliberate negative fixture.
public static final RegistryObject<Item> PACKAGED_PROCESSING_PATTERN = ITEMS.register(
        "packaged_processing_pattern",
        () -> new Item(new Item.Properties()));
"@
    Invoke-ReleaseAuditCase `
        -Name "removed packaged processing pattern registration fixture" `
        -RootPath $removedPatternItemFixture.RootPath `
        -JarPath $removedPatternItemFixture.JarPath `
        -ExpectedExitCode 1 `
        -ExpectedText "Removed packaged processing pattern item is not registered"

    $legacyBlockFixture = New-ReleaseAuditFixture "legacy-block-registration"
    Write-Utf8File -Path (Join-Path $legacyBlockFixture.RootPath "src/main/java/com/warmthdawn/appliedpackaging/registry/APBlocks.java") -Text @"
package com.warmthdawn.appliedpackaging.registry;

public final class APBlocks {
    public static final RegistryObject<Block> ME_PACKAGER = BLOCKS.register("me_packager", FixtureBlock::new);
    public static final RegistryObject<Block> PACKAGE_STORAGE_BUS = BLOCKS.register(
            "package_storage_bus",
            FixtureBlock::new);
}
"@
    Invoke-ReleaseAuditCase `
        -Name "legacy standalone block registration fixture" `
        -RootPath $legacyBlockFixture.RootPath `
        -JarPath $legacyBlockFixture.JarPath `
        -ExpectedExitCode 1 `
        -ExpectedText "Canceled standalone package bus and package pattern terminal blocks are not registered"

    $stalePartLootFixture = New-ReleaseAuditFixture "stale-part-block-loot"
    Write-Utf8File `
        -Path (Join-Path $stalePartLootFixture.RootPath "src/main/resources/data/appliedpackaging/loot_tables/blocks/package_storage_bus.json") `
        -Text '{"type":"minecraft:block","pools":[]}'
    Invoke-ReleaseAuditCase `
        -Name "stale part block loot fixture" `
        -RootPath $stalePartLootFixture.RootPath `
        -JarPath $stalePartLootFixture.JarPath `
        -ExpectedExitCode 1 `
        -ExpectedText "AE2 part item has no stale standalone block loot table: package_storage_bus"

    $advancedTerminalBlockItemFixture = New-ReleaseAuditFixture "advanced-terminal-blockitem"
    Write-Utf8File -Path (Join-Path $advancedTerminalBlockItemFixture.RootPath "src/main/java/com/warmthdawn/appliedpackaging/registry/APItems.java") -Text @"
package com.warmthdawn.appliedpackaging.registry;

import appeng.items.parts.PartItem;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;

public final class APItems {
    public static final RegistryObject<Item> ADVANCED_PROCESSING_PATTERN = ITEMS.register(
            "advanced_processing_pattern",
            () -> new AdvancedProcessingPatternItem(new Item.Properties()));

    public static final RegistryObject<Item> PACKAGE_STORAGE_BUS = ITEMS.register(
            "package_storage_bus",
            () -> new PartItem<>(new Item.Properties(), PackageStorageBusPart.class, PackageStorageBusPart::new));

    public static final RegistryObject<Item> PACKAGE_UNPACKING_BUS = ITEMS.register(
            "package_unpacking_bus",
            () -> new PartItem<>(new Item.Properties(), PackageUnpackingBusPart.class, PackageUnpackingBusPart::new));

    public static final RegistryObject<Item> ADVANCED_PATTERN_ENCODING_TERMINAL = ITEMS.register(
            "advanced_pattern_encoding_terminal",
            () -> new BlockItem(APBlocks.PACKAGE_PATTERN_TERMINAL.get(), new Item.Properties()));
}
"@
    Invoke-ReleaseAuditCase `
        -Name "advanced terminal block item fixture" `
        -RootPath $advancedTerminalBlockItemFixture.RootPath `
        -JarPath $advancedTerminalBlockItemFixture.JarPath `
        -ExpectedExitCode 1 `
        -ExpectedText "Advanced pattern encoding terminal item registers as an AE2 PartItem"

    Write-Host ""
    Write-Host "Release audit self-test passed." -ForegroundColor Green
} finally {
    if (Test-Path -LiteralPath $tempRoot) {
        $resolvedTempRoot = (Resolve-Path -LiteralPath $tempRoot).Path
        $resolvedTempParent = (Resolve-Path -LiteralPath ([System.IO.Path]::GetTempPath())).Path
        $tempLeaf = Split-Path -Leaf $resolvedTempRoot
        if (-not $resolvedTempRoot.StartsWith($resolvedTempParent, [System.StringComparison]::OrdinalIgnoreCase) -or
            -not $tempLeaf.StartsWith("appliedpackaging-release-audit-", [System.StringComparison]::Ordinal)) {
            throw "Refusing to remove unexpected release audit self-test path: $resolvedTempRoot"
        }

        Remove-Item -LiteralPath $resolvedTempRoot -Recurse -Force
    }
}
