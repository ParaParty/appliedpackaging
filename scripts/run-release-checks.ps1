param(
    [switch] $ReleaseCandidate,
    [switch] $AuditOnly,
    [switch] $SkipBuild,
    [switch] $SkipData,
    [switch] $SkipGameTest,
    [switch] $RunClientSmoke,
    [switch] $RunServerSmoke,
    [int] $ServerSmokeTimeoutSeconds = 240,
    [switch] $RequireClientSmokeScreenshots,
    [switch] $RequireServerWorldLoad,
    [switch] $RequireCleanGit,
    [switch] $RequireReadyForTag,
    [switch] $WriteReleaseManifest,
    [switch] $RequireReleaseManifest,
    [switch] $WriteReleaseBundle,
    [switch] $RequireReleaseBundle,
    [switch] $SkipDocs,
    [switch] $SkipAssetContracts,
    [switch] $PlanOnly
)

$ErrorActionPreference = "Stop"

$repoRoot = Resolve-Path (Join-Path $PSScriptRoot "..")
Set-Location $repoRoot

if ($ReleaseCandidate) {
    if ($AuditOnly) {
        throw "-ReleaseCandidate cannot be combined with -AuditOnly. Release candidate mode runs the full verification sequence."
    }

    $skipFlags = @()
    if ($SkipBuild) { $skipFlags += "-SkipBuild" }
    if ($SkipData) { $skipFlags += "-SkipData" }
    if ($SkipGameTest) { $skipFlags += "-SkipGameTest" }
    if ($SkipDocs) { $skipFlags += "-SkipDocs" }
    if ($SkipAssetContracts) { $skipFlags += "-SkipAssetContracts" }
    if ($skipFlags.Count -gt 0) {
        throw "-ReleaseCandidate cannot be combined with skip flags: $($skipFlags -join ', ')"
    }

    $RunClientSmoke = $true
    $RunServerSmoke = $true
    $WriteReleaseManifest = $true
    $RequireReleaseManifest = $true
    $WriteReleaseBundle = $true
    $RequireReleaseBundle = $true
}

if ($AuditOnly -and $RunServerSmoke) {
    throw "-RunServerSmoke cannot be combined with -AuditOnly. Use -RunServerSmoke during the active release check sequence, or run scripts\run-server-smoke.ps1 directly before an audit-only pass."
}

if ($RequireServerWorldLoad -and -not $AuditOnly -and -not $RunServerSmoke) {
    throw "-RequireServerWorldLoad is only valid with -AuditOnly unless -RunServerSmoke is also set. Use -RunServerSmoke to refresh run/logs/latest.log inside this release check sequence."
}

function Invoke-Step {
    param(
        [string] $Name,
        [string[]] $Command
    )

    Write-Host ""
    Write-Host "==> $Name" -ForegroundColor Cyan
    Write-Host ($Command -join " ")

    if ($PlanOnly) {
        return
    }

    $executable = $Command[0]
    $arguments = @()
    if ($Command.Count -gt 1) {
        $arguments = @($Command[1..($Command.Count - 1)])
    }

    & $executable @arguments
    if ($LASTEXITCODE -ne 0) {
        throw "$Name failed with exit code $LASTEXITCODE"
    }
}

$steps = [System.Collections.Generic.List[object]]::new()

if (-not $AuditOnly) {
    if (-not $SkipBuild) {
        $steps.Add(@{
            Name = "Gradle build"
            Command = @(".\gradlew.bat", "build", "--stacktrace")
        }) | Out-Null
    }

    if (-not $SkipData) {
        $steps.Add(@{
            Name = "Data generation"
            Command = @(".\gradlew.bat", "runData", "--stacktrace")
        }) | Out-Null
    }

    if (-not $SkipGameTest) {
        $steps.Add(@{
            Name = "GameTest server"
            Command = @(".\gradlew.bat", "runGameTestServer", "--stacktrace")
        }) | Out-Null
    }

    if ($RunClientSmoke) {
        $steps.Add(@{
            Name = "Client smoke screenshots"
            Command = @(".\gradlew.bat", "runClientSmoke", "--stacktrace")
        }) | Out-Null
    }

    if ($RunServerSmoke) {
        $steps.Add(@{
            Name = "Dedicated server world-load smoke"
            Command = @(
                "pwsh",
                "-NoProfile",
                "-ExecutionPolicy",
                "Bypass",
                "-File",
                ".\scripts\run-server-smoke.ps1",
                "-TimeoutSeconds",
                $ServerSmokeTimeoutSeconds.ToString()
            )
        }) | Out-Null
    }
}

