param(
    [switch] $SkipMarkdownLinks,
    [string] $RootPath = ""
)

$ErrorActionPreference = "Stop"

if ([string]::IsNullOrWhiteSpace($RootPath)) {
    $repoRoot = (Resolve-Path (Join-Path $PSScriptRoot "..")).Path
} else {
    $repoRoot = (Resolve-Path -LiteralPath $RootPath).Path
}
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
    if (Test-Path -LiteralPath "src/main/resources/assets/appliedpackaging/ae2guide") {
        $markdownFiles += @(
            Get-ChildItem -Path "src/main/resources/assets/appliedpackaging/ae2guide" -Filter "*.md" -File -Recurse
        )
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

function Test-GuideMePages {
    $guideRoot = "src/main/resources/assets/appliedpackaging/ae2guide"
    $translatedRoot = Join-Path $guideRoot "_zh_cn"
    if (-not (Test-Path -LiteralPath $guideRoot) -or -not (Test-Path -LiteralPath $translatedRoot)) {
        Add-Fail "GuideME English and zh_cn roots exist"
        return
    }

    $basePages = @(
        Get-ChildItem -LiteralPath $guideRoot -Filter "*.md" -File |
            Sort-Object Name |
            Select-Object -ExpandProperty Name
    )
    $translatedPages = @(
        Get-ChildItem -LiteralPath $translatedRoot -Filter "*.md" -File |
            Sort-Object Name |
            Select-Object -ExpandProperty Name
    )
    if (($basePages -join "`n") -eq ($translatedPages -join "`n")) {
        Add-Pass "GuideME English and zh_cn page sets match"
    } else {
        Add-Fail "GuideME English and zh_cn page sets match (English: $($basePages -join ', '); zh_cn: $($translatedPages -join ', '))"
    }

    foreach ($localeRoot in @($guideRoot, $translatedRoot)) {
        foreach ($pageName in $basePages) {
            $pagePath = Join-Path $localeRoot $pageName
            if (-not (Test-Path -LiteralPath $pagePath)) {
                continue
            }

            $content = Get-Content -LiteralPath $pagePath -Raw
            $relativePath = [System.IO.Path]::GetRelativePath($repoRoot, (Resolve-Path -LiteralPath $pagePath).Path).Replace("\", "/")
            $hasFrontmatter = $content.StartsWith("---") -and $content -match '(?m)^navigation:\s*$'
            $hasTitle = $content -match '(?m)^  title:\s*\S.*$'
            $hasIcon = $content -match '(?m)^  icon:\s*\S.*$'
            $hasParent = $pageName -eq "index.md" -or $content -match '(?m)^  parent:\s*index\.md\s*$'
            if ($hasFrontmatter -and $hasTitle -and $hasIcon -and $hasParent) {
                Add-Pass "GuideME navigation metadata is complete: $relativePath"
            } else {
                Add-Fail "GuideME navigation metadata is complete: $relativePath"
            }
        }
    }

    $expectedItemIdsByPage = [ordered]@{
        "packages.md" = @(
            "appliedpackaging:fluix_package",
            "appliedpackaging:white_package",
            "appliedpackaging:orange_package",
            "appliedpackaging:magenta_package",
            "appliedpackaging:light_blue_package",
            "appliedpackaging:yellow_package",
            "appliedpackaging:lime_package",
            "appliedpackaging:pink_package",
            "appliedpackaging:gray_package",
            "appliedpackaging:light_gray_package",
            "appliedpackaging:cyan_package",
            "appliedpackaging:purple_package",
            "appliedpackaging:blue_package",
            "appliedpackaging:brown_package",
            "appliedpackaging:green_package",
            "appliedpackaging:red_package",
            "appliedpackaging:black_package"
        )
        "advanced_pattern_terminal.md" = @(
            "appliedpackaging:advanced_pattern_encoding_terminal",
            "appliedpackaging:package_pattern",
            "appliedpackaging:advanced_processing_pattern"
        )
        "package_assembler.md" = @("appliedpackaging:package_assembler")
        "me_packager.md" = @("appliedpackaging:me_packager")
        "package_buses.md" = @(
            "appliedpackaging:package_storage_bus",
            "appliedpackaging:package_unpacking_bus"
        )
        "sequence_buffer.md" = @("appliedpackaging:sequence_buffer")
    }
    foreach ($entry in $expectedItemIdsByPage.GetEnumerator()) {
        $pagePath = Join-Path $guideRoot $entry.Key
        if (-not (Test-Path -LiteralPath $pagePath)) {
            continue
        }

        $content = Get-Content -LiteralPath $pagePath -Raw
        foreach ($itemId in $entry.Value) {
            if ($content -match "(?m)^\s*-\s+$([regex]::Escape($itemId))\s*$") {
                Add-Pass "GuideME item index maps $itemId to $($entry.Key)"
            } else {
                Add-Fail "GuideME item index maps $itemId to $($entry.Key)"
            }
        }
    }

    $indexPath = Join-Path $guideRoot "index.md"
    if (Test-Path -LiteralPath $indexPath) {
        $indexText = Get-Content -LiteralPath $indexPath -Raw
        foreach ($category in @(
            "applied packaging items",
            "applied packaging devices",
            "applied packaging machines"
        )) {
            if ($indexText.Contains("<CategoryIndex category=`"$category`" />")) {
                Add-Pass "GuideME index includes category: $category"
            } else {
                Add-Fail "GuideME index includes category: $category"
            }
        }
    }

    $expectedTagCounts = [ordered]@{
        "advanced_pattern_terminal.md|GameScene" = 1
        "advanced_pattern_terminal.md|RecipeFor" = 1
        "package_assembler.md|BlockImage" = 1
        "package_assembler.md|RecipeFor" = 1
        "me_packager.md|BlockImage" = 1
        "me_packager.md|RecipeFor" = 1
        "package_buses.md|GameScene" = 2
        "package_buses.md|RecipeFor" = 2
        "sequence_buffer.md|BlockImage" = 1
        "sequence_buffer.md|RecipeFor" = 1
        "example_setups.md|GameScene" = 3
    }
    foreach ($entry in $expectedTagCounts.GetEnumerator()) {
        $parts = $entry.Key -split '\|', 2
        $pagePath = Join-Path $guideRoot $parts[0]
        if (-not (Test-Path -LiteralPath $pagePath)) {
            continue
        }

        $content = Get-Content -LiteralPath $pagePath -Raw
        $count = [regex]::Matches($content, "<$($parts[1])(?:\s|>)").Count
        if ($count -ge $entry.Value) {
            Add-Pass "GuideME $($parts[0]) includes at least $($entry.Value) $($parts[1]) tag(s)"
        } else {
            Add-Fail "GuideME $($parts[0]) includes at least $($entry.Value) $($parts[1]) tag(s)"
        }
    }

    $resolvedGuideRoot = [System.IO.Path]::GetFullPath((Join-Path $repoRoot $guideRoot)).TrimEnd("\")
    foreach ($pageFile in Get-ChildItem -LiteralPath $guideRoot -Filter "*.md" -File -Recurse) {
        $content = Get-Content -LiteralPath $pageFile.FullName -Raw
        foreach ($match in [regex]::Matches($content, '<ImportStructure\s+src="(?<src>[^"]+)"')) {
            $src = $match.Groups["src"].Value
            $candidate = [System.IO.Path]::GetFullPath((Join-Path $pageFile.DirectoryName $src))
            $relativePage = [System.IO.Path]::GetRelativePath($repoRoot, $pageFile.FullName).Replace("\", "/")
            if (-not ($candidate.Equals($resolvedGuideRoot, [System.StringComparison]::OrdinalIgnoreCase) -or
                    $candidate.StartsWith($resolvedGuideRoot + "\", [System.StringComparison]::OrdinalIgnoreCase))) {
                Add-Fail "GuideME structure reference escapes guide root in $relativePage -> $src"
            } elseif (-not (Test-Path -LiteralPath $candidate)) {
                Add-Fail "Missing GuideME structure in $relativePage -> $src"
            } else {
                Add-Pass "GuideME structure reference resolves in $relativePage -> $src"
            }
        }
    }
}

function Test-FormalDocsHaveNoUnresolvedPlaceholders {
    $formalDocPaths = @(
        "docs/design.md",
        "docs/00-document-index.md",
        "docs/01-requirements.md",
        "docs/02-system-architecture.md",
        "docs/03-detailed-design.md",
        "docs/04-asset-spec.md",
        "docs/05-implementation-plan.md",
        "docs/07-references.md"
    )

    $placeholderPattern = "(?i)\bTODO\b|\bFIXME\b|\bTBD\b|待定|待补充|等待\s+[A-Z]\b"
    $placeholderMatches = [System.Collections.Generic.List[string]]::new()
    foreach ($path in $formalDocPaths) {
        if (-not (Test-Path -LiteralPath $path)) {
            continue
        }

        $lines = Get-Content -LiteralPath $path
        for ($index = 0; $index -lt $lines.Count; $index++) {
            if ($lines[$index] -match $placeholderPattern) {
                $placeholderMatches.Add("${path}:$($index + 1): $($lines[$index].Trim())") | Out-Null
            }
        }
    }

    if ($placeholderMatches.Count -eq 0) {
        Add-Pass "Formal design docs contain no unresolved placeholders"
    } else {
        Add-Fail "Unresolved placeholder in formal docs: $($placeholderMatches -join '; ')"
    }
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
    "src/main/resources/assets/appliedpackaging/ae2guide/index.md",
    "src/main/resources/assets/appliedpackaging/ae2guide/workflow.md",
    "src/main/resources/assets/appliedpackaging/ae2guide/packages.md",
    "src/main/resources/assets/appliedpackaging/ae2guide/advanced_pattern_terminal.md",
    "src/main/resources/assets/appliedpackaging/ae2guide/package_assembler.md",
    "src/main/resources/assets/appliedpackaging/ae2guide/me_packager.md",
    "src/main/resources/assets/appliedpackaging/ae2guide/package_buses.md",
    "src/main/resources/assets/appliedpackaging/ae2guide/sequence_buffer.md",
    "src/main/resources/assets/appliedpackaging/ae2guide/example_setups.md",
    "src/main/resources/assets/appliedpackaging/ae2guide/troubleshooting.md",
    "src/main/resources/assets/appliedpackaging/ae2guide/_zh_cn/index.md",
    "src/main/resources/assets/appliedpackaging/ae2guide/_zh_cn/workflow.md",
    "src/main/resources/assets/appliedpackaging/ae2guide/_zh_cn/packages.md",
    "src/main/resources/assets/appliedpackaging/ae2guide/_zh_cn/advanced_pattern_terminal.md",
    "src/main/resources/assets/appliedpackaging/ae2guide/_zh_cn/package_assembler.md",
    "src/main/resources/assets/appliedpackaging/ae2guide/_zh_cn/me_packager.md",
    "src/main/resources/assets/appliedpackaging/ae2guide/_zh_cn/package_buses.md",
    "src/main/resources/assets/appliedpackaging/ae2guide/_zh_cn/sequence_buffer.md",
    "src/main/resources/assets/appliedpackaging/ae2guide/_zh_cn/example_setups.md",
    "src/main/resources/assets/appliedpackaging/ae2guide/_zh_cn/troubleshooting.md",
    "src/main/resources/assets/appliedpackaging/ae2guide/assets/blocks/advanced_pattern_encoding_terminal.snbt",
    "src/main/resources/assets/appliedpackaging/ae2guide/assets/blocks/package_storage_bus.snbt",
    "src/main/resources/assets/appliedpackaging/ae2guide/assets/blocks/package_unpacking_bus.snbt",
    "src/main/resources/assets/appliedpackaging/ae2guide/assets/assemblies/package_assembly_line.snbt",
    "src/main/resources/assets/appliedpackaging/ae2guide/assets/assemblies/package_routing.snbt",
    "src/main/resources/assets/appliedpackaging/ae2guide/assets/assemblies/sequence_line.snbt",
    "scripts/run-release-checks.ps1",
    "scripts/run-server-smoke.ps1",
    "scripts/verify-release.ps1",
    "scripts/verify-assets.ps1",
    "scripts/verify-docs.ps1",
    "scripts/verify-release-readiness.ps1",
    "scripts/test-docs-audit.ps1",
    "scripts/test-assets-audit.ps1",
    "scripts/test-release-audit.ps1",
    "scripts/test-release-self-tests.ps1",
    "scripts/test-release-readiness.ps1",
    "scripts/test-release-check-plan.ps1",
    "scripts/test-release-manifest.ps1",
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

Test-FormalDocsHaveNoUnresolvedPlaceholders
Test-GuideMePages

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
