# leaf-bosses datapack

Loot tables and advancements for the biome bosses spawned and managed by the
`leaf-mobs` plugin (F6). This datapack supplies the content that `leaf-mobs`
references by id; the plugin owns spawn/boss-bar/death logic, this datapack owns
"what the kill is worth".

It is dependency-free: the boss artifacts are plain vanilla items stamped, in
the loot table, with the same persistent-data tags the `leaf-artifacts` /
`leaf-curios` plugins read (`leafartifacts:artifact_type` and `leafcurios:slots`
inside `minecraft:custom_data` -> `PublicBukkitValues`). No plugin dependency or
custom item registration is needed; when those plugins are present the dropped
item behaves as a real, equippable artifact, and when they are absent it is just
a themed vanilla item.

## Reward model

Each boss yields its themed artifact two ways, matching the design ("guaranteed
on the first kill PLUS rare on the loot table"):

- **First kill (guaranteed):** the boss advancement's `rewards.loot` points at a
  `*_firstkill` table that always contains exactly the artifact. `leaf-mobs`
  `RewardService` grants the advancement to the killer on a PDC-marked boss death;
  vanilla records it "done" once, so the artifact is handed out only the first time.
- **Repeat kills (rare):** the boss `EntityType` loot table (rolled by
  `RewardService` with the killer as looter) carries the artifact behind a
  `random_chance` gate plus plenty of biome-specific valuables.

## Contents

| Boss | Biome / base | Artifact | Loot table | Advancement |
|------|--------------|----------|------------|-------------|
| Grovewarden | giant dark forest / RAVAGER | Thorn Pendant | `leafbosses:boss/grovewarden` | `leafbosses:boss/grovewarden` |
| Blight Warden | swamp / WITCH | Antidote Vessel | `leafbosses:boss/blight_warden` | `leafbosses:boss/blight_warden` |
| Canopy Tyrant | jungle / RAVAGER | Feral Claws | `leafbosses:boss/canopy_tyrant` | `leafbosses:boss/canopy_tyrant` |
| Mire Colossus | swamp / RAVAGER | Steadfast Spikes | `leafbosses:boss/mire_colossus` | `leafbosses:boss/mire_colossus` |
| Old Shepherd | meadow / IRON_GOLEM | Crystal Heart | `leafbosses:boss/old_shepherd` | `leafbosses:boss/old_shepherd` |
| Blossom Sovereign | cherry/jacaranda grove / EVOKER | Lucky Scarf | `leafbosses:boss/blossom_sovereign` | `leafbosses:boss/blossom_sovereign` |
| Embervine Warden | volcanic mountain / WITHER_SKELETON | Obsidian Skull | `leafbosses:boss/embervine_warden` | `leafbosses:boss/embervine_warden` |
| Ashwood Revenant | badlands / WITHER_SKELETON | Vampiric Glove | `leafbosses:boss/ashwood_revenant` | `leafbosses:boss/ashwood_revenant` |

- `loot_table/boss/<id>.json` - repeat-kill drops (rare artifact + valuables),
  rolled by `leaf-mobs` into the death drops.
- `loot_table/boss/<id>_firstkill.json` - the guaranteed first-kill artifact,
  delivered through the advancement reward.
- `advancement/root.json` - "Biome Bosses" tab parent.
- `advancement/boss/<id>.json` - per-boss goal (impossible trigger, awarded by
  the plugin), with `rewards.loot` set to the matching `*_firstkill` table.

The loot-table / advancement ids must match the `loot_table:` / `advancement:`
fields of the corresponding rows in `leaf-mobs`' `mobs.yml`.

## Deploy

Auto-discovered by the modular datapack deploy (wipe-then-copy into
`<ServerBase>\<LevelName>\datapacks\leaf-bosses`):

```powershell
datapacks\deploy-datapacks.ps1 -Only leaf-bosses
```

Data-only, so it loads on the next `/reload` or server boot (user-triggered).
After a reload, kill a spawned boss (`/leafmobs spawn grovewarden`,
`/leafmobs spawn blight_warden`) and confirm: the advancement toast fires and the
artifact is granted on the first kill, and the rare artifact / valuables roll on
subsequent kills.
