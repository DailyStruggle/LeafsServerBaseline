$root = "C:\GameServers\Minecraft\testServer\RTP-Folia\26.1\plugins\Iris\packs\overworld\biomes"
$bad = @()
$files = Get-ChildItem $root -Recurse -Filter "*.json"
foreach ($f in $files) {
    $raw = Get-Content $f.FullName -Raw
    if ($raw -match '"place"\s*:\s*\[') {
        $bad += $f.FullName.Replace($root + "\", "")
    }
}
Write-Output "Files with place as array: $($bad.Count)"
$bad | ForEach-Object { Write-Output "  $_" }

# Also check our new custom biome files specifically
Write-Output ""
Write-Output "=== Checking our custom biome JSONs ==="
$customBiomes = @(
    "$root\frozen\boreal-shield.json",
    "$root\frozen\frostpeak.json",
    "$root\savanna\ashen-plains.json",
    "$root\mesa\bryce-spires.json",
    "$root\tundra\maple-forest.json",
    "$root\mountain\floating-islands.json",
    "$root\ocean\mirage-isles.json"
)
foreach ($p in $customBiomes) {
    if (Test-Path $p) {
        $j = Get-Content $p -Raw | ConvertFrom-Json
        $name = Split-Path $p -Leaf
        Write-Output "$name : $($j.objects.Count) objects"
        if ($j.objects) {
            $j.objects | Select-Object -First 2 | ForEach-Object {
                $placeType = if ($_.place -is [array]) { "ARRAY[$($_.place.Count)]" } else { "string" }
                Write-Output "  place=$placeType  chance=$($_.chance)"
            }
        }
    } else {
        Write-Output "MISSING: $p"
    }
}
