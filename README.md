Create: Hydraulics
==================

A Create addon (Victorian/industrial hydraulics theme). The first content is the **Hardened Iron Pipe**.

> **Minecraft version note:** This project targets **Minecraft 1.21.1 / NeoForge 21.1.219**, because the
> Create mod we depend on only ships for 1.21.1 (latest Create is `6.0.10` for 1.21.1; there is no 1.21.8
> build). Dependency coordinates live in `gradle.properties` (`create_version`, `ponder_version`,
> `flywheel_version`, `registrate_version`) and the repos/deps are wired in `build.gradle`. Create is a
> **required** dependency (declared in `src/main/templates/META-INF/neoforge.mods.toml`).

### Hardened Iron Pipe — how it works

* **Block** — `block/HardenedIronPipeBlock` extends vanilla `PipeBlock` (the same base Create's own
  `FluidPipeBlock` uses), giving it the six boolean connection properties `NORTH/EAST/SOUTH/WEST/UP/DOWN`
  (plus `WATERLOGGED`). Connections are recomputed with Create's exact rule:
  * 0 connections → straight **EAST–WEST** pipe (west & east arms always visible by default)
  * exactly 1 connection → also draw the opposite arm (straight pass-through)
  * 2+ connections → an arm per connected side (corner / tee / cross)
* **Pressure-only, not a fluid pipe** — the Hardened Iron Pipe carries **hydraulic pressure (PU) only**. It
  does **not** transfer liquid, expose an `IFluidHandler`, or join Create's fluid network, so Create's
  pumps/pipes/tanks cannot connect to it or push fluid through it. It connects to other hydraulic pipes (and
  its relatives, the Pressure Valve and Flow Gauge) and to the **Aqueduct Intake**'s top face.
* **BlockEntity** — `block/entity/HardenedIronPipeBlockEntity` tracks a single `pressure` value. Each server
  tick it propagates pressure toward lower-pressure connected neighbours at up to 100 PU/s and leaks down
  toward 0 when nothing feeds it. Pressure is supplied by hydraulic sources via `supplyPressureSource(...)`
  (used by the Aqueduct Intake) or injected directly via `addPressure(...)`.
* **Aqueduct Intake** — `block/AqueductIntakeBlock` is a submerged pressure source. Its BlockEntity checks
  every tick that all six neighbouring blocks are water (a waterlogged pipe on top counts), drives its
  `active` blockstate from that check, and while active pushes 100 PU/s into the pipe directly above through
  the same `supplyPressureSource(...)` pathway. Output is the top face only.
* **Model / blockstate** — `blockstates/hardened_iron_pipe.json` is a **multipart** definition: it applies
  the core model always and one arm model per `true` property. This maps every one of the 64 boolean
  combinations to the correct set of arms. The per-arm models in `models/block/` are split from your
  BlockBench export `models/hardened_iron_pipe.json` (group → file mapping is noted in each split file's
  `__comment`). If you re-export the BlockBench model, regenerate those splits to match.

### ⚠️ You still need to add the texture

The models reference `createhydraulics:block/texture`, so drop your pipe texture at:

```
src/main/resources/assets/createhydraulics/textures/block/texture.png
```

(64×64, to match `texture_size` in the models.) Until then the pipe renders with the missing-texture
placeholder — it will not crash.

### Running

`./gradlew runClient` launches a dev client. Create + its libraries are on the runtime classpath (Create
powers the Ponder scenes and the creative-tab theming), but the hydraulic pipes are a self-contained
pressure network and do **not** interconnect with Create's fluid pipes/pumps/tanks.


Installation information
=======

This template repository can be directly cloned to get you started with a new
mod. Simply create a new repository cloned from this one, by following the
instructions provided by [GitHub](https://docs.github.com/en/repositories/creating-and-managing-repositories/creating-a-repository-from-a-template).

Once you have your clone, simply open the repository in the IDE of your choice. The usual recommendation for an IDE is either IntelliJ IDEA or Eclipse.

If at any point you are missing libraries in your IDE, or you've run into problems you can
run `gradlew --refresh-dependencies` to refresh the local cache. `gradlew clean` to reset everything 
{this does not affect your code} and then start the process again.

Mapping Names:
============
By default, the MDK is configured to use the official mapping names from Mojang for methods and fields 
in the Minecraft codebase. These names are covered by a specific license. All modders should be aware of this
license. For the latest license text, refer to the mapping file itself, or the reference copy here:
https://github.com/NeoForged/NeoForm/blob/main/Mojang.md

Additional Resources: 
==========
Community Documentation: https://docs.neoforged.net/  
NeoForged Discord: https://discord.neoforged.net/
