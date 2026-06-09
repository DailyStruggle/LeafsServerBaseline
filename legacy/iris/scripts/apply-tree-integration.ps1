# apply-tree-integration.ps1
# Applies tree rate trims and adds custom tree objects to Iris overworld biome JSONs.
# Reads from live pack, modifies in place using raw string operations only -
# never round-trips through ConvertTo-Json to avoid corrupting stock structure.

param(
    [string]$PackRoot = "C:\GameServers\Minecraft\testServer\RTP-Folia\26.1\plugins\Iris\packs\overworld\biomes",
    # Compiled .iob source objects, organised under <species>\custom-<species>-<tier>-<n>.iob
    [string]$SourceObjects = "C:\Users\lxgol\IdeaProjects\LeafsServerBaseline\scripts\output\renamed",
    # Destination inside the Iris pack that the "trees/custom/<name>" placements resolve to
    [string]$ObjectsRoot = "C:\GameServers\Minecraft\testServer\RTP-Folia\26.1\plugins\Iris\packs\overworld\objects\trees\custom",
    # A/B isolation toggle for telling new vs. old trees apart:
    #   both         - stock + custom trees (normal production state)
    #   new-only     - keep ONLY custom trees in touched biomes (stock tree chances zeroed)
    #   old-only     - keep ONLY stock trees in touched biomes (custom entries removed)
    #   debug-single - place ONE object ($DebugObject) at high chance as the only tree in
    #                  every touched biome (stock zeroed) - a sanity check that a NEW tree loads
    [ValidateSet('both','new-only','old-only','debug-single')]
    [string]$Mode = 'both',
    # debug-single target: base name of a custom object (resolves to trees/custom/<name>).
    # Default is a giant spiral dark oak (~71+ blocks, source 'spiral_dark_oak_...').
    [string]$DebugObject = 'custom-darkoak-giant-1',
    [double]$DebugChance = 0.5,
    # Vertical offset applied to EVERY custom placement so the trunk base sits in the
    # ground instead of floating. Stock Iris tree entries use y:-3 (roots buried); tune
    # this if custom trees float (raise toward 0 / positive) or sink (more negative).
    [double]$CustomYOffset = -3.0,
    # Pristine stock baseline. Every Apply regenerates the live file FROM this snapshot,
    # so switching Mode is fully reversible and never compounds edits. Created on first run.
    [string]$SnapshotRoot = "C:\GameServers\Minecraft\testServer\RTP-Folia\26.1\plugins\Iris\packs\overworld\biomes-stock-snapshot"
)

# Snapshot the current (clean) biomes tree once, so it can serve as the immutable
# source for all Mode regenerations. Delete this folder (while the pack is clean)
# to re-baseline.
if (-not (Test-Path $SnapshotRoot)) {
    Write-Output "Creating stock snapshot: $SnapshotRoot"
    Copy-Item -Recurse -Force $PackRoot $SnapshotRoot
}
Write-Output "Mode: $Mode"

# Placement suffix mirroring stock Iris tree entries: random Y rotation plus a
# vertical translate so the object is grounded (not floating). $CustomYOffset tunes
# the burial depth.
function Placement-Suffix() {
    return ", `"rotation`": { `"enabled`": true, `"yAxis`": { `"enabled`": true, `"interval`": 90, `"min`": 0, `"max`": 360 } }, `"translate`": { `"x`": 0, `"y`": $CustomYOffset, `"z`": 0 }"
}

