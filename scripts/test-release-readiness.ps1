param()

$ErrorActionPreference = "Stop"

$repoRoot = (Resolve-Path (Join-Path $PSScriptRoot "..")).Path
Set-Location $repoRoot

$scriptPath = Join-Path $repoRoot "scripts/verify-release-readiness.ps1"
$tempRoot = Join-Path ([System.IO.Path]::GetTempPath()) ("appliedpackaging-readiness-" + [System.Guid]::NewGuid().ToString("N"))

function Write-Fixture {
    param(
        [string] $CaseName,
        [string] $ChangeIntake,
        [string] $Verification
    )

    $caseRoot = Join-Path $tempRoot $CaseName
    New-Item -ItemType Directory -Force -Path $caseRoot | Out-Null
    $changeIntakePath = Join-Path $caseRoot "08-change-intake.md"
    $verificationPath = Join-Path $caseRoot "06-verification-release.md"
    Set-Content -LiteralPath $changeIntakePath -Value $ChangeIntake -Encoding UTF8
    Set-Content -LiteralPath $verificationPath -Value $Verification -Encoding UTF8

    return @{
        ChangeIntake = $changeIntakePath
        Verification = $verificationPath
    }
}

function Invoke-ReadinessCase {
    param(
        [string] $Name,
        [hashtable] $Fixture,
        [int] $ExpectedExitCode
    )

    $output = & pwsh -NoProfile -ExecutionPolicy Bypass -File $scriptPath `
        -ChangeIntakePath $Fixture.ChangeIntake `
        -VerificationPath $Fixture.Verification `
        -RequireReadyForTag 2>&1 | Out-String
    $exitCode = $LASTEXITCODE

    if ($exitCode -ne $ExpectedExitCode) {
        Write-Host "[FAIL] $Name expected exit $ExpectedExitCode but got $exitCode" -ForegroundColor Red
        Write-Host $output
        exit 1
    }

    Write-Host "[PASS] $Name exited $ExpectedExitCode" -ForegroundColor Green
}

try {
    New-Item -ItemType Directory -Force -Path $tempRoot | Out-Null

    $ready = Write-Fixture `
        -CaseName "ready" `
        -ChangeIntake @"
# 变更接收与范围冻结

当前接收窗口：

````text
已冻结。
最终服务端 world-load：已完成。
发布 tag：可创建。
````

## 5. 新增项暂存表

| ID | 类型 | 标题 | 状态 | 迁移目标 | 验证要求 |
| --- | --- | --- | --- | --- | --- |
| IN-001 | 需求 | 已确认需求 | 已迁移 | docs/01-requirements.md | 通过 |
| IN-002 | 材质 | 已确认材质 | 已迁移 | docs/04-asset-spec.md | 通过 |
"@ `
        -Verification @"
# 验证与发布

当前目标完成判定：

````text
可以标记完成。
发布 tag 就绪门禁已通过。
````
"@

    $blocked = Write-Fixture `
        -CaseName "blocked" `
        -ChangeIntake @"
# 变更接收与范围冻结

最终服务端 world-load：当前基线已通过；尚未在新增需求/材质冻结后重新执行。
发布 tag：等待新增范围实现、验证和最终服务端 world-load 后创建。

## 5. 新增项暂存表

| ID | 类型 | 标题 | 状态 | 迁移目标 | 验证要求 |
| --- | --- | --- | --- | --- | --- |
| IN-001 | 需求 | 待用户补充 | 待输入 | 待判定 | 待判定 |
"@ `
        -Verification @"
# 验证与发布

当前目标完成判定：

````text
不能标记完成。
最终发布范围尚未冻结。
发布 tag 应等待新增范围完成。
````
"@

    $structuralFailure = Write-Fixture `
        -CaseName "structural" `
        -ChangeIntake @"
# 变更接收与范围冻结

缺少必需暂存表标题。
"@ `
        -Verification @"
# 验证与发布

当前目标完成判定：

````text
可以标记完成。
````
"@

    Invoke-ReadinessCase -Name "ready fixture" -Fixture $ready -ExpectedExitCode 0
    Invoke-ReadinessCase -Name "blocked fixture" -Fixture $blocked -ExpectedExitCode 1
    Invoke-ReadinessCase -Name "structural failure fixture" -Fixture $structuralFailure -ExpectedExitCode 1

    Write-Host ""
    Write-Host "Release readiness self-test passed." -ForegroundColor Green
} finally {
    if (Test-Path -LiteralPath $tempRoot) {
        $resolvedTempRoot = (Resolve-Path -LiteralPath $tempRoot).Path
        $resolvedTempParent = (Resolve-Path -LiteralPath ([System.IO.Path]::GetTempPath())).Path
        $tempLeaf = Split-Path -Leaf $resolvedTempRoot
        if (-not $resolvedTempRoot.StartsWith($resolvedTempParent, [System.StringComparison]::OrdinalIgnoreCase) -or
            -not $tempLeaf.StartsWith("appliedpackaging-readiness-", [System.StringComparison]::Ordinal)) {
            throw "Refusing to remove unexpected readiness self-test path: $resolvedTempRoot"
        }

        Remove-Item -LiteralPath $resolvedTempRoot -Recurse -Force
    }
}
