# ForkPatch to NeoForge 26.3

## Scope and source baselines

The goal is the complete feature set of `reamnet/ELN1.21.1ForkPatch`, carried into
`reamnet/ElectricalAge` branch `ports/26.3`. Work starts from:

* ForkPatch `b7444a7b8a0c5e072b1fd35eed2b74a282c3348c` (3.0.77).
* 26.3 `beb4f981d7a54aeb04b576f3ab769132b94991ec`.
* Minecraft 26.3, NeoForge 26.3.0.75-beta, Java 25 and Kotlin 2.4.21, as pinned by
  the destination branch. Its Java 21 launcher toolchain is also required.

EA Media is a separate repository/addon, not part of the incoming core source.
The core's reserved media IDs and integration API are in scope. The addon itself
will require a corresponding platform port before its browser implementation can
be used on 26.3.

## Conflict policy

The existing 26.3 port is the platform foundation. A shared file/class name does
not establish compatible behavior. Keep its working registry, save, networking,
rendering, menu, fluid, cable and CAD integrations while adapting incoming features.
Preserve destination-only functionality.

When implementations overlap, compare correctness first, then steady-state and
transition CPU time, allocation and rebuild cost under equivalent loads. Prefer
the more efficient implementation that meets the required electrical and gameplay
contracts. Numerical validation, energy conservation and world persistence are
part of those contracts; a faster result produced by omitting work is not a win.

In particular, 26.3 already has LU-based physical solves, diode re-solves and cached
interconnection resistances. ForkPatch has double-double factorization, floating
island validation, checked noncommitting probes and coupled conservative converters.
Do not replace either solver wholesale without exercising both sets of regressions.

## Full migration workstreams

Checkpoint branch: `port/forkpatch-3.0.77-26.3`, based on `ports/26.3`. Checkpoints
are work in progress, not a release or a claim that the entire fork is migrated.

Initial baseline: the destination's Minecraft artifacts, Kotlin, Java, checked CAD
models and mod jar compiled with JDK 25. The first attempt exhausted workspace disk;
reusable Gradle cache was preserved in temporary storage, with all previous release
jars retained. The retry reached the existing fluid property suite. That long-running
baseline test run was stopped so it would not block the code migration; it did not
establish a passing full baseline. Module-focused regressions and a reduced seeded
fluid run will be distinguished from the full property corpus in later checkpoints.

| Workstream | Incoming scope | Destination work required |
| --- | --- | --- |
| Simulation foundation | Checked probes, conservative converters, transformer/control mappings, insulation, energy and thermal fixes, performance improvements | Adapt solver APIs; retain diode/history behavior; compare correctness and performance |
| Solar and renewables | Norton I-V panels, large tracking arrays, roof mounting, MPPT, hybrid inverter, renewable supplies | Adapt registered panels and multiblocks; validate open, loaded, shaded and night networks |
| Utility grid | Cable gauges/insulation, poles, spans, primary fuses, service equipment, underground entries, meters and pad transformers | Register additional devices/assets; adapt topology, persistence, protection and rendering |
| Controls and backup | ATS, generator tiers/reset, UPS, protective relay, load shedding, SCADA and surge devices | Port physical state machines, node controls, packets, displays and safety interlocks |
| Off-grid storage | 16/32/48/64 kWh floor/wall LiFePO4 banks, inverter charger, charger, PV combiner/disconnect | Port BMS/energy state, external DC/AC ports, cabinet multiblocks, menus and visuals |
| Appliances and lighting | Cords/sockets, kitchen/laundry/climate devices, pumps, decorative and utility lamps, fire alarms | Adapt electrical connections, supports/collision, inventories, menus, sounds and baked geometry |
| Mechanical and industrial | Fusion/industrial equipment, thermal/mechanical changes, EV cells/chargers, welding and forklift | Integrate with destination shafts/fluid systems rather than reverting them |
| Vehicles | Four-seat cart, two-seat electric car, storage, seats, charging and clear windows | Port entity/vehicle state, input packets, collision/step handling and 26.3 rendering |
| Data and optional integrations | Recipes, translations, models, loot/tags, guide, diagnostics, metrics and mod bridges | Merge data and IDs; handle integrations only when a compatible 26.3 dependency exists |

See the [current feature backlog](forkpatch-port/remaining-work.md) for completed
adapters and the remaining equipment, parity and validation work.

These rows define the full scope, not a claim that all features are playable yet.
Only features with compiled production adapters and relevant passing regressions
should be marked migrated. Copying unused files into a staging directory does not
count as an implementation.

### Code checkpoint: simulation and controller foundations

The first incoming production sources are the 3.0.77 converter algebra, control
mappings, insulation model, protective relay/load-shed state machines, ATS transfer
logic/contact, UPS alarm, LiFePO4 saved-energy state and vehicle physics helpers.
Their original paths and hashes are in [the import ledger](forkpatch-port/imports.json).
The source solar I-V/Norton process and current-source component are adapted to the
destination method names and 26.3 NBT reads.

The incoming checked terminal-voltage probe uses a cached transposed response from
the destination's LU factors. It samples RHS callbacks once per read, preserves
physical/pending state, invalidates rows on a matrix rebuild and checks exceptional
RHS values with a complete scratch solve. It does not replace the destination's
LU physical solve, diode re-solves or cached interconnection resistances. The later converter checkpoint covers the coupled runtime; complete floating-gauge
and stiff high-voltage accuracy work remains pending.

Vehicle kernels still await their production entities and menus. Solar, off-grid,
UPS racks, ATS/standby and relay/load-shedding adapters
are described in the later checkpoints below.

### Code checkpoint: registered solar blocks

All four existing solar registrations now use the ForkPatch Norton I-V process.
Small panels have 37 V open circuit, 30 V at 8 A optimum and 8.6 A short circuit;
large panels are the 3-series / 2-parallel module (111 V / 90 V / 16 A / 17.2 A).
The destination's generation multiplier still scales the currents. Registry keys,
tracking, local weather support, ghost blocks and native rendering remain in use.
Old power-budget saves are superseded by the light-dependent physical model.
Metering reports actual generated power/current instead of the unconstrained budget.
Roof-mount geometry and the other renewable devices are still pending.

Checked probes now guard capacitor and Delay history updates; the destination's
legacy probe and physical-step behavior remains intact. The new finite-solve bound
is prepared lazily so unrelated physical matrix rebuilds do not pay for it.

Validation on the pinned toolchain passes [49 focused tests in 9 suites](forkpatch-port/foundation-tests.json),
including the source reference corpus's 51 deterministic checks and 12,000 seeded
property trials. This is not the full destination suite. Three
[native 26.3 solar game tests](forkpatch-port/native-solar-tests.json) also pass:
loaded I-V behavior/actual production, local weather, node persistence and 2x3
ghost creation/removal. Native tests use physical loads rather than asserting that
an unloaded panel produces its configured maximum power.

### Code checkpoint: array and probe efficiency

An explicit 96-module, 12-series by 8-parallel large-panel circuit passes open,
loaded, shaded, night, recovery and unload checks, including generated power versus
load power and wire losses. It exercises every terminal and wire on the destination
solver; it is an electrical regression, not a live-server TPS measurement.

Checked voltage probes now record live RHS stamps from known exact component
implementations. This avoids scanning unmodified rows while still sampling each
callback on every read. Unknown callbacks and subclasses retain a complete scan,
including direct RHS writes. Tests cover callback changes, moved source terminals,
superposed injections and subclass fallback. Response maps are allocated lazily.

