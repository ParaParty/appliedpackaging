param()

$ErrorActionPreference = "Stop"

$repoRoot = (Resolve-Path (Join-Path $PSScriptRoot "..")).Path
Set-Location $repoRoot

$writeManifestScript = Join-Path $repoRoot "scripts/write-release-manifest.ps1"
$writeBundleScript = Join-Path $repoRoot "scripts/write-release-bundle.ps1"
$verifyBundleScript = Join-Path $repoRoot "scripts/verify-release-bundle.ps1"
$tempRoot = Join-Path ([System.IO.Path]::GetTempPath()) ("appliedpackaging-bundle-" + [System.Guid]::NewGuid().ToString("N"))

function Invoke-Case {
    param(
        [string] $Name,
        [string[]] $Arguments,
        [int] $ExpectedExitCode,
        [string] $ExpectedText = ""
    )

    $output = & pwsh -NoProfile -ExecutionPolicy Bypass @Arguments 2>&1 | Out-String
    $exitCode = $LASTEXITCODE

    if ($exitCode -ne $ExpectedExitCode) {
        Write-Host "[FAIL] $Name expected exit $ExpectedExitCode but got $exitCode" -ForegroundColor Red
        Write-Host $output
        exit 1
    }

    if ($ExpectedText -ne "" -and -not $output.Contains($ExpectedText)) {
        Write-Host "[FAIL] $Name missing expected text: $ExpectedText" -ForegroundColor Red
        Write-Host $output
        exit 1
    }

    Write-Host "[PASS] $Name exited $ExpectedExitCode" -ForegroundColor Green
}

function Update-ZipEntryText {
    param(
        [string] $ZipPath,
        [string] $EntryName,
        [string] $Text
    )

    Add-Type -AssemblyName System.IO.Compression.FileSystem
    $zip = [System.IO.Compression.ZipFile]::Open($ZipPath, [System.IO.Compression.ZipArchiveMode]::Update)
    try {
        $entry = $zip.GetEntry($EntryName)
        if ($null -eq $entry) {
            throw "Zip entry not found: $EntryName"
        }

        $entry.Delete()
        $newEntry = $zip.CreateEntry($EntryName, [System.IO.Compression.CompressionLevel]::Optimal)
        $stream = $newEntry.Open()
        $writer = [System.IO.StreamWriter]::new($stream, [System.Text.UTF8Encoding]::new($false))
        try {
            $writer.Write($Text)
        } finally {
            $writer.Dispose()
            $stream.Dispose()
        }
    } finally {
        $zip.Dispose()
    }
}

try {
    New-Item -ItemType Directory -Force -Path $tempRoot | Out-Null

    $manifestPath = Join-Path $tempRoot "appliedpackaging-test-release-manifest.json"
    $bundlePath = Join-Path $tempRoot "appliedpackaging-test-release-bundle.zip"

    Invoke-Case `
        -Name "write manifest fixture" `
        -Arguments @("-File", $writeManifestScript, "-ManifestPath", $manifestPath) `
        -ExpectedExitCode 0

    Invoke-Case `
        -Name "write bundle fixture" `
        -Arguments @("-File", $writeBundleScript, "-ManifestPath", $manifestPath, "-BundlePath", $bundlePath) `
        -ExpectedExitCode 0

    Invoke-Case `
        -Name "valid bundle fixture" `
        -Arguments @("-File", $verifyBundleScript, "-ManifestPath", $manifestPath, "-BundlePath", $bundlePath) `
        -ExpectedExitCode 0

    $manifest = Get-Content -LiteralPath $manifestPath -Raw | ConvertFrom-Json -Depth 32
    $modId = [string]$manifest.mod.id
    $modVersion = [string]$manifest.mod.version
    $bundleRoot = "$modId-$modVersion"
    $manifestEntryName = "$bundleRoot/$modId-$modVersion-release-manifest.json"
    $readmeEntryName = "$bundleRoot/README.md"

    $badManifestBundlePath = Join-Path $tempRoot "bad-manifest-bundle.zip"
    Copy-Item -LiteralPath $bundlePath -Destination $badManifestBundlePath
    $badManifest = Get-Content -LiteralPath $manifestPath -Raw | ConvertFrom-Json -Depth 32
    $badManifest.mod.id = "wrong_mod_id"
    $badManifestJson = $badManifest | ConvertTo-Json -Depth 32
    Update-ZipEntryText -ZipPath $badManifestBundlePath -EntryName $manifestEntryName -Text $badManifestJson

    Invoke-Case `
        -Name "tampered bundle manifest fixture" `
        -Arguments @("-File", $verifyBundleScript, "-ManifestPath", $manifestPath, "-BundlePath", $badManifestBundlePath) `
        -ExpectedExitCode 1 `
        -ExpectedText "Bundle manifest mod.id expected"

    $badReadmeBundlePath = Join-Path $tempRoot "bad-readme-bundle.zip"
    Copy-Item -LiteralPath $bundlePath -Destination $badReadmeBundlePath
    Update-ZipEntryText -ZipPath $badReadmeBundlePath -EntryName $readmeEntryName -Text "tampered readme`n"

    Invoke-Case `
        -Name "tampered bundled readme fixture" `
        -Arguments @("-File", $verifyBundleScript, "-ManifestPath", $manifestPath, "-BundlePath", $badReadmeBundlePath) `
        -ExpectedExitCode 1 `
        -ExpectedText "$readmeEntryName hash expected"

    $statusLines = @(git status --porcelain=v1 --untracked-files=all)
    if ($statusLines.Count -eq 0) {
        Invoke-Case `
            -Name "valid clean-git bundle fixture" `
            -Arguments @("-File", $verifyBundleScript, "-ManifestPath", $manifestPath, "-BundlePath", $bundlePath, "-RequireCleanGit") `
            -ExpectedExitCode 0
    } else {
        Write-Host "[PASS] clean-git bundle fixture skipped because working tree is dirty" -ForegroundColor Yellow
    }

    Write-Host ""
    Write-Host "Release bundle self-test passed." -ForegroundColor Green
} finally {
    if (Test-Path -LiteralPath $tempRoot) {
        $resolvedTempRoot = (Resolve-Path -LiteralPath $tempRoot).Path
        $resolvedTempParent = (Resolve-Path -LiteralPath ([System.IO.Path]::GetTempPath())).Path
        $tempLeaf = Split-Path -Leaf $resolvedTempRoot
        if (-not $resolvedTempRoot.StartsWith($resolvedTempParent, [System.StringComparison]::OrdinalIgnoreCase) -or
            -not $tempLeaf.StartsWith("appliedpackaging-bundle-", [System.StringComparison]::Ordinal)) {
            throw "Refusing to remove unexpected bundle self-test path: $resolvedTempRoot"
        }

        Remove-Item -LiteralPath $resolvedTempRoot -Recurse -Force
    }
}
