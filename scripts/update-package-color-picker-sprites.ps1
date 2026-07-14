param(
    [string]$RootPath = (Split-Path -Parent $PSScriptRoot)
)

$ErrorActionPreference = 'Stop'
Add-Type -AssemblyName System.Drawing

$root = [System.IO.Path]::GetFullPath($RootPath)
$target = [System.IO.Path]::GetFullPath((Join-Path $root 'src/main/resources/assets/appliedpackaging/textures/gui/package-storagebus-sprites.png'))
if (-not $target.StartsWith($root, [System.StringComparison]::OrdinalIgnoreCase)) {
    throw "Sprite target escaped the repository root: $target"
}
if (-not (Test-Path -LiteralPath $target -PathType Leaf)) {
    throw "Missing sprite atlas: $target"
}

# Exact 8x8 pixel reduction of the three 6x-scaled cells in the user-supplied
# reference screenshot: default Fluix, None, and selected background.
$sprites = @(
    @{
        Name = 'default'
        X = 48
        Y = 0
        Pixels = @(
            'C0C0C0 C0C0C0 C0C0C0 C0C0C0 C0C0C0 C0C0C0 C0C0C0 C0C0C0'
            'C0C0C0 915DCD 915DCD 915DCD 915DCD 915DCD 915DCD C0C0C0'
            'C0C0C0 915DCD 915DCD E2A3E3 915DCD 915DCD 915DCD C0C0C0'
            'C0C0C0 915DCD E2A3E3 915DCD 915DCD 915DCD 915DCD C0C0C0'
            'C0C0C0 915DCD 915DCD 915DCD 915DCD E2A3E3 915DCD C0C0C0'
            'C0C0C0 915DCD 915DCD 915DCD E2A3E3 915DCD 915DCD C0C0C0'
            'C0C0C0 915DCD 915DCD 915DCD 915DCD 915DCD 915DCD C0C0C0'
            'C0C0C0 C0C0C0 C0C0C0 C0C0C0 C0C0C0 C0C0C0 C0C0C0 C0C0C0'
        )
    }
    @{
        Name = 'none'
        X = 56
        Y = 0
        Pixels = @(
            'C0C0C0 C0C0C0 C0C0C0 C0C0C0 C0C0C0 C0C0C0 C0C0C0 C0C0C0'
            'C0C0C0 696D88 696D88 696D88 696D88 696D88 696D88 C0C0C0'
            'C0C0C0 696D88 ADB0C4 ADB0C4 ADB0C4 696D88 696D88 C0C0C0'
            'C0C0C0 696D88 ADB0C4 ADB0C4 696D88 ADB0C4 696D88 C0C0C0'
            'C0C0C0 696D88 ADB0C4 696D88 ADB0C4 ADB0C4 696D88 C0C0C0'
            'C0C0C0 696D88 696D88 ADB0C4 ADB0C4 ADB0C4 696D88 C0C0C0'
            'C0C0C0 696D88 696D88 696D88 696D88 696D88 696D88 C0C0C0'
            'C0C0C0 C0C0C0 C0C0C0 C0C0C0 C0C0C0 C0C0C0 C0C0C0 C0C0C0'
        )
    }
    @{
        Name = 'selected'
        X = 48
        Y = 8
        Pixels = @(
            'F2F2F2 F2F2F2 F2F2F2 F2F2F2 F2F2F2 F2F2F2 F2F2F2 F2F2F2'
            'F2F2F2 ADB0C4 ADB0C4 ADB0C4 ADB0C4 ADB0C4 ADB0C4 F2F2F2'
            'F2F2F2 ADB0C4 ADB0C4 ADB0C4 ADB0C4 ADB0C4 ADB0C4 F2F2F2'
            'F2F2F2 ADB0C4 ADB0C4 ADB0C4 ADB0C4 ADB0C4 ADB0C4 F2F2F2'
            'F2F2F2 ADB0C4 ADB0C4 ADB0C4 ADB0C4 ADB0C4 ADB0C4 F2F2F2'
            'F2F2F2 ADB0C4 ADB0C4 ADB0C4 ADB0C4 ADB0C4 ADB0C4 F2F2F2'
            'F2F2F2 ADB0C4 ADB0C4 ADB0C4 ADB0C4 ADB0C4 ADB0C4 F2F2F2'
            'F2F2F2 F2F2F2 F2F2F2 F2F2F2 F2F2F2 F2F2F2 F2F2F2 F2F2F2'
        )
    }
)

function ConvertTo-Color {
    param([string]$Rgb)
    $red = [Convert]::ToInt32($Rgb.Substring(0, 2), 16)
    $green = [Convert]::ToInt32($Rgb.Substring(2, 2), 16)
    $blue = [Convert]::ToInt32($Rgb.Substring(4, 2), 16)
    return [System.Drawing.Color]::FromArgb(255, $red, $green, $blue)
}

$bitmap = $null
$temporary = "$target.tmp.png"
try {
    $bitmap = [System.Drawing.Bitmap]::new($target)
    if ($bitmap.Width -ne 256 -or $bitmap.Height -ne 256) {
        throw "Expected a 256x256 sprite atlas, got $($bitmap.Width)x$($bitmap.Height)"
    }

    foreach ($sprite in $sprites) {
        for ($row = 0; $row -lt 8; $row++) {
            $pixels = $sprite.Pixels[$row].Split(' ', [System.StringSplitOptions]::RemoveEmptyEntries)
            if ($pixels.Count -ne 8) {
                throw "Sprite '$($sprite.Name)' row $row does not contain 8 pixels"
            }
            for ($column = 0; $column -lt 8; $column++) {
                $expected = ConvertTo-Color $pixels[$column]
                $pixelX = $sprite.X + $column
                $pixelY = $sprite.Y + $row
                $current = $bitmap.GetPixel($pixelX, $pixelY)
                $matchesExpected = $current.A -eq 255 -and $current.ToArgb() -eq $expected.ToArgb()
                if ($current.A -ne 0 -and -not $matchesExpected) {
                    throw "Sprite '$($sprite.Name)' would overwrite a non-empty atlas pixel at ($pixelX,$pixelY)"
                }
                $bitmap.SetPixel($pixelX, $pixelY, $expected)
            }
        }
    }

    # System.Drawing's DrawImage path normalizes transparent RGB. Keep the
    # original atlas's seven transparent-white pixels byte-for-pixel stable.
    $transparentWhite = [System.Drawing.Color]::FromArgb(0, 255, 255, 255)
    for ($pixelX = 16; $pixelX -le 22; $pixelX++) {
        $bitmap.SetPixel($pixelX, 46, $transparentWhite)
    }

    if (Test-Path -LiteralPath $temporary) {
        Remove-Item -LiteralPath $temporary -Force
    }
    $bitmap.Save($temporary, [System.Drawing.Imaging.ImageFormat]::Png)
    $bitmap.Dispose()
    $bitmap = $null
    Move-Item -LiteralPath $temporary -Destination $target -Force
} finally {
    if ($bitmap) { $bitmap.Dispose() }
    if (Test-Path -LiteralPath $temporary) {
        Remove-Item -LiteralPath $temporary -Force
    }
}

$hash = (Get-FileHash -LiteralPath $target -Algorithm SHA256).Hash
Write-Host "Updated package color picker sprites in $target"
Write-Host "SHA-256: $hash"
