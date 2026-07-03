param(
    [string] $RootPath = ""
)

$ErrorActionPreference = "Stop"

$repoRoot = if ([string]::IsNullOrWhiteSpace($RootPath)) {
    (Resolve-Path (Join-Path $PSScriptRoot "..")).Path
} else {
    (Resolve-Path -LiteralPath $RootPath).Path
}
Set-Location $repoRoot

$failures = [System.Collections.Generic.List[string]]::new()

function Add-Pass {
    param([string] $Message)
    Write-Host "[PASS] $Message" -ForegroundColor Green
}

function Add-Fail {
    param([string] $Message)
    $script:failures.Add($Message) | Out-Null
    Write-Host "[FAIL] $Message" -ForegroundColor Red
}

function Assert-True {
    param(
        [bool] $Condition,
        [string] $Message
    )

    if ($Condition) {
        Add-Pass $Message
    } else {
        Add-Fail $Message
    }
}

function Get-PngInfo {
    param([string] $Path)

    $bytes = [System.IO.File]::ReadAllBytes($Path)
    $signature = [byte[]]@(137, 80, 78, 71, 13, 10, 26, 10)
    $validSignature = $bytes.Length -ge 29
    if ($validSignature) {
        for ($i = 0; $i -lt $signature.Length; $i++) {
            if ($bytes[$i] -ne $signature[$i]) {
                $validSignature = $false
                break
            }
        }
    }

    $validHeader = $false
    if ($validSignature) {
        $chunkName = [System.Text.Encoding]::ASCII.GetString($bytes, 12, 4)
        $validHeader = $chunkName -eq "IHDR"
    }

    if (-not $validSignature -or -not $validHeader) {
        return @{
            Valid = $false
            Width = 0
            Height = 0
            ColorType = -1
        }
    }

    return @{
        Valid = $true
        Width = [System.Net.IPAddress]::NetworkToHostOrder([BitConverter]::ToInt32($bytes, 16))
        Height = [System.Net.IPAddress]::NetworkToHostOrder([BitConverter]::ToInt32($bytes, 20))
        ColorType = [int] $bytes[25]
    }
}

function Get-ExpectedPngSize {
    param([string] $RelativePath)

    if ($RelativePath -eq "src/main/resources/assets/appliedpackaging/logo.png") {
        return @{ Width = 128; Height = 128; Label = "root logo" }
    }

    if ($RelativePath -eq "src/main/resources/assets/appliedpackaging/textures/gui/logo.png") {
        return @{ Width = 128; Height = 128; Label = "gui logo" }
    }

    if ($RelativePath.StartsWith("src/main/resources/assets/appliedpackaging/textures/item/", [System.StringComparison]::Ordinal)) {
        return @{ Width = 32; Height = 32; Label = "item texture" }
    }

    if ($RelativePath.StartsWith("src/main/resources/assets/appliedpackaging/textures/block/", [System.StringComparison]::Ordinal)) {
        return @{ Width = 32; Height = 32; Label = "block texture" }
    }

    if ($RelativePath.StartsWith("src/main/resources/assets/appliedpackaging/textures/part/", [System.StringComparison]::Ordinal)) {
        return @{ Width = 16; Height = 16; Label = "AE2 part texture" }
    }

    if ($RelativePath.StartsWith("src/main/resources/assets/appliedpackaging/textures/gui/icons/", [System.StringComparison]::Ordinal)) {
        return @{ Width = 16; Height = 16; Label = "GUI icon" }
    }

    if ($RelativePath.StartsWith("src/main/resources/assets/appliedpackaging/textures/gui/", [System.StringComparison]::Ordinal)) {
        return @{ Width = 128; Height = 128; Label = "GUI texture" }
    }

    return $null
}

Write-Host "Applied Packaging asset resource audit" -ForegroundColor Cyan
Write-Host "Repository: $repoRoot"

$assetRoot = "src/main/resources/assets/appliedpackaging"
Assert-True (Test-Path -LiteralPath $assetRoot) "Asset root exists: $assetRoot"

