param()

$ErrorActionPreference = "Stop"

$repoRoot = (Resolve-Path (Join-Path $PSScriptRoot "..")).Path
Set-Location $repoRoot

$runnerPath = Join-Path $repoRoot "scripts/run-release-checks.ps1"

function Invoke-Runner {
    param([string[]] $Arguments)

    $output = & pwsh -NoProfile -ExecutionPolicy Bypass -File $runnerPath @Arguments 2>&1 | Out-String
    return @{
        ExitCode = $LASTEXITCODE
        Output = $output
    }
}

function Assert-ExitCode {
    param(
        [string] $Name,
        [hashtable] $Result,
        [int] $ExpectedExitCode
    )

    if ($Result.ExitCode -ne $ExpectedExitCode) {
        Write-Host "[FAIL] $Name expected exit $ExpectedExitCode but got $($Result.ExitCode)" -ForegroundColor Red
        Write-Host $Result.Output
        exit 1
    }

    Write-Host "[PASS] $Name exited $ExpectedExitCode" -ForegroundColor Green
}

function Assert-Contains {
    param(
        [string] $Name,
        [string] $Text,
        [string] $Needle
    )

    if (-not $Text.Contains($Needle)) {
        Write-Host "[FAIL] $Name missing expected text: $Needle" -ForegroundColor Red
        Write-Host $Text
        exit 1
    }

    Write-Host "[PASS] $Name contains expected text: $Needle" -ForegroundColor Green
}

function Assert-OrderedText {
    param(
        [string] $Name,
        [string] $Text,
        [string[]] $Needles
    )

    $lastIndex = -1
    foreach ($needle in $Needles) {
        $index = $Text.IndexOf($needle, $lastIndex + 1, [System.StringComparison]::Ordinal)
        if ($index -lt 0) {
            Write-Host "[FAIL] $Name missing ordered text after index $lastIndex`: $needle" -ForegroundColor Red
            Write-Host $Text
            exit 1
        }

        $lastIndex = $index
    }

    Write-Host "[PASS] $Name has expected order" -ForegroundColor Green
}

$plan = Invoke-Runner @(
    "-PlanOnly",
    "-ReleaseCandidate",
    "-RequireCleanGit",
    "-RequireReadyForTag"
)
Assert-ExitCode "release candidate ready-for-tag plan" $plan 0

$expectedStepOrder = @(
    " - Gradle build:",
    " - Data generation:",
    " - GameTest server:",
    " - Dedicated server world-load smoke:",
    " - Mechanical release audit:",
    " - Asset resource audit:",
    " - Documentation audit:",
    " - Release readiness audit:",
    " - Release manifest:",
    " - Release manifest audit:",
    " - Release bundle:",
    " - Release bundle audit:"
)
Assert-OrderedText "release candidate ready-for-tag plan" $plan.Output $expectedStepOrder

Assert-Contains "release candidate ready-for-tag plan" $plan.Output "Mode: release candidate"
Assert-Contains "release candidate ready-for-tag plan" $plan.Output ".\gradlew.bat runGameTestServer --stacktrace"
Assert-Contains "release candidate ready-for-tag plan" $plan.Output ".\scripts\run-server-smoke.ps1 -TimeoutSeconds 240"
Assert-Contains "release candidate ready-for-tag plan" $plan.Output ".\scripts\verify-release.ps1 -RequireAssetContracts -RequireServerWorldLoad -RequireCleanGit"
Assert-Contains "release candidate ready-for-tag plan" $plan.Output ".\scripts\verify-assets.ps1"
Assert-Contains "release candidate ready-for-tag plan" $plan.Output ".\scripts\verify-release-readiness.ps1 -RequireReadyForTag"
Assert-Contains "release candidate ready-for-tag plan" $plan.Output ".\scripts\write-release-manifest.ps1 -RequireCleanGit"
Assert-Contains "release candidate ready-for-tag plan" $plan.Output ".\scripts\verify-release-bundle.ps1 -RequireCleanGit"

$forbiddenSkipFlags = @(
    "-SkipBuild",
    "-SkipData",
    "-SkipGameTest",
    "-SkipDocs",
    "-SkipAssetContracts"
)

foreach ($flag in $forbiddenSkipFlags) {
    $skipResult = Invoke-Runner @(
        "-PlanOnly",
        "-ReleaseCandidate",
        $flag
    )
    Assert-ExitCode "release candidate rejects $flag" $skipResult 1
    Assert-Contains "release candidate rejects $flag" $skipResult.Output "-ReleaseCandidate cannot be combined with skip flags: $flag"
}

$auditOnly = Invoke-Runner @(
    "-PlanOnly",
    "-ReleaseCandidate",
    "-AuditOnly"
)
Assert-ExitCode "release candidate rejects AuditOnly" $auditOnly 1
Assert-Contains "release candidate rejects AuditOnly" $auditOnly.Output "-ReleaseCandidate cannot be combined with -AuditOnly"

$serverSmokeAuditOnly = Invoke-Runner @(
    "-PlanOnly",
    "-AuditOnly",
    "-RunServerSmoke"
)
Assert-ExitCode "audit-only rejects RunServerSmoke" $serverSmokeAuditOnly 1
Assert-Contains "audit-only rejects RunServerSmoke" $serverSmokeAuditOnly.Output "-RunServerSmoke cannot be combined with -AuditOnly"

$serverWorldLoadWithoutSource = Invoke-Runner @(
    "-PlanOnly",
    "-RequireServerWorldLoad"
)
Assert-ExitCode "active run rejects stale server world-load audit" $serverWorldLoadWithoutSource 1
Assert-Contains "active run rejects stale server world-load audit" $serverWorldLoadWithoutSource.Output "-RequireServerWorldLoad is only valid with -AuditOnly unless -RunServerSmoke is also set"

Write-Host ""
Write-Host "Release check plan self-test passed." -ForegroundColor Green
