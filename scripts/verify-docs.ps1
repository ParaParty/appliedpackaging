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

function Assert-FileMatches {
    param(
        [string] $Path,
        [string] $Pattern,
        [string] $Message
    )

    if (-not (Test-Path -LiteralPath $Path)) {
        Add-Fail $Message
        return
    }

    $text = Get-Content -LiteralPath $Path -Raw
    if ([regex]::IsMatch($text, $Pattern)) {
        Add-Pass $Message
    } else {
        Add-Fail $Message
    }
}

function Assert-FileMatchCount {
    param(
        [string] $Path,
        [string] $Pattern,
        [int] $ExpectedCount,
        [string] $Message
    )

    if (-not (Test-Path -LiteralPath $Path)) {
        Add-Fail $Message
        return
    }

    $text = Get-Content -LiteralPath $Path -Raw
    $actualCount = [regex]::Matches($text, $Pattern).Count
    if ($actualCount -eq $ExpectedCount) {
        Add-Pass $Message
    } else {
        Add-Fail "$Message (expected: $ExpectedCount; actual: $actualCount)"
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

    $resolvedGuideRoot = (Resolve-Path -LiteralPath $guideRoot).Path
    $resolvedTranslatedRoot = (Resolve-Path -LiteralPath $translatedRoot).Path
    $basePages = @(
        Get-ChildItem -LiteralPath $guideRoot -Filter "*.md" -File -Recurse |
            Where-Object { -not $_.FullName.StartsWith($resolvedTranslatedRoot + [System.IO.Path]::DirectorySeparatorChar, [System.StringComparison]::OrdinalIgnoreCase) } |
            ForEach-Object { [System.IO.Path]::GetRelativePath($resolvedGuideRoot, $_.FullName).Replace("\", "/") } |
            Sort-Object
    )
    $translatedPages = @(
        Get-ChildItem -LiteralPath $translatedRoot -Filter "*.md" -File -Recurse |
            ForEach-Object { [System.IO.Path]::GetRelativePath($resolvedTranslatedRoot, $_.FullName).Replace("\", "/") } |
            Sort-Object
    )
    $expectedPages = @(
        "devices/advanced-pattern-terminal.md",
        "devices/index.md",
        "devices/package-storage-bus.md",
        "devices/package-unpacking-bus.md",
        "example-setups/basic-packaging-line.md",
        "example-setups/index.md",
        "example-setups/multi-color-routing.md",
        "example-setups/ordered-machine-inputs.md",
        "example-setups/ordered-machine-inputs/mechanical-crafting.md",
        "example-setups/ordered-machine-inputs/multi-step-crafting.md",
        "example-setups/ordered-machine-inputs/package-routing.md",
        "example-setups/ordered-machine-inputs/parallel-furnaces.md",
        "index.md",
        "machines/index.md",
        "machines/me-packager.md",
        "machines/package-assembler.md",
        "machines/sequence-buffer.md",
        "packaging-concepts/getting-started.md",
        "packaging-concepts/index.md",
        "packaging-concepts/packages.md",
        "troubleshooting.md"
    ) | Sort-Object
    if (($basePages -join "`n") -eq ($expectedPages -join "`n")) {
        Add-Pass "GuideME page set matches the expected hierarchy"
    } else {
        Add-Fail "GuideME page set matches the expected hierarchy (expected: $($expectedPages -join ', '); actual: $($basePages -join ', '))"
    }
    if (($basePages -join "`n") -eq ($translatedPages -join "`n")) {
        Add-Pass "GuideME English and zh_cn page sets match"
    } else {
        Add-Fail "GuideME English and zh_cn page sets match (English: $($basePages -join ', '); zh_cn: $($translatedPages -join ', '))"
    }

    foreach ($locale in @(
        @{ Name = "English"; Root = $guideRoot },
        @{ Name = "zh_cn"; Root = $translatedRoot }
    )) {
        $parents = @{}
        foreach ($pageName in $basePages) {
            $pagePath = Join-Path $locale.Root $pageName
            if (-not (Test-Path -LiteralPath $pagePath)) {
                continue
            }

            $content = Get-Content -LiteralPath $pagePath -Raw
            $relativePath = [System.IO.Path]::GetRelativePath($repoRoot, (Resolve-Path -LiteralPath $pagePath).Path).Replace("\", "/")
            $frontmatter = [regex]::Match($content, '(?s)\A---\s*\r?\n(?<yaml>.*?)\r?\n---(?:\s*\r?\n|\s*\z)')
            $yaml = if ($frontmatter.Success) { $frontmatter.Groups["yaml"].Value } else { "" }
            $hasNavigation = $yaml -match '(?m)^navigation:\s*$'
            $hasTitle = $yaml -match '(?m)^  title:\s*\S.*$'
            $hasIcon = $yaml -match '(?m)^  icon:\s*\S.*$'
            $parentMatch = [regex]::Match($yaml, '(?m)^  parent:\s*(?<parent>\S+)\s*$')
            $parentIsValid = $true
            if ($pageName -eq "index.md") {
                if ($parentMatch.Success) {
                    Add-Fail "GuideME navigation root has no parent: $relativePath"
                    $parentIsValid = $false
                }
            } elseif (-not $parentMatch.Success) {
                Add-Fail "GuideME navigation page has a parent: $relativePath"
                $parentIsValid = $false
            } else {
                $parent = $parentMatch.Groups["parent"].Value.Replace("\", "/")
                $parents[$pageName] = $parent
                if ($parent -eq $pageName) {
                    Add-Fail "GuideME navigation page is not its own parent: $relativePath"
                    $parentIsValid = $false
                } elseif ($parent -notin $basePages) {
                    Add-Fail "GuideME navigation parent exists: $relativePath -> $parent"
                    $parentIsValid = $false
                }
            }
            if ($frontmatter.Success -and $hasNavigation -and $hasTitle -and $hasIcon -and $parentIsValid) {
                Add-Pass "GuideME navigation metadata is complete: $relativePath"
            } else {
                Add-Fail "GuideME navigation metadata is complete: $relativePath"
            }
        }

        $hasCycle = $false
        foreach ($pageName in $basePages) {
            $seen = [System.Collections.Generic.HashSet[string]]::new([System.StringComparer]::Ordinal)
            $current = $pageName
            while ($parents.ContainsKey($current)) {
                if (-not $seen.Add($current)) {
                    Add-Fail "GuideME navigation tree is acyclic for $($locale.Name): $pageName"
                    $hasCycle = $true
                    break
                }
                $current = $parents[$current]
            }
        }
        if (-not $hasCycle) {
            Add-Pass "GuideME navigation tree is acyclic for $($locale.Name)"
        }
    }

    $expectedItemIdsByPage = [ordered]@{
        "packaging-concepts/packages.md" = @(
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
        "devices/advanced-pattern-terminal.md" = @(
            "appliedpackaging:advanced_pattern_encoding_terminal",
            "appliedpackaging:package_pattern",
            "appliedpackaging:advanced_processing_pattern"
        )
        "machines/package-assembler.md" = @("appliedpackaging:package_assembler")
        "machines/me-packager.md" = @("appliedpackaging:me_packager")
        "devices/package-storage-bus.md" = @("appliedpackaging:package_storage_bus")
        "devices/package-unpacking-bus.md" = @("appliedpackaging:package_unpacking_bus")
        "machines/sequence-buffer.md" = @("appliedpackaging:sequence_buffer")
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

    $itemIndexOwners = @{}
    foreach ($pageName in $basePages) {
        $englishPath = Join-Path $guideRoot $pageName
        $translatedPath = Join-Path $translatedRoot $pageName
        if (-not (Test-Path -LiteralPath $englishPath) -or -not (Test-Path -LiteralPath $translatedPath)) {
            continue
        }

        $englishContent = Get-Content -LiteralPath $englishPath -Raw
        $translatedContent = Get-Content -LiteralPath $translatedPath -Raw
        $itemBlockPattern = '(?ms)^item_ids:\s*\r?\n(?<items>(?:-\s+\S+\s*\r?\n)+)'
        $englishBlock = [regex]::Match($englishContent, $itemBlockPattern)
        $translatedBlock = [regex]::Match($translatedContent, $itemBlockPattern)
        $englishItemIds = @()
        $translatedItemIds = @()
        if ($englishBlock.Success) {
            $englishItemIds = @(
                [regex]::Matches($englishBlock.Groups["items"].Value, '(?m)^-\s+(?<id>\S+)\s*$') |
                    ForEach-Object { $_.Groups["id"].Value }
            )
        }
        if ($translatedBlock.Success) {
            $translatedItemIds = @(
                [regex]::Matches($translatedBlock.Groups["items"].Value, '(?m)^-\s+(?<id>\S+)\s*$') |
                    ForEach-Object { $_.Groups["id"].Value }
            )
        }

        if (($englishItemIds -join "`n") -eq ($translatedItemIds -join "`n")) {
            Add-Pass "GuideME English and zh_cn item indexes match: $pageName"
        } else {
            Add-Fail "GuideME English and zh_cn item indexes match: $pageName"
        }

        foreach ($itemId in $englishItemIds) {
            if ($itemIndexOwners.ContainsKey($itemId)) {
                Add-Fail "GuideME item index is unique: $itemId maps to both $($itemIndexOwners[$itemId]) and $pageName"
            } else {
                $itemIndexOwners[$itemId] = $pageName
            }
        }
    }
    Add-Pass "Checked $($itemIndexOwners.Count) unique GuideME item index entr$(if ($itemIndexOwners.Count -eq 1) { 'y' } else { 'ies' })"

    $expectedTagCounts = [ordered]@{
        "devices/advanced-pattern-terminal.md|GameScene" = 1
        "devices/advanced-pattern-terminal.md|RecipeFor" = 1
        "devices/index.md|SubPages" = 1
        "devices/package-storage-bus.md|GameScene" = 1
        "devices/package-storage-bus.md|RecipeFor" = 1
        "devices/package-unpacking-bus.md|GameScene" = 1
        "devices/package-unpacking-bus.md|RecipeFor" = 1
        "example-setups/basic-packaging-line.md|GameScene" = 1
        "example-setups/index.md|SubPages" = 1
        "example-setups/multi-color-routing.md|GameScene" = 1
        "example-setups/ordered-machine-inputs.md|GameScene" = 2
        "example-setups/ordered-machine-inputs.md|SubPages" = 1
        "example-setups/ordered-machine-inputs/mechanical-crafting.md|GameScene" = 1
        "example-setups/ordered-machine-inputs/multi-step-crafting.md|GameScene" = 1
        "example-setups/ordered-machine-inputs/package-routing.md|GameScene" = 1
        "example-setups/ordered-machine-inputs/parallel-furnaces.md|GameScene" = 1
        "machines/index.md|SubPages" = 1
        "machines/me-packager.md|BlockImage" = 1
        "machines/me-packager.md|GameScene" = 1
        "machines/me-packager.md|RecipeFor" = 1
        "machines/package-assembler.md|BlockImage" = 1
        "machines/package-assembler.md|GameScene" = 3
        "machines/package-assembler.md|RecipeFor" = 1
        "machines/sequence-buffer.md|BlockImage" = 1
        "machines/sequence-buffer.md|GameScene" = 2
        "machines/sequence-buffer.md|RecipeFor" = 1
        "packaging-concepts/index.md|SubPages" = 1
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
            # GuideME registers a translated file such as _zh_cn/devices/page.md
            # under the canonical page id devices/page.md. Runtime-relative
            # resources are therefore resolved from the canonical page directory,
            # not from the physical _zh_cn directory in the source tree.
            $runtimePage = if ($pageFile.FullName.StartsWith(
                    $resolvedTranslatedRoot + [System.IO.Path]::DirectorySeparatorChar,
                    [System.StringComparison]::OrdinalIgnoreCase)) {
                [System.IO.Path]::GetRelativePath($resolvedTranslatedRoot, $pageFile.FullName)
            } else {
                [System.IO.Path]::GetRelativePath($resolvedGuideRoot, $pageFile.FullName)
            }
            $runtimePageDirectory = Split-Path -Parent (Join-Path $resolvedGuideRoot $runtimePage)
            $candidate = [System.IO.Path]::GetFullPath((Join-Path $runtimePageDirectory $src))
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

    $explicitHelpTopicsChecked = 0
    $javaRoot = Join-Path $repoRoot "src/main/java"
    if (Test-Path -LiteralPath $javaRoot) {
        foreach ($javaFile in Get-ChildItem -LiteralPath $javaRoot -Filter "*.java" -File -Recurse) {
            $content = Get-Content -LiteralPath $javaFile.FullName -Raw
            foreach ($match in [regex]::Matches(
                    $content,
                    'AppliedPackaging\.id\("(?<page>[^"\r\n]+\.md)"\)')) {
                $page = $match.Groups["page"].Value.Replace("\", "/")
                $candidate = [System.IO.Path]::GetFullPath((Join-Path $resolvedGuideRoot $page))
                $relativeJava = [System.IO.Path]::GetRelativePath($repoRoot, $javaFile.FullName).Replace("\", "/")
                $explicitHelpTopicsChecked += 1
                if (-not ($candidate.Equals($resolvedGuideRoot, [System.StringComparison]::OrdinalIgnoreCase) -or
                        $candidate.StartsWith($resolvedGuideRoot + "\", [System.StringComparison]::OrdinalIgnoreCase))) {
                    Add-Fail "GuideME help topic escapes guide root in $relativeJava -> $page"
                } elseif (-not (Test-Path -LiteralPath $candidate)) {
                    Add-Fail "Missing GuideME help topic in $relativeJava -> $page"
                } else {
                    Add-Pass "GuideME help topic resolves in $relativeJava -> $page"
                }
            }
        }
    }
    Add-Pass "Checked $explicitHelpTopicsChecked explicit GuideME help topic(s)"

    $orderedInputsPage = Join-Path $guideRoot "example-setups/ordered-machine-inputs.md"
    $orderedInputsZhPage = Join-Path $translatedRoot "example-setups/ordered-machine-inputs.md"
    $mechanicalCraftingPage = Join-Path $guideRoot "example-setups/ordered-machine-inputs/mechanical-crafting.md"
    $mechanicalCraftingZhPage = Join-Path $translatedRoot "example-setups/ordered-machine-inputs/mechanical-crafting.md"
    $parallelFurnacesPage = Join-Path $guideRoot "example-setups/ordered-machine-inputs/parallel-furnaces.md"
    $parallelFurnacesZhPage = Join-Path $translatedRoot "example-setups/ordered-machine-inputs/parallel-furnaces.md"
    $multiStepPage = Join-Path $guideRoot "example-setups/ordered-machine-inputs/multi-step-crafting.md"
    $multiStepZhPage = Join-Path $translatedRoot "example-setups/ordered-machine-inputs/multi-step-crafting.md"
    $packageRoutingPage = Join-Path $guideRoot "example-setups/ordered-machine-inputs/package-routing.md"
    $packageRoutingZhPage = Join-Path $translatedRoot "example-setups/ordered-machine-inputs/package-routing.md"

    Assert-FileMatchCount $orderedInputsPage '(?m)^## Example [1-4]:' 0 "Ordered-input English overview does not embed the four example chapters"
    Assert-FileMatchCount $orderedInputsZhPage '(?m)^## 示例 [1-4]：' 0 "Ordered-input zh_cn overview does not embed the four example chapters"
    Assert-FileMatchCount $orderedInputsPage '<GameScene(?:\s|>)' 2 "Ordered-input English overview keeps only its two generic input scenes"
    Assert-FileMatchCount $orderedInputsZhPage '<GameScene(?:\s|>)' 2 "Ordered-input zh_cn overview keeps only its two generic input scenes"
    Assert-FileMatches $orderedInputsPage '<SubPages\s*/>' "Ordered-input English overview lists its example subpages"
    Assert-FileMatches $orderedInputsZhPage '<SubPages\s*/>' "Ordered-input zh_cn overview lists its example subpages"

    $orderedExamplePages = @(
        @{ English = $mechanicalCraftingPage; Chinese = $mechanicalCraftingZhPage; Number = 1; Position = 10; Structure = 'sequence_mechanical_crafting_5x5\.snbt' },
        @{ English = $parallelFurnacesPage; Chinese = $parallelFurnacesZhPage; Number = 2; Position = 20; Structure = 'sequence_furnace_array\.snbt' },
        @{ English = $multiStepPage; Chinese = $multiStepZhPage; Number = 3; Position = 30; Structure = 'advanced_multistep_crafting\.snbt' },
        @{ English = $packageRoutingPage; Chinese = $packageRoutingZhPage; Number = 4; Position = 40; Structure = 'packager_color_routing\.snbt' }
    )
    foreach ($examplePage in $orderedExamplePages) {
        Assert-FileMatches $examplePage.English "(?s)parent:\s*example-setups/ordered-machine-inputs\.md.*?position:\s*$($examplePage.Position)" "Ordered-input English example $($examplePage.Number) is a correctly ordered child page"
        Assert-FileMatches $examplePage.Chinese "(?s)parent:\s*example-setups/ordered-machine-inputs\.md.*?position:\s*$($examplePage.Position)" "Ordered-input zh_cn example $($examplePage.Number) is a correctly ordered child page"
        Assert-FileMatchCount $examplePage.English '<GameScene(?:\s|>)' 1 "Ordered-input English example $($examplePage.Number) owns exactly one scene"
        Assert-FileMatchCount $examplePage.Chinese '<GameScene(?:\s|>)' 1 "Ordered-input zh_cn example $($examplePage.Number) owns exactly one scene"
        Assert-FileMatches $examplePage.English $examplePage.Structure "Ordered-input English example $($examplePage.Number) references its own structure"
        Assert-FileMatches $examplePage.Chinese $examplePage.Structure "Ordered-input zh_cn example $($examplePage.Number) references its own structure"
    }

    Assert-FileMatchCount $mechanicalCraftingPage '9×9|9x9' 0 "Mechanical crafting English guide no longer describes a 9x9 scene"
    Assert-FileMatchCount $mechanicalCraftingZhPage '9×9|9x9' 0 "Mechanical crafting zh_cn guide no longer describes a 9x9 scene"
    Assert-FileMatches $mechanicalCraftingPage '(?s)horizontal Mechanical Crafter layout.*?JEI.*?Transpose Recipe.*?Outputs are not transposed' "Mechanical crafting guide explains JEI transfer followed by transpose for a horizontal layout"
    Assert-FileMatches $mechanicalCraftingZhPage '(?s)动力合成器水平铺设.*?JEI.*?转置配方.*?产物不会随转置改变' "Mechanical crafting zh_cn guide explains JEI transfer followed by transpose for a horizontal layout"

    $assemblyRoot = Join-Path $guideRoot "assets/assemblies"
    $blockRoot = Join-Path $guideRoot "assets/blocks"
    $packageGrid = Join-Path $assemblyRoot "package_assembly_line.snbt"
    Assert-FileMatches $packageGrid '(?m)^\s*size:\s*\[3,\s*2,\s*2\],?\s*$' "Package Assembler scene bounds fit a 2x2x2 grid plus one cable link"
    Assert-FileMatchCount $packageGrid 'state:\s*"ae2:pattern_provider\{' 4 "Package Assembler grid contains 4 Pattern Providers"
    Assert-FileMatchCount $packageGrid 'state:\s*"appliedpackaging:package_assembler\{' 4 "Package Assembler grid contains 4 Package Assemblers"
    Assert-FileMatchCount $packageGrid 'output_mode:\s*"me_network"' 4 "Package Assembler grid returns packages to ME storage"
    Assert-FileMatchCount $packageGrid 'id:\s*"ae2:fluix_smart_cable"' 1 "Package Assembler grid leaves one Smart Cable connection to the main network"
    Assert-FileMatches $packageGrid 'pos:\s*\[2,\s*0,\s*0\].*id:\s*"ae2:fluix_smart_cable"' "Package Assembler main-network cable is outside the 2x2x2 grid core"

    $hopperAssembler = Join-Path $assemblyRoot "package_assembler_hopper.snbt"
    Assert-FileMatchCount $hopperAssembler 'state:\s*"minecraft:hopper\{' 2 "Inserted-pattern setup contains input and output hoppers"
    Assert-FileMatchCount $hopperAssembler 'state:\s*"appliedpackaging:package_assembler\{' 1 "Inserted-pattern setup contains one Package Assembler"
    Assert-FileMatches $hopperAssembler 'output_mode:\s*"adjacent_block"' "Inserted-pattern setup uses adjacent-block output"
    Assert-FileMatches $hopperAssembler 'state:\s*"ae2:energy_cell\{' "Inserted-pattern setup powers the Package Assembler"

    $subnetworkAssembler = Join-Path $assemblyRoot "package_assembler_subnetwork.snbt"
    Assert-FileMatches $subnetworkAssembler 'state:\s*"ae2:pattern_provider\{push_direction:east\}"' "Routing setup uses a directional Pattern Provider"
    Assert-FileMatches $subnetworkAssembler 'output_mode:\s*"me_network"' "Routing setup outputs from the Package Assembler to its subnetwork"
    Assert-FileMatchCount $subnetworkAssembler 'id:\s*"appliedpackaging:package_storage_bus"' 2 "Routing setup contains two Package Storage Buses"
    Assert-FileMatches $subnetworkAssembler 'colors:\s*\[I;\s*15,' "Routing setup contains a red Package Storage Bus filter"
    Assert-FileMatches $subnetworkAssembler 'colors:\s*\[I;\s*12,' "Routing setup contains a blue Package Storage Bus filter"

    $mePackagerStructure = Join-Path $assemblyRoot "me_packager_network.snbt"
    Assert-FileMatchCount $mePackagerStructure 'state:\s*"minecraft:chest\{' 1 "ME Packager scene contains one chest"
    Assert-FileMatchCount $mePackagerStructure 'id:\s*"ae2:storage_bus"' 1 "ME Packager scene uses one ordinary AE2 Storage Bus"
    Assert-FileMatchCount $mePackagerStructure 'id:\s*"appliedpackaging:package_storage_bus"' 0 "ME Packager scene does not substitute a Package Storage Bus"
    Assert-FileMatchCount $mePackagerStructure 'state:\s*"appliedpackaging:me_packager\{' 1 "ME Packager scene contains one ME Packager"
    Assert-FileMatchCount $mePackagerStructure 'state:\s*"ae2:energy_cell\{' 1 "ME Packager scene contains one Energy Cell"
    Assert-FileMatches $mePackagerStructure 'id:\s*"ae2:fluix_smart_cable"' "ME Packager scene connects the components with Smart Cable"

    $terminalStructure = Join-Path $blockRoot "advanced_pattern_encoding_terminal.snbt"
    Assert-FileMatches $terminalStructure 'id:\s*"appliedpackaging:advanced_pattern_encoding_terminal"' "Advanced Pattern Encoding Terminal scene contains the terminal part"
    Assert-FileMatches $terminalStructure 'state:\s*"ae2:energy_cell\{' "Advanced Pattern Encoding Terminal scene shows a powered network connection"

    $storageBusStructure = Join-Path $blockRoot "package_storage_bus.snbt"
    Assert-FileMatches $storageBusStructure 'id:\s*"appliedpackaging:package_storage_bus"' "Package Storage Bus scene contains the bus part"
    Assert-FileMatches $storageBusStructure 'state:\s*"minecraft:chest\{' "Package Storage Bus scene shows an adjacent inventory"

    $unpackingBusStructure = Join-Path $blockRoot "package_unpacking_bus.snbt"
    Assert-FileMatches $unpackingBusStructure 'id:\s*"appliedpackaging:package_unpacking_bus"' "Package Unpacking Bus scene contains the bus part"
    Assert-FileMatches $unpackingBusStructure 'state:\s*"minecraft:chest\{' "Package Unpacking Bus scene shows an adjacent inventory"

    $routingStructure = Join-Path $assemblyRoot "package_routing.snbt"
    Assert-FileMatches $routingStructure 'id:\s*"appliedpackaging:package_storage_bus"' "Multi-color routing scene contains a Package Storage Bus"
    Assert-FileMatches $routingStructure 'id:\s*"appliedpackaging:package_unpacking_bus"' "Multi-color routing scene contains a Package Unpacking Bus"
    Assert-FileMatches $routingStructure 'state:\s*"ae2:drive\{' "Multi-color routing scene contains fallback ME storage"
    Assert-FileMatches $routingStructure 'colors:\s*\[I;\s*15,' "Multi-color routing scene contains the red filter"
    Assert-FileMatches $routingStructure 'colors:\s*\[I;\s*12,' "Multi-color routing scene contains the blue filter"

    $sequenceStructure = Join-Path $assemblyRoot "sequence_line.snbt"
    Assert-FileMatches $sequenceStructure 'id:\s*"appliedpackaging:package_unpacking_bus"' "Ordered-input scene starts with a Package Unpacking Bus"
    Assert-FileMatchCount $sequenceStructure 'state:\s*"ae2:pattern_provider\{' 0 "Package Unpacking Bus Sequence Buffer scene does not contain a Pattern Provider"
    Assert-FileMatchCount $sequenceStructure 'ae2:energy_cell' 0 "Package Unpacking Bus Sequence Buffer scene contains no Energy Cell"
    Assert-FileMatches $sequenceStructure 'id:\s*"ae2:fluix_smart_cable"' "Package Unpacking Bus Sequence Buffer scene connects its input to the ME network"
    Assert-FileMatchCount $sequenceStructure 'state:\s*"appliedpackaging:sequence_buffer\{[^"\r\n]*state:endpoint' 1 "Ordered-input scene contains one Sequence Buffer endpoint"
    Assert-FileMatchCount $sequenceStructure 'state:\s*"appliedpackaging:sequence_buffer\{[^"\r\n]*state:member_directed' 3 "Ordered-input scene contains three directed Sequence Buffer members"
    Assert-FileMatchCount $sequenceStructure 'state:\s*"minecraft:chest\{' 3 "Ordered-input scene contains three target inventories"
    Assert-FileMatches $sequenceStructure 'auto_output:\s*1b' "Ordered-input endpoint enables automatic output"
    Assert-FileMatches $sequenceStructure 'pattern_mode:\s*1b' "Ordered-input endpoint enables pattern mode"
    Assert-FileMatches $sequenceStructure 'synchronized_output:\s*1b' "Ordered-input endpoint enables synchronized output"

    $sequenceProviderStructure = Join-Path $assemblyRoot "sequence_line_pattern_provider.snbt"
    Assert-FileMatchCount $sequenceProviderStructure 'state:\s*"ae2:pattern_provider\{push_direction:east\}"' 1 "Pattern Provider Sequence Buffer scene uses one directional Pattern Provider"
    Assert-FileMatchCount $sequenceProviderStructure 'id:\s*"appliedpackaging:package_unpacking_bus"' 0 "Pattern Provider Sequence Buffer scene does not contain a Package Unpacking Bus"
    Assert-FileMatchCount $sequenceProviderStructure 'ae2:energy_cell' 0 "Pattern Provider Sequence Buffer scene contains no Energy Cell"
    Assert-FileMatches $sequenceProviderStructure 'id:\s*"ae2:fluix_smart_cable"' "Pattern Provider Sequence Buffer scene connects its input to the ME network"
    Assert-FileMatchCount $sequenceProviderStructure 'state:\s*"appliedpackaging:sequence_buffer\{[^"\r\n]*state:endpoint' 1 "Pattern Provider scene contains one Sequence Buffer endpoint"
    Assert-FileMatchCount $sequenceProviderStructure 'state:\s*"appliedpackaging:sequence_buffer\{[^"\r\n]*state:member_directed' 3 "Pattern Provider scene contains three directed Sequence Buffer members"
    Assert-FileMatchCount $sequenceProviderStructure 'state:\s*"minecraft:chest\{' 3 "Pattern Provider scene contains three target inventories"
    Assert-FileMatches $sequenceProviderStructure 'auto_output:\s*1b' "Pattern Provider endpoint enables automatic output"
    Assert-FileMatches $sequenceProviderStructure 'pattern_mode:\s*1b' "Pattern Provider endpoint enables pattern mode"
    Assert-FileMatches $sequenceProviderStructure 'synchronized_output:\s*1b' "Pattern Provider endpoint enables synchronized output"

    $furnaceArrayStructure = Join-Path $assemblyRoot "sequence_furnace_array.snbt"
    Assert-FileMatchCount $furnaceArrayStructure 'state:\s*"ae2:pattern_provider\{push_direction:north\}"' 1 "Parallel furnace scene uses one directional Pattern Provider"
    Assert-FileMatchCount $furnaceArrayStructure 'id:\s*"ae2:processing_pattern"' 1 "Parallel furnace Pattern Provider contains one processing pattern"
    Assert-FileMatches $furnaceArrayStructure '\{"#":\s*1L,\s*"#c":\s*"ae2:i",\s*id:\s*"minecraft:coal"\}' "Parallel furnace pattern input 1 is one Coal"
    Assert-FileMatches $furnaceArrayStructure '\{"#":\s*8L,\s*"#c":\s*"ae2:i",\s*id:\s*"minecraft:raw_iron"\}' "Parallel furnace pattern input 2 is eight Raw Iron"
    Assert-FileMatches $furnaceArrayStructure 'out:\s*\[\{"#":\s*8L,\s*"#c":\s*"ae2:i",\s*id:\s*"minecraft:iron_ingot"\}\]' "Parallel furnace pattern output is eight Iron Ingots"
    Assert-FileMatchCount $furnaceArrayStructure 'state:\s*"appliedpackaging:package_assembler\{' 1 "Parallel furnace scene contains one Package Assembler"
    Assert-FileMatches $furnaceArrayStructure 'output_mode:\s*"me_network"' "Parallel furnace Package Assembler outputs packages to the processing subnet"
    Assert-FileMatchCount $furnaceArrayStructure 'id:\s*"appliedpackaging:package_unpacking_bus"' 4 "Parallel furnace scene contains four Package Unpacking Buses"
    $furnacePriorities = @(
        @{ X = 2; Priority = 4 },
        @{ X = 3; Priority = 3 },
        @{ X = 4; Priority = 2 },
        @{ X = 5; Priority = 1 }
    )
    foreach ($branch in $furnacePriorities) {
        Assert-FileMatches $furnaceArrayStructure "pos:\s*\[$($branch.X),\s*1,\s*0\][^\r\n]*id:\s*`"appliedpackaging:package_unpacking_bus`"[^\r\n]*priority:\s*$($branch.Priority)" "Parallel furnace branch at x=$($branch.X) uses unpacking priority $($branch.Priority)"
    }
    Assert-FileMatchCount $furnaceArrayStructure 'state:\s*"appliedpackaging:sequence_buffer\{[^"\r\n]*state:endpoint' 4 "Parallel furnace scene contains four Sequence Buffer endpoints"
    Assert-FileMatchCount $furnaceArrayStructure 'state:\s*"appliedpackaging:sequence_buffer\{[^"\r\n]*state:member_directed' 8 "Parallel furnace scene contains two directed members per branch"
    Assert-FileMatchCount $furnaceArrayStructure 'state:\s*"appliedpackaging:sequence_buffer\{[^"\r\n]*state:member_directed,tail:false' 4 "Parallel furnace scene contains four fuel members"
    Assert-FileMatchCount $furnaceArrayStructure 'state:\s*"appliedpackaging:sequence_buffer\{[^"\r\n]*state:member_directed,tail:true' 4 "Parallel furnace scene contains four Raw Iron members"
    Assert-FileMatchCount $furnaceArrayStructure 'state:\s*"minecraft:hopper\{' 4 "Parallel furnace scene contains four fuel hoppers"
    Assert-FileMatchCount $furnaceArrayStructure 'state:\s*"minecraft:furnace\{' 4 "Parallel furnace scene contains four furnaces"
    Assert-FileMatchCount $furnaceArrayStructure 'id:\s*"ae2:import_bus"' 4 "Parallel furnace return subnet contains four Import Buses"
    Assert-FileMatchCount $furnaceArrayStructure 'id:\s*"ae2:storage_bus"' 1 "Parallel furnace return subnet uses one ordinary Storage Bus against the Pattern Provider"
    Assert-FileMatchCount $furnaceArrayStructure 'id:\s*"appliedpackaging:package_storage_bus"' 0 "Parallel furnace return subnet does not substitute a Package Storage Bus"
    Assert-FileMatchCount $furnaceArrayStructure 'id:\s*"ae2:quartz_fiber"' 2 "Parallel furnace subnets use two Quartz Fiber power bridges"
    Assert-FileMatches $furnaceArrayStructure 'id:\s*"ae2:white_smart_cable"' "Parallel furnace processing subnet uses white Smart Cable"
    Assert-FileMatches $furnaceArrayStructure 'id:\s*"ae2:orange_smart_cable"' "Parallel furnace return subnet uses orange Smart Cable"
    Assert-FileMatches $furnaceArrayStructure 'id:\s*"ae2:pink_smart_cable"' "Parallel furnace Pattern Provider remains on the pink main network"
    Assert-FileMatchCount $furnaceArrayStructure 'ae2:energy_cell' 0 "Parallel furnace scene contains no Energy Cell"

    $mechanicalCraftingStructure = Join-Path $assemblyRoot "sequence_mechanical_crafting_5x5.snbt"
    Assert-FileMatches $mechanicalCraftingStructure 'size:\s*\[8,\s*6,\s*6\]' "Mechanical crafting scene has room for the 5x5 target wall and its network edge"
    Assert-FileMatchCount $mechanicalCraftingStructure 'state:\s*"ae2:pattern_provider\{push_direction:north\}"' 1 "Mechanical crafting scene uses one directional Pattern Provider"
    Assert-FileMatchCount $mechanicalCraftingStructure 'id:\s*"appliedpackaging:advanced_processing_pattern"' 1 "Mechanical crafting Pattern Provider contains one advanced pattern"
    Assert-FileMatches $mechanicalCraftingStructure '"appliedpackaging\.advanced_processing_pattern":\s*\{version:\s*2' "Mechanical crafting pattern uses current sparse-column metadata"
    Assert-FileMatchCount $mechanicalCraftingStructure '\{index:\s*[0-4],\s*color:\s*"' 5 "Mechanical crafting advanced pattern contains five ordered columns"
    Assert-FileMatchCount $mechanicalCraftingStructure '(?<!allowed_)inputs:\s*\[' 5 "Mechanical crafting advanced pattern stores five per-column layouts"
    Assert-FileMatches $mechanicalCraftingStructure '(?<!allowed_)inputs:\s*\[\{\},' "Mechanical crafting advanced pattern preserves empty positions"
    foreach ($color in @("fluix", "white", "orange", "magenta", "light_blue")) {
        Assert-FileMatches $mechanicalCraftingStructure "color:\s*`"$color`"" "Mechanical crafting advanced pattern contains the $color package column"
    }
    Assert-FileMatchCount $mechanicalCraftingStructure 'state:\s*"appliedpackaging:package_assembler\{' 1 "Mechanical crafting scene contains one Package Assembler"
    Assert-FileMatchCount $mechanicalCraftingStructure 'blocking_mode:\s*1b' 1 "Mechanical crafting Package Assembler enables blocking mode"
    Assert-FileMatches $mechanicalCraftingStructure 'output_mode:\s*"me_network"' "Mechanical crafting Package Assembler outputs into the processing subnet"
    Assert-FileMatchCount $mechanicalCraftingStructure 'id:\s*"appliedpackaging:package_unpacking_bus"' 5 "Mechanical crafting scene contains five Package Unpacking Buses"
    Assert-FileMatchCount $mechanicalCraftingStructure 'colorEnabled:\s*\[B;\s*1b,' 5 "Mechanical crafting unpacking buses each enable one color filter"
    for ($ordinal = 0; $ordinal -lt 5; $ordinal++) {
        Assert-FileMatches $mechanicalCraftingStructure "colors:\s*\[I;\s*$ordinal," "Mechanical crafting scene routes package color ordinal $ordinal"
    }
    Assert-FileMatchCount $mechanicalCraftingStructure 'state:\s*"appliedpackaging:sequence_buffer\{[^"\r\n]*state:endpoint' 5 "Mechanical crafting scene contains five Sequence Buffer endpoints"
    Assert-FileMatchCount $mechanicalCraftingStructure 'state:\s*"appliedpackaging:sequence_buffer\{[^"\r\n]*state:member_directed' 25 "Mechanical crafting scene contains five members per buffer row"
    Assert-FileMatchCount $mechanicalCraftingStructure 'state:\s*"appliedpackaging:sequence_buffer\{[^"\r\n]*state:member_directed,tail:true' 5 "Mechanical crafting scene terminates each of its five buffer rows"
    Assert-FileMatchCount $mechanicalCraftingStructure 'auto_output:\s*1b' 5 "Mechanical crafting endpoints enable automatic output"
    Assert-FileMatchCount $mechanicalCraftingStructure 'pattern_mode:\s*1b' 5 "Mechanical crafting endpoints enable pattern mode"
    Assert-FileMatchCount $mechanicalCraftingStructure 'synchronized_output:\s*1b' 5 "Mechanical crafting endpoints enable synchronized output"
    Assert-FileMatchCount $mechanicalCraftingStructure 'state:\s*"minecraft:(?:trapped_)?chest\{' 25 "Mechanical crafting scene contains a 5x5 chest stand-in wall"
    Assert-FileMatchCount $mechanicalCraftingStructure 'pos:\s*\[[3-7],\s*[1-5],\s*3\][^\r\n]*state:\s*"minecraft:(?:trapped_)?chest\{facing:south' 25 "Mechanical crafting scene places all 25 chest stand-ins in front of the buffer members"
    Assert-FileMatchCount $mechanicalCraftingStructure 'id:\s*"ae2:fluix_smart_dense_cable"' 0 "Mechanical crafting 6-channel processing subnet does not require Smart Dense Cable"
    Assert-FileMatchCount $mechanicalCraftingStructure 'id:\s*"ae2:white_smart_cable"[^\r\n]*id:\s*"appliedpackaging:package_unpacking_bus"' 5 "Mechanical crafting scene mounts each unpacking bus on regular Smart Cable"
    Assert-FileMatches $mechanicalCraftingStructure 'id:\s*"ae2:pink_smart_cable"' "Mechanical crafting Pattern Provider remains on the pink main network"
    Assert-FileMatchCount $mechanicalCraftingStructure 'id:\s*"ae2:quartz_fiber"' 1 "Mechanical crafting processing subnet uses one Quartz Fiber power bridge"
    Assert-FileMatchCount $mechanicalCraftingStructure 'ae2:energy_cell' 0 "Mechanical crafting scene contains no Energy Cell"

    $packagerRoutingStructure = Join-Path $assemblyRoot "packager_color_routing.snbt"
    Assert-FileMatches $packagerRoutingStructure 'size:\s*\[13,\s*1,\s*7\]' "Packager routing scene fits four color destinations and the routing-network input"
    Assert-FileMatchCount $packagerRoutingStructure 'state:\s*"appliedpackaging:me_packager\{' 4 "Packager routing scene contains four ME Packagers"
    Assert-FileMatchCount $packagerRoutingStructure 'id:\s*"appliedpackaging:package_storage_bus"' 4 "Packager routing scene contains four Package Storage Buses"
    Assert-FileMatchCount $packagerRoutingStructure 'colorEnabled:\s*\[B;\s*1b,' 4 "Packager routing storage buses each enable one color filter"
    Assert-FileMatchCount $packagerRoutingStructure 'filter_application_mode:\s*"BOTH"' 4 "Packager routing ME Packagers apply their configured color to packing and unpacking"
    Assert-FileMatchCount $packagerRoutingStructure 'redstone_mode:\s*"ALWAYS"' 4 "Packager routing ME Packagers remain active without a redstone signal"
    $routingPairs = @(
        @{ Name = "red"; Ordinal = 15; PackagerPos = '\[2,\s*0,\s*1\]'; BusPos = '\[3,\s*0,\s*1\]' },
        @{ Name = "blue"; Ordinal = 12; PackagerPos = '\[2,\s*0,\s*5\]'; BusPos = '\[3,\s*0,\s*5\]' },
        @{ Name = "green"; Ordinal = 14; PackagerPos = '\[10,\s*0,\s*1\]'; BusPos = '\[9,\s*0,\s*1\]' },
        @{ Name = "yellow"; Ordinal = 5; PackagerPos = '\[10,\s*0,\s*5\]'; BusPos = '\[9,\s*0,\s*5\]' }
    )
    foreach ($pair in $routingPairs) {
        $colorTitle = (Get-Culture).TextInfo.ToTitleCase($pair.Name)
        Assert-FileMatches $packagerRoutingStructure "pos:\s*$($pair.BusPos)[^\r\n]*colors:\s*\[I;\s*$($pair.Ordinal),[^\r\n]*id:\s*`"appliedpackaging:package_storage_bus`"" "$colorTitle routing branch stores only $($pair.Name) packages"
        Assert-FileMatches $packagerRoutingStructure "pos:\s*$($pair.PackagerPos)[^\r\n]*selected_color:\s*`"$($pair.Name)`"" "$colorTitle routing branch keeps the bus and ME Packager color aligned"
    }
    Assert-FileMatchCount $packagerRoutingStructure 'id:\s*"ae2:orange_smart_cable"[^\r\n]*id:\s*"appliedpackaging:package_storage_bus"' 4 "Packager routing buses belong to the orange routing network"
    Assert-FileMatchCount $packagerRoutingStructure 'id:\s*"ae2:pink_smart_cable"[^\r\n]*id:\s*"appliedpackaging:package_storage_bus"' 0 "Packager routing destination networks do not contain the routing buses"
    Assert-FileMatches $packagerRoutingStructure 'pos:\s*\[6,\s*0,\s*0\][^\r\n]*id:\s*"ae2:orange_smart_cable"' "Packager routing network leaves one input stub for the package source"
    Assert-FileMatchCount $packagerRoutingStructure 'ae2:energy_cell' 0 "Packager routing scene leaves power and inventory connections to the external networks"

    $multiStepStructure = Join-Path $assemblyRoot "advanced_multistep_crafting.snbt"
    Assert-FileMatches $multiStepStructure 'size:\s*\[9,\s*1,\s*6\]' "Multi-step crafting scene fits the source, routing subnet, and four step inputs"
    Assert-FileMatchCount $multiStepStructure 'state:\s*"ae2:pattern_provider\{push_direction:north\}"' 1 "Multi-step crafting scene uses one directional Pattern Provider"
    Assert-FileMatchCount $multiStepStructure 'id:\s*"appliedpackaging:advanced_processing_pattern"' 1 "Multi-step Pattern Provider contains one advanced pattern"
    Assert-FileMatches $multiStepStructure '"appliedpackaging\.advanced_processing_pattern":\s*\{version:\s*2' "Multi-step advanced pattern uses current sparse-column metadata"
    Assert-FileMatchCount $multiStepStructure '\{index:\s*[0-3],\s*color:\s*"' 4 "Multi-step advanced pattern contains four ordered columns"
    $multiStepRoutes = @(
        @{ Name = "red"; Ordinal = 15; Index = 0; BusPos = '\[3,\s*0,\s*2\]' },
        @{ Name = "blue"; Ordinal = 12; Index = 1; BusPos = '\[4,\s*0,\s*2\]' },
        @{ Name = "green"; Ordinal = 14; Index = 2; BusPos = '\[6,\s*0,\s*2\]' },
        @{ Name = "yellow"; Ordinal = 5; Index = 3; BusPos = '\[7,\s*0,\s*2\]' }
    )
    foreach ($route in $multiStepRoutes) {
        $colorTitle = (Get-Culture).TextInfo.ToTitleCase($route.Name)
        Assert-FileMatches $multiStepStructure "\{index:\s*$($route.Index),\s*color:\s*`"$($route.Name)`"" "$colorTitle multi-step pattern column keeps its configured package color"
        Assert-FileMatches $multiStepStructure "pos:\s*$($route.BusPos)[^\r\n]*colors:\s*\[I;\s*$($route.Ordinal),[^\r\n]*id:\s*`"appliedpackaging:package_storage_bus`"" "$colorTitle multi-step storage bus matches its pattern column"
    }
    Assert-FileMatchCount $multiStepStructure 'state:\s*"appliedpackaging:package_assembler\{' 1 "Multi-step crafting scene contains one Package Assembler"
    Assert-FileMatches $multiStepStructure 'output_mode:\s*"me_network"' "Multi-step Package Assembler outputs into the routing subnet"
    Assert-FileMatchCount $multiStepStructure 'id:\s*"appliedpackaging:package_storage_bus"' 4 "Multi-step crafting scene contains four Package Storage Buses"
    Assert-FileMatchCount $multiStepStructure 'colorEnabled:\s*\[B;\s*1b,' 4 "Multi-step storage buses each enable one color filter"
    Assert-FileMatchCount $multiStepStructure 'state:\s*"minecraft:chest\{' 4 "Multi-step crafting scene contains four step-input chests"
    Assert-FileMatchCount $multiStepStructure 'id:\s*"ae2:white_smart_cable"[^\r\n]*id:\s*"appliedpackaging:package_storage_bus"' 4 "Multi-step storage buses belong to the white processing subnet"
    Assert-FileMatches $multiStepStructure 'id:\s*"ae2:pink_smart_cable"' "Multi-step Pattern Provider remains on the pink main network"
    Assert-FileMatchCount $multiStepStructure 'id:\s*"ae2:quartz_fiber"' 1 "Multi-step processing subnet uses one Quartz Fiber power bridge"
    Assert-FileMatchCount $multiStepStructure 'ae2:energy_cell' 0 "Multi-step crafting scene contains no Energy Cell"
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
    "src/main/resources/assets/appliedpackaging/ae2guide/devices/advanced-pattern-terminal.md",
    "src/main/resources/assets/appliedpackaging/ae2guide/devices/index.md",
    "src/main/resources/assets/appliedpackaging/ae2guide/devices/package-storage-bus.md",
    "src/main/resources/assets/appliedpackaging/ae2guide/devices/package-unpacking-bus.md",
    "src/main/resources/assets/appliedpackaging/ae2guide/example-setups/basic-packaging-line.md",
    "src/main/resources/assets/appliedpackaging/ae2guide/example-setups/index.md",
    "src/main/resources/assets/appliedpackaging/ae2guide/example-setups/multi-color-routing.md",
    "src/main/resources/assets/appliedpackaging/ae2guide/example-setups/ordered-machine-inputs.md",
    "src/main/resources/assets/appliedpackaging/ae2guide/example-setups/ordered-machine-inputs/mechanical-crafting.md",
    "src/main/resources/assets/appliedpackaging/ae2guide/example-setups/ordered-machine-inputs/multi-step-crafting.md",
    "src/main/resources/assets/appliedpackaging/ae2guide/example-setups/ordered-machine-inputs/package-routing.md",
    "src/main/resources/assets/appliedpackaging/ae2guide/example-setups/ordered-machine-inputs/parallel-furnaces.md",
    "src/main/resources/assets/appliedpackaging/ae2guide/machines/index.md",
    "src/main/resources/assets/appliedpackaging/ae2guide/machines/me-packager.md",
    "src/main/resources/assets/appliedpackaging/ae2guide/machines/package-assembler.md",
    "src/main/resources/assets/appliedpackaging/ae2guide/machines/sequence-buffer.md",
    "src/main/resources/assets/appliedpackaging/ae2guide/packaging-concepts/getting-started.md",
    "src/main/resources/assets/appliedpackaging/ae2guide/packaging-concepts/index.md",
    "src/main/resources/assets/appliedpackaging/ae2guide/packaging-concepts/packages.md",
    "src/main/resources/assets/appliedpackaging/ae2guide/troubleshooting.md",
    "src/main/resources/assets/appliedpackaging/ae2guide/_zh_cn/index.md",
    "src/main/resources/assets/appliedpackaging/ae2guide/_zh_cn/devices/advanced-pattern-terminal.md",
    "src/main/resources/assets/appliedpackaging/ae2guide/_zh_cn/devices/index.md",
    "src/main/resources/assets/appliedpackaging/ae2guide/_zh_cn/devices/package-storage-bus.md",
    "src/main/resources/assets/appliedpackaging/ae2guide/_zh_cn/devices/package-unpacking-bus.md",
    "src/main/resources/assets/appliedpackaging/ae2guide/_zh_cn/example-setups/basic-packaging-line.md",
    "src/main/resources/assets/appliedpackaging/ae2guide/_zh_cn/example-setups/index.md",
    "src/main/resources/assets/appliedpackaging/ae2guide/_zh_cn/example-setups/multi-color-routing.md",
    "src/main/resources/assets/appliedpackaging/ae2guide/_zh_cn/example-setups/ordered-machine-inputs.md",
    "src/main/resources/assets/appliedpackaging/ae2guide/_zh_cn/example-setups/ordered-machine-inputs/mechanical-crafting.md",
    "src/main/resources/assets/appliedpackaging/ae2guide/_zh_cn/example-setups/ordered-machine-inputs/multi-step-crafting.md",
    "src/main/resources/assets/appliedpackaging/ae2guide/_zh_cn/example-setups/ordered-machine-inputs/package-routing.md",
    "src/main/resources/assets/appliedpackaging/ae2guide/_zh_cn/example-setups/ordered-machine-inputs/parallel-furnaces.md",
    "src/main/resources/assets/appliedpackaging/ae2guide/_zh_cn/machines/index.md",
    "src/main/resources/assets/appliedpackaging/ae2guide/_zh_cn/machines/me-packager.md",
    "src/main/resources/assets/appliedpackaging/ae2guide/_zh_cn/machines/package-assembler.md",
    "src/main/resources/assets/appliedpackaging/ae2guide/_zh_cn/machines/sequence-buffer.md",
    "src/main/resources/assets/appliedpackaging/ae2guide/_zh_cn/packaging-concepts/getting-started.md",
    "src/main/resources/assets/appliedpackaging/ae2guide/_zh_cn/packaging-concepts/index.md",
    "src/main/resources/assets/appliedpackaging/ae2guide/_zh_cn/packaging-concepts/packages.md",
    "src/main/resources/assets/appliedpackaging/ae2guide/_zh_cn/troubleshooting.md",
    "src/main/resources/assets/appliedpackaging/ae2guide/assets/blocks/advanced_pattern_encoding_terminal.snbt",
    "src/main/resources/assets/appliedpackaging/ae2guide/assets/blocks/package_storage_bus.snbt",
    "src/main/resources/assets/appliedpackaging/ae2guide/assets/blocks/package_unpacking_bus.snbt",
    "src/main/resources/assets/appliedpackaging/ae2guide/assets/assemblies/package_assembly_line.snbt",
    "src/main/resources/assets/appliedpackaging/ae2guide/assets/assemblies/package_assembler_hopper.snbt",
    "src/main/resources/assets/appliedpackaging/ae2guide/assets/assemblies/package_assembler_subnetwork.snbt",
    "src/main/resources/assets/appliedpackaging/ae2guide/assets/assemblies/me_packager_network.snbt",
    "src/main/resources/assets/appliedpackaging/ae2guide/assets/assemblies/advanced_multistep_crafting.snbt",
    "src/main/resources/assets/appliedpackaging/ae2guide/assets/assemblies/packager_color_routing.snbt",
    "src/main/resources/assets/appliedpackaging/ae2guide/assets/assemblies/package_routing.snbt",
    "src/main/resources/assets/appliedpackaging/ae2guide/assets/assemblies/sequence_line.snbt",
    "src/main/resources/assets/appliedpackaging/ae2guide/assets/assemblies/sequence_line_pattern_provider.snbt",
    "src/main/resources/assets/appliedpackaging/ae2guide/assets/assemblies/sequence_furnace_array.snbt",
    "src/main/resources/assets/appliedpackaging/ae2guide/assets/assemblies/sequence_mechanical_crafting_5x5.snbt",
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
