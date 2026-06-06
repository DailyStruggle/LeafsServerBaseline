# Lessons Learned

Dated engineering pitfalls, non-obvious behaviors, and "things that bit us". Add entries here so future sessions don't repeat the same mistakes.

## Format

```
### YYYY-MM-DD - <short title>

**Context:** what task or feature was being worked on.
**What happened:** the surprising or painful behavior.
**Fix / workaround:** what resolved it.
```

---

### 2026-06-05 - Iris objects store block coords relative to center (custom trees floated)

**Context:** Custom tree `.iob` objects placed by Iris floated dozens of blocks above the ground, worse for larger trees (offset scaled with height).
**What happened:** Iris stores object block coordinates RELATIVE TO THE OBJECT CENTER (`w//2, h//2, d//2`) and re-adds `getCenter()` at placement time (`IrisObject.place` -> `at.add(getCenter()).add(i)`). Stock objects are saved this way (e.g. `antioch1`: w8/h18/d9 -> x[-4,3] y[-9,8] z[-4,4]). Our generator (`scripts/tree-gen/nbt.py`) normalised coords to a 0-based origin (base at y=0), so every object was shifted up by ~h//2 -> giants floated ~38 blocks. A small `translate.y` could not compensate (it is dwarfed by h//2).
**Fix / workaround:** `nbt.py write_iob` now subtracts the center on all axes. Existing compiled `.iob`s were re-centered in place with `scripts/recenter-iob.py` (idempotent: skips files that already have a negative min coord). After re-centering, objects match stock and sit on the ground; the per-placement `translate.y` (default -3, via `-CustomYOffset` in `apply-tree-integration.ps1`) just mirrors stock root burial.



### 2026-06-05 - Iris biome object placement format: array only, never bare string

**Context:** Editing biome JSON files to fix missing object references.
**What happened:** Used `{ "place": "trees/foo/bar", "chance": 0.05 }` (bare string) instead of the correct array format. Iris only accepts `place` as an array. The bare-string format causes `IrisObjectPlacement` parse errors at startup.
**Fix / workaround:** Always use array format with `density` and `rotation`:
```json
{
  "chance": 0.05,
  "density": 2,
  "rotation": { "yAxis": { "min": 0, "max": 270, "interval": 90, "enabled": true }, "enabled": true },
  "place": ["trees/foo/bar1", "trees/foo/bar2"]
}
```
Never use `{ "place": "single/string", "chance": 0.05 }` - this is invalid and will break Iris.


### 2026-06-05 - Safe additive edits to Iris biome JSON files

**Context:** Adding custom tree entries to vanilla Iris biome JSON files without breaking the pack.
**What happened:** Round-tripping a biome JSON through PowerShell `ConvertFrom-Json` / `ConvertTo-Json` then writing it back causes Iris to reject the file with "JSON document was not fully consumed". PowerShell's serializer changes formatting, adds trailing commas, and restructures arrays in ways Iris's strict parser rejects. Deleting a JSON file from an existing pack directory also causes errors - Iris detects the presence of the `overworld` directory and expects all files to be present. Downloading or replacing pack files via script introduces BOMs and format corruption.
**Fix / workaround:** The only safe method for additive edits:
1. Read the file as raw text with `[System.IO.File]::ReadAllText($path, [System.Text.Encoding]::UTF8)`.
2. Use regex to remove the trailing `]\n}` (closing of objects array + root object): `$raw.TrimEnd() -replace '\s*\]\s*\}\s*$', $newEntry` where `$newEntry` is a raw JSON string that ends with `  ]\n}`.
3. Validate with `$updated | ConvertFrom-Json | Out-Null` before writing.
4. Write back with `[System.IO.File]::WriteAllText($path, $updated, (New-Object System.Text.UTF8Encoding($false)))` - the `$false` suppresses BOM.
5. Never use `ConvertTo-Json` to rewrite a biome file - only use it for validation.
6. Never delete files from an existing pack directory.
7. Never download or copy pack files from external sources - the server auto-downloads the correct version on startup when the `overworld` directory is absent.
8. Custom tree `.iob` files from other biome folders (e.g. `trees/darkoak/spiral_large_1`) can be referenced in any biome - the path is relative to `objects/` in the pack, not the biome folder.
9. After a successful additive edit directly on the server file, always copy the working server file back to `iris-pack/` so the repo stays in sync: `Copy-Item <server-file> <iris-pack-file> -Force`. The deploy script copies `iris-pack` as-is, so a stale repo file will overwrite the working server file on next deploy.
10. Never use PowerShell `-replace` with a scriptblock (`{ param($m) ... }`) on pack JSON files - PowerShell treats the scriptblock as a literal string replacement, injecting `' + {` artifacts and duplicating the entire file. Use `[regex]::new().Replace()` with a `[System.Text.RegularExpressions.MatchEvaluator]` delegate, or better yet use Python for any non-trivial JSON field transformations on pack files.

### 2026-06-05 - Iris custom biome JSON: exact required fields and valid values

**Context:** Adding custom biomes to the Iris overworld pack overlay.
**What happened:** Multiple fields had invalid values causing `IrisBiomeCustom.getCategory()` to return null and crash Iris on startup with `NullPointerException` at `IrisBiomeCustom.generateJson`.
**Fix / workaround:** Every custom biome JSON (and every entry in `customDerivitives` arrays) must follow these exact rules:

1. **`category` field is required** at the top level of every biome file AND inside every `customDerivitives` array entry. Without it, `getCategory()` returns null.
2. **Valid `category` values** (must be exact lowercase): `beach`, `desert`, `extreme_hills`, `forest`, `icy`, `jungle`, `mesa`, `mushroom`, `nether`, `none`, `ocean`, `plains`, `river`, `savanna`, `swamp`, `taiga`, `the_end`. Values like `frozen`, `mountain`, `tropical`, `tundra` are NOT valid.
3. **`derivative` field** uses uppercase Minecraft biome enum names (e.g. `FROZEN_PEAKS`, `DARK_FOREST`). These are the `IrisBiome` derivative values, not the `IrisBiomeCustomCategory` enum.
4. **`vanillaDerivative` field** also uses uppercase Minecraft biome names (e.g. `SNOWY_TAIGA`, `BADLANDS`). This is separate from `category`.
5. **`place` arrays** must always be arrays, never bare strings: `"place": ["trees/foo/bar"]` not `"place": "trees/foo/bar"`.
6. **No BOM, no CRLF** - files must be UTF-8 without BOM, LF line endings only. Both PowerShell `Set-Content`/`Out-File` AND the Junie `create` tool write UTF-8 BOM by default. After creating any JSON file via either method, strip the BOM with: `$b = [System.IO.File]::ReadAllBytes($f); if ($b[0] -eq 0xEF) { [System.IO.File]::WriteAllBytes($f, $b[3..($b.Length-1)]) }`
7. **No PowerShell JSON round-tripping** - never use `ConvertFrom-Json` / `ConvertTo-Json` to rewrite biome files. Use Python scripts or raw text manipulation only.
8. **Deployment pipeline**: base pack (Layer 1) + overlay custom files (Layer 2) + custom `.iob` files (Layer 3). The overlay must contain ONLY files not present in the base pack, or intentional overrides. Run `iris/scripts/check-overlay-conflicts.ps1` to verify.