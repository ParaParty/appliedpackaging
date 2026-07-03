param(
    [int] $TimeoutSeconds = 240,
    [int] $PollSeconds = 2,
    [string] $LogPath = "run/logs/latest.log",
    [string] $OutputDir = "build/server-smoke"
)

$ErrorActionPreference = "Stop"

$repoRoot = (Resolve-Path (Join-Path $PSScriptRoot "..")).Path
Set-Location $repoRoot

$gradlew = Join-Path $repoRoot "gradlew.bat"
if (-not (Test-Path $gradlew)) {
    throw "gradlew.bat not found at $gradlew"
}

$eulaPath = Join-Path $repoRoot "run/eula.txt"
if (-not (Test-Path $eulaPath)) {
    throw "run/eula.txt not found. Run the server once and accept the Minecraft EULA before server smoke verification."
}

$eulaText = Get-Content $eulaPath -Raw
if ($eulaText -notmatch "(?m)^\s*eula\s*=\s*true\s*$") {
    throw "run/eula.txt does not contain eula=true. Server smoke verification requires explicit local EULA acceptance."
}

New-Item -ItemType Directory -Path $OutputDir -Force | Out-Null
$stdoutPath = Join-Path $OutputDir "run-server-smoke.stdout.log"
$stderrPath = Join-Path $OutputDir "run-server-smoke.stderr.log"

function Get-TextTail {
    param(
        [string] $Path,
        [int] $Count = 40
    )

    if (-not (Test-Path $Path)) {
        return "<missing: $Path>"
    }

    return (Get-Content $Path -Tail $Count -ErrorAction SilentlyContinue) -join "`n"
}

function Stop-ProcessTree {
    param([System.Diagnostics.Process] $Process)

    if ($null -eq $Process -or $Process.HasExited) {
        return
    }

    Write-Host "Stopping runServer process tree (PID $($Process.Id))..." -ForegroundColor Yellow
    $taskkill = Get-Command taskkill.exe -ErrorAction SilentlyContinue
    if ($null -ne $taskkill) {
        & $taskkill.Source /PID $Process.Id /T /F | Out-Null
    } else {
        Stop-Process -Id $Process.Id -Force -ErrorAction SilentlyContinue
    }

    try {
        $Process.WaitForExit(10000) | Out-Null
    } catch {
        # The process may already be gone after taskkill.
    }
}

function Test-PortClosed {
    param(
        [int] $Port,
        [int] $TimeoutSeconds = 30
    )

    $command = Get-Command Get-NetTCPConnection -ErrorAction SilentlyContinue
    if ($null -eq $command) {
        Write-Host "Get-NetTCPConnection not available; skipping port $Port cleanup check." -ForegroundColor Yellow
        return $true
    }

    $deadline = (Get-Date).AddSeconds($TimeoutSeconds)
    do {
        $listeners = @(Get-NetTCPConnection -LocalPort $Port -State Listen -ErrorAction SilentlyContinue)
        if ($listeners.Count -eq 0) {
            return $true
        }

        Start-Sleep -Seconds 1
    } while ((Get-Date) -lt $deadline)

    return $false
}

$startUtc = (Get-Date).ToUniversalTime()
$resolvedLogPath = Join-Path $repoRoot $LogPath
$worldLoadPattern = 'Done \([0-9.]+s\)! For help, type "help"'
$requiredLogPatterns = @(
    'Applied Packaging initialized',
    'Preparing level "world"',
    $worldLoadPattern
)

Write-Host "Applied Packaging dedicated server smoke" -ForegroundColor Cyan
Write-Host "Repository: $repoRoot"
Write-Host "Timeout: $TimeoutSeconds seconds"
Write-Host "Log: $LogPath"

$process = Start-Process `
    -FilePath $gradlew `
    -ArgumentList @("runServer", "--stacktrace") `
    -WorkingDirectory $repoRoot `
    -RedirectStandardOutput $stdoutPath `
    -RedirectStandardError $stderrPath `
    -WindowStyle Hidden `
    -PassThru

$worldLoaded = $false
$deadline = (Get-Date).AddSeconds($TimeoutSeconds)

try {
    do {
        if (Test-Path $resolvedLogPath) {
            $logItem = Get-Item $resolvedLogPath
            if ($logItem.LastWriteTimeUtc -ge $startUtc.AddSeconds(-5)) {
                $logText = Get-Content $resolvedLogPath -Raw -ErrorAction SilentlyContinue
                if ($logText -match $worldLoadPattern) {
                    $worldLoaded = $true
                    break
                }
            }
        }

        if ($process.HasExited) {
            break
        }

        Start-Sleep -Seconds $PollSeconds
    } while ((Get-Date) -lt $deadline)

    if (-not $worldLoaded) {
        $message = "runServer did not reach dedicated server world-load within $TimeoutSeconds seconds."
        if ($process.HasExited) {
            $message = "runServer exited before dedicated server world-load. Exit code: $($process.ExitCode)."
        }
        throw $message
    }

    $freshLogText = Get-Content $resolvedLogPath -Raw
    foreach ($pattern in $requiredLogPatterns) {
        if ($freshLogText -notmatch $pattern) {
            throw "Server smoke log is missing required pattern: $pattern"
        }
    }

    Write-Host "[PASS] Dedicated server reached world-load" -ForegroundColor Green
} catch {
    Write-Host "[FAIL] $($_.Exception.Message)" -ForegroundColor Red
    Write-Host ""
    Write-Host "latest.log tail:" -ForegroundColor Yellow
    Write-Host (Get-TextTail $resolvedLogPath)
    Write-Host ""
    Write-Host "stdout tail:" -ForegroundColor Yellow
    Write-Host (Get-TextTail $stdoutPath)
    Write-Host ""
    Write-Host "stderr tail:" -ForegroundColor Yellow
    Write-Host (Get-TextTail $stderrPath)
    Stop-ProcessTree $process
    exit 1
} finally {
    Stop-ProcessTree $process
}

if (-not (Test-PortClosed -Port 25565)) {
    Write-Host "[FAIL] Port 25565 is still listening after server smoke cleanup" -ForegroundColor Red
    exit 1
}

Write-Host "[PASS] Port 25565 is not listening after cleanup" -ForegroundColor Green
Write-Host "Server smoke passed. Run verify-release.ps1 -RequireServerWorldLoad to audit the refreshed latest.log." -ForegroundColor Green
