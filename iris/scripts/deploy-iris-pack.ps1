param(
    [string]$ServerBase = 'C:\GameServers\Minecraft\testServer\RTP-Paper\1.21.11',
    [switch]$SkipCacheClear
)

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

# ── 2. Deploy staging to server ─────────────────────────────────────────────
$destinations = @(
    "$ServerBase\test\iris\pack",
    "$ServerBase\plugins\Iris\packs\overworld"
)

foreach ($dest in $destinations) {
    Write-Output "Deploying to $dest ..."
    New-Item -ItemType Directory -Path $dest -Force | Out-Null
    Copy-Item "$staging\*" $dest -Recurse -Force
    Write-Output '  Done.'
}

# ── 3. Clear caches ──────────────────────────────────────────────────────────
if (-not $SkipCacheClear) {
    Write-Output 'Clearing caches...'
    $caches = @(
        "$ServerBase\test\region",
        "$ServerBase\test\entities",
        "$ServerBase\test\poi",
        "$ServerBase\test\mantle",
        "$ServerBase\test\iris\engine-data",
        "$ServerBase\test\iris\cache",
        "$ServerBase\plugins\Iris\cache",
        # Iris precompiles a per-dimension prefetch of jigsaw structures/pools/
        # pieces (*.ipfch). If left stale it pins the OLD structure set (e.g. the
        # pack would keep loading "17 prefetch jigsaw-structures" and ignore a
        # newly added one), so it MUST be cleared whenever the pack changes.
        "$ServerBase\plugins\Iris\prefetch"
    )
    foreach ($c in $caches) {
        if (Test-Path $c) {
            Remove-Item "$c\*" -Recurse -Force -ErrorAction SilentlyContinue
            $cnt = (Get-ChildItem $c -Recurse -File -ErrorAction SilentlyContinue).Count
            Write-Output "  $c -> $cnt files remaining"
        }
    }
}

Write-Output 'Deploy complete.'
