<div align="center">

<!-- Replace with your banner image -->
<img src="https://raw.githubusercontent.com/create-hydraulics/create-hydraulics-source/main/media/banner.png" alt="Create: Hydraulics" width="100%">

<br><br>

<img src="https://raw.githubusercontent.com/create-hydraulics/create-hydraulics-source/main/media/logo.png" alt="Create: Hydraulics Logo" width="400px">

<br>

<h3><i>A hydraulic expansion for the Create mod — pressure networks, industrial machines, and Victorian-era engineering.</i></h3>

<br>

[![Minecraft](https://img.shields.io/badge/Minecraft-1.21.1-brightgreen?style=flat-square)](https://minecraft.net)
[![NeoForge](https://img.shields.io/badge/NeoForge-21.1.219-orange?style=flat-square)](https://neoforged.net)
[![Requires Create](https://img.shields.io/badge/Requires-Create%206.x-red?style=flat-square)](https://github.com/Creators-of-Create/Create)

<br>

---

</div>

<br>

<div align="center">
<h2>⚙️ What is Create: Hydraulics?</h2>
</div>

Create: Hydraulics adds a **hydraulic pressure network** to Minecraft — a new energy system that sits alongside Create's rotational kinetics. Pipe pressurized fluid through **Hardened Iron Pipes**, source your pressure from submerged intakes or a compressor, and power a suite of heavy industrial machines that can press items, punch blocks, drill through terrain, and assemble complex crafts automatically.

Everything is designed to feel at home next to vanilla Create — the same Victorian-industrial aesthetic, the same engineering depth, and full Ponder scene support so you can learn every machine in-game.

<br>

---

<br>

<div align="center">

<!-- Replace with a screenshot showing a full pressure network setup -->
<img src="https://raw.githubusercontent.com/create-hydraulics/create-hydraulics-source/main/media/overview.png" alt="Hydraulic network overview" width="80%">

<br><br>

<h2>🔩 The Pressure Network</h2>

</div>

Hydraulic pressure is measured in **PU (Pressure Units)**. Pipes carry it, sources generate it, and machines consume it. The system works much like Create's stress — if your machines draw more PU than your sources supply, everything on that line stalls.

<br>

<table align="center" width="80%">
<tr>
<td width="50%" valign="top">

### Pressure Sources

| Block | How it works |
|-------|-------------|
| **Aqueduct Intake** | Submerge it in water — free, passive pressure. Stack multiple for more. |
| **Hydrostatic Intake** | Uses the pressure of a water column above it. Height = more PU. |
| **Centrifugal Compressor** | Connect a shaft. RPM × config = PU output. Consumes stress. |

</td>
<td width="50%" valign="top">

### Network Control

| Block | What it does |
|-------|-------------|
| **Hardened Iron Pipe** | The backbone — connects everything. Carries PU only, not fluid. |
| **Pressure Valve** | Redstone-controlled shutoff. Gate sections of your network. |
| **Flow Gauge** | Reads the line and shows PU. Works with Engineer's Goggles too. |

</td>
</tr>
</table>

<br>

---

<br>

<div align="center">
<h2>🏭 Machines</h2>
</div>

<br>

<div align="center">

<!-- Replace with screenshot of Hydraulic Press in action -->
<img src="https://raw.githubusercontent.com/create-hydraulics/create-hydraulics-source/main/media/hydraulic_press.png" alt="Hydraulic Press" width="70%">

</div>

<br>

### Hydraulic Press

A pressure-powered alternative to Create's Mechanical Press. Place it above a depot, connect a pipe above the casing, and fill with Hydro Fluid. It runs on pressure — no shaft required. Recipes are the same pressing recipes Create already defines; whatever you can press with a Mechanical Press, you can press here.

> *Needs pipe above · Hydro Fluid · pressure on the line*

<br>

---

<br>

<div align="center">

<!-- Replace with screenshot of Hydraulic Fist -->
<img src="https://raw.githubusercontent.com/create-hydraulics/create-hydraulics-source/main/media/hydraulic_fist.png" alt="Hydraulic Fist" width="70%">

</div>

<br>

### Hydraulic Fist

An industrial arm that telescopes out and punches what's in front of it. It can crush blocks into their fisting-recipe results, or smash items riding on a belt below. Configurable reach up to 16 blocks. Pipe feeds the back face.

> *Pipe on back · targets blocks or belt items · 1 item per stroke*

<br>

---

<br>

<div align="center">

<!-- Replace with screenshot of Hydraulic Drill -->
<img src="https://raw.githubusercontent.com/create-hydraulics/create-hydraulics-source/main/media/hydraulic_drill.png" alt="Hydraulic Drill" width="70%">

</div>

<br>

### Hydraulic Drill

A heavy breaker head. Point it at a block, connect a pipe on the back, and it hammers through anything a pickaxe can mine. Drops fall at the break position — line up a funnel or belt below to collect them.

> *Pipe on back · breaks any mineable block · loot drops in place*

<br>

---

<br>

<div align="center">

<!-- Replace with screenshot of Hydraulic Motor -->
<img src="https://raw.githubusercontent.com/create-hydraulics/create-hydraulics-source/main/media/hydraulic_motor.png" alt="Hydraulic Motor" width="70%">

</div>

<br>

### Hydraulic Motor

Converts pressure back into Create's rotational kinetics. Set the RPM with the scroll box, feed it Hydro Fluid and a pressurized line, and it generates stress capacity like a creative motor — at a cost. Higher RPM costs more PU and more fluid per second.

> *Pressure in · rotation out · fluid is consumed while running*

<br>

---

<br>

<div align="center">

<!-- Replace with screenshot of Centrifugal Compressor -->
<img src="https://raw.githubusercontent.com/create-hydraulics/create-hydraulics-source/main/media/centrifugal_compressor.png" alt="Centrifugal Compressor" width="70%">

</div>

<br>

### Centrifugal Compressor

The high-throughput pressure source. Drive the shaft with any rotation network — faster RPM means more PU output. Draws stress like any Create machine. Fill it with Hydro Fluid and the output floods up through the pipe above.

> *Rotation in · pressure out · costs stress · needs Hydro Fluid loaded*

<br>

---

<br>

<div align="center">

<!-- Replace with screenshot of Hydraulic Assembly Unit -->
<img src="https://raw.githubusercontent.com/create-hydraulics/create-hydraulics-source/main/media/hydraulic_assembly_unit.png" alt="Hydraulic Assembly Unit" width="70%">

</div>

<br>

### Hydraulic Assembly Unit

A slow, universal assembler. Right-click with your target item to set the recipe filter, then feed ingredients in from the top (hoppers, funnels, or throw items onto it). It scans for any crafting recipe that produces the filter item and assembles it automatically, consuming Hydro Fluid and pressure to run. Finished items eject from the bottom.

> *Filter sets recipe · ingredients in top · results out bottom · no JEI recipe? it still works*

<br>

---

<br>

<div align="center">
<h2>🪣 Hydro Fluid</h2>
</div>

All hydraulic machines use **Hydro Fluid** as their working medium — it's the oil that makes the system go. Unlike a fuel, it stays inside the machine while it runs; you load it once. Craft it into a bucket, right-click the machine, and you're done.

<div align="center">

<!-- Replace with screenshot of Hydro Fluid bucket and Aqueduct Intake -->
<img src="https://raw.githubusercontent.com/create-hydraulics/create-hydraulics-source/main/media/hydro_fluid.png" alt="Hydro Fluid" width="60%">

</div>

<br>

---

<br>

<div align="center">
<h2>🔬 Ponder Scenes</h2>
</div>

Every machine has a Ponder scene — press **W** while hovering the item in your inventory to open it. The scenes walk through placement, operation, and edge cases (including overpressure stalls and the underpressure flash).

<div align="center">

<!-- Replace with screenshot of a Ponder scene -->
<img src="https://raw.githubusercontent.com/create-hydraulics/create-hydraulics-source/main/media/ponder.png" alt="Ponder scenes" width="70%">

</div>

<br>

---

<br>

<div align="center">
<h2>📦 Installation</h2>
</div>

**Required dependencies:**
- [Minecraft 1.21.1](https://minecraft.net)
- [NeoForge 21.1.219+](https://neoforged.net)
- [Create 6.0.x for 1.21.1](https://github.com/Creators-of-Create/Create)

Drop the `.jar` into your `mods/` folder alongside Create and its dependencies. No configuration required — optional config values (pressure costs, fluid consumption rates) are in `config/createhydraulics-common.toml`.

<br>

---

<br>

<div align="center">
<h2>📷 Gallery</h2>

<!-- Add more screenshots here -->
<img src="https://raw.githubusercontent.com/create-hydraulics/create-hydraulics-source/main/media/gallery_1.png" alt="Gallery 1" width="45%">
&nbsp;
<img src="https://raw.githubusercontent.com/create-hydraulics/create-hydraulics-source/main/media/gallery_2.png" alt="Gallery 2" width="45%">

<br><br>

<img src="https://raw.githubusercontent.com/create-hydraulics/create-hydraulics-source/main/media/gallery_3.png" alt="Gallery 3" width="45%">
&nbsp;
<img src="https://raw.githubusercontent.com/create-hydraulics/create-hydraulics-source/main/media/gallery_4.png" alt="Gallery 4" width="45%">

</div>

<br>

---

<br>

<div align="center">

*Create: Hydraulics is an unofficial addon and is not affiliated with the Create mod team.*

<br>

[![GitHub](https://img.shields.io/badge/Source-GitHub-black?style=flat-square&logo=github)](https://github.com/create-hydraulics/create-hydraulics-source)

</div>
