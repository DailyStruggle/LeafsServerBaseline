# Runs every per-pack integration/dedup patch (datapacks/_patch-*.ps1) in one shot.
#
# These patches splice our leaf:* custom biomes into the third-party packs' biome tags
# (so their structures actually generate in our custom biomes) and resolve cross-pack
# dedup issues (e.g. a "replace": true tag that would wipe other contributors). They edit
# the repo's datapack SOURCE in place and are all idempotent, so this is safe to re-run and
# is invoked automatically by deploy-datapacks.ps1 before the copy step (unless -NoPatch).
#
# Packs wired purely through vanilla biome-category tags (#minecraft:is_forest, is_taiga,
# is_badlands, is_mountain, is_beach) do NOT need a patch script: leaf-worldgen adds the
# leaf:* biomes into those merging tags once (data/minecraft/tags/worldgen/biome/is_*.json),
# which automatically covers every category-driven pack (When Dungeons Arise, YUNG's,
# Explorify, ...). Only packs that key off their own collection tags (Dungeons & Taverns,
# Structory) or ship a clobbering tag (Towns & Towers) get a dedicated _patch-*.ps1.
$ErrorActionPreference = 'Stop'
$here = Split-Path -Parent $MyInvocation.MyCommand.Path

$patches = Get-ChildItem $here -File -Filter '_patch-*.ps1' | Sort-Object Name
if(-not $patches){ Write-Output 'No _patch-*.ps1 scripts found.'; return }

foreach($p in $patches){
  Write-Output "=== running $($p.Name) ==="
  & $p.FullName
}
Write-Output 'All datapack patches complete.'
