# One-shot patch: wire Structory + Structory: Towers onto our custom leaf:* biomes.
#
# A structure 'biomes' HolderSet in LIST form may only contain plain biome ids - NOT tag
# references (#...). So instead of editing each structure's 'biomes' (which reference tags),
# we append our leaf:* biome ids to the biome TAGS those structures point at (the same
# proven pattern used for Towns & Towers). Shared tags mean minor, benign theme spillover.
# The sole exception is swamp_ruin, whose 'biomes' is a single plain biome id, so it can be
# safely widened to a list of biome ids.
$ErrorActionPreference = 'Stop'
$repo = 'C:\Users\lxgol\IdeaProjects\LeafsServerBaseline\datapacks'

function Add-LeafToTag([string]$path, [string[]]$leaf){
  if(-not (Test-Path $path)){ Write-Output "MISSING TAG: $path"; return }
  $j = Get-Content $path -Raw | ConvertFrom-Json
  $vals = @($j.values)
  $existing = $vals | ForEach-Object { if($_ -is [string]){ $_ } elseif($_.id){ $_.id } }
  $toAdd = $leaf | Where-Object { $existing -notcontains $_ }
  $j.values = @($vals + $toAdd)
  $out = $j | ConvertTo-Json -Depth 50
  [System.IO.File]::WriteAllText($path, $out, (New-Object System.Text.UTF8Encoding($false)))
  Write-Output ("tag {0} += {1}" -f (Split-Path $path -Leaf), ($toAdd -join ','))
}

# Structory has_structure tags
$st = "$repo\structory\data\structory\tags\worldgen\biome\has_structure"
Add-LeafToTag "$st\boat.json"                    @('leaf:glass_beach')
Add-LeafToTag "$st\chapel.json"                  @('leaf:redwood_grove','leaf:taiga_body','leaf:boreal_shield','leaf:dark_forest_body','leaf:dark_forest_edge','leaf:maple_forest')
Add-LeafToTag "$st\firetower.json"               @('leaf:redwood_grove','leaf:boreal_shield','leaf:volcanic_mountain')
Add-LeafToTag "$st\old_manor.json"               @('leaf:dark_forest_core','leaf:dark_forest_body','leaf:bayou')
Add-LeafToTag "$st\northern_ruin.json"           @('leaf:boreal_shield','leaf:taiga_core','leaf:redwood_grove')
Add-LeafToTag "$st\outcast_villager_grassy.json" @('leaf:alpine_meadow','leaf:forest_body','leaf:forest_edge','leaf:lavender_fields','leaf:maple_forest')
Add-LeafToTag "$st\ruin_taiga.json"              @('leaf:taiga_body','leaf:taiga_core','leaf:redwood_grove','leaf:boreal_shield')

# Structory: Towers biome-category tags
$tt = "$repo\structory-towers\data\structory_towers\tags\worldgen\biome"
Add-LeafToTag "$tt\terralith_forests.json" @('leaf:lavender_fields','leaf:jacaranda_grove')
Add-LeafToTag "$tt\mangrove.json"          @('leaf:bayou','leaf:boreal_shield','leaf:volcanic_mountain')
Add-LeafToTag "$tt\taigas.json"            @('leaf:boreal_shield','leaf:taiga_core','leaf:redwood_grove')
Add-LeafToTag "$tt\dense_forests.json"     @('leaf:forest_core','leaf:maple_forest','leaf:redwood_grove')
Add-LeafToTag "$tt\oak_biomes.json"        @('leaf:alpine_meadow','leaf:maple_forest')
Add-LeafToTag "$tt\badlands.json"          @('leaf:lavender_fields','leaf:alpine_meadow')
Add-LeafToTag "$tt\mushroom.json"          @('leaf:bayou','leaf:dark_forest_core')
Add-LeafToTag "$tt\beaches.json"           @('leaf:glass_beach')
Add-LeafToTag "$tt\mountain_peaks.json"    @('leaf:volcanic_mountain','leaf:volcanic_hot_springs')
Add-LeafToTag "$tt\sparse_jungle.json"     @('leaf:volcanic_mountain','leaf:volcanic_hot_springs')
Add-LeafToTag "$tt\birch_biomes.json"      @('leaf:redwood_grove','leaf:taiga_body')

# pillager_lookout points at the vanilla #minecraft:has_structure/pillager_outpost tag.
# Add an additive (replace:false) minecraft tag in the Towers pack so it merges with vanilla.
$mcTagDir = "$repo\structory-towers\data\minecraft\tags\worldgen\biome\has_structure"
New-Item -ItemType Directory -Path $mcTagDir -Force | Out-Null
$mcTag = "$mcTagDir\pillager_outpost.json"
if(Test-Path $mcTag){ Add-LeafToTag $mcTag @('leaf:boreal_shield','leaf:ashen_plains') }
else {
  $obj = [ordered]@{ replace = $false; values = @('leaf:boreal_shield','leaf:ashen_plains') }
  [System.IO.File]::WriteAllText($mcTag, ($obj | ConvertTo-Json -Depth 50), (New-Object System.Text.UTF8Encoding($false)))
  Write-Output "created minecraft has_structure/pillager_outpost.json (+leaf:boreal_shield,leaf:ashen_plains)"
}

# swamp_ruin: biomes is a single plain biome id -> safe to widen to a biome-id list.
$sr = "$repo\structory\data\structory\worldgen\structure\swamp_ruin.json"
$j = Get-Content $sr -Raw | ConvertFrom-Json
$j.biomes = @('minecraft:mangrove_swamp','leaf:bayou')
[System.IO.File]::WriteAllText($sr, ($j | ConvertTo-Json -Depth 50), (New-Object System.Text.UTF8Encoding($false)))
Write-Output "struct swamp_ruin.json biomes -> minecraft:mangrove_swamp,leaf:bayou"

Write-Output 'Structory + Towers patch complete.'
