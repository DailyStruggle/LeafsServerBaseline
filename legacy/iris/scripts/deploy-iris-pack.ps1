param(
    [string]$ServerBase = 'C:\GameServers\Minecraft\testServer\RTP-Paper\26.1',
    [string]$LevelName,
    [switch]$SkipCacheClear
)

# Resolve the primary level name (used to locate Paper's dimension-folder chunk
# data at <ServerBase>\<LevelName>\dimensions\...). Prefer an explicit -LevelName,
# else read level-name from server.properties, else fall back to 'world'.
if (-not $LevelName) {
    $propsFile = "$ServerBase\server.properties"
    if (Test-Path $propsFile) {
        $line = Select-String -Path $propsFile -Pattern '^\s*level-name\s*=\s*(.+)$' -ErrorAction SilentlyContinue | Select-Object -First 1
        if ($line) { $LevelName = $line.Matches[0].Groups[1].Value.Trim() }
    }
    if (-not $LevelName) { $LevelName = 'world' }
}

$repo        = 'C:\Users\lxgol\IdeaProjects\LeafsServerBaseline'
$irisDir     = "$repo\iris"
$basePackSrc = "$irisDir\pack-base"
$overlaySrc  = "$irisDir\pack-overlay"
$iobSrc      = "$irisDir\output"
$staging     = "$irisDir\staging"
$patchesDir  = "$irisDir\pack-patches"        # private "special structures" overlay
$patchAssets = "$patchesDir\assets"            # additive files (objects, jigsaw-*)
$patchSpecs  = "$patchesDir\patches"           # *.patch.json amend files

# ── 1. Build staging ────────────────────────────────────────────────────────
Write-Output '=== Building staging ==='

# Wipe and recreate staging
if (Test-Path $staging) { Remove-Item $staging -Recurse -Force }
New-Item -ItemType Directory -Path $staging -Force | Out-Null

# Layer 1: base pack (vanilla Iris pack downloaded by server)
if (Test-Path $basePackSrc) {
    Copy-Item "$basePackSrc\*" $staging -Recurse -Force
    Write-Output '  base pack copied'
} else {
    Write-Output '  WARNING: iris/pack-base is empty - run sync-base-pack.ps1 first'
}

# Layer 2: our overlay (custom biomes, regions, dimensions, etc.)
if (Test-Path $overlaySrc) {
    Copy-Item "$overlaySrc\*" $staging -Recurse -Force
    Write-Output '  overlay copied'
}