$requiredPngPaths = @(
    "src/main/resources/assets/appliedpackaging/logo.png",
    "src/main/resources/assets/appliedpackaging/textures/gui/logo.png",
    "src/main/resources/assets/appliedpackaging/textures/gui/icons/auto_export.png",
    "src/main/resources/assets/appliedpackaging/textures/gui/icons/blocking_mode.png",
    "src/main/resources/assets/appliedpackaging/textures/gui/icons/capacity.png",
    "src/main/resources/assets/appliedpackaging/textures/gui/icons/color_select.png",
    "src/main/resources/assets/appliedpackaging/textures/gui/icons/marker_clear.png",
    "src/main/resources/assets/appliedpackaging/textures/gui/icons/marker_override.png",
    "src/main/resources/assets/appliedpackaging/textures/gui/icons/marker_retain.png",
    "src/main/resources/assets/appliedpackaging/textures/gui/icons/marker.png",
    "src/main/resources/assets/appliedpackaging/textures/gui/icons/pack_once.png",
    "src/main/resources/assets/appliedpackaging/textures/gui/icons/package_filter.png",
    "src/main/resources/assets/appliedpackaging/textures/gui/icons/status_blocked.png",
    "src/main/resources/assets/appliedpackaging/textures/gui/icons/status_error.png",
    "src/main/resources/assets/appliedpackaging/textures/gui/icons/status_ready.png",
    "src/main/resources/assets/appliedpackaging/textures/gui/icons/unpack_filter.png",
    "src/main/resources/assets/appliedpackaging/textures/item/black_package.png",
    "src/main/resources/assets/appliedpackaging/textures/item/blue_package.png",
    "src/main/resources/assets/appliedpackaging/textures/item/brown_package.png",
    "src/main/resources/assets/appliedpackaging/textures/item/cyan_package.png",
    "src/main/resources/assets/appliedpackaging/textures/item/fluix_package.png",
    "src/main/resources/assets/appliedpackaging/textures/item/gray_package.png",
    "src/main/resources/assets/appliedpackaging/textures/item/green_package.png",
    "src/main/resources/assets/appliedpackaging/textures/item/light_blue_package.png",
    "src/main/resources/assets/appliedpackaging/textures/item/light_gray_package.png",
    "src/main/resources/assets/appliedpackaging/textures/item/lime_package.png",
    "src/main/resources/assets/appliedpackaging/textures/item/magenta_package.png",
    "src/main/resources/assets/appliedpackaging/textures/item/orange_package.png",
    "src/main/resources/assets/appliedpackaging/textures/item/package_pattern.png",
    "src/main/resources/assets/appliedpackaging/textures/item/packaged_processing_pattern.png",
    "src/main/resources/assets/appliedpackaging/textures/item/pink_package.png",
    "src/main/resources/assets/appliedpackaging/textures/item/purple_package.png",
    "src/main/resources/assets/appliedpackaging/textures/item/red_package.png",
    "src/main/resources/assets/appliedpackaging/textures/item/white_package.png",
    "src/main/resources/assets/appliedpackaging/textures/item/yellow_package.png",
    "src/main/resources/assets/appliedpackaging/textures/part/package_pattern_terminal_back.png",
    "src/main/resources/assets/appliedpackaging/textures/part/package_pattern_terminal_bright.png",
    "src/main/resources/assets/appliedpackaging/textures/part/package_pattern_terminal_colored.png",
    "src/main/resources/assets/appliedpackaging/textures/part/package_pattern_terminal_dark.png",
    "src/main/resources/assets/appliedpackaging/textures/part/package_pattern_terminal_front.png",
    "src/main/resources/assets/appliedpackaging/textures/part/package_pattern_terminal_medium.png",
    "src/main/resources/assets/appliedpackaging/textures/part/package_pattern_terminal_sides.png",
    "src/main/resources/assets/appliedpackaging/textures/part/package_pattern_terminal_sides_status.png",
    "src/main/resources/assets/appliedpackaging/textures/block/me_packager_frame.png",
    "src/main/resources/assets/appliedpackaging/textures/block/me_packager_front.png",
    "src/main/resources/assets/appliedpackaging/textures/block/me_packager_side.png",
    "src/main/resources/assets/appliedpackaging/textures/block/me_packager_top.png",
    "src/main/resources/assets/appliedpackaging/textures/block/package_assembler_frame.png",
    "src/main/resources/assets/appliedpackaging/textures/block/package_assembler_front.png",
    "src/main/resources/assets/appliedpackaging/textures/block/package_assembler_side.png",
    "src/main/resources/assets/appliedpackaging/textures/block/package_assembler_top.png",
    "src/main/resources/assets/appliedpackaging/textures/block/package_export_bus_front.png",
    "src/main/resources/assets/appliedpackaging/textures/block/package_export_bus_side.png",
    "src/main/resources/assets/appliedpackaging/textures/block/package_pattern_terminal_front.png",
    "src/main/resources/assets/appliedpackaging/textures/block/package_pattern_terminal_side.png",
    "src/main/resources/assets/appliedpackaging/textures/block/package_pattern_terminal_top.png",
    "src/main/resources/assets/appliedpackaging/textures/block/package_storage_bus_front.png",
    "src/main/resources/assets/appliedpackaging/textures/block/package_storage_bus_side.png",
    "src/main/resources/assets/appliedpackaging/textures/block/package_unpacking_bus_front.png",
    "src/main/resources/assets/appliedpackaging/textures/block/package_unpacking_bus_side.png"
)

