param()

$ErrorActionPreference = "Stop"

$repoRoot = (Resolve-Path (Join-Path $PSScriptRoot "..")).Path
Set-Location $repoRoot

$writeManifestScript = Join-Path $repoRoot "scripts/write-release-manifest.ps1"
$verifyManifestScript = Join-Path $repoRoot "scripts/verify-release-manifest.ps1"
$tempRoot = Join-Path ([System.IO.Path]::GetTempPath()) ("appliedpackaging-manifest-" + [System.Guid]::NewGuid().ToString("N"))

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

try {
    New-Item -ItemType Directory -Force -Path $tempRoot | Out-Null

    $manifestPath = Join-Path $tempRoot "appliedpackaging-test-release-manifest.json"

    Invoke-Case `
        -Name "write manifest fixture" `
        -Arguments @("-File", $writeManifestScript, "-ManifestPath", $manifestPath) `
        -ExpectedExitCode 0

    Invoke-Case `
        -Name "valid manifest fixture" `
        -Arguments @("-File", $verifyManifestScript, "-ManifestPath", $manifestPath) `
        -ExpectedExitCode 0

    $badModManifestPath = Join-Path $tempRoot "bad-mod-release-manifest.json"
    $badManifest = Get-Content -LiteralPath $manifestPath -Raw | ConvertFrom-Json -Depth 32
    $badManifest.mod.id = "wrong_mod_id"
    $badManifest | ConvertTo-Json -Depth 32 | Set-Content -LiteralPath $badModManifestPath -Encoding UTF8

    Invoke-Case `
        -Name "tampered manifest mod id fixture" `
        -Arguments @("-File", $verifyManifestScript, "-ManifestPath", $badModManifestPath) `
        -ExpectedExitCode 1 `
        -ExpectedText "mod.id expected"

    $badHashManifestPath = Join-Path $tempRoot "bad-hash-release-manifest.json"
    $badManifest = Get-Content -LiteralPath $manifestPath -Raw | ConvertFrom-Json -Depth 32
    $badManifest.artifact.sha256 = "0000000000000000000000000000000000000000000000000000000000000000"
    $badManifest | ConvertTo-Json -Depth 32 | Set-Content -LiteralPath $badHashManifestPath -Encoding UTF8

    Invoke-Case `
        -Name "tampered manifest artifact hash fixture" `
        -Arguments @("-File", $verifyManifestScript, "-ManifestPath", $badHashManifestPath) `
        -ExpectedExitCode 1 `
        -ExpectedText "artifact.sha256 expected"

    $statusLines = @(git status --porcelain=v1 --untracked-files=all)
    if ($statusLines.Count -eq 0) {
        $cleanManifestPath = Join-Path $tempRoot "clean-release-manifest.json"

        Invoke-Case `
            -Name "write clean-git manifest fixture" `
            -Arguments @("-File", $writeManifestScript, "-ManifestPath", $cleanManifestPath, "-RequireCleanGit") `
            -ExpectedExitCode 0

        Invoke-Case `
            -Name "valid clean-git manifest fixture" `
            -Arguments @("-File", $verifyManifestScript, "-ManifestPath", $cleanManifestPath, "-RequireCleanGit") `
            -ExpectedExitCode 0
    } else {
        Write-Host "[PASS] clean-git manifest fixture skipped because working tree is dirty" -ForegroundColor Yellow
    }

    Write-Host ""
    Write-Host "Release manifest self-test passed." -ForegroundColor Green
} finally {
    if (Test-Path -LiteralPath $tempRoot) {
        $resolvedTempRoot = (Resolve-Path -LiteralPath $tempRoot).Path
        $resolvedTempParent = (Resolve-Path -LiteralPath ([System.IO.Path]::GetTempPath())).Path
        $tempLeaf = Split-Path -Leaf $resolvedTempRoot
        if (-not $resolvedTempRoot.StartsWith($resolvedTempParent, [System.StringComparison]::OrdinalIgnoreCase) -or
            -not $tempLeaf.StartsWith("appliedpackaging-manifest-", [System.StringComparison]::Ordinal)) {
            throw "Refusing to remove unexpected manifest self-test path: $resolvedTempRoot"
        }

        Remove-Item -LiteralPath $resolvedTempRoot -Recurse -Force
    }
}
