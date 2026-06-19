# Deployment

How to deploy this repo's content to the test server. Deployment is **modular**:
each datapack and each plugin is an independent unit, discovered and deployed on
its own. The deploy scripts only build/stage/copy files - they never boot the
server (server boots are user-triggered).

## What gets deployed

| Unit | Source | Script | Destination |
|------|--------|--------|-------------|
| Datapacks | `datapacks/<pack>/` (each with `pack.mcmeta`) | `datapacks/deploy-datapacks.ps1` | `<ServerBase>\<LevelName>\datapacks\<pack>` |
| Plugins | `plugins/<module>/` (each with `build.gradle`) | `plugins/deploy-plugins.ps1` | `<ServerBase>\plugins` |

Worldgen ships as vanilla datapacks (see
[ADR-005](../design/adr/ADR-005-retire-iris-for-vanilla-datapack-worldgen.md));
Iris and its `deploy-iris-pack.ps1` are retired under `legacy/iris/`.

## Deploying datapacks

```powershell
datapacks/deploy-datapacks.ps1
```

- Auto-discovers each `datapacks/<pack>` containing a `pack.mcmeta` and
  wipe-then-copies it into `<ServerBase>\<LevelName>\datapacks\<pack>` (the wipe
  ensures files removed from the repo do not linger as a stale snapshot).
- The target level name is resolved from `-LevelName`, else `level-name` in
  `server.properties`, else `world`.

Switches:

- `-Only <pack[,pack]>` - limit the deploy to specific pack folders.
- `-ResetWorld` - **destructive**: moves the world folder aside to a timestamped
  backup (or deletes it with `-NoBackup`) so the server regenerates it FROM
  SCRATCH on the next boot. Use this when worldgen/dimension changes must show up
  everywhere, not just in newly generated chunks. Off by default.
- `-NoBackup` - skip the backup when `-ResetWorld` is given.
- `-ServerBase <path>` - override the server root.

## Deploying plugins

```powershell
plugins/deploy-plugins.ps1
```

- Auto-discovers each plugin **module** (a direct subfolder of `plugins/`
  containing a `build.gradle`; each is a standalone Gradle project), **builds**
  it with `gradle jar`, then copies the resulting `build/libs/*.jar` into
  `<ServerBase>\plugins`.

Switches:

- `-NoBuild` - skip the build and copy whatever jars already exist in each
  module's `build/libs` (the old copy-only behaviour; also the fallback when
  `gradle` is not on PATH).
- `-Only <name[,name]>` - restrict the build/deploy to specific module folders.
- `-ServerBase <path>` - override the server root.

## After deploying

Deploy scripts never boot the server. To make changes take effect:

- **Data-only datapack changes** - run `/reload` in-game.
- **Worldgen/dimension changes** - regenerate only in newly generated chunks; use
  `-ResetWorld` (above) to regenerate from scratch.
- **Plugin jars** - load only on a fresh server start.

All of these happen on the **next** server boot, which is **user-triggered**: deploy
yourself, then ask the server operator to reboot (or `/reload`) when in-game
verification is needed.
