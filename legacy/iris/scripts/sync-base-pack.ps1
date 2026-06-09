param(
    [string]$ServerBase = 'C:\GameServers\Minecraft\testServer\RTP-Folia\26.1'
)

# Copies the server-downloaded vanilla Iris overworld pack into iris/pack-base/.
# Run this once after a fresh server startup (with no overworld pack present so
# Iris auto-downloads the correct version), then commit the result.
#
# WARNING: Never run this while the server is running.

$repo    = 'C:\Users\lxgol\IdeaProjects\LeafsServerBaseline'
$src     = "$ServerBase\plugins\Iris\packs\overworld"
$dst     = "$repo\iris\pack-base"

if (-not (Test-Path $src)) {
    Write-Error "Source pack not found at $src - start the server once to let Iris download it."
    exit 1
}

Write-Output "Syncing base pack from $src ..."

# Wipe and recreate pack-base. The whole pack-base/ tree is git-ignored, so the
# binary objects/ folder is NOT committed - but it MUST still be synced locally.
# The base Iris pack supplies thousands of vanilla objects (clutter/*, trees/*,
# structures/*) that base biomes reference. If objects/ is omitted here, the
# deploy's staging is incomplete and the deploy's wipe-then-copy step destroys
# the server-downloaded base objects, producing mass "Couldn't find Object"
# generation errors. Always include objects/.
if (Test-Path $dst) { Remove-Item $dst -Recurse -Force }
New-Item -ItemType Directory -Path $dst -Force | Out-Null

Get-ChildItem $src -Directory | ForEach-Object {
    Copy-Item $_.FullName "$dst\$($_.Name)" -Recurse -Force
}
Get-ChildItem $src -File | ForEach-Object {
    Copy-Item $_.FullName $dst -Force
}

# Strip BOM from all JSON files copied in
Get-ChildItem $dst -Recurse -Filter '*.json' | ForEach-Object {
    $bytes = [System.IO.File]::ReadAllBytes($_.FullName)
    if ($bytes.Length -ge 3 -and $bytes[0] -eq 0xEF -and $bytes[1] -eq 0xBB -and $bytes[2] -eq 0xBF) {
        [System.IO.File]::WriteAllBytes($_.FullName, $bytes[3..($bytes.Length-1)])
        Write-Output "  BOM stripped: $($_.Name)"
    }
}

$count = (Get-ChildItem $dst -Recurse -File).Count
$iob   = (Get-ChildItem "$dst\objects" -Recurse -File -Filter '*.iob' -ErrorAction SilentlyContinue).Count
Write-Output "Done. $count files in iris/pack-base ($iob base .iob objects included)."
Write-Output "Review the diff, then commit iris/pack-base to lock in the vanilla baseline."
