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
| Et Futurum Requiem | **Optional integration** — banners, composter, dirt paths, stripped logs, chest boats      | High (API verified)                      |
| D-Mod (makamys)    | **Optional integration** — foxes eat Vinery grape bushes via its public API                | High (API verified)                      |
| Hanging signs      | **Stubbed/TODO** — absent from 1.7.10 *and* the GTNH Et Futurum fork                       | Decided                                  |

---

## 1. Current state audit (verified)

**Build system** — already functional:
- `./gradlew help` → BUILD SUCCESSFUL, Gradle 9.3.1, `com.gtnewhorizons.gtnhconvention` 2.0.20 (2.0.34 available).
- MC 1.7.10 workspace is already decompiled/patched (`build/rfg/*`), so iteration is fast.
- `enableModernJavaSyntax = jabel` is already set and confirmed active (compiler prints `Jabel: initialized`).

**Source tree** — 183 Java files / 16,199 LOC:

| Area                           | Files       | Notes                                                       |
|--------------------------------|-------------|-------------------------------------------------------------|
| `core/`                        | 138         | blocks, items, entities, recipes, effects, registries       |
| `client/`                      | 29          | renderers, GUI, models                                      |
| `forge/`                       | 14          | NeoForge entry points, config, villager trades, mixins      |
| `core/mixin/` + `forge/mixin/` | 14 (10 + 4) | see §6 — shrinks to ~6 once Forge-native hooks replace them |

**Blocking problems:**

1. ~~All assets and data were deleted in the "Initial refactor" commit~~ — ✅ **RESOLVED**: assets have been restored. Verified in working tree: **294 textures, 418 models, 139 blockstates, 439 data files, lang + sounds** (1,339 resource files total).
2. 🔴 **The tree does not compile for a non-API reason.** 170 files declare `package com.mrfuzzihead.vinery.*` but **102 files still `import net.satisfy.vinery.*`**. This must be fixed before any API work.
3. 🟡 `mcmod.info` is still the RFG template placeholder, `pack.mcmeta` still says `pack_format: 34` (a 1.21 value — 1.7.10 wants `1`).

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
- 🔁 `BlockBehaviour.Properties` + `BlockState`/`IBlockState` → 1.7.10 `Block` ctor args + `IProperty` (`PropertyInteger`, `PropertyDirection`, `PropertyEnum`), metadata ints.
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
- ❌ `AddPackFindersEvent` "bushy leaves" pack → no discovery mechanism in 1.7.10. Options: (a) drop, (b) config-gated texture swap via `IIconHandler`/ISBRH.
- 🔁 `commands` → 1.7.10 `ICommand` (`net.minecraft.command.CommandBase`).

---

## 5. Rendering — **vanilla-path first, generated OBJ for decorative geometry**

### 5.1 What the assets actually contain (measured, not assumed)

| Bucket                                                                                                                                                                                                                                                                                                          | Count                               | 1.7.10 path                                                      |
|-----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|-------------------------------------|------------------------------------------------------------------|
| Vanilla geometry families (`cube_all`, `cube_column`, `cube_column_horizontal`, `cube_bottom_top`, `slab`, `slab_top`, `stairs`/`inner`/`outer`, `fence_post`, `template_fence_gate*`, `door_*` ×8, `template_trapdoor_*`, `button*`, `pressure_plate_*`, `template_glass_pane_*`, `cross`, `flower_pot_cross`) | ~70 of 139 blockstates              | ✅ **subclass the vanilla 1.7.10 block class, swap textures**    |
| Flat item sprites (`item/generated`)                                                                                                                                                                                                                                                                            | 82 of 168 item models               | ✅ plain `Item` + `setTextureName`                               |
| Item models that are really *block* models (lattice, wine racks, bags, slabs, stairs, barrel, chair…)                                                                                                                                                                                                           | 86 of 168 item models               | ✅ `ItemBlock` renders via the block's own renderer              |
| Reusable decorative templates (`template_lattice` ×10, `template_wine_rack_{1,2,3_closed,3_open}` ×40, `template_small_wine_bottle` ×6)                                                                                                                                                                         | 56 models, **6 shapes**             | 🟡 generated OBJ (see §5.3)                                      |
| Hand-shaped block models (apple press, barrel, cabinet, drawer, table, shelf, chair, window, storage pot, stackable log, wine box, signs)                                                                                                                                                                       | 82 distinct inline geometries total | 🔁 simple ones → hand-written ISBRH; fiddly ones → generated OBJ |
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

