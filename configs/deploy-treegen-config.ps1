param(
    [string]$ServerBase = 'C:\GameServers\Minecraft\testServer\RTP-Paper\26.2'
)

# Deploys the source-controlled LeafTreeGen inputs (config.yml + species/*.json)
# to the server's plugin config folder. LeafTreeGen is an externalized plugin
# (https://modrinth.com/plugin/leaf-treegen); this repo only owns its CONFIG.
#
# Like the other deploy scripts this ONLY stages/copies files - it does NOT boot
# the server and does NOT reload the plugin. After deploying, run "/leaftree
# reload" (config/species changes) or restart the server (worldgen changes only
# affect newly generated chunks); both are user-triggered.

$repo = 'C:\Users\lxgol\IdeaProjects\LeafsServerBaseline'
$src  = "$repo\configs\LeafTreeGen"
$dest = "$ServerBase\plugins\LeafTreeGen"

if (-not (Test-Path "$src\config.yml")) {
    throw "Source config not found: $src\config.yml"
}

New-Item -ItemType Directory -Path "$dest\species" -Force | Out-Null

Write-Output "=== Deploying LeafTreeGen config to $dest ==="

Copy-Item "$src\config.yml" "$dest\config.yml" -Force
Write-Output "  config.yml deployed"

# Wipe-then-copy species so files removed from source control don't linger.
if (Test-Path "$dest\species") { Remove-Item "$dest\species\*.json" -Force -ErrorAction SilentlyContinue }
Copy-Item "$src\species\*.json" "$dest\species" -Force
$count = (Get-ChildItem "$dest\species" -Filter *.json -ErrorAction SilentlyContinue).Count
Write-Output "  species deployed: $count files"

Write-Output 'LeafTreeGen config deploy complete.'
Write-Output 'Reminder: run /leaftree reload (config/species) or restart for worldgen;'
Write-Output 'worldgen changes only affect newly generated chunks (server boots are user-triggered).'
