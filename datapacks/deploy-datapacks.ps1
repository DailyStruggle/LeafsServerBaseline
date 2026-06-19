param(
    [string]$ServerBase = 'C:\GameServers\Minecraft\testServer\RTP-Paper\26.1',
    [string]$LevelName,
    [string[]]$Only,
    [switch]$ResetWorld,
    [switch]$NoBackup,
    [switch]$NoPatch
)

# Deploys the repo's vanilla datapacks to the test server's world datapacks folder.
# This is the post-Iris worldgen model (see ADR-005): worldgen now ships as
# vanilla datapacks (datapacks/leaf-worldgen, datapacks/leaf-skilltree) copied
# into <ServerBase>\<LevelName>\datapacks\<pack>.
#
# Like the other deploy scripts, this ONLY stages/copies files. It does NOT boot
# the server (server boots are user-triggered) - after deploying, /reload (or a
# server restart for worldgen/dimension changes) is needed for them to take
# effect. -Only <name[,name]> restricts the deploy to specific pack folders.
#
# -ResetWorld wipes the target world folder so the server regenerates it FROM
# SCRATCH on the next (user-triggered) boot - this is how stale chunk data is
# cleared so worldgen/dimension changes actually show up everywhere instead of
# only in newly generated chunks. The wiped world is first moved aside to a
# timestamped backup (world_backup_<stamp>) unless -NoBackup is given. This is a
# DESTRUCTIVE operation and is OFF by default; the datapacks are re-deployed into
# the freshly recreated world folder afterwards.

# Resolve the primary level name (the world whose datapacks/ folder receives the
# packs). Prefer an explicit -LevelName, else read level-name from
# server.properties, else fall back to 'world'.
if (-not $LevelName) {
    $propsFile = "$ServerBase\server.properties"
    if (Test-Path $propsFile) {
        $line = Select-String -Path $propsFile -Pattern '^\s*level-name\s*=\s*(.+)$' -ErrorAction SilentlyContinue | Select-Object -First 1
        if ($line) { $LevelName = $line.Matches[0].Groups[1].Value.Trim() }
    }
    if (-not $LevelName) { $LevelName = 'world' }
}

$repo         = 'C:\Users\lxgol\IdeaProjects\LeafsServerBaseline'
$datapacksSrc = "$repo\datapacks"
$dest         = "$ServerBase\$LevelName\datapacks"

$worldDir = "$ServerBase\$LevelName"

if ($ResetWorld) {
    if (Test-Path $worldDir -PathType Container) {
        if ($NoBackup) {
            Write-Output "=== Resetting world (no backup): $worldDir ==="
            Remove-Item $worldDir -Recurse -Force
        } else {
            $stamp  = Get-Date -Format 'yyyyMMdd-HHmmss'
            $backup = "$ServerBase\${LevelName}_backup_$stamp"
            Write-Output "=== Resetting world: moving $worldDir -> $backup ==="
            Move-Item -Path $worldDir -Destination $backup -Force
        }
    } else {
        Write-Output "=== Reset requested but world folder does not exist yet: $worldDir (will be created) ==="
    }
    # Recreate an empty world folder so datapacks can be staged before boot; the
    # server fills in level.dat/region/etc. from scratch on the next boot.
    New-Item -ItemType Directory -Path $worldDir -Force | Out-Null
}

if (-not (Test-Path $worldDir -PathType Container)) {
    throw "Server world folder not found: $worldDir (check -ServerBase / -LevelName)."
}
New-Item -ItemType Directory -Path $dest -Force | Out-Null

# Run the integration/dedup patches against the repo datapack SOURCE before copying, so
# the deployed packs carry our leaf:* biome wiring and the dedup fixes. Idempotent; skip
# with -NoPatch. (Category-tag wiring lives statically in leaf-worldgen and needs no patch.)
if (-not $NoPatch) {
    $patchAll = Join-Path $datapacksSrc 'patch-all.ps1'
    if (Test-Path $patchAll) {
        Write-Output '=== Running datapack integration/dedup patches ==='
        & $patchAll
    }
}

Write-Output "=== Deploying datapacks to $dest ==="

# A datapack source folder is any direct subfolder of datapacks/ that has a
# pack.mcmeta (skips loose scripts/docs sitting in datapacks/).
$packs = Get-ChildItem $datapacksSrc -Directory -ErrorAction SilentlyContinue |
    Where-Object { Test-Path (Join-Path $_.FullName 'pack.mcmeta') }

if ($Only) {
    $packs = $packs | Where-Object { $Only -contains $_.Name }
}

if (-not $packs) {
    Write-Output '  No datapacks found (expected subfolders of datapacks/ containing pack.mcmeta).'
    return
}

foreach ($pack in $packs) {
    $packDest = Join-Path $dest $pack.Name
    # Wipe the destination pack first so files removed from the repo do NOT linger
    # as a stale snapshot (Copy-Item -Force overwrites/adds but never deletes
    # extras), mirroring the wipe-then-copy discipline of the old Iris deploy.
    if (Test-Path $packDest) { Remove-Item $packDest -Recurse -Force }
    New-Item -ItemType Directory -Path $packDest -Force | Out-Null
    Copy-Item "$($pack.FullName)\*" $packDest -Recurse -Force
    $fileCount = (Get-ChildItem $packDest -Recurse -File -ErrorAction SilentlyContinue).Count
    Write-Output "  deployed: $($pack.Name) ($fileCount files)"
}

Write-Output 'Datapack deploy complete.'
if ($ResetWorld) {
    Write-Output "World was reset; boot the server to generate it FROM SCRATCH"
    Write-Output "with these datapacks active (server boots are user-triggered)."
} else {
    Write-Output "Reminder: run /reload (data-only changes) or restart the server"
    Write-Output "(worldgen/dimension changes regenerate only in newly generated chunks)."
    Write-Output "Use -ResetWorld to wipe the world and regenerate from scratch."
}
