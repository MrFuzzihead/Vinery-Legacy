# Vinery — Minecraft 1.7.10 (Forge) Backport Plan

Target: MC **1.7.10**, Forge **10.13.4.1614**, MCP **stable_12**, GTNH toolchain (RetroFuturaGradle).
Source of truth: `origin/main` (Vinery 1.21.1 NeoForge/Architectury).

---

## 0. Decisions at a glance

| Topic              | Decision                                                                                   | Confidence                               |
|--------------------|--------------------------------------------------------------------------------------------|------------------------------------------|
| Java version       | **`jabel`** (Java 8 bytecode, modern syntax)                                               | High — empirically verified in this repo |
| Build system       | Single-module RFG/GTNH convention (already in place)                                       | High                                     |
| Architectury API   | **Removed** — no 1.7.10 build exists; replace with Forge natives                           | High (verified)                          |
| Model system       | **Vanilla-path blocks + flat items + targeted OBJ converter for decorative geometry** (§5) | High — recommended, see §5               |
| Mod identity       | Keep `modId = vinery`; finish package migration to `com.mrfuzzihead.vinery`                | Needs your call                          |
| Et Futurum Requiem | **Optional integration** (dev-only in `dependencies.gradle`) — banners, composter, dirt paths, stripped logs, chest boats, cherry/mangrove/bamboo wood sets | High (API verified) |
| D-Mod (makamys)    | **Optional integration** — foxes eat Vinery grape bushes via its public API                | High (API verified)                      |
| Hanging signs      | **Stubbed/TODO** — absent from 1.7.10 *and* the GTNH Et Futurum fork                       | Decided                                  |

---

## 1. Current state audit (verified)

**Build system** — already functional:
- `./gradlew help` → BUILD SUCCESSFUL, Gradle 9.3.1, `com.gtnewhorizons.gtnhconvention` 2.0.20 (2.0.34 available).
- MC 1.7.10 workspace is already decompiled/patched (`build/rfg/*`), so iteration is fast.
- `enableModernJavaSyntax = jabel` is already set and confirmed active (compiler prints `Jabel: initialized`).

**Source tree** — 183 Java files / 16,199 LOC:

| Area                                | Files       | Notes                                                                                 |
|-------------------------------------|-------------|---------------------------------------------------------------------------------------|
| `core/`                             | 138         | blocks, items, entities, recipes, effects, registries                                 |
| `client/`                           | 29          | renderers, GUI, models                                                                |
| `forge/`                            | 14          | NeoForge entry points, config, villager trades, mixins                                |
| `mixins/early` + `mixins/late/dmod` | 14 (13 + 1) | see §6 — splits by UniMixins phase; shrinks to 7 once Forge-native hooks replace them |

**Blocking problems:**

1. ~~All assets and data were deleted in the "Initial refactor" commit~~ — ✅ **RESOLVED**: assets have been restored. Verified in working tree: **294 textures, 418 models, 139 blockstates, 439 data files, lang + sounds** (1,339 resource files total).
2. 🔴 **The tree does not compile for a non-API reason.** 170 files declare `package com.mrfuzzihead.vinery.*` but **102 files still `import net.satisfy.vinery.*`**. This must be fixed before any API work.
3. 🟡 `mcmod.info` is still the RFG template placeholder, `pack.mcmeta` still says `pack_format: 34` (a 1.21 value — 1.7.10 wants `1`).

> **Phase 0 status (done):** items 2 and 3 are fixed — 0 `net.satisfy` references remain, every package declaration matches its directory, mixins are consolidated into `com.mrfuzzihead.vinery.mixin`, `usesMixins = true` resolves UniMixins 0.2.1, `mcmod.info` and both `pack.mcmeta` files are 1.7.10-correct, and the dead 1.21-only files were removed (see §4.17 and the commit). The code itself still targets 1.21 APIs, which is Phase 1+ work.

Asset inventory (now restored, for reference):

| Asset class                  | Count                        | 1.7.10 usability                                                                                                                                          |
|------------------------------|------------------------------|-----------------------------------------------------------------------------------------------------------------------------------------------------------|
| Textures                     | 289 PNG + 5 `.mcmeta`        | ✅ usable as-is                                                                                                                                           |
| Lang files                   | 16                           | ✅ usable (JSON same format)                                                                                                                              |
| Sounds                       | 8 `.ogg` + `sounds.json`     | ✅ usable (1.7.10 supports `sounds.json`)                                                                                                                 |
| Resource pack `bushy_leaves` | 1 pack                       | 🟡 no `AddPackFindersEvent` in 1.7.10 → config-gated texture swap or drop                                                                                 |
| Block models                 | 418 JSON                     | 🟡 **no 1.7.10 JSON model system** — but per §5 only ~6 templates + wine bottles are actually *converted*; the rest are geometry reference / kept in-repo |
| Blockstates                  | 139 JSON                     | 🔁 → metadata mapping + per-family rendering path (§5)                                                                                                    |
| Recipes                      | 147 JSON                     | ❌ → `IRecipe` registrations                                                                                                                              |
| Loot tables                  | 109 JSON                     | ❌ no loot tables in 1.7.10 (108 are per-block drops; droppable)                                                                                          |
| Advancements                 | 70 JSON                      | ❌ no advancements → achievements or drop                                                                                                                 |
| Tags                         | 30 JSON                      | ⚠️ → Forge tags                                                                                                                                           |
| Worldgen                     | 22 JSON + 10 biome modifiers | ❌ → `IWorldGenerator`                                                                                                                                    |
| Structures                   | 4 `.nbt`                     | ❌ → 1.7.10 schematic format                                                                                                                              |

---

## 2. Java version: **`enableModernJavaSyntax = jabel`** ✅

### 2.1 Evidence (probes compiled through this project's actual RFG build, then run on a real Java 8 JRE)

| Feature                                                             | Occurrences in repo | Jabel verdict                                                            |
|---------------------------------------------------------------------|---------------------|--------------------------------------------------------------------------|
| Switch expressions (`case X ->`)                                    | 89                  | ✅ compiles                                                              |
| Enhanced `instanceof` patterns                                      | 45                  | ✅ compiles                                                              |
| `var`                                                               | 6                   | ✅ compiles                                                              |
| `record`                                                            | 5                   | ⚠️ **each record needs `@Desugar`** (`com.github.bsideup.jabel.Desugar`) |
| Text blocks / sealed types / pattern `switch`                       | 0                   | n/a                                                                      |
| `Stream.toList()`                                                   | 8                   | ❌ **not available** (Jabel is syntax-only)                              |
| `List.copyOf`                                                       | 1                   | ❌ not available (use Guava `ImmutableList.copyOf`)                      |
| `Map.of` / `Set.of` / `Map.entry` / `Optional.or` / `String.repeat` | 0                   | n/a                                                                      |

Verified output: `major version: 52` (Java 8 bytecode), executed successfully on `jdk1.8.0_202`.
Failure mode observed without `@Desugar`: `error: Must be annotated with @Desugar`.

### 2.2 Why not the alternatives

- **`modern` (Java 25 bytecode)** — a 1.7.10 mod must run on Java 8 clients. Rejected outright.
- **`jvmDowngrader`** — the only thing it would buy us is the 9 `toList()`/`copyOf` call sites. Cost: an extra runtime dependency (GTNHLib stubs or shaded stubs), a second bytecode transform in the pipeline, slower builds, and extra risk against UniMixins refmap remapping. Not worth it — but it stays a **one-line escape hatch** if the backport later needs Java 9+ library APIs wholesale.
- **`false` (Java 8 only)** — would mean hand-rewriting ~140 switch expressions/instanceof patterns and 6 `var`s. Pure churn, zero benefit.

### 2.3 Rules that follow from choosing Jabel

