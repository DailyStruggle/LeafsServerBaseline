# Slot icon pixel-art tool

`json_to_pixelart.py` renders JSON pixel-art definitions into 32x32 RGBA PNG
icons for the LeafArtifacts curio-slot placeholders (pixel art composited on a
light-gray stained-glass background). It is pure Python 3 standard library (no
Pillow / no external packages), matching the `iris/tree-gen` convention.

## Run

```
python json_to_pixelart.py                 # render every icons/*.json
python json_to_pixelart.py icons/head.json # render a single file
python json_to_pixelart.py --in <dir> --out <dir> --size 32
```

Defaults: input `tools/icons/`, output
`../assets/leafartifacts/textures/item/slot/` (the resource-pack texture dir).

## Definition format (`icons/<slot>.json`)

| Field | Meaning |
|---|---|
| `name` | output file stem (default: the JSON file name) |
| `size` | output square size in pixels (default `32`) |
| `background` | `light_gray_glass` preset (default), a `#RRGGBB[AA]` colour, or `none` |
| `palette` | single-character key -> `#RGB` / `#RRGGBB` / `#RRGGBBAA` colour |
| `art` | rectangular grid of rows; each cell is one palette key, `.` / space = transparent |

The `art` grid may be any rectangular size and is scaled with nearest-neighbour
sampling to fill the output square (e.g. a 16x16 grid scales 2x to 32x32).
Transparent cells fall through to the background.

The six icons shipped here (`head`, `necklace`, `hands`, `ring`, `charm`,
`feet`) correspond to the slot types LeafArtifacts registers in
`ArtifactSlotDefs`.
