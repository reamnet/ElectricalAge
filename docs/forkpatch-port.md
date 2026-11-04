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
LU physical solve, diode re-solves or cached interconnection resistances. Full
ForkPatch floating-gauge/converter integration is a subsequent adapter step.

The controller kernels do not yet register their corresponding cabinets, menus
or vehicles. Those production adapters remain part of the full scope above.

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
