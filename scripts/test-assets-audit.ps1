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
$transparent32PngBytes = [Convert]::FromBase64String("iVBORw0KGgoAAAANSUhEUgAAACAAAAAgCAYAAABzenr0AAAAGklEQVR42u3BAQEAAACCIP+vbkhAAQAAAO8GECAAAcm1w7EAAAAASUVORK5CYII=")
$solid32PngBytes = [Convert]::FromBase64String("iVBORw0KGgoAAAANSUhEUgAAACAAAAAgCAYAAABzenr0AAAAMElEQVR42u3OIQEAAAgDMAKQghT0LwYxbibmVz17SSUgICAgICAgICAgICAgIJAOPBSKlEzX+oz1AAAAAElFTkSuQmCC")

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
    $badDimensionPath = Join-Path $badDimensionFixture "src/main/resources/assets/appliedpackaging/textures/item/package_pattern.png"
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

    $transparentFixture = New-AssetsFixture "transparent-png"
    $transparentPath = Join-Path $transparentFixture "src/main/resources/assets/appliedpackaging/textures/item/package_pattern.png"
    [System.IO.File]::WriteAllBytes($transparentPath, $transparent32PngBytes)
    Invoke-AssetsCase `
        -Name "transparent PNG fixture" `
        -RootPath $transparentFixture `
        -ExpectedExitCode 1 `
        -ExpectedText "fully transparent"

    $solidFixture = New-AssetsFixture "solid-png"
    $solidPath = Join-Path $solidFixture "src/main/resources/assets/appliedpackaging/textures/item/package_pattern.png"
    [System.IO.File]::WriteAllBytes($solidPath, $solid32PngBytes)
    Invoke-AssetsCase `
        -Name "single-color PNG fixture" `
        -RootPath $solidFixture `
        -ExpectedExitCode 1 `
        -ExpectedText "single-color placeholder"

    $missingRequiredFixture = New-AssetsFixture "missing-required"
    Remove-Item -LiteralPath (Join-Path $missingRequiredFixture "src/main/resources/assets/appliedpackaging/logo.png") -Force
    Invoke-AssetsCase `
        -Name "missing required PNG fixture" `
        -RootPath $missingRequiredFixture `
        -ExpectedExitCode 1 `
        -ExpectedText "Required PNG exists: src/main/resources/assets/appliedpackaging/logo.png"

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

    $badOpaqueModelFixture = New-AssetsFixture "bad-opaque-model-render-type"
    $badOpaqueModelPath = Join-Path $badOpaqueModelFixture "src/main/resources/assets/appliedpackaging/models/block/package_assembler.json"
    $badOpaqueModel = Get-Content -Raw -LiteralPath $badOpaqueModelPath | ConvertFrom-Json
    $badOpaqueModel | Add-Member -NotePropertyName "render_type" -NotePropertyValue "minecraft:cutout_mipped"
    $badOpaqueModel | ConvertTo-Json -Depth 20 | Set-Content -LiteralPath $badOpaqueModelPath -Encoding UTF8
    Invoke-AssetsCase `
        -Name "bad opaque model render_type fixture" `
        -RootPath $badOpaqueModelFixture `
        -ExpectedExitCode 1 `
        -ExpectedText "Opaque block/part model must use the default solid render type"

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
