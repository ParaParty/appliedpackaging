param(
    [string] $JarPath = "build/libs/appliedpackaging-0.1.0-dev.jar",
    [string] $LogPath = "run/logs/latest.log",
    [string] $AssetgenPath,
    [string] $RootPath = "",
    [switch] $RequireLog,
    [switch] $RequireServerWorldLoad,
    [switch] $RequireAssetContracts,
    [switch] $RequireClientSmokeScreenshots,
    [switch] $RequireCleanGit
)

$ErrorActionPreference = "Stop"

$repoRoot = if ([string]::IsNullOrWhiteSpace($RootPath)) {
    Resolve-Path (Join-Path $PSScriptRoot "..")
} else {
    Resolve-Path -LiteralPath $RootPath
}
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

function Assert-Matches {
    param(
        [string] $Text,
        [string] $Pattern,
        [string] $Message
    )
    Assert-True ($Text -match $Pattern) $Message
}

function Read-PropertiesFile {
    param([string] $Path)

    $properties = @{}
    if (-not (Test-Path $Path)) {
        Add-Fail "Properties file exists: $Path"
        return $properties
    }

    foreach ($line in Get-Content $Path) {
        $trimmed = $line.Trim()
        if ($trimmed.Length -eq 0 -or $trimmed.StartsWith("#")) {
            continue
        }
        $separator = $trimmed.IndexOf("=")
        if ($separator -lt 0) {
            continue
        }
        $key = $trimmed.Substring(0, $separator).Trim()
        $value = $trimmed.Substring($separator + 1).Trim()
        $properties[$key] = $value
    }

    return $properties
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

function Get-ReleaseDiagnosticLogLines {
    param([string] $Path)

    $filtered = [System.Collections.Generic.List[string]]::new()
    $skippingYggdrasilKeyFailure = $false
    $script:ignoredYggdrasilKeyFailures = 0

    foreach ($line in Get-Content $Path) {
        if ($line -match "Yggdrasil Key Fetcher/ERROR.*Failed to request yggdrasil public key") {
            $skippingYggdrasilKeyFailure = $true
            $script:ignoredYggdrasilKeyFailures += 1
            continue
        }

        if ($skippingYggdrasilKeyFailure) {
            if ($line -match "^\[") {
                $skippingYggdrasilKeyFailure = $false
            } else {
                continue
            }
        }

        $filtered.Add($line) | Out-Null
    }

    return $filtered
}

function Test-PngSignature {
    param([string] $Path)

    $expected = @(137, 80, 78, 71, 13, 10, 26, 10)
    $stream = [System.IO.File]::OpenRead($Path)
    try {
        if ($stream.Length -lt $expected.Count) {
            return $false
        }

        foreach ($byte in $expected) {
            if ($stream.ReadByte() -ne $byte) {
                return $false
            }
        }

        return $true
    } finally {
        $stream.Dispose()
    }
}

function Test-CleanGitWorktree {
    $git = Get-Command git -ErrorAction SilentlyContinue
    if ($null -eq $git) {
        Add-Fail "git is available for clean working tree check"
        return
    }

    $statusOutput = & $git.Source status --porcelain=v1 --untracked-files=all 2>&1
    if ($LASTEXITCODE -ne 0) {
        Add-Fail "git status --porcelain succeeds: $($statusOutput -join ' ')"
        return
    }

    $statusLines = @($statusOutput | Where-Object { -not [string]::IsNullOrWhiteSpace($_) })
    if ($statusLines.Count -eq 0) {
        Add-Pass "Git working tree is clean"
    } else {
        Add-Fail "Git working tree is clean: $($statusLines -join '; ')"
    }
}

function Get-ZipEntryText {
    param(
        [System.IO.Compression.ZipArchive] $Zip,
        [string] $EntryName
    )

    $entry = $Zip.GetEntry($EntryName)
    if ($null -eq $entry) {
        Add-Fail "Jar contains $EntryName"
        return ""
    }

    $stream = $entry.Open()
    $reader = [System.IO.StreamReader]::new($stream)
    try {
        return $reader.ReadToEnd()
    } finally {
        $reader.Dispose()
        $stream.Dispose()
    }
}

function Get-ZipEntryBytes {
    param(
        [System.IO.Compression.ZipArchive] $Zip,
        [string] $EntryName
    )

    $entry = $Zip.GetEntry($EntryName)
    if ($null -eq $entry) {
        Add-Fail "Jar contains $EntryName"
        return $null
    }

    $stream = $entry.Open()
    $memory = [System.IO.MemoryStream]::new()
    try {
        $stream.CopyTo($memory)
        return $memory.ToArray()
    } finally {
        $memory.Dispose()
        $stream.Dispose()
    }
}

function Get-Sha256Hex {
    param([byte[]] $Bytes)

    if ($null -eq $Bytes) {
        return ""
    }

    $sha = [System.Security.Cryptography.SHA256]::Create()
    try {
        return (($sha.ComputeHash($Bytes) | ForEach-Object { $_.ToString("x2") }) -join "")
    } finally {
        $sha.Dispose()
    }
}

function Assert-ZipEntryMatchesFile {
    param(
        [System.IO.Compression.ZipArchive] $Zip,
        [string] $EntryName,
        [string] $SourcePath,
        [string] $Message
    )

    $source = Resolve-Path -LiteralPath $SourcePath -ErrorAction SilentlyContinue
    if ($null -eq $source) {
        Add-Fail "Repository source exists for ${EntryName}: $SourcePath"
        return
    }

    $entryBytes = Get-ZipEntryBytes $Zip $EntryName
    if ($null -eq $entryBytes) {
        return
    }

    $sourceBytes = [System.IO.File]::ReadAllBytes($source.Path)
    $entrySha = Get-Sha256Hex $entryBytes
    $sourceSha = Get-Sha256Hex $sourceBytes
    if ($entrySha -eq $sourceSha) {
        Add-Pass $Message
    } else {
        Add-Fail "$Message (jar sha256 $entrySha, source sha256 $sourceSha)"
    }
}

function Get-RepoRelativePath {
    param([string] $Path)

    $resolvedRoot = (Resolve-Path ".").Path
    $resolvedPath = (Resolve-Path -LiteralPath $Path).Path
    return [System.IO.Path]::GetRelativePath($resolvedRoot, $resolvedPath).Replace("\", "/")
}

function Get-ReleaseResourceSourceFiles {
    $sourceRoots = @(
        "src/main/resources",
        "src/generated/resources"
    )

    $sourceByEntry = @{}
    $duplicateEntries = [System.Collections.Generic.List[string]]::new()
    foreach ($sourceRoot in $sourceRoots) {
        $resolvedRoot = Resolve-Path -LiteralPath $sourceRoot -ErrorAction SilentlyContinue
        if ($null -eq $resolvedRoot) {
            continue
        }

        $sourceFiles = @(Get-ChildItem -LiteralPath $resolvedRoot.Path -Recurse -File -ErrorAction SilentlyContinue)
        foreach ($sourceFile in $sourceFiles) {
            $entryName = [System.IO.Path]::GetRelativePath($resolvedRoot.Path, $sourceFile.FullName).Replace("\", "/")
            if ($entryName -notmatch "^(assets|data)/appliedpackaging/") {
                continue
            }

            if ($sourceByEntry.ContainsKey($entryName)) {
                $duplicateEntries.Add("$entryName from $(Get-RepoRelativePath $sourceByEntry[$entryName]) and $(Get-RepoRelativePath $sourceFile.FullName)") | Out-Null
                continue
            }

            $sourceByEntry[$entryName] = $sourceFile.FullName
        }
    }

    return @{
        SourceByEntry = $sourceByEntry
        DuplicateEntries = $duplicateEntries
    }
}

function Test-ReleaseResourceSync {
    param([System.IO.Compression.ZipArchive] $Zip)

    $resourceSources = Get-ReleaseResourceSourceFiles
    $sourceByEntry = $resourceSources.SourceByEntry
    $duplicateEntries = $resourceSources.DuplicateEntries

    if ($duplicateEntries.Count -gt 0) {
        Add-Fail "Duplicate release resource source paths: $($duplicateEntries -join '; ')"
    }

    Assert-True ($sourceByEntry.Count -gt 0) "Applied Packaging release resource source files are present"
    if ($sourceByEntry.Count -eq 0) {
        return
    }

    $missingEntries = [System.Collections.Generic.List[string]]::new()
    $mismatchedEntries = [System.Collections.Generic.List[string]]::new()
    foreach ($entryName in @($sourceByEntry.Keys | Sort-Object)) {
        $sourcePath = $sourceByEntry[$entryName]
        $entry = $Zip.GetEntry($entryName)
        if ($null -eq $entry) {
            $missingEntries.Add("$entryName <- $(Get-RepoRelativePath $sourcePath)") | Out-Null
            continue
        }

        $stream = $entry.Open()
        $memory = [System.IO.MemoryStream]::new()
        try {
            $stream.CopyTo($memory)
            $entryBytes = $memory.ToArray()
        } finally {
            $memory.Dispose()
            $stream.Dispose()
        }

        $sourceBytes = [System.IO.File]::ReadAllBytes((Resolve-Path -LiteralPath $sourcePath).Path)
        $entrySha = Get-Sha256Hex $entryBytes
        $sourceSha = Get-Sha256Hex $sourceBytes
        if ($entrySha -ne $sourceSha) {
            $mismatchedEntries.Add("$entryName (jar sha256 $entrySha, source sha256 $sourceSha)") | Out-Null
        }
    }

    if ($missingEntries.Count -eq 0 -and $mismatchedEntries.Count -eq 0) {
        Add-Pass "$($sourceByEntry.Count) Applied Packaging release resources match jar entries"
    } else {
        if ($missingEntries.Count -gt 0) {
            Add-Fail "Missing jar release resources: $($missingEntries -join '; ')"
        }
        if ($mismatchedEntries.Count -gt 0) {
            Add-Fail "Stale jar release resources: $($mismatchedEntries -join '; ')"
        }
    }
}

function Get-RecipeResultItems {
    param([object] $Recipe)

    $items = [System.Collections.Generic.List[string]]::new()
    if ($null -eq $Recipe -or -not ($Recipe -is [System.Collections.IDictionary]) -or -not $Recipe.Contains("result")) {
        return $items
    }

    $result = $Recipe["result"]
    if ($result -is [string]) {
        $items.Add($result) | Out-Null
        return $items
    }

    if ($result -is [System.Collections.IDictionary]) {
        foreach ($key in @("item", "id")) {
            if ($result.Contains($key) -and $result[$key] -is [string]) {
                $items.Add($result[$key]) | Out-Null
            }
        }
        return $items
    }

    if ($result -is [System.Collections.IEnumerable] -and -not ($result -is [string])) {
        foreach ($entry in $result) {
            if ($entry -is [System.Collections.IDictionary]) {
                foreach ($key in @("item", "id")) {
                    if ($entry.Contains($key) -and $entry[$key] -is [string]) {
                        $items.Add($entry[$key]) | Out-Null
                    }
                }
            } elseif ($entry -is [string]) {
                $items.Add($entry) | Out-Null
            }
        }
    }

    return $items
}

function Get-TranslationPlaceholders {
    param([string] $Text)

    $placeholders = [System.Collections.Generic.List[string]]::new()
    foreach ($match in [regex]::Matches($Text, '(?<!%)%(?:(\d+)\$)?[bcdeEfgGaAhHsSxXo]')) {
        $placeholders.Add($match.Value) | Out-Null
    }

    return $placeholders
}

function Test-ProductInvariants {
    $forbiddenLocalPatternItems = @(
        "appliedpackaging:package_pattern",
        "appliedpackaging:packaged_processing_pattern"
    )

    $recipeFiles = @(Get-ChildItem "src/main/resources/data/appliedpackaging/recipes" -Filter "*.json" -File -ErrorAction SilentlyContinue)
    $forbiddenRecipeOutputs = [System.Collections.Generic.List[string]]::new()
    foreach ($recipeFile in $recipeFiles) {
        try {
            $recipe = Get-Content $recipeFile.FullName -Raw | ConvertFrom-Json -Depth 100 -AsHashtable
        } catch {
            continue
        }

        foreach ($resultItem in Get-RecipeResultItems $recipe) {
            if ($resultItem -in $forbiddenLocalPatternItems) {
                $forbiddenRecipeOutputs.Add("$(Get-RepoRelativePath $recipeFile.FullName) -> $resultItem") | Out-Null
            }
        }
    }

    if ($forbiddenRecipeOutputs.Count -eq 0) {
        Add-Pass "Local pattern compatibility items are not recipe outputs"
    } else {
        Add-Fail "Local pattern compatibility items are recipe outputs: $($forbiddenRecipeOutputs -join ', ')"
    }

    $creativeTabPath = "src/main/java/com/warmthdawn/appliedpackaging/registry/APCreativeTabs.java"
    if (Test-Path -LiteralPath $creativeTabPath) {
        $creativeTabText = Get-Content -LiteralPath $creativeTabPath -Raw
        $forbiddenCreativeItems = @(
            [regex]::Matches(
                $creativeTabText,
                'output\.accept\s*\(\s*APItems\.(PACKAGE_PATTERN|PACKAGED_PROCESSING_PATTERN)\.get\s*\(\s*\)\s*\)')
                | ForEach-Object { $_.Groups[1].Value }
        )
        if ($forbiddenCreativeItems.Count -eq 0) {
            Add-Pass "Creative tab does not expose local pattern compatibility items"
        } else {
            Add-Fail "Creative tab exposes local pattern compatibility items: $($forbiddenCreativeItems -join ', ')"
        }

        Assert-True `
            ($creativeTabText -match 'output\.accept\s*\(\s*APItems\.PACKAGE_PATTERN_TERMINAL\.get\s*\(\s*\)\s*\)') `
            "Creative tab exposes package pattern terminal item"
    } else {
        Add-Fail "Creative tab source exists: $creativeTabPath"
    }

    $itemsPath = "src/main/java/com/warmthdawn/appliedpackaging/registry/APItems.java"
    if (Test-Path -LiteralPath $itemsPath) {
        $itemsText = Get-Content -LiteralPath $itemsPath -Raw
        Assert-True `
            ($itemsText -match 'PACKAGE_PATTERN_TERMINAL\s*=\s*ITEMS\.register\s*\([\s\S]*?new\s+PartItem\s*<\s*>\s*\(') `
            "Package pattern terminal item registers as an AE2 PartItem"
        Assert-True `
            ($itemsText -notmatch 'PACKAGE_PATTERN_TERMINAL\s*=\s*ITEMS\.register\s*\([\s\S]*?new\s+BlockItem\s*\(\s*APBlocks\.PACKAGE_PATTERN_TERMINAL') `
            "Package pattern terminal item is not registered as a BlockItem"
    } else {
        Add-Fail "Item registry source exists: $itemsPath"
    }
}

function Resolve-Assetgen {
    if (-not [string]::IsNullOrWhiteSpace($AssetgenPath)) {
        $resolved = Resolve-Path $AssetgenPath -ErrorAction SilentlyContinue
        if ($null -eq $resolved) {
            Add-Fail "Assetgen path exists: $AssetgenPath"
            return $null
        }
        return @{
            Kind = "PythonScript"
            Path = $resolved.Path
        }
    }

    $command = Get-Command assetgen -ErrorAction SilentlyContinue
    if ($null -ne $command) {
        return @{
            Kind = "Command"
            Path = $command.Source
        }
    }

    $skillAssetgen = Join-Path $HOME ".codex/skills/minecraft-mod-asset-generation/scripts/assetgen"
    if (Test-Path $skillAssetgen) {
        return @{
            Kind = "PythonScript"
            Path = (Resolve-Path $skillAssetgen).Path
        }
    }

    return $null
}

function Invoke-AssetgenValidateContract {
    param(
        [hashtable] $Assetgen,
        [string] $ContractPath
    )

    if ($Assetgen.Kind -eq "Command") {
        $output = & $Assetgen.Path validate-contract $ContractPath 2>&1
        return @{
            ExitCode = $LASTEXITCODE
            Output = $output
        }
    }

    $python = Get-Command python -ErrorAction SilentlyContinue
    if ($null -eq $python) {
        return @{
            ExitCode = 1
            Output = "python command not found for assetgen script"
        }
    }

    $output = & $python.Source $Assetgen.Path validate-contract $ContractPath 2>&1
    return @{
        ExitCode = $LASTEXITCODE
        Output = $output
    }
}

Write-Host "Applied Packaging release audit" -ForegroundColor Cyan
Write-Host "Repository: $repoRoot"

if ($RequireCleanGit) {
    Test-CleanGitWorktree
}

$projectProperties = Read-PropertiesFile "gradle.properties"
$requiredProjectProperties = @(
    "mod_id",
    "mod_name",
    "mod_version",
    "mod_authors",
    "mod_license",
    "minecraft_version_range",
    "forge_version_range",
    "loader_version_range",
    "ae2_version_range",
    "guideme_version_range"
)
foreach ($propertyName in $requiredProjectProperties) {
    Assert-True $projectProperties.ContainsKey($propertyName) "gradle.properties defines $propertyName"
}

$resolvedJar = Resolve-Path $JarPath -ErrorAction SilentlyContinue
if ($null -eq $resolvedJar) {
    Add-Fail "Release jar exists at $JarPath"
} else {
    Add-Pass "Release jar exists at $JarPath"
    if ($projectProperties.ContainsKey("mod_id") -and $projectProperties.ContainsKey("mod_version")) {
        $expectedJarName = "$($projectProperties["mod_id"])-$($projectProperties["mod_version"]).jar"
        $actualJarName = [System.IO.Path]::GetFileName($resolvedJar.Path)
        Assert-True ($actualJarName -eq $expectedJarName) "Jar filename matches mod_id and mod_version ($expectedJarName)"
    }

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

        $sourceSyncedEntries = @(
            @{
                EntryName = "README.md"
                SourcePath = "README.md"
                Message = "Jar README.md matches repository README.md"
            },
            @{
                EntryName = "CHANGELOG.md"
                SourcePath = "CHANGELOG.md"
                Message = "Jar CHANGELOG.md matches repository CHANGELOG.md"
            },
            @{
                EntryName = "LICENSE.md"
                SourcePath = "LICENSE.md"
                Message = "Jar LICENSE.md matches repository LICENSE.md"
            },
            @{
                EntryName = "assets/appliedpackaging/lang/en_us.json"
                SourcePath = "src/main/resources/assets/appliedpackaging/lang/en_us.json"
                Message = "Jar en_us.json matches source en_us.json"
            },
            @{
                EntryName = "assets/appliedpackaging/lang/zh_cn.json"
                SourcePath = "src/main/resources/assets/appliedpackaging/lang/zh_cn.json"
                Message = "Jar zh_cn.json matches source zh_cn.json"
            }
        )
        foreach ($sourceSyncedEntry in $sourceSyncedEntries) {
            Assert-ZipEntryMatchesFile `
                -Zip $zip `
                -EntryName $sourceSyncedEntry.EntryName `
                -SourcePath $sourceSyncedEntry.SourcePath `
                -Message $sourceSyncedEntry.Message
        }
        Test-ReleaseResourceSync $zip

        $modsTomlText = Get-ZipEntryText $zip "META-INF/mods.toml"
        $manifestText = Get-ZipEntryText $zip "META-INF/MANIFEST.MF"
        if ($modsTomlText.Length -gt 0) {
            Assert-Matches $modsTomlText "modId=`"$([regex]::Escape($projectProperties["mod_id"]))`"" "mods.toml modId matches gradle.properties"
            Assert-Matches $modsTomlText "version=`"$([regex]::Escape($projectProperties["mod_version"]))`"" "mods.toml version matches gradle.properties"
            Assert-Matches $modsTomlText "displayName=`"$([regex]::Escape($projectProperties["mod_name"]))`"" "mods.toml displayName matches gradle.properties"
            Assert-Matches $modsTomlText "authors=`"$([regex]::Escape($projectProperties["mod_authors"]))`"" "mods.toml authors match gradle.properties"
            Assert-Matches $modsTomlText "license=`"$([regex]::Escape($projectProperties["mod_license"]))`"" "mods.toml license matches gradle.properties"
            Assert-Matches $modsTomlText "loaderVersion=`"$([regex]::Escape($projectProperties["loader_version_range"]))`"" "mods.toml loader version range matches gradle.properties"
            Assert-Matches $modsTomlText "modId=`"forge`"[\s\S]*?versionRange=`"$([regex]::Escape($projectProperties["forge_version_range"]))`"" "mods.toml Forge dependency range matches gradle.properties"
            Assert-Matches $modsTomlText "modId=`"minecraft`"[\s\S]*?versionRange=`"$([regex]::Escape($projectProperties["minecraft_version_range"]))`"" "mods.toml Minecraft dependency range matches gradle.properties"
            Assert-Matches $modsTomlText "modId=`"ae2`"[\s\S]*?versionRange=`"$([regex]::Escape($projectProperties["ae2_version_range"]))`"" "mods.toml AE2 dependency range matches gradle.properties"
            Assert-Matches $modsTomlText "modId=`"guideme`"[\s\S]*?versionRange=`"$([regex]::Escape($projectProperties["guideme_version_range"]))`"" "mods.toml GuideME dependency range matches gradle.properties"
        }
        if ($manifestText.Length -gt 0) {
            Assert-Matches $manifestText "Specification-Title: $([regex]::Escape($projectProperties["mod_name"]))" "manifest specification title matches gradle.properties"
            Assert-Matches $manifestText "Specification-Version: $([regex]::Escape($projectProperties["mod_version"]))" "manifest specification version matches gradle.properties"
            Assert-Matches $manifestText "Implementation-Title: $([regex]::Escape($projectProperties["mod_name"]))" "manifest implementation title matches gradle.properties"
            Assert-Matches $manifestText "Implementation-Version: $([regex]::Escape($projectProperties["mod_version"]))" "manifest implementation version matches gradle.properties"
            Assert-Matches $manifestText "Implementation-Vendor: $([regex]::Escape($projectProperties["mod_authors"]))" "manifest implementation vendor matches gradle.properties"
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

Test-ProductInvariants

$pngFiles = @(Get-ChildItem "src/main/resources/assets/appliedpackaging" -Recurse -Filter "*.png" -File)
Assert-True ($pngFiles.Count -gt 0) "PNG resources are present"
$emptyPng = @($pngFiles | Where-Object { $_.Length -le 0 })
if ($emptyPng.Count -eq 0) {
    Add-Pass "$($pngFiles.Count) PNG resources are non-empty"
} else {
    Add-Fail "Empty PNG resources: $($emptyPng.FullName -join ', ')"
}

if ($RequireClientSmokeScreenshots) {
    $expectedClientSmokeScreenshots = @(
        "appliedpackaging-client-smoke-package_assembler.png",
        "appliedpackaging-client-smoke-me_packager.png",
        "appliedpackaging-client-smoke-package_pattern_terminal.png",
        "appliedpackaging-client-smoke-package_storage_bus.png",
        "appliedpackaging-client-smoke-package_export_bus.png",
        "appliedpackaging-client-smoke-package_unpacking_bus.png"
    )

    $missingScreenshots = [System.Collections.Generic.List[string]]::new()
    $invalidScreenshots = [System.Collections.Generic.List[string]]::new()
    foreach ($screenshotName in $expectedClientSmokeScreenshots) {
        $screenshotPath = Join-Path "run/screenshots" $screenshotName
        $screenshot = Get-Item $screenshotPath -ErrorAction SilentlyContinue
        if ($null -eq $screenshot) {
            $missingScreenshots.Add($screenshotPath) | Out-Null
            continue
        }

        if ($screenshot.Length -le 0 -or -not (Test-PngSignature $screenshot.FullName)) {
            $invalidScreenshots.Add($screenshotPath) | Out-Null
        }
    }

    if ($missingScreenshots.Count -eq 0 -and $invalidScreenshots.Count -eq 0) {
        Add-Pass "$($expectedClientSmokeScreenshots.Count) client smoke screenshots are present and valid PNG files"
    } else {
        if ($missingScreenshots.Count -gt 0) {
            Add-Fail "Missing client smoke screenshots: $($missingScreenshots -join ', ')"
        }
        if ($invalidScreenshots.Count -gt 0) {
            Add-Fail "Invalid client smoke screenshots: $($invalidScreenshots -join ', ')"
        }
    }
}

$contractFiles = @(Get-ChildItem "docs/assets/contracts" -Filter "*.yaml" -File -ErrorAction SilentlyContinue)
Assert-True ($contractFiles.Count -gt 0) "Asset contracts are present"
if ($contractFiles.Count -gt 0) {
    $assetgen = Resolve-Assetgen
    if ($null -eq $assetgen) {
        if ($RequireAssetContracts) {
            Add-Fail "assetgen is available for required asset contract validation"
        } else {
            Add-Warn "assetgen not found; skipping asset contract validation"
        }
    } else {
        $badContracts = [System.Collections.Generic.List[string]]::new()
        foreach ($contract in $contractFiles) {
            $result = Invoke-AssetgenValidateContract $assetgen $contract.FullName
            if ($result.ExitCode -ne 0) {
                $badContracts.Add("$($contract.FullName): $($result.Output -join ' ')") | Out-Null
            }
        }
        if ($badContracts.Count -eq 0) {
            Add-Pass "$($contractFiles.Count) asset contracts validate with assetgen"
        } else {
            Add-Fail "Asset contract validation failed: $($badContracts -join '; ')"
        }
    }
}

$enPath = "src/main/resources/assets/appliedpackaging/lang/en_us.json"
$zhPath = "src/main/resources/assets/appliedpackaging/lang/zh_cn.json"
if ((Test-Path $enPath) -and (Test-Path $zhPath)) {
    $enLang = Get-Content $enPath -Raw | ConvertFrom-Json -AsHashtable
    $zhLang = Get-Content $zhPath -Raw | ConvertFrom-Json -AsHashtable
    $enKeys = @($enLang.Keys | Sort-Object)
    $zhKeys = @($zhLang.Keys | Sort-Object)
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

    $placeholderMismatches = [System.Collections.Generic.List[string]]::new()
    foreach ($key in $enKeys) {
        if (-not $zhLang.Contains($key)) {
            continue
        }

        $enValue = $enLang[$key]
        $zhValue = $zhLang[$key]
        if (-not ($enValue -is [string]) -or -not ($zhValue -is [string])) {
            continue
        }

        $enPlaceholders = @(Get-TranslationPlaceholders $enValue)
        $zhPlaceholders = @(Get-TranslationPlaceholders $zhValue)
        if (($enPlaceholders -join "|") -ne ($zhPlaceholders -join "|")) {
            $placeholderMismatches.Add("$key en=[$($enPlaceholders -join ',')] zh=[$($zhPlaceholders -join ',')]") | Out-Null
        }
    }

    if ($placeholderMismatches.Count -eq 0) {
        Add-Pass "English and Simplified Chinese language placeholders match"
    } else {
        Add-Fail "Language placeholder mismatches: $($placeholderMismatches -join '; ')"
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
    $diagnosticLogText = (Get-ReleaseDiagnosticLogLines $resolvedLog.Path) -join "`n"
    if ($ignoredYggdrasilKeyFailures -gt 0) {
        Add-Warn "Ignored $ignoredYggdrasilKeyFailures external Yggdrasil public-key fetch failure(s)"
    }
    if ($diagnosticLogText -match $badLogPattern) {
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
