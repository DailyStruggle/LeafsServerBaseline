$packBiomes = "C:\GameServers\Minecraft\testServer\RTP-Folia\26.1\plugins\Iris\packs\overworld-custom\biomes"
$projectBiomes = "C:\Users\lxgol\IdeaProjects\LeafsServerBaseline\iris-biomes"

function Fix-PlaceArrays($path) {
    $j = Get-Content $path -Raw | ConvertFrom-Json
    if (-not $j.objects) { return $false }
    $changed = $false
    $newObjects = @()
    foreach ($obj in $j.objects) {
        if ($obj.place -is [array]) {
            # Expand each array entry into its own object entry
            foreach ($p in $obj.place) {
                $newObj = [PSCustomObject]@{ place = $p; chance = $obj.chance }
                $newObjects += $newObj
            }
            $changed = $true
        } else {
            $newObjects += $obj
        }
    }
    if ($changed) {
        $j.objects = $newObjects
        $json = $j | ConvertTo-Json -Depth 10
        [System.IO.File]::WriteAllText($path, $json, (New-Object System.Text.UTF8Encoding $false))
        return $true
    }
    return $false
}

# Fix in live pack
$targets = @("tundra\maple-forest.json", "mountain\floating-islands.json", "ocean\mirage-isles.json", "frozen\boreal-shield.json")
foreach ($rel in $targets) {
    $p = "$packBiomes\$rel"
    if (Test-Path $p) {
        if (Fix-PlaceArrays $p) { Write-Output "Fixed place arrays (live): $rel" }
        else { Write-Output "No arrays found (live): $rel" }
    }
}

# Fix in project source too
foreach ($rel in $targets) {
    $p = "$projectBiomes\$($rel -replace '\\','/')"
    $p2 = "$projectBiomes\$rel"
    $actual = if (Test-Path $p) { $p } elseif (Test-Path $p2) { $p2 } else { $null }
    if ($actual) {
        if (Fix-PlaceArrays $actual) { Write-Output "Fixed place arrays (project): $rel" }
    }
}

# Copy missing hot-springs biomes to live pack
$hotSprings = @(
    "frozen\hot-springs-frozen.json",
    "mountain\hot-springs-mountain.json",
    "tropical\hot-springs-tropical.json",
    "mesa\hot-springs-mesa.json",
    "ocean\hot-springs-ocean.json"
)
foreach ($rel in $hotSprings) {
    $src = "$projectBiomes\$rel"
    $dst = "$packBiomes\$rel"
    if (Test-Path $src) {
        # Strip BOM on copy
        $bytes = [System.IO.File]::ReadAllBytes($src)
        $start = if ($bytes[0] -eq 0xEF -and $bytes[1] -eq 0xBB -and $bytes[2] -eq 0xBF) { 3 } else { 0 }
        [System.IO.File]::WriteAllBytes($dst, $bytes[$start..($bytes.Length-1)])
        Write-Output "Copied: $rel"
    } else {
        Write-Output "MISSING in project: $rel"
    }
}

Write-Output "=== Done ==="
