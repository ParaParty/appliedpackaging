param(
    [switch] $RequireReadyForTag
)

$ErrorActionPreference = "Stop"

$repoRoot = (Resolve-Path (Join-Path $PSScriptRoot "..")).Path
Set-Location $repoRoot

$failures = [System.Collections.Generic.List[string]]::new()
$blockers = [System.Collections.Generic.List[string]]::new()

function Add-Pass {
    param([string] $Message)
    Write-Host "[PASS] $Message" -ForegroundColor Green
}

function Add-Warn {
    param([string] $Message)
    Write-Host "[WARN] $Message" -ForegroundColor Yellow
}

function Add-Fail {
    param([string] $Message)
    $script:failures.Add($Message) | Out-Null
    Write-Host "[FAIL] $Message" -ForegroundColor Red
}

function Add-Blocker {
    param([string] $Message)
    $script:blockers.Add($Message) | Out-Null
    Add-Warn $Message
}

function Get-RequiredText {
    param([string] $Path)

    if (-not (Test-Path -LiteralPath $Path)) {
        Add-Fail "Required readiness source exists: $Path"
        return $null
    }

    Add-Pass "Required readiness source exists: $Path"
    return Get-Content -LiteralPath $Path -Raw
}

function Test-Contains {
    param(
        [string] $SourceName,
        [string] $Text,
        [string] $Needle
    )

    if ($null -eq $Text) {
        return
    }

    if ($Text.Contains($Needle)) {
        Add-Pass "$SourceName contains required marker: $Needle"
    } else {
        Add-Fail "$SourceName contains required marker: $Needle"
    }
}

function Test-IntakeRows {
    param([string] $Text)

    if ($null -eq $Text) {
        return
    }

    $rows = @()
    foreach ($line in ($Text -split "`r?`n")) {
        if ($line -match '^\|\s*IN-[0-9]+\s*\|') {
            $rows += $line
        }
    }

    if ($rows.Count -eq 0) {
        Add-Pass "No pending intake rows are listed"
        return
    }

    foreach ($row in $rows) {
        $columns = @($row.Trim().Trim("|").Split("|") | ForEach-Object { $_.Trim() })
        if ($columns.Count -lt 6) {
            Add-Fail "Intake row has expected 6 columns: $row"
            continue
        }

        $id = $columns[0]
        $status = $columns[3]
        $verification = $columns[5]
        if (($status -match '待|未完成|等待') -or ($verification -match '待|未完成|等待')) {
            Add-Blocker "$id is not ready for tag: status='$status', verification='$verification'"
        } else {
            Add-Pass "$id is ready for tag"
        }
    }
}

Write-Host "Applied Packaging release readiness audit" -ForegroundColor Cyan
Write-Host "Repository: $repoRoot"

$changeIntakeText = Get-RequiredText "docs/08-change-intake.md"
$verificationText = Get-RequiredText "docs/06-verification-release.md"

Test-Contains "docs/08-change-intake.md" $changeIntakeText "## 5. 新增项暂存表"
Test-Contains "docs/06-verification-release.md" $verificationText "当前目标完成判定"

Test-IntakeRows $changeIntakeText

if ($null -ne $changeIntakeText) {
    if ($changeIntakeText -match '(?m)^发布 tag：等待|^最终服务端 world-load：.*尚未|^\|\s*IN-[0-9]+\s*\|.*待用户') {
        Add-Blocker "change intake document still describes an open intake or deferred final server validation"
    } else {
        Add-Pass "change intake document does not describe an open intake window"
    }
}

if ($null -ne $verificationText) {
    if ($verificationText -match '(?m)^不能标记完成。|^发布 tag：未完成。|^.*发布 tag 应等待.*|^.*最终发布范围尚未冻结.*') {
        Add-Blocker "verification document still marks the goal or release tag as incomplete"
    } else {
        Add-Pass "verification document does not mark the goal or release tag as incomplete"
    }
}

if ($failures.Count -gt 0) {
    Write-Host ""
    Write-Host "Release readiness audit failed with $($failures.Count) structural issue(s):" -ForegroundColor Red
    foreach ($failure in $failures) {
        Write-Host " - $failure" -ForegroundColor Red
    }
    exit 1
}

if ($blockers.Count -gt 0) {
    Write-Host ""
    Write-Host "Release readiness has $($blockers.Count) blocker(s):" -ForegroundColor Yellow
    foreach ($blocker in $blockers) {
        Write-Host " - $blocker" -ForegroundColor Yellow
    }

    if ($RequireReadyForTag) {
        Write-Host ""
        Write-Host "-RequireReadyForTag was set; release readiness blockers are fatal." -ForegroundColor Red
        exit 1
    }

    Write-Host ""
    Write-Host "Release readiness audit completed with blockers. Omit -RequireReadyForTag for pre-freeze checks only." -ForegroundColor Yellow
    exit 0
}

Write-Host ""
Write-Host "Release readiness audit passed." -ForegroundColor Green
