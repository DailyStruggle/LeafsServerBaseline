$packBiomes = "C:\GameServers\Minecraft\testServer\RTP-Paper\1.21.11\plugins\Iris\packs\overworld-custom\biomes"
$projectBiomes = "C:\Users\lxgol\IdeaProjects\LeafsServerBaseline\iris-biomes"

function Remove-BOM($path) {
    $b = [System.IO.File]::ReadAllBytes($path)
    if ($b.Length -ge 3 -and $b[0] -eq 0xEF -and $b[1] -eq 0xBB -and $b[2] -eq 0xBF) {
        [System.IO.File]::WriteAllBytes($path, $b[3..($b.Length - 1)])
        return $true
    }
    return $false
}

$fixed = 0

# Fix all custom biome JSONs in live pack
foreach ($f in (Get-ChildItem $packBiomes -Recurse -Filter "*.json" -File)) {
    if (Remove-BOM $f.FullName) {
        Write-Output "Fixed BOM (live): $($f.FullName.Replace($packBiomes + '\', ''))"
        $fixed++
    }
}

# Fix all custom biome JSONs in project
foreach ($f in (Get-ChildItem $projectBiomes -Recurse -Filter "*.json" -File)) {
    if (Remove-BOM $f.FullName) {
        Write-Output "Fixed BOM (project): $($f.FullName.Replace($projectBiomes + '\', ''))"
        $fixed++
    }
}

Write-Output "Total BOM fixes: $fixed"