foreach ($requiredPath in $requiredPngPaths) {
    Assert-True (Test-Path -LiteralPath $requiredPath) "Required PNG exists: $requiredPath"
}

$pngFiles = @(Get-ChildItem -LiteralPath $assetRoot -Recurse -Filter "*.png" -File -ErrorAction SilentlyContinue)
Assert-True ($pngFiles.Count -gt 0) "PNG assets are present"

$badPngs = [System.Collections.Generic.List[string]]::new()
$badDimensions = [System.Collections.Generic.List[string]]::new()
$badColorTypes = [System.Collections.Generic.List[string]]::new()
$unexpectedPngs = [System.Collections.Generic.List[string]]::new()

foreach ($file in $pngFiles) {
    $relativePath = [System.IO.Path]::GetRelativePath($repoRoot, $file.FullName).Replace("\", "/")
    $info = Get-PngInfo $file.FullName
    if (-not $info.Valid) {
        $badPngs.Add($relativePath) | Out-Null
        continue
    }

    if ($info.ColorType -ne 6) {
        $badColorTypes.Add("$relativePath colorType=$($info.ColorType)") | Out-Null
    }

    $expected = Get-ExpectedPngSize $relativePath
    if ($null -eq $expected) {
        $unexpectedPngs.Add($relativePath) | Out-Null
        continue
    }

    if ($info.Width -ne $expected.Width -or $info.Height -ne $expected.Height) {
        $badDimensions.Add("$relativePath expected $($expected.Width)x$($expected.Height) $($expected.Label), got $($info.Width)x$($info.Height)") | Out-Null
    }
}

if ($badPngs.Count -eq 0) {
    Add-Pass "$($pngFiles.Count) PNG assets have valid PNG headers"
} else {
    Add-Fail "Invalid PNG headers: $($badPngs -join ', ')"
}

if ($badColorTypes.Count -eq 0) {
    Add-Pass "$($pngFiles.Count) PNG assets use RGBA color type"
} else {
    Add-Fail "PNG assets must use RGBA color type 6: $($badColorTypes -join '; ')"
}

if ($unexpectedPngs.Count -eq 0) {
    Add-Pass "All PNG assets are in known release asset directories"
} else {
    Add-Fail "Unexpected PNG asset paths: $($unexpectedPngs -join ', ')"
}

if ($badDimensions.Count -eq 0) {
    Add-Pass "PNG asset dimensions match asset specification"
} else {
    Add-Fail "PNG asset dimension mismatches: $($badDimensions -join '; ')"
}

if ($failures.Count -gt 0) {
    Write-Host ""
    Write-Host "Asset resource audit failed with $($failures.Count) issue(s):" -ForegroundColor Red
    foreach ($failure in $failures) {
        Write-Host " - $failure" -ForegroundColor Red
    }
    exit 1
}

Write-Host ""
Write-Host "Asset resource audit passed." -ForegroundColor Green