The [paired benchmark](forkpatch-port/probe-benchmark.json) compares two legacy
terminal reads with one checked differential read on the same native LU circuit.
Median wall times after warmup were 0.152/0.104 microseconds (legacy/checked) for
12 voltage states, 0.303/0.080 for 80 and 0.682/0.144 for 240. Both paths measured
zero median allocated bytes per read. These are synthetic sparse-source probes;
they do not predict whole-array speed or multiplayer TPS. Enable the optional
measurement with `ELN26_BENCH_PROBES=1` and the focused performance test.

The [broader checkpoint report](forkpatch-port/checkpoint-validation.json) records
318 passing main tests, 3 skipped tests and one failed fluid coverage assertion in
a deliberately reduced two-case smoke run. That assertion requires at least ten
layered and mixed examples; its separate 20-case rerun passed without changing
fluid code or the assertion. All 15 CAD tests and all three native solar tests also
passed. This is multi-configuration verification, not a passing full default fluid
corpus or validation of the still-pending equipment adapters.

### Code checkpoint: coupled converter runtime

The 3.0.77 regulated controller, reversible safe transformer, retry policy and
bounded connected-group convergence loop now run against the native 26.3 LU
solver. Source branches explicitly open when disabled. Port probes exclude their
own command before projection and preserve commands, masks, pending values and
physical history. A separate scratch constraint handles an unloaded, otherwise
singular ground-referenced port without adding leakage or enabling its source.
Unbalanced injected current remains an invalid port.

The root solver validates candidate power/current/voltage before one physical
flush. Retry clocks advance once per physical step. Delay history now commits in
that flush, so repeated trial solves cannot advance it. Converter port networks
are assembled without private/size partition proxies; unrelated legacy networks
retain the native decomposition and solver. Process groups are cached until their
membership changes; the existing 50 legacy sweeps do not multiply the exact
controller loop.

The registered transformer's isolated mode now uses the safe reversible controller;
its existing non-isolated model, inventory, ratios, IDs, menus and save flag remain
in use. Inventory changes reset the controller's numerical retry timer.

[66 focused regressions](forkpatch-port/converter-focused-tests.json) passed for
the runtime foundation: parallel current sharing across public/private boundaries,
overload/supply recovery, zero/invalid controls, forward/reverse ratio and energy
balance, once-per-step commits, unloaded ports and probe/history preservation.
Native block checks and broader destination validation are recorded separately.

The [broader converter checkpoint](forkpatch-port/converter-validation.json) passes
331 main tests (334 selected, with three optional image-preview tests skipped),
all 15 CAD tests and four native transformer cases. The fluid corpus uses
`-PfluidCases=20`, not the default 200. The native cases include both loaded and
unloaded isolated transformers, their GUI isolation toggle, inventory completeness
and saved-state round trips. A fifth native test verifies the regulated converter
on normal server ticks at 120 V with 144 W of balanced output. Those round trips
exposed and fixed the base loader
reading component state before the saved isolated circuit had been selected.

That converter checkpoint alone did not register the new MPPT/hybrid cabinets. Fully floating
differential islands, the source double-double/gauge solver and stiff high-voltage
port accuracy still require their own adaptation and regressions before those
remaining controllers can be considered complete. Unsupported singular floating
ports are rejected rather than silently grounded.

### Code checkpoint: MPPT and hybrid cabinets

The floor and wall MPPT/hybrid adapters now register their original source identities
7944, 7945, 7951 and 7952, which were unused in the destination. The incoming
harvest/DC-link accounting, output priorities, grid-following export, saved settings
and item energy reset are adapted to native node lists and NBT APIs. Eight
[accounting and array regressions](forkpatch-port/solar-controller-tests.json)
pass, including the real 96-large-panel layout feeding the production MPPT,
night-time DC-link depletion and two MPPTs exporting together.

All eight [native solar tests](forkpatch-port/solar-cabinet-native-tests.json)
pass. Five new cabinet cases exercise floor/wall placement, real loads, grid export,
GUI packets, published display values, thin collision, recorded live geometry,
item energy reset and node save round trips. Three existing panel cases retain
their weather, I-V and ghost checks. Native datagen also succeeds, producing all
four recipes, advancements, item/block definitions and English display names.
The [final destination regression selection](forkpatch-port/solar-checkpoint-validation.json)
passes 338 tests, with three optional preview cases skipped and no failures,
using the same reduced 20-case fluid setting.

Source cabinet geometry and live seven-segment panels record through the existing
26.3 facade. Menus use native ElnScreen controls and element packets. Inventory icons
are reproducible from the imported geometry with `tools/port/solar_icons.py`.
Source terrain-body batching and configuration-tool integration remain pending;
those APIs are not available in this destination foundation yet. Native geometry
recording has been validated; an interactive client GPU screenshot is not claimed.

Model-index generation now includes the existing native generated CAD catalog,
not just shipped OBJ files; all 158 existing entries are retained alongside the
four solar cabinets. Shared `eln:` material resources retain their namespace,
while legacy Windows texture paths still use the native filename fallback.

## Off-grid equipment checkpoint

The next checkpoint adapts all eight 16/32/48/64 kWh floor and wall LiFePO4
banks, the off-grid inverter charger, standalone AC charger, battery MPPT,
fused PV combiner and manual DC disconnect. Source IDs 8192–8204 are retained;
new paid cell modules, cell stacks and fuse cartridges use their source IDs.
None of these entries replaces an existing destination identity.

Native 26.3 thermal loads use a fixed 20 C reference. BMS temperature limits
read that absolute scale; biome ambient remains the native cooling target.
Electrical-rate losses deposit joules with `moveEnergyTo`, so slow thermal
integration cannot multiply charging heat. Charger bypass headroom uses the
existing checked differential LU probe. These choices preserve native behavior
where copying the source implementation would be incorrect.

Models record through the native rendering facade and screens use native menus.
Upper ghost collision matches each thin wall cabinet or floor battery enclosure.
Configuration methods preserve settings only; copying stored battery energy
through a configuration tag is rejected. Moving the actual bank retains its paid
energy. The separate utility transformer/meter/recloser devices and UPS enclosures
are still pending; this checkpoint imports their shared fuse/cell items only.

All 18 adapted source off-grid circuit regressions pass on the native LU solver,
including service headroom, parallel-bank energy balance, night isolation, cold/hot
protection and electrical-rate-independent thermal energy. Native datagen also passes: 29 recipes and advancements, 30 item definitions,
13 blockstates, 43 item models and 43 English names. All 356 previous registry
identities are retained. The full destination test selection passes 356 tests,
with three optional preview skips and no failures (`-PfluidCases=20`; the default
200-case fluid corpus is not claimed). All 21 native tests pass: the 13 new
equipment cases plus eight existing converter, transformer and solar cases.
Wall batteries cover all four orientations. Paid fuse/cell contracts, live displays,
recorded enclosure geometry, thin upper ghost collision, moved battery energy,
configuration whitelisting and node save/load round trips pass.

Reports: [circuit tests](forkpatch-port/offgrid-circuit-tests.json),
[native cases](forkpatch-port/offgrid-native-tests.json),
[datagen](forkpatch-port/offgrid-datagen.json), and
[destination regressions](forkpatch-port/offgrid-checkpoint-validation.json).
An interactive client GPU screenshot and the full 200-case fluid run are not claimed.

A one-pole disconnect registers only its actual contact and terminals. Carrying
over the unused second open pole made the native matrix singular; it is removed
in this adapter, with open/reclose behavior covered by a dedicated regression.

## Standby and ATS checkpoint

