$root = "C:\GameServers\Minecraft\testServer\RTP-Folia\26.1\plugins\Iris\packs\overworld\biomes"
$base = "https://raw.githubusercontent.com/IrisDimensions/overworld/master/biomes"

# Find all patched biomes
$patched = Select-String -Path "$root\**\*.json" -Pattern "trees/custom" -SimpleMatch |
    Select-Object -ExpandProperty Path | Sort-Object -Unique

Write-Output "Found $($patched.Count) patched biomes - restoring from GitHub..."

$ok = 0
$fail = 0
foreach ($p in $patched) {
    $rel = $p.Replace($root + "\", "").Replace("\", "/")
    $url = "$base/$rel"
    try {
        $original = Invoke-RestMethod $url -ErrorAction Stop
        # original is already parsed as object by Invoke-RestMethod - get raw string instead
        $raw = (Invoke-WebRequest $url -UseBasicParsing -ErrorAction Stop).Content
        [System.IO.File]::WriteAllText($p, $raw, (New-Object System.Text.UTF8Encoding $false))
        $ok++
    } catch {
        Write-Output "FAIL: $rel -- $_"
        $fail++
    }
}

Write-Output "Restored: $ok  Failed: $fail"
