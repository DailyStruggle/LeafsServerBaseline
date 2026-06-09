$root = "C:\GameServers\Minecraft\testServer\RTP-Folia\26.1\plugins\Iris\packs\overworld\biomes"
$objRoot = "C:\GameServers\Minecraft\testServer\RTP-Folia\26.1\plugins\Iris\packs\overworld\objects"

function Test-IobExists($place) {
    $path = Join-Path $objRoot "$place.iob"
    return Test-Path $path
}

$customFiles = @(
    "frozen\boreal-shield.json",
    "frozen\frostpeak.json",
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

    $newObjects = @()
    foreach ($obj in $j.objects) {
        $places = if ($obj.place -is [array]) { $obj.place } else { @($obj.place) }
        $validPlaces = $places | Where-Object { Test-IobExists $_ }
        if ($validPlaces.Count -eq 0) {
            Write-Output "DROPPED entry (no valid places): $rel"
            continue
        }
        if ($validPlaces.Count -ne $places.Count) {
            $removed = $places | Where-Object { -not (Test-IobExists $_) }
            Write-Output "TRIMMED $($removed.Count) missing refs from entry in $rel"
            $obj.place = $validPlaces
        }
        $newObjects += $obj
    }

    $j.objects = $newObjects
    $j | ConvertTo-Json -Depth 10 | Set-Content $p -Encoding UTF8
    Write-Output "Saved: $rel ($($newObjects.Count) objects)"
}
