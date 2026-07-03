param(
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
    [switch] $WriteReleaseManifest,
    [switch] $RequireReleaseManifest,
    [switch] $SkipDocs,
    [switch] $SkipAssetContracts,
    [switch] $PlanOnly
)

$ErrorActionPreference = "Stop"

$repoRoot = Resolve-Path (Join-Path $PSScriptRoot "..")
Set-Location $repoRoot

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

Write-Host "Applied Packaging release check plan" -ForegroundColor Cyan
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
