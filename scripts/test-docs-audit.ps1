param()

$ErrorActionPreference = "Stop"

$repoRoot = (Resolve-Path (Join-Path $PSScriptRoot "..")).Path
Set-Location $repoRoot

$verifyDocsScript = Join-Path $repoRoot "scripts/verify-docs.ps1"
$tempRoot = Join-Path ([System.IO.Path]::GetTempPath()) ("appliedpackaging-docs-audit-" + [System.Guid]::NewGuid().ToString("N"))

$requiredPaths = @(
    "AGENTS.md",
    "README.md",
    "CHANGELOG.md",
    "LICENSE.md",
    "docs/design.md",
    "docs/00-document-index.md",
    "docs/01-requirements.md",
    "docs/02-system-architecture.md",
    "docs/03-detailed-design.md",
    "docs/04-asset-spec.md",
    "docs/05-implementation-plan.md",
    "docs/06-verification-release.md",
    "docs/07-references.md",
    "docs/08-change-intake.md",
    "docs/chat-summary.md",
    "docs/development-log.md",
    "docs/assets/palette.md",
    "docs/assets/acceptance.md",
    "docs/assets/asset-briefs/packages.md",
    "docs/assets/asset-briefs/machines.md",
    "docs/assets/asset-briefs/terminal-and-buses.md",
    "docs/assets/asset-briefs/ui-and-icons.md",
    "docs/assets/contracts/package_items.yaml",
    "docs/assets/contracts/me_packager.yaml",
    "docs/assets/contracts/package_assembler.yaml",
    "docs/assets/contracts/terminal_and_buses.yaml",
    "docs/assets/contracts/ui_icons.yaml",
    "docs/assets/reports/packages.md",
    "docs/assets/reports/machines.md",
    "docs/assets/reports/terminal-and-buses.md",
    "docs/assets/reports/ui-and-icons.md",
    "src/main/resources/assets/appliedpackaging/ae2guide/assets/assemblies/advanced_multistep_crafting.snbt",
    "scripts/run-release-checks.ps1",
    "scripts/run-server-smoke.ps1",
    "scripts/verify-release.ps1",
    "scripts/verify-assets.ps1",
    "scripts/verify-docs.ps1",
    "scripts/verify-release-readiness.ps1",
    "scripts/test-docs-audit.ps1",
    "scripts/test-assets-audit.ps1",
    "scripts/test-release-audit.ps1",
    "scripts/test-release-self-tests.ps1",
    "scripts/test-release-readiness.ps1",
    "scripts/test-release-check-plan.ps1",
    "scripts/test-release-manifest.ps1",
    "scripts/test-release-bundle.ps1",
    "scripts/write-release-manifest.ps1",
    "scripts/verify-release-manifest.ps1",
    "scripts/write-release-bundle.ps1",
    "scripts/verify-release-bundle.ps1"
)

$designDocNames = @(
    "00-document-index.md",
    "01-requirements.md",
    "02-system-architecture.md",
    "03-detailed-design.md",
    "04-asset-spec.md",
    "05-implementation-plan.md",
    "06-verification-release.md",
    "07-references.md",
    "08-change-intake.md",
    "chat-summary.md",
    "development-log.md"
)

$indexDocNames = @(
    "01-requirements.md",
    "02-system-architecture.md",
    "03-detailed-design.md",
    "04-asset-spec.md",
    "05-implementation-plan.md",
    "06-verification-release.md",
    "07-references.md",
    "08-change-intake.md",
    "chat-summary.md",
    "development-log.md"
)

