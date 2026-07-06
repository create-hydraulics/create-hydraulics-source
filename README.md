# Create: Hydraulics — Dev Repo

NeoForge 1.21.1 addon for Create. Adds a hydraulic pressure network (PU system) and a suite of machines powered by it.

**Stack:** Java 21 · NeoForge 21.1.219 · Create 6.0.x · Ponder · JEI compat

---

## Running

```bash
./gradlew runClient   # dev client with Create on the classpath
./gradlew build       # produces the jar in build/libs/
```

Dependencies pull from Maven Central + Create's maven. No manual setup needed beyond a JDK 21.

---

## Structure

```
src/main/java/com/createhydro/hydro/
├── block/              Block classes + HydraulicAssemblyUnitBlock
│   └── entity/         All BlockEntities (press, motor, fist, drill, compressor, pipe, gauge…)
├── client/             BERs (block entity renderers)
├── compat/jei/         JEI category plugins
├── contraption/        Create contraption movement behaviour (drill)
├── item/               HydroFluidBucketItem
├── ponder/             Ponder scenes + plugin registration
├── recipe/             HydraulicPressRecipe, HydraulicFistRecipe
├── registry/           ModBlocks, ModItems, ModBlockEntities, ModFluids…
├── Config.java         NeoForge config spec (pressure costs, fluid rates)
├── CreateHydraulics.java   Mod entry point, capability registration
└── CreateHydraulicsClient.java   Client setup, renderers, Ponder plugin
```

---

## Key systems

**Pressure network** — `HardenedIronPipeBlockEntity` carries PU between segments. Sources (`AqueductIntakeBlockEntity`, `HydrostaticIntakeBlockEntity`, `CentrifugalCompressorBlockEntity`) call `supplyPressureSource()` each tick. Machines do a BFS over the connected network at tick time to read available PU and count their load against it.

**Kinetic integration** — `HydraulicMotorBlockEntity` extends `GeneratingKineticBlockEntity`; compressor extends `KineticBlockEntity`. Both wire into Create's stress network normally.

**Assembly Unit** — searches the recipe manager for any `CraftingRecipe` producing the filter item. Ingredient satisfaction is simulated before committing; results ride in a separate output handler that funnels can extract from.

---

## Notes

- `workspace.xml` is excluded from git — IntelliJ regenerates it on first open.
- The `.github/workflows/build.yml` CI file must be added via GitHub UI (token lacks `workflow` scope).
- Fluid textures reuse water still/flow with a teal tint (`0xFF2BB3C0`). Replace in `CreateHydraulicsClient` once custom art exists.
