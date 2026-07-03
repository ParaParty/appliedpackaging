param()

$ErrorActionPreference = "Stop"

$repoRoot = (Resolve-Path (Join-Path $PSScriptRoot "..")).Path
Set-Location $repoRoot

$selfTests = @(
    @{
        Name = "Release readiness self-test"
        Path = Join-Path $repoRoot "scripts/test-release-readiness.ps1"
    },
    @{
        Name = "Release check plan self-test"
        Path = Join-Path $repoRoot "scripts/test-release-check-plan.ps1"
    },
    @{
        Name = "Release manifest self-test"
        Path = Join-Path $repoRoot "scripts/test-release-manifest.ps1"
    },
    @{
        Name = "Release bundle self-test"
        Path = Join-Path $repoRoot "scripts/test-release-bundle.ps1"
    }
)

foreach ($selfTest in $selfTests) {
    Write-Host ""
    Write-Host "==> $($selfTest.Name)" -ForegroundColor Cyan

    $output = & pwsh -NoProfile -ExecutionPolicy Bypass -File $selfTest.Path 2>&1 | Out-String
    $exitCode = $LASTEXITCODE

    Write-Host $output.TrimEnd()

    if ($exitCode -ne 0) {
        Write-Host ""
        Write-Host "[FAIL] $($selfTest.Name) exited $exitCode" -ForegroundColor Red
        exit $exitCode
    }

    Write-Host "[PASS] $($selfTest.Name)" -ForegroundColor Green
}

Write-Host ""
Write-Host "Release self-tests passed." -ForegroundColor Green
