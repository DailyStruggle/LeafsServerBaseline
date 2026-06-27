param(
    [string]$ServerBase = 'C:\GameServers\Minecraft\testServer\RTP-Paper\26.2'
)

# Deploys the source-controlled custom Log4j2 config (configs/logging/log4j2.xml)
# to the Paper server root and wires it into start.bat via
# -Dlog4j.configurationFile=log4j2.xml.
#
# The custom config is Paper's bundled log4j2.xml plus a single RegexFilter that
# DENYs the benign "Detected unsafe terrain read during worldgen" diagnostic
# spammed by large third-party jigsaw structures (see
# docs/design/STRUCTURE-PACK-INTEGRATION.md). Generation is unaffected; only the
# log line is suppressed.
#
# Like the other deploy scripts this ONLY stages/copies/patches files - it does
# NOT boot the server. The new logging config takes effect on the next server
# start, which is user-triggered. The script is idempotent (safe to re-run).

$repo = 'C:\Users\lxgol\IdeaProjects\LeafsServerBaseline'
$src  = "$repo\configs\logging\log4j2.xml"
$dest = "$ServerBase\log4j2.xml"
$bat  = "$ServerBase\start.bat"

if (-not (Test-Path $src)) {
    throw "Source config not found: $src"
}
if (-not (Test-Path $ServerBase)) {
    throw "Server base not found: $ServerBase"
}

Write-Output "=== Deploying custom Log4j2 config to $dest ==="
Copy-Item $src $dest -Force
Write-Output "  log4j2.xml deployed"

# Wire the JVM flag into start.bat (idempotent).
if (-not (Test-Path $bat)) {
    Write-Output "  WARNING: $bat not found - add '-Dlog4j.configurationFile=log4j2.xml' to your JVM args manually."
    return
}

$lines = Get-Content -LiteralPath $bat
if ($lines -match 'log4j\.configurationFile') {
    Write-Output "  start.bat already references log4j.configurationFile - left as-is"
} else {
    # Insert the flag (with a caret line-continuation) just before the '-jar' line.
    $jarIdx = ($lines | Select-String -SimpleMatch '-jar ' | Select-Object -First 1).LineNumber
    if (-not $jarIdx) {
        Write-Output "  WARNING: could not find a '-jar' line in start.bat - add '-Dlog4j.configurationFile=log4j2.xml' manually."
        return
    }
    $insertAt = $jarIdx - 1   # 0-based index of the '-jar' line
    $flagLine = ' -Dlog4j.configurationFile=log4j2.xml ^'
    $new = @()
    $new += $lines[0..($insertAt - 1)]
    $new += $flagLine
    $new += $lines[$insertAt..($lines.Count - 1)]
    Set-Content -LiteralPath $bat -Value $new -Encoding ASCII
    Write-Output "  start.bat patched with -Dlog4j.configurationFile=log4j2.xml"
}

Write-Output 'Logging config deploy complete.'
Write-Output 'Reminder: restart the server to apply (server boots are user-triggered).'
