# Programmable Load Bank

The 3.0.77 load bank now uses a native 26.3 adapter at the original source
lookup **8064 (126:0)**, registry name `programmable_load_bank`. Existing target
identities are unchanged. Crafting keeps the source bill of materials: iron
plates, redstone, copper cable, a blaze rod and a machine block.

Connect supply to the blue left or bottom terminal. The front bottom terminal
accepts a signal; at 60% or above it inhibits the load. There is no power output.
The source front-top trip terminal remains a wired low signal: this device uses
automatic thermal cutoff rather than a latched trip. The native menu selects nominal input voltage
(12–230,000 V), demand (0–1,000,000 W), and enable state.

This is a passive resistor, not a constant-power sink. At half nominal voltage it
draws one quarter of programmed power. The source's 0.001-ohm resistance floor
also remains, so very low nominal voltages cannot realize every requested kW
setting. There are no per-device Thevenin probes. Repeated healthy steps retain
the native LU factors.

The source case/fan model integrates actual resistor watts once per physical
interval, with 12 kJ/K case capacity and cooling conductance
`max(100 W/K, programmedWatts / 45 K)`. The 50 kW default reaches 65 C at nominal
voltage. A case at or above 120 C opens until it cools; input beyond 130% nominal
also opens until voltage recovers. Switching observes physical step boundaries,
so the last conducting interval is included in consumed energy and heat. Trial
preparation cannot advance either clock. This is the source's scalar electrical
and case-cooling model, not a detailed fan/room airflow simulation.

Publication carries accepted voltage, watts, current, absolute case temperature
and lifetime consumed joules. The original enclosure, reproducible icon and fan
sample are included; the native sound loop follows actual accepted power above
50 W. The dedicated menu provides the live readings. The source mesh has no
separate numerical display part, so this adapter does not invent a mesh display.

Placed saves and carried machine NBT retain settings, enable state, heat and
consumed-energy history. The configuration tool copies settings only; it cannot
cool another machine or copy its meter history. This is not a 1.21.1 world
converter. Appliance cords remain part of the separate appliance-power backlog;
the load bank currently uses native cable terminals.

Validation and exact commands are in [the checkpoint report](load-bank-validation.json).
Interactive GPU/audio playback is separate from headless geometry, sound-event
and loop-state validation.
