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
            BitDepth = -1
            ColorType = -1
            Interlace = -1
        }
    }

    return @{
        Valid = $true
        Width = [System.Net.IPAddress]::NetworkToHostOrder([BitConverter]::ToInt32($bytes, 16))
        Height = [System.Net.IPAddress]::NetworkToHostOrder([BitConverter]::ToInt32($bytes, 20))
        BitDepth = [int] $bytes[24]
        ColorType = [int] $bytes[25]
        Interlace = [int] $bytes[28]
    }
}

function Get-PaethPredictor {
    param(
        [int] $Left,
        [int] $Up,
        [int] $UpperLeft
    )

    $estimate = $Left + $Up - $UpperLeft
    $leftDistance = [Math]::Abs($estimate - $Left)
    $upDistance = [Math]::Abs($estimate - $Up)
    $upperLeftDistance = [Math]::Abs($estimate - $UpperLeft)

    if ($leftDistance -le $upDistance -and $leftDistance -le $upperLeftDistance) {
        return $Left
    }
    if ($upDistance -le $upperLeftDistance) {
        return $Up
    }
    return $UpperLeft
}

function Get-PngVisualStats {
    param(
        [string] $Path,
        [hashtable] $Info
    )

    if ($Info.BitDepth -ne 8 -or $Info.ColorType -ne 6 -or $Info.Interlace -ne 0) {
        return @{
            Valid = $false
            Error = "unsupported PNG pixel layout"
            VisiblePixels = 0
            UniquePixels = 0
        }
    }

    $bytes = [System.IO.File]::ReadAllBytes($Path)
    $idatBytes = [System.IO.MemoryStream]::new()
    try {
        $position = 8
        while ($position + 8 -le $bytes.Length) {
            $length = [System.Net.IPAddress]::NetworkToHostOrder([BitConverter]::ToInt32($bytes, $position))
            $chunkType = [System.Text.Encoding]::ASCII.GetString($bytes, $position + 4, 4)
            $chunkStart = $position + 8
            if ($chunkStart + $length + 4 -gt $bytes.Length) {
                return @{
                    Valid = $false
                    Error = "truncated PNG chunk"
                    VisiblePixels = 0
                    UniquePixels = 0
                }
            }

            if ($chunkType -eq "IDAT") {
                $idatBytes.Write($bytes, $chunkStart, $length)
            } elseif ($chunkType -eq "IEND") {
                break
            }

            $position = $chunkStart + $length + 4
        }

        $compressed = [System.IO.MemoryStream]::new($idatBytes.ToArray())
        $decompressed = [System.IO.MemoryStream]::new()
        try {
            $zlib = [System.IO.Compression.ZLibStream]::new($compressed, [System.IO.Compression.CompressionMode]::Decompress)
            try {
                $zlib.CopyTo($decompressed)
            } finally {
                $zlib.Dispose()
            }
        } catch {
            return @{
                Valid = $false
                Error = "could not decompress PNG IDAT"
                VisiblePixels = 0
                UniquePixels = 0
            }
        } finally {
            $compressed.Dispose()
        }

        $raw = $decompressed.ToArray()
        $bytesPerPixel = 4
        $stride = $Info.Width * $bytesPerPixel
        $expectedLength = ($stride + 1) * $Info.Height
        if ($raw.Length -ne $expectedLength) {
            return @{
                Valid = $false
                Error = "unexpected PNG scanline length"
                VisiblePixels = 0
                UniquePixels = 0
            }
        }

        $previous = [byte[]]::new($stride)
        $uniquePixels = [System.Collections.Generic.HashSet[string]]::new()
        $visiblePixels = 0
        $offset = 0
        for ($y = 0; $y -lt $Info.Height; $y++) {
            $filter = [int] $raw[$offset]
            $offset++
            $scanline = [byte[]]::new($stride)
            [Array]::Copy($raw, $offset, $scanline, 0, $stride)
            $offset += $stride

            for ($i = 0; $i -lt $stride; $i++) {
                $left = if ($i -ge $bytesPerPixel) { [int] $scanline[$i - $bytesPerPixel] } else { 0 }
                $up = [int] $previous[$i]
                $upperLeft = if ($i -ge $bytesPerPixel) { [int] $previous[$i - $bytesPerPixel] } else { 0 }
                $predictor = switch ($filter) {
                    0 { 0 }
                    1 { $left }
                    2 { $up }
                    3 { [Math]::Floor(($left + $up) / 2) }
                    4 { Get-PaethPredictor -Left $left -Up $up -UpperLeft $upperLeft }
                    default {
                        return @{
                            Valid = $false
                            Error = "unsupported PNG filter $filter"
                            VisiblePixels = 0
                            UniquePixels = 0
                        }
                    }
                }
                $scanline[$i] = [byte] (([int] $scanline[$i] + [int] $predictor) -band 0xFF)
            }

            for ($i = 0; $i -lt $stride; $i += $bytesPerPixel) {
                $red = [int] $scanline[$i]
                $green = [int] $scanline[$i + 1]
                $blue = [int] $scanline[$i + 2]
                $alpha = [int] $scanline[$i + 3]
                if ($alpha -ne 0) {
                    $visiblePixels++
                }
                $uniquePixels.Add("$red,$green,$blue,$alpha") | Out-Null
            }

            $previous = $scanline
        }

        return @{
            Valid = $true
            Error = ""
            VisiblePixels = $visiblePixels
            UniquePixels = $uniquePixels.Count
        }
    } finally {
        $idatBytes.Dispose()
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

    if ($RelativePath -eq "src/main/resources/assets/appliedpackaging/textures/gui/mepackager.png") {
        return @{ Width = 256; Height = 256; Label = "ME Packager GUI atlas" }
    }

    if ($RelativePath -eq "src/main/resources/assets/appliedpackaging/textures/gui/mepackageassembler.png") {
        return @{ Width = 256; Height = 256; Label = "ME Package Assembler GUI atlas" }
    }

    if ($RelativePath -eq "src/main/resources/assets/appliedpackaging/textures/gui/advanced_pattern_encoding_terminal.png") {
        return @{ Width = 256; Height = 256; Label = "advanced pattern encoding terminal GUI atlas" }
    }

    if ($RelativePath -eq "src/main/resources/assets/appliedpackaging/textures/gui/advanced_pattern_encoding_terminal_sprites.png") {
        return @{ Width = 256; Height = 256; Label = "advanced pattern encoding terminal sprite atlas" }
    }

    if ($RelativePath -eq "src/main/resources/assets/appliedpackaging/textures/gui/advanced_pattern_encoding_terminal_states.png") {
        return @{ Width = 256; Height = 256; Label = "advanced pattern encoding terminal AE2 states atlas" }
    }

    if ($RelativePath -eq "src/main/resources/assets/appliedpackaging/textures/gui/advanced_pattern_encoding_terminal_middle_row.png") {
        return @{ Width = 195; Height = 18; Label = "advanced pattern encoding terminal middle row" }
    }

    if ($RelativePath -eq "src/main/resources/assets/appliedpackaging/textures/gui/advanced_pattern_encoding_terminal_scrollbar.png") {
        return @{ Width = 256; Height = 256; Label = "advanced pattern encoding terminal AE2 scrollbar atlas" }
    }

    if ($RelativePath -eq "src/main/resources/assets/appliedpackaging/textures/gui/pattern_mode_packaging.png") {
        return @{ Width = 256; Height = 256; Label = "package pattern mode GUI atlas" }
    }

    if ($RelativePath -eq "src/main/resources/assets/appliedpackaging/textures/gui/package-storagebus.png") {
        return @{ Width = 256; Height = 256; Label = "package bus GUI atlas" }
    }

    if ($RelativePath -eq "src/main/resources/assets/appliedpackaging/textures/gui/package-storagebus-sprites.png") {
        return @{ Width = 256; Height = 256; Label = "package bus user sprite atlas" }
    }

    if ($RelativePath -eq "src/main/resources/assets/appliedpackaging/textures/gui/ae2-states.png") {
        return @{ Width = 256; Height = 256; Label = "package bus AE2 states source atlas" }
    }

    if ($RelativePath -eq "src/main/resources/assets/appliedpackaging/textures/gui/package_bus_extra_panels.png") {
        return @{ Width = 128; Height = 128; Label = "package bus current-AE2 extra panels texture" }
    }

    if ($RelativePath -eq "src/main/resources/assets/appliedpackaging/textures/gui/package_bus_vertical_buttons_bg.png") {
        return @{ Width = 21; Height = 26; Label = "package bus current-AE2 vertical button background" }
    }

    if ($RelativePath.StartsWith("src/main/resources/assets/appliedpackaging/textures/item/", [System.StringComparison]::Ordinal)) {
        return @{ Width = 32; Height = 32; Label = "item texture" }
    }

    if ($RelativePath.StartsWith("src/main/resources/assets/appliedpackaging/textures/block/package_box/", [System.StringComparison]::Ordinal)) {
        $leaf = Split-Path -Leaf $RelativePath
        if ($leaf -in @("package_box_front.png", "package_box_back.png", "package_box_side.png")) {
            return @{ Width = 10; Height = 8; Label = "package box vertical face texture" }
        }
        if ($leaf -in @("package_box_top.png", "package_box_bottom.png")) {
            return @{ Width = 10; Height = 10; Label = "package box horizontal face texture" }
        }
    }

    if ($RelativePath.StartsWith("src/main/resources/assets/appliedpackaging/textures/block/me_packager_create/", [System.StringComparison]::Ordinal)) {
        $leaf = Split-Path -Leaf $RelativePath
        if ($leaf -in @(
                "factory_panel_packager_mode.png",
                "packager_iris_closed.png",
                "packager_iris_open.png",
                "packager_particle.png",
                "vault_front_small.png")) {
            return @{ Width = 16; Height = 16; Label = "Create-style temporary packager detail texture" }
        }
        return @{ Width = 32; Height = 32; Label = "Create-style temporary packager texture" }
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

function Get-JsonFile {
    param([string] $Path)

    try {
        return Get-Content -Raw -LiteralPath $Path | ConvertFrom-Json
    } catch {
        Add-Fail "JSON asset should parse: $Path ($($_.Exception.Message))"
        return $null
    }
}

Write-Host "Applied Packaging asset resource audit" -ForegroundColor Cyan
Write-Host "Repository: $repoRoot"

$assetRoot = "src/main/resources/assets/appliedpackaging"
Assert-True (Test-Path -LiteralPath $assetRoot) "Asset root exists: $assetRoot"

$requiredPngPaths = @(
    "src/main/resources/assets/appliedpackaging/logo.png",
    "src/main/resources/assets/appliedpackaging/textures/gui/logo.png",
    "src/main/resources/assets/appliedpackaging/textures/gui/mepackager.png",
    "src/main/resources/assets/appliedpackaging/textures/gui/mepackageassembler.png",
    "src/main/resources/assets/appliedpackaging/textures/gui/advanced_pattern_encoding_terminal.png",
    "src/main/resources/assets/appliedpackaging/textures/gui/advanced_pattern_encoding_terminal_sprites.png",
    "src/main/resources/assets/appliedpackaging/textures/gui/advanced_pattern_encoding_terminal_states.png",
    "src/main/resources/assets/appliedpackaging/textures/gui/advanced_pattern_encoding_terminal_middle_row.png",
    "src/main/resources/assets/appliedpackaging/textures/gui/advanced_pattern_encoding_terminal_scrollbar.png",
    "src/main/resources/assets/appliedpackaging/textures/gui/pattern_mode_packaging.png",
    "src/main/resources/assets/appliedpackaging/textures/gui/package-storagebus.png",
    "src/main/resources/assets/appliedpackaging/textures/gui/package-storagebus-sprites.png",
    "src/main/resources/assets/appliedpackaging/textures/gui/ae2-states.png",
    "src/main/resources/assets/appliedpackaging/textures/gui/package_bus_extra_panels.png",
    "src/main/resources/assets/appliedpackaging/textures/gui/package_bus_vertical_buttons_bg.png",
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
    "src/main/resources/assets/appliedpackaging/textures/item/package_pattern.png",
    "src/main/resources/assets/appliedpackaging/textures/item/packaged_processing_pattern.png",
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

$packageColors = @(
    "fluix",
    "white",
    "orange",
    "magenta",
    "light_blue",
    "yellow",
    "lime",
    "pink",
    "gray",
    "light_gray",
    "cyan",
    "purple",
    "blue",
    "brown",
    "green",
    "red",
    "black"
)
$packageFaces = @("front", "back", "side", "top", "bottom")
foreach ($color in $packageColors) {
    foreach ($face in $packageFaces) {
        $requiredPngPaths += "src/main/resources/assets/appliedpackaging/textures/block/package_box/$color/package_box_$face.png"
    }
}

foreach ($requiredPath in $requiredPngPaths) {
    Assert-True (Test-Path -LiteralPath $requiredPath) "Required PNG exists: $requiredPath"
}

$bytePreservedPngHashes = [ordered]@{
    "src/main/resources/assets/appliedpackaging/textures/gui/package-storagebus.png" = "506BE44EF826C14C1DBE37C076EDC7955C0DBFE35A7DB9B157EABA8E241787DE"
    "src/main/resources/assets/appliedpackaging/textures/gui/package-storagebus-sprites.png" = "14D7D26A93BF46D1BA0EF33A5408197718D0AF5BD3ADE662AA8A46E8DE662281"
    "src/main/resources/assets/appliedpackaging/textures/gui/ae2-states.png" = "0996B0084C7BF37F65A97A745982AB681EBD86F142FADE526F14C823C4727E55"
    "src/main/resources/assets/appliedpackaging/textures/gui/package_bus_extra_panels.png" = "C67FED0F98C9CA67A0602B5589A5191D59D5DD2BD3848C62DE0E209E0E44B8B0"
    "src/main/resources/assets/appliedpackaging/textures/gui/package_bus_vertical_buttons_bg.png" = "62150F9869EE17CBD15BDA963542287BF798482CEED1F18F0E24DD82381F7715"
}
foreach ($entry in $bytePreservedPngHashes.GetEnumerator()) {
    if (Test-Path -LiteralPath $entry.Key) {
        $actualHash = (Get-FileHash -Algorithm SHA256 -LiteralPath $entry.Key).Hash
        Assert-True `
            ($actualHash -eq $entry.Value) `
            "Byte-preserved PNG keeps its source hash: $($entry.Key)"
    }
}

foreach ($color in $packageColors) {
    $packageBoxModelPath = "src/main/resources/assets/appliedpackaging/models/item/package_box/$color.json"
    $packageItemModelPath = "src/main/resources/assets/appliedpackaging/models/item/$($color)_package.json"

    Assert-True (Test-Path -LiteralPath $packageBoxModelPath) "Package box model exists: $packageBoxModelPath"
    Assert-True (Test-Path -LiteralPath $packageItemModelPath) "Package item model exists: $packageItemModelPath"

    if (Test-Path -LiteralPath $packageItemModelPath) {
        $itemModel = Get-JsonFile $packageItemModelPath
        if ($null -ne $itemModel) {
            Assert-True `
                ($itemModel.parent -eq "appliedpackaging:item/package_box/$color") `
                "Package item model $color references its 3D package_box parent"
            $markerOverride = @($itemModel.overrides) | Where-Object {
                $null -ne $_.predicate -and
                $_.predicate."appliedpackaging:has_marker" -eq 1.0 -and
                $_.model -eq "appliedpackaging:item/package_box/marked"
            }
            Assert-True `
                ($markerOverride.Count -eq 1) `
                "Package item model $color declares the marker custom-render override"
        }
    }

    if (-not (Test-Path -LiteralPath $packageBoxModelPath)) {
        continue
    }

    $model = Get-JsonFile $packageBoxModelPath
    if ($null -eq $model) {
        continue
    }

    Assert-True `
        ($model.parent -eq "appliedpackaging:item/package_box/_transforms") `
        "Package box model $color inherits only the shared display transforms"
    Assert-True `
        ($model.render_type -eq "minecraft:cutout_mipped") `
        "Package box model $color uses cutout_mipped render type"

    $elements = @($model.elements)
    Assert-True ($elements.Count -eq 1) "Package box model $color has one cuboid element"
    if ($elements.Count -ne 1) {
        continue
    }

    $element = $elements[0]
    Assert-True `
        ((@($element.from) -join ",") -eq "3,1,3" -and (@($element.to) -join ",") -eq "13,9,13") `
        "Package box model $color keeps the v7 10x10x8 cuboid bounds"

    $expectedFaceTextures = @{
        north = "#front"
        south = "#back"
        west = "#side"
        east = "#side"
        up = "#top"
        down = "#bottom"
    }
    foreach ($faceName in $expectedFaceTextures.Keys) {
        $face = $element.faces.$faceName
        Assert-True ($null -ne $face) "Package box model $color declares $faceName face"
        if ($null -eq $face) {
            continue
        }

        Assert-True `
            ($face.texture -eq $expectedFaceTextures[$faceName]) `
            "Package box model $color $faceName face uses $($expectedFaceTextures[$faceName])"
        Assert-True `
            ($face.PSObject.Properties.Name -contains "uv") `
            "Package box model $color $faceName face declares full-face uv"
        Assert-True `
            ((@($face.uv) -join ",") -eq "0,0,16,16") `
            "Package box model $color $faceName face uses full-face uv [0,0,16,16]"
    }
}

$opaqueSolidModelPaths = @(
    "src/main/resources/assets/appliedpackaging/models/block/package_assembler.json",
    "src/main/resources/assets/appliedpackaging/models/block/package_export_bus.json",
    "src/main/resources/assets/appliedpackaging/models/block/package_pattern_terminal.json",
    "src/main/resources/assets/appliedpackaging/models/block/package_storage_bus.json",
    "src/main/resources/assets/appliedpackaging/models/block/package_unpacking_bus.json",
    "src/main/resources/assets/appliedpackaging/models/part/package_pattern_terminal_base.json"
)

foreach ($opaqueModelPath in $opaqueSolidModelPaths) {
    Assert-True (Test-Path -LiteralPath $opaqueModelPath) "Opaque solid model exists: $opaqueModelPath"
    if (-not (Test-Path -LiteralPath $opaqueModelPath)) {
        continue
    }

    $opaqueModel = Get-JsonFile $opaqueModelPath
    if ($null -eq $opaqueModel) {
        continue
    }

    Assert-True `
        (-not ($opaqueModel.PSObject.Properties.Name -contains "render_type")) `
        "Opaque block/part model must use the default solid render type: $opaqueModelPath"
}

$pngFiles = @(Get-ChildItem -LiteralPath $assetRoot -Recurse -Filter "*.png" -File -ErrorAction SilentlyContinue)
Assert-True ($pngFiles.Count -gt 0) "PNG assets are present"

$badPngs = [System.Collections.Generic.List[string]]::new()
$badDimensions = [System.Collections.Generic.List[string]]::new()
$badColorTypes = [System.Collections.Generic.List[string]]::new()
$badVisualContent = [System.Collections.Generic.List[string]]::new()
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

    $visualStats = Get-PngVisualStats -Path $file.FullName -Info $info
    if (-not $visualStats.Valid) {
        $badVisualContent.Add("$relativePath $($visualStats.Error)") | Out-Null
    } elseif ($visualStats.VisiblePixels -eq 0) {
        $badVisualContent.Add("$relativePath is fully transparent") | Out-Null
    } elseif ($visualStats.UniquePixels -lt 2) {
        $badVisualContent.Add("$relativePath is a single-color placeholder") | Out-Null
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

if ($badVisualContent.Count -eq 0) {
    Add-Pass "PNG assets contain visible, non-placeholder pixel content"
} else {
    Add-Fail "PNG assets must not be fully transparent or single-color placeholders: $($badVisualContent -join '; ')"
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