1. **Syntax freedom, library conservatism.** Java 9+ *language* features are fine; Java 9+ *library* APIs are not.
2. Add `@Desugar` to the 5 records — or better, since all 5 (`WineYearComponent`, 3 × `RecipeInput`, nested `JuiceData`) live in subsystems that need rewriting anyway (§4), convert them to plain classes and skip the annotation.
3. Audit toolchain: `enableGenericInjection = true` already set (good), `minecraftVersion`/`forgeVersion`/`channel`/`mappingsVersion` already correct.
4. Toolchain JDK for building: **21** (installed, and used to run Gradle). Output stays Java 8.

---

## 3. Build & repo architecture

### 3.1 Layout

```
build.gradle.kts            # gtnhconvention only (unchanged)
dependencies.gradle         # compat deps
settings.gradle.kts         # gtnhsettingsconvention 2.0.20 (consider bump)
src/main/java/com/mrfuzzihead/vinery/
    Vinery.java             # @Mod entry (FMLPreInitializationEvent/InitEvent)
    core/                   # blocks, items, entities, recipes, effects, registries, util
    client/                 # renderers, GUI, models (client-only, loaded via client @Mod class)
    compat/jei/             # JEI plugin (REI support deleted)
    mixin/                  # UniMixins (core + client mixins)
src/main/resources/
    mcmod.info              # real 1.7.10 content (currently RFG template!)
    pack.mcmeta             # pack_format 1  (currently 34!)
    assets/vinery/{textures,lang,sounds,models,resourcepacks}
```

- Delete `build.gradle.OLD`, `settings.gradle.OLD`, `gradle.properties.OLD` once the port is stable.
- Drop the `forge/` package: everything in it (villager trades, POI, config, REI) becomes native Forge or JEI code.
- Keep `client/` separate from `core/` (standard 1.7.10 practice; prevents server-side class loading issues).

### 3.2 `gradle.properties` changes required

| Property                                    | Current   | Target                                                     |
|---------------------------------------------|-----------|------------------------------------------------------------|
| `usesMixins`                                | `false`   | **`true`** (UniMixins is auto-added)                       |
| `mixinsPackage`                             | *(empty)* | `com.mrfuzzihead.vinery.mixin` (must match the mixin JSON) |
| `separateMixinSourceSet`                    | *(empty)* | `mixin` (optional; speeds compiles)                        |
| `enableModernJavaSyntax`                    | `jabel`   | `jabel` ✅ keep                                            |
| `apiPackage`                                | *(empty)* | optional                                                   |
| `modrinthProjectId` / `curseForgeProjectId` | *(empty)* | fill in when publishing is decided                         |

### 3.3 `dependencies.gradle`

- **JEI (1.7.10)**: `compileOnlyApi` + `runtimeOnlyNonPublishable` — `mezz.jei:jei_1.7.10:...` (API differs completely from 1.21).
- **NEI**: optional `compileOnly` (`NotEnoughItems`) if NEI support is wanted.
- **UniMixins**: automatic via `usesMixins`.
- **Fastutil**: Forge 1.7.10 already ships `it.unimi.dsi.fastutil` (used in `EventHandler`) — no dependency needed.
- **No Architectury** (verified: no 1.7.10 artifact on `maven.architectury.dev` and no 1.7.10 release on Modrinth).
- **Integration compile-only deps** (dev-only, see §4.15 for detail):
  ```groovy
  devOnlyNonPublishable(rfg.deobf("curse.maven:et-futurum-<cfid>:<fileid>"))   // GTNewHorizons/Et-Futurum-Requiem
  devOnlyNonPublishable(rfg.deobf("curse.maven:d-mod-<cfid>:<fileid>"))        // makamys/DMod
  ```
  Compile-only, so they never become runtime dependencies — Vinery stays standalone.

---

## 4. Mod architecture — subsystem backport matrix

Legend: ✅ native 1.7.10 equivalent · 🔁 rewrite · ➕ new code needed · ❌ no equivalent (drop or reinvent)

### 4.1 Registries & entry points
- 🔁 `dev.architectury.registry.registries.DeferredRegister` / `RegistrySupplier` → `GameRegistry.registerBlock/registerItem/registerTileEntity/registerWorldGenerator` + static `Item`/`Block` fields.
  **204 `RegistrySupplier<>` references** across the codebase — expect a wide but mechanical refactor. Recommend a thin internal `Registry`/`Ref<T>` shim so call sites read like `.get()` and stay similar to today's code.
- 🔁 `@Mod` constructor + NeoForge buses → `FMLPreInitializationEvent` (registries) / `FMLInitializationEvent` / `FMLPostInitializationEvent` / `FMLServerStartingEvent`.
- 🔁 `ModConfigSpec` → Forge 1.7.10 `Configuration` object (`new Configuration(event.getSuggestedConfigurationFile())`); ~25 config values.

### 4.2 Blocks & block states — **biggest mechanical change**
- 🔁 `BlockBehaviour.Properties` + `BlockState`/`IBlockState` → 1.7.10 `Block` constructor args and **plain metadata ints**.
  > **Correction (Phase 2):** this project's 1.7.10 has **no block-property system at all** — verified against the compiled `patchedMc`: there is no `net.minecraft.block.properties` package, no `net.minecraftforge.common.property`, and `BlockFenceGate` exposes only raw metadata statics (`isFenceGateOpen(int)`). The `IProperty`/`PropertyInteger` system arrived in 1.8. **This is simpler than assumed** — no properties to port, only metadata integers, and `BlockBush.canPlaceBlockOn(Block)` covers the placement rule the 1.21 `PlantBlockMixin` needed.
- 🔁 **139 blockstate JSONs** → metadata mapping table. Affected properties: `HORIZONTAL_FACING` (8 uses), `OPEN` (9), `AGE_2/3/4`, `SNOWY`, `ROTATION_16`, `ATTACHED`, `DOUBLE_BLOCK_HALF`, `BED_PART`, `WATERLOGGED`.
- ❌ Properties with no 1.7.10 home: `WATERLOGGED` (drop), `SNOWY`/`SpreadingSnowyDirtBlock` (no snowy dirt), dirt-path blocks → use Et Futurum's `BlockDirtPath` (see §4.15) so `ShovelItemHooks.addFlattenable` keeps working.
- 🔁 `Tags.can_not_connect` → 1.7.10 has no single "is exception for connection" method. The real hooks are **`BlockFence#canConnectFenceTo(IBlockAccess, x, y, z)`** and **`Block#canConnectRedstone(IBlockAccess, x, y, z, side)`** (line 2076) — both overridable, so `BlockMixin` becomes plain overrides in Vinery's own block classes (see §6).
- 🔁 Flammability (`FlammableBlockRegistry`) → `Block.setFireResistance`/`Block.setLightOpacity` at construction.
- 🔁 Fuel (`FuelRegistry`) → `GameRegistry.addFuel`.

### 4.3 Effects — 15 effects, `MobEffect` → `Potion`
- 🔁 `MobEffect` → `Potion` (+ `PotionEffect` for instances), registered namespaced via `Potion.registerPotion`.
- 🔁 `Holder<MobEffect>` (153 `Holder` references overall) → plain `Potion` references / int ids.
- 🔁 `Entity.hasEffect/addEffect/removeEffect` → `isPotionActive/addPotionEffect/removePotionEffect`.
- 🔁 Effects that grant attributes → `SharedMonsterAttributes` + `AttributeModifier` (1.7.10 has no armor/toughness attributes; armor mods go on `armorValues` by slot).

### 4.4 Items
- 🔁 `Item.Properties` (stacksTo, durability, food, rarity) → 1.7.10 `Item` ctor params + subclass overrides.
- 🔁 `FoodProperties` → `ItemFood` (fields for saturation/modifiers are hardcoded in 1.7.10) + `EntityPlayer.getFoodStats()`.
- 🔁 `ArmorMaterial`/`Holder<ArmorMaterial>` (14 custom armor pieces) → `net.minecraftforge.common.util.ArmorMaterial` + `ItemArmor`, custom durability/damage-reduction arrays.
- 🔁 Sign items → 1.7.10 `ItemSign` with sign-edit GUI.
- ❌ `HangingSignItem` → custom item + `ItemBlock` subclass (see §4.8).

