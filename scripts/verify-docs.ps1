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
        "example-setups/ordered-machine-inputs.md|GameScene" = 1
        "machines/index.md|SubPages" = 1
        "machines/me-packager.md|BlockImage" = 1
        "machines/me-packager.md|RecipeFor" = 1
        "machines/package-assembler.md|BlockImage" = 1
        "machines/package-assembler.md|GameScene" = 3
        "machines/package-assembler.md|RecipeFor" = 1
        "machines/sequence-buffer.md|BlockImage" = 1
        "machines/sequence-buffer.md|GameScene" = 1
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

    $assemblyRoot = Join-Path $guideRoot "assets/assemblies"
    $blockRoot = Join-Path $guideRoot "assets/blocks"
    $packageGrid = Join-Path $assemblyRoot "package_assembly_line.snbt"
    Assert-FileMatches $packageGrid '(?m)^\s*size:\s*\[4,\s*4,\s*4\],?\s*$' "Package Assembler grid is exactly 4x4x4"
    Assert-FileMatchCount $packageGrid 'state:\s*"ae2:pattern_provider\{' 32 "Package Assembler grid contains 32 Pattern Providers"
    Assert-FileMatchCount $packageGrid 'state:\s*"appliedpackaging:package_assembler\{' 32 "Package Assembler grid contains 32 Package Assemblers"
    Assert-FileMatchCount $packageGrid 'output_mode:\s*"me_network"' 32 "Package Assembler grid returns packages to ME storage"

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
    Assert-FileMatchCount $sequenceStructure 'state:\s*"appliedpackaging:sequence_buffer\{[^"\r\n]*state:endpoint' 1 "Ordered-input scene contains one Sequence Buffer endpoint"
    Assert-FileMatchCount $sequenceStructure 'state:\s*"appliedpackaging:sequence_buffer\{[^"\r\n]*state:member_directed' 3 "Ordered-input scene contains three directed Sequence Buffer members"
    Assert-FileMatchCount $sequenceStructure 'state:\s*"minecraft:chest\{' 3 "Ordered-input scene contains three target inventories"
    Assert-FileMatches $sequenceStructure 'auto_output:\s*1b' "Ordered-input endpoint enables automatic output"
    Assert-FileMatches $sequenceStructure 'pattern_mode:\s*1b' "Ordered-input endpoint enables pattern mode"
    Assert-FileMatches $sequenceStructure 'synchronized_output:\s*1b' "Ordered-input endpoint enables synchronized output"
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
