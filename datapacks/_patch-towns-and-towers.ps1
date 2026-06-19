# One-shot patch: dedup fix for Towns & Towers.
#
# T&T ships data/minecraft/tags/worldgen/biome/has_structure/pillager_outpost.json with
# "replace": true and a single value (minecraft:plains). Because that tag is the merge
# point every pillager-outpost provider feeds into (vanilla, Structory: Towers, etc.), a
# replacing tag WIPES all the other contributors at load time - so only T&T's own list
# survives and runtime variety/coexistence is lost. Flipping it to "replace": false makes
# the tag MERGE instead, which is the whole-series goal (keep every option, select at
# runtime). Idempotent: re-running is a no-op once replace is already false.
$ErrorActionPreference = 'Stop'
$repo = 'C:\Users\lxgol\IdeaProjects\LeafsServerBaseline\datapacks'
$path = "$repo\towns-and-towers\data\minecraft\tags\worldgen\biome\has_structure\pillager_outpost.json"

if(-not (Test-Path $path)){ Write-Output "MISSING TAG: $path"; return }
$j = Get-Content $path -Raw | ConvertFrom-Json
if($j.replace -eq $true){
  $j.replace = $false
  $out = $j | ConvertTo-Json -Depth 50
  [System.IO.File]::WriteAllText($path, $out, (New-Object System.Text.UTF8Encoding($false)))
  Write-Output "tag pillager_outpost.json  replace: true -> false (now merges)"
} else {
  Write-Output "tag pillager_outpost.json  replace already false (no-op)"
}
Write-Output 'Towns & Towers patch complete.'