$verifyCommand = @(
    "pwsh",
    "-NoProfile",
    "-ExecutionPolicy",
    "Bypass",
    "-File",
    ".\scripts\verify-release.ps1"
)

if (-not $SkipAssetContracts) {
    $verifyCommand += "-RequireAssetContracts"
}

if ($RequireServerWorldLoad -or $RunServerSmoke) {
    $verifyCommand += "-RequireServerWorldLoad"
}

if ($RunClientSmoke -or $RequireClientSmokeScreenshots) {
    $verifyCommand += "-RequireClientSmokeScreenshots"
}

if ($RequireCleanGit) {
    $verifyCommand += "-RequireCleanGit"
}

$steps.Add(@{
    Name = "Mechanical release audit"
    Command = $verifyCommand
}) | Out-Null

if (-not $SkipDocs) {
    $steps.Add(@{
        Name = "Documentation audit"
        Command = @(
            "pwsh",
            "-NoProfile",
            "-ExecutionPolicy",
            "Bypass",
            "-File",
            ".\scripts\verify-docs.ps1"
        )
    }) | Out-Null
}

if ($RequireReadyForTag) {
    $steps.Add(@{
        Name = "Release readiness audit"
        Command = @(
            "pwsh",
            "-NoProfile",
            "-ExecutionPolicy",
            "Bypass",
            "-File",
            ".\scripts\verify-release-readiness.ps1",
            "-RequireReadyForTag"
        )
    }) | Out-Null
}

if ($WriteReleaseManifest) {
    $manifestCommand = @(
        "pwsh",
        "-NoProfile",
        "-ExecutionPolicy",
        "Bypass",
        "-File",
        ".\scripts\write-release-manifest.ps1"
    )
    if ($RequireCleanGit) {
        $manifestCommand += "-RequireCleanGit"
    }

    $steps.Add(@{
        Name = "Release manifest"
        Command = $manifestCommand
    }) | Out-Null
}

if ($RequireReleaseManifest) {
    $verifyManifestCommand = @(
        "pwsh",
        "-NoProfile",
        "-ExecutionPolicy",
        "Bypass",
        "-File",
        ".\scripts\verify-release-manifest.ps1"
    )
    if ($RequireCleanGit) {
        $verifyManifestCommand += "-RequireCleanGit"
    }

    $steps.Add(@{
        Name = "Release manifest audit"
        Command = $verifyManifestCommand
    }) | Out-Null
}

if ($WriteReleaseBundle) {
    $bundleCommand = @(
        "pwsh",
        "-NoProfile",
        "-ExecutionPolicy",
        "Bypass",
        "-File",
        ".\scripts\write-release-bundle.ps1"
    )
    if ($RequireCleanGit) {
        $bundleCommand += "-RequireCleanGit"
    }

    $steps.Add(@{
        Name = "Release bundle"
        Command = $bundleCommand
    }) | Out-Null
}

if ($RequireReleaseBundle) {
    $verifyBundleCommand = @(
        "pwsh",
        "-NoProfile",
        "-ExecutionPolicy",
        "Bypass",
        "-File",
        ".\scripts\verify-release-bundle.ps1"
    )
    if ($RequireCleanGit) {
        $verifyBundleCommand += "-RequireCleanGit"
    }

    $steps.Add(@{
        Name = "Release bundle audit"
        Command = $verifyBundleCommand
    }) | Out-Null
}

Write-Host "Applied Packaging release check plan" -ForegroundColor Cyan
if ($ReleaseCandidate) {
    Write-Host "Mode: release candidate" -ForegroundColor Cyan
}
foreach ($step in $steps) {
    Write-Host " - $($step.Name): $($step.Command -join ' ')"
}

if ($RequireServerWorldLoad -and -not $RunServerSmoke) {
    Write-Host ""
    Write-Host "Note: -RequireServerWorldLoad checks run/logs/latest.log only. Run .\gradlew.bat runServer manually after final scope freeze to refresh that log." -ForegroundColor Yellow
} elseif ($RunServerSmoke) {
    Write-Host ""
    Write-Host "Note: -RunServerSmoke refreshes run/logs/latest.log before the release audit checks dedicated server world-load evidence." -ForegroundColor Yellow
}

foreach ($step in $steps) {
    Invoke-Step -Name $step.Name -Command $step.Command
}

if ($PlanOnly) {
    Write-Host ""
    Write-Host "Plan only; no commands executed." -ForegroundColor Yellow
} else {
    Write-Host ""
    Write-Host "Release checks completed." -ForegroundColor Green
}