# Emits a SINGLE merged entry whose "place" array pools all variant paths.
# Iris picks one variant at random per successful placement (per-tier grouping).
function Make-Entry([string[]]$paths, [double]$chance) {
    $arr = ($paths | ForEach-Object { "`"$_`"" }) -join ", "
    $script:pendingEntries += "    { `"chance`": $chance, `"place`": [$arr]$(Placement-Suffix) }"
}

# Insert entry strings before the closing ] of the top-level "objects" array.
# String-literal aware bracket counting so [ ] inside "place" values are ignored;
# handles an empty objects array (no spurious leading comma).
function Insert-Entries([string]$raw, [string[]]$entries) {
    if (-not $entries -or $entries.Count -eq 0) { return $raw }
    $insert = $entries -join ",`n"
    $objKeyIdx = $raw.IndexOf('"objects"')
    if ($objKeyIdx -lt 0) { return $raw }
    $openIdx = $raw.IndexOf('[', $objKeyIdx)
    if ($openIdx -lt 0) { return $raw }
    $i = $openIdx
    $depth = 0
    $insertAt = -1
    while ($i -lt $raw.Length) {
        $c = $raw[$i]
        if ($c -eq '"') {
            $i++
            while ($i -lt $raw.Length) {
                if ($raw[$i] -eq '\') { $i += 2; continue }
                if ($raw[$i] -eq '"') { break }
                $i++
            }
        } elseif ($c -eq '[') { $depth++ }
        elseif ($c -eq ']') { $depth--; if ($depth -eq 0) { $insertAt = $i; break } }
        $i++
    }
    if ($insertAt -lt 0) { return $raw }
    $inner = $raw.Substring($openIdx + 1, $insertAt - $openIdx - 1)
    if ($inner.Trim().Length -eq 0) {
        return $raw.Substring(0, $openIdx + 1) + "`n" + $insert + "`n" + $raw.Substring($insertAt)
    }
    return $raw.Substring(0, $insertAt).TrimEnd() + ",`n" + $insert + "`n" + $raw.Substring($insertAt)
}

# Build list of custom object paths: custom-<species>-<tier>-1 through -N
function Custom([string]$species, [string]$tier, [int]$n) {
    1..$n | ForEach-Object { "trees/custom/custom-$species-$tier-$_" }
}

# Trim: replace chance value for any object entry whose place string contains matchStr.
# Operates on $script:currentRaw so the stock structure is never corrupted by ConvertTo-Json.
function Trim-Objects($ignored, [string]$matchStr, [double]$newChance) {
    $pattern = '(\{[^{}]*"place"\s*:\s*"[^"]*' + [regex]::Escape($matchStr) + '[^"]*"[^{}]*\})'
    $script:currentRaw = [regex]::Replace($script:currentRaw, $pattern, {
        param($m)
        $m.Value -replace '"chance"\s*:\s*[\d.Ee+-]+', ('"chance": ' + $newChance)
    })
}

# Zero the "chance" of every STOCK tree placement in the objects array, leaving
# custom entries and non-tree decorations untouched. Walks the array brace-aware
# (string-literal safe) so nested "rotation" objects and "place" arrays don't
# corrupt the scan. Used only in new-only Mode.
function Suppress-StockTrees([string]$raw) {
    $objKeyIdx = $raw.IndexOf('"objects"')
    if ($objKeyIdx -lt 0) { return $raw }
    $i = $raw.IndexOf('[', $objKeyIdx)
    if ($i -lt 0) { return $raw }
    $sb = New-Object System.Text.StringBuilder
    [void]$sb.Append($raw.Substring(0, $i + 1))
    $i++
    $arrDepth = 1
    while ($i -lt $raw.Length -and $arrDepth -ge 1) {
        $c = $raw[$i]
        if ($c -eq '"') {
            $start = $i; $i++
            while ($i -lt $raw.Length) {
                if ($raw[$i] -eq '\') { $i += 2; continue }
                if ($raw[$i] -eq '"') { $i++; break }
                $i++
            }
            [void]$sb.Append($raw.Substring($start, $i - $start))
            continue
        }
        elseif ($c -eq '{' -and $arrDepth -eq 1) {
            $start = $i
            $d = 0
            while ($i -lt $raw.Length) {
                $ch = $raw[$i]
                if ($ch -eq '"') {
                    $i++
                    while ($i -lt $raw.Length) {
                        if ($raw[$i] -eq '\') { $i += 2; continue }
                        if ($raw[$i] -eq '"') { break }
                        $i++
                    }
                } elseif ($ch -eq '{') { $d++ }
                elseif ($ch -eq '}') { $d--; if ($d -eq 0) { $i++; break } }
                $i++
            }
            $entry = $raw.Substring($start, $i - $start)
            if ($entry -match 'trees/' -and $entry -notmatch 'trees/custom') {
                $entry = $entry -replace '"chance"\s*:\s*[\d.Ee+-]+', '"chance": 0.0'
            }
            [void]$sb.Append($entry)
            continue
        }
        elseif ($c -eq '[') { $arrDepth++ }
        elseif ($c -eq ']') {
            $arrDepth--
            if ($arrDepth -eq 0) { [void]$sb.Append($raw.Substring($i)); return $sb.ToString() }
        }
        [void]$sb.Append($c)
        $i++
    }
    return $sb.ToString()
}

function Apply([string]$rel, [scriptblock]$block) {
    $relWin = $rel -replace '/','\'
    $srcPath = "$SnapshotRoot\$relWin"
    $dstPath = "$PackRoot\$relWin"
    # Always regenerate the live file from the pristine snapshot so Mode switches
    # are reversible and never compound (suppression/trims won't accumulate).
    if (-not (Test-Path $srcPath)) { Write-Warning "MISSING: $srcPath"; return }
    $script:currentRaw = Get-Content $srcPath -Raw
    # Strip any custom tree entries carried in the snapshot so we start from stock
    # (each custom entry is a flat object with no nested braces, always comma-prefixed).
    $script:currentRaw = [regex]::Replace($script:currentRaw, ',\s*\{[^{}]*trees/custom[^{}]*\}', '')

    # $j is a dummy object passed to block only so param($j) call sites work unchanged
    $j = [PSCustomObject]@{ objects = @() }
    $script:pendingEntries = @()
    & $block $j

    # old-only: discard the custom entries the block queued (stock trees stay).
    if ($Mode -eq 'old-only') { $script:pendingEntries = @() }
    # debug-single: replace queued entries with one high-chance entry for $DebugObject.
    elseif ($Mode -eq 'debug-single') {
        $script:pendingEntries = @("    { `"chance`": $DebugChance, `"place`": [`"trees/custom/$DebugObject`"]$(Placement-Suffix) }")
    }

    # Append new entries before the closing ] of the top-level "objects" array.
    if ($script:pendingEntries.Count -gt 0) {
        $script:currentRaw = Insert-Entries $script:currentRaw $script:pendingEntries
    }

    # new-only / debug-single: zero out stock tree chances so only custom trees generate.
    if ($Mode -eq 'new-only' -or $Mode -eq 'debug-single') { $script:currentRaw = Suppress-StockTrees $script:currentRaw }

    [System.IO.File]::WriteAllText($dstPath, $script:currentRaw, [System.Text.UTF8Encoding]::new($false))
    Write-Output "  saved: $(Split-Path $dstPath -Leaf)"
}

# Add-Entries: no-op wrapper - Make-Entry already wrote to $script:pendingEntries
function Add-Entries($ignored, [object[]]$also_ignored) { }

Write-Output "=== Applying tree integration to live pack ==="
Write-Output ""

# -----------------------------------------------------------------------
# temperate/forest - oak supplement
# -----------------------------------------------------------------------
Write-Output "--- temperate/forest ---"
foreach ($f in @("temperate/forest.json","temperate/forest-extended.json","temperate/forest-flat.json")) {
    Apply $f {
        param($j)
        Add-Entries $j @(
            (Make-Entry (Custom "oak" "smol" 10) 0.04 2)
            (Make-Entry (Custom "oak" "med"  10) 0.02 1)
            (Make-Entry (Custom "oak" "tall"  5) 0.0035 1)
            (Make-Entry (Custom "oak" "large" 1) 0.0002 1)
        )
    }
}

# -----------------------------------------------------------------------
# temperate/birch-forest + birch-forest-extended
# -----------------------------------------------------------------------
Write-Output "--- temperate/birch-forest ---"
Apply "temperate/birch-forest.json" {
    param($j)
    Add-Entries $j @(
        (Make-Entry (Custom "birch" "smol" 10) 0.05 2)
        (Make-Entry (Custom "birch" "med"  10) 0.02 1)
    )
}
Apply "temperate/birch-forest-extended.json" {
    param($j)
    Trim-Objects $j.objects "antioch" 0.40
    Add-Entries $j @(
        (Make-Entry (Custom "birch" "smol" 10) 0.05 2)
        (Make-Entry (Custom "birch" "med"  10) 0.02 1)
    )
}

# -----------------------------------------------------------------------
# temperate/birch-tall
# -----------------------------------------------------------------------
Write-Output "--- temperate/birch-tall ---"
Apply "temperate/birch-tall.json" {
    param($j)
    Trim-Objects $j.objects "largeponderosa" 0.15
    Add-Entries $j @(
        (Make-Entry (Custom "birch-tall" "med"   5) 0.06 1)
        (Make-Entry (Custom "birch-tall" "tall"  5) 0.014 1)
        (Make-Entry (Custom "birch-tall" "large" 2) 0.002 1)
    )
}

# -----------------------------------------------------------------------
# temperate/flower-forest + flower-forest-extended
# -----------------------------------------------------------------------
Write-Output "--- temperate/flower-forest ---"
foreach ($f in @("temperate/flower-forest.json","temperate/flower-forest-extended.json")) {
    Apply $f {
        param($j)
        Trim-Objects $j.objects "antioch" 0.40
        Add-Entries $j @(
            (Make-Entry (Custom "birch" "smol" 5) 0.04 2)
            (Make-Entry (Custom "birch" "med"  3) 0.015 1)
        )
    }
}

# -----------------------------------------------------------------------
# temperate/plateau + plateau-extended
# -----------------------------------------------------------------------
Write-Output "--- temperate/plateau ---"
foreach ($f in @("temperate/plateau.json","temperate/plateau-extended.json")) {
    Apply $f {
        param($j)
        Trim-Objects $j.objects "antioch" 0.50
        Add-Entries $j @(
            (Make-Entry (Custom "birch" "smol" 5) 0.03 1)
        )
    }
}

# -----------------------------------------------------------------------
# temperate/longtree-forest + longtree-forest-extended
# -----------------------------------------------------------------------
Write-Output "--- temperate/longtree-forest ---"
foreach ($f in @("temperate/longtree-forest.json","temperate/longtree-forest-extended.json")) {
    Apply $f {
        param($j)
        Trim-Objects $j.objects "toak" 0.25
        Add-Entries $j @(
            (Make-Entry (Custom "oak" "med"  5) 0.04 1)
            (Make-Entry (Custom "oak" "tall" 3) 0.007 1)
        )
    }
}

# -----------------------------------------------------------------------
# temperate/oak-forest + oak-forest-extended + oak-forest-flat
# -----------------------------------------------------------------------
Write-Output "--- temperate/oak-forest ---"
foreach ($f in @("temperate/oak-forest.json","temperate/oak-forest-extended.json","temperate/oak-forest-flat.json")) {
    Apply $f {
        param($j)
        Add-Entries $j @(
            (Make-Entry (Custom "oak" "smol" 10) 0.04 2)
            (Make-Entry (Custom "oak" "med"  10) 0.02 1)
            (Make-Entry (Custom "oak" "tall"  5) 0.0028 1)
        )
    }
}

# -----------------------------------------------------------------------
# temperate/combo-forest + combo-forest-extended
# -----------------------------------------------------------------------
Write-Output "--- temperate/combo-forest ---"
foreach ($f in @("temperate/combo-forest.json","temperate/combo-forest-extended.json")) {
    Apply $f {
        param($j)
        Add-Entries $j @(
            (Make-Entry (Custom "oak"   "smol" 5) 0.03 1)
            (Make-Entry (Custom "oak"   "med"  5) 0.015 1)
            (Make-Entry (Custom "birch" "smol" 5) 0.03 1)
            (Make-Entry (Custom "birch" "med"  5) 0.015 1)
        )
    }
}

# -----------------------------------------------------------------------
# temperate/croak
# -----------------------------------------------------------------------
Write-Output "--- temperate/croak ---"
Apply "temperate/croak.json" {
    param($j)
    Add-Entries $j @(
        (Make-Entry (Custom "oak" "smol" 5) 0.04 1)
        (Make-Entry (Custom "oak" "med"  3) 0.015 1)
    )
}

# -----------------------------------------------------------------------
# temperate/roughplains
# -----------------------------------------------------------------------
Write-Output "--- temperate/roughplains ---"
Apply "temperate/roughplains.json" {
    param($j)
    Add-Entries $j @(
        (Make-Entry (Custom "oak" "smol" 5) 0.025 1)
        (Make-Entry (Custom "oak" "med"  3) 0.008 1)
    )
}

# -----------------------------------------------------------------------
# temperate/sakura-forest + osaka-violet-forest
# -----------------------------------------------------------------------
Write-Output "--- temperate/sakura + osaka ---"
foreach ($f in @("temperate/sakura-forest.json","temperate/osaka-violet-forest.json","temperate/osaka-red-forest.json")) {
    Apply $f {
        param($j)
        Add-Entries $j @(
            (Make-Entry (Custom "cherry" "smol" 5) 0.03 1)
            (Make-Entry (Custom "cherry" "med"  5) 0.015 1)
        )
    }
}

# -----------------------------------------------------------------------
# tundra/maple-forest (our custom biome - full tree set)
# -----------------------------------------------------------------------
Write-Output "--- tundra/maple-forest ---"
Apply "tundra/maple-forest.json" {
    param($j)
    Add-Entries $j @(
        (Make-Entry (Custom "oak"   "smol" 10) 0.05 2)
        (Make-Entry (Custom "oak"   "med"  10) 0.025 1)
        (Make-Entry (Custom "birch" "smol"  5) 0.04 1)
        (Make-Entry (Custom "birch" "med"   5) 0.02 1)
        (Make-Entry (Custom "oak"   "tall"  3) 0.0035 1)
    )
}

# -----------------------------------------------------------------------
# tropical/rainforest + rainforest-hills + rainforest-wicked
# -----------------------------------------------------------------------
Write-Output "--- tropical/rainforest ---"
Apply "tropical/rainforest.json" {
    param($j)
    Trim-Objects $j.objects "largegeneric" 0.004
    Add-Entries $j @(
        (Make-Entry (Custom "jungle" "med"   2) 0.04 2)
        (Make-Entry (Custom "jungle" "tall"  5) 0.011 1)
        (Make-Entry (Custom "jungle" "large" 5) 0.002 1)
        (Make-Entry (Custom "jungle" "giant" 3) 0.0004 1)
    )
}
Apply "tropical/rainforest-hills.json" {
    param($j)
    Trim-Objects $j.objects "largegeneric" 0.008
    Add-Entries $j @(
        (Make-Entry (Custom "jungle" "med"   2) 0.05 2)
        (Make-Entry (Custom "jungle" "tall"  5) 0.014 1)
        (Make-Entry (Custom "jungle" "large" 5) 0.003 1)
        (Make-Entry (Custom "jungle" "giant" 3) 0.0005 1)
    )
}
Apply "tropical/rainforest-wicked.json" {
    param($j)
    Trim-Objects $j.objects "largegeneric" 0.004
    Add-Entries $j @(
        (Make-Entry (Custom "jungle" "med"   2) 0.04 2)
        (Make-Entry (Custom "jungle" "tall"  5) 0.011 1)
        (Make-Entry (Custom "jungle" "large" 5) 0.002 1)
        (Make-Entry (Custom "jungle" "giant" 3) 0.0004 1)
    )
}

# -----------------------------------------------------------------------
# tropical/plains + tropical/plains-hills
# -----------------------------------------------------------------------
Write-Output "--- tropical/plains ---"
foreach ($f in @("tropical/plains.json","tropical/plains-hills.json")) {
    Apply $f {
        param($j)
        Add-Entries $j @(
            (Make-Entry (Custom "jungle" "med"  2) 0.03 1)
            (Make-Entry (Custom "jungle" "tall" 3) 0.008 1)
        )
    }
}

# -----------------------------------------------------------------------
# tropical/mountain variants
# -----------------------------------------------------------------------
Write-Output "--- tropical/mountain ---"
foreach ($f in @("tropical/mountain.json","tropical/mountain-middle.json","tropical/mountain-extreme.json","tropical/mountain-plains.json")) {
    Apply $f {
        param($j)
        Add-Entries $j @(
            (Make-Entry (Custom "jungle" "med"  2) 0.03 1)
            (Make-Entry (Custom "jungle" "tall" 3) 0.01 1)
        )
    }
}

# -----------------------------------------------------------------------
# tropical/bamboo-forest
# -----------------------------------------------------------------------
Write-Output "--- tropical/bamboo-forest ---"
Apply "tropical/bamboo-forest.json" {
    param($j)
    Add-Entries $j @(
        (Make-Entry (Custom "jungle" "med" 2) 0.025 1)
    )
}

# -----------------------------------------------------------------------
# tundra/redwood-forest + redwood-extended-cliffs + autumn + autumn-extended
# -----------------------------------------------------------------------
Write-Output "--- tundra/redwood + autumn ---"
foreach ($f in @("tundra/redwood-forest.json","tundra/autumn.json","tundra/autumn-extended.json")) {
    Apply $f {
        param($j)
        Trim-Objects $j.objects "tredwood" 0.12
        Trim-Objects $j.objects "tredwoodsmol" 0.20
        Add-Entries $j @(
            (Make-Entry (Custom "spruce-og" "med"   5) 0.06 1)
            (Make-Entry (Custom "spruce-og" "tall"  5) 0.014 1)
            (Make-Entry (Custom "spruce-og" "large" 3) 0.002 1)
        )
    }
}
Apply "tundra/redwood-extended-cliffs.json" {
    param($j)
    Trim-Objects $j.objects "tredwood" 0.12
    Trim-Objects $j.objects "tredwoodsmol" 0.20
    Trim-Objects $j.objects "sup-pine" 0.10
    Add-Entries $j @(
        (Make-Entry (Custom "spruce-og" "med"  5) 0.05 1)
        (Make-Entry (Custom "spruce-og" "tall" 3) 0.01 1)
    )
}

# -----------------------------------------------------------------------
# frozen/tundra-winter
# -----------------------------------------------------------------------
Write-Output "--- frozen/tundra-winter ---"
Apply "frozen/tundra-winter.json" {
    param($j)
    Trim-Objects $j.objects "tredwood" 0.12
    Trim-Objects $j.objects "tredwoodsmol" 0.20
    Add-Entries $j @(
        (Make-Entry (Custom "spruce-og" "med"  5) 0.05 1)
        (Make-Entry (Custom "spruce-og" "tall" 3) 0.01 1)
    )
}

# -----------------------------------------------------------------------
# swamp/roofed-forest variants
# -----------------------------------------------------------------------
Write-Output "--- swamp/roofed-forest ---"
foreach ($f in @("swamp/roofed-forest.json","swamp/roofed-forest-extended.json","swamp/roofed-wayward.json","swamp/roofed-wayward-extended.json")) {
    Apply $f {
        param($j)
        Add-Entries $j @(
            (Make-Entry (Custom "darkoak" "smol" 5) 0.03 1)
            (Make-Entry (Custom "darkoak" "med"  5) 0.015 1)
        )
    }
}

# -----------------------------------------------------------------------
# swamp/willow-forest + willow-forest-extended
# -----------------------------------------------------------------------
Write-Output "--- swamp/willow-forest ---"
foreach ($f in @("swamp/willow-forest.json","swamp/willow-forest-extended.json")) {
    Apply $f {
        param($j)
        Add-Entries $j @(
            (Make-Entry (Custom "darkoak" "smol" 5) 0.025 1)
            (Make-Entry (Custom "darkoak" "med"  3) 0.01 1)
        )
    }
}

# -----------------------------------------------------------------------
# swamp/cambian-drift + cambian-drift-extended
# -----------------------------------------------------------------------
Write-Output "--- swamp/cambian-drift ---"
foreach ($f in @("swamp/cambian-drift.json","swamp/cambian-drift-extended.json")) {
    Apply $f {
        param($j)
        Add-Entries $j @(
            (Make-Entry (Custom "darkoak" "smol" 5) 0.02 1)
            (Make-Entry (Custom "darkoak" "med"  3) 0.008 1)
        )
    }
}

# -----------------------------------------------------------------------
# frozen/hills + frozen/hills-extended + frozen/plains + frozen/pine-plains
# -----------------------------------------------------------------------
Write-Output "--- frozen/hills + plains ---"
foreach ($f in @("frozen/hills.json","frozen/hills-extended.json","frozen/plains.json","frozen/pine-plains.json")) {
    Apply $f {
        param($j)
        Add-Entries $j @(
            (Make-Entry (Custom "spruce" "smol" 5) 0.03 1)
            (Make-Entry (Custom "spruce" "med"  3) 0.015 1)
        )
    }
}

# -----------------------------------------------------------------------
# frozen/spruce-hills + spruce-hills-extended + spruce-plains + pines
# -----------------------------------------------------------------------
Write-Output "--- frozen/spruce-hills + pines ---"
foreach ($f in @("frozen/spruce-hills.json","frozen/spruce-hills-extended.json","frozen/spruce-plains.json","frozen/pines.json")) {
    Apply $f {
        param($j)
        Add-Entries $j @(
            (Make-Entry (Custom "spruce" "smol" 5) 0.02 1)
            (Make-Entry (Custom "spruce" "med"  3) 0.01 1)
        )
    }
}

# -----------------------------------------------------------------------
# frozen/pine-hills + mountain/forest + mountain/forest-extended
# -----------------------------------------------------------------------
Write-Output "--- frozen/pine-hills + mountain/forest ---"
foreach ($f in @("frozen/pine-hills.json","mountain/forest.json","mountain/forest-extended.json")) {
    Apply $f {
        param($j)
        Add-Entries $j @(
            (Make-Entry (Custom "spruce" "smol" 5) 0.04 1)
            (Make-Entry (Custom "spruce" "med"  5) 0.02 1)
        )
    }
}

# -----------------------------------------------------------------------
# tundra/taiga + tundra/taiga-extended
# -----------------------------------------------------------------------
Write-Output "--- tundra/taiga ---"
foreach ($f in @("tundra/taiga.json","tundra/taiga-extended.json")) {
    Apply $f {
        param($j)
        Trim-Objects $j.objects "tredwoodsmol" 0.08
        Add-Entries $j @(
            (Make-Entry (Custom "spruce" "smol" 5) 0.04 1)
            (Make-Entry (Custom "spruce" "med"  5) 0.02 1)
        )
    }
}

# -----------------------------------------------------------------------
# tundra/mountains + tundra/mountains-extended-cliffs + tundra/forest-extended-cliffs
# -----------------------------------------------------------------------
Write-Output "--- tundra/mountains ---"
foreach ($f in @("tundra/mountains.json","tundra/mountains-extended-cliffs.json","tundra/forest-extended-cliffs.json")) {
    Apply $f {
        param($j)
        Add-Entries $j @(
            (Make-Entry (Custom "spruce" "smol" 5) 0.025 1)
            (Make-Entry (Custom "spruce" "med"  3) 0.01 1)
        )
    }
}

# -----------------------------------------------------------------------
# mountain/mplain-extended
# -----------------------------------------------------------------------
Write-Output "--- mountain/mplain-extended ---"
Apply "mountain/mplain-extended.json" {
    param($j)
    Add-Entries $j @(
        (Make-Entry (Custom "oak" "smol" 5) 0.03 1)
        (Make-Entry (Custom "oak" "med"  3) 0.012 1)
    )
}

# -----------------------------------------------------------------------
# mountain/floating-islands
# -----------------------------------------------------------------------
Write-Output "--- mountain/floating-islands ---"
Apply "mountain/floating-islands.json" {
    param($j)
    Add-Entries $j @(
        (Make-Entry (Custom "oak"    "smol" 3) 0.015 1)
        (Make-Entry (Custom "spruce" "smol" 2) 0.005 1)
    )
}

# -----------------------------------------------------------------------
# hot/small-valley + mesa/valleys
# -----------------------------------------------------------------------
Write-Output "--- hot/small-valley + mesa/valleys ---"
foreach ($f in @("hot/small-valley.json","mesa/valleys.json")) {
    Apply $f {
        param($j)
        Add-Entries $j @(
            (Make-Entry (Custom "acacia" "smol" 5) 0.03 1)
            (Make-Entry (Custom "acacia" "med"  3) 0.01 1)
        )
    }
}

# -----------------------------------------------------------------------
# savanna/savanna + savanna/cliff + savanna/cliff-extended
# -----------------------------------------------------------------------
Write-Output "--- savanna ---"
foreach ($f in @("savanna/savanna.json","savanna/cliff.json","savanna/cliff-extended.json")) {
    Apply $f {
        param($j)
        Add-Entries $j @(
            (Make-Entry (Custom "acacia" "smol" 5) 0.025 1)
            (Make-Entry (Custom "acacia" "med"  3) 0.01 1)
        )
    }
}

# -----------------------------------------------------------------------
# mesa/plateau-dirt + mesa/plateau-dirt-high
# -----------------------------------------------------------------------
Write-Output "--- mesa/plateau ---"
foreach ($f in @("mesa/plateau-dirt.json","mesa/plateau-dirt-high.json")) {
    Apply $f {
        param($j)
        Add-Entries $j @(
            (Make-Entry (Custom "acacia" "smol" 3) 0.02 1)
            (Make-Entry (Custom "acacia" "med"  2) 0.006 1)
        )
    }
}

# -----------------------------------------------------------------------
# terralost/amethyst-canyon + amethyst-rainforest (AmyLarge trim)
# -----------------------------------------------------------------------
Write-Output "--- terralost/amethyst ---"
Apply "terralost/amethyst-canyon.json" {
    param($j)
    Trim-Objects $j.objects "AmyLarge" 0.08
}
Apply "terralost/amethyst-rainforest.json" {
    param($j)
    Trim-Objects $j.objects "AmyLarge" 0.10
    Trim-Objects $j.objects "AmyMed" 0.30
}

# -----------------------------------------------------------------------
# temperate/birch-denmyre
# -----------------------------------------------------------------------
Write-Output "--- temperate/birch-denmyre ---"
Apply "temperate/birch-denmyre.json" {
    param($j)
    Add-Entries $j @(
        (Make-Entry (Custom "birch" "smol" 5) 0.04 1)
        (Make-Entry (Custom "birch" "med"  3) 0.015 1)
    )
}

# -----------------------------------------------------------------------
# temperate/cherry-blossom-forest (cherry supplement)
# -----------------------------------------------------------------------
Write-Output "--- temperate/cherry-blossom-forest ---"
Apply "temperate/cherry-blossom-forest.json" {
    param($j)
    Add-Entries $j @(
        (Make-Entry (Custom "cherry" "smol" 5) 0.03 1)
        (Make-Entry (Custom "cherry" "med"  5) 0.015 1)
    )
}

# -----------------------------------------------------------------------
# temperate/reaching-forest + reaching-forest-violet (oak supplement)
# -----------------------------------------------------------------------
Write-Output "--- temperate/reaching-forest ---"
foreach ($f in @("temperate/reaching-forest.json","temperate/reaching-forest-violet.json")) {
    Apply $f {
        param($j)
        Add-Entries $j @(
            (Make-Entry (Custom "oak" "smol" 5) 0.03 1)
            (Make-Entry (Custom "oak" "med"  3) 0.01 1)
        )
    }
}

# -----------------------------------------------------------------------
# debug-single: force $DebugObject as the ONLY tree across EVERY biome in the
# pack (not just the curated list), zeroing all stock tree placements. This is
# what makes the sanity check unambiguous - no other trees can spawn anywhere.
# -----------------------------------------------------------------------
if ($Mode -eq 'debug-single') {
    Write-Output ""
    Write-Output "--- debug-single: forcing $DebugObject across ALL biomes ---"
    $entry = "    { `"chance`": $DebugChance, `"place`": [`"trees/custom/$DebugObject`"]$(Placement-Suffix) }"
    $count = 0
    Get-ChildItem -Recurse $SnapshotRoot -Filter *.json -File | ForEach-Object {
        $rel = $_.FullName.Substring($SnapshotRoot.Length).TrimStart('\')
        $dst = Join-Path $PackRoot $rel
        $raw = Get-Content $_.FullName -Raw
        $raw = [regex]::Replace($raw, ',\s*\{[^{}]*trees/custom[^{}]*\}', '')
        $raw = Suppress-StockTrees $raw
        $raw = Insert-Entries $raw @($entry)
        New-Item -ItemType Directory -Force -Path (Split-Path $dst) | Out-Null
        [System.IO.File]::WriteAllText($dst, $raw, [System.Text.UTF8Encoding]::new($false))
        $count++
    }
    Write-Output "  processed $count biome files"
}

# -----------------------------------------------------------------------
# Sync custom tree objects into the pack so every "trees/custom/<name>"
# placement resolves. Without this, Iris logs "Couldn't find Object" and
# silently skips the placement even though the biome JSON is valid.
# -----------------------------------------------------------------------
Write-Output ""
Write-Output "--- syncing custom tree objects ---"
if (-not (Test-Path $SourceObjects)) {
    Write-Warning "Source objects dir not found: $SourceObjects (skipping object sync)"
} else {
    # Collect every distinct trees/custom/<name> referenced across all biome JSONs.
    $refs = New-Object System.Collections.Generic.HashSet[string]
    Get-ChildItem -Recurse $PackRoot -Filter *.json -File | ForEach-Object {
        [regex]::Matches((Get-Content $_.FullName -Raw), 'trees/custom/([A-Za-z0-9\-]+)') |
            ForEach-Object { [void]$refs.Add($_.Groups[1].Value) }
    }
    # Index source objects by base name for O(1) lookup.
    $index = @{}
    Get-ChildItem $SourceObjects -Recurse -Filter *.iob -File | ForEach-Object { $index[$_.BaseName] = $_.FullName }
    New-Item -ItemType Directory -Force -Path $ObjectsRoot | Out-Null
    $copied = 0; $missing = @()
    foreach ($n in $refs) {
        if ($index.ContainsKey($n)) {
            Copy-Item $index[$n] (Join-Path $ObjectsRoot "$n.iob") -Force
            $copied++
        } else {
            $missing += $n
        }
    }
    Write-Output "  referenced=$($refs.Count) copied=$copied missing=$($missing.Count)"
    foreach ($m in $missing) { Write-Warning "MISSING OBJECT: $m (no $m.iob under $SourceObjects)" }
}

Write-Output ""
Write-Output "=== Done ==="
