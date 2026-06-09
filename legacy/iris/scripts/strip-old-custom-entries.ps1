# strip-old-custom-entries.ps1
# Removes custom tree entries (both old multi-line and new compact single-line formats)
# from Iris biome JSON files, preserving all stock entries untouched.
# Uses brace-counting to excise complete object blocks containing trees/custom/ paths.
# After running this script, re-run apply-tree-integration.ps1 to re-patch.

param(
    [string]$PackRoot = "C:\GameServers\Minecraft\testServer\RTP-Folia\26.1\plugins\Iris\packs\overworld\biomes"
)

# Given a string and a position inside a { ... } block, find the start of that block
# by scanning backwards (skipping string literals) until depth reaches 0.
function Find-BlockStart([string]$s, [int]$fromPos) {
    $depth = 0
    $i = $fromPos
    while ($i -ge 0) {
        $c = $s[$i]
        if ($c -eq '}') { $depth++ }
        elseif ($c -eq '{') {
            $depth--
            if ($depth -eq -1) { return $i }
        }
        $i--
    }
    return -1
}

# Given a string and the position of the opening { of a block, find its closing }
function Find-BlockEnd([string]$s, [int]$fromPos) {
    $depth = 0
    $i = $fromPos
    $inString = $false
    while ($i -lt $s.Length) {
        $c = $s[$i]
        if ($inString) {
            if ($c -eq '\') { $i += 2; continue }
            if ($c -eq '"') { $inString = $false }
        } else {
            if ($c -eq '"') { $inString = $true }
            elseif ($c -eq '{') { $depth++ }
            elseif ($c -eq '}') {
                $depth--
                if ($depth -eq 0) { return $i }
            }
        }
        $i++
    }
    return -1
}

$files = Get-ChildItem -Recurse -Path $PackRoot -Filter "*.json" -File | Where-Object {
    (Get-Content $_.FullName -Raw) -match 'trees/custom/'
}

Write-Output "=== Stripping old custom tree entries ==="
Write-Output "Found $($files.Count) affected files."
Write-Output ""

foreach ($file in $files) {
    $path = $file.FullName
    $raw = Get-Content $path -Raw

    # Back up before modifying (skip if .bak already exists)
    $bakPath = $path + ".bak"
    if (-not (Test-Path $bakPath)) {
        Copy-Item $path $bakPath
        Write-Output "  backed up: $($file.Name)"
    }

    $result = $raw

    # Repeatedly find and remove object blocks containing trees/custom/ until none remain
    $maxPasses = 50
    $pass = 0
    while ($result -match 'trees/custom/' -and $pass -lt $maxPasses) {
        $pass++
        $idx = $result.IndexOf('trees/custom/')
        if ($idx -lt 0) { break }

        # Find the opening { of the object containing this path
        $blockStart = Find-BlockStart $result $idx
        if ($blockStart -lt 0) { Write-Warning "Could not find block start in $($file.Name)"; break }

        # Find the closing } of that block
        $blockEnd = Find-BlockEnd $result $blockStart
        if ($blockEnd -lt 0) { Write-Warning "Could not find block end in $($file.Name)"; break }

        # Determine what to excise: include leading comma+whitespace OR trailing comma+whitespace
        $exciseStart = $blockStart
        $exciseEnd = $blockEnd

        # Check for leading comma (,\s* before blockStart)
        $before = $result.Substring(0, $blockStart)
        if ($before -match ',\s*$') {
            $exciseStart = $blockStart - $Matches[0].Length
        }
        # If no leading comma, check for trailing comma (\s*, after blockEnd)
        elseif ($result.Length -gt $blockEnd + 1) {
            $after = $result.Substring($blockEnd + 1)
            if ($after -match '^\s*,') {
                $exciseEnd = $blockEnd + $Matches[0].Length
            }
        }

        $result = $result.Substring(0, $exciseStart) + $result.Substring($exciseEnd + 1)
    }

    # Fix trailing commas before ] left by removal of last entry
    $result = [regex]::Replace($result, ',(\s*\n\s*\])', '$1')

    [System.IO.File]::WriteAllText($path, $result, [System.Text.UTF8Encoding]::new($false))
    Write-Output "  cleaned:  $($file.Name) (passes: $pass)"
}

Write-Output ""
Write-Output "=== Done. Now re-run apply-tree-integration.ps1 ==="
