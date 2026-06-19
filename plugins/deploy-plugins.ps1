param(
    [string]$ServerBase = 'C:\GameServers\Minecraft\testServer\RTP-Paper\26.1',
    [string[]]$Only,
    [switch]$NoBuild
)

# Deploys Leaf plugin jars to the test server's plugins folder.
# Each plugin module is a standalone Gradle project (its own settings.gradle, no
# wrapper) that produces build/libs/<Name>-<version>.jar (see each plugin's
# README "Build" section). This script BUILDS each plugin (gradle jar) and then
# copies the resulting jars to $ServerBase\plugins.
#
# -NoBuild skips the build step and just copies whatever jars are already in
#   each module's build/libs (the old copy-only behaviour).
# -Only <name[,name]> restricts the build/deploy to specific plugin module
#   folders (matched by their direct-subfolder name under plugins/).
#
# Like the other deploy scripts, this ONLY stages/copies files. It does NOT boot
# the server (server boots are user-triggered).

$repo       = 'C:\Users\lxgol\IdeaProjects\LeafsServerBaseline'
$pluginsSrc = "$repo\plugins"
$dest       = "$ServerBase\plugins"

if (-not (Test-Path $dest)) {
    throw "Server plugins folder not found: $dest"
}

# A plugin module is any direct subfolder of plugins/ that has a build.gradle
# (skips loose scripts/docs sitting in plugins/).
$modules = Get-ChildItem $pluginsSrc -Directory -ErrorAction SilentlyContinue |
    Where-Object { Test-Path (Join-Path $_.FullName 'build.gradle') }

if ($Only) {
    $modules = $modules | Where-Object { $Only -contains $_.Name }
}

if (-not $modules) {
    Write-Output '  No plugin modules found (expected subfolders of plugins/ containing build.gradle).'
    return
}

# Build each module unless -NoBuild was given. Each module is its own Gradle
# build, so invoke gradle from within the module folder.
if (-not $NoBuild) {
    $gradle = Get-Command gradle -ErrorAction SilentlyContinue
    if (-not $gradle) {
        throw "gradle not found on PATH (needed to build plugins). Install Gradle or pass -NoBuild to copy already-built jars."
    }

    Write-Output "=== Building plugin modules ==="
    foreach ($module in $modules) {
        Write-Output "  building: $($module.Name)"
        Push-Location $module.FullName
        try {
            & $gradle.Source jar --console=plain
            if ($LASTEXITCODE -ne 0) {
                throw "gradle build failed for $($module.Name) (exit $LASTEXITCODE)."
            }
        } finally {
            Pop-Location
        }
    }
}

Write-Output "=== Deploying plugin jars to $dest ==="

$jars = $modules | ForEach-Object {
    Get-ChildItem (Join-Path $_.FullName 'build\libs') -Filter '*.jar' -ErrorAction SilentlyContinue
}

if (-not $jars) {
    Write-Output '  No built plugin jars found (run with build enabled, or `gradle jar` first).'
    return
}

foreach ($jar in $jars) {
    Copy-Item $jar.FullName $dest -Force
    Write-Output "  deployed: $($jar.Name)"
}

Write-Output 'Plugin deploy complete.'
