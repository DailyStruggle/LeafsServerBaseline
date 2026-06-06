$biomesRoot = "C:\GameServers\Minecraft\testServer\RTP-Paper\1.21.11\plugins\Iris\packs\overworld\biomes"
$results = @()

Get-ChildItem -Recurse -Path $biomesRoot -Filter "*.json" -File | ForEach-Object {
    $file = $_
    $json = Get-Content $file.FullName -Raw | ConvertFrom-Json -ErrorAction SilentlyContinue
    if (-not $json) { return }
    if (-not $json.objects) { return }
    foreach ($obj in $json.objects) {
        $places = ($obj.place -split "\s+") | Where-Object { $_ -ne "" }
        foreach ($p in $places) {
            if ($p.Length -le 2) {
                $results += [PSCustomObject]@{
                    File     = $file.FullName.Replace($biomesRoot + "\", "")
                    ShortRef = $p
                    Chance   = $obj.chance
                    FullLine = $obj.place.Substring(0, [Math]::Min(80, $obj.place.Length))
                }
            }
        }
    }
}

Write-Output "=== Broken/short object refs (length <= 2) ==="
Write-Output "Total occurrences: $($results.Count)"
Write-Output ""
$results | Sort-Object File | ForEach-Object {
    Write-Output "[$($_.File)] ref='$($_.ShortRef)' chance=$($_.Chance)"
    Write-Output "  full: $($_.FullLine)"
}
