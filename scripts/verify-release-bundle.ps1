param(
    [string] $BundlePath,
    [string] $JarPath,
    [string] $ManifestPath,
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

function Get-EntryHash {
    param([System.IO.Compression.ZipArchiveEntry] $Entry)

    $sha = [System.Security.Cryptography.SHA256]::Create()
    $stream = $Entry.Open()
    try {
        $bytes = $sha.ComputeHash($stream)
        return [System.BitConverter]::ToString($bytes).Replace("-", "").ToLowerInvariant()
    } finally {
        $stream.Dispose()
        $sha.Dispose()
    }
}

function Get-EntryText {
    param([System.IO.Compression.ZipArchiveEntry] $Entry)

    $stream = $Entry.Open()
    $reader = [System.IO.StreamReader]::new($stream, [System.Text.UTF8Encoding]::new($false), $true)
    try {
        return $reader.ReadToEnd()
    } finally {
        $reader.Dispose()
        $stream.Dispose()
    }
}

function Get-JsonValue {
    param(
        [object] $Root,
        [string] $Path
    )

    $current = $Root
    foreach ($segment in $Path.Split(".")) {
        if ($null -eq $current) {
            Add-Fail "Bundle manifest contains $Path"
            return $null
        }

        $property = $current.PSObject.Properties[$segment]
        if ($null -eq $property) {
            Add-Fail "Bundle manifest contains $Path"
            return $null
        }

        $current = $property.Value
    }

    return $current
}

function Assert-BundleManifestText {
    param(
        [object] $Manifest,
        [string] $Path,
        [string] $Expected
    )

    $actual = Get-JsonValue $Manifest $Path
    if ($null -eq $actual) {
        return
    }

    if ([string]$actual -eq $Expected) {
        Add-Pass "Bundle manifest $Path matches expected value"
    } else {
        Add-Fail "Bundle manifest $Path expected '$Expected' but was '$actual'"
    }
}

function Assert-BundleManifestBool {
    param(
        [object] $Manifest,
        [string] $Path,
        [bool] $Expected
    )

    $actual = Get-JsonValue $Manifest $Path
    if ($null -eq $actual) {
        return
    }

    if ([bool]$actual -eq $Expected) {
        Add-Pass "Bundle manifest $Path matches expected value"
    } else {
        Add-Fail "Bundle manifest $Path expected '$Expected' but was '$actual'"
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
$bundleRoot = "$modId-$modVersion"

if ([string]::IsNullOrWhiteSpace($JarPath)) {
    $JarPath = Join-Path "build/libs" "$modId-$modVersion.jar"
}

if ([string]::IsNullOrWhiteSpace($ManifestPath)) {
    $ManifestPath = Join-Path "build/release" "$modId-$modVersion-release-manifest.json"
}

if ([string]::IsNullOrWhiteSpace($BundlePath)) {
    $BundlePath = Join-Path "build/release" "$modId-$modVersion-release-bundle.zip"
}

Write-Host "Applied Packaging release bundle audit" -ForegroundColor Cyan
Write-Host "Repository: $repoRoot"
Write-Host "Bundle: $BundlePath"

$gitStatusLines = @()
$gitIsClean = $null
if ($RequireCleanGit) {
    $gitStatusLines = @((Invoke-Git @("status", "--porcelain=v1", "--untracked-files=all")) | Where-Object { -not [string]::IsNullOrWhiteSpace($_) })
    $gitIsClean = $gitStatusLines.Count -eq 0
    if ($gitIsClean) {
        Add-Pass "Git working tree is clean"
    } else {
        Add-Fail "Git working tree is clean: $($gitStatusLines -join '; ')"
    }
}

$expectedSources = [ordered]@{
    "$bundleRoot/$modId-$modVersion.jar" = $JarPath
    "$bundleRoot/$modId-$modVersion-release-manifest.json" = $ManifestPath
    "$bundleRoot/README.md" = "README.md"
    "$bundleRoot/CHANGELOG.md" = "CHANGELOG.md"
    "$bundleRoot/LICENSE.md" = "LICENSE.md"
}
$expectedEntries = [System.Collections.Generic.HashSet[string]]::new([StringComparer]::Ordinal)
foreach ($entryName in $expectedSources.Keys) {
    $expectedEntries.Add($entryName) | Out-Null
}
$expectedEntries.Add("$bundleRoot/SHA256SUMS.txt") | Out-Null

$resolvedBundle = Resolve-Path $BundlePath -ErrorAction SilentlyContinue
if ($null -eq $resolvedBundle) {
    Add-Fail "Release bundle exists at $BundlePath"
} else {
    Add-Pass "Release bundle exists at $BundlePath"
}

$resolvedSources = [ordered]@{}
foreach ($entryName in $expectedSources.Keys) {
    $resolved = Resolve-Path $expectedSources[$entryName] -ErrorAction SilentlyContinue
    if ($null -eq $resolved) {
        Add-Fail "Source file exists for ${entryName}: $($expectedSources[$entryName])"
    } else {
        $resolvedSources[$entryName] = Get-Item $resolved.Path
    }
}

if ($failures.Count -eq 0) {
    Add-Type -AssemblyName System.IO.Compression.FileSystem
    $zip = [System.IO.Compression.ZipFile]::OpenRead($resolvedBundle.Path)
    try {
        $entryNames = @($zip.Entries | ForEach-Object { $_.FullName })
        $actualSet = [System.Collections.Generic.HashSet[string]]::new([StringComparer]::Ordinal)
        foreach ($entryName in $entryNames) {
            $actualSet.Add($entryName) | Out-Null
        }

        foreach ($entryName in $expectedEntries) {
            if ($actualSet.Contains($entryName)) {
                Add-Pass "Bundle contains $entryName"
            } else {
                Add-Fail "Bundle contains $entryName"
            }
        }

        $extraEntries = @($entryNames | Where-Object { -not $expectedEntries.Contains($_) })
        if ($extraEntries.Count -eq 0) {
            Add-Pass "Bundle contains no unexpected entries"
        } else {
            Add-Fail "Bundle contains unexpected entries: $($extraEntries -join ', ')"
        }

        foreach ($entryName in $resolvedSources.Keys) {
            $entry = $zip.GetEntry($entryName)
            if ($null -eq $entry) {
                continue
            }

            $sourceHash = (Get-FileHash -Algorithm SHA256 -Path $resolvedSources[$entryName].FullName).Hash.ToLowerInvariant()
            $entryHash = Get-EntryHash $entry
            if ($entryHash -eq $sourceHash) {
                Add-Pass "$entryName hash matches source file"
            } else {
                Add-Fail "$entryName hash expected '$sourceHash' but was '$entryHash'"
            }
        }

        $checksumEntry = $zip.GetEntry("$bundleRoot/SHA256SUMS.txt")
        if ($null -ne $checksumEntry) {
            $checksumText = Get-EntryText $checksumEntry
            $expectedChecksumLines = [System.Collections.Generic.List[string]]::new()
            foreach ($entryName in $resolvedSources.Keys) {
                $sourceHash = (Get-FileHash -Algorithm SHA256 -Path $resolvedSources[$entryName].FullName).Hash.ToLowerInvariant()
                $relativeName = $entryName.Substring($bundleRoot.Length + 1)
                $expectedChecksumLines.Add("$sourceHash  $relativeName") | Out-Null
            }
            $expectedChecksumText = ($expectedChecksumLines -join "`n") + "`n"
            if ($checksumText -eq $expectedChecksumText) {
                Add-Pass "SHA256SUMS.txt matches bundled source files"
            } else {
                Add-Fail "SHA256SUMS.txt matches bundled source files"
            }
        }

        $manifestEntry = $zip.GetEntry("$bundleRoot/$modId-$modVersion-release-manifest.json")
        $jarEntry = $zip.GetEntry("$bundleRoot/$modId-$modVersion.jar")
        if ($null -ne $manifestEntry -and $null -ne $jarEntry) {
            $manifest = Get-EntryText $manifestEntry | ConvertFrom-Json -Depth 32
            Assert-BundleManifestText $manifest "mod.id" $modId
            Assert-BundleManifestText $manifest "mod.version" $modVersion

            $artifactSha = Get-JsonValue $manifest "artifact.sha256"
            $artifactFileName = Get-JsonValue $manifest "artifact.fileName"
            $jarHash = Get-EntryHash $jarEntry
            if ($artifactFileName -eq "$modId-$modVersion.jar") {
                Add-Pass "Bundle manifest artifact fileName matches expected jar"
            } else {
                Add-Fail "Bundle manifest artifact fileName expected '$modId-$modVersion.jar' but was '$artifactFileName'"
            }
            if ($artifactSha -eq $jarHash) {
                Add-Pass "Bundle manifest artifact sha256 matches bundled jar"
            } else {
                Add-Fail "Bundle manifest artifact sha256 expected '$jarHash' but was '$artifactSha'"
            }

            if ($RequireCleanGit) {
                $commit = @(Invoke-Git @("rev-parse", "HEAD"))[0]
                $shortCommit = @(Invoke-Git @("rev-parse", "--short", "HEAD"))[0]
                $branch = @(Invoke-Git @("rev-parse", "--abbrev-ref", "HEAD"))[0]

                Assert-BundleManifestText $manifest "git.commit" $commit
                Assert-BundleManifestText $manifest "git.shortCommit" $shortCommit
                Assert-BundleManifestText $manifest "git.branch" $branch
                Assert-BundleManifestBool $manifest "git.clean" $gitIsClean

                $gitProperty = $manifest.PSObject.Properties["git"]
                if ($null -eq $gitProperty) {
                    Add-Fail "Bundle manifest contains git"
                } else {
                    $statusProperty = $gitProperty.Value.PSObject.Properties["statusPorcelain"]
                    if ($null -eq $statusProperty) {
                        Add-Fail "Bundle manifest contains git.statusPorcelain"
                    } else {
                        Assert-StringArraysEqual @($statusProperty.Value | ForEach-Object { [string]$_ }) $gitStatusLines "Bundle manifest git.statusPorcelain matches current git status"
                    }
                }
            }
        }
    } finally {
        $zip.Dispose()
    }
}

if ($failures.Count -gt 0) {
    Write-Host ""
    Write-Host "Release bundle audit failed with $($failures.Count) issue(s):" -ForegroundColor Red
    foreach ($failure in $failures) {
        Write-Host " - $failure" -ForegroundColor Red
    }
    exit 1
}

Write-Host ""
Write-Host "Release bundle audit passed." -ForegroundColor Green
