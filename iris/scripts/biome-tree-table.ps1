$packRoot = "C:\GameServers\Minecraft\testServer\RTP-Folia\26.1\plugins\Iris\packs\overworld\biomes"
$surfaceDirs = @("frozen","hot","mesa","mountain","mushroom","savanna","swamp","temperate","terralost","tropical","tundra")

# Verified heights from .iob binary headers (W x H x D measured 2026-06-04)
# Format: "folder/name" = H (block height)
$verifiedH = @{
    "mixed/tredwood1"         = 93
    "mixed/tredwood2"         = 90
    "mixed/tredwood3"         = 103
    "mixed/tredwood4"         = 110
    "mixed/tredwood5"         = 93
    "mixed/tredwoodsmol1"     = 30
    "mixed/tredwoodsmol2"     = 39
    "mixed/tredwoodsmol3"     = 36
    "mixed/tredwoodsmol4"     = 21
    "mixed/tredwoodsmol5"     = 23
    "mixed/tredwoodbee1"      = 92
    "mixed/AmyLarge1"         = 88
    "mixed/AmyLarge2"         = 78
    "mixed/AmyLarge3"         = 76
    "mixed/AmyLarge4"         = 70
    "mixed/AmyMed1"           = 21
    "mixed/AmyMed2"           = 21
    "mixed/AmyMed3"           = 21
    "mixed/AmyMed4"           = 21
    "mixed/AmyNormal1"        = 10
    "mixed/AmyNormal2"        = 10
    "mixed/AmyNormal3"        = 9
    "mixed/AmyNormal4"        = 9
    "mixed/AmySmol1"          = 4
    "mixed/AmySmol2"          = 4
    "mixed/pollup1"           = 6
    "mixed/pollup2"           = 7
    "mixed/pollup3"           = 8
    "mixed/pollup4"           = 8
    "mixed/dotree1"           = 49
    "mixed/dotree2"           = 51
    "mixed/dotree3"           = 47
    "mixed/smoakog1"          = 94
    "mixed/smoakog80"         = 75
    "mixed/smoakog160"        = 60
    "mixed/bleedingserralita1"= 15
    "mixed/tourmalinelarge1"  = 51
    "spruce/sup-pine-1"       = 63
    "spruce/sup-pine-2"       = 66
    "spruce/sup-pine-3"       = 52
    "spruce/sup-pine-4"       = 39
    "spruce/levergreen1"      = 42
    "spruce/levergreen2"      = 42
    "spruce/levergreen3"      = 57
    "spruce/levergreen4"      = 58
    "spruce/pine1"            = 25
    "spruce/pine2"            = 27
    "spruce/pine3"            = 29
    "spruce/pine4"            = 34
    "spruce/lfrostgeneric1"   = 36
    "spruce/lfrostgeneric2"   = 29
    "spruce/vgeneric1"        = 28
    "sproak/generic1"         = 30
    "sproak/generic2"         = 25
    "sproak/generic3"         = 26
    "sproak/generic4"         = 27
    "sproak/sp1"              = 60
    "oak/toak1"               = 16
    "oak/toak2"               = 15
    "oak/toak3"               = 17
    "oak/toak4"               = 20
    "oak/hoakgeneric1"        = 34
    "oak/hoakgeneric2"        = 27
    "oak/hoakgeneric3"        = 31
    "oak/hoakgeneric4"        = 28
    "oak/troofed1"            = 16
    "oak/troofed2"            = 14
    "oak/mroofed1"            = 12
    "oak/oakFancy1"           = 16
    "oak/croak1"              = 31
    "oak/lponderosa1"         = 32
    "oak/truegeneric1"        = 19
    "oak/dadwood1"            = 18
    "oak/dead1"               = 9
    "birch/antioch1"          = 18
    "birch/antioch2"          = 14
    "birch/antioch3"          = 13
    "birch/largeponderosa1"   = 30
    "birch/largeponderosa2"   = 32
    "darkoak/talldrift1"      = 77
    "darkoak/talldrift2"      = 76
    "darkoak/generic1"        = 20
    "darkoak/generic2"        = 10
    "bonsai/med-1"            = 18
    "bonsai/med-2"            = 14
    "bonsai/med-3"            = 12
    "bonsai/med-4"            = 14
    "jungle/lgeneric1"        = 12
    "jungle/lgeneric2"        = 14
    "jungle/largegeneric1"    = 124
    "jungle/largegeneric2"    = 60
    "sakura/genericsak1"      = 51
    "sakura/genericsak2"      = 48
    "mangrove/mangrove1"      = 48
    "mushroom/ice1"           = 19
    "mushroom/froShroom1"     = 13
    "mushroom/browngeneric1"  = 9
    "mushroom/redgeneric1"    = 15
    "mushroom/smolshroom1"    = 2
    "mushroom/redlumotall1"   = 15
    "mushroom/mushclut1"      = 11
    "acacia/vexed1"           = 12
    "acacia/savannaD1"        = 8
    "acacia/savannaF1"        = 10
    "acacia/savannaS1"        = 3
    "acacia/denmyre1"         = 26
}