Why this is the *portable* choice: 70 of 139 blockstates never touch a custom renderer; the rest use only vanilla Forge APIs (`ISBRH`, `IItemRenderer`, `AdvancedModelLoader`, `Tessellator`) — nothing that breaks under Fast Render, OptiFine/shaders, FTB texture packs, or NEI/JEI item rendering.

### 5.3 The one gotcha that decides Tier 5's implementation

Forge 1.7.10's `WavefrontObject` OBJ parser **ignores `mtllib` and `usemtl`** (verified in the patched sources) — `GroupObject` has no texture field, so the stock loader is single-texture. Multi-texture is still possible because `WavefrontObject` exposes:

```java
void tessellateOnly(Tessellator tessellator, String... groupNames);
```

So the converter must emit **one `g <textureKey>` group per texture**, and the shared renderer binds `textureKey` then calls `tessellateOnly(...)` per group. This keeps every texture individually named `vinery:block/...` (so texture packs still override them) instead of baking an atlas.

**Renderer footprint: ~1 class** for all Tier 5 models, plus one thin `ISBRH` adapter to draw the same models in-world.

### 5.4 Converter scope (small and bounded)

- Input: only the ~6 `template_*` models + the wine-bottle models (keep the other 250 block models in the repo as the reference/intent source).
- Must handle: `#texture` variable indirection, per-face `rotation: 90/180/270` (bake into UVs), `from`/`to`/`rotation` → OBJ verts, group-per-texture emission, UV scaling for non-16px textures (`texture_size: [80,80]` on the lattice).
- Run it as an offline dev tool (or a Gradle task for reproducibility); commit the generated `.obj` + `.mtl`.
- Keep the original 1.21 JSONs in the repo, but **exclude them from the released jar** (~1.5 MB) via RFG's `jar { exclude }`.

### 5.5 Revised effort

| Work                                     | Estimate                                                                                |
|------------------------------------------|-----------------------------------------------------------------------------------------|
| Tier 1 vanilla block wiring (~20 blocks) | 2–3 days                                                                                |
| Tier 2/3 item wiring (~168 items)        | 1 day                                                                                   |
| Tier 4 ISBRH (~10 renderers)             | 3–5 days                                                                                |
| Tier 5 converter + shared renderer       | 3–5 days                                                                                |
| Entity models + renderers                | 2–3 days                                                                                |
| **Total**                                | **~2–3 weeks**, mostly mechanical (down from the 1–3 week "unknown" with far less risk) |

---

## 6. Mixins (14 → ~6)

> **Correction (audit pass):** the previous draft of this section claimed 1.7.10 has **no `BlockBush`** and that XP-orb pickup needs `EntityPlayer#onEntityContact`. Both were wrong. Verified against the patched 1.7.10 sources: **`BlockBush` exists** (with Forge `IPlantable`), and **`EntityXPOrb.onCollideWithPlayer` fires a cancelable Forge `PlayerPickupXpEvent`**. Together with the D-Mod integration (§4.15) and the fence hook below, **4 of the 14 mixins have no workarounds at all** — they become plain overrides or event handlers, which is much safer on 1.7.10.

RFG/UniMixins path: `usesMixins = true`, `mixinsPackage = com.mrfuzzihead.vinery.mixin`, and the mixin JSON needs `package` fixed (currently `net.satisfy.vinery.neoforge.mixin` — wrong), the 10 `core/mixin` classes **added** (currently only the 4 `forge/mixin` entries are listed), the 4 dropped classes removed, and `compatibilityLevel: "JAVA_8"`.

