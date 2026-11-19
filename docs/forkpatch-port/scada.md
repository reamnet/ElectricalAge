# Grid Control Console / SCADA

The console preserves source lookup **8065 (126:1)** and native name
`grid_control_console`. Its eight explicit target slots retain world-local block
addresses and circuit selections (1–12). Bind within a spherical 256-block range;
selecting another bound slot restores its saved address to the native fields.
The new bind button sends draft settings and binding together in one atomic
packet, while the original separate settings/bind packet IDs remain supported.

Supply the blue left or bottom terminal with nominal 120 V. Its physical resistor
draws 20 W at nominal voltage; sag changes draw with voltage squared. Front bottom
is inhibit and front top retains the source's low trip signal. Commands require
90–156 V magnitude, enable, a loaded owner chunk and a released finite inhibit
signal. Unlike the old source command helper, an inhibited or overvoltage console
cannot command targets while its own load is disconnected. Read-only telemetry
continues polling loaded targets once per physical second, as in the source.
There is no global per-tick node scan or solver probe.

Each operation resolves the selected address afresh in the owner's dimension.
It checks distance and `getChunkNow` before looking up an existing native node;
missing/unloaded addresses remain unavailable rather than requesting terrain.
No old node reference is cached across reloads. Telemetry publishes the target's
actual HUD readings, with bounded string length.

Remote open/close delegates to existing production APIs for service disconnects
and installed smart meters, gang-operated air-break poles, reclosers, switchgear,
load banks, motor drives, protective relays, load shedders and pad transformers.
Closing never resets trips, replenishes paid hardware or clears recloser lockout.
Console-to-console control and unsupported devices are rejected. The source's
12-circuit breaker-panel family is not yet adapted: circuit addresses are retained
for that future endpoint, not interpreted as new native breakers. Media addon
remote interfaces likewise need their own later adaptation.

Native saves and carried NBT retain all eight addresses, circuits, selected slot,
settings, enable and consumed-energy counter. Addresses are local to the world
where the console is placed, matching the source; this is not a cross-dimension
link or general 1.21.1 world conversion. Native geometry, live display, menu,
renderer factory, icon and paid source recipe are included.

The [checkpoint report](scada-validation.json) separates circuit tests, actual
native endpoints, saved-node reload, unloaded/range checks, control packets and
broader regressions. Interactive GUI/GPU/multiplayer coverage remains pending.