Seven source identities are now registered: floor/wall ATS cabinets and all five
standby gasoline generator tiers (10, 20, 40, 80 and 240 kW). Native screens and
recorded front panels expose rated voltage, channel, engine mode, fuel and the
active feed. Source start/cooldown/transfer delays and fault latches are retained.
The ATS sends a one-shot reset to faulted loaded generators on its case-sensitive,
dimension-scoped channel, without changing their fuel or interrupting healthy peers.

The 26.3 adapter uses the destination's transactional fluid capability and native
container interaction. Fluid-stack codecs preserve fluid components, while a paid
whole-mB energy reserve prevents pipes from reclaiming already consumed fuel.
Fuel acceptance compares configured registry paths directly rather than allocating
a scan of the complete fluid registry on each insertion. The existing fuel
generators and fluid network implementation remain unchanged.

Larger source tiers lacked survival recipes; native upgrades combine two of the
previous tier (three for the extreme tier), preserving their installed capacity.
All seven entries have geometry-derived inventory icons. All 19 adapted electrical/controller/link regressions pass with a metered fuel
budget. All 31 selected native cases pass, including ten new standby cases
and the previous 21 solar/off-grid/converter/transformer cases. Checks cover
real bucket transactions, pipe capabilities, fuel components and fractional-reserve
codecs, all generator multiblocks, floor/wall ATS transfers, linked fault reset,
recorded live display geometry and native node save/load. Fuel-ledger and reset
checks use an absolute 1e-7 J tolerance. The full destination suite passes
375 tests with three optional preview skips and no failures (`-PfluidCases=20`). Native datagen passes for seven recipes and
advancements, seven blockstates/items, 14 item models and 14 English names.
The original 26.3 200 V fuel generator supplies the small standby recipe
(the source called it 240 V), preserving the existing device and ID.
Interactive client validation and the default 200-case fluid run are not claimed.
The source mixed-voltage pole-feeder case awaits the utility equipment port.

Reports: [controller tests](forkpatch-port/standby-circuit-tests.json),
[native cases](forkpatch-port/standby-native-tests.json),
[datagen](forkpatch-port/standby-datagen.json), and
[destination regressions](forkpatch-port/standby-checkpoint-validation.json).

### Code checkpoint: floor and wall UPS racks

The floor Programmable UPS (7947) and wall Programmable UPS (7953) now have
native production adapters, source cabinet models and live status displays. Both
24-cell racks appear together in the native inventory menu. Actual removable
battery items supply their own capacity, charge and discharge ratings; the existing
26.3 portable batteries keep their native specifications. Only complete racks
activate. Native inventory read/remove/save/clear paths flush cached cell energy
before transferring the physical item, including legacy hopper/container methods.

The online charger and inverter share an energy-accounted internal bus. Manual
bypass supplies the input directly without backup. The native LU solver requires
a voltage reference at disconnected terminals: local high-resistance sense paths
supply it without changing the shared solver or adding an idle output load.
Electronics losses enter the native thermal system as physical joules per step.
The recipe uses the destination's existing DC-DC Converter for the source's
Variable DC-DC Converter ingredient, preserving the native device and ID.

All 16 focused UPS electrical/controller regressions pass. All 40 selected native
world cases pass, including nine new UPS cases: floor outage/restore, an empty
unloaded rack, all four wall orientations, real battery inventory/menu transfers,
and generator stop/restart and empty-bank startup. Checks cover exact cell-energy
preservation (absolute 1e-6 J tolerance), slot filters, legacy extraction, cached
energy flush on clear, shift-click, native item capabilities, recorded display
geometry, packets and node persistence. Datagen passes for two recipes and
advancements, two blockstates/item definitions, four item models and four English
names. All 393 previous registry identities and 176 geometry models are retained.
The destination suite passes 391 tests with 3 optional preview skips
and no failures (`:test -PfluidCases=20`); the default 200-case fluid corpus was not run.

The compact four-cell plug-in UPS remains pending with the appliance cord/socket
layer; it is not registered as a partially functional device. Interactive client
graphics/audio checks are pending. The UPS keeps the native thermal convention:
`Tc` is temperature rise above its 20 C reference, so the source 90 K rise cutoff
corresponds to 110 C absolute in this adapter.

Reports: [circuit tests](forkpatch-port/ups-circuit-tests.json),
[native cases](forkpatch-port/ups-native-tests.json), and
[datagen](forkpatch-port/ups-datagen.json), and
[destination regressions](forkpatch-port/ups-checkpoint-validation.json).

### Code checkpoint: protective relay and priority load shedding

The source Protective Relay Cabinet (8066) and Automatic Load Shedding Controller
(8070) now have native production adapters, source enclosure/panel geometry and
native menus. Only those two industrial kinds are registered in this checkpoint;
the shared kind metadata does not imply implementations of the seven other devices.
The relay retains paired forward/return contacts, voltage/current/differential
limits, physical-time delayed latching and a wired trip input. Enabling a tripped
relay never resets its protection. The shedder measures all four actual branches,
sheds P4 first, restores P1 first, remembers disconnected demand and checks voltage
and spare capacity before retrying. Settings, relay state and remembered demand
persist with source NBT keys. Packet parsing applies only complete validated settings.

The native solver gets local high-resistance terminal references for disconnected
circuits. Healthy contacts are assigned only their desired state, avoiding the
source's open/reclose sequence and unnecessary LU invalidations. Timers advance
once after each accepted physical solve, never in nonlinear retry loops.
Menus expose the six settings, live measurements, branch priorities, delay timers
and explicit trip/reset/retry controls. Source assets are adapted to the native
recorded GL facade. All 12 focused production-circuit tests pass, including paired
return/real ground leakage, delayed latching, signal trips, priority restoration,
headroom/retry, exact power balance including voltage-sensing losses, atomic invalid
packet rejection and save/load. A steady-state regression verifies unchanged native
LU matrix versions over 100 physical steps for each healthy device.

All 46 selected native port cases pass, including six new relay/shedder scenarios.
These cover actual gate input/output terminals, paired-conductor leakage and reset,
latched overcurrent, headroom/retry and undervoltage restoration, wired inhibition,
all four front orientations, published measurements, recorded enclosure/panel
geometry, the native menu specification and saved-node state. Datagen passes for
two recipes/advancements, two blockstates/item definitions, four item models and
four English names. All 395 earlier registry identities and 178 geometry models
are retained. The destination suite passes 403 tests with 3 optional preview skips
and no failures (`:test -PfluidCases=20`); the default 200-case fluid corpus was not run.
Interactive client validation and the seven other industrial devices remain pending.

Reports: [circuit tests](forkpatch-port/industrial-control-circuit-tests.json),
[native cases](forkpatch-port/industrial-control-native-tests.json), and
[datagen](forkpatch-port/industrial-control-datagen.json), and
[destination regressions](forkpatch-port/industrial-control-checkpoint-validation.json).

### Code checkpoint: passive pad transformer and primary fuse

The Pad Mounted Neighbourhood Transformer (8068) and five physical transformer
items (7879–7883, 10/25/50/75/100 kW) now have native adapters. Existing IDs are
retained. The capacity is supplied by an installed item; voltage settings cannot
upgrade it. A physical primary fuse is required. The 10 kW core recipe uses the
native DC-DC Converter for the source Mains Step-down Unit, which is not ported yet;
subsequent source recipes consume the previous capacity and additional copper/iron.
The source legacy Mains Step-down item fallback/migration remains pending with that
device. The pole transformer item visual extracts the source can and bushing
submeshes without registering the unported overhead pole.

Source passive service taps are adapted to the existing native `getCurrentInto`
API. Primary and secondary terminal currents differ by the physical turns ratio;
no global simulator/current interface replacement is needed. One paid winding
supplies both scalar 120 V legs and the full secondary, sharing the core's capacity
and following primary sag. Source ELN scalar service equivalents are retained;
this is not an explicit AC phase/neutral simulation. Voltage-sensing references
support disconnected terminals in the native LU model.