### 4.5 Data components → NBT
- 🔁 `DataComponentRegistry` + `WineYearComponent` + `FoodComponent` (DFU `Codec`-based) → NBT tags on `ItemStack` (`NBTTagCompound`), read in item use/tick, synced automatically.
- ⚠️ `WineYearComponent` is a `record` → `@Desugar` or plain class.

### 4.6 Tile entities & containers
- 🔁 `AbstractContainerMenu`/`MenuType` → `IInventory` + `Container` + `IGuiHandler` (1.7.10 has `IGuiHandler` natively — this is a *simplification*).
  5 tile entities affected: `ApplePress`, `FermentationBarrel`, `Cabinet`, `DarkCherryBarrel`, `StorageBlockEntity`.
- 🔁 `ChestMenu` (9x2/9x3) → `ContainerChest` via `BlockChest`-style helper or a custom small container.
- ✅ `ImplementedInventory` interface concept carries over cleanly.

### 4.7 GUI
- 🔁 `Screen`/`GuiGraphics`/`Component` (2 GUIs: `ApplePressGui`, `FermentationBarrelGui`, plus 3 `Slot` subclasses) → `GuiScreen`/`GuiContainer` + `Gui.drawString/drawTexturedModalRect` + `StatCollector.translateToLocal`.
- 🔁 84 `Component.literal/translatable` callsites → `I18n`-style strings. (`Component` does not exist in 1.7.10.)

### 4.8 Signs & banners — **Et Futurum integration, one open decision**
- ✅ `BlockSign` + `BlockWallSign` exist in 1.7.10 vanilla → dark cherry standing/wall signs are cheap and need **no** Et Futurum dependency.
- ✅ **Banners: Et Futurum Requiem provides them** (`ganymedes01.etfuturum.blocks.BlockBanner`, `items.block.ItemBlockBanner`, `tileentities.TileEntityBanner`) **plus a public registration API**:
  ```java
  BannerPatternHelper.addPattern(String name, String id,
                                 String craftingTop, String craftingMid, String craftingBot)
  ```
  → Vinery's completionist banner becomes **N banner patterns** (crafted from 3 wine items) rather than its own banner blocks/items/tile entity/renderer. This **deletes 4 classes** (`CompletionistBannerBlock`, `CompletionistWallBannerBlock`, `CompletionistBannerEntity`, `CompletionistBannerRenderer`) and all banner blockstate/model work.
  - Caveat: with Et Futurum absent, that content simply does not exist (documented as an Et Futurum-dependent feature). Alternative if you want standalone banners: implement banner rendering ourselves (~2–3 days).
- ❌ **Hanging signs: confirmed absent from `GTNewHorizons/Et-Futurum-Requiem` too** (checked the GTNH fork directly: 761 Java files, 0 `HangingSign` matches). **Decision: stub for the initial release.** Keep the 1.21 classes and registration calls in the tree, clearly commented `// TODO(1.7.10): hanging signs not available in 1.7.10 or Et Futurum — implement or drop`, but don't register the blocks/items/renderers so nothing broken ships. Fully gated behind a single constant so it can be enabled later.
- ✅ Et Futurum's sign stack (`BlockWoodSign`, `ItemWoodSign`, `TileEntityWoodSign`, `GuiEditWoodSign`, `WoodSignOpenHandler`) is available if we want better-looking signs, but 1.7.10 vanilla `BlockSign`/`ItemSign` is sufficient and dependency-free → **use vanilla** for standing/wall signs.

### 4.9 Entities
| 1.21 entity                 | 1.7.10 plan                                                                                                               |
|-----------------------------|---------------------------------------------------------------------------------------------------------------------------|
| `TraderMuleEntity`          | 🔁 subclass `EntityHorse` (1.7.10 has horse armor + `HorseType` DONKEY)                                                   |
| `WanderingWinemakerEntity`  | 🔁 subclass `EntityVillager` + custom trades; 1.7.10 villagers are profession-based (`VillagerRegistry`)                  |
| `ChairEntity`               | 🔁 1.7.10 riding API (`EntityRiding`, `ridingEntity`, `forceRiding`) — `mountEntity` logic differs                        |
| `DarkCherryBoatEntity`      | 🔁 `EntityBoat` subclass; 1.7.10 boat types are fixed per class → 1 boat + N `EntityBoat` subclass types or palette trick |
| `DarkCherryChestBoatEntity` | 🟡 Et Futurum has chest boats (`ChestBoatRenderer`, `ChestBoatOpenInventory*`) → optional gated integration, or drop      |
| Fox (mixin only)            | ✅ **No code needed** — see §4.15 D-Mod integration                                                                       |

### 4.10 Worldgen
- 🔁 22 configured/placed feature JSONs + 10 biome-modifier JSONs → 2 `IWorldGenerator` implementations registered via `WorldGeneratorRegistry.registerGenerator` + `GameRegistry.registerWorldGenerator`, each gating on `BiomeGenBase` instances (plains/savanna/taiga/jungle/forest).
- 🔁 Tree features (`apple`, `cherry`) → hand-rolled placement (1.7.10 has no tree feature registry; vanilla `WorldGenTrees`/`ForestGenBase` patterns).
- 🔁 4 structure `.nbt` files → 1.7.10 schematic format (`structure/template` NBT with `blocks`/`entities`) and village/custom-structure integration. Likely needs a converter + manual placement in-editor.

### 4.11 Recipes
- 🔁 3 custom recipe types (`ApplePressMashing` ×2, `ApplePressFermenting` ×2, `FermentationBarrel` ×26 = **30 recipes**) → 3 `IRecipe` implementations + `CraftingManager.getInstance().addRecipe(...)`.
- 🔁 ~117 vanilla-shaped JSON recipes (planks, stairs, doors, ladders, wine racks, etc.) → generated `IRecipe`/`ShapedRecipes`/`ShapelessRecipes` registrations. Recommend a one-off Python/Java converter reading the origin JSONs and emitting a Java/Groovy registration source, kept in-repo for regeneration.
- ❌ Smithing/stonecutting recipes → drop (1.9+/1.14+).

### 4.12 Tags
- 🔁 30 tag JSONs → 1.7.10 Forge tags (`GameRegistry.registerTag` + `net.minecraftforge.common.tags.BlockTags/ItemTags`), so other 1.7.10 mods (e.g. Forestry, Railcraft interop) can see them.
- ⚠️ 1.7.10 tag names are the **uppercase enum** (`BlockTags.LOGS`), so Vinery tags become e.g. `VINERY_LATTICE`.

### 4.13 Advancements / loot / progression
- ❌ 70 advancement JSONs → 1.7.10 has achievements only (`AchievementPage` + custom `Achievement`). Recommendation: drop, or ship a small custom achievement page if you care about progression.
- ❌ 108 per-block loot tables → drop (1.7.10 block drops are hardcoded in `Block.getDrops`).
- ❌ `completionist` advancement loot → drop.
- ✅ **Compostables: Et Futurum integration** (1.7.10 vanilla has no composter at all; it was added in 1.8). Replace the NeoForge `data_maps/item/compostables.json` + `CompostableRegistry` with:
  ```java
  // ganymedes01.etfuturum.api.CompostingRegistry  (AvailableSince 2.4.4)
  CompostingRegistry.registerCompostable(Object itemOrOreDictName, int percent); // 1..600
  ```
  Accepts `ItemStack` / `Item` / `Block` / OreDict `String`, so Vinery's existing compostables JSON maps over almost 1:1 (just `percent` values and ore-dict strings).
- 🔁 `AxeItemHooks.addStrippable(...)` (4 calls) → Et Futurum `StrippedLogRegistry.addLog(Block from, int fromMeta, Block to, int toMeta)` (AvailableSince 2.3.0, handles pillar rotation states automatically); or Forge 1.7.10's own `IBlock`/`removeBlock` + `ItemAxe` event handling if Et Futurum is absent.

