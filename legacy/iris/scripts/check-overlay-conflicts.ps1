$repo = 'C:\Users\lxgol\IdeaProjects\LeafsServerBaseline'
$baseDir    = "$repo\iris\pack-base"
$overlayDir = "$repo\iris\pack-overlay"

$baseFiles    = Get-ChildItem $baseDir    -Recurse -File -Filter '*.json' | ForEach-Object { $_.FullName.Substring($baseDir.Length + 1) }
$overlayFiles = Get-ChildItem $overlayDir -Recurse -File -Filter '*.json' | ForEach-Object { $_.FullName.Substring($overlayDir.Length + 1) }

$conflicts = $overlayFiles | Where-Object { $baseFiles -contains $_ }
Write-Output "Overlay JSON files that overwrite base pack ($($conflicts.Count) total):"
$conflicts | ForEach-Object { Write-Output "  $_" }

Write-Output ""
Write-Output "Overlay-only files (new, not in base) ($( ($overlayFiles | Where-Object { $baseFiles -notcontains $_ }).Count ) total):"
$overlayFiles | Where-Object { $baseFiles -notcontains $_ } | ForEach-Object { Write-Output "  $_" }