Primary overcurrent consumes the installed fuse after its physical two-second
delay. Transformer overload latches a separate trip and leaves a healthy primary
fuse intact. Changing fault type restarts the timer. Settings, fault reason and
pending timer use source NBT keys; installed hardware persists only in the actual
inventory. The native menu exposes both slots, primary/secondary programming,
actual primary and secondary amps, installed fuse/core ratings and explicit reset.
Native pipe/hopper extraction and insertion use the destination inventory APIs.
The source pad enclosure, compartment door and fuse-blow sound are included.
All 12 production-circuit tests pass, covering the user's 145 A / 240 V load on
a 200 A primary link, separate core-overload protection, actual primary shorts,
fault-type timer separation, pending-delay persistence, shared capacity across the
service legs, primary sag, bidirectional power balance, accurate terminal currents,
missing hardware, invalid packets and stable native LU factors over 100 healthy
steps. All 52 selected native port cases pass, including six new pad cases. These
exercise actual paid transformer/fuse items, physical fuse consumption/replacement,
shared service loading and sag, native item/panel geometry, sided inventory rules,
menu slots, signal output, packet publication and world-node persistence. Carrying
the cabinet's settings cannot copy installed hardware.
Datagen passes for six recipes/advancements, six item definitions, one blockstate,
12 item models and seven English names, with no missing transformer icon warnings.
All 397 previous registry identities and 180 geometry models are retained.
The destination suite passes 415 tests with 3 optional preview skips
and no failures (`:test -PfluidCases=20`); the default 200-case fluid corpus was not run.
The other utility
equipment, six remaining industrial devices and interactive client validation remain pending.

Reports: [circuit tests](forkpatch-port/pad-circuit-tests.json) and
[native cases](forkpatch-port/pad-native-tests.json),
[datagen](forkpatch-port/pad-datagen.json), and
[destination regressions](forkpatch-port/pad-checkpoint-validation.json).

### Wired service checkpoint

Native adapters now cover the compact North American Meter Base (7987), Wide
Underground Meter Socket (8128), Underground Service Entrance Riser (8129),
service handhole (8130), Red Handle Service Disconnect (7992), and removable
analog/smart meter items (7877/7878). These are placed-cable devices. The following
overhead checkpoint adds their weatherheads and span endpoint integration.

One real 240 V wire supplies the proportional scalar 120 V legs through passive
taps. Meter contacts measure signed physical power; import and export registers
remain independent. The meter item retains its serial, counters and smart open
state when removed, swapped, extracted or saved. Native legacy inventory methods
flush the immutable item data before transfer; empty socket settings cannot copy
meter readings. A native custom-data write reuses one copied root instead of
copying it separately for comparison and modification.

The wide socket reserves a second wall cell using the existing directional thin
cabinet ghost shapes. Both the node and ghost retain wall-depth collision, avoiding
an invisible full cube. Native topology hooks validate the two separate downward
entries and prevent the adjacent wall downleads from bypassing the meter. Wiring
uses normal native NodeConnection lifetimes, reconnection and simulation-veto
semantics. Weak cable markers avoid per-tick world searches and strong owner
retention; topology preparation runs only during connection construction.

The native block's cached legacy fallback previously bypassed tile-dependent wall
collision helpers. Wall blocks now opt into dynamic shapes; a descriptor-oriented
hook supplies thread-safe cached shapes for their four fronts. It serves the
service cabinets and earlier wall-mounted power cabinets while retaining the
existing model-hitbox path, floor fallback and fixed-shape fast path (no extra
block-entity lookup for unrelated devices). Native cabinet cases query the
block's actual collision shape, including both the origin and reserved battery/
meter cells, rather than accepting the descriptor helper alone.

The source analog dials/disk, smart LCD pages, glass, red handle/blades and cabinet
geometry are adapted to recorded native GL. Twelve production-circuit tests pass,
including actual passive service voltages/sag, isolated contacts, separate import/
export registers and unchanged LU factors across 100 stable steps. All 65 native
ForkPatch cases pass, including 13 service cases. They check both placement orders
and all four fronts with real rated wall cables, isolation during open/empty/removed
meters, freeze/reconnect without a bypass, real collision on both cells, legacy
cube-ghost repair, actual item inventory/serial/NBT transfers, live recorded
geometry/publication and native node persistence. Earlier wall MPPT/UPS/ATS/battery
cases also now check actual origin collision.

Native datagen passes: five blockstates, seven item definitions, 12 item models,
seven recipes/advancements and 12 English names. Seven new identities and five new
model entries retain all 403 previous identities and 182 earlier geometry models.
All destination root tests with `-PfluidCases=20` pass: 427 passed, three optional
preview cases skipped, zero failures/errors (430 selected across 90 suites). The
default 200-case fluid corpus was not run.

Interactive client validation and complete source-world migration remain pending;
the following checkpoint covers the next overhead-service work.

Reports: [production circuits](forkpatch-port/service-circuit-tests.json),
[native service and previous cases](forkpatch-port/service-native-tests.json),
[datagen](forkpatch-port/service-datagen.json), and
[destination regressions](forkpatch-port/service-checkpoint-validation.json).

### Overhead service checkpoint

Four source identities now have native adapters: Service Weatherhead (7986),
Compact Side Entry Service Mast (8028), Through Roof Service Mast (8029), and
Gooseneck Service Mast (8030). Their source models, item icons and recipes are
included. Masts accept either a three-wire service bundle or an individual service
circuit and continue into placed wall cables; meters, disconnects and underground
risers reject new overhead links. Existing saved meter/service terminals still
have their original independent buses rather than gaining parallel split taps.

The native span layer now has integer terminals with legacy Direction encodings
0..5 retained. Three-channel bundles retain independent electrical loads and pay
for three conductor lengths. Wire resistance remains additional to terminal
resistance across resistance notifications, with no per-step span-world lookups.
The destination LU solver, native pole ratings and normal connection objects are
retained. Chunk copies merge into one canonical span; disconnect removes spans
before their terminal states, and native simulation veto prevents reconnection
into frozen endpoints. Endpoint references resolve against the node manager rather
than retaining replaced nodes after reload. Breaking refunds paid cable once,
including its native item components.

The native catenary packet supports one/three service wires and retains the legacy
two-wire crossing optimisation. Unchanged wire geometry reuses native display lists
across live voltage/status packets, and renderer disposal releases the lists. The
same snapshot can restore released geometry. Wall shapes use the native oriented-shape cache.
Standard weatherheads reserve non-solid clearance with native ghost metadata 8;
metadata 3..7 retain their existing cabinet/battery meanings. Roof masts permit
real roof blocks above the wall base.

Eight span circuit tests and the twelve earlier meter circuit tests pass. All 85
selected native server cases pass: 20 new overhead cases and 65 preceding cases.
The new cases cover all four mast styles in all four wall orientations, independent
120/120/240 V circuit equivalents (including asymmetric supply), physical loads,
bundle/single duplication rejection, actual click payments and component-preserving
single refunds, menu/carry state, roof blocks, clearance ghosts, native collision,
publication/catenary geometry, status-only mesh reuse, disposal/restoration,
canonical reload merging, unloaded endpoints and simulation-veto exclusion. Both
existing native utility-pole types retain actual load power and the DC-DC ratio,
legacy Direction terminal tags, two-wire rendering and native node persistence.

Native datagen passes: four blockstates, four item definitions, eight item models,
four recipes/advancements and eight English names. The four new identities and
model entries retain every one of the 410 earlier identities and 187 model entries.

