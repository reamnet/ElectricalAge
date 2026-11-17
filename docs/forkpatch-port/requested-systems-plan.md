> **2026-11-17 wrap-up:** See [current handoff](wrap-up-handoff.md) for implemented panel/carrying, all seven alarms, road vehicles/charger/lead, test evidence and remaining difficulty. Historical “not registered” notes below are superseded by that checkpoint; pending live GUI/gameplay checks remain explicit.

# Requested panel, vehicles, alarms and portable carrying

Requested after checkpoint `fbbc6552473cfc72195379345464b8db5b9c2424`.
Source remains read-only ForkPatch `b7444a7b8a0c5e072b1fd35eed2b74a282c3348c`.
This document defines the requested scope and acceptance checks. Entries below
are planned work until backed by a validated implementation report.

## Twelve-circuit breaker panel

Source entry points: `utility/BreakerPanel.kt`, `BreakerPanelClient.kt`,
`WirelessPowerChannels.kt`; source transparent-node lookup7988. Preserve all
existing native identities when checking that lookup.

- Twelve separately paid breaker items and branches; alternating120V buses and
  selectable240V branches; actual feeder/branch terminals and orientation.
- Source breaker thermal trip accumulation, saved latches, explicit resets and
  absent-breaker behavior. Clearing a fault alone must not reclose a trip.
- Generalized wireless channel ownership compatible with existing lamp supplies
  and consumers, bounded range, controls/dusk/aggregators and SCADA endpoints.
- Real upstream power exactly once per consumer, including after repeated demand
  reports, channel changes, reconnects and unloads. No stale or foreign-world source.
- Source menus, branch selection, recipes, models, publication and save settings.

Acceptance: powered120/240V circuits, independent branches/returns, overload and
reset, absent stock, all12 selections, malformed controls, reload and native
consumer integration. Shared service split and native cached solver are reused.

## Road vehicles and plug-in charging

Source entry points: `vehicle/RoadVehicles.kt`, `VehicleControls.kt`,
`VehicleClient.kt`, `VehicleCharger.kt`.

- Electric Golf Cart: four seats, one20kWh traction pack, source driving/step and
  dangerous-corner policy. Arc Runner Coupe: two seats and six traction packs.
- Registered placement items/entities, server-authoritative controls, passengers,
  collision/stepping, cargo, energy/condition, saves and exactly-once drops.
- Visible source models, moving wheels, clear coupe windows, controls and menus.
- Separate240V100A Vehicle Charger (source transparent lookup8079) and **100 A
  Vehicle Charging Lead**, allowing the player to select a station and plug into
  a parked vehicle within six blocks. Existing removable-pack charging is retained.
- Driving/charging mutual exclusion, correct range/world/owner checks, disconnect
  and restart behavior, and measured upstream energy with source charge efficiency.

Acceptance: actual placement/driving/passengers, invalid or remote controls,
slopes/collision/cornering, one/six-pack accounting, full/empty/mixed pack bays,
station/lead interaction, sag/overvoltage/unload, cargo and fresh-server saves.

## Fire alarm system

Source entry points: `transparentnode/firealarm/AlarmKind.kt`, `AlarmNetwork.kt`,
`AlarmElement.kt`, `FireEnvironment.kt`, `AlarmClient.kt`. Source transparent-node
lookups8053–8059 cover panel, pull station, horn/strobe, bell, strobe, smoke and
heat detectors.

- Real powered network association and bounded environmental sensing.
- Source panel latch, silence/reset/test, backup-energy accounting and individual
  field-device loads without duplicate payment between panels/own power.
- Correct placement, menus, recipes, models, synchronized flash/sound and saves.

Acceptance: actual smoke/fire/heat and occlusion/range cases, independent networks,
power outage/backup exhaustion, reset while a source remains active, silent alarm
behavior, multiple panels, removed/replaced owners and unloaded members.

## Portable carrying

Source entry points: `transparentnode/cord/ApplianceCord.kt`, `CordInteraction.kt`
and each intended consumer's `cordPortable` policy. Integrate the native ItemNbt
and placement/destruction paths rather than copying legacy stack-tag APIs.

- Carry is an ownership transfer, preserving contents, settings, component
  condition/heat, energy and paid cord token/anchor exactly once.
- Release downstream claims before snapshotting a portable socket source.
- Suppress duplicate ordinary drops/refunds during transfer; old owners must not
  detach restored owners. Respect physical reach, rating, dimension and live claims.
- Support existing intended portable families; do not register placeholder
  appliances merely to expose a carry action.

Acceptance: actual survival pick/carry/place, repeated callbacks, dropped or copied
carriers, occupied outlets, unload/reload, range and dimension changes, exact
inventory/energy/condition and single paid-cord refunds.

## Validation and publication

Baseline:701 root cases (698pass, three unchanged optional skips),15 CAD cases,
1416 native cases (1414pass, only two historical golden failures),950 passing
ForkPatch cases. All prior714 golden assertion differences remain recorded.
Do not weaken expectations, assertions, tolerances, skips or test selection.

Each completed slice records explicit source mappings, provenance and recipe
choices, meaningful independent native tests, data-generation results, saved-owner
checks and any remaining client limitations. Push completed slices to the existing
draft PR as reamnet. Keep the PR title, base, draft state and project Gradle pins.

Do not compile changed main classes while game/test JVMs use the same outputs.
Run actual GUI/game checks for new interactions after automated checks, then build
and certify a clean development artifact from the final code commit.
