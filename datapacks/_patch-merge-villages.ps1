# One-shot patch: collapse every pack's independent village placement into ONE shared set.
#
# By default each village pack ships its own worldgen/structure_set with its own salt, i.e.
# its own placement grid, so the game rolls each pack independently -> multiple village
# generators run side by side (overlap possible) instead of "one village type chosen per
# location". To get a single weighted selection per village slot, leaf-worldgen overrides
# vanilla minecraft:villages (data/minecraft/worldgen/structure_set/villages.json) to list
# ALL village structures (vanilla + Towns & Towers + Dungeons & Taverns) under one grid.
#
# This patch removes the packs' OWN village structure_set files so those structures only
# place through the merged minecraft:villages set (a structure generates only if some
# structure_set references it; the structures/template_pools themselves are untouched).
# Idempotent: a missing file is a no-op.
#
# NOTE: CTOV villages are intentionally NOT merged - they use "type": "lithostitched:jigsaw"
# and place via Lithostitched worldgen modifiers, both of which require the Lithostitched
# mod and are inert on a vanilla/Paper server, so they cannot join the merged set anyway.
$ErrorActionPreference = 'Stop'
$repo = 'C:\Users\lxgol\IdeaProjects\LeafsServerBaseline\datapacks'

$sets = @(
  "towns-and-towers\data\towns_and_towers\worldgen\structure_set\towns.json"
  "dungeons-and-taverns\data\nova_structures\worldgen\structure_set\villages_birch.json"
  "dungeons-and-taverns\data\nova_structures\worldgen\structure_set\villages_jungle.json"
  "dungeons-and-taverns\data\nova_structures\worldgen\structure_set\villages_swamp.json"
)

foreach($rel in $sets){
  $path = Join-Path $repo $rel
  if(Test-Path $path){
    Remove-Item $path -Force
    Write-Output "removed independent village set: $rel"
  } else {
    Write-Output "already merged (no-op): $rel"
  }
}
Write-Output 'Village-merge patch complete.'