All destination root tests with `-PfluidCases=20` pass: 435 passed, three optional
preview cases skipped, zero failures/errors (438 selected across 91 suites). The
default 200-case fluid corpus was not run.

At this checkpoint, utility spools/thermal models remained pending; the following
single-core checkpoint adds them. New distribution poles/phase routing, multicore
wires, advanced span geometry, complete source-world migration and interactive
client validation remain pending.

Reports: [production span circuits](forkpatch-port/overhead-circuit-tests.json),
[native cases](forkpatch-port/overhead-native-tests.json),
[datagen](forkpatch-port/overhead-datagen.json), and
[destination regressions](forkpatch-port/overhead-checkpoint-validation.json).

### Single-core utility cable checkpoint

The source single-core catalog is now adapted: 90 regular copper/aluminum bare,
insulated and damaged variants, 20 save-stable copper HV variants, both molten
metal pile states, and Wire Scraps. Their 113 identities preserve the source ids. At this checkpoint, multicore ids
2268..2327 remained reserved; the following multicore checkpoint registers them.
The former native cable identities, voxel-kit behavior and protection are retained.

Spools are native single-stack items with immutable custom-data length/identity.
Placement spends 1 m, while overhead bundles spend every conductor's length;
creative overhead placement keeps the spool. Fractional lengths and unrelated item
components survive cuts and refunds. Broken placed wire returns its actual one-metre
metal mass as scrap instead of creating a fresh default spool. Hidden damaged states
and molten piles remain out of the creative tab; intact creative spools use 128 m.

DC resistance uses source material, cross-section, length and absolute temperature.
Native thermal transfer now calls an energy-integration hook: ordinary loads retain
their previous calculation, while wire loads integrate sensible and latent heat.
They retain overshoot, cooling, queued electrical joules, precise temperature and
partial fusion across disconnects, replacements and saves. Native fixed-reference
Tc is kept distinct from biome ambient and source ambient-relative saves. Junction
heating sums real incident I²R rather than squaring an aggregate branch current.

Jacket failure replaces a placed wire with its hot exposed-conductor variant;
completed fusion leaves a nonconducting molten pile of the same metal mass. Finite
100-ohm insulation faults latch and consume physical power. Topology changes run
on the native server task queue and preserve voxel slot/offsets. Span cores each
own only their paid length and thermal mass, accumulate actual I²R joules, sample
endpoint climate at most once per second, and bound resistance drift to 0.01%.
Heat and jacket damage survive canonical span reloads; failed metal opens the
span, and manual removal returns the paid damaged length and components once.
Native cable/glow/pile rendering and live temperatures are adapted.

Twenty-three focused source/adapter regressions pass. Nine new native wire cases
pass alongside all 85 prior cases (94 total): catalog/stack rules, actual placement,
junction and span heat, hot-state reload, damage/fusion transitions, insulation
faults and component-preserving refunds. Native datagen passes with 113 new item
definitions, item models and English names, retaining all 414 preceding identities
and 191 geometry models. No manufacturing recipes are added at this checkpoint.
The destination regression run passes 458 tests across 94 suites, with three
optional CAD preview cases skipped (461 selected, zero failures/errors), using
`-PfluidCases=20`; the default 200-case fluid corpus was not run.

Scope limits at this checkpoint: the new spools were available through creative
inventory, with source wire-processing machinery and manufacturing recipes pending.
Placed multicore routing follows below. Device-inventory cable trimming, snips/config-copy tools,
player-contact shock hooks, room-temperature coupling, advanced per-material span
meshes, complete source-world migration and interactive client checks remain pending.
The original native cable recipes and devices remain usable.

Reports: [wire circuits and thermal physics](forkpatch-port/wire-circuit-tests.json),
[native cases](forkpatch-port/wire-native-tests.json),
[datagen](forkpatch-port/wire-datagen.json), and
[destination regressions](forkpatch-port/wire-checkpoint-validation.json).

### Multicore placed-cable checkpoint

All 60 source multicore identities (2268..2327) are now adapted: copper signal
bundles, flat building cables, copper/aluminum triplex and quadruplex, and their
hidden damaged states. Intact bundles keep two to eight independent native MNA
loads. One metre of jacket contains every core's metal; spool lengths, shared
thermal mass, heating and scrap accounting use the total cross-section. The
jacket-damaged state retains that mass as one exposed conductor, as in the source.

Connections expand only while constructing topology. Bundle-to-bundle links match
colors, including palettes with different array orders; a scalar colored tap selects
one matching core, while an uncolored device selects the default hot. An absent
color makes no electrical connection. Direct uncolored ground terminals select a
protective core and cannot ground a hot in a bundle without one. Native face,
voxel boundary, internal corner and off-centre pin paths use the same routing;
their existing lifetime containers own every added link. Ordinary scalar routing,
bus links, native connection masks and simulation veto remain in place.

Palette changes reconnect the native topology and persist by the source palette
index. Insulation stress checks all core pairs and installs one finite 100-ohm
fault at its saved endpoints. Shared heating sums every core's accepted I²R and
fault power. Flat cable geometry displays conductor colors; source body dimensions,
damaged texture, native knuckles and live temperatures remain adapted. The native
HUD lists every core and preserves its existing Java return signature.

Six routing regressions pass. All 105 native server cases pass, including 11 new
multicore cases: catalog identity/mass/stack rules, actual eight-core boundary and
inner-corner circuits, heating and hot saves, scalar taps, palette reconnects,
finite fault endpoints, direct protective grounding, distinct flat-render colors,
per-core HUD readings, native freeze/thaw and mass-preserving jacket failure.
Datagen passes with 60 new item definitions, item models and English names,
retaining all 527 preceding identities and 191 geometry models. All destination
regressions pass with `-PfluidCases=20`: 464 passed, three optional CAD preview
cases skipped, zero failures/errors (467 selected across 95 suites). The default
200-case fluid corpus was not run.

Scope limits: multicore overhead phase mapping/payment and automatic grounding
through chains of uncolored single wires are pending. New multicore overhead links
are rejected until their adapter is ready; existing single-wire service spans stay
available. Manufacturing recipes/machines, inventory trimming/tools, player shock
hooks, room coupling, full source-world migration and interactive client checks
also remain pending.

Reports: [routing regressions](forkpatch-port/multicore-routing-tests.json),
[native cases](forkpatch-port/multicore-native-tests.json),
[datagen](forkpatch-port/multicore-datagen.json), and
[destination regressions](forkpatch-port/multicore-checkpoint-validation.json).

### Multicore overhead payment checkpoint

New intact multicore spools can supply a terminal bundle with the same conductor
count. A three-core jacket spanning 4 m pays 4 m of spool; three separate scalar
wires still pay 12 m. Every active core retains 4 m of its own physical metal,
resistance, I²R heat and cooling. Mismatched core counts and scalar-terminal use
are rejected before payment. Existing service circuits remain the native scalar
L1/L2/240 V equivalents; this does not introduce a new AC neutral solver.

New jacket-length records save an explicit `spanBundleMeters` marker. Unmarked
source records retain their earlier total-conductor-length interpretation instead
of silently changing paid lengths. Canonical reloads preserve each core ledger,
and damaged refunds return exactly the paid jacket length with its full metal
cross-section and unrelated item components, once. The new plan runs only during
link construction; it adds no per-tick routing scans.

Six payment/routing regressions pass. All 110 native server cases pass, including
five new cases covering actual clicks, independent voltage/heat, hot reloads,
damaged refunds, duplicate/creative payments, incompatible cores and old records.
The following distribution checkpoint also validates the complete destination
regression suite, including these changes. Manufacturing and full source-world migration remain pending.

