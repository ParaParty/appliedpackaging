param(
    [switch] $AuditOnly,
    [switch] $SkipBuild,
    [switch] $SkipData,
    [switch] $SkipGameTest,
    [switch] $RunClientSmoke,
    [switch] $RequireServerWorldLoad,
    [switch] $SkipAssetContracts,
    [switch] $PlanOnly
)

$ErrorActionPreference = "Stop"

$repoRoot = Resolve-Path (Join-Path $PSScriptRoot "..")
Set-Location $repoRoot

if ($RequireServerWorldLoad -and -not $AuditOnly) {
    throw "-RequireServerWorldLoad is only valid with -AuditOnly. Run full checks first, then run .\gradlew.bat runServer manually to refresh run/logs/latest.log, then run this script again with -AuditOnly -RequireServerWorldLoad."
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

if ($RequireServerWorldLoad) {
    $verifyCommand += "-RequireServerWorldLoad"
}

$steps.Add(@{
    Name = "Mechanical release audit"
    Command = $verifyCommand
}) | Out-Null

Write-Host "Applied Packaging release check plan" -ForegroundColor Cyan
foreach ($step in $steps) {
    Write-Host " - $($step.Name): $($step.Command -join ' ')"
}

if ($RequireServerWorldLoad) {
    Write-Host ""
    Write-Host "Note: -RequireServerWorldLoad checks run/logs/latest.log only. Run .\gradlew.bat runServer manually after final scope freeze to refresh that log." -ForegroundColor Yellow
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
