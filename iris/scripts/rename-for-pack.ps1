# rename-for-pack.ps1
# Renames generated .iob files to stable custom-<species>-<tier>-<n>.iob names
# and copies them to scripts/output/renamed/<species>/
#
# Usage: .\scripts\rename-for-pack.ps1
# Output: scripts/output/renamed/<species>/custom-<species>-<tier>-<n>.iob

param(
    [string]$SourceRoot = "scripts\output",
    [string]$OutRoot    = "scripts\output\renamed"
)

# Height tier boundaries (inclusive lower, exclusive upper)
# Label  | h range
# smol   | 5-12
# med    | 13-25
# tall   | 26-45
# large  | 46-70
# giant  | 71+
function Get-Tier([int]$h) {
    if ($h -le 12)  { return "smol" }
    if ($h -le 25)  { return "med" }
    if ($h -le 45)  { return "tall" }
    if ($h -le 70)  { return "large" }
    return "giant"
}

# Map source directory name -> species label used in output filenames
$speciesMap = @{
    "forest"                  = "oak"
    "birch-forest"            = "birch"
    "old-growth-birch-forest" = "birch-tall"
    "cherry-grove"            = "cherry"
    "dark-forest"             = "darkoak"
    "jungle"                  = "jungle"
    "taiga"                   = "spruce"
    "old-growth-pine-taiga"   = "spruce-og"
    "savanna"                 = "acacia"
}

$totalCopied = 0

foreach ($entry in $speciesMap.GetEnumerator()) {
    $srcDir  = Join-Path $SourceRoot $entry.Key
    $species = $entry.Value

    if (-not (Test-Path $srcDir)) {
        Write-Warning "Source dir not found: $srcDir"
        continue
    }

    $files = Get-ChildItem $srcDir -Filter "*.iob" -File | Sort-Object Name

    # Group by tier, then assign sequential n within each tier
    $tierGroups = @{}
    foreach ($f in $files) {
        if ($f.Name -match '_h(\d+)_') {
            $h    = [int]$matches[1]
            $tier = Get-Tier $h
        } else {
            $tier = "smol"
        }
        if (-not $tierGroups.ContainsKey($tier)) { $tierGroups[$tier] = [System.Collections.Generic.List[string]]::new() }
        $tierGroups[$tier].Add($f.FullName)
    }

    foreach ($tier in $tierGroups.Keys) {
        $outDir = Join-Path $OutRoot $species
        if (-not (Test-Path $outDir)) { New-Item -ItemType Directory -Path $outDir -Force | Out-Null }

        $n = 1
        foreach ($srcPath in $tierGroups[$tier]) {
            $destName = "custom-$species-$tier-$n.iob"
            $destPath = Join-Path $outDir $destName
            Copy-Item $srcPath $destPath -Force
            $n++
            $totalCopied++
        }
    }

    # Summary per species
    $summary = $tierGroups.GetEnumerator() | Sort-Object Key | ForEach-Object { "$($_.Key)=$($_.Value.Count)" }
    Write-Output "[$species]  $($summary -join '  ')"
}

Write-Output ""
Write-Output "Done. $totalCopied files written to $OutRoot"
