param(
    [switch] $SkipMarkdownLinks
)

$ErrorActionPreference = "Stop"

$repoRoot = (Resolve-Path (Join-Path $PSScriptRoot "..")).Path
Set-Location $repoRoot

$failures = [System.Collections.Generic.List[string]]::new()

function Add-Pass {
    param([string] $Message)
    Write-Host "[PASS] $Message" -ForegroundColor Green
}

function Add-Fail {
    param([string] $Message)
    $script:failures.Add($Message) | Out-Null
    Write-Host "[FAIL] $Message" -ForegroundColor Red
}

function Assert-PathExists {
    param([string] $Path)

    if (Test-Path -LiteralPath $Path) {
        Add-Pass "Required path exists: $Path"
    } else {
        Add-Fail "Required path exists: $Path"
    }
}

function Test-DocumentIndexMentions {
    param(
        [string] $Path,
        [string[]] $ExpectedFileNames
    )

    if (-not (Test-Path -LiteralPath $Path)) {
        Add-Fail "Document index source exists: $Path"
        return
    }

    $text = Get-Content -LiteralPath $Path -Raw
    foreach ($fileName in $ExpectedFileNames) {
        if ($text.Contains($fileName)) {
            Add-Pass "$Path mentions $fileName"
        } else {
            Add-Fail "$Path mentions $fileName"
        }
    }
}

function Test-MarkdownLinks {
    $markdownFiles = @()
    $markdownFiles += @(Get-ChildItem -Path "." -Filter "*.md" -File)
    if (Test-Path -LiteralPath "docs") {
        $markdownFiles += @(Get-ChildItem -Path "docs" -Filter "*.md" -File -Recurse)
    }

    $checkedLinks = 0
    $inlineLinkPattern = '(?<!!)\[[^\]]+\]\((?<target>[^)\r\n]+)\)'

    foreach ($file in $markdownFiles) {
        $content = Get-Content -LiteralPath $file.FullName -Raw
        $matches = [regex]::Matches($content, $inlineLinkPattern)
        foreach ($match in $matches) {
            $target = $match.Groups["target"].Value.Trim()
            if ($target.StartsWith("<") -and $target.EndsWith(">")) {
                $target = $target.Substring(1, $target.Length - 2)
            }

            if ([string]::IsNullOrWhiteSpace($target)) {
                continue
            }

            if ($target.StartsWith("#")) {
                continue
            }

            if ($target -match '^[A-Za-z][A-Za-z0-9+.-]*:') {
                continue
            }

            $pathPart = ($target -split '#', 2)[0]
            $pathPart = ($pathPart -split '\?', 2)[0]
            if ([string]::IsNullOrWhiteSpace($pathPart)) {
                continue
            }

            $pathPart = [System.Uri]::UnescapeDataString($pathPart)
            $basePath = Split-Path -Parent $file.FullName
            $candidate = Join-Path $basePath $pathPart
            $checkedLinks += 1
            if (-not (Test-Path -LiteralPath $candidate)) {
                $relativeFile = [System.IO.Path]::GetRelativePath($repoRoot, $file.FullName).Replace("\", "/")
                Add-Fail "Broken local markdown link in $relativeFile -> $target"
            }
        }
    }

    Add-Pass "Checked $checkedLinks local markdown link(s)"
}

Write-Host "Applied Packaging documentation audit" -ForegroundColor Cyan
Write-Host "Repository: $repoRoot"

$requiredPaths = @(
    "AGENTS.md",
    "README.md",
    "CHANGELOG.md",
    "LICENSE.md",
    "docs/design.md",
    "docs/00-document-index.md",
    "docs/01-requirements.md",
    "docs/02-system-architecture.md",
    "docs/03-detailed-design.md",
    "docs/04-asset-spec.md",
    "docs/05-implementation-plan.md",
    "docs/06-verification-release.md",
    "docs/07-references.md",
    "docs/08-change-intake.md",
    "docs/chat-summary.md",
    "docs/development-log.md",
    "docs/assets/palette.md",
    "docs/assets/acceptance.md",
    "docs/assets/asset-briefs/packages.md",
    "docs/assets/asset-briefs/machines.md",
    "docs/assets/asset-briefs/terminal-and-buses.md",
    "docs/assets/asset-briefs/ui-and-icons.md",
    "docs/assets/contracts/package_items.yaml",
    "docs/assets/contracts/me_packager.yaml",
    "docs/assets/contracts/package_assembler.yaml",
    "docs/assets/contracts/terminal_and_buses.yaml",
    "docs/assets/contracts/ui_icons.yaml",
    "docs/assets/reports/packages.md",
    "docs/assets/reports/machines.md",
    "docs/assets/reports/terminal-and-buses.md",
    "docs/assets/reports/ui-and-icons.md",
    "scripts/run-release-checks.ps1",
    "scripts/run-server-smoke.ps1",
    "scripts/verify-release.ps1",
    "scripts/verify-docs.ps1",
    "scripts/verify-release-readiness.ps1",
    "scripts/test-release-readiness.ps1",
    "scripts/test-release-check-plan.ps1",
    "scripts/test-release-bundle.ps1",
    "scripts/write-release-manifest.ps1",
    "scripts/verify-release-manifest.ps1",
    "scripts/write-release-bundle.ps1",
    "scripts/verify-release-bundle.ps1"
)

foreach ($path in $requiredPaths) {
    Assert-PathExists $path
}

$expectedDocNames = @(
    "00-document-index.md",
    "01-requirements.md",
    "02-system-architecture.md",
    "03-detailed-design.md",
    "04-asset-spec.md",
    "05-implementation-plan.md",
    "06-verification-release.md",
    "07-references.md",
    "08-change-intake.md",
    "chat-summary.md",
    "development-log.md"
)

Test-DocumentIndexMentions "docs/design.md" $expectedDocNames
Test-DocumentIndexMentions "docs/00-document-index.md" @(
    "01-requirements.md",
    "02-system-architecture.md",
    "03-detailed-design.md",
    "04-asset-spec.md",
    "05-implementation-plan.md",
    "06-verification-release.md",
    "07-references.md",
    "08-change-intake.md",
    "chat-summary.md",
    "development-log.md"
)

if (-not $SkipMarkdownLinks) {
    Test-MarkdownLinks
}

if ($failures.Count -gt 0) {
    Write-Host ""
    Write-Host "Documentation audit failed with $($failures.Count) issue(s):" -ForegroundColor Red
    foreach ($failure in $failures) {
        Write-Host " - $failure" -ForegroundColor Red
    }
    exit 1
}

Write-Host ""
Write-Host "Documentation audit passed." -ForegroundColor Green