| Mixin (1.21 target)                                                   | 1.7.10 handling                                                                                                                                          | Mixin needed?                   |
|-----------------------------------------------------------------------|----------------------------------------------------------------------------------------------------------------------------------------------------------|---------------------------------|
| `BlockMixin` (`Block#isExceptionForConnection`)                       | override `BlockFence#canConnectFenceTo` + `Block#canConnectRedstone` in our own blocks                                                                   | ❌ **no mixin**                 |
| `BoneMealItemMixin` (`BoneMealItem#useOn`)                            | mixin `ItemBoneMeal#applyBonemeal`                                                                                                                       | ✅ mixin                        |
| `ClientPlayerEntityMixin` (`LocalPlayer`)                             | mixin `EntityPlayerSP`                                                                                                                                   | ✅ mixin                        |
| `ExperienceOrbMixin` (`ExperienceOrb#playerTouch`)                    | **`@SubscribeEvent PlayerPickupXpEvent`** (fired by `EntityXPOrb#onCollideWithPlayer`, cancelable, before XP is granted)                                 | ❌ **no mixin** — event handler |
| `FoxEntityEatSweetBerriesGoalMixin`                                   | D-Mod `Compat.registerBerryBushHandler` (§4.15)                                                                                                          | ❌ **delete**                   |
| `LivingEntityMixin`                                                   | mixin `EntityLivingBase`                                                                                                                                 | ✅ mixin                        |
| `PlantBlockMixin` (`BushBlock#mayPlaceOn`)                            | 1.7.10 **has `BlockBush`** (+ Forge `IPlantable`); just put the check in our own block's `canPlaceBlockAt`/`canBlockStay`                                | ❌ **no mixin**                 |
| `ShovelItemMixin` (flattenable / dirt paths)                          | Et Futurum `BlockDirtPath` + our own `ItemSpade` subclass or `PlayerInteractEvent`                                                                       | 🔁 likely no mixin              |
| `BootsItem/ChestplateItem/HelmetItem/LeggingsItemMixin` (`ArmorItem`) | mixin `ItemArmor`                                                                                                                                        | ✅ **4 mixins**                 |
| `SpreadingSnowyDirtBlockMixin`                                        | no snowy dirt in 1.7.10                                                                                                                                  | ❌ **delete**                   |
| `WanderingTraderManagerMixin`                                         | no wandering trader anywhere in 1.7.10 → our own spawn routine (`LivingSpawnEvent` / server tick) driven by `TRADER_SPAWN_CHANCE` + `TRADER_SPAWN_DELAY` | ❌ **no mixin**                 |

**Net: 14 mixins → ~6** (`ItemBoneMeal`, `EntityPlayerSP`, `EntityLivingBase`, 4 × `ItemArmor`). Less mixin surface = less port fragility.

---

## 7. Work breakdown

| Phase                    | Content                                                                                                                                                                | Rough effort                 |
|--------------------------|------------------------------------------------------------------------------------------------------------------------------------------------------------------------|------------------------------|
| **0. Repo hygiene**      | Fix `net.satisfy` → `com.mrfuzzihead` package/import migration (102 files), real `mcmod.info`, `pack_format: 1`, delete `.OLD` files, set `usesMixins`/`mixinsPackage` | 1 day                        |
| **1. Skeleton compiles** | `@Mod` class, registry shim, all 204 registry entries mapped, drops work, lang file                                                                                    | 2–3 days                     |
| **2. Core content**      | 37 block classes + 15 item classes + effects → `Potion`, tile entities, NBT components, no rendering (debug models)                                                    | 1.5–2 weeks                  |
| **3. Containers & GUI**  | 5 `IGuiHandler`s, 2 `GuiScreen`s, slot classes                                                                                                                         | 3–5 days                     |
| **4. Entities**          | mule, winemaker, chair, boat                                                                                                                                           | 3–5 days                     |
| **5. Worldgen**          | grape/tree generators, structures (if kept)                                                                                                                            | 2–4 days                     |
| **6. Rendering**         | Vanilla-path block wiring → flat items → ~10 ISBRH → targeted OBJ converter + 1 shared renderer → entity models (§5)                                                   | 2–3 weeks, mostly mechanical |
| **7. Recipes & compat**  | recipe converter + custom recipe classes, tags, config, **Et Futurum / D-Mod integration** (§4.15), ~6 mixins, JEI                                                     | 1 week                       |
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

1. Phase 0 hygiene commit (package fix + `mcmod.info` + `pack_format: 1`).
2. Enable mixins and get a UniMixins bootstrap compiling with the corrected mixin JSON.
3. Build the `Registry` shim and port `ObjectRegistry` first — it's the dependency root for ~everything else.

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

What is still **assumed** (verify during implementation):
- JEI 1.7.10 API shape (exact artifact coordinates still to be pinned).
- Whether the 4 structure `.nbt` files convert cleanly to 1.7.10 schematics.
- Effort estimates (they're judgment, not measurement).
- `pack_format` for 1.7.10 (`1`) — trivially fixable if a pack tool complains.