function New-DocsFixture {
    param([string] $CaseName)

    $caseRoot = Join-Path $tempRoot $CaseName
    New-Item -ItemType Directory -Force -Path $caseRoot | Out-Null

    foreach ($path in $requiredPaths) {
        $target = Join-Path $caseRoot $path
        $parent = Split-Path -Parent $target
        if (-not [string]::IsNullOrWhiteSpace($parent)) {
            New-Item -ItemType Directory -Force -Path $parent | Out-Null
        }

        Set-Content -LiteralPath $target -Value "fixture`n" -Encoding UTF8
    }

    Set-Content -LiteralPath (Join-Path $caseRoot "README.md") -Value "[design](docs/design.md)`n" -Encoding UTF8
    Set-Content -LiteralPath (Join-Path $caseRoot "docs/design.md") -Value ($designDocNames -join "`n") -Encoding UTF8
    Set-Content -LiteralPath (Join-Path $caseRoot "docs/00-document-index.md") -Value ($indexDocNames -join "`n") -Encoding UTF8

    $guideSource = Join-Path $repoRoot "src/main/resources/assets/appliedpackaging/ae2guide"
    $guideTargetParent = Join-Path $caseRoot "src/main/resources/assets/appliedpackaging"
    New-Item -ItemType Directory -Force -Path $guideTargetParent | Out-Null
    Copy-Item -LiteralPath $guideSource -Destination $guideTargetParent -Recurse -Force

    return $caseRoot
}

