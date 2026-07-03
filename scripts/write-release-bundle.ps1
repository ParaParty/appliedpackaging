param(
    [string] $JarPath,
    [string] $ManifestPath,
    [string] $OutputDir = "build/release",
    [string] $BundlePath,
    [switch] $RequireCleanGit
)

$ErrorActionPreference = "Stop"

$repoRoot = (Resolve-Path (Join-Path $PSScriptRoot "..")).Path
Set-Location $repoRoot

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

function Get-RelativePath {
    param([string] $Path)
    return [System.IO.Path]::GetRelativePath($repoRoot, $Path).Replace("\", "/")
}

function Resolve-OutputPath {
    param([string] $Path)

    if ([System.IO.Path]::IsPathRooted($Path)) {
        return $Path
    }

    return (Join-Path (Get-Location) $Path)
}

$properties = Read-PropertiesFile "gradle.properties"
$modId = Require-Property $properties "mod_id"
$modVersion = Require-Property $properties "mod_version"
$bundleRoot = "$modId-$modVersion"

if ([string]::IsNullOrWhiteSpace($JarPath)) {
    $JarPath = Join-Path "build/libs" "$modId-$modVersion.jar"
}

if ([string]::IsNullOrWhiteSpace($ManifestPath)) {
    $ManifestPath = Join-Path "build/release" "$modId-$modVersion-release-manifest.json"
}

if ([string]::IsNullOrWhiteSpace($BundlePath)) {
    New-Item -ItemType Directory -Path $OutputDir -Force | Out-Null
    $BundlePath = Join-Path $OutputDir "$modId-$modVersion-release-bundle.zip"
} else {
    $parent = Split-Path -Parent $BundlePath
    if (-not [string]::IsNullOrWhiteSpace($parent)) {
        New-Item -ItemType Directory -Path $parent -Force | Out-Null
    }
}

$statusLines = @(Invoke-Git @("status", "--porcelain=v1", "--untracked-files=all"))
if ($RequireCleanGit -and $statusLines.Count -gt 0) {
    throw "Git working tree is not clean: $($statusLines -join '; ')"
}

$sourceFiles = [ordered]@{
    "$bundleRoot/$modId-$modVersion.jar" = $JarPath
    "$bundleRoot/$modId-$modVersion-release-manifest.json" = $ManifestPath
    "$bundleRoot/README.md" = "README.md"
    "$bundleRoot/CHANGELOG.md" = "CHANGELOG.md"
    "$bundleRoot/LICENSE.md" = "LICENSE.md"
}

$resolvedFiles = [ordered]@{}
foreach ($entryName in $sourceFiles.Keys) {
    $resolved = Resolve-Path $sourceFiles[$entryName] -ErrorAction SilentlyContinue
    if ($null -eq $resolved) {
        throw "Release bundle source file not found: $($sourceFiles[$entryName])"
    }
    $resolvedFiles[$entryName] = (Get-Item $resolved.Path)
}

$resolvedJar = $resolvedFiles["$bundleRoot/$modId-$modVersion.jar"]
if ($resolvedJar.Name -ne "$modId-$modVersion.jar") {
    throw "Release jar name '$($resolvedJar.Name)' does not match expected '$modId-$modVersion.jar'"
}

$checksums = [System.Collections.Generic.List[string]]::new()
foreach ($entryName in $resolvedFiles.Keys) {
    $hash = (Get-FileHash -Algorithm SHA256 -Path $resolvedFiles[$entryName].FullName).Hash.ToLowerInvariant()
    $relativeName = $entryName.Substring($bundleRoot.Length + 1)
    $checksums.Add("$hash  $relativeName") | Out-Null
}

$resolvedBundlePath = Resolve-OutputPath $BundlePath
if (Test-Path $resolvedBundlePath) {
    Remove-Item -LiteralPath $resolvedBundlePath -Force
}

Add-Type -AssemblyName System.IO.Compression.FileSystem
$zip = [System.IO.Compression.ZipFile]::Open($resolvedBundlePath, [System.IO.Compression.ZipArchiveMode]::Create)
try {
    foreach ($entryName in $resolvedFiles.Keys) {
        [System.IO.Compression.ZipFileExtensions]::CreateEntryFromFile($zip, $resolvedFiles[$entryName].FullName, $entryName, [System.IO.Compression.CompressionLevel]::Optimal) | Out-Null
    }

    $checksumEntry = $zip.CreateEntry("$bundleRoot/SHA256SUMS.txt", [System.IO.Compression.CompressionLevel]::Optimal)
    $stream = $checksumEntry.Open()
    $writer = [System.IO.StreamWriter]::new($stream, [System.Text.UTF8Encoding]::new($false))
    try {
        $writer.Write(($checksums -join "`n") + "`n")
    } finally {
        $writer.Dispose()
        $stream.Dispose()
    }
} finally {
    $zip.Dispose()
}

$bundleItem = Get-Item $resolvedBundlePath
$bundleHash = Get-FileHash -Algorithm SHA256 -Path $bundleItem.FullName
$relativeBundlePath = Get-RelativePath $bundleItem.FullName

Write-Host "Release bundle written: $relativeBundlePath" -ForegroundColor Green
Write-Host "Bundle size: $($bundleItem.Length) bytes"
Write-Host "Bundle SHA-256: $($bundleHash.Hash.ToLowerInvariant())"
foreach ($entryName in $resolvedFiles.Keys) {
    Write-Host "Included: $entryName <- $(Get-RelativePath $resolvedFiles[$entryName].FullName)"
}
Write-Host "Included: $bundleRoot/SHA256SUMS.txt"
