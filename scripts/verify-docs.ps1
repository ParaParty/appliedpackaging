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

function Assert-True {
    param([bool] $Condition, [string] $Message)
    if ($Condition) {
        Add-Pass $Message
    } else {
        Add-Fail $Message
    }
}

$requiredFiles = @(
    "README.md",
    "CHANGELOG.md",
    "LICENSE.md",
    "AGENTS.md",
    "docs/design.md",
    "docs/verification.md"
)
foreach ($path in $requiredFiles) {
    Assert-True (Test-Path -LiteralPath $path -PathType Leaf) "Required document exists: $path"
}

$coreDocs = @(Get-ChildItem -LiteralPath "docs" -Filter "*.md" -File -Recurse)
$coreDocPaths = @($coreDocs | ForEach-Object {
    [System.IO.Path]::GetRelativePath($repoRoot, $_.FullName).Replace("\", "/")
} | Sort-Object)
$expectedCoreDocs = @("docs/design.md", "docs/verification.md")
Assert-True (($coreDocPaths -join "`n") -eq ($expectedCoreDocs -join "`n")) `
    "docs contains only the design and verification sources"

$placeholderPattern = '(?im)(^|\W)(TODO|FIXME|TBD|待定|待补充|等待输入|等待确认)(\W|$)'
foreach ($path in $expectedCoreDocs) {
    if (Test-Path -LiteralPath $path) {
        $content = Get-Content -LiteralPath $path -Raw
        Assert-True (-not [regex]::IsMatch($content, $placeholderPattern)) `
            "Core document has no unresolved placeholder: $path"
    }
}

$guideRoot = "src/main/resources/assets/appliedpackaging/ae2guide"
$translatedRoot = Join-Path $guideRoot "_zh_cn"
Assert-True (Test-Path -LiteralPath $guideRoot -PathType Container) "GuideME English root exists"
Assert-True (Test-Path -LiteralPath $translatedRoot -PathType Container) "GuideME zh_cn root exists"

if ((Test-Path -LiteralPath $guideRoot) -and (Test-Path -LiteralPath $translatedRoot)) {
    $resolvedGuideRoot = (Resolve-Path -LiteralPath $guideRoot).Path
    $resolvedTranslatedRoot = (Resolve-Path -LiteralPath $translatedRoot).Path
    $englishPages = @(
        Get-ChildItem -LiteralPath $guideRoot -Filter "*.md" -File -Recurse |
            Where-Object {
                -not $_.FullName.StartsWith(
                    $resolvedTranslatedRoot + [System.IO.Path]::DirectorySeparatorChar,
                    [System.StringComparison]::OrdinalIgnoreCase)
            } |
            ForEach-Object {
                [System.IO.Path]::GetRelativePath($resolvedGuideRoot, $_.FullName).Replace("\", "/")
            } |
            Sort-Object
    )
    $translatedPages = @(
        Get-ChildItem -LiteralPath $translatedRoot -Filter "*.md" -File -Recurse |
            ForEach-Object {
                [System.IO.Path]::GetRelativePath($resolvedTranslatedRoot, $_.FullName).Replace("\", "/")
            } |
            Sort-Object
    )
    Assert-True ($englishPages.Count -gt 0) "GuideME contains pages"
    Assert-True (($englishPages -join "`n") -eq ($translatedPages -join "`n")) `
        "GuideME English and zh_cn page sets match"

    foreach ($locale in @(
        @{ Name = "English"; Root = $guideRoot },
        @{ Name = "zh_cn"; Root = $translatedRoot }
    )) {
        foreach ($page in $englishPages) {
            $path = Join-Path $locale.Root $page
            if (-not (Test-Path -LiteralPath $path)) {
                continue
            }
            $content = Get-Content -LiteralPath $path -Raw
            $frontmatter = [regex]::Match($content, '(?s)\A---\s*\r?\n(?<yaml>.*?)\r?\n---(?:\s*\r?\n|\s*\z)')
            $valid = $frontmatter.Success -and
                $frontmatter.Groups["yaml"].Value -match '(?m)^navigation:\s*$' -and
                $frontmatter.Groups["yaml"].Value -match '(?m)^  title:\s*\S.*$' -and
                $frontmatter.Groups["yaml"].Value -match '(?m)^  icon:\s*\S.*$'
            Assert-True $valid "GuideME navigation metadata is complete: $($locale.Name)/$page"
        }
    }
}

$markdownFiles = @()
$markdownFiles += @(Get-ChildItem -LiteralPath "." -Filter "*.md" -File)
$markdownFiles += $coreDocs
if (Test-Path -LiteralPath $guideRoot) {
    $markdownFiles += @(Get-ChildItem -LiteralPath $guideRoot -Filter "*.md" -File -Recurse)
}

$inlineLinkPattern = '(?<!!)\[[^\]]+\]\((?<target>[^)\r\n]+)\)'
$checkedLinks = 0
foreach ($file in $markdownFiles) {
    $content = Get-Content -LiteralPath $file.FullName -Raw
    foreach ($match in [regex]::Matches($content, $inlineLinkPattern)) {
        $target = $match.Groups["target"].Value.Trim().Trim('<', '>')
        if ([string]::IsNullOrWhiteSpace($target) -or
                $target.StartsWith("#") -or
                $target -match '^[A-Za-z][A-Za-z0-9+.-]*:') {
            continue
        }
        $pathPart = (($target -split '#', 2)[0] -split '\?', 2)[0]
        if ([string]::IsNullOrWhiteSpace($pathPart)) {
            continue
        }
        $candidate = Join-Path $file.DirectoryName ([System.Uri]::UnescapeDataString($pathPart))
        $checkedLinks += 1
        if (-not (Test-Path -LiteralPath $candidate)) {
            $relativeFile = [System.IO.Path]::GetRelativePath($repoRoot, $file.FullName).Replace("\", "/")
            Add-Fail "Broken local Markdown link in $relativeFile -> $target"
        }
    }
}
Add-Pass "Checked $checkedLinks local Markdown link(s)"

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
