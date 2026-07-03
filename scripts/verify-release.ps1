param(
    [string] $JarPath = "build/libs/appliedpackaging-0.1.0-dev.jar",
    [string] $LogPath = "run/logs/latest.log",
    [switch] $RequireLog,
    [switch] $RequireServerWorldLoad
)

$ErrorActionPreference = "Stop"

$repoRoot = Resolve-Path (Join-Path $PSScriptRoot "..")
Set-Location $repoRoot

$failures = [System.Collections.Generic.List[string]]::new()

function Add-Pass {
    param([string] $Message)
    Write-Host "[PASS] $Message" -ForegroundColor Green
}

function Add-Warn {
    param([string] $Message)
    Write-Host "[WARN] $Message" -ForegroundColor Yellow
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

function Get-TextureValues {
    param([object] $Node)

    $values = [System.Collections.Generic.List[string]]::new()
    if ($null -eq $Node) {
        return $values
    }

    if ($Node -is [System.Collections.IDictionary]) {
        foreach ($key in $Node.Keys) {
            $value = $Node[$key]
            if ($key -eq "textures" -and $value -is [System.Collections.IDictionary]) {
                foreach ($texture in $value.Values) {
                    if ($texture -is [string]) {
                        $values.Add($texture) | Out-Null
                    }
                }
            } else {
                foreach ($nested in Get-TextureValues $value) {
                    $values.Add($nested) | Out-Null
                }
            }
        }
        return $values
    }

    if ($Node -is [System.Collections.IEnumerable] -and -not ($Node -is [string])) {
        foreach ($item in $Node) {
            foreach ($nested in Get-TextureValues $item) {
                $values.Add($nested) | Out-Null
            }
        }
    }

    return $values
}

Write-Host "Applied Packaging release audit" -ForegroundColor Cyan
Write-Host "Repository: $repoRoot"

$resolvedJar = Resolve-Path $JarPath -ErrorAction SilentlyContinue
if ($null -eq $resolvedJar) {
    Add-Fail "Release jar exists at $JarPath"
} else {
    Add-Pass "Release jar exists at $JarPath"

    Add-Type -AssemblyName System.IO.Compression.FileSystem
    $zip = [System.IO.Compression.ZipFile]::OpenRead($resolvedJar.Path)
    try {
        $entries = @($zip.Entries | ForEach-Object { $_.FullName })
        $entrySet = [System.Collections.Generic.HashSet[string]]::new([StringComparer]::OrdinalIgnoreCase)
        foreach ($entry in $entries) {
            $entrySet.Add($entry) | Out-Null
        }

        $requiredEntries = @(
            "META-INF/mods.toml",
            "META-INF/MANIFEST.MF",
            "LICENSE.md",
            "README.md",
            "CHANGELOG.md",
            "assets/appliedpackaging/logo.png"
        )

        foreach ($entry in $requiredEntries) {
            Assert-True $entrySet.Contains($entry) "Jar contains $entry"
        }

        $forbiddenEntryPattern = "(?i)(^|/)(com/warmthdawn/appliedpackaging/client/ClientSmokeRunner|com/warmthdawn/appliedpackaging/gametest/|build/|tmp/|docs/assets/|run/)|reference|preview"
        $forbiddenEntries = @($entries | Where-Object { $_ -match $forbiddenEntryPattern })
        if ($forbiddenEntries.Count -eq 0) {
            Add-Pass "Jar contains no dev/test/reference/preview entries"
        } else {
            Add-Fail "Jar contains forbidden entries: $($forbiddenEntries -join ', ')"
        }

        $textExtensions = [System.Collections.Generic.HashSet[string]]::new([StringComparer]::OrdinalIgnoreCase)
        foreach ($extension in @(".json", ".mcmeta", ".toml", ".md", ".txt", ".properties", ".lang")) {
            $textExtensions.Add($extension) | Out-Null
        }

        $forbiddenTextPattern = "(?i)(E:\\|C:\\Users|build/reference|build/asset-reference|\.codex|asset-reference)"
        $leakedText = [System.Collections.Generic.List[string]]::new()
        foreach ($entry in $zip.Entries) {
            $extension = [System.IO.Path]::GetExtension($entry.FullName)
            if (-not $textExtensions.Contains($extension)) {
                continue
            }
            $stream = $entry.Open()
            $reader = [System.IO.StreamReader]::new($stream)
            try {
                $content = $reader.ReadToEnd()
                if ($content -match $forbiddenTextPattern) {
                    $leakedText.Add($entry.FullName) | Out-Null
                }
            } finally {
                $reader.Dispose()
                $stream.Dispose()
            }
        }

        if ($leakedText.Count -eq 0) {
            Add-Pass "Jar text resources contain no local absolute/reference paths"
        } else {
            Add-Fail "Jar text resources leak local/reference paths: $($leakedText -join ', ')"
        }
    } finally {
        $zip.Dispose()
    }
}

$jsonFiles = @(Get-ChildItem "src/main/resources" -Recurse -Filter "*.json" -File)
Assert-True ($jsonFiles.Count -gt 0) "Resource JSON files are present"
$badJson = [System.Collections.Generic.List[string]]::new()
foreach ($file in $jsonFiles) {
    try {
        Get-Content $file.FullName -Raw | ConvertFrom-Json -Depth 100 | Out-Null
    } catch {
        $badJson.Add($file.FullName) | Out-Null
    }
}
if ($badJson.Count -eq 0) {
    Add-Pass "$($jsonFiles.Count) resource JSON files parse"
} else {
    Add-Fail "Invalid JSON files: $($badJson -join ', ')"
}

$pngFiles = @(Get-ChildItem "src/main/resources/assets/appliedpackaging" -Recurse -Filter "*.png" -File)
Assert-True ($pngFiles.Count -gt 0) "PNG resources are present"
$emptyPng = @($pngFiles | Where-Object { $_.Length -le 0 })
if ($emptyPng.Count -eq 0) {
    Add-Pass "$($pngFiles.Count) PNG resources are non-empty"
} else {
    Add-Fail "Empty PNG resources: $($emptyPng.FullName -join ', ')"
}

$enPath = "src/main/resources/assets/appliedpackaging/lang/en_us.json"
$zhPath = "src/main/resources/assets/appliedpackaging/lang/zh_cn.json"
if ((Test-Path $enPath) -and (Test-Path $zhPath)) {
    $enKeys = @(((Get-Content $enPath -Raw | ConvertFrom-Json).PSObject.Properties | ForEach-Object { $_.Name }) | Sort-Object)
    $zhKeys = @(((Get-Content $zhPath -Raw | ConvertFrom-Json).PSObject.Properties | ForEach-Object { $_.Name }) | Sort-Object)
    $missingZh = @($enKeys | Where-Object { $_ -notin $zhKeys })
    $missingEn = @($zhKeys | Where-Object { $_ -notin $enKeys })
    if ($missingZh.Count -eq 0 -and $missingEn.Count -eq 0) {
        Add-Pass "English and Simplified Chinese language keys match ($($enKeys.Count) keys)"
    } else {
        if ($missingZh.Count -gt 0) {
            Add-Fail "zh_cn.json missing keys: $($missingZh -join ', ')"
        }
        if ($missingEn.Count -gt 0) {
            Add-Fail "en_us.json missing keys: $($missingEn -join ', ')"
        }
    }
} else {
    Add-Fail "Both en_us.json and zh_cn.json language files exist"
}

$modelFiles = @(Get-ChildItem "src/main/resources/assets/appliedpackaging/models" -Recurse -Filter "*.json" -File)
$missingTextures = [System.Collections.Generic.List[string]]::new()
foreach ($file in $modelFiles) {
    $json = Get-Content $file.FullName -Raw | ConvertFrom-Json -Depth 100 -AsHashtable
    foreach ($texture in Get-TextureValues $json) {
        if ($texture.StartsWith("#")) {
            continue
        }
        if (-not $texture.StartsWith("appliedpackaging:", [StringComparison]::OrdinalIgnoreCase)) {
            continue
        }
        $relativeTexture = $texture.Substring("appliedpackaging:".Length)
        $texturePath = Join-Path "src/main/resources/assets/appliedpackaging/textures" ($relativeTexture + ".png")
        if (-not (Test-Path $texturePath)) {
            $missingTextures.Add("$($file.FullName) -> $texture") | Out-Null
        }
    }
}
if ($missingTextures.Count -eq 0) {
    Add-Pass "Applied Packaging model texture references resolve"
} else {
    Add-Fail "Missing model texture references: $($missingTextures -join '; ')"
}

$resolvedLog = Resolve-Path $LogPath -ErrorAction SilentlyContinue
if ($null -eq $resolvedLog) {
    if ($RequireLog -or $RequireServerWorldLoad) {
        Add-Fail "Log exists at $LogPath"
    } else {
        Add-Warn "No log found at $LogPath; skipping log checks"
    }
} else {
    Add-Pass "Log exists at $LogPath"
    $badLogPattern = "ERROR|FATAL|ClientSmokeRunner|NoClassDefFoundError|ClassNotFoundException|InvocationTargetException|IllegalStateException|Dist\.CLIENT|OnlyIn|Missing model|Unable to load model|missing texture|Exception|Crash|crash"
    if (Select-String -Path $resolvedLog.Path -Pattern $badLogPattern -Quiet) {
        Add-Fail "Log contains release-blocking diagnostic keywords"
    } else {
        Add-Pass "Log contains no release-blocking diagnostic keywords"
    }

    if ($RequireServerWorldLoad) {
        $logText = Get-Content $resolvedLog.Path -Raw
        Assert-True ($logText -match "Applied Packaging initialized") "Server log shows Applied Packaging initialization"
        Assert-True ($logText -match "Preparing level `"world`"") "Server log shows world preparation"
        Assert-True ($logText -match "Done \([0-9.]+s\)! For help, type `"help`"") "Server log shows full dedicated server world-load"
    }
}

if ($failures.Count -gt 0) {
    Write-Host ""
    Write-Host "Release audit failed with $($failures.Count) issue(s):" -ForegroundColor Red
    foreach ($failure in $failures) {
        Write-Host " - $failure" -ForegroundColor Red
    }
    exit 1
}

Write-Host ""
Write-Host "Release audit passed." -ForegroundColor Green
