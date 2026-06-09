# Third-Party Derived Structure Assets (quarantine)

This directory holds structure assets **extracted from, or converted from, third-party
structure packs** (for example, Towns & Towers) before they are merged into the live Iris
pack. It exists to keep license-restricted derivatives in one clearly-marked, version-control
-excluded place.

## Redistribution policy (read before adding anything)

Source packs such as Towns & Towers are licensed **CC BY-NC-ND 4.0**:

- **BY** - attribution required (keep the upstream credits with the assets).
- **NC** - non-commercial use only. This server is non-commercial, so this is satisfied.
- **ND** - **No Derivatives may be Shared.** CC BY-NC-ND 4.0 Section 2 explicitly permits
  *producing* adapted material for private, non-commercial use, but forbids **Sharing** it.

Therefore, for everything under this directory:

1. **Private, non-commercial use only.** Run it on this server; do not distribute it.
2. **Never publish the extracted or converted assets** - not to a public git remote, not as a
   downloadable pack, not to another operator. Publishing would be "Sharing Adapted Material"
   and is not permitted by the license.
3. **Attribution stays with the assets.** Record the source pack, version, author, and license
   for each import (see `provenance/` notes you add per import).

## What is tracked vs ignored

- This `README.md` **is** tracked - so the policy travels with the repo.
- **Everything else** under `iris/thirdparty-derived/` is git-ignored (see root `.gitignore`).
  The extracted `.nbt` pieces and converted Iris objects are never committed.

## Where derived assets end up at runtime

These assets are a *staging/source* holding area. When wired into the pack they are merged into
`iris/staging/` (`objects/`, `jigsaw-pieces/`, `jigsaw-pools/`, `jigsaw-structures/`), which is
**also** git-ignored. They must never be placed in the tracked `iris/pack-overlay/` tree.

## Suggested layout per source pack

```
iris/thirdparty-derived/
  README.md                     (tracked)
  <pack-name>/
    SOURCE.md                   (provenance: pack, version, author, license, download URL)
    raw/                        (extracted .nbt pieces, untouched)
    iris/                       (converted Iris objects + jigsaw wiring)
```
