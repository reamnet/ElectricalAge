# Native chip fabrication

Fabricator 4480 and shared components 8064–8068 retain the source's free lookup
slots: Silicon Wafer, Transistor, NTC Thermistor, Nibble Memory Chip and Arithmetic
Logic Unit. Existing native chip items and recipes remain available. The machine
uses native inventories, menus, publication, models, automation and cached LU.

Each attempt consumes one silicon wafer, one copper plate and 4,000 J of actual
accepted electrical work. At 200 V its 40-ohm load draws 1 kW; voltage sag slows the
job through the real circuit. Readiness/GUI/algebraic checks cannot pay work.
A power outage retains paid progress for the same job. Changing the operation or
inventory clears partial work. Excess energy beyond the current attempt's budget
is dissipated rather than carried into another input pair.

The 15 source operations retain their ordinal IDs and batch sizes. Transistors
yield 16; the ordinary digital/analog chips yield 4; Arithmetic Logic Unit yields 2
with 50% success. Every attempt debits materials and its entire 4 kJ budget once,
including failures. A saved per-machine seed and attempt counter determine yield,
so loading partial work does not reroll the next outcome or depend on render or
unrelated world random calls. The seed stays server-side.

Source process defects are fixed: inputs match actual native wafer/plate items,
a complete legal batch may fill output exactly to 64, incompatible output
components cannot be overwritten, and completion always requires current real
inputs and compatible output capacity. Failed yields cannot reuse their energy.

Output remains slot 0, wafer input 1 and copper input 2. Sided transactions insert
only the appropriate ingredients and extract only output. The fixed source
ZN/Down electrical inlet and model orientation remain independent of player
facing; all four placement fronts have native coverage. The body and both source
etcher parts draw through the native mesh pipeline. Motion advances on the client
clock when measured power is present; mesh capture never advances motion/work.

World saves preserve source keys `operation` and `powerConsumed`, plus native
accepted/completed energy and yield counters. Invalid/nonfinite/out-of-budget
work fails closed. Ordinary picked/configuration items carry only the operation;
actual break drops inputs/output separately and never carries partial work,
yield seed or inventory. Repeated destruction and carried reads cannot create
another input/output owner. Full 1.21.1 world conversion remains separate work.

The pinned source had no explicit survival bootstrap for the machine/wafer.
This adapter adds documented native routes: four silicon ingots still become one
silicon plate in the existing plate machine for 10 kJ; one such plate becomes one
wafer in the same machine for another 10 kJ. Machine crafting uses four iron
ingots, two copper plates, a native machine block, electrical motor and configured
basic chip. These are destination bootstrap recipes, not claimed source recipes.
No existing ingot/plate or chip recipe is replaced.

Optional JEI 31 lists all 15 fabrication examples with their paid-attempt energy
and yield, and the existing plate-machine category lists wafer production.
The actual native recipe manager, input/output predicates and every operation
have headless checks. See [validation](chip-fabrication-validation.json).
Interactive screen/GPU, JEI runtime, audio and multiplayer checks remain pending.
The [source review](chip-fabrication-review.md) retains the original defects and
contract; this remains part of the unfinished full ForkPatch port.
