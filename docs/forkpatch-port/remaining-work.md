# Feature backlog for the 3.0.77 → NeoForge 26.3 port

Source baseline: `b7444a7b8a0c5e072b1fd35eed2b74a282c3348c`. This is a feature
backlog, not a count of missing files or a statement that every inherited class
has been reviewed. The [source inventory](source-inventory.json) retains that
file-by-file review surface; the [import audit](import-audit.json) checks provenance.

## Registered adapters with passing production checks

- Norton solar panels and large tracking arrays; MPPT and hybrid floor/wall controls.
- Pitched solar roofs for all four panel variants, saved orientation, uphill reservations/return terminal and replicated cached collision shapes.
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
- Programmable Load Bank with passive sag response, case/fan cooling and native controls.
- Variable Frequency Motor Drive with passive V/Hz ramp, output impedance and latched thermal protection.
- Grid Control Console/SCADA with eight world-local targets, bounded loaded-node lookup and native device controls.
- Standalone Pole Surge Arrester and paid overhead HV pole kits with finite cartridge state and native lightning protection.
- Programmable EV Pack Charger, removable 20 kWh traction pack, Electric Welding Station and welded frames; native inventory, accepted-energy and save checks.
- Electric Forklift and Warehouse Equipment Pallet with server controls, actual pack drain/lifting costs, guarded inventories, cargo saves and refunds.

Each family still participates in the cross-cutting validation/migration work below.

The solar-roof checkpoint passes **634 full root regressions**, with three optional
previews skipped (637 selected), all **15 CAD tests**, and all **335 selected native
port cases**, including 18 new roof cases. The default 200-case fluid corpus is
included. All 637 identities and 935 English entries remain unchanged, and all
453 referenced native item textures resolve. See [roof evidence](solar-roof-validation.json)
and the [roof guide](solar-roofs.md). The earlier [workshop/transport evidence](workshop-transport-validation.json),
[workshop guide](workshop.md) and [transport guide](industrial-transport.md) remain available.

The previous workshop/transport unfiltered run passes **781 of 783 GameTests**. The historical descriptor
and slot comparisons remain failing; [exact differences](native-suite-review.md)
are retained without changing expectations. Restoring shared test-player facing
after oriented renewable placement fixes two order-dependent battery/furnace layouts.
The charge-drive-recharge case passes in both the unfiltered and filtered-port runs.

Earlier checkpoints remain documented in [large renewable validation](large-renewable-validation.json),
[industrial controls](industrial-work-block-validation.json), and the
[charging/welding checkpoint](workshop-validation.json).

## Equipment and behavior still requiring adapters or parity review

| Area | Remaining scope |
| --- | --- |
| Other power electronics | Remaining source variable converter/control behavior; review shared transformer, capacitor and regulated-supply changes against the destination solver. |
| Utility attachments | Legacy non-distribution pole streetlight ports, pole height attachments, adaptive primary/service rack alignment and associated clearance/lead routing. Hidden standalone utility light variants need their own recovery/parity review. |
| Utility manufacturing | Wire drawing/processing machinery, spool/jacket manufacturing, inventory trimming and remaining wire/cable tools. |
| Industrial devices | Appliance cord/outlet integration and interactive client/audio parity for the registered industrial families. |
| Industrial transport | Interactive client controls, audio and multiplayer parity of the registered forklift/pallet adapters. Road vehicles remain a separate adapter group. |
| Vehicles | Four-seat golf cart, two-seat electric car, stepping/collision, turn-overload fire/explosion behavior, passengers, storage, six-cell traction pack and in-car charging. Preserve the electric car's clear windows. |
| Appliance power | Plugs/cords, wall/industrial outlets, power strip, rating and support/wiring rules, machine cord adapters and compact plug-in UPS. |
| Kitchen/laundry | Source kitchen and laundry/matter-machine families, food/process inventories, recipes, temperature/heat behavior, appliance controls and graphics. |
| Climate and fans | Heating/cooling/humidity controls, fans, evaporative cooling and water handling, building/room thermal changes and integration with the destination fluid systems. |
| Pumps and growing | Industrial/world-intake pumps, fluid-port integration, growing-bed cycle and associated inventory/render/UI behavior. |
| Lighting/decor | Remaining street/flood/work lights, festive/fairy lights/tree/candle devices, fire alarms and their networks, optics, damage, controls and audio. |
| Other mechanics/processes | Source shaft/generator/turbine, fusion reactor and other machine/process changes not already covered by an adapter checkpoint; reconcile with existing native mechanical/fluid implementations. |
| Tools and integration | Remaining probes, configuration interactions, wireless/control channels, diagnostics/metrics, guides and optional-mod bridges. SCADA endpoint integration for the still-unadapted source 12-circuit breaker panel and media addon remains pending. A source file already present in 26.3 still needs conflict review. |
| Media addon | EAMedia is a separate addon: jukebox/external speakers and Plex/web-interface behavior need a separate platform port and integration validation. |

## Cross-cutting completion work

- Implement full **1.21.1 source-world conversion**, including blocks/entities/items,
  NBT and registry conflicts. Native save round trips and a source-ID lookup helper
  are not a world converter. Preserve existing 26.3 save identities.
- Complete recipes/manufacturing, translations, guide material, tags/loot, models,
  icons, sounds, packets and menus for every remaining adapter.
- Reconcile historical 1.12 descriptor/slot golden snapshots with source additions
  and intentional native changes. Do not waive differences by simply regenerating
  expected snapshots from the implementation.
- The unfiltered workshop/transport run passes 781 of 783 cases; descriptor and slot
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