Report: [bundle validation](forkpatch-port/bundle-span-validation.json).

### Distribution protection and phase-routing checkpoint

Twelve source pole variants are adapted with their original identities: single-
and three-phase utility poles, the three-phase underground feeder pole, single-
and three-phase fused cutouts, the three-phase recloser, the secondary pole,
capacitor bank, single-/three-phase regulators, the gang-operated air-break switch
and the three-phase pole with a fused single-phase tap. The remaining eighteen
transformer, light, underground and related variants stay reserved rather than
registering nonfunctional devices.

Primary phases stay electrically independent. Cutouts consume physical cartridges
and keep blown/open states through native saves and carried items. Reclosers trip,
retry, lock out and require explicit reset; manual opening does not auto-retry.
Regulators use native power-conserving passive taps, moving at most 1% each second
within a 90–110% range. Capacitor banks retain the native capacitor history model.
Healthy contacts and steady taps retain native LU factors, and no primary pole
invents a secondary supply. Protection skips the stale measurement immediately
after a contact closes. Fuse/arc/recloser sounds and status publications follow
actual contact events, not every simulation tick.

Single-conductor runs inherit upstream phase identity at link/configuration time.
Identity-based traversal terminates through loops and follows both sides of a
single-phase cutout even when open. Three-phase legacy aliases retain their actual
phase-A identity. A fused tap cannot switch a separately fed branch to another
phase. Bundles cannot duplicate individual phase links. New phase work adds no
per-tick world traversal.

The native controls expose line/load or service terminals, fuse operation, tap
selection, recloser settings/reset and regulator target/mode. Source pole geometry,
animated cutout carriers, equipment leads, all four orientations, native catenary
cache/disposal behavior and real crossarm clearance are adapted. Rack attachment
points match the rendered service insulators. Twelve reproducible inventory icons
are generated with `python3 tools/port/distribution_icons.py`.

Fifteen native production-circuit regressions pass. All 132 native server cases
pass, including 22 distribution cases: all twelve devices, protection/carry/packet
state, paid cartridge insertion, three-phase power/payment, open-fuse inheritance,
phase changes, legacy aliases, a twenty-pole cycle, geometry and actual clearance.
Datagen adds twelve recipes/advancements, blockstates and item definitions, twenty-
four item models/English keys, one geometry model and three sound events. All 587
preceding identities and 191 geometry models are retained, with no missing icons.
The source Direct Utility Pole recipe ingredient uses the existing native Utility
Pole until the separate source base equipment is adapted.

All destination regressions pass with `-PfluidCases=20`: 485 passed, three optional
CAD preview cases skipped, zero failures/errors (488 selected across 97 suites).
The default 200-case fluid corpus was not run. Transformer/light/
underground poles, height/surge/streetlight attachments, independent adaptive rack
alignment, wire manufacturing, full source-world migration and interactive GPU/
audio validation remain pending. Circuits keep the native scalar phase/service
equivalents; this checkpoint does not add an AC phasor/neutral solver.

Reports: [production circuits](forkpatch-port/distribution-circuit-tests.json),
[native cases](forkpatch-port/distribution-native-tests.json),
[datagen](forkpatch-port/distribution-datagen.json), and
[destination regressions](forkpatch-port/distribution-checkpoint-validation.json).

### Transformer pole checkpoint

The single- and three-phase transformer poles now retain source IDs 7972/7973,
bringing registered distribution variants to fourteen. Both use one installed
10/25/50/75/100 kW physical can. The three-phase pole selects one primary phase;
it does not silently install three transformers. All 120/120/240 V service
outputs share that can and conserve power through the destination's passive
linear taps. Both source 120/240 V winding settings, primary sag and reverse
power are supported without nonlinear converter probes. Steady operation keeps
cached native LU factors.

Primary cartridges measure actual primary current. A 145 A, 240 V secondary
load leaves a healthy 200 A primary cartridge intact. Separate delayed core
protection aggregates every service output, and resetting a core trip cannot
restore a spent cartridge. Copper losses enter native thermal loads as joules
once per accepted physical step; the relative-to-ambient temperature API is
preserved. An overheated can disconnects until cool.

The real one-slot menu supports paid core installation, shift-click extraction,
sided item automation and inventory clearing. Every extraction path immediately
removes capacity. Native saves retain core, cartridge and control state; carried
settings cannot duplicate the core. The live panel publishes capacity, winding,
phase, temperature and fault status. Installed can dimensions, bushings and leads
follow all five source ratings, with two reproducible inventory icons.

Fourteen new native-MNA regressions and all 144 native server cases pass, including
twelve new placed-device cases. Datagen adds two recipes/advancements, blockstates
and item definitions, four item models/English keys and two icons; all 599 prior
identities remain unchanged. Full destination regressions are rerun after the
next equipment increment. Source-world migration, adaptive
rack alignment/clearances, remaining sixteen pole variants and other unfinished
workstreams remain pending; this is a development checkpoint, not a release.

Reports: [production circuits](forkpatch-port/pole-transformer-circuit-tests.json),
[native server cases](forkpatch-port/pole-transformer-native-tests.json).

### Service lighting and single-phase feeder checkpoint

The single-phase underground feeder pole now uses source ID 8131, with its fused
primary reaching the bottom Up/left base terminals and an independent secondary
rack. Five older built-in street-light poles retain source IDs 7979/7984/7985/
7989/7990, remain hidden from creative inventory and have no new recipes, as in
3.0.77. Their actual 70 W loads, power-dependent brightness, dusk mode and fixture
height/yaw/pitch are adapted, including the older transformer lamp save keys.
Twenty distribution identities now work; fifteen are current creative variants.

New builds use the paid Pole Streetlight Attachment (source shared item 7896).
Right-clicking one of the adapted non-light distribution poles consumes one item;
duplicate installation consumes none. The native pole menu controls supply,
switching, dusk operation, height and aim (Shift-click reverses each adjustment).
Automatic 120/240 V selection has source 25% hysteresis and draws from only one
service circuit, without connecting the service buses together. Fixed supply
selection does not silently fall back. Steady lamps keep native LU factors.

Brightness follows accepted electrical power; no supply produces no light and
voltage sag dims the fixture. The rendered head and temporary native world light
share placement math. Refreshes avoid loading chunks, and world light expires
after switch-off/removal. Native saves retain paid installation and validated
controls. Removal/breakage refunds one item, and carried pole settings exclude
paid attachment presence. Other source pole families' attachment adapters and
adaptive alignment remain pending.

All eighteen new production-circuit cases and all 166 native server cases pass,
including the former 144 and twenty-two new placed-device cases. The native tests
exercise real insulated service spans, each legacy lamp kind, world light expiry,
controls, model pose/illumination, the fused feeder, payment, actual reload and
single break refunds. The source Basic Lamp Holder recipe ingredient uses the
destination Simple Lamp Socket until that source holder is adapted. Full destination regressions are rerun after the final
equipment increment in this work block.

Reports: [lamp circuits](forkpatch-port/pole-lamp-circuit-tests.json),
[attachment circuits](forkpatch-port/pole-attachment-circuit-tests.json),
[native cases](forkpatch-port/pole-lamp-native-tests.json), and
[generated data](forkpatch-port/pole-lamp-datagen.json).

### Underground distribution and current variant parity

The fused underground service pedestal, single-phase fused underground feeder
cabinet and transformer pole with underground service riser retain source IDs
8032/8033/8133. All eighteen variants exposed for new construction by ForkPatch
3.0.77 are now registered, alongside five hidden older lamp variants. Seven other
hidden/retired variants still need legacy adapters; this does not complete the
whole ForkPatch port. The explicit per-variant [parity report](forkpatch-port/distribution-parity.json)
links every implemented identity to a passing native placement/power case.

