param(
    [string]$ServerBase = 'C:\GameServers\Minecraft\testServer\RTP-Folia\26.1',
    [string[]]$Only
)

# Deploys built Leaf plugin jars to the test server's plugins folder.
# Each plugin module produces build/libs/<Name>-<version>.jar (see each plugin's
# README "Build" section). This script copies those jars to $ServerBase\plugins.
# It does NOT build the plugins (run `gradle jar` / `mvn package` first) and it
# does NOT boot the server (server boots are user-triggered).

$repo       = 'C:\Users\lxgol\IdeaProjects\LeafsServerBaseline'
$pluginsSrc = "$repo\plugins"
$dest       = "$ServerBase\plugins"

if (-not (Test-Path $dest)) {
    throw "Server plugins folder not found: $dest"
}

Write-Output "=== Deploying plugin jars to $dest ==="

$jars = Get-ChildItem $pluginsSrc -Recurse -Filter '*.jar' -ErrorAction SilentlyContinue |
    Where-Object { $_.FullName -match '\\build\\libs\\' }

if ($Only) {
    $jars = $jars | Where-Object {
        $module = $_.FullName.Substring($pluginsSrc.Length).TrimStart('\').Split('\')[0]
        $Only -contains $module
    }
}

if (-not $jars) {
    Write-Output '  No built plugin jars found (run `gradle jar` / `mvn package` first).'
    return
}

foreach ($jar in $jars) {
    Copy-Item $jar.FullName $dest -Force
    Write-Output "  deployed: $($jar.Name)"
}

Write-Output 'Plugin deploy complete.'
