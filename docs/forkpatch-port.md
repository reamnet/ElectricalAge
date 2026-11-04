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

These controller kernels do not yet register their corresponding cabinets, menus
or vehicles. The solar process needs to replace the destination panel adapter
before the changed I-V model becomes available on registered solar blocks.
The incoming production Java/Kotlin sources compile against the pinned 26.3
toolchain. Focused MNA, controller and solar regressions are running at this
checkpoint; passing totals will be recorded after they finish.

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
