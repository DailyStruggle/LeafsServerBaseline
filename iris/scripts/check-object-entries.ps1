$root = "C:\GameServers\Minecraft\testServer\RTP-Paper\1.21.11\plugins\Iris\packs\overworld\biomes"

# Check all our custom biome files for any object entry missing "place"
$customFiles = @(
    "frozen\boreal-shield.json",
    "frozen\frostpeak.json",
    "savanna\ashen-plains.json",
    "mesa\bryce-spires.json",
    "mesa\cinnabar-mesa.json",
    "mountain\floating-islands.json",
    "mountain\ashcrown.json",
    "ocean\mirage-isles.json",
    "ocean\embertide.json",
    "tropical\cinderfall.json",
    "tundra\maple-forest.json"
)

foreach ($rel in $customFiles) {
    $p = "$root\$rel"
    if (-not (Test-Path $p)) { Write-Output "MISSING: $rel"; continue }
    $j = Get-Content $p -Raw | ConvertFrom-Json
    if (-not $j.objects) { Write-Output "NO OBJECTS: $rel"; continue }
    $i = 0
    foreach ($obj in $j.objects) {
        $i++
        if (-not $obj.place) { Write-Output "NULL PLACE at obj[$i]: $rel" }
        if (-not $obj.chance) { Write-Output "NULL CHANCE at obj[$i]: $rel" }
        # Check place entries exist as files
        $places = if ($obj.place -is [array]) { $obj.place } else { @($obj.place) }
        foreach ($pl in $places) {
            $iobPath = "$root\..\objects\$pl.iob"
            if (-not (Test-Path $iobPath)) {
                Write-Output "MISSING IOB: $pl  (in $rel obj[$i])"
            }
        }
    }
    Write-Output "OK: $rel ($i objects)"
}