### 4.14 Compat
- ❌ Delete REI (6 files: `core/compat/rei/**`, `forge/core/rei/**`) — REI does not exist for 1.7.10.
- 🔁 Rewrite JEI plugin (`VineryJEIPlugin` + 3 categories + transfer) against **mezz.jei 1.7.10** API (`IGuiHandler` era, no `RecipeIngredient`, no `RecipeType` registry).
- 🔁 Optional NEI support if desired.

### 4.15 Optional integrations (new architecture)

Two target mods already expose the exact extension points Vinery needs, so we **integrate instead of reinventing**. Rule: **Vinery must work standalone**; integrations are gated, additive, and never required.

| Mod                                                                                                                                                                                                            | API used                                             | Replaces                                  |
|----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|------------------------------------------------------|-------------------------------------------|
| **Et Futurum Requiem** (`etfuturum`, pkg `ganymedes01.etfuturum`, **use the GTNH fork `GTNewHorizons/Et-Futurum-Requiem`**, parent `Roadhog360/Et-Futurum-Requiem`, 761 Java files; Modrinth slug `etfuturum`) | `CompostingRegistry.registerCompostable`             | NeoForge compostable data map             |
|                                                                                                                                                                                                                | `BannerPatternHelper.addPattern`                     | own banner blocks/TE/renderer             |
|                                                                                                                                                                                                                | `BlockDirtPath`                                      | dirt-path flattening feature              |
|                                                                                                                                                                                                                | `StrippedLogRegistry.addLog`                         | Architectury `AxeItemHooks.addStrippable` |
|                                                                                                                                                                                                                | chest boat classes/renderers                         | own chest boat entity                     |
| **D-Mod** (`dmod`, pkg `makamys.dmod`, src `makamys/DMod`)                                                                                                                                                     | `Compat.registerBerryBushHandler(IBerryBushHandler)` | `FoxEntityEatSweetBerriesGoalMixin`       |

**Implementation pattern** (Forge 1.7.10 idiomatic):
```
com.mrfuzzihead.vinery.compat.etfuturum.EtFuturumCompat        // guarded by Loader.isModLoaded("etfuturum")
com.mrfuzzihead.vinery.compat.etfuturum.VineryBerryBushHandler // D-Mod's IBerryBushHandler impl
com.mrfuzzihead.vinery.compat.dmod.DModCompat
com.mrfuzzihead.vinery.compat.CompatLoader                     // single entry point, dispatches by modid
```
- Guard every call site with `Loader.isModLoaded(...)`; annotate integration classes with `@cpw.mods.fml.common.Optional.Interface("etfuturum")` so FML never loads them when the mod is absent.
- **Compile-time deps** (dev-only, not published):
  ```groovy
  dependencies {
      devOnlyNonPublishable(rfg.deobf("curse.maven:et-futurum-<cfid>:<fileid>"))
      devOnlyNonPublishable(rfg.deobf("curse.maven:d-mod-<cfid>:<fileid>"))
  }
  ```
  (`includeWellKnownRepositories = true` already adds CurseMaven/Modrinth.) Pin exact IDs once the dev jars are chosen. Both mods use UniMixins, same as us — no conflict expected.
- **Also useful**: Vinery's chest-boat/mule/winemaker and any Et Futurum modern-wood feature should register into Et Futurum's tags where sensible so pack interop works.

### 4.16 Misc
- 🔁 `ResourceLocation.fromNamespaceAndPath` → `new ResourceLocation(...)` (1.7.10 has `ResourceLocation` ✅).
- ✅ `SoundEvent` registry → not needed; 1.7.10 `sounds.json` + `playSound` (already present in resources).
- ❌ `AddPackFindersEvent` “bushy leaves” pack → no discovery mechanism in 1.7.10, **and** its content is 1.21 model/blockstate JSON that 1.7.10 cannot read even if a player copies it into their resource packs. Parked in `src/port-holding/resources/` pending re-implementation as a config-gated texture swap (`IIconHandler`/ISBRH) on the leaves blocks.
- 🔁 `commands` → 1.7.10 `ICommand` (`net.minecraft.command.CommandBase`).

### 4.17 Access widener → 1.7.10 access transformer translation

