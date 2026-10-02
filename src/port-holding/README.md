# Porting holding area

The 1.21 original sources live here while they are being ported to 1.7.10.

**This directory is not on any Gradle source path**, so nothing here is compiled — which is what
keeps `src/main/java` building while most of the mod is still 1.21 code. The files are complete and
unmodified (apart from the `net.satisfy` -> `com.mrfuzzihead` package rename done in Phase 0), so a
port is a rewrite-in-place rather than a recovery exercise.

## Workflow

1. Pick the next subsystem in dependency order (see `BACKPORT_PLAN.md` phase list).
2. `git mv` the file from here into `src/main/java/com/mrfuzzihead/vinery/...` as you port it.
3. Rewrite it against the 1.7.10 API. Notes for each subsystem live in `BACKPORT_PLAN.md` sections
   4.1-4.17.
4. If the file was a mixin, also uncomment its entry in
   `src/main/java/com/mrfuzzihead/vinery/mixins/Mixins.java` — a mixin that is listed but not
   compiled kills the game at mod construction.
5. Nothing to undo: `git mv` back if a port turns out to be a dead end.

## Contents at the end of Phase 1

| Area | Files | Notes |
|---|---|---|
| `core/` | 138 | blocks, items, entities, recipes, effects, registries, worldgen |
| `client/` | 29 | renderers, GUI, models |
| `forge/` | 12 | 1.21-only NeoForge entry points, config, datagen, REI — mostly deleted rather than ported |
| `mixins/early/` | 13 | see `BACKPORT_PLAN.md` section 6; several will not be mixins on 1.7.10 |
| `mixins/late/dmod/` | 1 | D-Mod fox integration, or replaced by its `IBerryBushHandler` API |
| root | 2 | `Vinery.java` (ported), `PlatformHelper.java` (needs review) |
| **total** | **185** | against 204 registry entries still to port |