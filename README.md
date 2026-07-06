<p align="center">
  <img src="https://raw.githubusercontent.com/create-hydraulics/create-hydraulics-source/main/media/banner.png" alt="Create: Hydraulics" width="100%">
</p>

<p align="center">
  <img src="https://raw.githubusercontent.com/create-hydraulics/create-hydraulics-source/main/media/logo.png" alt="Create: Hydraulics" width="360px">
</p>

<p align="center">
  A hydraulic pressure expansion for the Create mod. Victorian industrial engineering, brought to life.
</p>

<p align="center">
  <a href="https://discord.gg/INVITE_LINK_HERE"><img src="https://img.shields.io/discord/000000000000000000?label=Discord&logo=discord&color=5865F2&style=flat-square" alt="Discord"></a>
  &nbsp;
  <img src="https://img.shields.io/badge/Minecraft-1.21.1-brightgreen?style=flat-square" alt="Minecraft 1.21.1">
  &nbsp;
  <img src="https://img.shields.io/badge/NeoForge-21.1.219-orange?style=flat-square" alt="NeoForge">
</p>

---

## About

Create: Hydraulics adds a **hydraulic pressure network** to Minecraft as a companion system to Create's rotational kinetics. Build pipelines from Hardened Iron Pipes, source pressure from submerged intakes or a kinetic compressor, and drive a set of heavy industrial machines that press, punch, drill, and assemble.

Every machine has full Ponder support. Press **W** while hovering any item from this mod to open its scene.

<p align="center">
  <img src="https://raw.githubusercontent.com/create-hydraulics/create-hydraulics-source/main/media/overview.png" alt="Overview screenshot" width="80%">
</p>

---

## Installation Requirements

This mod requires the following before it can be installed:

<p>
  <a href="https://neoforged.net">
    <img src="https://raw.githubusercontent.com/create-hydraulics/create-hydraulics-source/main/media/dep_neoforge.png" alt="NeoForge" width="80px">
  </a>
  &nbsp;
  <a href="https://github.com/Creators-of-Create/Create">
    <img src="https://raw.githubusercontent.com/create-hydraulics/create-hydraulics-source/main/media/dep_create.png" alt="Create" width="80px">
  </a>
</p>

Install **NeoForge 21.1.219** and **Create 6.0.x** for Minecraft 1.21.1, then drop the Create: Hydraulics `.jar` into your `mods/` folder alongside them. No additional setup is required.

Optional configuration lives in `config/createhydraulics-common.toml` after the first run.

---

## The Pressure System

Hydraulic pressure is measured in **PU (Pressure Units)**. Pipes carry pressure between segments, sources push it in, and machines draw from it. If the total load on a line exceeds its supplied pressure, every machine on that line stalls until the balance is restored.

<p align="center">
  <img src="https://raw.githubusercontent.com/create-hydraulics/create-hydraulics-source/main/media/pressure_network.png" alt="Pressure network diagram" width="75%">
</p>

**Sources**

- **Aqueduct Intake** - Submerge it in water for free, passive pressure. Combine multiple intakes for higher output.
- **Hydrostatic Intake** - Draws pressure from the height of a water column above it. More height equals more PU.
- **Centrifugal Compressor** - Converts shaft rotation into pressure. Output scales with RPM and draws stress from the kinetic network.

**Network blocks**

- **Hardened Iron Pipe** - The core conduit. Carries PU only, not fluid, and does not interact with Create's fluid network.
- **Pressure Valve** - A redstone-controlled gate for isolating sections of your network.
- **Flow Gauge** - Reads and displays the pressure of the segment it sits in. Compatible with Engineer's Goggles.

---

## Machines

<p align="center">
  <img src="https://raw.githubusercontent.com/create-hydraulics/create-hydraulics-source/main/media/hydraulic_press.png" alt="Hydraulic Press" width="75%">
</p>

**Hydraulic Press**

A pressure-driven alternative to Create's Mechanical Press. Position it above a depot, connect a pipe above the casing, fill with Hydro Fluid, and it processes any standard pressing recipe. No shaft required.

---

<p align="center">
  <img src="https://raw.githubusercontent.com/create-hydraulics/create-hydraulics-source/main/media/hydraulic_fist.png" alt="Hydraulic Fist" width="75%">
</p>

**Hydraulic Fist**

An industrial arm that extends and strikes whatever is in front of it. Crushes blocks into their fisting-recipe results or processes items on a belt below. Reach is configurable up to 16 blocks. Pipe connects to the back face.

---

<p align="center">
  <img src="https://raw.githubusercontent.com/create-hydraulics/create-hydraulics-source/main/media/hydraulic_drill.png" alt="Hydraulic Drill" width="75%">
</p>

**Hydraulic Drill**

A heavy breaker head for mining. Aim it at any mineable block, connect a pipe to the back, and it breaks through automatically. Drops land at the broken position, ready to be collected by a funnel or belt.

---

<p align="center">
  <img src="https://raw.githubusercontent.com/create-hydraulics/create-hydraulics-source/main/media/hydraulic_motor.png" alt="Hydraulic Motor" width="75%">
</p>

**Hydraulic Motor**

Converts pressure back into rotational kinetics. Set the target RPM with the scroll box on the side, feed it a pressurized line and Hydro Fluid, and it generates stress capacity for your Create machines. Higher RPM requires more PU and consumes more fluid per second.

---

<p align="center">
  <img src="https://raw.githubusercontent.com/create-hydraulics/create-hydraulics-source/main/media/centrifugal_compressor.png" alt="Centrifugal Compressor" width="75%">
</p>

**Centrifugal Compressor**

The high-output pressure source. Connect a shaft, fill with Hydro Fluid, and it floods the pipe above with pressure proportional to its current speed. Draws stress from the rotation network like any Create machine.

---

<p align="center">
  <img src="https://raw.githubusercontent.com/create-hydraulics/create-hydraulics-source/main/media/hydraulic_assembly_unit.png" alt="Hydraulic Assembly Unit" width="75%">
</p>

**Hydraulic Assembly Unit**

A universal assembler. Right-click with your target item to set the recipe filter, feed ingredients in from the top, and it assembles any crafting recipe that produces the chosen result. Powered by Hydro Fluid and pressure. Finished items are ejected from the bottom for collection.

---

## Gallery

<p align="center">
  <img src="https://raw.githubusercontent.com/create-hydraulics/create-hydraulics-source/main/media/gallery_1.png" width="48%">
  &nbsp;
  <img src="https://raw.githubusercontent.com/create-hydraulics/create-hydraulics-source/main/media/gallery_2.png" width="48%">
</p>

<p align="center">
  <img src="https://raw.githubusercontent.com/create-hydraulics/create-hydraulics-source/main/media/gallery_3.png" width="48%">
  &nbsp;
  <img src="https://raw.githubusercontent.com/create-hydraulics/create-hydraulics-source/main/media/gallery_4.png" width="48%">
</p>

---

## Community

Join the Discord server to ask questions, share builds, report bugs, or follow development.

<p>
  <a href="https://discord.gg/INVITE_LINK_HERE">
    <img src="https://img.shields.io/badge/Join%20the%20Discord-5865F2?style=for-the-badge&logo=discord&logoColor=white" alt="Join Discord">
  </a>
</p>

---

## Credits

**Lead Developer**

[Your name here]

**Supporters**

[Supporter names here]

---

<p align="center">
  Made with care for the Create community. Thank you to everyone who tested, reported bugs, and kept this going.
</p>