# Height threshold above which a tree is flagged [LARGE] (verified, not harvestable without scaffolding)
$largeThreshold = 45

# Verified block palettes from .iob binary parsing (2026-06-04)
# Format: "folder/name" = "block1, block2, ..." (air excluded, namespace stripped, blockstate stripped)
$paletteMap = @{
    "mixed/tredwood1"         = "dark_oak_wood, spruce_leaves, spruce_wood, birch_leaves"
    "mixed/tredwoodsmol1"     = "spruce_wood, spruce_leaves, oak_leaves"
    "mixed/tredwoodbee1"      = "dark_oak_wood, spruce_leaves, spruce_wood, birch_leaves"
    "mixed/AmyLarge1"         = "dark_oak_wood, jungle_leaves, oak_leaves"
    "mixed/AmyMed1"           = "dark_oak_wood, jungle_leaves, oak_leaves, birch_leaves, spruce_leaves"
    "mixed/AmyNormal1"        = "dark_oak_wood, jungle_leaves, oak_leaves"
    "mixed/AmySmol1"          = "dark_oak_wood, jungle_leaves, oak_leaves"
    "mixed/pollup1"           = "birch_wood, birch_leaves, spruce_wood, spruce_leaves, oak_leaves, azalea_leaves"
    "mixed/dotree1"           = "dark_oak_wood, dark_oak_leaves, stripped_dark_oak_wood, spruce_wood, dark_oak_fence, oak_fence, spruce_fence, dark_oak_slab"
    "mixed/smoakog1"          = "oak_wood, oak_leaves, black_wool, white_wool"
    "mixed/smoakog80"         = "oak_wood, oak_leaves, black_wool, white_wool"
    "mixed/smoakog160"        = "oak_wood, oak_leaves, black_wool, white_wool"
    "spruce/sup-pine-1"       = "spruce_wood, spruce_leaves, spruce_fence"
    "spruce/levergreen1"      = "spruce_wood, spruce_leaves"
    "spruce/pine1"            = "spruce_wood, spruce_leaves, dark_oak_wood, dark_oak_fence, dark_oak_fence_gate, spruce_sapling"
    "spruce/lfrostgeneric1"   = "spruce_wood, spruce_leaves, dark_oak_wood, snow"
    "spruce/vgeneric1"        = "spruce_wood, spruce_leaves, dark_oak_wood"
    "sproak/sp1"              = "spruce_wood, spruce_leaves, dark_oak_wood, dark_oak_leaves, oak_leaves"
    "sproak/generic1"         = "spruce_wood, spruce_leaves, oak_wood, stripped_spruce_wood, spruce_fence, spruce_planks, snow"
    "oak/toak1"               = "oak_wood, oak_leaves, spruce_fence"
    "oak/hoakgeneric1"        = "oak_wood, oak_leaves"
    "oak/troofed1"            = "oak_wood, stripped_dark_oak_wood, dark_oak_leaves, spruce_leaves, vine"
    "oak/croak1"              = "oak_log, oak_leaves"
    "oak/lponderosa1"         = "oak_wood, oak_leaves"
    "oak/dadwood1"            = "oak_wood, stripped_oak_wood, oak_fence"
    "oak/dead1"               = "oak_wood, oak_leaves"
    "birch/antioch3"          = "birch_wood, birch_leaves"
    "birch/largeponderosa1"   = "birch_wood, birch_leaves"
    "darkoak/talldrift1"      = "dark_oak_wood, dark_oak_leaves, spruce_wood, oak_leaves"
    "darkoak/generic1"        = "dark_oak_wood, dark_oak_fence, oak_leaves"
    "bonsai/med-1"            = "oak_wood, oak_leaves, spruce_leaves"
    "jungle/lgeneric1"        = "jungle_wood, jungle_leaves, birch_leaves, vine"
    "jungle/largegeneric1"    = "jungle_wood, jungle_leaves, oak_wood, vine"
    "jungle/largegeneric2"    = "jungle_wood, jungle_leaves, oak_wood, vine"
    "sakura/genericsak1"      = "stripped_birch_wood, oak_leaves"
    "mangrove/mangrove1"      = "mangrove_wood, mangrove_leaves, mangrove_roots, jungle_leaves, oak_fence"
    "acacia/denmyre1"         = "acacia_wood, acacia_leaves, acacia_planks, acacia_fence"
    "acacia/savannaF1"        = "acacia_wood, acacia_leaves, acacia_fence"
    "acacia/vexed1"           = "acacia_wood, acacia_leaves"
    "acacia/savannaD1"        = "acacia_wood, acacia_leaves"
    "acacia/savannaS1"        = "acacia_wood, acacia_leaves"
}