The 1.21 `vinery.accesswidener` was a useful checklist of encapsulation walls. Access *transformers* (1.7.10's equivalent, via `accessTransformersFile`) are **almost certainly unnecessary**, because RFG's dev workspace is already MCP-named and 1.7.10's equivalents are public. Translation:

| 1.21 access widener entry                              | 1.7.10 equivalent                                                             | AT needed? |
|--------------------------------------------------------|-------------------------------------------------------------------------------|------------|
| `MobEffectInstance.amplifier` / `.duration`            | `PotionEffect#getAmplifier()` / `#getDuration()` are public                   | ❌ no      |
| `PoiTypes.TYPE_BY_STATE`                               | 1.7.10 POI system is entirely different (`net.minecraft.entity.ai.village.*`) | ❌ n/a     |
| `VillagerTrades$EmeraldForItems` / `$ItemsForEmeralds` | 1.7.10 `MerchantRecipeList`                                                   | ❌ no      |
| `SpreadingSnowyDirtBlock.canBeGrass`                   | dropped (no snowy dirt)                                                       | ❌ n/a     |
| `WoodType.register`                                    | 1.7.10 `BlockWood` enum, registered by FML                                    | ❌ no      |
| `FireBlock.setFlammable`                               | 1.7.10 `Block#setFireResistance` + `BlockFlammable`                           | ❌ no      |

**Conclusion: leave `accessTransformersFile` empty.** Revisit only if a compile error reports a genuinely inaccessible 1.7.10 field.

### 4.18 1.7.10 traps learned the hard way (Phase 1)

Every one of these was hit in a real `runServer` boot or caught by `tools/verify-registrations.mjs`, not inferred.

**Why that tool exists:** a dedicated server loads classes, registries and NBT but *never* textures or
lang files. A green `runServer` says nothing about whether
`setBlockTextureName("vinery:dark_cherry_planks")` points at a file that exists — and 1.7.10 fails
**silently** in both cases (placeholder texture / raw key, no log line).
`tools/verify-registrations.mjs` cross-checks every registration against the real PNG and
`en_US.json`, and should be re-run after any registry change.

| Trap | Symptom | Correct handling |
|---|---|---|
| Texture directory names | Missing-texture placeholder, **no log line at all** | 1.7.10 uses plural directories: `textures/blocks/`, `textures/items/`, `textures/entities/`. 1.21 uses singular `block/`, `item/`, `entity/`. All three renamed. `mob_effect/` and `gui/` are unchanged. |
| Lang locale filename case | Files silently ignored | Locale codes are matched case-sensitively: `en_us.json` -> `en_US.json`. Handled by `tools/convert-lang-1-7-10.mjs`. |
| Slab item collision | `IllegalStateException: Can't free registry slot N occupied by ItemBlock` | Register the half slab with a `null` `ItemBlock` class so the single `ItemSlab` can take that slot — exactly what vanilla does for `stone_slab`. |
| `requiredMods` in `mcmod.info` | `MissingModsException: UniMixins` | Mod ids are lower case: `unimixins`, not `UniMixins`. |
| Mixin listed but not compiled | `InvalidMixinException: The specified mixin ... was not found` at mod construction | Only add a name to `Mixins` once its class compiles (§6). |
| Empty `MixinBuilder` | `IllegalArgumentException: No mixin class registered for IMixins` | Placeholder entries are illegal; a group appears only when fully ported. |
| `GameRegistry` return types | Compile errors | `registerBlock` returns erased `Block`; `registerItem` and `registerTileEntity` return `void`. Wrapped in `VineryRegistry`. |
| `BlockRotatedPillar` API | Compile error | 1.7.10 splits icons into `getTopIcon(int)` / `getSideIcon(int)` rather than a side parameter. |
| Shaped recipe rows must match in width | `StringIndexOutOfBoundsException` at load | `CraftingManager.addRecipe` concatenates rows and indexes with the **last** row's length, so every row needs the same width (3 for vanilla-style shapes). |
| `BlockFence` side textures are hardcoded | Fence sides render with vanilla's texture | 1.7.10 `RenderBlocks` uses `TextureBlocks.fence` directly, ignoring the block's icon. Mod fences need a custom ISBRH (Tier 4) — the dark cherry fence is deferred to that batch. |
| `Block.canPlaceBlockOn` replaces a mixin | — | Where 1.21 needed `PlantBlockMixin`, 1.7.10's `BlockBush` exposes `canPlaceBlockOn(Block)`. Confirms the §6 removal. |
| `new ItemStack(block)` on an itemless block | **Creative tab crash** (NPE inside NEI/JEI) | A block registered with a `null` ItemBlock has no item form — every slab pair here. `new ItemStack(block)` then yields a stack whose item is null: vanilla renders it, NEI/JEI crash stringifying it. Enforced by `tools/verify-registrations.mjs`. |
| **Forge 1.7.10 only loads `.lang` files** | Names stayed untranslated even with correct keys | `LanguageRegistry` scans jar entries with `assets/(.*)/lang/([w_-]+).lang` — an `en_US.json` in the jar is silently ignored. The converter now emits `.lang`. |
| **`getLightBrightnessForSkyBlocks` is packed** | Every face rendered at full white | It returns `(sky << 20) | (block << 4)` — not a 0-15 or 0-240 brightness. Unpack `>> 20 & 15` and `& 15` and divide each by 15 before shading. |
| **`getCollisionBoundingBoxFromPool` already offsets** | Blocks had no collision | Vanilla builds the box as `x + minX, y + minY, z + minZ ...`, and `addCollisionBoxesToList` compares it against the entity mask *without* offsetting again. An override returning an unoffset `0,0,0-1,1,1` puts the volume at the world origin. Do not override it. |
| **The 6th `renderWorldBlock` argument is not the block metadata** | Block always faces one direction | Forge's `RenderingRegistry#renderWorldBlock` looks the handler up by its `modelId` and then passes that same id into the metadata slot. Read the real value with `world.getBlockMetadata(x, y, z)`. The interface parameter name says `metadata`, which is what makes it a trap. |
| **Player yaw is negative** | Facing wrong for roughly half of all angles | `placer.rotationYaw` is routinely `-177.6`, `-360.45`. `(int)` **truncates toward zero** where vanilla uses `MathHelper.floor_double`, so `-1.473` becomes `-1` instead of `-2` — and `-1 & 3 == 3`. Copy `BlockFurnace#onBlockPlacedBy` exactly. |
| **`onBlockPlacedBy` also fires client-side** | A wrong facing for a frame | The client gets the callback with an unnormalised yaw and writes metadata the server then overwrites. Guard with `if (!world.isRemote)`. |
| **A 180° rotation is its own inverse** | Rotation bug survives testing | 180° (`facing=2`) looks correct even when the 90° cases are swapped, so testing one direction proves nothing. Test a **quarter turn**. |
| **Overriding `registerBlockIcons` loses `blockIcon`** | Block invisible in world *and* missing-texture in inventory | Vanilla's `Block#registerBlockIcons` assigns `this.blockIcon`; overriding it to fill a per-face table must set `blockIcon` too, because `getIcon` and `getBlockTextureFromSide` both resolve through it. A `null` icon draws nothing and renders as the missing-texture square. |
| **Never early-return from a block renderer** | Block draws once, then vanishes | Chunks re-render on every block change, so a one-shot debug guard that returns early makes the block appear for one frame only. Log once, always render. |
| **ISBRH runs inside an open Tessellator session** | `IllegalStateException: Already tesselating!` | Forge calls `renderWorldBlock` from `RenderBlocks#renderBlockByRenderType` **mid-batch**, so a renderer must only append vertices — calling `startDrawing` throws. The inventory path is the opposite: `RenderBlocks#renderInventoryBlock` runs after every vanilla `startDrawing/draw` pair has closed, so it owns its own session. |
| **Vertices need explicit normals** | Garbage/blocky lighting | Even with per-face vertex colours, GL lighting is on during the block pass, so `Tessellator#setNormal` must be called per face as `RenderBlocks` does. |
| **Custom render id `-1` draws nothing** | Placed blocks invisible | 1.7.10 does not use `-1` for "custom handler": `RenderBlocks#renderBlockByRenderType` returns false for `-1` before reaching Forge's handler map. Blocks must return an id from `RenderingRegistry#getNextAvailableRenderId()`, which is client-only, so the block holds it and the client proxy assigns it. |
| **Unlocalized names are null by default** | Everything showed as raw keys / nothing localised | 1.7.10 never calls `Block.setBlockName`, and `getUnlocalizedName()` is `"tile." + unlocalizedName` — so it returned `tile.null`. `VineryRegistry` now calls `setBlockName(MOD_ID + "." + name)` / `setUnlocalizedName(...)`, which matches the converted lang keys exactly. |
| A block with no `setBlockTextureName` has a null `blockIcon` | Item invisible in the creative inventory | `Block#getIcon` returns `blockIcon`, and `ItemBlock` uses that for its own icon. Custom-rendered blocks still need one texture for the item form; `VineryCheckableBlock` lets the self-test assert it. |
| Mod translations cannot be resolved server-side | False positives in the self-test | Mod languages load into StatCollector's locale-dependent translator, which a dedicated server never populates; its fallback reads only minecraft's hardcoded `en_US.lang`. Key coverage stays in `tools/verify-registrations.mjs`. |
| `CreativeTabs.displayAllReleventItems` does not exist server-side | `NoSuchMethodError` on a dedicated server | Client-only despite living in common code. Creative-tab contents therefore <b>cannot</b> be checked by the self-test — the verifier does it at source level instead. |
| Blocks register before items | `NullPointerException` at load | A block that captures an `Item` in its constructor sees `null`, because `VineryItems` is initialised after `VineryBlocks`. Resolve the item lazily inside the method that uses it (`GrapeBushBlock.ripeDrop()`). |
| **A dev dependency can break the launch, not the code** | `runServer` fails with `An error occurred trying to configure the minecraft home` — which reads like a mod-registration bug, but is not | A dependency that **bundles its own Mixin** (DMod) sorts before `unimixins-*.jar` alphabetically, so UniMixins' sanity check throws `java.lang.Error: A different version of Mixin …` and FML aborts before any mod code runs. Read the *first* stack trace in the log, not the Gradle failure at the end. |
| **A wood's texture names are not derivable from the wood** | Missing or magenta textures when batching variants | The 1.21 models disagree with the obvious naming: acacia's rack frame uses `acacia_drawer_side` rather than a cabinet texture, and cherry's carry a `_pink` suffix. Read each model's `textures` block instead of constructing names. |
| **Ten variants of one shape need no renderer work** | Tempting to register ten render ids | Forge dispatches on `getRenderType()` but passes the real `Block` to the handler, so every rack shares one id and one handler and selects geometry through `WineRackBlock#geometry()`. |
| **A loop in the registry file silently escapes verification** | Fewer checks than you think | `tools/verify-registrations.mjs` finds registrations by regex over the source. A `for` loop registering ten blocks drops all ten out of checking, which is why the ten big racks are written out one call per line. |
| Registry names are not namespaced | FML "illegal extra prefix" warning | `GameData` prefixes with the mod id itself (`dark_cherry_planks` -> `vinery_dark_cherry_planks`). Never put a colon in a registry name; use explicit `vinery:<path>` texture names instead. |

---

## 5. Rendering — **vanilla path first, then vanilla's own cube renderer for boxes**

### 5.1 What the assets actually contain (measured, not assumed)

| Bucket                                                                                                                                                                                                                                                                                                          | Count                               | 1.7.10 path                                                      |
|-----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|-------------------------------------|------------------------------------------------------------------|
| Vanilla geometry families (`cube_all`, `cube_column`, `cube_column_horizontal`, `cube_bottom_top`, `slab`, `slab_top`, `stairs`/`inner`/`outer`, `fence_post`, `template_fence_gate*`, `door_*` ×8, `template_trapdoor_*`, `button*`, `pressure_plate_*`, `template_glass_pane_*`, `cross`, `flower_pot_cross`) | ~70 of 139 blockstates              | ✅ **subclass the vanilla 1.7.10 block class, swap textures**    |
| Flat item sprites (`item/generated`)                                                                                                                                                                                                                                                                            | 82 of 168 item models               | ✅ plain `Item` + `setTextureName`                               |
| Item models that are really *block* models (lattice, wine racks, bags, slabs, stairs, barrel, chair…)                                                                                                                                                                                                           | 86 of 168 item models               | ✅ `ItemBlock` renders via the block's own renderer              |
| Reusable decorative templates (`template_lattice` ×10, `template_wine_rack_{1,2,3_closed,3_open}` ×40, `template_small_wine_bottle` ×6)                                                                                                                                                                         | 56 models, **6 shapes**             | ✅ axis-aligned shapes → §5.3 cube path                             |
| Hand-shaped block models (apple press, barrel, cabinet, drawer, table, shelf, chair, window, storage pot, stackable log, wine box, signs)                                                                                                                                                                       | 82 distinct inline geometries total | 🔁 axis-aligned → §5.3; rotated elements → §5.4 |
| Entity models (mule, winemaker, 4 armour layers, straw hat, boat)                                                                                                                                                                                                                                               | ~6                                  | 🔁 hand-written `ModelBase`/`ModelRenderer` cuboid models        |
| Wine bottle shapes (one per wine, 2–3 textures each)                                                                                                                                                                                                                                                            | ~20                                 | 🟡 generated OBJ, or one parameterised bottle renderer           |

**Key finding: no item needs hand-modeled 3D geometry.** Every non-flat item model is a block, and 1.7.10 already renders blocks in item form. This removes the biggest cost from the original estimate.

### 5.2 Recommendation (fidelity + portability)

**Do not build a general JSON→OBJ converter. Use a targeted one, and put the bulk of blocks on the vanilla path.**

| Tier                                | Content                                                                                                                                                                               | Why it wins                                                                                                                                                                                                                                                                                                                                                                                                                                                                |
|-------------------------------------|---------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| **1 — Vanilla path**                | dark cherry/wood set: planks, slabs, stairs, fence, fence gate, door, trapdoor, button, pressure plate, log, stripped log/wood, leaves, big table, window, glass panes, cross-sprites | Subclassing `BlockWoodSlab`, `BlockStairs`, `BlockFence`, `BlockFenceGate`, `BlockDoor`, `BlockTrapDoor`, `BlockButton`, `BlockPressurePlateWeighted`, `BlockRotatedPillar`, `BlockNewLeaf`, `BlockPane`, `BlockBush` — all verified present in 1.7.10 sources. Gives correct lighting, mipmaps, biome tint, breaking animation, hand rendering, **FTB texture-pack replacement**, and shader compatibility *for free*. This is exactly how every 1.7.10 wood mod does it. |
| **2 — Flat items**                  | 82 wine/food/seed/armour sprites                                                                                                                                                      | `setTextureName` only. Max portability.                                                                                                                                                                                                                                                                                                                                                                                                                                    |
| **3 — Block-as-item**               | 86 block items                                                                                                                                                                        | `ItemBlock`; no extra renderer.                                                                                                                                                                                                                                                                                                                                                                                                                                            |
| **4 — Simple custom blocks**        | apple press, fermentation barrel, cabinet, drawer, table, shelf, chair, storage pot, stackable log, wine box                                                                          | Hand-written `ISBRH` built from `RenderBlocks` primitives (`renderStandardBlock`, `renderCrossedSprites`) so vanilla lighting/mipmaps/particle behaviour is retained. ~10 renderers.                                                                                                                                                                                                                                                                                       |
| **5 — Decorative/rotated geometry** | `template_lattice` (has 45° braces), 4 wine-rack shapes, wine bottles                                                                                                                 | **Generated OBJ** from the existing JSON (exact geometry, mechanical conversion — no eyeballing) + **one** shared renderer.                                                                                                                                                                                                                                                                                                                                                |
| **6 — Entities**                    | ~6 models                                                                                                                                                                             | Hand-written `ModelBase`/`ModelRenderer`.                                                                                                                                                                                                                                                                                                                                                                                                                                  |

Why this is the *portable* choice: 70 of 139 blockstates never touch a custom renderer, and tier 4 delegates rendering back to vanilla rather than hand-issuing GL. Nothing that breaks under Fast Render, OptiFine/shaders, FTB texture packs, or NEI/JEI item rendering.

**Revised after the wine rack (2026-10-02):** the original recommendation put rotated and decorative geometry on a generated-OBJ renderer and assumed a hand-rolled `ISBRH` for tier 4. The wine rack showed the hand-rolled version needs no fewer than four preconditions to be correct (open Tessellator batch, block position baked into vertex coordinates, explicit atlas bind, unpacked light value) — each of which fails silently. Delegating to `renderStandardBlock` removes all four. **The OBJ tier is now deferred until a shape genuinely cannot be expressed as axis-aligned boxes**, which may never happen for the racks and lattice; the small rack's 45° braces are the first known case.

### 5.3 Drawing axis-aligned boxes (proven — the wine rack)

**Render through vanilla's own cube renderer. Do not hand-issue quads.**

```java
for (Box box : boxes) {
    renderer.overrideBlockTexture = iconFor(box);          // per-box texture, may be null
    renderer.setRenderBounds(box.minX, box.minY, box.minZ, box.maxX, box.maxY, box.maxZ);
    renderer.renderStandardBlock(block, x, y, z);
}
renderer.clearOverrideBlockTexture();
```

That is the whole custom renderer. It is what TFC's `RenderPottery` does, and it hands
coordinates, lighting, ambient occlusion, mipmapping and atlas binding back to vanilla.

Requirements and traps when using it (all learned the hard way):

| Requirement | Why |
|---|---|
| **Set `blockIcon` in `registerBlockIcons`** | Vanilla's version does `this.blockIcon = reg.registerIcon(getTextureName())`. Overriding it to fill a per-face table silently leaves `blockIcon` null, and both `getIcon` and `getBlockTextureFromSide` resolve through it — a null icon draws nothing and renders as the missing-texture square. |
| **Get facing from `world.getBlockMetadata(x, y, z)`** | Forge's `RenderingRegistry#renderWorldBlock` passes its own `modelId` into the interface's `metadata` slot, because that is the key it just looked the handler up with. |
| **`shouldRender3DInInventory` must return false** | `renderStandardBlock` resolves lighting from `RenderBlocks#blockAccess`, which is only set during world rendering. Calling it from a GUI throws `NullPointerException` in `getMixedBrightnessForBlock`. The item uses its flat icon instead. |
| **Apply facing by rotating the render bounds** | `renderStandardBlock` only draws axis-aligned boxes, so rotate the bounds, not the vertices. Clockwise 90° seen from above maps `(x, z) → (1 - z, x)`; 180° is its own inverse, so it looks correct even when the 90° cases are wrong — test a quarter turn, not a half turn. |
| **Never early-return from the handler** | Chunks re-render on every block change, so a one-shot debug guard that returns early makes the block appear for one frame and then vanish. |

### 5.4 What stays deferred: rotated elements

Shapes containing elements rotated off-axis — the lattice's 45° braces, the small wine rack's
angled supports — cannot be expressed as axis-aligned boxes. **Nothing in the port needs this yet**,
so no OBJ converter is planned. When the first such shape arrives:

- Input: just that template (keep the other ~250 block models as the reference/intent source).
- Must handle: `#texture` variable indirection, per-face `rotation: 90/180/270` (bake into UVs),
  `from`/`to`/`rotation` → OBJ verts, UV scaling for non-16px textures (`texture_size: [80,80]` on
  the lattice).
- The stock OBJ loader **ignores `mtllib` and `usemtl`** — `GroupObject` has no texture field — so
  multi-texture models need one `g <textureKey>` group per texture plus a small `ICustomRenderer`
  that binds each group via `WavefrontObject#tessellateOnly(Tessellator, String...)`.
- Run it as an offline dev tool (or a Gradle task); commit the generated `.obj`/`.mtl`.
- The 1.21 JSONs stay in the repo as reference but are excluded from the released jar (~2.5 MB)
  via `jar { exclude }` in `build.gradle.kts`.

### 5.5 Revised effort

| Work                                                          | Estimate  | Status                                    |
|---------------------------------------------------------------|-----------|-------------------------------------------|
| Tier 1 vanilla block wiring (~20 blocks)                      | 2–3 days  | ~14 blocks done                           |
| Tier 2/3 item wiring (~168 items)                             | 1 day     | ~11 items done                            |
| Tier 4 box rendering via vanilla's cube renderer (~10 shapes) | 1–2 days  | 1 shape done (big wine rack, verified)    |
| Tier 5 OBJ converter                                          | deferred  | not needed until a rotated shape appears   |
| Entity models + renderers                                     | 2–3 days  | not started                               |
| **Total**                                                     | **~2 wks**| |

---

## 6. Mixins (14 → 7), UniMixins early/late via GTNHLib `gtnhmixins`

### 6.1 Architecture

`com.gtnewhorizon.gtnhmixins` (shipped **inside UniMixins 0.2.1** — no extra dependency) splits mixins by *load phase* instead of hand-maintaining two config files:

| Phase   | Loader                                                | Config package  | For                                                              |
|---------|-------------------------------------------------------|-----------------|------------------------------------------------------------------|
| `EARLY` | `EarlyMixinsLoader` (an `IFMLLoadingPlugin` core mod) | `…mixins.early` | Minecraft, Forge, **and Vinery's own classes**                   |
| `LATE`  | `LateMixinsLoader` (`@LateMixin`)                     | `…mixins.late`  | third-party mod classes, gated by `addRequiredMod(TargetMods.X)` |

- `Mixins` enum = every mixin, grouped by concern, tagged `Phase.EARLY`/`Phase.LATE`. Names are **short names resolved against the config's `package`** — e.g. `"BlockMixin"`, or `"dmod.FoxEntityEatSweetBerriesGoalMixin"` for a late mixin living in a per-mod subpackage.
- `TargetMods` enum = `ITargetMod` entries (`DMOD`, `ET_FUTURUM`) so a mod-targeted mixin applies only when its target actually loaded.
- `gradle.properties`: `usesMixins = true`, `mixinsPackage = mixins`, `coreModClass = mixins.EarlyMixinsLoader`, `mixinPlugin` stays empty. Configs: `mixins.vinery.json` + `.early.json` + `.late.json`.

### 6.2 Per-mixin disposition

> **Correction (audit pass 2):** the previous draft claimed the four armor mixins target `ItemArmor`. They actually target **Vinery's own item classes** (`WinemakerBootsItem`, `WinemakerChestItem`, `WinemakerHelmetItem`, `WinemakerLegsItem`) to supply custom armor models/textures. Because that rendering code (`IClientItemExtensions`, `HumanoidModel`, `ArmorRegistryClient`) is client-only, they are **client-side early mixins**, not common.

| Mixin (1.21 target)                                           | Phase / side                                  | 1.7.10 handling                                                                   | Stays a mixin? |
|---------------------------------------------------------------|-----------------------------------------------|-----------------------------------------------------------------------------------|----------------|
| `BlockMixin` (`Block#isExceptionForConnection`)               | early / common                                | override `BlockFence#canConnectFenceTo` + `Block#canConnectRedstone`              | ❌ no          |
| `PlantBlockMixin` (`BushBlock#mayPlaceOn`)                    | early / common                                | 1.7.10 **has `BlockBush`** (+ Forge `IPlantable`); put the check in our own block | ❌ no          |
| `SpreadingSnowyDirtBlockMixin`                                | early / common                                | no snowy dirt in 1.7.10                                                           | ❌ delete      |
| `ShovelItemMixin` (flattenable / dirt paths)                  | early / common                                | Et Futurum `BlockDirtPath` + our own `ItemSpade`                                  | ❌ likely no   |
| `BoneMealItemMixin` (`BoneMealItem#useOn`)                    | early / common                                | mixin `ItemBoneMeal#applyBonemeal`                                                | ✅             |
| `LivingEntityMixin`                                           | early / common                                | mixin `EntityLivingBase`                                                          | ✅             |
| `ExperienceOrbMixin` (`playerTouch`)                          | early / common                                | **`PlayerPickupXpEvent`** handler (cancelable, fired before XP is granted)        | ❌ no          |
| `WanderingTraderManagerMixin` (`WanderingTraderSpawner`)      | early / common                                | no wandering trader anywhere in 1.7.10 → our own spawn routine                    | ❌ no          |
| `ClientPlayerEntityMixin` (`LocalPlayer`)                     | early / **client**                            | mixin `EntityPlayerSP`                                                            | ✅             |
| `Boots/Chestplate/Helmet/LeggingsItemMixin` (our own items)   | early / **client**                            | 1.7.10 equivalent is `ItemArmor#getArmorTexture` + `RenderBiped` armour layers    | ✅ 4           |
| `FoxEntityEatSweetBerriesGoalMixin` (`Fox.FoxEatBerriesGoal`) | **late** / common, requires `TargetMods.DMOD` | or drop entirely for D-Mod's `IBerryBushHandler` API (§4.15)                      | ➕ late        |

**Net: 14 → 7** (6 early + 1 late) — a 50% cut in mixin surface, which matters because mixins are the main source of port fragility on 1.7.10.

---
## 7. Work breakdown

| Phase                    | Content                                                                                                                                                                | Rough effort                 |
|--------------------------|------------------------------------------------------------------------------------------------------------------------------------------------------------------------|------------------------------|
| **0. Repo hygiene** | Package/import migration, real `mcmod.info`, `pack_format: 1`, mixin wiring | ✅ done |
| **1. Skeleton compiles** | `@Mod` class, registry shim, first content slice, lang conversion — **verified booting on a dedicated server** | ✅ done (4/204 blocks) |
| **2. Core content** (in progress: 36/204 blocks, 7 items; all 10 big wine racks done) | 37 block classes + 15 item classes + effects → `Potion`, tile entities, NBT components, no rendering (debug models)                                                    | 1.5–2 weeks                  |
| **3. Containers & GUI**  | 5 `IGuiHandler`s, 2 `GuiScreen`s, slot classes                                                                                                                         | 3–5 days                     |
| **4. Entities**          | mule, winemaker, chair, boat                                                                                                                                           | 3–5 days                     |
| **5. Worldgen**          | grape/tree generators, structures (if kept)                                                                                                                            | 2–4 days                     |
| **6. Rendering**         | Vanilla-path block wiring → flat items → ~10 ISBRH → targeted OBJ converter + 1 shared renderer → entity models (§5)                                                   | 2–3 weeks, mostly mechanical |
| **7. Recipes & compat**  | recipe converter + custom recipe classes, tags, config, **Et Futurum / D-Mod integration** (§4.15), remaining mixins (§6), JEI                                         | 1 week                       |
| **8. Polish**            | lang cleanup (16 files), sounds, textures, `mcmod.info`, CI, publishing                                                                                                | 2–3 days                     |

Total ≈ **6–9 weeks** now that rendering is scoped (§5, ~2–3 weeks and mostly mechanical) and the mixin surface is cut by half. The spread is driven by the remaining open questions in §8 (structures, advancements, standalone banners add or remove ~1–2 weeks).

---

## 8. Open questions for you

### ✅ Resolved since last update
- **Architectury** — dropped entirely, Forge 1.7.10 native only.
- **Assets** — restored and verified in the working tree (1,339 resource files).
- **Banners / composter / dirt paths / chest boats / foxes** — resolved via Et Futurum + D-Mod integrations (§4.15), not reinvention.
- **Hanging signs** — verified absent from the GTNH Et Futurum fork as well; **stubbed/TODO** for the initial release (§4.8).
- **Rendering** — vanilla-path-first hybrid recommended (§5); no general JSON→OBJ converter needed.

### Still open
1. **Chest boat** — gate behind Et Futurum, or drop entirely?
2. **Banners without Et Futurum** — accept that completionist banners only exist when Et Futurum is installed (recommended), or implement a standalone banner system?
3. **Wandering winemaker spawning** — confirm the desired semantics now that we control it directly (config already has `TRADER_SPAWN_CHANCE`, `TRADER_SPAWN_DELAY`, `SPAWN_WITH_MULES`).
4. **Advancements** (70) — drop, or port to a custom `AchievementPage`?
5. **Structures** (4) — port the schematic data, or defer to a later phase?
6. **JEI only, or JEI + NEI?** NEI is already a runtime dev jar in your deps.
7. **Package identity** — keep `com.mrfuzzihead.vinery`, or return to upstream's `net.satisfy.vinery` (affects mixin config, save compatibility, and any mod depending on Vinery)?
8. **Publishing targets** — Modrinth / CurseForge / GTNH Maven? (properties currently empty.)

---

## 9. Immediate next steps (once questions answered)

1. ✅ Phase 0 hygiene (package fix, `mcmod.info`, `pack_format: 1`, mixin wiring).
2. ✅ Mixin bootstrap compiling and loading as a coremod at runtime.
3. ✅ `VineryRegistry` + `VineryBlocks`/`VineryItems`, dark cherry planks/log/slab, lang conversion.
4. **Next: Phase 2** — port `ObjectRegistry`'s 204 entries. `RegistrySupplier<X>` becomes a plain static
   field on `VineryBlocks`/`VineryItems`; `registerBlock(...)`/`registerItem(...)` become `VineryRegistry`
   calls. Start with the rest of the dark cherry wood set (leaves, stairs, fence, fence gate, door,
   trapdoor, button, pressure plate) because those validate the whole Tier 1 rendering strategy before
   any custom ISBRH work begins. Re-run `tools/verify-registrations.mjs` after each batch.
5. **Next: the remaining wine rack shapes.** The big rack (9 axis-aligned boxes) renders correctly in
   all four facings, and now exists in all ten woods (one shape, ten texture pairs — no extra renderer
   work, because every rack shares a single render id and supplies its own geometry via
   `WineRackBlock#geometry()`). Two shapes remain:
   - *mid* → `template_wine_rack_3_closed` / `_3_open` (2 and 6 boxes, no rotations) — note
     `_3_closed` contains an element with **inverted bounds** (`from [15,1,0]` > `to [1,15,15]`), so
     confirm the intended shape before transcribing it.
   - *small* → `template_wine_rack_2` (7 boxes, **two at 45°**) — the first case that genuinely
     cannot be expressed as axis-aligned boxes (§5.4). Decide OBJ vs. approximation then.
   Then the 11 remaining wood variants, which are texture-only repeats of whichever shapes exist.

---

## 10. Evidence appendix

What was **verified** (checked against source/registry, not assumed):

| Claim                                                                                                                               | How verified                                                                                              |
|-------------------------------------------------------------------------------------------------------------------------------------|-----------------------------------------------------------------------------------------------------------|
| Jabel supports switch expressions, pattern `instanceof`, `var`, `@Desugar` records → Java 8 bytecode                                | Compiled probes through this repo's RFG build; `javap` shows major version 52; executed on `jdk1.8.0_202` |
| Jabel rejects un-annotated records and `Stream.toList()`                                                                            | Compiler output: `error: Must be annotated with @Desugar` / `cannot find symbol: method toList()`         |
| No Architectury for 1.7.10                                                                                                          | `maven.architectury.dev/dev/architectury/architectury-forge/` listing + Modrinth API query                |
| 1.7.10 has no JSON model/blockstate system                                                                                          | `net/minecraftforge/client/model/` contains only `IModelCustom`, `IModelCustomLoader`, `obj/`, `techne/`  |
| OBJ loader ignores `mtllib`/`usemtl`; `tessellateOnly(groupNames)` exists                                                           | Read `WavefrontObject.java` + `GroupObject.java` from patched sources                                     |
| Vanilla 1.7.10 block classes for slabs/stairs/fences/doors/pane/pressure plate                                                      | Class listing from `mcp_patched_minecraft-sources.jar`                                                    |
| `BlockBush` + Forge `IPlantable` exist                                                                                              | `net/minecraft/block/BlockBush.java`                                                                      |
| `EntityXPOrb#onCollideWithPlayer` fires cancelable `PlayerPickupXpEvent`                                                            | `net/minecraft/entity/item/EntityXPOrb.java` line ~236                                                    |
| `Block#canConnectRedstone` (line 2076) + `BlockFence#canConnectFenceTo`                                                             | `net/minecraft/block/Block.java`, `BlockFence.java`                                                       |
| No composter / banners / hanging signs / chest boats / foxes in vanilla 1.7.10                                                      | Absence from patched sources                                                                              |
| Et Futurum APIs (`CompostingRegistry`, `BannerPatternHelper`, `StrippedLogRegistry`, `BlockDirtPath`, chest boats, `BlockWoodSign`) | Read from `GTNewHorizons/Et-Futurum-Requiem` source                                                       |
| **No hanging signs in the GTNH Et Futurum fork**                                                                                    | Full recursive tree scan (761 Java files, 0 matches)                                                      |
| D-Mod `Compat.registerBerryBushHandler(IBerryBushHandler)`; `EntityFox.AIEatSweetBerries` uses it                                   | Read from `makamys/DMod` source                                                                           |
| Model/asset composition (82 flat items, 86 block-items, ~70 vanilla blockstates, 82 distinct inline geometries, 6 templates)        | Parsed all 418 model + 139 blockstate JSONs                                                               |
| Mixin count is 14 (10 core + 4 forge)                                                                                               | `find` over the working tree                                                                              |
| DMod, not our code, aborts the dev launch | UniMixins' sanity-check error names `DMod-513764-4523625-deobf.jar`; with that dependency removed the identical launch reaches `Self-test: 36 block(s) 0 failure(s)` |
| Wine racks need **no** Et Futurum API — cherry/mangrove/bamboo racks are self-contained blocks carrying their own textures | Every `vinery:block/*` reference in all ten `<wood>_wine_rack_1.json` models resolves to a file on disk (0 missing), and all ten `tile.vinery.*_wine_rack_big.name` translation keys already exist |
| Box rendering via `RenderBlocks#setRenderBounds` + `renderStandardBlock` works | Confirmed in game: renders, textures, collides, and faces the placer in all four directions |
| Wine-rack rotation: 180° is its own inverse, so only a quarter turn exposes a swapped 90° mapping | Computing the back panel's rotated position for all four facings (`facing 1` must put it at `x≈0.03`, not `x≈0.97`) |
| `BlockFurnace#onBlockPlacedBy` floors the yaw; a plain `(int)` cast breaks roughly half of all angles | Player yaw observed as `-177.6` / `-360.45` in the placement log; truncation yields `-1` where flooring yields `-2` |
| `RenderingRegistry#renderWorldBlock` passes its own `modelId`, not metadata, into the handler's metadata slot | Bytecode: the same `iload_7` feeds both the map lookup and the interface call |

What is still **assumed** (verify during implementation):
- JEI 1.7.10 API shape (exact artifact coordinates still to be pinned).
- Whether the 4 structure `.nbt` files convert cleanly to 1.7.10 schematics.
- Effort estimates (they're judgment, not measurement).
- `pack_format` for 1.7.10 (`1`) — trivially fixable if a pack tool complains.
