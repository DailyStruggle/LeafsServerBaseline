# Potential Bugs

Backlog of incidental findings discovered while working on other tasks. Each entry is a problem the current task chose not to solve.

**This file is NOT for:** work you just finished, session state, build errors, or roadmap items. See `.junie/AGENTS.md` - *Stay-On-Task Policy* for the full rules.

## Format

```
### YYYY-MM-DD - <short title>

**Discovered during:** <link or short ref to the task>
**Location:** <file path + line range or symbol>
**Symptom / hypothesis:** one or two sentences.
**Impact:** best guess at user-visible effect.
**Suggested next step:** minimal investigation or fix sketch.
```

---

## 2026-06-05 | discovered-during: iris-tree-integration fix

- **Location**: `scripts/apply-tree-integration.ps1` lines 65-75, 228-237, 483-489
- **Symptom**: `apply-tree-integration.ps1` references `temperate/forest.json`, `temperate/forest-extended.json`, `temperate/forest-flat.json`, `tundra/maple-forest.json`, and `mountain/floating-islands.json` - none of these exist in the current server's iris pack. The script emits MISSING warnings for all five.
- **Impact**: Those biomes never receive custom tree objects. If the pack is updated and these files appear, they will be patched automatically on next run - but currently they are silently skipped.
- **Suggested next step**: Confirm whether these biome files exist in a newer version of the overworld pack, or remove the dead entries from the script.

## 2026-06-05 | discovered-during: iris-tree-integration place-array fix

- **Location**: `scripts/apply-tree-integration.ps1` `Trim-Objects` (lines 24-30)
- **Symptom**: The trim regex requires `"place"\s*:\s*"<string>"` with `[^{}]*` between the opening `{` and `place`. Stock Iris object entries store `place` as a multi-line JSON array and contain nested `{...}` (e.g. `rotation`), so the pattern never matches.
- **Impact**: Every `Trim-Objects` call (antioch, tredwood, AmyLarge, etc.) is a silent no-op; stock tree spawn rates are never reduced, only custom trees are added.
- **Suggested next step**: Rework `Trim-Objects` to locate the target object via its `place` array element and adjust `chance` (e.g. brace-aware object scan like the insert logic in `Apply`), rather than a single-line string regex.

## 2026-06-05 | discovered-during: iris-tree-integration place-array fix

- **Location**: `scripts/apply-tree-integration.ps1` `Make-Entry` (line 11) and call sites (e.g. lines 94-97)
- **Symptom**: `Make-Entry` is declared `([string[]]$paths, [double]$chance)` but call sites pass a third positional argument (e.g. `(Make-Entry (Custom "oak" "smol" 10) 0.04 2)`). The trailing number is silently dropped into `$args` and ignored.
- **Impact**: Likely an intended per-tier weight/count that has no effect; placement counts may differ from author intent.
- **Suggested next step**: Decide whether the third value is meaningful; if so, add a `$weight`/`$count` parameter and use it, otherwise remove it from call sites.
