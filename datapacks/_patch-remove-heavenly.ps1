# One-shot patch: stop the airborne "heavenly" When Dungeons Arise structures from
# generating (heavenly_challenger / heavenly_conqueror / heavenly_rider). They are
# floating sky structures the maintainer considers unseemly for this world.
#
# A structure only generates if some worldgen/structure_set references it, so the
# surgical, safe fix is to drop the heavenly entries from dungeons-arise's
# major_structures.json (their structure defs, pools, loot and advancements are
# left intact -> no dangling references). Idempotent: re-running is a no-op once
# the entries are gone.
$ErrorActionPreference = 'Stop'
$repo = 'C:\Users\lxgol\IdeaProjects\LeafsServerBaseline\datapacks'
$set = "$repo\dungeons-arise\data\dungeons_arise\worldgen\structure_set\major_structures.json"

if(-not (Test-Path $set)){ Write-Output "MISSING structure_set: $set"; return }

$j = Get-Content $set -Raw | ConvertFrom-Json
$before = @($j.structures).Count
$kept = @($j.structures | Where-Object { $_.structure -notmatch 'heavenly' })
$removed = $before - $kept.Count
$j.structures = $kept

$out = $j | ConvertTo-Json -Depth 50
[System.IO.File]::WriteAllText($set, $out, (New-Object System.Text.UTF8Encoding($false)))
Write-Output ("remove-heavenly: dropped {0} heavenly entry(ies) from major_structures.json ({1} -> {2})" -f $removed, $before, $kept.Count)
Write-Output 'Remove-heavenly patch complete.'
