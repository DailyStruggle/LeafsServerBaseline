$base   = "C:\GameServers\Minecraft\testServer\RTP-Paper\1.21.11\plugins\Iris\packs\overworld"
$custom = "C:\GameServers\Minecraft\testServer\RTP-Paper\1.21.11\plugins\Iris\packs\overworld-custom"

$bBiomes  = (Get-ChildItem "$base\biomes"   -Recurse -Filter "*.json").Count
$cBiomes  = (Get-ChildItem "$custom\biomes" -Recurse -Filter "*.json").Count
$bRegions = (Get-ChildItem "$base\regions"  -Filter "*.json" -ErrorAction SilentlyContinue).Count
$cRegions = (Get-ChildItem "$custom\regions" -Filter "*.json").Count
$bIobs    = (Get-ChildItem "$base\objects"   -Recurse -Filter "*.iob").Count
$cIobs    = (Get-ChildItem "$custom\objects" -Recurse -Filter "*.iob").Count

Write-Output "                  overworld   overworld-custom"
Write-Output "biomes            $bBiomes          $cBiomes"
Write-Output "regions           $bRegions           $cRegions"
Write-Output "objects (.iob)    $bIobs        $cIobs"
Write-Output ""

$bPrefix = "$base\biomes\"
$cPrefix = "$custom\biomes\"
$bNames = Get-ChildItem "$base\biomes"   -Recurse -Filter "*.json" | ForEach-Object { $_.FullName.Substring($bPrefix.Length) }
$cNames = Get-ChildItem "$custom\biomes" -Recurse -Filter "*.json" | ForEach-Object { $_.FullName.Substring($cPrefix.Length) }

Write-Output "Extra biomes in overworld-custom:"
$cNames | Where-Object { $bNames -notcontains $_ } | ForEach-Object { Write-Output "  + $_" }

Write-Output ""
Write-Output "Folders only in overworld-custom:"
$bDirs = (Get-ChildItem $base   -Directory | Select-Object -ExpandProperty Name)
$cDirs = (Get-ChildItem $custom -Directory | Select-Object -ExpandProperty Name)
$cDirs | Where-Object { $bDirs -notcontains $_ } | ForEach-Object { Write-Output "  + $_" }