# Biomes flagged for trimming: "category\file.json" -> hashtable of "trees/path/name_prefix" -> annotation
# Based on verified heights - toak removed (H=15-20, fine); tredwoodsmol added (H=21-39, still tall at density)
# levergreen flagged in sequoia context; largegeneric1 critical (H=124)
$trimMap = @{
    "tundra\redwood-forest.json"             = @{
        "trees/mixed/tredwood1"    = "0.35 -> 0.12 [TRIM] (H=93-110)"
        "trees/mixed/tredwoodsmol1"= "0.45 -> 0.20 [TRIM] (H=21-39, dense)"
    }
    "tundra\redwood-forest-extended.json"    = @{
        "trees/mixed/tredwood1"    = "0.35 -> 0.12 [TRIM] (H=93-110)"
        "trees/mixed/tredwoodsmol1"= "0.45 -> 0.20 [TRIM] (H=21-39, dense)"
    }
    "tundra\redwood-extended-cliffs.json"    = @{
        "trees/mixed/tredwood1"    = "0.35 -> 0.12 [TRIM] (H=93-110)"
        "trees/spruce/sup-pine-1"  = "0.30 -> 0.10 [TRIM] (H=52-66)"
    }
    "tundra\autumn.json"                     = @{
        "trees/mixed/tredwood1"    = "0.35 -> 0.12 [TRIM] (H=93-110)"
        "trees/mixed/tredwoodsmol1"= "0.45 -> 0.20 [TRIM] (H=21-39, dense)"
    }
    "tundra\autumn-extended.json"            = @{
        "trees/mixed/tredwood1"    = "0.35 -> 0.12 [TRIM] (H=93-110)"
        "trees/mixed/tredwoodsmol1"= "0.45 -> 0.20 [TRIM] (H=21-39, dense)"
    }
    "frozen\tundra-winter.json"              = @{
        "trees/mixed/tredwood1"    = "0.35 -> 0.12 [TRIM] (H=93-110)"
        "trees/mixed/tredwoodsmol1"= "0.45 -> 0.20 [TRIM] (H=21-39, dense)"
    }
    "temperate\birch-forest-extended.json"   = @{ "trees/birch/antioch3"       = "1.0 -> 0.4 [TRIM] (density, not height)" }
    "temperate\birch-tall.json"              = @{ "trees/birch/largeponderosa1" = "0.39 -> 0.15 [TRIM] (H=30-32)" }
    "temperate\flower-forest.json"           = @{ "trees/birch/antioch3"        = "1.0 -> 0.4 [TRIM] (density)" }
    "temperate\flower-forest-extended.json"  = @{ "trees/birch/antioch3"        = "1.0 -> 0.4 [TRIM] (density)" }
    "temperate\plateau.json"                 = @{ "trees/birch/antioch3"        = "1.0 -> 0.5 [TRIM] (density)" }
    "temperate\plateau-extended.json"        = @{ "trees/birch/antioch3"        = "1.0 -> 0.5 [TRIM] (density)" }
    "terralost\amethyst-canyon.json"         = @{ "trees/mixed/AmyLarge1"       = "0.2 -> 0.08 [TRIM] (H=70-88)" }
    "terralost\amethyst-rainforest.json"     = @{
        "trees/mixed/AmyLarge1"    = "0.3 -> 0.1 [TRIM] (H=70-88)"
        "trees/mixed/AmyMed1"      = "0.6 -> 0.3 [TRIM] (density)"
    }
    "tropical\rainforest-hills.json"         = @{ "trees/jungle/largegeneric1"  = "0.035 -> 0.01 [TRIM] (H=124 - critical)" }
    # All other rainforest variants with largegeneric1
    "tropical\rainforest.json"               = @{ "trees/jungle/largegeneric1"  = "0.01 -> 0.005 [TRIM] (H=124)" }
    "tropical\rainforest-extended.json"      = @{ "trees/jungle/largegeneric1"  = "0.01 -> 0.005 [TRIM] (H=124)" }
    "tropical\rainforest-hills-extended.json"= @{ "trees/jungle/largegeneric1"  = "0.035 -> 0.01 [TRIM] (H=124 - critical)" }
    "tropical\rainforest-island.json"        = @{ "trees/jungle/largegeneric1"  = "0.01 -> 0.005 [TRIM] (H=124)" }
    "tropical\rainforest-wicked.json"        = @{ "trees/jungle/largegeneric1"  = "0.01 -> 0.005 [TRIM] (H=124)" }
    "tropical\rainforest-wicked-child.json"  = @{ "trees/jungle/largegeneric1"  = "0.01 -> 0.005 [TRIM] (H=124)" }
    "tropical\wilds.json"                    = @{ "trees/jungle/largegeneric1"  = "0.01 -> 0.005 [TRIM] (H=124)" }
}

