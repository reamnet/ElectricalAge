# Feature backlog for the 3.0.77 → NeoForge 26.3 port

Source baseline: `b7444a7b8a0c5e072b1fd35eed2b74a282c3348c`. This is a feature
backlog, not a count of missing files or a statement that every inherited class
has been reviewed. The [source inventory](source-inventory.json) retains that
file-by-file review surface; the [import audit](import-audit.json) checks provenance.

## Current continuation

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
- Passive pad transformer, primary bayonet fuses and separate core protection.
- Service weatherheads, meter bases and wide underground socket, meter registers,
  manual disconnect, buried entries and handholes with thin wall collision.
- Single/multicore placed utility wiring, overhead paid spans and cached catenaries.
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
- Native surface outlets/strips and nine cord ratings with paid fixed-machine links, source recipes, native controls/geometry and voxel inlet isolation. Extended cooler/transmitter/turret inputs, miner/transporter auxiliary terminals and recessed wall ownership/cable geometry are covered by subsequent continuations; portable carry remains pending.
- Fabricator 4480 and five source basic components with real 4 kJ attempts, paid wafer/machine recipes, deterministic yields, native inventories/controls and optional JEI examples.
- Wire Roller 4608, Insulator 4609 and Combiner 4610 with real paid buffers, wheels/bundles, hot stock, source recipes, native automation and one powered multicore production chain.
- Electric Forklift and Warehouse Equipment Pallet with server controls, actual pack drain/lifting costs, guarded inventories, cargo saves and refunds.

Each family still participates in the cross-cutting validation/migration work below.

The current surface-cord checkpoint passes **670 full root tests**, with three
optional previews skipped (673 selected), all **15 CAD tests**, and all
**657 port cases inside 1123 unfiltered GameTests**. The default 200-case
fluid corpus is included. The full native run passes 1121 cases; only the historical
descriptor and slot goldens fail, retaining 467 individual assertions for review.
No expected references, skip lists or tolerances changed. See the
[complete current evidence and artifact](session7-validation.json).

All 658 preceding identities and 966 English entries remain unchanged. The current
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
| Utility attachments | Legacy non-distribution pole streetlight ports, pole height attachments, adaptive primary/service rack alignment and associated clearance/lead routing. Hidden standalone utility light variants need their own recovery/parity review. |
| Utility manufacturing | Remaining wire/cable tools and source parity/integration review. Roller4608, Insulator 4609, Combiner 4610 and their wheels/bundles now pass paid native production checks; see [manufacturing](wire-manufacturing.md). |
| Industrial devices | Appliance cord/outlet integration and interactive client/audio parity for the registered industrial families. |
| Industrial transport | Interactive client controls, audio and multiplayer parity of the registered forklift/pallet adapters. Road vehicles remain a separate adapter group. |
| Vehicles | Four-seat golf cart, two-seat electric car, stepping/collision, turn-overload fire/explosion behavior, passengers, storage, six-cell traction pack and in-car charging. Preserve the electric car's clear windows. |
| Appliance power | Portable consumers/carry, newer rated lamp-supply consumers, generalized panel channels. Compact plug-in UPS now has its own native adapter; portable carry remains separate. Recessed wall placement/restoration and centered legacy/voxel cable terminals now have their own adapter; interactive multi-client and additional resource-pack parity remain to check. Surface outlets/strips and fixed-machine families now use paid native links; see [adapter](cord-outlets.md) and [source review](appliance-cord-review.md). |
| Kitchen/laundry | Source kitchen and laundry/matter-machine families, food/process inventories, recipes, temperature/heat behavior, appliance controls and graphics. |
| Climate and fans | Heating/cooling/humidity controls, fans, evaporative cooling and water handling, building/room thermal changes and integration with the destination fluid systems. |
| Pumps and growing | Industrial/world-intake pumps, fluid-port integration, growing-bed cycle and associated inventory/render/UI behavior. |
| Lighting/decor | Remaining street/flood/work lights, festive/fairy lights/tree/candle devices, fire alarms and their networks, optics, damage, controls and audio. |
| Chip fabrication | Interactive graphics/JEI and broader basic-component integration remain pending. Fabricator 4480 and components 8064–8068 now have paid native processing/bootstrap checks; see [fabrication](chip-fabrication.md). |
| Other mechanics/processes | Source shaft/generator/turbine, fusion reactor and other machine/process changes not already covered by an adapter checkpoint; reconcile with existing native mechanical/fluid implementations. |
| Tools and integration | Remaining probes, configuration interactions, wireless/control channels, diagnostics/metrics, guides and optional-mod bridges. SCADA endpoint integration for the still-unadapted source 12-circuit breaker panel and media addon remains pending. A source file already present in 26.3 still needs conflict review. |
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
- The latest unfiltered run passes 1121 of 1123 cases; descriptor and slot
  goldens still fail. Review the [exact differences](native-suite-review.md), then
  rerun after explicitly reconciling the historical expectations. The wind/watermill
  fixture now restores shared test-player facing, fixing two order-dependent layouts.
- Perform interactive client/GPU/audio and multiplayer checks, including geometry,
  animations, transparent vehicle windows, pitched roof selection/collision, screen interactions and resource reloads.
  Review tall legacy pole/span culling bounds and source mesh display/status parts
  as part of that pass; emitting geometry alone does not verify those behaviors.
- Compare representative loaded-world server performance against both baselines,
  especially solar arrays, coupled converter banks, wiring and mechanical/fluid networks.
- Build and review release artifacts only after the intended release scope passes;
  the development branch and draft PR are not a finished full-fork release.