The pedestal has actual incoming and outgoing L1/L2/240 V equivalents. One fuse
observes combined 240 V-equivalent service current and isolates all three outputs.
A placed 240 V feed passively supplies its two 120 V equivalents, without copying
power or invoking converter probes. Independent overhead channels remain separate.
The same wired split is used by the single-phase feeder's independent service
entry. Healthy taps/contacts retain cached LU factors, and reverse power is
conserved. A spent physical cartridge remains spent through saves/reset attempts.

The primary cabinet exposes separate incoming/fused outgoing HV terminals and
independent LV services. Both cabinets reject new overhead spans, reserve their
real second block instead of pole-height clearance, and use the source enclosure,
fuse/carrier and service-lug models. The buried-service transformer keeps one paid
can and shared capacity, with a visible service riser and actual underground
outputs. Multimeter readings and HUD faults reflect these real circuits. Cabinet
crafting retains the source shaped bill of materials in the native shapeless
callback; original registry identities remain unchanged.

Eight new production-circuit regressions and all 175 native server cases pass.
Native coverage includes physical placed service feeds, aggregate cartridge trips,
reverse flow, primary/secondary separation, 145 A transformer service, all cabinet
orientations, actual two-block clearance and overhead-link rejection. Data
validation and the final destination regression totals accompany this checkpoint.

Reports: [production circuits](forkpatch-port/underground-distribution-circuit-tests.json),
[native cases](forkpatch-port/underground-distribution-native-tests.json),
[generated data](forkpatch-port/underground-distribution-datagen.json), and
[variant parity](forkpatch-port/distribution-parity.json).

### Final distribution validation and provenance audit

The completed work-block suite passes 526 destination regressions with three
optional CAD preview skips, zero failures/errors (529 selected, 101 suites,
`-PfluidCases=20`). All 175 native server cases also pass against the final code,
including forty-three new placed-device cases in this block. Forty-one new
production regressions were added. The final transformer case independently
sums accepted winding losses and verifies that startup and the final powered
interval before a fuse opens both deposit their joules exactly once. The original
one-argument JVM step method remains available.

All 599 pre-block registry identities and 858 English keys remain unchanged.
The block adds eleven pole identities plus the attachment item, twelve icons,
seven recipes/advancements, twenty-three English keys and two raw geometry models.
Gradle configuration and wrapper are unchanged. Detailed evidence is in the
[final validation report](forkpatch-port/distribution-work-block-validation.json).
The full ForkPatch migration remains in progress.

The [import audit](forkpatch-port/import-audit.json) checks all 161 ledger entries,
covering 108 distinct source files and 151 target paths. Every source hash matches
3.0.77, all target files exist, and all eighteen declared exact-copy entries match;
twenty-eight entries are byte-identical overall. This verifies provenance, not
complete gameplay parity. Reproduce it from this checkout with:

```sh
python3 tools/port/audit_imports.py --source ../ELN1.21.1ForkPatch
```

For further adapters, add explicit `transparent_node`/`shared_item` identities
before runtime tests, give raw model-index entries their `folder/file.obj` paths,
and place inventory icons at `textures/item/<registry-name>.png`. Target thermal
`Tc` is relative to ambient; processes running at electrical-step frequency must
use `moveEnergyTo(watts * seconds)` when their thermal load steps less often.
Real ItemStack component/inventory behavior belongs in native server cases.

### Legacy fused-tap lighting and retired cabinet checkpoint

The hidden fused-tap secondary-fed street-light pole (8022) and retired three-
phase underground feeder cabinet (8034) now retain their source identities and
working native adapters. They remain outside creative inventory and gain no new
crafting recipes. The new cabinet uses the unchanged source three-phase model;
both variants have reproducible inventory icons.

The lamp's actual 120 V service supply remains independent of the protected HV
tap. A spent tap cartridge opens its real branch while the supplied lamp and
through primary phases stay powered. Phase selection still rejects a connection
that would bridge separately fed phases. The retired cabinet keeps three separate
incoming/fused outgoing primary circuits; a fault on one phase leaves the other
two healthy, and no primary circuit invents LV service. Native reloads preserve
blown cartridge state; extracting it returns the spent physical item.

Six new production regressions pass. The complete destination run passes 532
regressions, with three optional CAD preview skips and zero failures/errors
(535 selected, 102 suites, `fluidCases=20`). All 182 native server cases pass,
including seven new cases for placement/power, lamp power and world-light expiry,
phase-change guarding, physical cartridge faults and actual reloads. Datagen adds
two block/item definitions, four models/English keys, one raw geometry model and
two icons, retaining all 611 preceding identities; neither hidden item gains a
recipe or advancement.

The registry now covers 25 of the 30 source distribution variants: all eighteen
current construction variants and seven hidden older variants. Five hidden
legacy adapters and the wider ForkPatch migration remain incomplete. See the
[validation report](forkpatch-port/legacy-distribution-validation.json),
[updated parity](forkpatch-port/distribution-parity.json), and
[continuation contracts](forkpatch-port/next-distribution-work.md).

## Inventory

`forkpatch-port/source-inventory.json` records every incoming main-source file,
its content hash, whether the 1.21.1 build includes it, and the same-qualified-name
destination file if one exists. The initial inventory has 1,277 source files:
778 active names absent from the destination, 448 shared names needing review,
38 byte-identical sources needing runtime checks, and 13 excluded by the source
build. Renamed classes and Kotlin files with multiple declarations mean these are
file-review counts, not counts of missing devices.

Regenerate from this checkout with:

```sh
python3 tools/port/forkpatch_inventory.py --source ../ELN1.21.1ForkPatch
```

Registry identities must be mapped explicitly against `eln-ids.json`. Existing
26.3 identities cannot be repurposed for incoming devices. Save compatibility must
be tested against actual target saves and explicit legacy mappings.

## Build and validation

Use full JDK 25 and JDK 21 installations, as documented by the destination's
`flake.nix` and `gradle.properties`:

```sh
export JDK21=/path/to/jdk21
export JDK25=/path/to/jdk25
export JAVA_HOME="$JDK25"
export PATH="$JAVA_HOME/bin:$PATH"
./gradlew --no-daemon --max-workers=2 test
```

First verify the unmodified destination, then run the migrated module's tests and
the destination regressions. Native game tests and client checks follow the
production registry/menu/render adapters; passing pure state-machine tests alone
does not establish working in-game equipment.

### Legacy lighting completion checkpoint: 29 of 30 variants

The combined transformer/fused-tap streetlight (8023), lighted underground
single/three-phase cabinets (8035/8036), and lighted single-phase feeder (8132)
now have production adapters. The combined pole has independent transformer and
HV branch contacts, current measurements and physical fuse latches. A fault in
either path leaves the other path operating. Its tap cutout has its own rendered
offset; phase changes retain the native span guard. The native passive transformer,
accepted copper heating and cached LU factors remain in use.

Cabinet lamps draw actual upstream LV power, use source models with a five-block
fixture reservation, and retain non-solid upper clearance cells. The feeder lamp
explicitly chooses one 120/240 V service, not both. New variants retain their exact
source offsets and hidden identities and have no new construction recipes.

Validation: 545 regressions passed, three optional CAD previews skipped; 200
selected native server cases passed; data generation passed for 617 identities
without missing icons. The reduced fluid setting was 20, not the default 200.
See [the report](forkpatch-port/legacy-lighting-validation.json). Twin-pole recovery
and broader migration remain pending. An initial unfiltered smoke run found the
missing cabinet model-index entries (fixed) and separate gallery/menu findings;
[it does not establish a passing full-native baseline](forkpatch-port/unfiltered-native-smoke-findings.json).

