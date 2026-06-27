# One-shot patch: replace farmable plain spawners in the third-party structure
# packs with non-farmable vanilla TRIAL spawners, so the structures still ambush
# the player with mobs (and eject earned loot) but can no longer be farmed.
#
# The heavy lifting (binary structure-NBT rewrite) lives in the Python tool
# tools/despawnerize/trial_spawnerize.py; this wrapper just locates a working
# Python interpreter and runs it against the repo's datapacks. The tool is
# idempotent (once a spawner is converted no plain mob_spawner remains) and skips
# "dungeon"/"mineshaft"/"ancient"-named structures plus non-living trap spawners.
$ErrorActionPreference = 'Stop'
$here = Split-Path -Parent $MyInvocation.MyCommand.Path
$repo = Split-Path -Parent $here
$tool = Join-Path $repo 'tools\despawnerize\trial_spawnerize.py'

if(-not (Test-Path $tool)){ Write-Output "MISSING tool: $tool"; return }

# Find a usable Python (the bare 'python'/'py' PATH stubs are often broken Windows
# Store aliases, so prefer an explicit interpreter, then fall back to PATH).
$candidates = @(
  $env:JUNIE_PYTHON,
  'C:\Users\lxgol\AppData\Local\Programs\Python\Python313\python.exe',
  'python',
  'py'
) | Where-Object { $_ }

$py = $null
foreach($c in $candidates){
  try {
    $v = & $c --version 2>&1
    if($LASTEXITCODE -eq 0){ $py = $c; break }
  } catch { }
}
if(-not $py){ Write-Output 'trial-spawners: no working Python interpreter found; skipping.'; return }

Write-Output "trial-spawners: using interpreter '$py'"
& $py $tool
if($LASTEXITCODE -ne 0){ throw "trial_spawnerize.py exited with code $LASTEXITCODE" }
Write-Output 'Trial-spawners patch complete.'