# Build verified height lookup: strip trailing digits from schematic name to match prefix
function Get-Palette($place) {
    $key = $place -replace "^trees/", ""
    if ($paletteMap.ContainsKey($key)) { return $paletteMap[$key] }
    $base = $key -replace "\d+$", ""
    $match = $paletteMap.Keys | Where-Object { $_ -like "$base*" } | Select-Object -First 1
    if ($match) { return $paletteMap[$match] }
    return ""
}

function Get-VerifiedH($place) {
    # place is like "trees/mixed/tredwood1" - strip leading "trees/"
    $key = $place -replace "^trees/", ""
    if ($verifiedH.ContainsKey($key)) { return $verifiedH[$key] }
    # Try stripping trailing digits for variant lookup (e.g. tredwood3 -> tredwood)
    $base = $key -replace "\d+$", ""
    $match = $verifiedH.Keys | Where-Object { $_ -like "$base*" } | Select-Object -First 1
    if ($match) { return $verifiedH[$match] }
    return $null
}

$lines = @()
$lines += "# Iris Overworld - Adoption Plan and Tree Inventory"
$lines += "Generated: $(Get-Date -Format 'yyyy-MM-dd HH:mm') - heights verified from .iob binary headers"
$lines += ""
$lines += "## Adoption Key"
$lines += ""
$lines += "| Symbol | Meaning |"
$lines += "|--------|---------|"
$lines += "| (as-is) | Adopt unchanged |"
$lines += "| (trim) | Adopt with reduced spawn rate - see inline note |"
$lines += "| H=N | Verified block height from .iob schematic header |"
$lines += "| [LARGE] | Height >= 45 blocks (harvestable only with scaffolding) |"
$lines += "| [TRIM] | This specific rate should be reduced before going live |"
$lines += ""
$lines += "## Verified Height Reference"
$lines += ""
$lines += "Key schematics measured from .iob headers:"
$lines += ""
$lines += "| Schematic | H | Notes |"
$lines += "|-----------|---|-------|"
$lines += "| tredwood1-5 | 90-110 | Full redwood - atmosphere tree, not harvest |"
$lines += "| tredwoodsmol1-5 | 21-39 | 'Small' redwood - still tall at density |"
$lines += "| tredwoodbee1 | 92 | Bee redwood variant |"
$lines += "| AmyLarge1-4 | 70-88 | Large amethyst crystal tree |"
$lines += "| AmyMed1-4 | 21 | Medium amethyst - harvestable |"
$lines += "| AmyNormal1-4 | 9-10 | Normal amethyst - fine |"
$lines += "| AmySmol1-2 | 4 | Small amethyst - fine |"
$lines += "| sup-pine-1-4 | 39-66 | Sequoia pine - atmosphere tree |"
$lines += "| levergreen1-4 | 42-58 | Tall spruce - borderline |"
$lines += "| sproak/sp1 | 60 | Tall spruce-oak hybrid |"
$lines += "| dotree1-3 | 47-51 | Magic/Ether forest tree - atmosphere |"
$lines += "| smoakog1 | 94 | Massive oak (name misleading - not 1 block) |"
$lines += "| smoakog80 | 75 | Large oak |"
$lines += "| smoakog160 | 60 | Large oak (name misleading - not 160 blocks) |"
$lines += "| jungle/largegeneric1 | 124 | CRITICAL - largest tree in pack |"
$lines += "| jungle/largegeneric2 | 60 | Large jungle tree |"
$lines += "| sakura/genericsak1-2 | 48-51 | Sakura - atmosphere/spectacle |"
$lines += "| darkoak/talldrift1-2 | 76-77 | Tall dark oak - atmosphere (Cambian Drift) |"
$lines += "| toak1-4 | 15-20 | Long oak - FINE, fully harvestable |"
$lines += "| largeponderosa1-2 | 30-32 | Tall birch - borderline but ok |"
$lines += "| antioch1-3 | 13-18 | Birch variant - fine |"
$lines += "| hoakgeneric1-4 | 27-34 | Tall oak - harvestable |"
$lines += "| pollup1-4 | 6-8 | Round snow-capped - fine |"
$lines += "| bonsai/med-1-4 | 12-18 | Bonsai - fine |"
$lines += "| sproak/generic1-4 | 25-30 | Spruce-oak - fine |"
$lines += "| pine1-4 | 25-34 | Standard spruce - fine |"
$lines += "| lfrostgeneric1-2 | 29-36 | Frost spruce - fine |"
$lines += "| vgeneric1 | 28 | Mountain spruce - fine |"
$lines += ""

