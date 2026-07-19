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

    $invalidAssemblerGridFixture = New-DocsFixture "invalid-assembler-grid"
    $invalidAssemblerGrid = Join-Path $invalidAssemblerGridFixture (
        "src/main/resources/assets/appliedpackaging/ae2guide/assets/assemblies/package_assembly_line.snbt"
    )
    $invalidAssemblerGridText = Get-Content -LiteralPath $invalidAssemblerGrid -Raw
    $invalidAssemblerGridText = $invalidAssemblerGridText.Replace("size: [4, 4, 4]", "size: [4, 8, 4]")
    Set-Content -LiteralPath $invalidAssemblerGrid -Value $invalidAssemblerGridText -Encoding UTF8
    Invoke-DocsCase `
        -Name "invalid Package Assembler grid fixture" `
        -RootPath $invalidAssemblerGridFixture `
        -ExpectedExitCode 1 `
        -ExpectedText "Package Assembler grid is exactly 4x4x4"

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
