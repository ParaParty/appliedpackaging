param(
    [string] $JarPath,
    [string] $OutputDir = "build/release",
    [string] $ManifestPath,
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

function Resolve-OutputPath {
    param([string] $Path)

    if ([System.IO.Path]::IsPathRooted($Path)) {
        return $Path
    }

    return (Join-Path (Get-Location) $Path)
}

$properties = Read-PropertiesFile "gradle.properties"
$modId = Require-Property $properties "mod_id"
$modName = Require-Property $properties "mod_name"
$modVersion = Require-Property $properties "mod_version"
$modGroup = Require-Property $properties "mod_group_id"
$modAuthors = Require-Property $properties "mod_authors"
$modLicense = Require-Property $properties "mod_license"

if ([string]::IsNullOrWhiteSpace($JarPath)) {
    $JarPath = Join-Path "build/libs" "$modId-$modVersion.jar"
}

$resolvedJar = Resolve-Path $JarPath -ErrorAction SilentlyContinue
if ($null -eq $resolvedJar) {
    throw "Release jar not found: $JarPath"
}

$jarItem = Get-Item $resolvedJar.Path
$expectedJarName = "$modId-$modVersion.jar"
if ($jarItem.Name -ne $expectedJarName) {
    throw "Release jar name '$($jarItem.Name)' does not match expected '$expectedJarName'"
}

$statusLines = @(Invoke-Git @("status", "--porcelain=v1", "--untracked-files=all"))
$isClean = $statusLines.Count -eq 0
if ($RequireCleanGit -and -not $isClean) {
    throw "Git working tree is not clean: $($statusLines -join '; ')"
}

$commit = @(Invoke-Git @("rev-parse", "HEAD"))[0]
$shortCommit = @(Invoke-Git @("rev-parse", "--short", "HEAD"))[0]
$branch = @(Invoke-Git @("rev-parse", "--abbrev-ref", "HEAD"))[0]
$hash = Get-FileHash -Algorithm SHA256 -Path $jarItem.FullName

if ([string]::IsNullOrWhiteSpace($ManifestPath)) {
    New-Item -ItemType Directory -Path $OutputDir -Force | Out-Null
    $ManifestPath = Join-Path $OutputDir "$modId-$modVersion-release-manifest.json"
} else {
    $parent = Split-Path -Parent $ManifestPath
    if (-not [string]::IsNullOrWhiteSpace($parent)) {
        New-Item -ItemType Directory -Path $parent -Force | Out-Null
    }
}

$resolvedManifestPath = Resolve-OutputPath $ManifestPath
$relativeJarPath = [System.IO.Path]::GetRelativePath($repoRoot, $jarItem.FullName).Replace("\", "/")
$relativeManifestPath = [System.IO.Path]::GetRelativePath($repoRoot, $resolvedManifestPath).Replace("\", "/")

$manifest = [ordered]@{
    schemaVersion = 1
    generatedAtUtc = (Get-Date).ToUniversalTime().ToString("o")
    mod = [ordered]@{
        id = $modId
        name = $modName
        version = $modVersion
        group = $modGroup
        authors = $modAuthors
        license = $modLicense
        description = $properties["mod_description"]
    }
    target = [ordered]@{
        minecraftVersion = Require-Property $properties "minecraft_version"
        minecraftVersionRange = Require-Property $properties "minecraft_version_range"
        forgeVersion = Require-Property $properties "forge_version"
        forgeVersionRange = Require-Property $properties "forge_version_range"
        loaderVersionRange = Require-Property $properties "loader_version_range"
        javaVersion = 17
    }
    dependencies = [ordered]@{
        ae2Version = Require-Property $properties "ae2_version"
        ae2VersionRange = Require-Property $properties "ae2_version_range"
        guideMeVersion = Require-Property $properties "guideme_version"
        guideMeVersionRange = Require-Property $properties "guideme_version_range"
    }
    artifact = [ordered]@{
        path = $relativeJarPath
        fileName = $jarItem.Name
        sizeBytes = $jarItem.Length
        sha256 = $hash.Hash.ToLowerInvariant()
        lastWriteTimeUtc = $jarItem.LastWriteTimeUtc.ToString("o")
    }
    git = [ordered]@{
        commit = $commit
        shortCommit = $shortCommit
        branch = $branch
        clean = $isClean
        statusPorcelain = @($statusLines)
    }
    release = [ordered]@{
        manifestPath = $relativeManifestPath
        generatedBy = "scripts/write-release-manifest.ps1"
    }
}

$json = $manifest | ConvertTo-Json -Depth 8
Set-Content -LiteralPath $resolvedManifestPath -Value $json -Encoding UTF8

Write-Host "Release manifest written: $ManifestPath" -ForegroundColor Green
Write-Host "Artifact: $relativeJarPath"
Write-Host "SHA-256: $($hash.Hash.ToLowerInvariant())"
Write-Host "Git: $shortCommit on $branch (clean=$isClean)"