foreach ($cat in $surfaceDirs) {
    $files = Get-ChildItem -Path "$packRoot\$cat" -Filter "*.json" -File -ErrorAction SilentlyContinue | Sort-Object Name
    if (-not $files) { continue }

    $lines += "## $cat"
    $lines += ""
    $lines += "| Biome | File | Rarity | Derivative | Tree Objects | Palette |"
    $lines += "|-------|------|--------|------------|--------------|---------|"

    foreach ($f in $files) {
        $json = Get-Content $f.FullName -Raw | ConvertFrom-Json -ErrorAction SilentlyContinue
        if (-not $json) { continue }

        $relKey = "$cat\$($f.Name)"
        $hasTrim = $trimMap.ContainsKey($relKey)

        $treeEntries = @()
        $anyTrim = $false
        if ($json.objects) {
            foreach ($obj in $json.objects) {
                $places = ($obj.place -split "\s+") | Where-Object { $_ -ne "" }
                $first = $places[0]
                if ($first -notmatch "^trees/") { continue }
                $count = $places.Count

                $h = Get-VerifiedH $first
                $hTag = if ($h) { " H=$h" } else { "" }
                $isLarge = $h -and $h -ge $largeThreshold
                $largeTag = if ($isLarge) { " [LARGE]" } else { "" }

                $trimTag = ""
                if ($hasTrim) {
                    # Match on prefix (strip trailing digits)
                    $matchKey = $trimMap[$relKey].Keys | Where-Object {
                        $first -like "$_*" -or $first -eq $_
                    } | Select-Object -First 1
                    if ($matchKey) {
                        $trimTag = " -> " + $trimMap[$relKey][$matchKey]
                        $anyTrim = $true
                    }
                }

                $pal = Get-Palette $first
                $palTag = if ($pal) { " [$pal]" } else { "" }
                $treeEntries += "`"$first`" +$($count-1) @ $($obj.chance)$hTag$largeTag$trimTag$palTag"
            }
        }

        $verdict = if ($anyTrim) { "(trim)" } else { "(as-is)" }
        $treesCell = if ($treeEntries.Count -gt 0) { $treeEntries -join "<br>" } else { "none" }
        $name = if ($json.name) { $json.name } else { $f.BaseName }
        $rarity = if ($json.rarity) { $json.rarity } else { "-" }
        $deriv = if ($json.derivative) { $json.derivative } else { "-" }

        # Collect unique palette blocks across all tree objects in this biome
        $biomePalette = @()
        if ($json.objects) {
            foreach ($obj in $json.objects) {
                $places = ($obj.place -split "\s+") | Where-Object { $_ -ne "" }
                $first = $places[0]
                if ($first -notmatch "^trees/") { continue }
                $pal = Get-Palette $first
                if ($pal) { $biomePalette += $pal -split ", " }
            }
        }
        $paletteCell = ($biomePalette | Sort-Object -Unique) -join ", "
        $lines += "| $name $verdict | `"$($f.Name)`" | $rarity | $deriv | $treesCell | $paletteCell |"
    }
    $lines += ""
}

$outPath = "C:\Users\lxgol\IdeaProjects\LeafsServerBaseline\docs\scratch\IRIS-ADOPTION-PLAN.md"
$lines | Set-Content -Path $outPath -Encoding UTF8
Write-Output "Written: $outPath  ($($lines.Count) lines)"