# Layer 3: custom .iob files from iris/output into staging/objects/trees/
# Each output subfolder must have a matching entry in $iobFolderMap to specify
# the correct objects/trees/<dest> subfolder. Unmapped folders default to their own name.
$iobFolderMap = @{
    'spiral-crown-forest' = 'darkoak'
}
Get-ChildItem $iobSrc -Recurse -Filter '*.iob' -ErrorAction SilentlyContinue | ForEach-Object {
    $rel    = $_.DirectoryName.Substring($iobSrc.Length).TrimStart('\')
    $mapped = if ($iobFolderMap.ContainsKey($rel)) { $iobFolderMap[$rel] } else { $rel }
    $target = "$staging\objects\trees\$mapped"
    New-Item -ItemType Directory -Path $target -Force | Out-Null
    Copy-Item $_.FullName $target -Force
}
$iobCount = (Get-ChildItem "$staging\objects" -Recurse -Filter '*.iob' -ErrorAction SilentlyContinue).Count
Write-Output "  custom .iob files staged: $iobCount"

# Layer 2.5: private "special structures" additive overlay (NOT shipped to
# consumers - see iris/pack-patches/README.md and ADR-004). First merge the
# purely-additive assets (derived jigsaw objects/pieces/pools/structures), then
# apply *.patch.json files that AMEND staged biome JSON (add/remove syntax)
# instead of whole-file overriding them via pack-overlay.
if (Test-Path $patchAssets) {
    Copy-Item "$patchAssets\*" $staging -Recurse -Force
    Write-Output '  special-structure assets merged'
}
if (Test-Path $patchSpecs) {
    Write-Output '  applying pack patches...'
    python "$irisDir\scripts\apply-pack-patches.py" --staging $staging --patches $patchSpecs
    if ($LASTEXITCODE -ne 0) { throw "apply-pack-patches.py failed (exit $LASTEXITCODE)" }
} else {
    Write-Output '  no pack-patches/patches dir - skipping patch layer'
}

# Strip UTF-8 BOM from all staged JSON files (Iris region/object loaders reject a
# leading BOM with "A JSONObject text must begin with '{'"). Editors/PowerShell
# can reintroduce a BOM when files are rewritten, so normalize here before deploy.
Get-ChildItem $staging -Recurse -Filter '*.json' | ForEach-Object {
    $bytes = [System.IO.File]::ReadAllBytes($_.FullName)
    if ($bytes.Length -ge 3 -and $bytes[0] -eq 0xEF -and $bytes[1] -eq 0xBB -and $bytes[2] -eq 0xBF) {
        [System.IO.File]::WriteAllBytes($_.FullName, $bytes[3..($bytes.Length-1)])
        Write-Output "  BOM stripped: $($_.Name)"
    }
}

Write-Output '=== Staging ready ==='

# ── 1.5 Safety guard: never wipe-deploy an incomplete pack ───────────────────
# The deploy below WIPES each destination before copying staging. That is only
# safe if staging is the COMPLETE pack. The base Iris pack supplies thousands of
# vanilla objects (clutter/*, trees/*, structures/*) via iris/pack-base/objects
# (git-ignored, populated by sync-base-pack.ps1). If pack-base/objects is missing
# the wipe would destroy the server-downloaded base objects and cause mass
# "Couldn't find Object" errors, so abort instead.
$baseObjCount = (Get-ChildItem "$basePackSrc\objects" -Recurse -Filter '*.iob' -ErrorAction SilentlyContinue).Count
if (Test-Path $basePackSrc -PathType Container) {
    if ($baseObjCount -lt 1000) {
        throw "Refusing to deploy: iris/pack-base/objects has only $baseObjCount .iob (expected thousands). Run sync-base-pack.ps1 to repopulate base objects before deploying, or the wipe-deploy will destroy the server's base objects."
    }
}
$stagedObjCount = (Get-ChildItem "$staging\objects" -Recurse -Filter '*.iob' -ErrorAction SilentlyContinue).Count
Write-Output "  staging total .iob objects (base + custom): $stagedObjCount"

# ── 2. Deploy staging to server ─────────────────────────────────────────────
$destinations = @(
    "$ServerBase\test\iris\pack",
    "$ServerBase\plugins\Iris\packs\overworld"
)

foreach ($dest in $destinations) {
    Write-Output "Deploying to $dest ..."
    # Wipe the destination first so objects/files removed from staging do NOT
    # linger as a stale snapshot. Copy-Item -Force only overwrites/added files;
    # it never deletes extras, which is how the world's frozen pack
    # ($ServerBase\test\iris\pack) drifted to a smaller, stale object set and
    # produced "Couldn't find Object" errors at generation time.
    if (Test-Path $dest) { Remove-Item $dest -Recurse -Force }
    New-Item -ItemType Directory -Path $dest -Force | Out-Null
    Copy-Item "$staging\*" $dest -Recurse -Force
    Write-Output '  Done.'
}

# ── 3. Clear caches ──────────────────────────────────────────────────────────
if (-not $SkipCacheClear) {
    Write-Output 'Clearing caches...'

    # Only the Iris-generated world(s) should have their chunk data wiped - never
    # blow away unrelated vanilla worlds/dimensions. Discover which Bukkit worlds
    # use an Iris generator from bukkit.yml (e.g. `test: { generator: Iris:overworld }`).
    $irisWorlds = @()
    $bukkitYml = "$ServerBase\bukkit.yml"
    if (Test-Path $bukkitYml) {
        $yml = Get-Content $bukkitYml
        $curWorld = $null
        foreach ($l in $yml) {
            if ($l -match '^\s{4}([^\s#][^:]*):\s*$') { $curWorld = $matches[1].Trim() }
            elseif ($curWorld -and $l -match '^\s{6,}generator:\s*Iris\b') { $irisWorlds += $curWorld }
        }
    }
    if (-not $irisWorlds) { $irisWorlds = @('test') }   # fallback to the known Iris world
    $irisWorlds = $irisWorlds | Select-Object -Unique
    Write-Output ("  Iris world(s): {0}" -f ($irisWorlds -join ', '))

    # Paper's newer "dimension folder" layout no longer stores a world's chunk
    # data flat under <world>\{region,entities,poi}. A Bukkit world named <w>
    # generated by Iris surfaces as the minecraft:<w> dimension under the primary
    # level folder, i.e. <ServerBase>\<LevelName>\dimensions\minecraft\<w>\*. We
    # clear both that new path and the legacy flat <ServerBase>\<w>\* path so a
    # deploy always wipes the Iris world's preexisting chunk data the way it used
    # to - and only for Iris worlds, leaving other dimensions untouched.
    $caches = @(
        "$ServerBase\plugins\Iris\cache",
        # Iris precompiles a per-dimension prefetch of jigsaw structures/pools/
        # pieces (*.ipfch). If left stale it pins the OLD structure set (e.g. the
        # pack would keep loading "17 prefetch jigsaw-structures" and ignore a
        # newly added one), so it MUST be cleared whenever the pack changes.
        "$ServerBase\plugins\Iris\prefetch"
    )
    foreach ($w in $irisWorlds) {
        $caches += @(
            # Iris per-world data (unchanged by the dimension-folder layout).
            "$ServerBase\$w\mantle",
            "$ServerBase\$w\iris\engine-data",
            "$ServerBase\$w\iris\cache",
            # Legacy flat-layout chunk data (pre dimension-folder Paper).
            "$ServerBase\$w\region",
            "$ServerBase\$w\entities",
            "$ServerBase\$w\poi",
            # New dimension-folder chunk data for the minecraft:<w> dimension.
            "$ServerBase\$LevelName\dimensions\minecraft\$w\region",
            "$ServerBase\$LevelName\dimensions\minecraft\$w\entities",
            "$ServerBase\$LevelName\dimensions\minecraft\$w\poi"
        )
    }
    foreach ($c in $caches) {
        if (Test-Path $c) {
            Remove-Item "$c\*" -Recurse -Force -ErrorAction SilentlyContinue
            $cnt = (Get-ChildItem $c -Recurse -File -ErrorAction SilentlyContinue).Count
            Write-Output "  $c -> $cnt files remaining"
        }
    }
}

Write-Output 'Deploy complete.'