### Twin LED pole and standalone step-down checkpoint

All thirty distribution variants now have native production adapters. The twin
secondary pole shares one 120 W load across its two source LED heads and explicitly
selects 120 or 240 V service. Former HV spans are rejected as secondary connections.
Two read-only recovery slots preserve actual old hardware; only a genuine old
step-down item can become a supported rated core, and a former fuse record is
required to recover its cartridge. Repeated carried reads cannot repeat recovery;
new saves close the legacy settings gate and carried settings do not copy inventory.

The standalone Mains Step-down Unit uses the existing conservative regulated
controller with selectable 120/240 V output, 10/25/50/75/100 kW capacity, 100 A input
limit, 95% electronics efficiency and physical winding/thermal losses. It has native
ports, display/menu, config-tool hooks, crafting and save lifecycle. Its exact
source lookup is 123:10 (7882), not the explicitly group-124 wind ID 7946. The
first pole-transformer crafting tier now uses the standalone unit. Its recipe uses
the destination DC-DC converter for the still-unadapted source variable converter.
Existing target lookup conflicts are [recorded separately](forkpatch-port/registry-conflicts.md).

Validation: 99 focused utility/converter regressions and 210 selected native cases
passed; data generation passed for 619 identities. This is not a new full-regression
run. Native checks cover actual 120/240 V loads, losses, ports, rendering, display,
placed reload and recovery-only hardware. See [validation](forkpatch-port/twin-stepdown-validation.json).
General world conversion and interactive client validation remain pending.

### Six switchgear devices and actual client factory coverage

Source identities 8037–8042 now register single/three-pole contactors, two/four-pole
changeover relays, and single/three-pole solid-state relays. Each pole remains an
independent physical circuit, with signal or 120/240 V coil actuation, hysteresis,
break-before-make transfer, contact/coil heat, overload and thermal trip latches,
native control screens, live graphics and paid recipes. Re-enable/reset/reload
defers an NC make to the physical switching boundary so stale unsolved voltages
cannot create a fictitious fault. Saves preserve valid hot-case temperatures and
overload integrals beyond the trip threshold.

The broader catalog check caught a real integration gap: incoming descriptors had
render classes but lacked explicit native `ClientRenders` factory registrations.
Those factories now cover the service cabinets, pad transformer, industrial
controls, UPS, standby/ATS, off-grid and solar cabinets, step-down and switchgear.
Eighteen dedicated checks exercise actual block-entity publish packets and native
factory construction rather than directly constructing only a renderer.

The catalog sweep passes all 446 publication/render paths, 172 menus and 233 drawn
items. Its fixtures now clear waterlogged fluids and tall/wide ghosts, support both
halves of wide meters, isolate the fake player's hand, and use the disconnect's
normal sneak-to-open gesture. Historical 1.12 golden descriptor/slot snapshots
remain a separate reconciliation task. This is not an unfiltered full-native pass.

The full default root regression corpus passed 569 tests with three optional CAD
previews skipped (572 selected, 107 suites, no errors/failures), including the
default 200-case fluid corpus. All 238 selected `eln:forkpatch_*` native server
cases passed. Data generation covers 625 identities with no missing icons.
Gradle configuration and wrapper remain unchanged. Evidence is recorded in
[`switchgear-client-validation.json`](forkpatch-port/switchgear-client-validation.json).

### Large wind and watermill checkpoint

The Utility Wind Turbine 3.2kV and Large Watermill 3.2kV now have registered native
adapters, paid native recipes, source meshes, reproducible icons, animated rotors,
complete publication/factory coverage and bounds covering their tall geometry.
Wind preserves source lookup 7946. Water uses destination-only 16383 because UPS
saves already use source water's 7947; an explicit source lookup helper records the
relocation without altering existing UPS identities. This is not world conversion.

The conservative supply bounds generation to available environmental power,
3200 V and 50 A, including shorts and parallel unequal generators; external
backfeed opens the source. Real 5.12-ohm windings retain electrical loss. Sparse
environment checks run every two seconds: nine open-sky wind samples with weather,
height and cut-in/out behavior, or three flowing-water samples. Native chunk-now
lookups do not generate missing chunks. Still water produces no power.

A real finite-source/hybrid regression exposed a source low-voltage branch that
could remain at 4.85 kW for a restored 5 kW load after overload. Nominal-voltage
retries now run every 0.5 physical seconds while power-limited; algebraic trials
cannot advance the retry clock, and all power/current acceptance checks remain.
This preserves cached native LU factors and avoids repeating that extra nominal
search every overloaded tick.

All 20 focused renewable/solar/converter tests passed. All 244 selected native
server cases passed, including four watermill orientations, real loaded output,
still-water/removal behavior, wind obstruction, tall clearance, renderer factory
publication and geometry. Data generation covers 627 identities; all preceding
625 entries are unchanged and all 443 referenced native item textures resolve.
The final complete root regression run passed 577 tests with three optional
previews skipped (580 selected across 108 suites, no errors/failures), including
the default 200-case fluid corpus, against code commit `3b91d82f`. Evidence: [large renewable validation](forkpatch-port/large-renewable-validation.json).

The [feature backlog](forkpatch-port/remaining-work.md) distinguishes registered
adapters from remaining equipment, shared-code review, full save conversion and
interactive client/multiplayer validation. Gradle configuration and wrapper remain
unchanged.

### Programmable resistive load bank

Source lookup 8064 now has a native programmable load-bank adapter, preserving
passive voltage-squared power, the source resistance floor, real fan/case cooling,
thermal recovery, signal inhibition and consumed-energy accounting. Stable loads
retain native LU factors; repeated preparations cannot advance meters or heat.
The adapter has a native menu, renderer factory, original mesh and fan sound,
reproducible icon, source crafting bill and placed-node save/reload coverage.
Configuration tools transfer settings without copying heat or meter history.
See the [equipment guide](forkpatch-port/load-bank.md) and
[validation report](forkpatch-port/load-bank-validation.json). Appliance cord
integration, full source-world conversion and interactive audio/client checks
remain pending. This is another development checkpoint, not completion of the port.

### Passive variable frequency motor drive

The source motor drive at 8069 now uses the destination's power-conserving scalar
ServiceTap with real output impedance, physical V/Hz ramp, signal inhibition and
latched winding-temperature protection. Disable coasts down; hot faults open and
require a cool explicit reset. Saves retain heat/settings/faults but restart at
zero frequency. Accepted-step accounting retains the final powered interval,
and stable operation retains native LU factors. Native crafting, source geometry,
icon, menu, publication factory and placed-node reload are included. The model
is scalar, not an AC phasor solver or ideal current clamp. See the
[motor-drive guide](forkpatch-port/motor-drive.md) and
[validation report](forkpatch-port/motor-drive-validation.json).

### Eight-target native SCADA console

Source identity 8065 now has a native console with eight explicit world-local
addresses, a real 120 V / 20 W passive supply, physical-time polling, same-world
256-block range and loaded-chunk guards. Existing endpoint APIs handle service
disconnects/smart meters, pole switches/reclosers, switchgear and industrial
controls without resetting protection latches or paid hardware. Native binding
is transactional, saved addresses survive actual node reload and unsupported
console recursion is rejected. Inhibit and overvoltage also block commands,
consistent with the console's disconnected supply load. Source breaker-panel
and media-addon endpoints remain pending with those adapters. See the
[SCADA guide](forkpatch-port/scada.md) and
[validation report](forkpatch-port/scada-validation.json).
