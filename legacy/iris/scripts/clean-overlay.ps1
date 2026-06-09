$repo = 'C:\Users\lxgol\IdeaProjects\LeafsServerBaseline'
$baseDir    = "$repo\iris\pack-base"
$overlayDir = "$repo\iris\pack-overlay"

$baseFiles    = Get-ChildItem $baseDir    -Recurse -File | ForEach-Object { $_.FullName.Substring($baseDir.Length + 1) }
$overlayFiles = Get-ChildItem $overlayDir -Recurse -File | ForEach-Object { $_.FullName.Substring($overlayDir.Length + 1) }

$conflicts = $overlayFiles | Where-Object { $baseFiles -contains $_ }
Write-Output "Removing $($conflicts.Count) files from overlay that duplicate base pack..."

foreach ($rel in $conflicts) {
    $path = "$overlayDir\$rel"
    Remove-Item $path -Force
}

# Remove empty directories
Get-ChildItem $overlayDir -Recurse -Directory | Sort-Object FullName -Descending | ForEach-Object {
    if ((Get-ChildItem $_.FullName -Recurse -File).Count -eq 0) {
        Remove-Item $_.FullName -Force -ErrorAction SilentlyContinue
    }
}

$remaining = (Get-ChildItem $overlayDir -Recurse -File).Count
Write-Output "Done. Overlay now has $remaining files (should be ~65)."