function Invoke-DocsCase {
    param(
        [string] $Name,
        [string] $RootPath,
        [int] $ExpectedExitCode,
        [string] $ExpectedText = ""
    )

    $output = & pwsh -NoProfile -ExecutionPolicy Bypass -File $verifyDocsScript -RootPath $RootPath 2>&1 | Out-String
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

    $validFixture = New-DocsFixture "valid"
    Invoke-DocsCase -Name "valid docs fixture" -RootPath $validFixture -ExpectedExitCode 0

    $missingRequiredFixture = New-DocsFixture "missing-required"
    Remove-Item -LiteralPath (Join-Path $missingRequiredFixture "docs/04-asset-spec.md") -Force
    Invoke-DocsCase `
        -Name "missing required path fixture" `
        -RootPath $missingRequiredFixture `
        -ExpectedExitCode 1 `
        -ExpectedText "Required path exists: docs/04-asset-spec.md"

    $brokenLinkFixture = New-DocsFixture "broken-link"
    Add-Content -LiteralPath (Join-Path $brokenLinkFixture "README.md") -Value "[missing](docs/missing.md)"
    Invoke-DocsCase `
        -Name "broken markdown link fixture" `
        -RootPath $brokenLinkFixture `
        -ExpectedExitCode 1 `
        -ExpectedText "Broken local markdown link in README.md -> docs/missing.md"

    $placeholderFixture = New-DocsFixture "placeholder"
    Add-Content -LiteralPath (Join-Path $placeholderFixture "docs/03-detailed-design.md") -Value "AE2 自动合成仍然等待 X"
    Invoke-DocsCase `
        -Name "unresolved placeholder fixture" `
        -RootPath $placeholderFixture `
        -ExpectedExitCode 1 `
        -ExpectedText "Unresolved placeholder in formal docs"

    $missingGuideTranslationFixture = New-DocsFixture "missing-guide-translation"
    Remove-Item -LiteralPath (
        Join-Path $missingGuideTranslationFixture "src/main/resources/assets/appliedpackaging/ae2guide/_zh_cn/machines/me-packager.md"
    ) -Force
    Invoke-DocsCase `
        -Name "missing GuideME translation fixture" `
        -RootPath $missingGuideTranslationFixture `
        -ExpectedExitCode 1 `
        -ExpectedText "GuideME English and zh_cn page sets match"

    $brokenGuideStructureFixture = New-DocsFixture "broken-guide-structure"
    $brokenGuidePage = Join-Path $brokenGuideStructureFixture (
        "src/main/resources/assets/appliedpackaging/ae2guide/example-setups/basic-packaging-line.md"
    )
    $brokenGuideText = Get-Content -LiteralPath $brokenGuidePage -Raw
    $brokenGuideText = $brokenGuideText.Replace(
        "../assets/assemblies/package_assembly_line.snbt",
        "../assets/assemblies/missing_setup.snbt"
    )
    Set-Content -LiteralPath $brokenGuidePage -Value $brokenGuideText -Encoding UTF8
    Invoke-DocsCase `
        -Name "broken GuideME structure fixture" `
        -RootPath $brokenGuideStructureFixture `
        -ExpectedExitCode 1 `
        -ExpectedText "Missing GuideME structure"

    $mergedOrderedInputsFixture = New-DocsFixture "merged-ordered-input-examples"
    $mergedOrderedInputsPage = Join-Path $mergedOrderedInputsFixture (
        "src/main/resources/assets/appliedpackaging/ae2guide/example-setups/ordered-machine-inputs.md"
    )
    $mergedOrderedInputsText = Get-Content -LiteralPath $mergedOrderedInputsPage -Raw
    $mergedOrderedInputsText = $mergedOrderedInputsText.Replace(
        "## Examples",
        "## Example 1: 5×5 Mechanical Crafting`n`n## Examples"
    )
    Set-Content -LiteralPath $mergedOrderedInputsPage -Value $mergedOrderedInputsText -Encoding UTF8
    Invoke-DocsCase `
        -Name "merged ordered-input examples fixture" `
        -RootPath $mergedOrderedInputsFixture `
        -ExpectedExitCode 1 `
        -ExpectedText "Ordered-input English overview does not embed the four example chapters"

    $invalidAssemblerGridFixture = New-DocsFixture "invalid-assembler-grid"
    $invalidAssemblerGrid = Join-Path $invalidAssemblerGridFixture (
        "src/main/resources/assets/appliedpackaging/ae2guide/assets/assemblies/package_assembly_line.snbt"
    )
    $invalidAssemblerGridText = Get-Content -LiteralPath $invalidAssemblerGrid -Raw
    $invalidAssemblerGridText = $invalidAssemblerGridText.Replace("size: [3, 2, 2]", "size: [4, 4, 4]")
    Set-Content -LiteralPath $invalidAssemblerGrid -Value $invalidAssemblerGridText -Encoding UTF8
    Invoke-DocsCase `
        -Name "invalid Package Assembler grid fixture" `
        -RootPath $invalidAssemblerGridFixture `
        -ExpectedExitCode 1 `
        -ExpectedText "Package Assembler scene bounds fit a 2x2x2 grid plus one cable link"

    $poweredSequenceFixture = New-DocsFixture "powered-sequence-buffer"
    $poweredSequence = Join-Path $poweredSequenceFixture (
        "src/main/resources/assets/appliedpackaging/ae2guide/assets/assemblies/sequence_line.snbt"
    )
    $poweredSequenceText = Get-Content -LiteralPath $poweredSequence -Raw
    $poweredSequenceText = $poweredSequenceText.Replace(
        'state: "ae2:cable_bus{light_level:0,waterlogged:false}"',
        'state: "ae2:energy_cell{fullness:4}"'
    )
    Set-Content -LiteralPath $poweredSequence -Value $poweredSequenceText -Encoding UTF8
    Invoke-DocsCase `
        -Name "powered Sequence Buffer fixture" `
        -RootPath $poweredSequenceFixture `
        -ExpectedExitCode 1 `
        -ExpectedText "Package Unpacking Bus Sequence Buffer scene contains no Energy Cell"

    $wrongFurnacePatternFixture = New-DocsFixture "wrong-parallel-furnace-pattern"
    $wrongFurnacePattern = Join-Path $wrongFurnacePatternFixture (
        "src/main/resources/assets/appliedpackaging/ae2guide/assets/assemblies/sequence_furnace_array.snbt"
    )
    $wrongFurnacePatternText = Get-Content -LiteralPath $wrongFurnacePattern -Raw
    $wrongFurnacePatternText = $wrongFurnacePatternText.Replace(
        '{"#": 8L, "#c": "ae2:i", id: "minecraft:raw_iron"}',
        '{"#": 1L, "#c": "ae2:i", id: "minecraft:raw_iron"}'
    )
    Set-Content -LiteralPath $wrongFurnacePattern -Value $wrongFurnacePatternText -Encoding UTF8
    Invoke-DocsCase `
        -Name "wrong parallel furnace pattern fixture" `
        -RootPath $wrongFurnacePatternFixture `
        -ExpectedExitCode 1 `
        -ExpectedText "Parallel furnace pattern input 2 is eight Raw Iron"

    $wrongFurnacePriorityFixture = New-DocsFixture "wrong-parallel-furnace-priority"
    $wrongFurnacePriority = Join-Path $wrongFurnacePriorityFixture (
        "src/main/resources/assets/appliedpackaging/ae2guide/assets/assemblies/sequence_furnace_array.snbt"
    )
    $wrongFurnacePriorityText = Get-Content -LiteralPath $wrongFurnacePriority -Raw
    $wrongFurnacePriorityText = $wrongFurnacePriorityText.Replace(
        'id: "appliedpackaging:package_unpacking_bus", priority: 1',
        'id: "appliedpackaging:package_unpacking_bus", priority: 0'
    )
    Set-Content -LiteralPath $wrongFurnacePriority -Value $wrongFurnacePriorityText -Encoding UTF8
    Invoke-DocsCase `
        -Name "wrong parallel furnace priority fixture" `
        -RootPath $wrongFurnacePriorityFixture `
        -ExpectedExitCode 1 `
        -ExpectedText "Parallel furnace branch at x=5 uses unpacking priority 1"

    $unblockedMechanicalCraftingFixture = New-DocsFixture "unblocked-mechanical-crafting"
    $unblockedMechanicalCrafting = Join-Path $unblockedMechanicalCraftingFixture (
        "src/main/resources/assets/appliedpackaging/ae2guide/assets/assemblies/sequence_mechanical_crafting_5x5.snbt"
    )
    $unblockedMechanicalCraftingText = Get-Content -LiteralPath $unblockedMechanicalCrafting -Raw
    $unblockedMechanicalCraftingText = $unblockedMechanicalCraftingText.Replace(
        'blocking_mode: 1b',
        'blocking_mode: 0b'
    )
    Set-Content -LiteralPath $unblockedMechanicalCrafting -Value $unblockedMechanicalCraftingText -Encoding UTF8
    Invoke-DocsCase `
        -Name "unblocked mechanical crafting fixture" `
        -RootPath $unblockedMechanicalCraftingFixture `
        -ExpectedExitCode 1 `
        -ExpectedText "Mechanical crafting Package Assembler enables blocking mode"

    $rearMechanicalChestFixture = New-DocsFixture "rear-mechanical-crafting-chest"
    $rearMechanicalChest = Join-Path $rearMechanicalChestFixture (
        "src/main/resources/assets/appliedpackaging/ae2guide/assets/assemblies/sequence_mechanical_crafting_5x5.snbt"
    )
    $rearMechanicalChestText = Get-Content -LiteralPath $rearMechanicalChest -Raw
    $rearMechanicalChestText = $rearMechanicalChestText.Replace(
        'pos: [3, 1, 3], state: "minecraft:trapped_chest{facing:south',
        'pos: [3, 1, 4], state: "minecraft:trapped_chest{facing:south'
    )
    Set-Content -LiteralPath $rearMechanicalChest -Value $rearMechanicalChestText -Encoding UTF8
    Invoke-DocsCase `
        -Name "rear mechanical crafting chest fixture" `
        -RootPath $rearMechanicalChestFixture `
        -ExpectedExitCode 1 `
        -ExpectedText "Mechanical crafting scene places all 25 chest stand-ins in front of the buffer members"

    $mismatchedPackagerRoutingFixture = New-DocsFixture "mismatched-packager-routing"
    $mismatchedPackagerRouting = Join-Path $mismatchedPackagerRoutingFixture (
        "src/main/resources/assets/appliedpackaging/ae2guide/assets/assemblies/packager_color_routing.snbt"
    )
    $mismatchedPackagerRoutingText = Get-Content -LiteralPath $mismatchedPackagerRouting -Raw
    $mismatchedPackagerRoutingText = $mismatchedPackagerRoutingText.Replace(
        'selected_color: "yellow"',
        'selected_color: "black"'
    )
    Set-Content -LiteralPath $mismatchedPackagerRouting -Value $mismatchedPackagerRoutingText -Encoding UTF8
    Invoke-DocsCase `
        -Name "mismatched packager routing fixture" `
        -RootPath $mismatchedPackagerRoutingFixture `
        -ExpectedExitCode 1 `
        -ExpectedText "Yellow routing branch keeps the bus and ME Packager color aligned"

    $mismatchedMultiStepFixture = New-DocsFixture "mismatched-multi-step-routing"
    $mismatchedMultiStep = Join-Path $mismatchedMultiStepFixture (
        "src/main/resources/assets/appliedpackaging/ae2guide/assets/assemblies/advanced_multistep_crafting.snbt"
    )
    $mismatchedMultiStepText = Get-Content -LiteralPath $mismatchedMultiStep -Raw
    $mismatchedMultiStepText = $mismatchedMultiStepText.Replace(
        'colors: [I; 5, 0, 0, 0, 0, 0, 0]',
        'colors: [I; 11, 0, 0, 0, 0, 0, 0]'
    )
    Set-Content -LiteralPath $mismatchedMultiStep -Value $mismatchedMultiStepText -Encoding UTF8
    Invoke-DocsCase `
        -Name "mismatched multi-step routing fixture" `
        -RootPath $mismatchedMultiStepFixture `
        -ExpectedExitCode 1 `
        -ExpectedText "Yellow multi-step storage bus matches its pattern column"

    $wrongPackagerBusFixture = New-DocsFixture "wrong-packager-bus"
    $wrongPackagerBus = Join-Path $wrongPackagerBusFixture (
        "src/main/resources/assets/appliedpackaging/ae2guide/assets/assemblies/me_packager_network.snbt"
    )
    $wrongPackagerBusText = Get-Content -LiteralPath $wrongPackagerBus -Raw
    $wrongPackagerBusText = $wrongPackagerBusText.Replace(
        'id: "ae2:storage_bus"',
        'id: "appliedpackaging:package_storage_bus"'
    )
    Set-Content -LiteralPath $wrongPackagerBus -Value $wrongPackagerBusText -Encoding UTF8
    Invoke-DocsCase `
        -Name "wrong ME Packager Storage Bus fixture" `
        -RootPath $wrongPackagerBusFixture `
        -ExpectedExitCode 1 `
        -ExpectedText "ME Packager scene uses one ordinary AE2 Storage Bus"

    $cyclicGuideNavigationFixture = New-DocsFixture "cyclic-guide-navigation"
    $cyclicGuideRootPage = Join-Path $cyclicGuideNavigationFixture (
        "src/main/resources/assets/appliedpackaging/ae2guide/_zh_cn/index.md"
    )
    $cyclicGuideText = Get-Content -LiteralPath $cyclicGuideRootPage -Raw
    $cyclicGuideText = $cyclicGuideText.Replace("navigation:`n", "navigation:`n  parent: index.md`n")
    Set-Content -LiteralPath $cyclicGuideRootPage -Value $cyclicGuideText -Encoding UTF8
    Invoke-DocsCase `
        -Name "cyclic GuideME navigation fixture" `
        -RootPath $cyclicGuideNavigationFixture `
        -ExpectedExitCode 1 `
        -ExpectedText "GuideME navigation root has no parent"

    Write-Host ""
    Write-Host "Documentation audit self-test passed." -ForegroundColor Green
} finally {
    if (Test-Path -LiteralPath $tempRoot) {
        $resolvedTempRoot = (Resolve-Path -LiteralPath $tempRoot).Path
        $resolvedTempParent = (Resolve-Path -LiteralPath ([System.IO.Path]::GetTempPath())).Path
        $tempLeaf = Split-Path -Leaf $resolvedTempRoot
        if (-not $resolvedTempRoot.StartsWith($resolvedTempParent, [System.StringComparison]::OrdinalIgnoreCase) -or
            -not $tempLeaf.StartsWith("appliedpackaging-docs-audit-", [System.StringComparison]::Ordinal)) {
            throw "Refusing to remove unexpected docs audit self-test path: $resolvedTempRoot"
        }

        Remove-Item -LiteralPath $resolvedTempRoot -Recurse -Force
    }
}
