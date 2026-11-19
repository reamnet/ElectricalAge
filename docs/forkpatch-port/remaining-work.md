> **Cable source-value audit:** All170 cable identities and the size/material/physics tables match the pinned1.21.1 source;195 native cable tests and26 unit tests pass. Room-temperature coupling, failed-cable player shock and post-fault cooling policy still need parity work. See [cable-source-parity-audit.md](cable-source-parity-audit.md).

> **Final lighting checkpoint:** Nine remaining standalone fixtures, supplemental120/240V emergency lamps and source lifetime/configuration are implemented. Full native1552selected/1550pass/2 historical golden failures/0skips; all1086 ForkPatch cases pass. All nine live menus and real supply demand were recorded. See [final-lighting.md](final-lighting.md) and `final-lighting-validation.json`; historical registration/configuration backlog notes below are superseded. Hardware audio/GPU parity, full source-world migration and general tools remain broader acceptance work.

> Final expanded-systems verification: 1536 native selected/1534 passed/2 historical golden failures/zero skips; all1070 ForkPatch cases and all66 added cases pass. Root698pass/3 unchanged optional skips; CAD15pass. Datagen repeats with0 writes. Golden832 is recorded without expectation changes. Live GUI/audio/multiplayer acceptance remains pending. See [current feature handoff](appliances-substations-bulbs-fans.md).

> **Expanded systems continuation:** Kitchen/laundry/fusion, substations8016–8019 plus7875 prerequisite, all18 source bulbs, and four standalone fans now have native implementations. See [implementation and acceptance details](appliances-substations-bulbs-fans.md) and `expanded-systems-validation.json` for current evidence; historical not-implemented notes below predate this slice.

> Final wrap verification:1470 native selected,1468pass,2 historical golden failures; all1004 ForkPatch cases pass. Root698pass/3 existing skips, CAD15pass, datagen repeat0 writes. See `requested-systems-validation.json`.

> **2026-11-17 wrap-up:** See [current handoff](wrap-up-handoff.md) for implemented panel/carrying, all seven alarms, road vehicles/charger/lead, test evidence and remaining difficulty. Historical “not registered” notes below are superseded by that checkpoint; pending live GUI/gameplay checks remain explicit.

# Feature backlog for the 3.0.77 → NeoForge 26.3 port

Source baseline: `b7444a7b8a0c5e072b1fd35eed2b74a282c3348c`. This is a feature
backlog, not a count of missing files or a statement that every inherited class
has been reviewed. The [source inventory](source-inventory.json) retains that
file-by-file historical baseline; the [import audit](import-audit.json) checks provenance.

The [whole-fork completion plan](port-completion-plan.md) is the current detailed
work breakdown: 37 streams with dependencies, native foundations, concrete
implementation tasks and completion checks. Its refreshed [audit](completion-audit.json)
accounts for all 1,277 source main files (1,264 build-active, 13 excluded),
2,460 main resources, 262 test files and the other 3,133 tracked generated-data/
tooling/reference/build files (including 2,496 generated runtime data files).
All 7,132 tracked source-repository files are accounted for. The [lighting matrix](lighting-port-matrix.md)
lists all 108 fixtures and 18 source bulb registrations. These indexes track
review obligations; matching names, copied assets and file counts do not certify
behavioral completion.

## Current continuation

The [twelve-circuit panel](breaker-panel.md), supported-family [portable carrying](portable-carry.md), all seven [fire alarms](fire-alarms.md), and [road vehicles/station/lead](road-vehicles.md) are implemented. Road native tests pass28/28; the earlier all-ForkPatch fire checkpoint passes976/976. Current root698pass/3 unchanged skips and CAD15pass. Live GUI/gameplay checks remain pending. See [current handoff](wrap-up-handoff.md) for final whole-run evidence and remaining acceptance gates.


The [decorative lighting port](decorative-lighting.md) now registers all 108
fixtures, including the five fans, motion floodlight and adjustable warehouse
light. It restores per-head aiming, wide beams, original palettes/item poses,
water-preserving light leases and all 108 exact source recipes. Source String
Lights is a working prerequisite. Fixture cord input and carrying for source table-lamp styles are integrated;
partial-support mounting and additional bulb technology/configuration remain
unfinished. Source 120/240 V LEDs use native lookups 302/303, preserving
old 50/200 V bulbs. See the [review since bb962e1](review-since-bb962e1.md) and
[lighting evidence](decorative-lighting-validation.json) for actual validation
and its limits. Registration counts are not a claim of universal source parity.

