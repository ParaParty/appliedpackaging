param(
    [string] $ManifestPath,
    [string] $JarPath,
    [switch] $RequireCleanGit
)

$ErrorActionPreference = "Stop"

$repoRoot = (Resolve-Path (Join-Path $PSScriptRoot "..")).Path
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

function Read-PropertiesFile {
    param([string] $Path)

    $properties = @{}
    if (-not (Test-Path $Path)) {
        throw "Properties file not found: $Path"
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

function Require-Property {
    param(
        [hashtable] $Properties,
        [string] $Name
    )

    if (-not $Properties.ContainsKey($Name) -or [string]::IsNullOrWhiteSpace($Properties[$Name])) {
        throw "gradle.properties is missing required property: $Name"
    }

    return $Properties[$Name]
}

function Invoke-Git {
    param([string[]] $Arguments)

    $git = Get-Command git -ErrorAction SilentlyContinue
    if ($null -eq $git) {
        throw "git command not found"
    }

    $output = & $git.Source @Arguments 2>&1
    if ($LASTEXITCODE -ne 0) {
        throw "git $($Arguments -join ' ') failed: $($output -join ' ')"
    }

    return @($output)
}

function Get-ManifestValue {
    param(
        [object] $Root,
        [string] $Path
    )

    $current = $Root
    foreach ($segment in $Path.Split(".")) {
        if ($null -eq $current) {
            Add-Fail "Manifest contains $Path"
            return $null
        }

        $property = $current.PSObject.Properties[$segment]
        if ($null -eq $property) {
            Add-Fail "Manifest contains $Path"
            return $null
        }

        $current = $property.Value
    }

    return $current
}

function Assert-ManifestText {
    param(
        [object] $Manifest,
        [string] $Path,
        [string] $Expected
    )

    $actual = Get-ManifestValue $Manifest $Path
    if ($null -eq $actual) {
        return
    }

    if ([string]$actual -eq $Expected) {
        Add-Pass "$Path matches expected value"
    } else {
        Add-Fail "$Path expected '$Expected' but was '$actual'"
    }
}

function Assert-ManifestInt64 {
    param(
        [object] $Manifest,
        [string] $Path,
        [long] $Expected
    )

    $actual = Get-ManifestValue $Manifest $Path
    if ($null -eq $actual) {
        return
    }

    try {
        $actualInt = [long]$actual
    } catch {
        Add-Fail "$Path is not an integer: $actual"
        return
    }

    if ($actualInt -eq $Expected) {
        Add-Pass "$Path matches expected value"
    } else {
        Add-Fail "$Path expected '$Expected' but was '$actualInt'"
    }
}

function Assert-ManifestBool {
    param(
        [object] $Manifest,
        [string] $Path,
        [bool] $Expected
    )

    $actual = Get-ManifestValue $Manifest $Path
    if ($null -eq $actual) {
        return
    }

    if ([bool]$actual -eq $Expected) {
        Add-Pass "$Path matches expected value"
    } else {
        Add-Fail "$Path expected '$Expected' but was '$actual'"
    }
}

function Assert-ManifestUtcInstant {
    param(
        [object] $Manifest,
        [string] $Path,
        [string] $Expected
    )

    $actual = Get-ManifestValue $Manifest $Path
    if ($null -eq $actual) {
        return
    }

    try {
        if ($actual -is [datetime]) {
            $actualTicks = $actual.ToUniversalTime().Ticks
        } else {
            $actualTicks = [DateTimeOffset]::Parse([string]$actual).UtcDateTime.Ticks
        }
        $expectedTicks = [DateTimeOffset]::Parse($Expected).UtcDateTime.Ticks
    } catch {
        Add-Fail "$Path is not a parseable UTC instant: $actual"
        return
    }

    if ($actualTicks -eq $expectedTicks) {
        Add-Pass "$Path matches expected value"
    } else {
        Add-Fail "$Path expected '$Expected' but was '$actual'"
    }
}

function Assert-StringArraysEqual {
    param(
        [string[]] $Actual,
        [string[]] $Expected,
        [string] $Message
    )

    if ($Actual.Count -ne $Expected.Count) {
        Add-Fail "$Message expected $($Expected.Count) line(s) but was $($Actual.Count)"
        return
    }

    for ($index = 0; $index -lt $Actual.Count; $index += 1) {
        if ($Actual[$index] -ne $Expected[$index]) {
            Add-Fail "$Message differs at line $($index + 1): expected '$($Expected[$index])' but was '$($Actual[$index])'"
            return
        }
    }

    Add-Pass $Message
}

$properties = Read-PropertiesFile "gradle.properties"
$modId = Require-Property $properties "mod_id"
$modVersion = Require-Property $properties "mod_version"

if ([string]::IsNullOrWhiteSpace($JarPath)) {
    $JarPath = Join-Path "build/libs" "$modId-$modVersion.jar"
}

if ([string]::IsNullOrWhiteSpace($ManifestPath)) {
    $ManifestPath = Join-Path "build/release" "$modId-$modVersion-release-manifest.json"
}

Write-Host "Applied Packaging release manifest audit" -ForegroundColor Cyan
Write-Host "Repository: $repoRoot"
Write-Host "Manifest: $ManifestPath"

$resolvedManifest = Resolve-Path $ManifestPath -ErrorAction SilentlyContinue
if ($null -eq $resolvedManifest) {
    Add-Fail "Release manifest exists at $ManifestPath"
} else {
    Add-Pass "Release manifest exists at $ManifestPath"
}

$resolvedJar = Resolve-Path $JarPath -ErrorAction SilentlyContinue
if ($null -eq $resolvedJar) {
    Add-Fail "Release jar exists at $JarPath"
}

if ($failures.Count -eq 0) {
    $manifestItem = Get-Item $resolvedManifest.Path
    $jarItem = Get-Item $resolvedJar.Path
    $manifest = Get-Content $manifestItem.FullName -Raw | ConvertFrom-Json -Depth 32

    $expectedJarName = "$modId-$modVersion.jar"
    if ($jarItem.Name -eq $expectedJarName) {
        Add-Pass "Release jar filename matches gradle.properties"
    } else {
        Add-Fail "Release jar filename expected '$expectedJarName' but was '$($jarItem.Name)'"
    }

    $jarHash = (Get-FileHash -Algorithm SHA256 -Path $jarItem.FullName).Hash.ToLowerInvariant()
    $relativeJarPath = [System.IO.Path]::GetRelativePath($repoRoot, $jarItem.FullName).Replace("\", "/")
    $relativeManifestPath = [System.IO.Path]::GetRelativePath($repoRoot, $manifestItem.FullName).Replace("\", "/")

    $commit = @(Invoke-Git @("rev-parse", "HEAD"))[0]
    $shortCommit = @(Invoke-Git @("rev-parse", "--short", "HEAD"))[0]
    $branch = @(Invoke-Git @("rev-parse", "--abbrev-ref", "HEAD"))[0]
    $statusLines = @((Invoke-Git @("status", "--porcelain=v1", "--untracked-files=all")) | Where-Object { -not [string]::IsNullOrWhiteSpace($_) })
    $isClean = $statusLines.Count -eq 0

    if ($RequireCleanGit -and -not $isClean) {
        Add-Fail "Git working tree is clean: $($statusLines -join '; ')"
    } elseif ($RequireCleanGit) {
        Add-Pass "Git working tree is clean"
    }

    Assert-ManifestInt64 $manifest "schemaVersion" 1
    $generatedAtUtc = Get-ManifestValue $manifest "generatedAtUtc"
    if ($null -ne $generatedAtUtc) {
        try {
            [DateTimeOffset]::Parse([string]$generatedAtUtc) | Out-Null
            Add-Pass "generatedAtUtc is parseable"
        } catch {
            Add-Fail "generatedAtUtc is not parseable: $generatedAtUtc"
        }
    }

    Assert-ManifestText $manifest "mod.id" $modId
    Assert-ManifestText $manifest "mod.name" (Require-Property $properties "mod_name")
    Assert-ManifestText $manifest "mod.version" $modVersion
    Assert-ManifestText $manifest "mod.group" (Require-Property $properties "mod_group_id")
    Assert-ManifestText $manifest "mod.authors" (Require-Property $properties "mod_authors")
    Assert-ManifestText $manifest "mod.license" (Require-Property $properties "mod_license")
    Assert-ManifestText $manifest "mod.description" $properties["mod_description"]

    Assert-ManifestText $manifest "target.minecraftVersion" (Require-Property $properties "minecraft_version")
    Assert-ManifestText $manifest "target.minecraftVersionRange" (Require-Property $properties "minecraft_version_range")
    Assert-ManifestText $manifest "target.forgeVersion" (Require-Property $properties "forge_version")
    Assert-ManifestText $manifest "target.forgeVersionRange" (Require-Property $properties "forge_version_range")
    Assert-ManifestText $manifest "target.loaderVersionRange" (Require-Property $properties "loader_version_range")
    Assert-ManifestInt64 $manifest "target.javaVersion" 17

    Assert-ManifestText $manifest "dependencies.ae2Version" (Require-Property $properties "ae2_version")
    Assert-ManifestText $manifest "dependencies.ae2VersionRange" (Require-Property $properties "ae2_version_range")
    Assert-ManifestText $manifest "dependencies.guideMeVersion" (Require-Property $properties "guideme_version")
    Assert-ManifestText $manifest "dependencies.guideMeVersionRange" (Require-Property $properties "guideme_version_range")

    Assert-ManifestText $manifest "artifact.path" $relativeJarPath
    Assert-ManifestText $manifest "artifact.fileName" $jarItem.Name
    Assert-ManifestInt64 $manifest "artifact.sizeBytes" $jarItem.Length
    Assert-ManifestText $manifest "artifact.sha256" $jarHash
    Assert-ManifestUtcInstant $manifest "artifact.lastWriteTimeUtc" $jarItem.LastWriteTimeUtc.ToString("o")

    Assert-ManifestText $manifest "git.commit" $commit
    Assert-ManifestText $manifest "git.shortCommit" $shortCommit
    Assert-ManifestText $manifest "git.branch" $branch
    Assert-ManifestBool $manifest "git.clean" $isClean

    $manifestStatus = Get-ManifestValue $manifest "git.statusPorcelain"
    if ($null -ne $manifestStatus) {
        Assert-StringArraysEqual @($manifestStatus | ForEach-Object { [string]$_ }) $statusLines "git.statusPorcelain matches current git status"
    }

    Assert-ManifestText $manifest "release.manifestPath" $relativeManifestPath
    Assert-ManifestText $manifest "release.generatedBy" "scripts/write-release-manifest.ps1"
}

if ($failures.Count -gt 0) {
    Write-Host ""
    Write-Host "Release manifest audit failed with $($failures.Count) issue(s):" -ForegroundColor Red
    foreach ($failure in $failures) {
        Write-Host " - $failure" -ForegroundColor Red
    }
    exit 1
}

Write-Host ""
Write-Host "Release manifest audit passed." -ForegroundColor Green
