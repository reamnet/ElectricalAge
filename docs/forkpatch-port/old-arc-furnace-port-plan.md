# Old 480 V Arc Furnace port plan

Baseline: current port `80b560790`, original ForkPatch `b7444a7b`. This document specifies the port and adds no recipes or runtime behavior.

## Original machine

ForkPatch calls `registerArcFurnace(34)` in `registration/TransparentNodeRegistration.kt:137`. The active entry at line 801 is `Old 480V Arc Furnace`, sub-ID 0, transparent descriptor **2176** (`34 << 6`). The destination transparent slot is free. The six-node descriptor at 2176 belongs to a different registry and must retain its identity. Preserve arc-clay and arc-metal ingots at shared-item IDs 4422 and 4423 and retain both storage blocks and their 9:1 conversions. The source's commented-out 800 V furnace at sub-ID 1 is outside this port.

The original machine craft in `craft/CraftingRecipes.kt:2645` makes one furnace from `ICI / DMD / IcI`. It costs four alloy ingots (`I`), one 3x Graphite Rods bundle (`C`), two gold plates (`D`), one Advanced Machine Block (`M`), and one Synthetic Diamond (`c`). Its source processing list is at lines 2244–2362.

| Input | Output | Energy per operation |
| --- | --- | ---: |
| 2 clay balls | 1 Arc Clay Ingot | 400 kJ |
| 1 iron ingot | 1 Arc Metal Ingot | 200 kJ |
| 1 iron, gold, coal, diamond, emerald, nether quartz, copper, lead, or tungsten ore block | 2 of the corresponding ingot or resource | 5 kJ |
| 1 redstone ore block | 6 redstone | 5 kJ |
| 1 lapis ore block | 1 lapis block | 5 kJ |
| 1 Alloy Dust | 1 Alloy Ingot | 5 kJ |
| 1 Canister of Water | 1 Canister of Arc Water | 7 MJ |

The source uses `OldArcFurnaceDescriptor.java` in `transparentnode/electricalmachine`. It is a **480 V, 10 kW** machine with `ThermalLoadInitializer(80.0, -100.0, 10.0, 100000.0)` and the source high-voltage cable descriptor. Power faces exclude the front and rear. The constructor passes a 600 V maximum, while the source element installs a nominal-voltage watchdog at 1.3× nominal (624 V). Compare effective trip and thermal behavior before changing any limits. The current common tiers are 48 V, 230 V, 400 V and 11 kV. Keep the furnace explicitly rated at 480 V and use its actual terminal voltage and delivered energy. Validate nameplate behavior on a 480 V source and characterize operation on a 400 V feed separately. Check the native cable rating independently of the connector/model choice. The existing keyed 480 V industrial outlet and cord are candidates for supply only if the standard cord integration and load path support this machine.

## Implementation sequence

1. Add `OldArcFurnaceDescriptor.java` in the current electrical-machine package and register it through `ElnDeviceRegistry.java` at transparent 2176. Add an explicit `eln-ids.json` mapping for its own registry path, such as `old_480v_arc_furnace`. Reuse `ElectricalMachineElement`, `ElectricalStackMachineProcess`, `ElectricalMachineInventory`, `ElectricalMachineScreen` and `ElectricalMachineRender` for accepted electrical work, input counts, output capacity, GUI, and NBT. Keep drawing and side-port overrides local to the descriptor. Check machine boost, thermal protection, and the native `Brownout.SLOW` behavior against the source.
2. Add a dedicated furnace `RecipesList` in `Eln.java` and populate it in the `ElnMachineRecipes.registerAll()` refill path. Wire the list to the descriptor and `JeiPages.MACHINES` so the real machine gates JEI acquisition. Add its JEI title in `ElnLang.java`. Add the furnace construction recipe in `ElnRecipes.java` with the exact source costs. Resolve the original copper ore input against vanilla copper ore. The arc-water identities and routes are recorded below.
3. Import the original `assets/eln/model/arcfurnaceold/` OBJ, MTL, TXT and `tex.png`. Preserve the `main` and `move` parts with `tyoff 0.0` and `tyon -0.125`. The Blender file is authoring data. Import `textures/blocks/old800varcfurnace.png` as the native item icon. Wire the original `eln:arcfurnace_loop` sound event to the OGG already present in the port and check start/stop behavior. Run `runClientData` twice and check the generated item model, blockstate, recipe, advancement, language and JEI title. The second run should write zero files.

## Acceptance tests

- Check descriptor identity, crafting inputs and uniqueness, six-node 2176 independence, and both 9:1 storage round trips.
- Run powered GameTests for both metal recipes at 480 V. Check 10 kW unboosted demand and accepted work of 400/200 kJ, exact consumption and yield, insufficient clay, full outputs, removal or replacement of input mid-cycle, and pause on loss of supply. Exercise sided transfer, side ports, brownout and overvoltage protection. Check placed and item geometry and the moving part.
- Reload a partially paid operation. Verify inventory, recipe identity and progress survive, with no duplicate output or credited work. Check the ordinary native machine cases for regressions.
- Run `jei_every_item_reachable` with actual machine and crafting records. The baseline is 44 unreachable of 876. The current graph predicts 35 existing items become reachable if both routes and the furnace bootstrap are available. Check the measured count against the changed item catalog and keep the historical snapshots intact.

## Arc-water source mapping

ForkPatch `registration/ItemRegistration.kt` calls `registerArc(69)` and registers three plain shared-item descriptors. The source generated models give the registry paths below. The contents are represented by distinct stackable items.

| Shared-item ID | Registry key | Original texture |
| ---: | --- | --- |
| 4424 | `eln:inert_canister` | `inertcanister.png` |
| 4427 | `eln:canister_of_water` | `canisterofwater.png` |
| 4428 | `eln:canister_of_arc_water` | `canisterofarcwater.png` |

In `craft/CraftingRecipes.kt`, `recipeDust` combines eight lapis dust and one diamond dust into one inert canister. `recipeElectricalFurnace` registers the shapeless inert canister and water bucket recipe, with the vanilla empty-bucket remainder. The magnetizer also turns one inert canister into one ender pearl at 150 kJ. The Old 480 V Arc Furnace turns one water canister into one arc-water canister at 7 MJ, which takes 700 seconds of accepted work at 10 kW. The inspected source has no further consumer or fluid interaction for the finished arc-water canister.