The [steel transmission poles and risers](power-poles.md) add source lookups
8014, 8015, 8020 and 8021, independent primary/service circuits, paid spans,
primary alignment, native base cable entries, tall rendering and removable
streetlights. All 63 new pole cases and all 821 ForkPatch cases pass in the
1,287-case native run. The [validation](power-poles-validation.json) records the
unchanged historical failures and the [nine added golden differences](power-poles-golden-delta.json).
The source substation transformers/disconnects remain separate work.

The [GUI matching first batch](gui-parity.md) restores source coordinates for
13 classic layout variants. Larger device screens and the remaining classic
panels still need visual review; universal GUI parity is not complete.

The [rated lamp-supply adapters](rated-lamp-supplies.md) add explicit 120/240 V
cord inputs to the existing native three-channel supply system, preserving the
generic native supply at lookup 4160. Source 120 V lookup 4160 maps explicitly
to free native lookup 4162; 240 V retains 4161. Their validation is recorded
separately in [rated supply evidence](rated-lamp-supplies-validation.json).
Its final run passes all 680 root regressions (three existing optional previews
skipped), 15 CAD tests and all 758 ForkPatch cases inside 1,224 unfiltered native
cases. Only the two historical goldens fail: their 469 previous failed
assertions remain unchanged, with four new identity/container differences.
Software-OpenGL client placement, controls, paid cords, resource reload and
fresh-JVM ownership restoration are verified; hardware/audio/multiplayer remain
separate checks.

The [compact UPS adapter](compact-ups.md) adds source identity 8076, four
independent cell slots, four durable native socket reservations, 120/240 V
selection, conservative 3 kW power control and compact native controls.
The [current evidence](compact-ups-validation.json) records its production,
runtime and artifact checks. The preceding [recessed-wall adapter](recessed-outlets.md),
[extended consumers](extended-consumers.md) and [miner/transporter inputs](ghost-consumers.md)
remain implemented; older counts below are historical checkpoint evidence.

## Registered adapters with passing production checks

- Norton solar panels and large tracking arrays; MPPT and hybrid floor/wall controls.
- Pitched solar roofs for all four panel variants, saved orientation, uphill reservations/return terminal and replicated cached collision shapes.
- Fixed flat solar-array half-height reservations and resident-only scheduled repair of older native cube states.
- Front/pitch-correct daylight sampling, modern opaque shadows, resident-only bounded voxel rays and conservative heightmap skipping.
- Utility Wind Turbine 3.2kV and Large Watermill 3.2kV with bounded renewable
  supplies, sparse environmental sampling and native publication/animation.
- Off-grid inverter/charger, charging/protection/combiner equipment and floor/wall
  16/32/48/64 kWh LiFePO4 banks.
- ATS floor/wall controls, standby generator tiers and synchronized reset.
- Floor/wall programmable UPS, protective relay and automatic priority load shedding.
- Four Cell Plug-in UPS 8076 with independently active cells, four durable cord sockets, native cable terminals, 120/240 V controls and conservative 3 kW power.
- Passive pad transformer, primary bayonet fuses and separate core protection.
- Service weatherheads, meter bases and wide underground socket, meter registers,
  manual disconnect, buried entries and handholes with thin wall collision.
- Single/multicore placed utility wiring, overhead paid spans and cached catenaries.
- 69 kV / 230 kV steel transmission poles and cable risers, automatic primary
  alignment with independent secondary suspension, real base cable routing,
  tall/span visibility and paid removable 70 W streetlights.
- All 30 distribution variants (18 current, 12 hidden legacy), physical transformer
  hardware, independent fuses/phases, regulator/recloser/capacitor/switch functions.
- Removable distribution streetlights, selected 120/240 V lighting, twin LED pole
  recovery and standalone Mains Step-down Unit.
- Six independent-pole contactor/changeover/solid-state switchgear devices.
- Fixed winding DC/DC converter130 and four grounded one-way converter variants131–134, retaining source V1 policy and V2 physical gain/current limits.
- Variable DC/DC converter129 with paid fractional windings, versioned controls, conservative reversible transfer, hot-wire saves and native menus/rendering.
- Signal-controlled variable inductor6179 with the source E6 range, one-core menu and retained native LU/voxel behavior.
- Programmable Load Bank with passive sag response, case/fan cooling and native controls.
- Variable Frequency Motor Drive with passive V/Hz ramp, output impedance and latched thermal protection.
- Grid Control Console/SCADA with eight world-local targets, bounded loaded-node lookup and native device controls.
- Standalone Pole Surge Arrester and paid overhead HV pole kits with finite cartridge state and native lightning protection.
- Programmable EV Pack Charger, removable 20 kWh traction pack, Electric Welding Station and welded frames; native inventory, accepted-energy and save checks.
- Wire Snips 7873 with native main/off-hand menus, paid fractional cuts, preserved hot/damaged/multicore data and exact native returns.
- Native surface outlets/strips and nine cord ratings with paid fixed-machine links, source recipes, native controls/geometry and voxel inlet isolation. Extended cooler/transmitter/turret inputs, miner/transporter auxiliary terminals and recessed wall ownership/cable geometry are covered by subsequent continuations; carrying for supported lamps/UPS and portable kitchen/fan families is implemented; broader consumer families remain.
- Explicit 120/240 V lamp supplies with native three-channel controls, optional cable range extensions, paid cord inputs and passive wireless-outlet power; the established unrated Lamp Supply retains its identity and normal channel behavior. Shared malformed-channel decoding and phantom channel registration are corrected.
- Fabricator 4480 and five source basic components with real 4 kJ attempts, paid wafer/machine recipes, deterministic yields, native inventories/controls and optional JEI examples.
- Wire Roller 4608, Insulator 4609 and Combiner 4610 with real paid buffers, wheels/bundles, hot stock, source recipes, native automation and one powered multicore production chain.
- Electric Forklift and Warehouse Equipment Pallet with server controls, actual pack drain/lifting costs, guarded inventories, cargo saves and refunds.

