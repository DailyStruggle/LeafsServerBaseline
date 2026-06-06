$packRoot = "C:\GameServers\Minecraft\testServer\RTP-Paper\1.21.11\plugins\Iris\packs\overworld-custom"
$projectRoot = "C:\Users\lxgol\IdeaProjects\LeafsServerBaseline"

$missing = @()
$ourBiomes = Get-ChildItem "$projectRoot\iris-biomes" -Recurse -Filter "*.json" | Where-Object { $_.DirectoryName -notmatch "regions" }
foreach ($f in $ourBiomes) {
    $rel = $f.FullName.Replace("$projectRoot\iris-biomes\", "")
    $dest = Join-Path "$packRoot\biomes" $rel
    if (-not (Test-Path $dest)) {
        $missing += $rel
        Copy-Item $f.FullName $dest -Force
        Write-Output "COPIED: $rel"
    }
}
if ($missing.Count -eq 0) { Write-Output "All custom biomes already present in live pack" }

$ourRegions = Get-ChildItem "$projectRoot\iris-biomes\regions" -Filter "*.json"
foreach ($f in $ourRegions) {
    $dest = Join-Path "$packRoot\regions" $f.Name
    if (-not (Test-Path $dest)) {
        Copy-Item $f.FullName $dest -Force
        Write-Output "COPIED region: $($f.Name)"
    }
}
Write-Output "Sync complete"

# Summary
$biomeCount = (Get-ChildItem "$packRoot\biomes" -Recurse -Filter "*.json").Count
$regionCount = (Get-ChildItem "$packRoot\regions" -Filter "*.json").Count
$objectCount = (Get-ChildItem "$packRoot\objects" -Recurse -Filter "*.iob" -ErrorAction SilentlyContinue).Count
Write-Output "Live pack: $biomeCount biomes, $regionCount regions, $objectCount custom .iob objects"
