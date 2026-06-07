param(
    [string]$ServerBase = 'C:\GameServers\Minecraft\testServer\RTP-Paper\1.21.11',
    # Explicit target Iris pack dir; overrides the default plugins\Iris\packs\overworld.
    # The active "test" world uses a WORLD-LOCAL pack at <ServerBase>\test\iris\pack.
    [string]$PackDir = ''
)

# Deploys the (git-ignored, CC BY-NC-ND) Towns & Towers -derived village-meadow
# jigsaw assets into the LIVE Iris overworld pack on the private test server.
# These derived assets must never be redistributed (ADR-004); this only copies
# them onto the local non-commercial test server.

$src = 'C:\Users\lxgol\IdeaProjects\LeafsServerBaseline\iris\thirdparty-derived\towns-and-towers\iris'
if ($PackDir -ne '') { $ow = $PackDir } else { $ow = Join-Path $ServerBase 'plugins\Iris\packs\overworld' }

foreach ($d in 'objects', 'jigsaw-pieces', 'jigsaw-pools', 'jigsaw-structures') {
    $from = Join-Path $src $d
    $to   = Join-Path $ow $d
    New-Item -ItemType Directory -Path $to -Force | Out-Null
    Copy-Item (Join-Path $from '*') $to -Recurse -Force
    Write-Output "copied $d"
}

Write-Output ("structure deployed : " + (Test-Path (Join-Path $ow 'jigsaw-structures\village-meadow.json')))
Write-Output ("meadow piece jsons : " + (Get-ChildItem (Join-Path $ow 'jigsaw-pieces\village\meadow_swiss') -Recurse -Filter *.json | Measure-Object).Count)
Write-Output ("meadow iob objects : " + (Get-ChildItem (Join-Path $ow 'objects\jigsaw\village\meadow_swiss') -Recurse -Filter *.iob | Measure-Object).Count)
