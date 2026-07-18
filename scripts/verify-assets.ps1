param(
    [string] $RootPath = "",
    [switch] $SkipPngVisualContent
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

    if ($RelativePath -eq "src/main/resources/assets/appliedpackaging/textures/gui/mepackager.png") {
        return @{ Width = 256; Height = 256; Label = "ME Packager GUI atlas" }
    }

    if ($RelativePath -eq "src/main/resources/assets/appliedpackaging/textures/gui/mepackageassembler.png") {
        return @{ Width = 256; Height = 256; Label = "ME Package Assembler GUI atlas" }
    }

    if ($RelativePath -in @(
            "src/main/resources/assets/appliedpackaging/textures/gui/sequence_buffer.png",
            "src/main/resources/assets/appliedpackaging/textures/gui/sequence_buffer_side.png")) {
        return @{ Width = 256; Height = 256; Label = "Sequence Buffer GUI atlas" }
    }

    if ($RelativePath -eq "src/main/resources/assets/appliedpackaging/textures/gui/advanced_pattern_encoding_terminal.png") {
        return @{ Width = 256; Height = 256; Label = "advanced pattern encoding terminal GUI atlas" }
    }

    if ($RelativePath -eq "src/main/resources/assets/appliedpackaging/textures/gui/advanced_pattern_encoding_terminal_sprites.png") {
        return @{ Width = 256; Height = 256; Label = "advanced pattern encoding terminal sprite atlas" }
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

    if ($RelativePath -eq "src/main/resources/assets/appliedpackaging/textures/gui/ae2-terminal.png") {
        return @{ Width = 256; Height = 256; Label = "cached current-AE2 terminal GUI" }
    }

    if ($RelativePath -eq "src/main/resources/assets/ae2/textures/guis/text_field.png") {
        return @{ Width = 128; Height = 128; Label = "cached current-AE2 text field GUI" }
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

    if ($RelativePath -in @(
            "src/main/resources/assets/appliedpackaging/textures/item/package_pattern.png",
            "src/main/resources/assets/appliedpackaging/textures/item/advanced_processing_pattern.png")) {
        return @{ Width = 16; Height = 16; Label = "user pattern item texture" }
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

    if ($RelativePath -eq "src/main/resources/assets/appliedpackaging/textures/block/me_packager/base.png") {
        return @{ Width = 64; Height = 64; Label = "ME Packager body atlas" }
    }

    if ($RelativePath -eq "src/main/resources/assets/appliedpackaging/textures/block/me_packager/curtain.png") {
        return @{ Width = 16; Height = 16; Label = "ME Packager curtain texture" }
    }

    if ($RelativePath -eq "src/main/resources/assets/appliedpackaging/textures/block/me_packager/belt_scroll.png") {
        return @{ Width = 32; Height = 32; Label = "ME Packager two-period scrolling belt texture" }
    }

    if ($RelativePath -eq "src/main/resources/assets/appliedpackaging/textures/block/package_assembler.png") {
        return @{ Width = 16; Height = 16; Label = "AE2 v19 package assembler user surface" }
    }

    if ($RelativePath -eq "src/main/resources/assets/appliedpackaging/textures/block/package_assembler_lights.png") {
        return @{ Width = 16; Height = 192; Label = "AE2 v19 package assembler animated light strip" }
    }

    if ($RelativePath.StartsWith("src/main/resources/assets/appliedpackaging/textures/block/sequence_buffer/faces/", [System.StringComparison]::Ordinal)) {
        return @{ Width = 16; Height = 16; Label = "Sequence Buffer face texture" }
    }

    if ($RelativePath.StartsWith("src/main/resources/assets/appliedpackaging/textures/block/", [System.StringComparison]::Ordinal)) {
        return @{ Width = 32; Height = 32; Label = "block texture" }
    }

    if ($RelativePath.StartsWith("src/main/resources/assets/appliedpackaging/textures/part/", [System.StringComparison]::Ordinal)) {
        return @{ Width = 16; Height = 16; Label = "AE2 part texture" }
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

$sequenceBufferFaceNames = @(
    "undirected_unformed",
    "undirected_formed_middle_side",
    "undirected_formed_edge_side",
    "controller_back",
    "directed_front_unformed",
    "directed_front_formed_middle_side",
    "directed_front_formed_edge_side",
    "formed_middle_side_edge_occluded",
    "directed_side_unformed",
    "directed_side_formed_middle_side",
    "directed_side_formed_edge_side",
    "controller_side",
    "directed_back_unformed",
    "directed_back_formed_middle_side",
    "directed_back_formed_edge_side",
    "tail_back"
)

$specializedPatternStylePath = "src/main/resources/assets/ae2/screens/appliedpackaging/advanced_pattern_encoding_terminal.json"
$forbiddenNativePatternStylePath = "src/main/resources/assets/ae2/screens/terminals/pattern_encoding_terminal.json"
$forbiddenNativeModesTexturePath = "src/main/resources/assets/appliedpackaging/textures/gui/pattern_modes.png"
Assert-True `
    (Test-Path -LiteralPath $specializedPatternStylePath) `
    "Combined specialized pattern terminal screen style exists: $specializedPatternStylePath"
Assert-True `
    (-not (Test-Path -LiteralPath $forbiddenNativePatternStylePath)) `
    "AE2 native pattern terminal ScreenStyle is not overridden"
Assert-True `
    (-not (Test-Path -LiteralPath $forbiddenNativeModesTexturePath)) `
    "AE2 native mode atlas is not copied into Applied Packaging"
if (Test-Path -LiteralPath $specializedPatternStylePath) {
    $specializedPatternStyle = Get-JsonFile $specializedPatternStylePath
    if ($null -ne $specializedPatternStyle) {
        Assert-True `
            (@($specializedPatternStyle.includes) -contains "../terminals/terminal.json") `
            "Combined specialized pattern terminal screen inherits AE2 terminal behavior"
        Assert-True `
            ($specializedPatternStyle.terminalStyle.header.texture -eq "appliedpackaging:textures/gui/advanced_pattern_encoding_terminal.png") `
            "Combined specialized pattern terminal screen uses the advanced terminal base"
        Assert-True `
            ($specializedPatternStyle.widgets.viewCells.right -eq 800) `
            "Combined specialized pattern terminal suppresses the inherited view-cell panel"
        Assert-True `
            ($specializedPatternStyle.widgets.packagePatternModeScrollbar.height -eq 52 -and
                $specializedPatternStyle.widgets.packagePatternModeScrollbar.left -eq 15 -and
                $specializedPatternStyle.widgets.packagePatternModeScrollbar.bottom -eq 164) `
            "Combined specialized pattern terminal declares the package-mode scrollbar geometry"
        Assert-True `
            ((@($specializedPatternStyle.terminalStyle.bottom.srcRect) -join ",") -eq "0,53,195,192") `
            "Combined specialized pattern terminal uses the shared 195x245 two-row frame"
        Assert-True `
            ($specializedPatternStyle.slots.PROCESSING_INPUTS.left -eq 21 -and
                $specializedPatternStyle.slots.PROCESSING_INPUTS.bottom -eq 164 -and
                $specializedPatternStyle.slots.PROCESSING_OUTPUTS.left -eq 119 -and
                $specializedPatternStyle.slots.PROCESSING_OUTPUTS.bottom -eq 164) `
            "Combined specialized pattern terminal declares the revised advanced editor slots"
        Assert-True `
            ($specializedPatternStyle.slots.BLANK_PATTERN.left -eq 150 -and
                $specializedPatternStyle.slots.BLANK_PATTERN.bottom -eq 165 -and
                $specializedPatternStyle.slots.ENCODED_PATTERN.left -eq 150 -and
                $specializedPatternStyle.slots.ENCODED_PATTERN.bottom -eq 118 -and
                $specializedPatternStyle.widgets.encodePattern.left -eq 150 -and
                $specializedPatternStyle.widgets.encodePattern.bottom -eq 145) `
            "Combined specialized pattern terminal aligns both carrier slots and Encode to the revised base"
        Assert-True `
            ($specializedPatternStyle.widgets.processingCycleOutput.left -eq 106 -and
                $specializedPatternStyle.widgets.processingCycleOutput.bottom -eq 174) `
            "Combined specialized pattern terminal keeps advanced header controls above the input-grid border"
    }
}

$requiredPngPaths = @(
    "src/main/resources/assets/appliedpackaging/logo.png",
    "src/main/resources/assets/appliedpackaging/textures/gui/mepackager.png",
    "src/main/resources/assets/appliedpackaging/textures/gui/mepackageassembler.png",
    "src/main/resources/assets/appliedpackaging/textures/gui/advanced_pattern_encoding_terminal.png",
    "src/main/resources/assets/appliedpackaging/textures/gui/advanced_pattern_encoding_terminal_sprites.png",
    "src/main/resources/assets/appliedpackaging/textures/gui/advanced_pattern_encoding_terminal_middle_row.png",
    "src/main/resources/assets/appliedpackaging/textures/gui/advanced_pattern_encoding_terminal_scrollbar.png",
    "src/main/resources/assets/appliedpackaging/textures/gui/pattern_mode_packaging.png",
    "src/main/resources/assets/appliedpackaging/textures/gui/ae2-terminal.png",
    "src/main/resources/assets/ae2/textures/guis/text_field.png",
    "src/main/resources/assets/appliedpackaging/textures/gui/package-storagebus.png",
    "src/main/resources/assets/appliedpackaging/textures/gui/package-storagebus-sprites.png",
    "src/main/resources/assets/appliedpackaging/textures/gui/ae2-states.png",
    "src/main/resources/assets/appliedpackaging/textures/gui/package_bus_extra_panels.png",
    "src/main/resources/assets/appliedpackaging/textures/gui/package_bus_vertical_buttons_bg.png",
    "src/main/resources/assets/appliedpackaging/textures/item/package_pattern.png",
    "src/main/resources/assets/appliedpackaging/textures/item/advanced_processing_pattern.png",
    "src/main/resources/assets/appliedpackaging/textures/part/advanced_pattern_encoding_terminal_back.png",
    "src/main/resources/assets/appliedpackaging/textures/part/advanced_pattern_encoding_terminal_bright.png",
    "src/main/resources/assets/appliedpackaging/textures/part/advanced_pattern_encoding_terminal_colored.png",
    "src/main/resources/assets/appliedpackaging/textures/part/advanced_pattern_encoding_terminal_dark.png",
    "src/main/resources/assets/appliedpackaging/textures/part/advanced_pattern_encoding_terminal_front.png",
    "src/main/resources/assets/appliedpackaging/textures/part/advanced_pattern_encoding_terminal_medium.png",
    "src/main/resources/assets/appliedpackaging/textures/part/advanced_pattern_encoding_terminal_sides.png",
    "src/main/resources/assets/appliedpackaging/textures/part/advanced_pattern_encoding_terminal_sides_status.png",
    "src/main/resources/assets/appliedpackaging/textures/part/advanced_pattern_encoding_terminal_status_has_channel.png",
    "src/main/resources/assets/appliedpackaging/textures/part/advanced_pattern_encoding_terminal_status_off.png",
    "src/main/resources/assets/appliedpackaging/textures/part/advanced_pattern_encoding_terminal_status_on.png",
    "src/main/resources/assets/appliedpackaging/textures/part/package_storage_bus_front.png",
    "src/main/resources/assets/appliedpackaging/textures/part/package_storage_bus_back.png",
    "src/main/resources/assets/appliedpackaging/textures/part/package_storage_bus_sides.png",
    "src/main/resources/assets/appliedpackaging/textures/part/package_unpacking_bus_front.png",
    "src/main/resources/assets/appliedpackaging/textures/part/package_unpacking_bus_back.png",
    "src/main/resources/assets/appliedpackaging/textures/part/package_unpacking_bus_sides.png",
    "src/main/resources/assets/appliedpackaging/textures/block/me_packager/base.png",
    "src/main/resources/assets/appliedpackaging/textures/block/me_packager/curtain.png",
    "src/main/resources/assets/appliedpackaging/textures/block/me_packager/belt_scroll.png",
    "src/main/resources/assets/appliedpackaging/textures/block/package_assembler.png",
    "src/main/resources/assets/appliedpackaging/textures/block/package_assembler_lights.png",
    "src/main/resources/assets/appliedpackaging/textures/gui/sequence_buffer.png",
    "src/main/resources/assets/appliedpackaging/textures/gui/sequence_buffer_side.png"
)
foreach ($sequenceBufferFaceName in $sequenceBufferFaceNames) {
    $requiredPngPaths += "src/main/resources/assets/appliedpackaging/textures/block/sequence_buffer/faces/$sequenceBufferFaceName.png"
}

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
$packageTransformsPath = "src/main/resources/assets/appliedpackaging/models/item/package_box/_transforms.json"
$markedPackageModelPath = "src/main/resources/assets/appliedpackaging/models/item/package_box/marked.json"
foreach ($packageDisplayModelPath in @($packageTransformsPath, $markedPackageModelPath)) {
    Assert-True (Test-Path -LiteralPath $packageDisplayModelPath) "Package display model exists: $packageDisplayModelPath"
    if (-not (Test-Path -LiteralPath $packageDisplayModelPath)) {
        continue
    }
    $packageDisplayModel = Get-JsonFile $packageDisplayModelPath
    if ($null -eq $packageDisplayModel) {
        continue
    }
    Assert-True `
        ((@($packageDisplayModel.display.gui.rotation) -join ",") -eq "30,135,0") `
        "Package display model keeps GUI rotation [30,135,0]: $packageDisplayModelPath"
    Assert-True `
        ((@($packageDisplayModel.display.gui.translation) -join ",") -eq "0,2,0") `
        "Package display model centers the transformed cuboid in the GUI: $packageDisplayModelPath"
    Assert-True `
        ((@($packageDisplayModel.display.gui.scale) -join ",") -eq "0.75,0.75,0.75") `
        "Package display model keeps GUI scale [0.75,0.75,0.75]: $packageDisplayModelPath"
}

foreach ($color in $packageColors) {
    foreach ($face in $packageFaces) {
        $requiredPngPaths += "src/main/resources/assets/appliedpackaging/textures/block/package_box/$color/package_box_$face.png"
    }
}

foreach ($requiredPath in $requiredPngPaths) {
    Assert-True (Test-Path -LiteralPath $requiredPath) "Required PNG exists: $requiredPath"
}

$bytePreservedPngHashes = [ordered]@{
    "src/main/resources/assets/appliedpackaging/textures/item/package_pattern.png" = "04E0C00E41C68AEA57C1B97CEF6E736F0DB2A75F768B6050BEF48C116F05E349"
    "src/main/resources/assets/appliedpackaging/textures/item/advanced_processing_pattern.png" = "084B03A92C440BC7CF675F1C30D3883BC6808F519924C224C1CB8E5D3FAE4FF9"
    "src/main/resources/assets/appliedpackaging/textures/block/me_packager/base.png" = "C98E01D32207CD77D50C1B5AEE5176FBD40264E16BADF2569737003A7DD6385E"
    "src/main/resources/assets/appliedpackaging/textures/block/me_packager/curtain.png" = "D4A4BCC86B497CAD066F364CE8E187D616283925FE6D58140934B9EBE1893F02"
    "src/main/resources/assets/appliedpackaging/textures/block/me_packager/belt_scroll.png" = "EDD93FF96C09554B23B5B70266F5D2320C2A19AAC6F66445F8A4B9240379BA04"
    "src/main/resources/assets/appliedpackaging/textures/part/advanced_pattern_encoding_terminal_back.png" = "0FBD8A8743D7C56FCBC5ECF3957E3C6B7B50D67B43169129ECCFEFB5605E0AF5"
    "src/main/resources/assets/appliedpackaging/textures/part/advanced_pattern_encoding_terminal_bright.png" = "1488EC1F42AFFA736CB5E5687C29B9F4EC7A41C7AAC2FD28C86AE1E6EF4914CF"
    "src/main/resources/assets/appliedpackaging/textures/part/advanced_pattern_encoding_terminal_colored.png" = "0A55D056F3DB2F501922AEFB00F8DA66605262EE8E95C413DA1C1BE057CCF7D2"
    "src/main/resources/assets/appliedpackaging/textures/part/advanced_pattern_encoding_terminal_dark.png" = "36D633037B7B40A5B289457533F63B817F098F9DDBF0F99CBCCA47002D12D4A3"
    "src/main/resources/assets/appliedpackaging/textures/part/advanced_pattern_encoding_terminal_front.png" = "0C304DD14AB433EAB35115EEDAF411278AB9C79786252F461AC525EA337852A6"
    "src/main/resources/assets/appliedpackaging/textures/part/advanced_pattern_encoding_terminal_medium.png" = "FE7A93FC055AD74AF9113711F799E56FE600A44E129B3D354D9F89C9BF2CCB98"
    "src/main/resources/assets/appliedpackaging/textures/part/advanced_pattern_encoding_terminal_sides.png" = "7F801E1DF17B380A42AE7501C89CEEB2FBCF5338E68CB5C32E986CD915DCAC7E"
    "src/main/resources/assets/appliedpackaging/textures/part/advanced_pattern_encoding_terminal_sides_status.png" = "6919534933452D822A8DB16E3BB78A385163A24A73F221C9C937F22167049829"
    "src/main/resources/assets/appliedpackaging/textures/part/advanced_pattern_encoding_terminal_status_has_channel.png" = "5BDC442AC0FA9D38C79C036D0B6AD732DA0187109209DDF3B7497DF932B525F1"
    "src/main/resources/assets/appliedpackaging/textures/part/advanced_pattern_encoding_terminal_status_off.png" = "7FCF547A4B40CDD91686354A120422D0868EA05BF4AC3D635D89E86E8B169892"
    "src/main/resources/assets/appliedpackaging/textures/part/advanced_pattern_encoding_terminal_status_on.png" = "5DDA081025A4D57B25EA404AEECAC6EBE73776432402591069282586E50CC0EA"
    "src/main/resources/assets/appliedpackaging/textures/part/package_storage_bus_front.png" = "B682F316CB77A407736E4FD73D1CAE5104F679918090F23E1D994F3E63DBA1AB"
    "src/main/resources/assets/appliedpackaging/textures/part/package_unpacking_bus_front.png" = "A6FB292B206693865094DF901A4A0789F051630C14001C9003423F5B3E44E96F"
    "src/main/resources/assets/appliedpackaging/textures/part/package_unpacking_bus_back.png" = "3086B228171D19F1DFB55FDF6384165FDB78CEABB25B09C4706CE2E23599CD07"
    "src/main/resources/assets/appliedpackaging/textures/block/package_assembler.png" = "345A070081B556D2EF44AE0DAB65210F7728C33BB7C29FD46B526C607605FCE0"
    "src/main/resources/assets/appliedpackaging/textures/gui/package-storagebus.png" = "506BE44EF826C14C1DBE37C076EDC7955C0DBFE35A7DB9B157EABA8E241787DE"
    "src/main/resources/assets/appliedpackaging/textures/gui/package-storagebus-sprites.png" = "1E5A223CBBE07D14CE9A97389596E188C668B4A44F0011EA8AA64D9E99EC3EC6"
    "src/main/resources/assets/appliedpackaging/textures/gui/mepackageassembler.png" = "C96749C3F8EF43DDB63B5F2F6A1E4B769319F52B9964ACD0AEAC7053481B5F33"
    "src/main/resources/assets/appliedpackaging/textures/gui/advanced_pattern_encoding_terminal.png" = "AEDD18C31813DC23287EF0C53FF57274672AFCA803CF7CBA755AC757B360062A"
    "src/main/resources/assets/appliedpackaging/textures/gui/pattern_mode_packaging.png" = "65DE82E33052D1F941182863D8303C4D22BA52C07528AC69702B9BA685153096"
    "src/main/resources/assets/appliedpackaging/textures/gui/ae2-states.png" = "0996B0084C7BF37F65A97A745982AB681EBD86F142FADE526F14C823C4727E55"
    "src/main/resources/assets/appliedpackaging/textures/gui/ae2-terminal.png" = "9CE91ECCF149E1703960906B349093AF726E6DEB753985C7E85B5D0DB359B3E4"
    "src/main/resources/assets/ae2/textures/guis/text_field.png" = "73BBA41174D3EC15D83947E439915873611735FE436AD0CBC7653ECA15E23AD1"
    "src/main/resources/assets/appliedpackaging/textures/gui/package_bus_extra_panels.png" = "C67FED0F98C9CA67A0602B5589A5191D59D5DD2BD3848C62DE0E209E0E44B8B0"
    "src/main/resources/assets/appliedpackaging/textures/gui/package_bus_vertical_buttons_bg.png" = "62150F9869EE17CBD15BDA963542287BF798482CEED1F18F0E24DD82381F7715"
    "src/main/resources/assets/appliedpackaging/textures/gui/sequence_buffer.png" = "075E3329882A3AAE7FE7EBDAAB32EBF799531DC4224F3F37B563CD6B537A2C67"
    "src/main/resources/assets/appliedpackaging/textures/gui/sequence_buffer_side.png" = "2749D7BDAB5E3B9BFF240B6F618AB55AE14A3C2252D9DEB63D959874456D91A0"
}
foreach ($entry in $bytePreservedPngHashes.GetEnumerator()) {
    if (Test-Path -LiteralPath $entry.Key) {
        $actualHash = (Get-FileHash -Algorithm SHA256 -LiteralPath $entry.Key).Hash
        Assert-True `
            ($actualHash -eq $entry.Value) `
            "Byte-preserved PNG keeps its source hash: $($entry.Key)"
    }
}

$mePackagerItemModelPath = "src/main/resources/assets/appliedpackaging/models/block/me_packager/item.json"
$mePackagerBlockstatePath = "src/main/resources/assets/appliedpackaging/blockstates/me_packager.json"
Assert-True (Test-Path -LiteralPath $mePackagerItemModelPath) "ME Packager complete item model exists"
Assert-True (Test-Path -LiteralPath $mePackagerBlockstatePath) "ME Packager directional blockstate exists"

if (Test-Path -LiteralPath $mePackagerItemModelPath) {
    $mePackagerItemModel = Get-JsonFile $mePackagerItemModelPath
    if ($null -ne $mePackagerItemModel) {
        Assert-True `
            ($mePackagerItemModel.parent -eq "minecraft:block/block") `
            "ME Packager item model inherits standard block display transforms"
        Assert-True `
            (@($mePackagerItemModel.elements).Count -eq 11) `
            "ME Packager item model preserves all 11 source cubes"
    }
}

if (Test-Path -LiteralPath $mePackagerBlockstatePath) {
    $mePackagerBlockstate = Get-JsonFile $mePackagerBlockstatePath
    if ($null -ne $mePackagerBlockstate) {
        $expectedFacingRotations = [ordered]@{
            north = 270
            east = 0
            south = 90
            west = 180
        }
        foreach ($facingEntry in $expectedFacingRotations.GetEnumerator()) {
            $variantKey = "facing=$($facingEntry.Key)"
            $variantProperty = $mePackagerBlockstate.variants.PSObject.Properties[$variantKey]
            Assert-True ($null -ne $variantProperty) "ME Packager blockstate declares $variantKey"
            if ($null -eq $variantProperty) {
                continue
            }

            $variant = $variantProperty.Value
            $actualRotation = if ($variant.PSObject.Properties.Name -contains "y") {
                [int] $variant.y
            } else {
                0
            }
            Assert-True `
                ($variant.model -eq "appliedpackaging:block/me_packager/body") `
                "ME Packager $variantKey renders the upright body"
            Assert-True `
                ($actualRotation -eq $facingEntry.Value) `
                "ME Packager $variantKey uses the expected model rotation"
        }
        Assert-True `
            (@($mePackagerBlockstate.variants.PSObject.Properties).Count -eq 4) `
            "ME Packager blockstate declares only the four horizontal facing variants"
    }
}

$sequenceBufferDirections = @("down", "up", "north", "south", "west", "east")
$sequenceBufferAxes = @("x", "y", "z")
$sequenceBufferDirectionAxes = @{
    down = "y"
    up = "y"
    north = "z"
    south = "z"
    west = "x"
    east = "x"
}
$sequenceBufferGeneratedModelPaths = @()
foreach ($direction in $sequenceBufferDirections) {
    $sequenceBufferGeneratedModelPaths += "src/main/resources/assets/appliedpackaging/models/block/sequence_buffer/generated/unformed_directed/$direction.json"
    $sequenceBufferGeneratedModelPaths += "src/main/resources/assets/appliedpackaging/models/block/sequence_buffer/generated/endpoint/$direction.json"
    $sequenceBufferGeneratedModelPaths += "src/main/resources/assets/appliedpackaging/models/block/sequence_buffer/generated/tail/$direction.json"
}
foreach ($axis in $sequenceBufferAxes) {
    $sequenceBufferGeneratedModelPaths += "src/main/resources/assets/appliedpackaging/models/block/sequence_buffer/generated/member/$axis.json"
    foreach ($facing in $sequenceBufferDirections) {
        if ($sequenceBufferDirectionAxes[$facing] -ne $axis) {
            $sequenceBufferGeneratedModelPaths += "src/main/resources/assets/appliedpackaging/models/block/sequence_buffer/generated/member_directed/$axis/$facing.json"
        }
    }
}
foreach ($sequenceDirection in $sequenceBufferDirections) {
    foreach ($facing in $sequenceBufferDirections) {
        if ($sequenceBufferDirectionAxes[$facing] -ne $sequenceBufferDirectionAxes[$sequenceDirection]) {
            $sequenceBufferGeneratedModelPaths += "src/main/resources/assets/appliedpackaging/models/block/sequence_buffer/generated/tail_directed/$sequenceDirection/$facing.json"
        }
    }
}
Assert-True `
    ($sequenceBufferGeneratedModelPaths.Count -eq 57) `
    "Sequence Buffer audit declares all 57 six-direction generated models"

$sequenceBufferAssetPaths = @(
    "src/main/resources/assets/appliedpackaging/blockstates/sequence_buffer.json",
    "src/main/resources/assets/appliedpackaging/models/block/sequence_buffer/shell.json",
    "src/main/resources/assets/appliedpackaging/models/block/sequence_buffer/unformed.json",
    "src/main/resources/assets/appliedpackaging/models/item/sequence_buffer.json"
) + $sequenceBufferGeneratedModelPaths
foreach ($sequenceBufferAssetPath in $sequenceBufferAssetPaths) {
    Assert-True (Test-Path -LiteralPath $sequenceBufferAssetPath) "Sequence Buffer model asset exists: $sequenceBufferAssetPath"
    if (Test-Path -LiteralPath $sequenceBufferAssetPath) {
        $null = Get-JsonFile $sequenceBufferAssetPath
    }
}

$sequenceBufferBlockstatePath = $sequenceBufferAssetPaths[0]
if (Test-Path -LiteralPath $sequenceBufferBlockstatePath) {
    $sequenceBufferBlockstate = Get-JsonFile $sequenceBufferBlockstatePath
    if ($null -ne $sequenceBufferBlockstate) {
        $sequenceBufferStates = @($sequenceBufferBlockstate.multipart | ForEach-Object { $_.when.state } | Where-Object { $null -ne $_ } | Sort-Object -Unique)
        Assert-True `
            (($sequenceBufferStates -join ",") -eq "endpoint,member,member_directed,unformed,unformed_directed") `
            "Sequence Buffer blockstate renders all five visual states"
        Assert-True `
            (@($sequenceBufferBlockstate.multipart).Count -eq 58) `
            "Sequence Buffer blockstate declares the complete six-direction middle/tail orientation set"
        $sequenceBufferTailValues = @($sequenceBufferBlockstate.multipart | ForEach-Object { $_.when.tail } | Where-Object { $null -ne $_ } | Sort-Object -Unique)
        Assert-True `
            (($sequenceBufferTailValues -join ",") -eq "false,true") `
            "Sequence Buffer blockstate declares formed middle and tail segment variants"
        $sequenceBufferTailDirections = @($sequenceBufferBlockstate.multipart | Where-Object { $_.when.tail -eq "true" } | ForEach-Object { $_.when.sequence_direction } | Sort-Object -Unique)
        Assert-True `
            (($sequenceBufferTailDirections -join ",") -eq "down,east,north,south,up,west") `
            "Sequence Buffer tail models cover all six sequence directions"
        $sequenceBufferFacings = @($sequenceBufferBlockstate.multipart | ForEach-Object { $_.when.facing } | Where-Object { $null -ne $_ } | Sort-Object -Unique)
        Assert-True `
            (($sequenceBufferFacings -join ",") -eq "down,east,north,south,up,west") `
            "Sequence Buffer directed models cover all six block-facing directions"
        $sequenceBufferStateAxes = @($sequenceBufferBlockstate.multipart | ForEach-Object { $_.when.axis } | Where-Object { $null -ne $_ } | Sort-Object -Unique)
        Assert-True `
            (($sequenceBufferStateAxes -join ",") -eq "x,y,z") `
            "Sequence Buffer member models cover the X, Y, and Z structure axes"
        $sequenceBufferStateCounts = @{}
        foreach ($entry in @($sequenceBufferBlockstate.multipart)) {
            $stateName = [string] $entry.when.state
            if (-not $sequenceBufferStateCounts.ContainsKey($stateName)) {
                $sequenceBufferStateCounts[$stateName] = 0
            }
            $sequenceBufferStateCounts[$stateName] += 1
        }
        Assert-True `
            ($sequenceBufferStateCounts.unformed -eq 1 -and
                $sequenceBufferStateCounts.unformed_directed -eq 6 -and
                $sequenceBufferStateCounts.endpoint -eq 6 -and
                $sequenceBufferStateCounts.member -eq 9 -and
                $sequenceBufferStateCounts.member_directed -eq 36) `
            "Sequence Buffer blockstate contains the expected 3D orientation matrix"
    }
}

$sequenceBufferEastTailPath = "src/main/resources/assets/appliedpackaging/models/block/sequence_buffer/generated/tail/east.json"
if (Test-Path -LiteralPath $sequenceBufferEastTailPath) {
    $sequenceBufferEastTail = Get-JsonFile $sequenceBufferEastTailPath
    if ($null -ne $sequenceBufferEastTail) {
        Assert-True `
            ($sequenceBufferEastTail.textures.east -eq "appliedpackaging:block/sequence_buffer/faces/tail_back" -and
                $sequenceBufferEastTail.textures.west -eq "appliedpackaging:block/sequence_buffer/faces/formed_middle_side_edge_occluded" -and
                [int] $sequenceBufferEastTail.elements[0].faces.up.rotation -eq 90) `
            "Sequence Buffer east tail keeps its cap outward and its open edge toward the controller"
    }
}

$sequenceBufferUpTailPath = "src/main/resources/assets/appliedpackaging/models/block/sequence_buffer/generated/tail/up.json"
if (Test-Path -LiteralPath $sequenceBufferUpTailPath) {
    $sequenceBufferUpTail = Get-JsonFile $sequenceBufferUpTailPath
    if ($null -ne $sequenceBufferUpTail) {
        $sequenceBufferUpTailNorthRotation = if ($sequenceBufferUpTail.elements[0].faces.north.PSObject.Properties.Name -contains "rotation") {
            [int] $sequenceBufferUpTail.elements[0].faces.north.rotation
        } else {
            0
        }
        Assert-True `
            ($sequenceBufferUpTail.textures.up -eq "appliedpackaging:block/sequence_buffer/faces/tail_back" -and
                $sequenceBufferUpTail.textures.down -eq "appliedpackaging:block/sequence_buffer/faces/formed_middle_side_edge_occluded" -and
                $sequenceBufferUpTailNorthRotation -eq 0) `
            "Sequence Buffer upward tail keeps its cap outward and its open edge toward the controller"
    }
}

$sequenceBufferDirectedEastTailPath = "src/main/resources/assets/appliedpackaging/models/block/sequence_buffer/generated/tail_directed/east/up.json"
if (Test-Path -LiteralPath $sequenceBufferDirectedEastTailPath) {
    $sequenceBufferDirectedEastTail = Get-JsonFile $sequenceBufferDirectedEastTailPath
    if ($null -ne $sequenceBufferDirectedEastTail) {
        $sequenceBufferDirectedEastTailSouth = $sequenceBufferDirectedEastTail.elements[0].faces.south
        Assert-True `
            ([int] $sequenceBufferDirectedEastTail.elements[0].faces.up.rotation -eq 90 -and
                [int] $sequenceBufferDirectedEastTailSouth.rotation -eq 270 -and
                ((@($sequenceBufferDirectedEastTailSouth.uv) -join ",") -eq "0,16,16,0")) `
            "Sequence Buffer directed tail preserves its output arrow while keeping the side cap outward"
    }
}

$sequenceBufferShellPath = $sequenceBufferAssetPaths[1]
if (Test-Path -LiteralPath $sequenceBufferShellPath) {
    $sequenceBufferShell = Get-JsonFile $sequenceBufferShellPath
    if ($null -ne $sequenceBufferShell) {
        Assert-True `
            ($sequenceBufferShell.parent -eq "minecraft:block/block" -and
                @($sequenceBufferShell.elements).Count -eq 1 -and
                ((@($sequenceBufferShell.elements[0].from) -join ",") -eq "0,0,0") -and
                ((@($sequenceBufferShell.elements[0].to) -join ",") -eq "16,16,16")) `
            "Sequence Buffer shell inherits the standard 3D block item transforms and remains one in-bounds full-block cuboid"
    }
}

$sequenceBufferItemPath = "src/main/resources/assets/appliedpackaging/models/item/sequence_buffer.json"
if (Test-Path -LiteralPath $sequenceBufferItemPath) {
    $sequenceBufferItem = Get-JsonFile $sequenceBufferItemPath
    if ($null -ne $sequenceBufferItem) {
        Assert-True `
            ($sequenceBufferItem.parent -eq "appliedpackaging:block/sequence_buffer/unformed") `
            "Sequence Buffer item model uses the unformed block model"
    }
}

$sequenceBufferModelText = @(
    @($sequenceBufferAssetPaths) |
        Where-Object { $_ -like "*/models/block/sequence_buffer/*.json" } |
        ForEach-Object { Get-Content -Raw -LiteralPath $_ }
) -join "`n"
foreach ($sequenceBufferFaceName in $sequenceBufferFaceNames) {
    Assert-True `
        ($sequenceBufferModelText.Contains("appliedpackaging:block/sequence_buffer/faces/$sequenceBufferFaceName")) `
        "Sequence Buffer models reference split face texture: $sequenceBufferFaceName"
}

$sequenceBufferMainStylePath = "src/main/resources/assets/ae2/screens/appliedpackaging/sequence_buffer_main.json"
$sequenceBufferSideStylePath = "src/main/resources/assets/ae2/screens/appliedpackaging/sequence_buffer_side.json"
foreach ($sequenceBufferStylePath in @($sequenceBufferMainStylePath, $sequenceBufferSideStylePath)) {
    Assert-True (Test-Path -LiteralPath $sequenceBufferStylePath) "Sequence Buffer screen style exists: $sequenceBufferStylePath"
}
if (Test-Path -LiteralPath $sequenceBufferMainStylePath) {
    $sequenceBufferMainStyle = Get-JsonFile $sequenceBufferMainStylePath
    if ($null -ne $sequenceBufferMainStyle) {
        $mainContents = $sequenceBufferMainStyle.slots.APPLIEDPACKAGING_SEQUENCE_BUFFER_CONTENTS
        Assert-True `
            ($sequenceBufferMainStyle.background.texture -eq "appliedpackaging:textures/gui/sequence_buffer.png" -and
                ((@($sequenceBufferMainStyle.background.srcRect) -join ",") -eq "0,0,195,170")) `
            "Sequence Buffer main screen uses the user 195x170 terminal base"
        Assert-True `
            ($mainContents.left -eq 8 -and $mainContents.top -eq 19 -and $mainContents.grid -eq "BREAK_AFTER_9COLS") `
            "Sequence Buffer main screen declares the 3x9 dynamic storage origin"
        Assert-True `
            ($sequenceBufferMainStyle.widgets.sequenceBufferScrollbar.left -eq 175 -and
                $sequenceBufferMainStyle.widgets.sequenceBufferScrollbar.top -eq 18 -and
                $sequenceBufferMainStyle.widgets.sequenceBufferScrollbar.height -eq 54) `
            "Sequence Buffer main screen keeps the high-version three-row scrollbar geometry"
        Assert-True `
            ($null -eq $sequenceBufferMainStyle.slots.CONFIG -and
                $null -eq $sequenceBufferMainStyle.widgets.inputFilter -and
                $null -eq $sequenceBufferMainStyle.images.inputFilter) `
            "Sequence Buffer main screen leaves the deferred input-filter panel hidden"
        Assert-True `
            ($sequenceBufferMainStyle.widgets.upgrades.right -eq 2 -and
                $null -eq $sequenceBufferMainStyle.widgets.upgrades.left) `
            "Sequence Buffer main upgrade panel stays attached to the right side"
    }
}
if (Test-Path -LiteralPath $sequenceBufferSideStylePath) {
    $sequenceBufferSideStyle = Get-JsonFile $sequenceBufferSideStylePath
    if ($null -ne $sequenceBufferSideStyle) {
        $sideContents = $sequenceBufferSideStyle.slots.APPLIEDPACKAGING_SEQUENCE_BUFFER_CONTENTS
        Assert-True `
            ($sequenceBufferSideStyle.background.texture -eq "appliedpackaging:textures/gui/sequence_buffer_side.png" -and
                ((@($sequenceBufferSideStyle.background.srcRect) -join ",") -eq "0,0,176,168")) `
            "Sequence Buffer side screen uses the byte-preserved ME Chest base"
        Assert-True `
            ($sideContents.left -eq 80 -and $sideContents.top -eq 37) `
            "Sequence Buffer side screen keeps the single central storage slot"
        Assert-True `
            ($null -eq $sequenceBufferSideStyle.slots.CONFIG -and
                $null -eq $sequenceBufferSideStyle.widgets.inputFilter -and
                $null -eq $sequenceBufferSideStyle.images.inputFilter) `
            "Sequence Buffer side screen leaves the deferred input-filter panel hidden"
        Assert-True `
            ($sequenceBufferSideStyle.widgets.upgrades.right -eq 2 -and
                $null -eq $sequenceBufferSideStyle.widgets.upgrades.left) `
            "Sequence Buffer side upgrade panel stays attached to the right side"
    }
}

$sequenceBufferMainScreenPath = "src/main/java/com/warmthdawn/appliedpackaging/client/screen/SequenceBufferMainScreen.java"
if (Test-Path -LiteralPath $sequenceBufferMainScreenPath) {
    $sequenceBufferMainScreenText = Get-Content -Raw -LiteralPath $sequenceBufferMainScreenPath
    Assert-True `
        ($sequenceBufferMainScreenText -match 'addScrollBar\("sequenceBufferScrollbar",\s*ModernScrollbarStyles\.BIG\)') `
        "Sequence Buffer scrollbar uses the cached current-AE2 12x15 enabled and disabled handles"
}

$advancedPatternTerminalScreenPath = "src/main/java/com/warmthdawn/appliedpackaging/client/screen/AdvancedPatternEncodingTermScreen.java"
if (Test-Path -LiteralPath $advancedPatternTerminalScreenPath) {
    $advancedPatternTerminalScreenText = Get-Content -Raw -LiteralPath $advancedPatternTerminalScreenPath
    Assert-True `
        ($advancedPatternTerminalScreenText.Contains('"textures/gui/ae2-terminal.png"') -and
            $advancedPatternTerminalScreenText -match '(?s)drawBackgroundSegment\(\s*graphics,\s*LATEST_TERMINAL' -and
            $advancedPatternTerminalScreenText.Contains('Blitter.texture(LATEST_TERMINAL)')) `
        "Advanced Pattern Terminal search header and pinned row use the cached current-AE2 terminal atlas"

    $colorModeWidgetIndex = $advancedPatternTerminalScreenText.IndexOf('addRenderableWidget(colorModeButton);')
    $toolbarCaptureIndex = $advancedPatternTerminalScreenText.IndexOf('modernToolbar.captureIconButtons(children());')
    $toolbarRendererIndex = $advancedPatternTerminalScreenText.IndexOf('modernToolbar.createIconButtonRenderers()')
    Assert-True `
        ($advancedPatternTerminalScreenText.Contains('Icon.SCHEDULING_DEFAULT') -and
            $advancedPatternTerminalScreenText.Contains('Icon.SCHEDULING_ROUND_ROBIN') -and
            $colorModeWidgetIndex -ge 0 -and
            $toolbarCaptureIndex -gt $colorModeWidgetIndex -and
            $toolbarRendererIndex -gt $toolbarCaptureIndex) `
        "Advanced Pattern Terminal registers color mode before shared toolbar capture and renders its overlay last"

    $advancedPanelDrawIndex = $advancedPatternTerminalScreenText.IndexOf('drawAdvancedPanel(graphics, offsetX, offsetY);')
    $advancedSlotDrawIndex = $advancedPatternTerminalScreenText.IndexOf('drawAdvancedInputSlotBackgrounds(graphics, offsetX, offsetY);')
    Assert-True `
        ($advancedPatternTerminalScreenText -match '(?s)ADVANCED_PANEL\s*=\s*Blitter\.texture\(PACKAGE_PANEL_TEXTURE\)\.src\(0,\s*128,\s*PACKAGE_PANEL_WIDTH,\s*PACKAGE_PANEL_HEIGHT\)' -and
            $advancedPanelDrawIndex -ge 0 -and
            $advancedSlotDrawIndex -gt $advancedPanelDrawIndex) `
        "Advanced Pattern Terminal paints the advanced panel over the corrected gray base before slot overlays"
    Assert-True `
        ($advancedPatternTerminalScreenText.Contains('modernToolbar.isToolbarButton(getFocused())') -and
            $advancedPatternTerminalScreenText.Contains('setFocused(null)')) `
        "Advanced Pattern Terminal releases mouse focus from toolbar buttons after clicks"
}

$modernSlotRenderingPath = "src/main/java/com/warmthdawn/appliedpackaging/client/widget/ModernSlotRendering.java"
if (Test-Path -LiteralPath $modernSlotRenderingPath) {
    $modernSlotRenderingText = Get-Content -Raw -LiteralPath $modernSlotRenderingPath
    Assert-True `
        ($modernSlotRenderingText -match 'Blitter\.texture\(PACKAGE_SPRITES\)\.src\(0,\s*64,\s*18,\s*18\)') `
        "Shared slot renderer uses the current-AE2 transparent-border slot sprite"
}

$modernScrollbarStylesPath = "src/main/java/com/warmthdawn/appliedpackaging/client/widget/ModernScrollbarStyles.java"
if (Test-Path -LiteralPath $modernScrollbarStylesPath) {
    $modernScrollbarStylesText = Get-Content -Raw -LiteralPath $modernScrollbarStylesPath
    Assert-True `
        ($modernScrollbarStylesText -match '(?s)Scrollbar\.Style\.create\(.*?7,\s*15,\s*0,\s*32,\s*16,\s*32\)') `
        "Shared small scrollbar style uses the current-AE2 enabled and disabled sprites"
}

$packageAssemblerScreenPath = "src/main/java/com/warmthdawn/appliedpackaging/client/screen/PackageAssemblerScreen.java"
if (Test-Path -LiteralPath $packageAssemblerScreenPath) {
    $packageAssemblerScreenText = Get-Content -Raw -LiteralPath $packageAssemblerScreenPath
    Assert-True `
        ($packageAssemblerScreenText.Contains("ModernSlotRendering.drawSlotBackground") -and
            $packageAssemblerScreenText.Contains("ModernScrollbarStyles.SMALL") -and
            -not $packageAssemblerScreenText.Contains("SLOT_DISABLED_OVERLAY")) `
        "Package Assembler screen uses shared current-AE2 slot and scrollbar rendering"
    Assert-True `
        ($packageAssemblerScreenText -match 'COLOR_BUTTON_X\s*=\s*95;' -and
            $packageAssemblerScreenText -match 'COLOR_BUTTON_Y\s*=\s*29;') `
        "Package Assembler color trigger aligns its 8x8 swatch to the corrected user frame"
}

$packageAssemblerStylePath = "src/main/resources/assets/ae2/screens/appliedpackaging/package_assembler.json"
if (Test-Path -LiteralPath $packageAssemblerStylePath) {
    $packageAssemblerStyle = Get-JsonFile $packageAssemblerStylePath
    if ($null -ne $packageAssemblerStyle) {
        Assert-True `
            ($packageAssemblerStyle.widgets.packageQueueScrollbar.left -eq 12 -and
                $packageAssemblerStyle.widgets.packageQueueScrollbar.top -eq 31 -and
                $packageAssemblerStyle.widgets.packageQueueScrollbar.height -eq 72) `
            "Package Assembler scrollbar stays aligned one pixel right on the user track"
        Assert-True `
            ($packageAssemblerStyle.slots.PROCESSING_INPUTS.left -eq 21 -and
                $packageAssemblerStyle.slots.PROCESSING_INPUTS.top -eq 33 -and
                $packageAssemblerStyle.slots.MACHINE_CRAFTING_GRID.left -eq 21 -and
                $packageAssemblerStyle.slots.MACHINE_CRAFTING_GRID.top -eq 51 -and
                $packageAssemblerStyle.slots.CRAFTING_GRID.left -eq 21 -and
                $packageAssemblerStyle.slots.CRAFTING_GRID.top -eq 69 -and
                $packageAssemblerStyle.slots.CONFIG.left -eq 21 -and
                $packageAssemblerStyle.slots.CONFIG.top -eq 87) `
            "Package Assembler input rows alone start one pixel right on the corrected user atlas"
        Assert-True `
            ($packageAssemblerStyle.slots.BLANK_PATTERN.left -eq 108 -and
                $packageAssemblerStyle.slots.BLANK_PATTERN.top -eq 32) `
            "Package Assembler marker filter occupies the user atlas marker frame"
    }
}

$packageToolbarSpritesPath = "src/main/java/com/warmthdawn/appliedpackaging/client/widget/PackageToolbarSprites.java"
$modernToolbarPath = "src/main/java/com/warmthdawn/appliedpackaging/client/widget/ModernVerticalToolbar.java"
$modernUpgradeableScreenPath = "src/main/java/com/warmthdawn/appliedpackaging/client/screen/ModernUpgradeableScreen.java"
$sequenceBufferSharedScreenPath = "src/main/java/com/warmthdawn/appliedpackaging/client/screen/AbstractSequenceBufferScreen.java"
$mePackagerScreenPath = "src/main/java/com/warmthdawn/appliedpackaging/client/screen/MePackagerScreen.java"
$packageBusScreenPath = "src/main/java/com/warmthdawn/appliedpackaging/client/screen/PackageBusScreen.java"
if (Test-Path -LiteralPath $packageToolbarSpritesPath) {
    $packageToolbarSpritesText = Get-Content -Raw -LiteralPath $packageToolbarSpritesPath
    $expectedToolbarSpriteCoordinates = @(
        'ANTI_CLOG_ON = icon(0, 96)',
        'ANTI_CLOG_OFF = icon(0, 112)',
        'SYNCHRONIZED_OUTPUT_ON = icon(16, 96)',
        'SYNCHRONIZED_OUTPUT_OFF = icon(16, 112)',
        'PATTERN_SYNC_ON = icon(32, 96)',
        'PATTERN_SYNC_OFF = icon(32, 112)',
        'INPUT_DELAY = icon(0, 128)',
        'ITEMS_ONLY = icon(16, 128)',
        'FLUIDS_ONLY = icon(32, 128)'
    )
    $toolbarSpriteCoordinatesValid = $true
    foreach ($coordinate in $expectedToolbarSpriteCoordinates) {
        if (-not $packageToolbarSpritesText.Contains($coordinate)) {
            $toolbarSpriteCoordinatesValid = $false
        }
    }
    Assert-True `
        $toolbarSpriteCoordinatesValid `
        "Applied Packaging toolbar sprites use the user-authored 3x3 block at atlas origin 0,96"
}
if ((Test-Path -LiteralPath $sequenceBufferSharedScreenPath) -and
        (Test-Path -LiteralPath $mePackagerScreenPath) -and
        (Test-Path -LiteralPath $packageBusScreenPath)) {
    $sequenceBufferSharedScreenText = Get-Content -Raw -LiteralPath $sequenceBufferSharedScreenPath
    $mePackagerScreenText = Get-Content -Raw -LiteralPath $mePackagerScreenPath
    $packageBusScreenText = Get-Content -Raw -LiteralPath $packageBusScreenPath
    Assert-True `
        ($sequenceBufferSharedScreenText.Contains('PackageToolbarSprites.ANTI_CLOG_ON') -and
            $sequenceBufferSharedScreenText.Contains('PackageToolbarSprites.SYNCHRONIZED_OUTPUT_ON') -and
            $sequenceBufferSharedScreenText.Contains('PackageToolbarSprites.PATTERN_SYNC_ON') -and
            $sequenceBufferSharedScreenText.Contains('PackageToolbarSprites.INPUT_DELAY') -and
            $mePackagerScreenText.Contains('PackageToolbarSprites.ANTI_CLOG_ON') -and
            $packageBusScreenText.Contains('PackageToolbarSprites.ANTI_CLOG_ON')) `
        "Package-specific machine and bus controls use the new toolbar sprite groups"
}
if (Test-Path -LiteralPath $modernToolbarPath) {
    $modernToolbarText = Get-Content -Raw -LiteralPath $modernToolbarPath
    Assert-True `
        ($modernToolbarText.Contains('Icon.TYPE_FILTER_ITEMS') -and
            $modernToolbarText.Contains('PackageToolbarSprites.ITEMS_ONLY') -and
            $modernToolbarText.Contains('Icon.TYPE_FILTER_FLUIDS') -and
            $modernToolbarText.Contains('PackageToolbarSprites.FLUIDS_ONLY')) `
        "Terminal type filters use the supplied item-only and fluid-only sprites"
    Assert-True `
        ($modernToolbarText.Contains('button.isHovered() ? BUTTON_HOVER : BUTTON') -and
            -not $modernToolbarText.Contains('BUTTON_FOCUS') -and
            -not $modernToolbarText.Contains('button.isFocused()')) `
        "Modern toolbar overlay uses only normal and hover backgrounds"
    Assert-True `
        $modernToolbarText.Contains('public boolean isToolbarButton(GuiEventListener listener)') `
        "Modern toolbar exposes button ownership for post-click focus release"
    Assert-True `
        ($modernToolbarText.Contains('public void captureIconButtons(Iterable<? extends GuiEventListener> children)') -and
            -not $modernToolbarText.Contains('void setButtons(') -and
            -not $modernToolbarText.Contains('void appendButton(') -and
            -not $modernToolbarText.Contains('public static void renderButton(')) `
        "Modern toolbar has one capture-based ownership and rendering path"
}
if (Test-Path -LiteralPath $modernUpgradeableScreenPath) {
    $modernUpgradeableScreenText = Get-Content -Raw -LiteralPath $modernUpgradeableScreenPath
    Assert-True `
        ($modernUpgradeableScreenText.Contains('modernToolbar.isToolbarButton(getFocused())') -and
            $modernUpgradeableScreenText.Contains('setFocused(null)')) `
        "Modern upgradeable screens release mouse focus from toolbar buttons after clicks"
}
if (Test-Path -LiteralPath $packageBusScreenPath) {
    $packageBusScreenText = Get-Content -Raw -LiteralPath $packageBusScreenPath
    Assert-True `
        ($packageBusScreenText.Contains('modernToolbar.isToolbarButton(getFocused())') -and
            $packageBusScreenText.Contains('setFocused(null)')) `
        "Package Bus screens release mouse focus from toolbar buttons after clicks"
    $packageBusSuperInitIndex = $packageBusScreenText.IndexOf('super.init();')
    $packageBusLocalButtonsIndex = $packageBusScreenText.IndexOf('for (Button button : toolbarButtons)')
    $packageBusToolbarCaptureIndex = $packageBusScreenText.IndexOf('modernToolbar.captureIconButtons(children());')
    $packageBusToolbarRendererIndex = $packageBusScreenText.IndexOf('modernToolbar.createIconButtonRenderers()')
    Assert-True `
        ($packageBusSuperInitIndex -ge 0 -and
            $packageBusLocalButtonsIndex -gt $packageBusSuperInitIndex -and
            $packageBusToolbarCaptureIndex -gt $packageBusLocalButtonsIndex -and
            $packageBusToolbarRendererIndex -gt $packageBusToolbarCaptureIndex -and
            -not $packageBusScreenText.Contains('modernToolbar.setButtons(') -and
            -not $packageBusScreenText.Contains('class ModernActionButton') -and
            -not $packageBusScreenText.Contains('class ModernServerSettingToggleButton')) `
        "Package Bus captures AE2 guide and local buttons in one shared toolbar pass"
}

$mePackagerScreenPath = "src/main/java/com/warmthdawn/appliedpackaging/client/screen/MePackagerScreen.java"
if (Test-Path -LiteralPath $mePackagerScreenPath) {
    $mePackagerScreenText = Get-Content -Raw -LiteralPath $mePackagerScreenPath
    Assert-True `
        ($mePackagerScreenText.Contains("ModernSlotRendering.drawSlotBackground") -and
            -not $mePackagerScreenText.Contains("SLOT_BACKGROUND_TOP") -and
            -not $mePackagerScreenText.Contains("SLOT_BACKGROUND_BODY")) `
        "ME Packager optional slots use the shared sprite instead of flat-color reconstruction"
}

$clientJavaRoot = "src/main/java/com/warmthdawn/appliedpackaging"
if (Test-Path -LiteralPath $clientJavaRoot) {
    $javaSourceTexts = Get-ChildItem -LiteralPath $clientJavaRoot -Filter "*.java" -File -Recurse |
        ForEach-Object { Get-Content -Raw -LiteralPath $_.FullName }
    $legacyOptionalSlotRenderers = @($javaSourceTexts | Where-Object {
        $_ -match '(?s)isRenderDisabled\s*\(\s*\)\s*\{[^}]*return\s+true\s*;'
    })
    $legacySlotIcons = @($javaSourceTexts | Where-Object {
        $_.Contains("Icon.SLOT_BACKGROUND")
    })
    Assert-True `
        ($legacyOptionalSlotRenderers.Count -eq 0) `
        "Applied Packaging optional slots never enable AE2 15's legacy slot-background renderer"
    Assert-True `
        ($legacySlotIcons.Count -eq 0) `
        "Applied Packaging screens never bind the legacy Icon.SLOT_BACKGROUND texture"
}

$sequenceBufferSharedScreenPath = "src/main/java/com/warmthdawn/appliedpackaging/client/screen/AbstractSequenceBufferScreen.java"
if (Test-Path -LiteralPath $sequenceBufferSharedScreenPath) {
    $sequenceBufferSharedScreenText = Get-Content -Raw -LiteralPath $sequenceBufferSharedScreenPath
    $sequenceBufferSettingActions = @(
        "menu::toggleAutoOutput",
        "menu::toggleBlockingMode",
        "menu::toggleAntiClogMode",
        "menu::toggleSynchronizedOutput",
        "menu::togglePatternMode",
        "menu.cycleInputDelay"
    )
    $hasAllSequenceBufferSettingActions = $true
    foreach ($sequenceBufferSettingAction in $sequenceBufferSettingActions) {
        if (-not $sequenceBufferSharedScreenText.Contains($sequenceBufferSettingAction)) {
            $hasAllSequenceBufferSettingActions = $false
        }
    }
    Assert-True `
        ($hasAllSequenceBufferSettingActions -and
            $sequenceBufferSharedScreenText.Contains("menu.canEditConfiguration()")) `
        "Sequence Buffer endpoint settings are constructed only for an editable main menu"
}

$sequenceBufferSideScreenPath = "src/main/java/com/warmthdawn/appliedpackaging/client/screen/SequenceBufferSideScreen.java"
if (Test-Path -LiteralPath $sequenceBufferSideScreenPath) {
    $sequenceBufferSideScreenText = Get-Content -Raw -LiteralPath $sequenceBufferSideScreenPath
    Assert-True `
        ($sequenceBufferSideScreenText.Contains("Icon.ENTER") -and
            $sequenceBufferSideScreenText.Contains("menu.openMain()") -and
            $sequenceBufferSideScreenText.Contains("menu.canOpenMain()")) `
        "Sequence Buffer member side screen only offers the current-AE2 endpoint navigation icon"
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

$assemblerModelPath = "src/main/resources/assets/appliedpackaging/models/block/package_assembler.json"
$assemblerLightsModelPath = "src/main/resources/assets/appliedpackaging/models/block/package_assembler_lights.json"
Assert-True (Test-Path -LiteralPath $assemblerModelPath) "AE2 v19 package assembler model exists"
Assert-True (Test-Path -LiteralPath $assemblerLightsModelPath) "AE2 v19 package assembler lights model exists"
if (Test-Path -LiteralPath $assemblerModelPath) {
    $assemblerModel = Get-JsonFile $assemblerModelPath
    if ($null -ne $assemblerModel) {
        Assert-True ($assemblerModel.render_type -eq "cutout") "Package assembler model uses its transparent cutout chamber layer"
        Assert-True ($assemblerModel.textures.base -eq "appliedpackaging:block/package_assembler") "Package assembler model uses the user surface"
        Assert-True (@($assemblerModel.elements).Count -eq 13) "Package assembler preserves the AE2 v19 molecular assembler geometry"
    }
}
if (Test-Path -LiteralPath $assemblerLightsModelPath) {
    $assemblerLightsModel = Get-JsonFile $assemblerLightsModelPath
    if ($null -ne $assemblerLightsModel) {
        Assert-True ($assemblerLightsModel.textures.all -eq "appliedpackaging:block/package_assembler_lights") "Package assembler lights model uses the animated light strip"
    }
}

$packagePartModels = @(
    "src/main/resources/assets/appliedpackaging/models/part/advanced_pattern_encoding_terminal_base.json",
    "src/main/resources/assets/appliedpackaging/models/part/advanced_pattern_encoding_terminal_off.json",
    "src/main/resources/assets/appliedpackaging/models/part/advanced_pattern_encoding_terminal_on.json",
    "src/main/resources/assets/appliedpackaging/models/part/advanced_pattern_encoding_terminal_status_off.json",
    "src/main/resources/assets/appliedpackaging/models/part/advanced_pattern_encoding_terminal_status_on.json",
    "src/main/resources/assets/appliedpackaging/models/part/advanced_pattern_encoding_terminal_status_has_channel.json",
    "src/main/resources/assets/appliedpackaging/models/part/package_storage_bus_base.json",
    "src/main/resources/assets/appliedpackaging/models/part/package_unpacking_bus_base.json",
    "src/main/resources/assets/appliedpackaging/models/part/package_bus_status_off.json",
    "src/main/resources/assets/appliedpackaging/models/part/package_bus_status_on.json",
    "src/main/resources/assets/appliedpackaging/models/part/package_bus_status_has_channel.json"
)
foreach ($packagePartModel in $packagePartModels) {
    Assert-True (Test-Path -LiteralPath $packagePartModel) "AE2 v19 part model exists: $packagePartModel"
}

$advancedTerminalBasePath = "src/main/resources/assets/appliedpackaging/models/part/advanced_pattern_encoding_terminal_base.json"
$advancedTerminalOffPath = "src/main/resources/assets/appliedpackaging/models/part/advanced_pattern_encoding_terminal_off.json"
$advancedTerminalOnPath = "src/main/resources/assets/appliedpackaging/models/part/advanced_pattern_encoding_terminal_on.json"
$advancedTerminalStatusOffPath = "src/main/resources/assets/appliedpackaging/models/part/advanced_pattern_encoding_terminal_status_off.json"
$advancedTerminalStatusOnPath = "src/main/resources/assets/appliedpackaging/models/part/advanced_pattern_encoding_terminal_status_on.json"
$advancedTerminalStatusChannelPath = "src/main/resources/assets/appliedpackaging/models/part/advanced_pattern_encoding_terminal_status_has_channel.json"
$advancedTerminalItemPath = "src/main/resources/assets/appliedpackaging/models/item/advanced_pattern_encoding_terminal.json"

if (Test-Path -LiteralPath $advancedTerminalBasePath) {
    $advancedTerminalBase = Get-JsonFile $advancedTerminalBasePath
    if ($null -ne $advancedTerminalBase) {
        $baseElements = @($advancedTerminalBase.elements)
        Assert-True ($baseElements.Count -eq 3) "Advanced terminal placed Part uses the translated v19 item display geometry"
        Assert-True `
            ($advancedTerminalBase.textures.front_base -eq "appliedpackaging:part/advanced_pattern_encoding_terminal_front") `
            "Advanced terminal placed Part uses the copied v19 front material"
        Assert-True `
            ([int] $baseElements[1].faces.north.tintindex -eq 4 -and $baseElements[1].faces.north.texture -eq "#front_medium_bright") `
            "Advanced terminal placed Part keeps the v19 medium-bright tint layer used by its item model"
        Assert-True `
            (($baseElements[2].from -join ",") -eq "4,4,2" -and ($baseElements[2].to -join ",") -eq "12,12,3") `
            "Advanced terminal placed Part translates the v19 item back geometry into cable-part coordinates"
    }
}

if (Test-Path -LiteralPath $advancedTerminalOnPath) {
    $advancedTerminalOn = Get-JsonFile $advancedTerminalOnPath
    if ($null -ne $advancedTerminalOn) {
        $onElements = @($advancedTerminalOn.elements)
        $onTintOrder = @($onElements | ForEach-Object { [int] $_.faces.north.tintindex })
        Assert-True `
            (($onTintOrder -join ",") -eq "3,2,1") `
            "Advanced terminal on model uses v19 dark/medium/bright tint order [3,2,1]"
        foreach ($element in $onElements) {
            $face = $element.faces.north
            Assert-True `
                ($null -ne $face.forge_data -and [int] $face.forge_data.block_light -eq 15 -and [int] $face.forge_data.sky_light -eq 15) `
                "Advanced terminal on layers use Forge 1.20.1 full-bright face data"
            Assert-True `
                (-not ($face.PSObject.Properties.Name -contains "neoforge_data")) `
                "Advanced terminal on layers do not leak NeoForge-only face data"
        }
    }
}

if (Test-Path -LiteralPath $advancedTerminalOffPath) {
    $advancedTerminalOff = Get-JsonFile $advancedTerminalOffPath
    if ($null -ne $advancedTerminalOff) {
        $offElements = @($advancedTerminalOff.elements)
        $offTintOrder = @($offElements | ForEach-Object { [int] $_.faces.north.tintindex })
        Assert-True `
            (($offTintOrder -join ",") -eq "3,2,1") `
            "Advanced terminal off model keeps all three tint masks without full-bright lighting"
        Assert-True `
            (@($offElements | Where-Object { $_.faces.north.PSObject.Properties.Name -contains "forge_data" }).Count -eq 0) `
            "Advanced terminal off model remains environment-lit"
    }
}

foreach ($statusPath in @($advancedTerminalStatusOffPath, $advancedTerminalStatusOnPath, $advancedTerminalStatusChannelPath)) {
    if (-not (Test-Path -LiteralPath $statusPath)) {
        continue
    }
    $statusModel = Get-JsonFile $statusPath
    if ($null -ne $statusModel) {
        Assert-True `
            (@($statusModel.elements).Count -eq 4) `
            "Advanced terminal status model uses the v19 four-segment indicator geometry: $statusPath"
    }
}

Assert-True (Test-Path -LiteralPath $advancedTerminalItemPath) "Advanced terminal v19 item model exists"
if (Test-Path -LiteralPath $advancedTerminalItemPath) {
    $advancedTerminalItem = Get-JsonFile $advancedTerminalItemPath
    if ($null -ne $advancedTerminalItem) {
        Assert-True `
            ($advancedTerminalItem.parent -eq "ae2:item/part_base") `
            "Advanced terminal item model keeps the AE2 part display transforms"
        Assert-True `
            (@($advancedTerminalItem.elements).Count -eq 6) `
            "Advanced terminal item model uses the v19 display-base geometry"
        $itemTintOrder = @($advancedTerminalItem.elements | ForEach-Object {
                if ($null -ne $_.faces.north -and $_.faces.north.PSObject.Properties.Name -contains "tintindex") {
                    [int] $_.faces.north.tintindex
                }
            })
        Assert-True `
            (($itemTintOrder -join ",") -eq "3,4,2,1") `
            "Advanced terminal item model keeps the v19 four tint layers"
    }
}

$opaqueSolidModelPaths = @(
    "src/main/resources/assets/appliedpackaging/models/part/advanced_pattern_encoding_terminal_base.json",
    "src/main/resources/assets/appliedpackaging/models/part/package_storage_bus_base.json",
    "src/main/resources/assets/appliedpackaging/models/part/package_unpacking_bus_base.json"
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

$pngFiles = @(
    Get-ChildItem -LiteralPath $assetRoot -Recurse -Filter "*.png" -File -ErrorAction SilentlyContinue
    Get-ChildItem -LiteralPath "src/main/resources/assets/ae2" -Recurse -Filter "*.png" -File -ErrorAction SilentlyContinue
)
Assert-True ($pngFiles.Count -gt 0) "PNG assets are present"

$badPngs = [System.Collections.Generic.List[string]]::new()
$badDimensions = [System.Collections.Generic.List[string]]::new()
$badColorTypes = [System.Collections.Generic.List[string]]::new()
$badVisualContent = [System.Collections.Generic.List[string]]::new()
$unexpectedPngs = [System.Collections.Generic.List[string]]::new()
$intentionalTransparentPngs = @(
    # AE2 v19 ships this tint layer as a transparent compatibility surface; its exact upstream
    # bytes are pinned above, so allowing it here cannot hide an arbitrary empty placeholder.
    "src/main/resources/assets/appliedpackaging/textures/part/advanced_pattern_encoding_terminal_colored.png"
)

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

    if (-not $SkipPngVisualContent) {
        $visualStats = Get-PngVisualStats -Path $file.FullName -Info $info
        if (-not $visualStats.Valid) {
            $badVisualContent.Add("$relativePath $($visualStats.Error)") | Out-Null
        } elseif ($visualStats.VisiblePixels -eq 0 -and $relativePath -notin $intentionalTransparentPngs) {
            $badVisualContent.Add("$relativePath is fully transparent") | Out-Null
        } elseif ($visualStats.UniquePixels -lt 2 -and $relativePath -notin $intentionalTransparentPngs) {
            $badVisualContent.Add("$relativePath is a single-color placeholder") | Out-Null
        }
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

if ($SkipPngVisualContent) {
    Add-Pass "PNG visual-content scan skipped by explicit targeted-fixture option"
} elseif ($badVisualContent.Count -eq 0) {
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
