param()

$ErrorActionPreference = "Stop"

$repoRoot = (Resolve-Path (Join-Path $PSScriptRoot "..")).Path
Set-Location $repoRoot

$verifyAssetsScript = Join-Path $repoRoot "scripts/verify-assets.ps1"
$sourceAssetsRoot = Join-Path $repoRoot "src/main/resources/assets/appliedpackaging"
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

function New-AssetsFixture {
    param([string] $CaseName)

    $caseRoot = Join-Path $tempRoot $CaseName
    $caseAssetsRoot = Join-Path $caseRoot "src/main/resources/assets"
    New-Item -ItemType Directory -Force -Path $caseAssetsRoot | Out-Null
    Copy-Item -LiteralPath $sourceAssetsRoot -Destination $caseAssetsRoot -Recurse
    return $caseRoot
}

function Invoke-AssetsCase {
    param(
        [string] $Name,
        [string] $RootPath,
        [int] $ExpectedExitCode,
        [string] $ExpectedText = ""
    )

    $output = & pwsh -NoProfile -ExecutionPolicy Bypass -File $verifyAssetsScript -RootPath $RootPath 2>&1 | Out-String
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
    Invoke-AssetsCase -Name "valid assets fixture" -RootPath $validFixture -ExpectedExitCode 0

    $badDimensionFixture = New-AssetsFixture "bad-dimension"
    $badDimensionPath = Join-Path $badDimensionFixture "src/main/resources/assets/appliedpackaging/textures/item/fluix_package.png"
    [System.IO.File]::WriteAllBytes($badDimensionPath, $tinyPngBytes)
    Invoke-AssetsCase `
        -Name "bad item dimension fixture" `
        -RootPath $badDimensionFixture `
        -ExpectedExitCode 1 `
        -ExpectedText "expected 32x32 item texture"

    $badHeaderFixture = New-AssetsFixture "bad-header"
    $badHeaderPath = Join-Path $badHeaderFixture "src/main/resources/assets/appliedpackaging/textures/gui/icons/color_select.png"
    [System.IO.File]::WriteAllBytes($badHeaderPath, [byte[]]@(1, 2, 3, 4))
    Invoke-AssetsCase `
        -Name "bad PNG header fixture" `
        -RootPath $badHeaderFixture `
        -ExpectedExitCode 1 `
        -ExpectedText "Invalid PNG headers"

    $missingRequiredFixture = New-AssetsFixture "missing-required"
    Remove-Item -LiteralPath (Join-Path $missingRequiredFixture "src/main/resources/assets/appliedpackaging/logo.png") -Force
    Invoke-AssetsCase `
        -Name "missing required PNG fixture" `
        -RootPath $missingRequiredFixture `
        -ExpectedExitCode 1 `
        -ExpectedText "Required PNG exists: src/main/resources/assets/appliedpackaging/logo.png"

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