Each family still participates in the cross-cutting validation/migration work below.

The historical surface-cord checkpoint passed **670 full root tests**, with three
optional previews skipped (673 selected), all **15 CAD tests**, and all
**657 port cases inside 1123 unfiltered GameTests**. The default 200-case
fluid corpus is included. The full native run passes 1121 cases; only the historical
descriptor and slot goldens fail, retaining 467 individual assertions for review.
No expected references, skip lists or tolerances changed. See the
[historical surface-cord evidence and artifact](session7-validation.json).

All 658 preceding identities and 966 English entries remain unchanged. That checkpoint
catalog has 677 identities, 995 English entries and 493 resolved native item
textures. See [surface cords/outlets](cord-outlets.md) and [validation](cord-outlets-validation.json).
Earlier adapter/performance evidence remains in its individual reports and does
not establish whole-mod comparisons.

The [exact historical differences](native-suite-review.md) remain visible.
Restoring shared test-player facing after oriented renewable placement continues
to protect unrelated battery/furnace layouts. All earlier charge/drive/recharge
integration cases also pass in the final expanded run.

The flat-array follow-up passes the same 648 root regressions and 15 CAD tests,
plus all 350 native port cases. Its 638 identities and 936 English entries are
unchanged; see [evidence](flat-solar-validation.json) and [guide](flat-solar.md).

Earlier checkpoints remain documented in [large renewable validation](large-renewable-validation.json),
[industrial controls](industrial-work-block-validation.json), and the
[charging/welding checkpoint](workshop-validation.json).

## Equipment and behavior still requiring adapters or parity review

