# One-shot patch: inject leaf:* biomes into Dungeons & Taverns (nova_structures) biome
# collection tags so DnT structures generate inside our custom biomes. collections/land
# aggregates the granular sub-collections below, so editing the leaf-level collections
# bubbles up to all 'land' based structures (shrines, ruins, bunkers, taverns, etc.).
$ErrorActionPreference = 'Stop'
$base = 'C:\Users\lxgol\IdeaProjects\LeafsServerBaseline\datapacks\dungeons-and-taverns\data\nova_structures'
$tagBase = "$base\tags\worldgen\biome"
$structBase = "$base\worldgen\structure"

$tagMap = @{
  "collections\meadows.json"                        = @('leaf:alpine_meadow')
  "collections\floral.json"                         = @('leaf:lavender_fields','leaf:alpine_meadow')
  "collections\birch_forests.json"                  = @('leaf:maple_forest')
  "collections\cherry_forests.json"                 = @('leaf:jacaranda_grove')
  "collections\spooky_forests.json"                 = @('leaf:dark_forest_core','leaf:dark_forest_body','leaf:dark_forest_edge')
  "collections\any_forests.json"                    = @('leaf:forest_core','leaf:forest_body','leaf:forest_edge','leaf:maple_forest','leaf:redwood_grove','leaf:jacaranda_grove')
  "collections\regular_forests.json"                = @('leaf:forest_core','leaf:forest_body','leaf:forest_edge')
  "collections\oak_structure_biomes.json"           = @('leaf:forest_core','leaf:forest_body','leaf:forest_edge')
  "collections\taiga.json"                          = @('leaf:taiga_core','leaf:taiga_body','leaf:taiga_edge')
  "collections\any_taiga.json"                      = @('leaf:taiga_core','leaf:taiga_body','leaf:taiga_edge','leaf:redwood_grove','leaf:boreal_shield')
  "collections\giant_taigas.json"                   = @('leaf:redwood_grove')
  "collections\snowy_structures.json"               = @('leaf:boreal_shield')
  "collections\snowy_structures_plus_cave.json"     = @('leaf:boreal_shield')
  "collections\snowy_forests.json"                  = @('leaf:boreal_shield')
  "collections\swamps.json"                         = @('leaf:bayou')
  "collections\swamps_village.json"                 = @('leaf:bayou')
  "collections\mangroves.json"                      = @('leaf:bayou')
  "collections\stronghold_related_ruins_biomes.json"= @('leaf:dark_forest_core','leaf:ashen_plains','leaf:boreal_shield','leaf:redwood_grove','leaf:bayou')
  "collections\badlands.json"                       = @('leaf:ashen_plains')
  "collections\mountains.json"                      = @('leaf:volcanic_mountain','leaf:volcanic_hot_springs')
  "collections\beaches.json"                        = @('leaf:glass_beach')
  "illager_hideout.json"                            = @('leaf:ashen_plains','leaf:boreal_shield','leaf:alpine_meadow','leaf:redwood_grove')
}

foreach($rel in $tagMap.Keys){
  $path = Join-Path $tagBase $rel
  if(-not (Test-Path $path)){ Write-Output "MISSING TAG: $rel"; continue }
  $j = Get-Content $path -Raw | ConvertFrom-Json
  $vals = @($j.values)
  $existing = $vals | ForEach-Object { if($_ -is [string]){ $_ } elseif($_.id){ $_.id } }
  $toAdd = $tagMap[$rel] | Where-Object { $existing -notcontains $_ }
  $j.values = @($vals + $toAdd)
  $out = $j | ConvertTo-Json -Depth 50
  [System.IO.File]::WriteAllText($path, $out, (New-Object System.Text.UTF8Encoding($false)))
  Write-Output ("tag {0}  += {1}" -f $rel, ($toAdd -join ','))
}

# Hardcoded single-biome structures: append leaf biomes directly to their biomes field.
$structMap = @{
  "mangrove_witch_hut.json"      = @('leaf:bayou')
  "firewatch_tower_mangrove.json"= @('leaf:bayou')
  "firewatch_tower_forest.json"  = @('leaf:forest_core','leaf:forest_body','leaf:forest_edge')
}
foreach($rel in $structMap.Keys){
  $path = Join-Path $structBase $rel
  if(-not (Test-Path $path)){ Write-Output "MISSING STRUCT: $rel"; continue }
  $j = Get-Content $path -Raw | ConvertFrom-Json
  $orig = @($j.biomes)
  $merged = @($orig + $structMap[$rel] | Select-Object -Unique)
  $j.biomes = $merged
  $out = $j | ConvertTo-Json -Depth 50
  [System.IO.File]::WriteAllText($path, $out, (New-Object System.Text.UTF8Encoding($false)))
  Write-Output ("struct {0}  biomes -> {1}" -f $rel, ($merged -join ','))
}
Write-Output 'Dungeons & Taverns patch complete.'