| Area | Remaining scope |
| --- | --- |
| Ghost collision parity | Remaining owner-specific ghost picking/collision and source import mappings; fixed flat solar reservations now have their own state and scheduled native-save upgrade. |
| Shared mounting | Source partial-support insets, cable rises and matching selection/render transforms beyond the registered stair roofs; preserve native voxel geometry and resident-only queries. See the [mounting review](mounting-review.md). |
| Other power electronics | Development-only floating isolation135 and broader inherited transformer/capacitor/regulated-supply changes; fixed130 and grounded one-way131–134 are registered; broader inherited changes still need review against the destination solver. See the [identity/control/winding review](power-electronics-review.md). |
| Utility attachments | Legacy non-distribution pole streetlight ports outside the new steel family, pole height attachments, adaptive primary/service rack alignment and associated clearance/lead routing. Hidden standalone utility light variants need their own recovery/parity review. |
| Transmission substations | Substation transformers/disconnects8016–8019 and prerequisite7875 now have native adapters. Complete upstream/downstream transmission, protection coordination and live campaigns remain. The four new steel poles/risers have their own native adapter. |
| Utility manufacturing | Remaining wire/cable tools and source parity/integration review. Roller4608, Insulator 4609, Combiner 4610 and their wheels/bundles now pass paid native production checks; see [manufacturing](wire-manufacturing.md). |
| Industrial devices | Appliance cord/outlet integration and interactive client/audio parity for the registered industrial families. |
| Industrial transport | Interactive client controls, audio and multiplayer parity of the registered forklift/pallet adapters. Road vehicles remain a separate adapter group. |
| Vehicles | Four-seat golf cart, two-seat electric car, stepping/collision, turn-overload fire/explosion behavior, passengers, storage, six-cell traction pack and in-car charging. Preserve the electric car's clear windows. |
| Appliance power | Portable consumers/carry and generalized panel channels. Explicit 120/240 V lamp supplies now have [native adapters](rated-lamp-supplies.md); remaining rated lighting/consumer families still need their own review. Compact plug-in UPS now has its own native adapter; supported table-lamp/compactUPS carrying is implemented; additional families remain. Recessed wall placement/restoration and centered legacy/voxel cable terminals now have their own adapter; interactive multi-client and additional resource-pack parity remain to check. Surface outlets/strips and fixed-machine families now use paid native links; see [adapter](cord-outlets.md) and [source review](appliance-cord-review.md). |
| Kitchen/laundry/fusion | All14 requested devices are implemented; see [adapter](appliances-substations-bulbs-fans.md). Room climate/spoilage compatibility, live client/automation/restart and broader recipe-value campaigns remain. |
| Climate and fans | All nine standalone/ceiling fans are registered. Room heating/cooling/humidity, evaporative cooling/water handling and building thermal integration remain. |
| Pumps and growing | Industrial/world-intake pumps, fluid-port integration, growing-bed cycle and associated inventory/render/UI behavior. |
| Lighting/decor | All 108 decorative fixtures have adapters, with aiming, motion, fan, suspension and water-preserving leases. All18 source-rated bulb variants are registered; broader technology configuration/parity, carrying for additional families, partial-support mounting and full client parity remain. See the [complete fixture/bulb matrix](lighting-port-matrix.md) and [implementation gates](port-completion-plan.md#lighting). Standalone flood/work/festive equipment and fire alarm networks are separate streams. |
| Chip fabrication | Interactive graphics/JEI and broader basic-component integration remain pending. Fabricator 4480 and components 8064–8068 now have paid native processing/bootstrap checks; see [fabrication](chip-fabrication.md). |
| Other mechanics/processes | Source shaft/generator/turbine, fusion reactor and other machine/process changes not already covered by an adapter checkpoint; reconcile with existing native mechanical/fluid implementations. |
| Tools and integration | Remaining probes, configuration interactions, wireless/control channels, diagnostics/metrics, guides and optional-mod bridges. The source12-circuit panel now has a native SCADA endpoint; media-addon integration remains pending. A source file already present in 26.3 still needs conflict review. |
| Media addon | EAMedia is a separate addon: jukebox/external speakers and Plex/web-interface behavior need a separate platform port and integration validation. |

## Cross-cutting completion work

- Implement full **1.21.1 source-world conversion**, including blocks/entities/items,
  NBT and registry conflicts. Native save round trips and a source-ID lookup helper
  are not a world converter. Preserve existing 26.3 save identities. Review the [source/native ghost-state differences](ghost-migration-review.md) before converting reservations.
- Complete recipes/manufacturing, translations, guide material, tags/loot, models,
  icons, sounds, packets and menus for every remaining adapter.
- Reconcile historical 1.12 descriptor/slot golden snapshots with source additions
  and intentional native changes. Do not waive differences by simply regenerating
  expected snapshots from the implementation.
- The latest completed code checkpoint passes **1,363 of 1,365** native cases;
  all **899 ForkPatch cases** pass. Only descriptor and slot goldens fail.
  The [lighting delta](decorative-lighting-golden-delta.json) records 641 failed
  assertions, preserving all 631 prior non-aggregate assertions and explicitly
  recording the new bulb-acceptance/aggregate differences. Review the
  [historical differences](native-suite-review.md) before reconciling expectations;
  no snapshot, existing assertion, skip or tolerance is relaxed. See
  [current code evidence](decorative-lighting-validation.json).
- Perform interactive client/GPU/audio and multiplayer checks, including geometry,
  animations, transparent vehicle windows, pitched roof selection/collision, screen interactions and resource reloads.
  Review tall legacy pole/span culling bounds and source mesh display/status parts
  as part of that pass; emitting geometry alone does not verify those behaviors.
- Compare representative loaded-world server performance against both baselines,
  especially solar arrays, coupled converter banks, wiring and mechanical/fluid networks.
- Build and review release artifacts only after the intended release scope passes;
  the development branch and draft PR are not a finished full-fork release.

## Additional source-wide review obligations

The earlier family table is not the whole completion checklist. The detailed plan
also covers [MQTT/Modbus and optional bridges](port-completion-plan.md#integrations),
[railroad/replicator entities](port-completion-plan.md#entities),
[world generation/progression/guides](port-completion-plan.md#world-content),
[bootstrap/configuration/mixins](port-completion-plan.md#build-compat), and all
[inherited classic behavior](port-completion-plan.md#classic),
[shared support code](port-completion-plan.md#review-remainder) and
[source test/oracle mappings](port-completion-plan.md#verification).
The12-circuit panel and cart/coupe entities now have native adapters and tests; live-client acceptance remains due. Full source-world
conversion, all-screen reference comparison and representative loaded-world
performance remain explicit completion gates.
