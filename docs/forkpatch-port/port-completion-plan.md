# ForkPatch 3.0.77 → NeoForge 26.3 completion plan

Source pin: `b7444a7b8a0c5e072b1fd35eed2b74a282c3348c`. Target reviewed: `c59e547138e53409f7a1bd282175ec25e16bb74d`. This is a work breakdown for the whole fork, including shared inherited behavior, assets, optional systems and verification. It does not certify that every implementation has already received a line-by-line semantic review.

Every source main file is assigned to an open review stream. Same-name classes, byte-identical files, model directories and import-ledger links are useful evidence locations; none alone proves a feature is ported. Source-excluded scaffolding needs an explicit retain/exclude decision, not automatic registration. Conditional integration status preserves source feature gates and does not authorize silently dropping active source features.

## What a device port needs

An existing model saves mesh-creation work. A working 26.3 device also needs stable registration, a native electrical/mechanical/fluid connection, real material/energy costs, coherent placement/collision/rendering, an authoritative menu/packet path, preserved save/item state and paid recipes. These are separate checks; copying a model or compiling a class does not finish them.

Simple fixed fixtures can reuse the new shared lighting adapter. Adjustable heads, fan motors, motion sensors, underwater leases and movable suspension add behavior that the fixed adapter does not provide. Likewise, existing native machines should keep their validated simulation and receive the actual source behavior changes, rather than being rewritten solely to match source filenames.

For a first implementation slice, resolve the later lighting ID ranges and static variant data, then add independent aim/motion/water/fan/suspension adapters. In parallel, shared mounting, panel channels, portable ownership and room climate/fluid services unlock larger appliance groups. The workstreams below identify the dependencies and tests for each.

## Audit coverage

- **1277 main Java/Kotlin files**: 1264 selected by the pinned source include list and 13 excluded.
- **2460 main resource files**, **262 source test files** and **781 distinct literal source names** have searchable review indexes.
- **All 7132 tracked source-repository files are accounted for**, including 3133 generated-data/tooling/artwork/reference/build files outside those directories. In particular, 2496 generated runtime data files require generator/native-output review. This is coverage, not a count of things to copy.
- **37 workstreams** below define existing foundations, remaining implementation, dependencies and acceptance checks.
- File/name observations are **not missing-feature counts**. Renamed/split adapters can have no same-name candidate; byte-identical helpers still need runtime integration checks. Full migration is new cross-cutting work and has no assigned incoming source file.
- [Source review rows](completion-source-audit.tsv): exact source hashes, same-identity candidates, explicit adapted targets, active/excluded source status and assigned stream.
- [Resource review rows](completion-resource-audit.tsv): exact-path byte observations and recorded renamed/adapted targets. Unreferenced assets are not automatically required registrations.
- [Test/oracle review rows](completion-test-audit.tsv): the source test include-pattern observation and any exact-path target. Different test names require semantic/oracle mapping.
- [Generated data and support review rows](completion-support-audit.tsv): every other tracked source path, including generated runtime content, workflows, scripts, artwork, license/documentation and build contracts.
- [Literal-name review rows](completion-name-audit.tsv): search aid for named content. Loop-generated/dynamic names, source feature gates and entity identities still require actual runtime registration export.
- [Lighting matrix](lighting-port-matrix.md): all 108 decorative fixtures, their source lookups, minimum special adapters and all 18 source bulb registrations.
- [Audit summary](completion-audit.json) and [curated task definitions](completion-workstreams.json). Existing [adapter backlog/evidence](remaining-work.md) remains the entry point for completed checkpoints.

## Recommended implementation order

Resolve identities, mounting/ownership and semantic solver differences first. Validate the registered108 lighting adapters, including aim/motion/underwater/fan/suspension behavior. Portable carry and the generalized panel unlock the remaining appliance families. Room climate/fluid services precede cooling and growing behavior. Road vehicles reuse pack/charger foundations but still require entities and controls. Run GUI, assets, network, migration and source-oracle reviews alongside each family; finish loaded-world and release campaigns after the intended scope is implemented.

Existing native save compatibility is required throughout. Full source-world conversion is a separate deliverable; do not block all feature development on it or claim native save round trips implement it.

| Stream | Current assessment | Source files | Prerequisites |
|---|---|---:|---|
| [Registration and identity contracts](#registry) | partial | 26 | Identity baseline |
| [Node lifecycle, partial mounting and ghost ownership](#mounting) | partial | 65 | registry |
| [Electrical/thermal solver and shared simulation changes](#solver) | behavior-review | 116 | mounting |
| [Grid spans, cables, pole height and routing](#wiring) | partial | 23 | mounting, solver |
| [Distribution, services and legacy pole attachments](#distribution) | partial | 27 | wiring |
| [69/230 kV substation transformers and disconnects](#substations) | implementation-needed | 2 | distribution, electronics |
| [Twelve-circuit panel and generalized wireless power](#panel) | implementation-needed | 3 | distribution, solver |
| [Remaining decorative fixture catalog](#lighting) | partial | 29 | mounting, registry |
| [Bulb technologies, lifetime and lighting configuration](#bulbs) | partial | 3 | solver, registry |
| [Standalone flood/work/festive lighting](#other-lights) | partial | 24 | lighting, bulbs, cords |
| [Fire alarm network and environmental sensing](#alarms) | implementation-needed | 5 | panel, network, mounting |
| [Portable appliances, carry and remaining cord consumers](#cords) | partial | 12 | mounting, registry |
| [Kitchen appliances, foods and refrigeration](#kitchen) | implementation-needed | 8 | cords, climate, resources |
| [Washer, dryer and fusion matter machine](#laundry) | implementation-needed | 15 | cords, fluids, resources |
| [Room thermal/humidity services and climate appliances](#climate) | implementation-needed | 8 | solver, mounting, cords |
| [Floor/ceiling fans and evaporative cooling](#fans) | implementation-needed | 12 | climate, cords, lighting |
| [Industrial/world-intake pumping and growing beds](#pumps) | implementation-needed | 7 | fluids, climate, cords |
| [Mechanical shafts, thermal machines and fluid systems](#fluids) | behavior-review | 65 | solver, mounting |
| [Road vehicles and in-car charging](#vehicles) | foundation-only | 4 | cords, network, resources |
| [Forklift, pallet, EV pack charger and welding follow-up](#transport) | validation-needed | 1 | network, gui, rendering |
| [Wire machines, chip fabrication and industrial controls](#manufacturing) | partial | 17 | solver, resources, gui |
| [Converters and inherited power electronics](#electronics) | partial | 34 | solver, registry |
| [Renewable and solar adapter completion checks](#renewables) | validation-needed | 23 | mounting, solver, rendering |
| [Storage, standby, UPS, generation and protection completion](#storage) | validation-needed | 30 | solver, cords, gui |
| [Inherited devices, sensing, control and world interactions](#classic) | behavior-review | 277 | solver, mounting, network |
| [Railroad and replicator/entity parity](#entities) | behavior-review | 16 | solver, network, rendering |
| [Tools, copy/configuration, scanners and circuit import](#tools) | behavior-review | 52 | registry, network, classic |
| [Packets, server lifecycle and input authority](#network) | behavior-review | 32 | registry, mounting |
| [Rendering, covers, distant models and graphics compatibility](#rendering) | partial | 50 | mounting, network |
| [Source-matching menus and user interaction](#gui) | partial | 30 | registry, network |
| [Crafting, materials, assets, sound and translations](#resources) | partial | 11 | registry |
| [World generation, progression, tutorials and guides](#world-content) | behavior-review | 20 | registry, resources, classic |
| [MQTT, Modbus, computer/mechanical bridges and addon API](#integrations) | conditional-review | 69 | network, panel, registry |
| [Source oracles, whole-world performance and release verification](#verification) | validation-needed | 95 | registry, network, rendering, gui |
| [Full 1.21.1 world/item/entity conversion](#migration) | implementation-needed | 0 | registry, mounting, network |
| [Bootstrap, platform hooks, configs and compatibility mixins](#build-compat) | behavior-review | 11 | registry, network |
| [Remaining shared support code and explicit review queue](#review-remainder) | behavior-review | 55 | registry, solver |

<a id="registry"></a>
## Registration and identity contracts

**Assessment:** partial. **Prerequisites:** none beyond the pinned source/native baseline.

**Reuse:** The 707-entry native manifest and existing conflict helpers; keep shared-item, six-node, transparent-node and entity namespaces separate.

**Needed:**

- Export the actual active 1.21.1 runtime catalog, including loop-generated variants, hidden legacy entries and development/config gates. Compare names, numeric IDs, properties and recipes with native registration; literal-name search alone cannot finish this job.
- Record an explicit destination for every incoming identity. Preserve occupied native IDs, inventories and old recipe outputs; document display-name aliases and newly required ingredients.
- Adapt entry points, capabilities, attributes, menus, packets, creative categories and lifecycle registration. Do not copy source bootstrap, Gradle configuration or an old dependency set wholesale.

**Completion checks:**

- Every source runtime identity is accounted for as adapted, intentionally hidden/conditional, superseded with a recorded mapping, or still open.
- Load an existing native world and prove old identities/stack data stay exact; run datagen twice and inspect missing/duplicate registrations.

**Source entry points:** [DecorativeLightRegistration.kt](https://github.com/reamnet/ELN1.21.1ForkPatch/blob/b7444a7b8a0c5e072b1fd35eed2b74a282c3348c/src/main/kotlin/mods/eln/registration/DecorativeLightRegistration.kt), [EntityRegistration.kt](https://github.com/reamnet/ELN1.21.1ForkPatch/blob/b7444a7b8a0c5e072b1fd35eed2b74a282c3348c/src/main/kotlin/mods/eln/registration/EntityRegistration.kt), [ItemRegistration.kt](https://github.com/reamnet/ELN1.21.1ForkPatch/blob/b7444a7b8a0c5e072b1fd35eed2b74a282c3348c/src/main/kotlin/mods/eln/registration/ItemRegistration.kt), [SingleNodeRegistration.kt](https://github.com/reamnet/ELN1.21.1ForkPatch/blob/b7444a7b8a0c5e072b1fd35eed2b74a282c3348c/src/main/kotlin/mods/eln/registration/SingleNodeRegistration.kt). Filter `registry` in [the full source audit](completion-source-audit.tsv) for every assigned file.

**Existing review/evidence:** [registry-conflicts.md](registry-conflicts.md), [decorative-lighting.md](decorative-lighting.md). Historical counts in those reports apply to their recorded commits.

<a id="mounting"></a>
## Node lifecycle, partial mounting and ghost ownership

**Assessment:** partial. **Prerequisites:** registry.

**Reuse:** Native node/voxel placement, cached oriented shapes, solar roof/flat adapters and resident-only owner queries.

**Needed:**

- Carry source support-plane insets through placement, selection, collision, drawing, cable rises and light origins together. Preserve grid owner coordinates and invalidate cached geometry when supports change.
- Map source clearance/lamp-clearance/cabinet/roof reservations by owner meaning. Repair and remove reservations without duplicate drops or stale-owner teardown; never reinterpret numeric metadata globally.
- Review generic node save/load, block callbacks, automation ownership and chunk/dimension lifecycle against the source; a matching class name does not establish matching behavior.

**Completion checks:**

- Exercise slabs, every stair half/facing, walls/ceilings, mixed-height terminals, support changes, unloaded neighbors, ray picking and fresh-JVM restoration.
- Native voxel ownership and existing roof/flat/cabinet saves remain valid; loaded-only repair does not request chunks.

**Source entry points:** [GhostElement.kt](https://github.com/reamnet/ELN1.21.1ForkPatch/blob/b7444a7b8a0c5e072b1fd35eed2b74a282c3348c/src/main/kotlin/mods/eln/ghost/GhostElement.kt), [INodeElement.kt](https://github.com/reamnet/ELN1.21.1ForkPatch/blob/b7444a7b8a0c5e072b1fd35eed2b74a282c3348c/src/main/kotlin/mods/eln/node/INodeElement.kt), [SixNodeElement.kt](https://github.com/reamnet/ELN1.21.1ForkPatch/blob/b7444a7b8a0c5e072b1fd35eed2b74a282c3348c/src/main/kotlin/mods/eln/node/six/SixNodeElement.kt), [SixNodeElementInventory.kt](https://github.com/reamnet/ELN1.21.1ForkPatch/blob/b7444a7b8a0c5e072b1fd35eed2b74a282c3348c/src/main/kotlin/mods/eln/node/six/SixNodeElementInventory.kt). Filter `mounting` in [the full source audit](completion-source-audit.tsv) for every assigned file.

**Existing review/evidence:** [mounting-review.md](mounting-review.md), [ghost-migration-review.md](ghost-migration-review.md), [solar-roofs.md](solar-roofs.md), [flat-solar.md](flat-solar.md). Historical counts in those reports apply to their recorded commits.

<a id="solver"></a>
## Electrical/thermal solver and shared simulation changes

**Assessment:** behavior-review. **Prerequisites:** mounting.

**Reuse:** The destination conservative power helpers, accepted-step accounting, guarded finite states and cached LU factors.

**Needed:**

- Diff source sim, MNA, NBT, nonlinear components, regulators and equation helpers against native implementations. Classify each change as already equivalent, intentional native behavior or a required adapter.
- Preserve physical-time integration: solver retries must not consume energy, fuel, bulb life, heat or charge. Retain finite limits, sign conventions, reset/disconnect behavior and stable-matrix caching.
- Review source diagnostics and configuration interaction before changing shared code used by already passing devices.

**Completion checks:**

- Run exact circuit/conservation/thermal regressions, sag/short/reverse-flow and unload/reload campaigns with the existing tolerances.
- Compare matrix rebuild counts and representative coupled-network CPU/memory; retain the default fluid corpus and existing expectations.

**Source entry points:** [SignalLoadSupport.kt](https://github.com/reamnet/ELN1.21.1ForkPatch/blob/b7444a7b8a0c5e072b1fd35eed2b74a282c3348c/src/main/kotlin/mods/eln/sim/SignalLoadSupport.kt), [DiodeProcess.java](https://github.com/reamnet/ELN1.21.1ForkPatch/blob/b7444a7b8a0c5e072b1fd35eed2b74a282c3348c/src/main/java/mods/eln/sim/DiodeProcess.java), [ElectricalConnection.java](https://github.com/reamnet/ELN1.21.1ForkPatch/blob/b7444a7b8a0c5e072b1fd35eed2b74a282c3348c/src/main/java/mods/eln/sim/ElectricalConnection.java), [ElectricalLoad.java](https://github.com/reamnet/ELN1.21.1ForkPatch/blob/b7444a7b8a0c5e072b1fd35eed2b74a282c3348c/src/main/java/mods/eln/sim/ElectricalLoad.java). Filter `solver` in [the full source audit](completion-source-audit.tsv) for every assigned file.

**Existing review/evidence:** [power-electronics-review.md](power-electronics-review.md), [probe-benchmark.json](probe-benchmark.json). Historical counts in those reports apply to their recorded commits.

<a id="wiring"></a>
## Grid spans, cables, pole height and routing

**Assessment:** partial. **Prerequisites:** mounting, solver.

**Reuse:** Paid native utility/multicore wire, catenary caches, manufacturing ledgers and steel span alignment.

**Needed:**

- Review remaining cable tools, legacy pole height upgrades, attachments, phase inheritance and adaptive primary/service lead/rack alignment.
- Reconcile source wind/weather/thermal-span refresh, covered cable behavior and span visibility with native conductor resistance, mass, insulation, melt state and actual paid lengths.
- Keep topology updates on real endpoint changes; geometry/status polling must not recreate stable electrical links or retain unloaded worlds.

**Completion checks:**

- Verify all facings/heights and terminal combinations, obstructed turns, long spans, hot/damaged partial spools, reload, refunds and unloaded endpoints.
- Check physical cable endpoints, phase labels and loaded-pole visibility in the client, including legacy pole families.

**Source entry points:** [CableRenderType.kt](https://github.com/reamnet/ELN1.21.1ForkPatch/blob/b7444a7b8a0c5e072b1fd35eed2b74a282c3348c/src/main/kotlin/mods/eln/cable/CableRenderType.kt), [GridElement.kt](https://github.com/reamnet/ELN1.21.1ForkPatch/blob/b7444a7b8a0c5e072b1fd35eed2b74a282c3348c/src/main/kotlin/mods/eln/gridnode/GridElement.kt), [ElectricalPoleElement.kt](https://github.com/reamnet/ELN1.21.1ForkPatch/blob/b7444a7b8a0c5e072b1fd35eed2b74a282c3348c/src/main/kotlin/mods/eln/gridnode/electricalpole/ElectricalPoleElement.kt), [CableRenderDescriptor.kt](https://github.com/reamnet/ELN1.21.1ForkPatch/blob/b7444a7b8a0c5e072b1fd35eed2b74a282c3348c/src/main/kotlin/mods/eln/cable/CableRenderDescriptor.kt). Filter `wiring` in [the full source audit](completion-source-audit.tsv) for every assigned file.

**Existing review/evidence:** [overhead-checkpoint-validation.json](overhead-checkpoint-validation.json), [next-distribution-work.md](next-distribution-work.md), [power-poles.md](power-poles.md), [wire-manufacturing.md](wire-manufacturing.md). Historical counts in those reports apply to their recorded commits.

<a id="distribution"></a>
## Distribution, services and legacy pole attachments

**Assessment:** partial. **Prerequisites:** wiring.

**Reuse:** All 30 distribution variants, source-style hardware, protection, paid pole lamps, services/meters/handholes and steel poles.

**Needed:**

- Review non-distribution legacy streetlight ports, other height/surge attachments, hidden standalone recovery and source/native selection geometry.
- Finish adaptive service/primary rack alignment and verify fuse/core/regulator/recloser/capacitor/switch behavior in whole upstream/downstream installations.
- Recheck service meters, split-phase wiring, grounded/return paths, phase isolation and source display/status parts rather than equating render emission with visual correctness.

**Completion checks:**

- Use all 30 registered variants, all four steel types and old-save recovery fixtures without removing coverage.
- Run paid attachment/removal, fault/reset, multi-phase isolation, remote-client and owner reload campaigns.

**Source entry points:** [ServiceMastStyle.kt](https://github.com/reamnet/ELN1.21.1ForkPatch/blob/b7444a7b8a0c5e072b1fd35eed2b74a282c3348c/src/main/kotlin/mods/eln/utility/ServiceMastStyle.kt), [ColorCableTray.kt](https://github.com/reamnet/ELN1.21.1ForkPatch/blob/b7444a7b8a0c5e072b1fd35eed2b74a282c3348c/src/main/kotlin/mods/eln/utility/ColorCableTray.kt), [DistributionClient.kt](https://github.com/reamnet/ELN1.21.1ForkPatch/blob/b7444a7b8a0c5e072b1fd35eed2b74a282c3348c/src/main/kotlin/mods/eln/utility/DistributionClient.kt), [DistributionPole.kt](https://github.com/reamnet/ELN1.21.1ForkPatch/blob/b7444a7b8a0c5e072b1fd35eed2b74a282c3348c/src/main/kotlin/mods/eln/utility/DistributionPole.kt). Filter `distribution` in [the full source audit](completion-source-audit.tsv) for every assigned file.

**Existing review/evidence:** [distribution-parity.json](distribution-parity.json), [next-distribution-work.md](next-distribution-work.md), [power-poles.md](power-poles.md). Historical counts in those reports apply to their recorded commits.

<a id="substations"></a>
## 69/230 kV substation transformers and disconnects

**Assessment:** implementation-needed. **Prerequisites:** distribution, electronics.

**Reuse:** Steel pole/risers 8014/8015/8020/8021, pad-transformer and native protection/converter primitives.

**Needed:**

- Adapt source transmission lookups 8016–8019 with their actual transformer/disconnect circuit topology, independent terminals, paid hardware and thermal/protection state.
- Bring over station-specific ghost footprint, collision/picking, nameplates, leads, controls, recipes and packet/save fields; check every target identity first.
- Connect a real transmission source through the equipment to an actual distribution/service load; a pole model or an ideal voltage assignment is insufficient.

**Completion checks:**

- Check open/closed isolation, voltage ratio, delivered work/loss, overload trips and reset, removed hardware and restored ownership.
- Validate complete footprints, control authority and nameplate/phase rendering on a dedicated server and client.

**Source entry points:** [TransmissionClient.kt](https://github.com/reamnet/ELN1.21.1ForkPatch/blob/b7444a7b8a0c5e072b1fd35eed2b74a282c3348c/src/main/kotlin/mods/eln/utility/TransmissionClient.kt), [TransmissionEquipment.kt](https://github.com/reamnet/ELN1.21.1ForkPatch/blob/b7444a7b8a0c5e072b1fd35eed2b74a282c3348c/src/main/kotlin/mods/eln/utility/TransmissionEquipment.kt). Filter `substations` in [the full source audit](completion-source-audit.tsv) for every assigned file.

**Existing review/evidence:** [power-poles.md](power-poles.md). Historical counts in those reports apply to their recorded commits.

<a id="panel"></a>
## Twelve-circuit panel and generalized wireless power

**Assessment:** implementation-needed. **Prerequisites:** distribution, solver.

**Reuse:** Native three-channel lamp supplies, passive outlet wireless paths and SCADA controls; these are not the source panel.

**Needed:**

- Adapt BreakerPanel and shared WirelessPowerChannels: twelve real branch breaker items, bus selection, 120/240 V service topology, demand ownership, ranges and controls.
- Preserve trip latches/heat and explicit resets; removing a fault must not silently close a breaker. Avoid duplicate demand, cross-world channels and stale-owner registration.
- Add the full branch-selection menu, feeder orientation, source recipes, diagnostics and SCADA endpoint support. Coordinate panel support with portable consumers and the media API.

**Completion checks:**

- Test all branches, bus/return isolation, brownouts, missing breaker stock, overload/reset, channel changes, duplicate owners and unload/reconnect.
- Source channel consumers draw actual upstream power once and restored panels preserve settings and trip state.

**Source entry points:** [BreakerPanel.kt](https://github.com/reamnet/ELN1.21.1ForkPatch/blob/b7444a7b8a0c5e072b1fd35eed2b74a282c3348c/src/main/kotlin/mods/eln/utility/BreakerPanel.kt), [BreakerPanelClient.kt](https://github.com/reamnet/ELN1.21.1ForkPatch/blob/b7444a7b8a0c5e072b1fd35eed2b74a282c3348c/src/main/kotlin/mods/eln/utility/BreakerPanelClient.kt), [WirelessPowerChannels.kt](https://github.com/reamnet/ELN1.21.1ForkPatch/blob/b7444a7b8a0c5e072b1fd35eed2b74a282c3348c/src/main/kotlin/mods/eln/utility/WirelessPowerChannels.kt). Filter `panel` in [the full source audit](completion-source-audit.tsv) for every assigned file.

**Existing review/evidence:** [rated-lamp-supplies.md](rated-lamp-supplies.md), [scada.md](scada.md). Historical counts in those reports apply to their recorded commits.

<a id="lighting"></a>
## Remaining decorative fixture catalog

**Assessment:** partial. **Prerequisites:** mounting, registry.

**Reuse:** All108 registered decorative adapters, original meshes, shared multi-bulb loads, loaded-only projections and native menus; newly implemented aim, motion, water, fan and suspension paths await recorded validation.

**Needed:**

- Validate all108 explicit styles using lighting-port-matrix.md. The current explicit source-ordinal helper covers all three ranges:0–29 group64+20,30–87 group68,and88+ group70; preserve those IDs. Portable carry/cord ownership, technology/configuration and partial-block support remain open.
- Transfer exact per-style placement, clearance, head positions, beam widths, item poses, colors and exposed-bulb rules. Large chandeliers and strings need their complete source reservation/render bounds.
- Validate implemented per-head pan/tilt and axial parking/street aiming in physical light origins and rendering with matching save/packet validation. Validate motion detection, 30-second hold and mode controls using physical time.
- Validate ceiling fan motors/rotors/hazards, 0–7 m warehouse suspension and portable fixtures through their dependent adapters. Validate pool fittings through the new water-preserving lease adapter, including overlap and expiry; registration alone does not certify underwater behavior.

**Completion checks:**

- Every one of 108 styles has an explicit stable ID/status and passes allowed/disallowed placement, partial bulb count/power, lease/obstruction/unload and fresh-owner publication checks.
- Use source-sized aim/suspension menus, real powered night scenes, resource reload and client/server controls; compare matching source reference views.

**Source entry points:** [DecorativeLampStyle.kt](https://github.com/reamnet/ELN1.21.1ForkPatch/blob/b7444a7b8a0c5e072b1fd35eed2b74a282c3348c/src/main/kotlin/mods/eln/sixnode/lampsocket/DecorativeLampStyle.kt), [LampSocketElement.kt](https://github.com/reamnet/ELN1.21.1ForkPatch/blob/b7444a7b8a0c5e072b1fd35eed2b74a282c3348c/src/main/kotlin/mods/eln/sixnode/lampsocket/LampSocketElement.kt), [LampSocketDescriptor.kt](https://github.com/reamnet/ELN1.21.1ForkPatch/blob/b7444a7b8a0c5e072b1fd35eed2b74a282c3348c/src/main/kotlin/mods/eln/sixnode/lampsocket/LampSocketDescriptor.kt), [LightBlock.kt](https://github.com/reamnet/ELN1.21.1ForkPatch/blob/b7444a7b8a0c5e072b1fd35eed2b74a282c3348c/src/main/kotlin/mods/eln/lightblock/LightBlock.kt). Filter `lighting` in [the full source audit](completion-source-audit.tsv) for every assigned file.

**Existing review/evidence:** [decorative-lighting.md](decorative-lighting.md), [decorative-lighting-validation.json](decorative-lighting-validation.json). Historical counts in those reports apply to their recorded commits.

<a id="bulbs"></a>
## Bulb technologies, lifetime and lighting configuration

**Assessment:** partial. **Prerequisites:** solver, registry.

**Reuse:** Native bulbs and the source-rated 120/240 V LEDs at 302/303; old 50/200 V identities are preserved.

**Needed:**

- Inventory every source 12/120/240 V technology and its registered identity, brightness, power, crop growth, minimum voltage, warmup and lifetime/configuration behavior. There are 18 source registrations; two have explicit source-rated LED adapters, leaving sixteen ratings/technology identities to reconcile.
- Adapt remaining incandescent/carbon/fluorescent/farming/halogen variants and source reset/configuration tools without overwriting old bulbs or silently replacing native aging semantics.
- Handle used-stack condition, technology-specific infinite-life settings and conversion/crafting policies explicitly. Reusing a shape does not establish equivalent wear or electrical demand.

**Completion checks:**

- At matching and mismatched voltages verify real watts, startup/dimming, growth and wear; multi-bulb wear must be per bulb.
- Used components survive insertion, automation, copy/configuration, removal and reload without renewal or loss; old menu acceptance changes stay visible in golden review.

**Source entry points:** [LampDescriptor.kt](https://github.com/reamnet/ELN1.21.1ForkPatch/blob/b7444a7b8a0c5e072b1fd35eed2b74a282c3348c/src/main/kotlin/mods/eln/item/lampitem/LampDescriptor.kt), [LampItemSlot.kt](https://github.com/reamnet/ELN1.21.1ForkPatch/blob/b7444a7b8a0c5e072b1fd35eed2b74a282c3348c/src/main/kotlin/mods/eln/item/lampitem/LampItemSlot.kt), [LampTechnology.kt](https://github.com/reamnet/ELN1.21.1ForkPatch/blob/b7444a7b8a0c5e072b1fd35eed2b74a282c3348c/src/main/kotlin/mods/eln/item/lampitem/LampTechnology.kt). Filter `bulbs` in [the full source audit](completion-source-audit.tsv) for every assigned file.

**Existing review/evidence:** [decorative-lighting.md](decorative-lighting.md). Historical counts in those reports apply to their recorded commits.

<a id="other-lights"></a>
## Standalone flood/work/festive lighting

**Assessment:** partial. **Prerequisites:** lighting, bulbs, cords.

**Reuse:** Native utility lamps, legacy floodlights and the new fixed decorative renderer; check source device identity before choosing a counterpart.

**Needed:**

- Review remaining basic/motorized floodlights, gasoline construction work light, Christmas tree, holiday candle, standalone strings and fairy/deck globe strings separately from the 108 decorative socket styles.
- Adapt fuel/startup, real motor/aiming power, optical damage/interaction, bounds, controls, bulb inventories, cords and paid recipes for each relevant owner.
- Match source seasonal colors, motion/animation, on/off effects and sound loops; do not register interactive devices with a static lamp-only renderer.

**Completion checks:**

- Run no-fuel/no-voltage, partial bulbs, moving aim, paid fuel/use, saved state and relevant damage cases.
- Inspect lit/unlit models, animation and sound starts/stops after unload, removal and network reconnection.

**Source entry points:** [FestiveElement.kt](https://github.com/reamnet/ELN1.21.1ForkPatch/blob/b7444a7b8a0c5e072b1fd35eed2b74a282c3348c/src/main/kotlin/mods/eln/transparentnode/festive/FestiveElement.kt), [FloodlightElement.kt](https://github.com/reamnet/ELN1.21.1ForkPatch/blob/b7444a7b8a0c5e072b1fd35eed2b74a282c3348c/src/main/kotlin/mods/eln/transparentnode/floodlight/FloodlightElement.kt), [FloodlightDescriptor.kt](https://github.com/reamnet/ELN1.21.1ForkPatch/blob/b7444a7b8a0c5e072b1fd35eed2b74a282c3348c/src/main/kotlin/mods/eln/transparentnode/floodlight/FloodlightDescriptor.kt), [UtilityLightDescriptors.kt](https://github.com/reamnet/ELN1.21.1ForkPatch/blob/b7444a7b8a0c5e072b1fd35eed2b74a282c3348c/src/main/kotlin/mods/eln/transparentnode/utilitylight/UtilityLightDescriptors.kt). Filter `other-lights` in [the full source audit](completion-source-audit.tsv) for every assigned file.

**Existing review/evidence:** [legacy-lighting-validation.json](legacy-lighting-validation.json), [pole-lamp-native-tests.json](pole-lamp-native-tests.json). Historical counts in those reports apply to their recorded commits.

<a id="alarms"></a>
## Fire alarm network and environmental sensing

**Assessment:** implementation-needed. **Prerequisites:** panel, network, mounting.

**Reuse:** Native generic alarms/sensors and actual power/packet primitives; source networked alarm equipment is a distinct family.

**Needed:**

- Port the panel, pull station, horn/strobe, bell, strobe, smoke and heat detector definitions, stable registration, network association and environmental detection.
- Retain panel latching, silence/reset/test behavior, power demands, detection thresholds and wall/ceiling placement. Bound network and world queries to loaded owners.
- Add source menus, models, flash/sound timing, recipes and configuration/copy behavior.

**Completion checks:**

- Test independent networks, unpowered detection/notification behavior, latching/reset, removed/replaced panels, unloaded detectors and saved links.
- Check synchronized flash/sound and remote controls with more than one observer.

**Source entry points:** [AlarmKind.kt](https://github.com/reamnet/ELN1.21.1ForkPatch/blob/b7444a7b8a0c5e072b1fd35eed2b74a282c3348c/src/main/kotlin/mods/eln/transparentnode/firealarm/AlarmKind.kt), [AlarmElement.kt](https://github.com/reamnet/ELN1.21.1ForkPatch/blob/b7444a7b8a0c5e072b1fd35eed2b74a282c3348c/src/main/kotlin/mods/eln/transparentnode/firealarm/AlarmElement.kt), [AlarmClient.kt](https://github.com/reamnet/ELN1.21.1ForkPatch/blob/b7444a7b8a0c5e072b1fd35eed2b74a282c3348c/src/main/kotlin/mods/eln/transparentnode/firealarm/AlarmClient.kt), [AlarmNetwork.kt](https://github.com/reamnet/ELN1.21.1ForkPatch/blob/b7444a7b8a0c5e072b1fd35eed2b74a282c3348c/src/main/kotlin/mods/eln/transparentnode/firealarm/AlarmNetwork.kt). Filter `alarms` in [the full source audit](completion-source-audit.tsv) for every assigned file.

<a id="cords"></a>
## Portable appliances, carry and remaining cord consumers

**Assessment:** partial. **Prerequisites:** mounting, registry.

**Reuse:** Nine native cord ratings, outlets/strips, compact UPS sources, fixed-machine adapters and durable paid reservations.

**Needed:**

- Implement portable carry as an ownership transition: preserve item/component inventories, settings, UUIDs, paid cords and socket claims while packing, moving, replacing and dropping devices.
- Audit every source consumer against its actual inlet and return topology. Add only intended consumers; generators and storage outputs must not become accidental appliance inputs.
- Handle loops, duplicate tokens, range/rating changes, dimensions, dead sources versus temporary unload, one-time refunds and mutually exclusive cable/wireless/cord feeds.
- Review remaining connector/cord geometry, source selection and un/plug controls on partial supports and new portable fixtures.

**Completion checks:**

- Survival carry/drop/reconnect campaigns retain exact contents and paid hardware once; stale owners cannot detach new owners.
- Test multiple clients, simultaneous socket claims, source replacement, chunk boundaries and fresh-server restoration.

**Source entry points:** [OutletElement.kt](https://github.com/reamnet/ELN1.21.1ForkPatch/blob/b7444a7b8a0c5e072b1fd35eed2b74a282c3348c/src/main/kotlin/mods/eln/transparentnode/cord/OutletElement.kt), [MachineCordSupport.kt](https://github.com/reamnet/ELN1.21.1ForkPatch/blob/b7444a7b8a0c5e072b1fd35eed2b74a282c3348c/src/main/kotlin/mods/eln/transparentnode/cord/MachineCordSupport.kt), [ApplianceCord.kt](https://github.com/reamnet/ELN1.21.1ForkPatch/blob/b7444a7b8a0c5e072b1fd35eed2b74a282c3348c/src/main/kotlin/mods/eln/transparentnode/cord/ApplianceCord.kt), [CompactSocketGeometry.kt](https://github.com/reamnet/ELN1.21.1ForkPatch/blob/b7444a7b8a0c5e072b1fd35eed2b74a282c3348c/src/main/kotlin/mods/eln/transparentnode/cord/CompactSocketGeometry.kt). Filter `cords` in [the full source audit](completion-source-audit.tsv) for every assigned file.

**Existing review/evidence:** [appliance-cord-review.md](appliance-cord-review.md), [cord-outlets.md](cord-outlets.md), [compact-ups.md](compact-ups.md), [ghost-consumers.md](ghost-consumers.md). Historical counts in those reports apply to their recorded commits.

<a id="kitchen"></a>
## Kitchen appliances, foods and refrigeration

**Assessment:** implementation-needed. **Prerequisites:** cords, climate, resources.

**Reuse:** Native paid inlets, inventory/recipe helpers, heat networks and source mesh assets.

**Needed:**

- Adapt all eleven KitchenType entries: microwave, toaster, cooktop, refrigerator, coffee maker, kettle, blender, rice cooker, mini oven, mini fridge and patio grill.
- Preserve each food/material inventory, recipe timing, actual electrical/heat input, water handling, battery heating interactions and output-food registration. Keep cold storage tied to actual thermal behavior.
- Review refrigerator preservation/spoilage hooks and optional mixins as separate compatibility contracts; implement source menus, door/status parts, animations, cords and food translations.

**Completion checks:**

- Prove no processing or preservation benefit from absent power, conserved material/output counts, temperature behavior and exact inventories after reload/automation.
- Exercise food recipes, condition/spoilage integration with each optional mod both absent and present, and visual/menu interaction.

**Source entry points:** [KitchenType.kt](https://github.com/reamnet/ELN1.21.1ForkPatch/blob/b7444a7b8a0c5e072b1fd35eed2b74a282c3348c/src/main/kotlin/mods/eln/transparentnode/kitchen/KitchenType.kt), [KitchenElement.kt](https://github.com/reamnet/ELN1.21.1ForkPatch/blob/b7444a7b8a0c5e072b1fd35eed2b74a282c3348c/src/main/kotlin/mods/eln/transparentnode/kitchen/KitchenElement.kt), [RefrigeratorPreservation.kt](https://github.com/reamnet/ELN1.21.1ForkPatch/blob/b7444a7b8a0c5e072b1fd35eed2b74a282c3348c/src/main/kotlin/mods/eln/compat/RefrigeratorPreservation.kt), [KitchenClient.kt](https://github.com/reamnet/ELN1.21.1ForkPatch/blob/b7444a7b8a0c5e072b1fd35eed2b74a282c3348c/src/main/kotlin/mods/eln/transparentnode/kitchen/KitchenClient.kt). Filter `kitchen` in [the full source audit](completion-source-audit.tsv) for every assigned file.

<a id="laundry"></a>
## Washer, dryer and fusion matter machine

**Assessment:** implementation-needed. **Prerequisites:** cords, fluids, resources.

**Reuse:** Native inventory/energy accounting, 30 A dryer cord rating and source value-graph helpers for review.

**Needed:**

- Adapt washer water transactions and wet-equipment item metadata; dryer heating, 240 V/30 A connection and actual energy-paid cycles must preserve source ratings.
- Port Fusion Matter Reactor recipe/value graph, arithmetic/collectors and material valuation safely. Do not confuse this family with a free-energy generator.
- Bring over process progress, cancellation/automation, outputs, recipes, meshes, sound, menus and versioned saves.

**Completion checks:**

- Check input shortage, interrupted cycles, water rollback, exact wet-item contents and no material/energy duplication across reload.
- Prove reactor valuation/cycles terminate for recursive or unsupported recipes and outputs are paid by actual accepted energy/materials.

**Source entry points:** [MatterMachineKind.kt](https://github.com/reamnet/ELN1.21.1ForkPatch/blob/b7444a7b8a0c5e072b1fd35eed2b74a282c3348c/src/main/kotlin/mods/eln/transparentnode/laundry/MatterMachineKind.kt), [MatterMachineElement.kt](https://github.com/reamnet/ELN1.21.1ForkPatch/blob/b7444a7b8a0c5e072b1fd35eed2b74a282c3348c/src/main/kotlin/mods/eln/transparentnode/laundry/MatterMachineElement.kt), [SimpleGraphMapper.java](https://github.com/reamnet/ELN1.21.1ForkPatch/blob/b7444a7b8a0c5e072b1fd35eed2b74a282c3348c/src/main/java/mods/eln/transparentnode/fusion/value/SimpleGraphMapper.java), [FullBigFractionArithmetic.java](https://github.com/reamnet/ELN1.21.1ForkPatch/blob/b7444a7b8a0c5e072b1fd35eed2b74a282c3348c/src/main/java/mods/eln/transparentnode/fusion/value/arithmetic/FullBigFractionArithmetic.java). Filter `laundry` in [the full source audit](completion-source-audit.tsv) for every assigned file.

<a id="climate"></a>
## Room thermal/humidity services and climate appliances

**Assessment:** implementation-needed. **Prerequisites:** solver, mounting, cords.

**Reuse:** Native thermal networks and environmental sampling; source room model is not established by those existing systems.

**Needed:**

- Adapt biome climate, room sampling/cache/invalidation and block-change events with bounded loaded-world work and explicit heat-capacity/exchange units.
- Port seven ClimateType variants (portable heater, three fireplace stands, window AC, mini-split and patio heater), thermostat hysteresis, target/enable controls and real heat/cooling/rejection power.
- Coordinate humidity, room leaks/doors/support changes and appliance sound with the fluid/evaporative and growing-bed families.

**Completion checks:**

- Brownouts/no supply yield no artificial heat/cooling; rejected energy and heat exchange balance and timers advance only physically.
- Run room openings/rebuilds, large buildings, chunk unload, invalid sensors, saved targets and hot/cold/weather performance comparisons.

**Source entry points:** [ClimateType.kt](https://github.com/reamnet/ELN1.21.1ForkPatch/blob/b7444a7b8a0c5e072b1fd35eed2b74a282c3348c/src/main/kotlin/mods/eln/transparentnode/climate/ClimateType.kt), [ClimateElement.kt](https://github.com/reamnet/ELN1.21.1ForkPatch/blob/b7444a7b8a0c5e072b1fd35eed2b74a282c3348c/src/main/kotlin/mods/eln/transparentnode/climate/ClimateElement.kt), [RoomThermalManager.kt](https://github.com/reamnet/ELN1.21.1ForkPatch/blob/b7444a7b8a0c5e072b1fd35eed2b74a282c3348c/src/main/kotlin/mods/eln/environment/RoomThermalManager.kt), [BiomeClimateService.kt](https://github.com/reamnet/ELN1.21.1ForkPatch/blob/b7444a7b8a0c5e072b1fd35eed2b74a282c3348c/src/main/kotlin/mods/eln/environment/BiomeClimateService.kt). Filter `climate` in [the full source audit](completion-source-audit.tsv) for every assigned file.

<a id="fans"></a>
## Floor/ceiling fans and evaporative cooling

**Assessment:** implementation-needed. **Prerequisites:** climate, cords, lighting.

**Reuse:** Source mesh assets, native electricity and sound/animation primitives; five decorative ceiling-fan motor adapters are registered with behavior validation pending. Other fan appliances and airflow integration remain open.

**Needed:**

- Adapt industrial, classic, shop and compact standalone fans and validate all five registered decorative ceiling fan styles. Include motor watts, spin-up/coast, controls, airflow effects and industrial blade hazards.
- Port evaporative cooler variants, actual water use, humidity/temperature response, tank/port automation and dry/unpowered behavior.
- Keep rendered rotor state, collision/hazard radius, sound lifecycle and actual power synchronized across server, client and reload.

**Completion checks:**

- Stopped/unpowered fans do not grant free continuous airflow; every active motor and cooler pays actual power/water.
- Check players/entities near blades, walls, all mounting orientations, occlusion, audio and multi-client animation.

**Source entry points:** [FanType.kt](https://github.com/reamnet/ELN1.21.1ForkPatch/blob/b7444a7b8a0c5e072b1fd35eed2b74a282c3348c/src/main/kotlin/mods/eln/transparentnode/fan/FanType.kt), [FanElement.kt](https://github.com/reamnet/ELN1.21.1ForkPatch/blob/b7444a7b8a0c5e072b1fd35eed2b74a282c3348c/src/main/kotlin/mods/eln/transparentnode/fan/FanElement.kt), [EvaporationModel.java](https://github.com/reamnet/ELN1.21.1ForkPatch/blob/b7444a7b8a0c5e072b1fd35eed2b74a282c3348c/src/main/java/mods/eln/transparentnode/evaporative/EvaporationModel.java), [EvaporativeControls.java](https://github.com/reamnet/ELN1.21.1ForkPatch/blob/b7444a7b8a0c5e072b1fd35eed2b74a282c3348c/src/main/java/mods/eln/transparentnode/evaporative/EvaporativeControls.java). Filter `fans` in [the full source audit](completion-source-audit.tsv) for every assigned file.

<a id="pumps"></a>
## Industrial/world-intake pumping and growing beds

**Assessment:** implementation-needed. **Prerequisites:** fluids, climate, cords.

**Reuse:** Native pressure/fluid systems, world interactions, actual electrical loads and machine inventories.

**Needed:**

- Adapt 480 V Industrial Fluid Pump ports/intake, head/pressure and source fluid ownership rules to native fluids; avoid treating existing pressure pumps as identical devices.
- Port growing-bed crop cycle, water/nutrient/material inventories and climate/light dependencies with source controls and visuals.
- Make transfer/world-intake and processing transactions atomic with accepted energy, safe boundaries, loaded-only lookups and persisted progress.

**Completion checks:**

- Prove paid transfer/cycles under dry intake, obstruction, backpressure, brownout, blocked output and unload/reload.
- Verify finite water extraction, material counts, crop/climate effects, automation and exact port/selection geometry.

**Source entry points:** [GrowingBedElement.kt](https://github.com/reamnet/ELN1.21.1ForkPatch/blob/b7444a7b8a0c5e072b1fd35eed2b74a282c3348c/src/main/kotlin/mods/eln/transparentnode/growing/GrowingBedElement.kt), [IndustrialPumpElement.kt](https://github.com/reamnet/ELN1.21.1ForkPatch/blob/b7444a7b8a0c5e072b1fd35eed2b74a282c3348c/src/main/kotlin/mods/eln/transparentnode/pump/IndustrialPumpElement.kt), [GrowingBedClient.kt](https://github.com/reamnet/ELN1.21.1ForkPatch/blob/b7444a7b8a0c5e072b1fd35eed2b74a282c3348c/src/main/kotlin/mods/eln/transparentnode/growing/GrowingBedClient.kt), [GrowingCycle.kt](https://github.com/reamnet/ELN1.21.1ForkPatch/blob/b7444a7b8a0c5e072b1fd35eed2b74a282c3348c/src/main/kotlin/mods/eln/transparentnode/growing/GrowingCycle.kt). Filter `pumps` in [the full source audit](completion-source-audit.tsv) for every assigned file.

<a id="fluids"></a>
## Mechanical shafts, thermal machines and fluid systems

**Assessment:** behavior-review. **Prerequisites:** solver, mounting.

**Reuse:** Existing native shaft networks, boilers/turbines/water systems, fluid corpus and cached geometry.

**Needed:**

- Diff source shaft joints, motors/generators, flywheels, clutches, blade wear and large-machine layouts against native behavior; preserve real torque, inertia, energy and ghost shaft ports.
- Review legacy furnaces, incubator, heat exchangers/dissipators, turbines, fuel/thermal/fluid registry and sided/precise transfer changes. Keep native steam/pressure conventions rather than copying foreign APIs.
- Bring missing controls, shapes, recipes and wear/fuel/temperature state across only after identifying real semantic differences.

**Completion checks:**

- Run mechanical start/stop/reverse/overload and shaft-layout campaigns; prove electrical/mechanical/thermal work balances.
- Retain all fluid goldens, default 200-case corpus, pressure/phase transitions and native saves; compare long-running source/native installations.

**Source entry points:** [FluidRegistration.kt](https://github.com/reamnet/ELN1.21.1ForkPatch/blob/b7444a7b8a0c5e072b1fd35eed2b74a282c3348c/src/main/kotlin/mods/eln/fluid/FluidRegistration.kt), [ElementFluidHandler.java](https://github.com/reamnet/ELN1.21.1ForkPatch/blob/b7444a7b8a0c5e072b1fd35eed2b74a282c3348c/src/main/java/mods/eln/fluid/ElementFluidHandler.java), [ThermalCableElement.java](https://github.com/reamnet/ELN1.21.1ForkPatch/blob/b7444a7b8a0c5e072b1fd35eed2b74a282c3348c/src/main/java/mods/eln/sixnode/thermalcable/ThermalCableElement.java), [EggIncubatorElement.java](https://github.com/reamnet/ELN1.21.1ForkPatch/blob/b7444a7b8a0c5e072b1fd35eed2b74a282c3348c/src/main/java/mods/eln/transparentnode/eggincubator/EggIncubatorElement.java). Filter `fluids` in [the full source audit](completion-source-audit.tsv) for every assigned file.

<a id="vehicles"></a>
## Road vehicles and in-car charging

**Assessment:** foundation-only. **Prerequisites:** cords, network, resources.

**Reuse:** Native RoadPhysics is a pure helper; it does not establish registered road vehicles. Reuse the paid traction pack/charger primitives.

**Needed:**

- Adapt the four-seat Electric Golf Cart and two-seat Arc Runner Coupe entity/item/menu registrations, server-authoritative driving, stepping/collision, passenger positions and storage.
- Preserve one/six pack capacity, actual driving drain and 240 V parked charging, disconnect/range behavior, dangerous-corner/fire/explosion policy and item/drop accounting.
- Match clear coupe windows, moving wheels, camera/seat transforms, sound, controls, recipes and persisted cargo/energy.

**Completion checks:**

- Use actual passengers and driver/observer clients; reject remote/stale control and charging while driving or with invalid packs.
- Exercise slopes, collisions, rollover thresholds, death/removal, multi-pack debit limits, cargo and fresh-server restoration.

**Source entry points:** [RoadVehicles.kt](https://github.com/reamnet/ELN1.21.1ForkPatch/blob/b7444a7b8a0c5e072b1fd35eed2b74a282c3348c/src/main/kotlin/mods/eln/vehicle/RoadVehicles.kt), [VehicleCharger.kt](https://github.com/reamnet/ELN1.21.1ForkPatch/blob/b7444a7b8a0c5e072b1fd35eed2b74a282c3348c/src/main/kotlin/mods/eln/vehicle/VehicleCharger.kt), [VehicleClient.kt](https://github.com/reamnet/ELN1.21.1ForkPatch/blob/b7444a7b8a0c5e072b1fd35eed2b74a282c3348c/src/main/kotlin/mods/eln/vehicle/VehicleClient.kt), [VehicleControls.kt](https://github.com/reamnet/ELN1.21.1ForkPatch/blob/b7444a7b8a0c5e072b1fd35eed2b74a282c3348c/src/main/kotlin/mods/eln/vehicle/VehicleControls.kt). Filter `vehicles` in [the full source audit](completion-source-audit.tsv) for every assigned file.

**Existing review/evidence:** [industrial-transport.md](industrial-transport.md). Historical counts in those reports apply to their recorded commits.

<a id="transport"></a>
## Forklift, pallet, EV pack charger and welding follow-up

**Assessment:** validation-needed. **Prerequisites:** network, gui, rendering.

**Reuse:** Registered forklift/pallet, real traction packs, charger/welder and paid lifting/manufacturing adapters.

**Needed:**

- Complete interactive steering/lifting/loading, audio, passenger/driver authority, remote observers and transport through chunk boundaries.
- Review source state/geometry changes not covered by the existing adapter evidence, including cargo selection and pack/recharge interactions.
- Retain one-time cargo/pack/hardware refunds and conservative accepted-energy debit in mixed manufacturing campaigns.

**Completion checks:**

- Run two-client drive/lift/store/reload/unload/recharge campaigns with exact cargo and energy checks.
- Inspect hands/controls, moving parts, source menu geometry, audio start/stop and removal under load.

**Source entry points:** [ForkliftControls.kt](https://github.com/reamnet/ELN1.21.1ForkPatch/blob/b7444a7b8a0c5e072b1fd35eed2b74a282c3348c/src/main/kotlin/mods/eln/transparentnode/industrial/ForkliftControls.kt). Filter `transport` in [the full source audit](completion-source-audit.tsv) for every assigned file.

**Existing review/evidence:** [industrial-transport.md](industrial-transport.md), [workshop.md](workshop.md), [workshop-transport-validation.json](workshop-transport-validation.json). Historical counts in those reports apply to their recorded commits.

<a id="manufacturing"></a>
## Wire machines, chip fabrication and industrial controls

**Assessment:** partial. **Prerequisites:** solver, resources, gui.

**Reuse:** Paid Roller/Insulator/Combiner, Wire Snips, Fabricator/components, load bank, VFD, surge and SCADA adapters.

**Needed:**

- Audit remaining wire/cable tools and complete source acquisition/automation chains through wafer/components, hot partial spools, jacket/core material and finished bundles.
- Review source industrial device displays, controls, optional JEI examples and interactive audio; finish SCADA endpoints for newly adapted panel/media/other owners.
- Preserve paid attempt buffers, failed-yield debits, output blockage and source/native recipe aliases; do not replace native process implementations wholesale.

**Completion checks:**

- Run survival acquisition-to-finished-product and drive/charge/recharge scenes with output automation and actual owner reloads.
- Prove exact material/energy ledgers, cached matrices, hot/damaged metadata, malformed controls and remote clients.

**Source entry points:** [IndustrialKind.kt](https://github.com/reamnet/ELN1.21.1ForkPatch/blob/b7444a7b8a0c5e072b1fd35eed2b74a282c3348c/src/main/kotlin/mods/eln/transparentnode/industrial/IndustrialKind.kt), [IndustrialElement.kt](https://github.com/reamnet/ELN1.21.1ForkPatch/blob/b7444a7b8a0c5e072b1fd35eed2b74a282c3348c/src/main/kotlin/mods/eln/transparentnode/industrial/IndustrialElement.kt), [BasicComponents.kt](https://github.com/reamnet/ELN1.21.1ForkPatch/blob/b7444a7b8a0c5e072b1fd35eed2b74a282c3348c/src/main/kotlin/mods/eln/item/BasicComponents.kt), [WireMachineItems.kt](https://github.com/reamnet/ELN1.21.1ForkPatch/blob/b7444a7b8a0c5e072b1fd35eed2b74a282c3348c/src/main/kotlin/mods/eln/item/WireMachineItems.kt). Filter `manufacturing` in [the full source audit](completion-source-audit.tsv) for every assigned file.

**Existing review/evidence:** [wire-manufacturing.md](wire-manufacturing.md), [chip-fabrication-review.md](chip-fabrication-review.md), [chip-fabrication.md](chip-fabrication.md), [motor-drive.md](motor-drive.md). Historical counts in those reports apply to their recorded commits.

<a id="electronics"></a>
## Converters and inherited power electronics

**Assessment:** partial. **Prerequisites:** solver, registry.

**Reuse:** Native variable129, fixed130 and one-way131–134 with paid windings and the retained converter128 identity.

**Needed:**

- Finish semantic comparison of inherited transformer, capacitor, inductor, regulated-supply, resistor/rheostat and power-source/sink changes.
- Treat development-only floating isolation135 as a separate conditional decision/adapter; do not publish it merely because its code exists in the source.
- Preserve V1/V2 control meanings, grounding/isolation, paid fractional winding wire, current/voltage/temperature limits and thermal accepted-step timing.

**Completion checks:**

- Run floating/grounded and coupled converter banks, reverse flow, short/recovery, hot wire removal and native voxel integration.
- Review display/coil geometry and configuration copying; retain all existing conservative solver/protection expectations.

**Source entry points:** [PowerCapacitorElement.java](https://github.com/reamnet/ELN1.21.1ForkPatch/blob/b7444a7b8a0c5e072b1fd35eed2b74a282c3348c/src/main/java/mods/eln/transparentnode/powercapacitor/PowerCapacitorElement.java), [PowerInductorElement.java](https://github.com/reamnet/ELN1.21.1ForkPatch/blob/b7444a7b8a0c5e072b1fd35eed2b74a282c3348c/src/main/java/mods/eln/transparentnode/powerinductor/PowerInductorElement.java), [GridTransformerElement.kt](https://github.com/reamnet/ELN1.21.1ForkPatch/blob/b7444a7b8a0c5e072b1fd35eed2b74a282c3348c/src/main/kotlin/mods/eln/gridnode/transformer/GridTransformerElement.kt), [PowerCapacitorDescriptor.java](https://github.com/reamnet/ELN1.21.1ForkPatch/blob/b7444a7b8a0c5e072b1fd35eed2b74a282c3348c/src/main/java/mods/eln/transparentnode/powercapacitor/PowerCapacitorDescriptor.java). Filter `electronics` in [the full source audit](completion-source-audit.tsv) for every assigned file.

**Existing review/evidence:** [power-electronics-review.md](power-electronics-review.md), [converter-family.md](converter-family.md), [variable-dcdc.md](variable-dcdc.md), [variable-inductor.md](variable-inductor.md). Historical counts in those reports apply to their recorded commits.

<a id="renewables"></a>
## Renewable and solar adapter completion checks

**Assessment:** validation-needed. **Prerequisites:** mounting, solver, rendering.

**Reuse:** Registered solar/roof/flat, MPPT/hybrid, wind and large watermill adapters and bounded environmental sampling.

**Needed:**

- Review any remaining source control, model/status, roof selection, shade/daylight, fluid/rotor and partial-support differences against current native paths.
- Complete source-sized larger menus, actual GPU/audio and observer-client campaigns, including changed supports and distant loaded installations.
- Compare daylight/occlusion and mixed charging/drive operation in representative source and native worlds without discarding native caches.

**Completion checks:**

- Retain all environmental sampling, paid hardware, accepted energy and fresh-owner tests.
- Show correct graphics/selection and bounded server cost at representative installation scale and through chunk lifecycle changes.

**Source entry points:** [SolarPanelElement.java](https://github.com/reamnet/ELN1.21.1ForkPatch/blob/b7444a7b8a0c5e072b1fd35eed2b74a282c3348c/src/main/java/mods/eln/transparentnode/solarpanel/SolarPanelElement.java), [WaterTurbineElement.java](https://github.com/reamnet/ELN1.21.1ForkPatch/blob/b7444a7b8a0c5e072b1fd35eed2b74a282c3348c/src/main/java/mods/eln/transparentnode/waterturbine/WaterTurbineElement.java), [WindTurbineElement.java](https://github.com/reamnet/ELN1.21.1ForkPatch/blob/b7444a7b8a0c5e072b1fd35eed2b74a282c3348c/src/main/java/mods/eln/transparentnode/windturbine/WindTurbineElement.java), [SolarElement.kt](https://github.com/reamnet/ELN1.21.1ForkPatch/blob/b7444a7b8a0c5e072b1fd35eed2b74a282c3348c/src/main/kotlin/mods/eln/transparentnode/renewable/SolarElement.kt). Filter `renewables` in [the full source audit](completion-source-audit.tsv) for every assigned file.

**Existing review/evidence:** [solar-sampling.md](solar-sampling.md), [solar-roofs.md](solar-roofs.md), [large-renewable-validation.json](large-renewable-validation.json). Historical counts in those reports apply to their recorded commits.

<a id="storage"></a>
## Storage, standby, UPS, generation and protection completion

**Assessment:** validation-needed. **Prerequisites:** solver, cords, gui.

**Reuse:** Registered off-grid cabinets, ATS/generators, relay/shedding and programmable/compact UPS adapters.

**Needed:**

- Compare source control/state, cabinet geometry, battery chemistry/cell rules, bypass, fuel/start behavior and protection changes not yet covered by evidence.
- Complete large screen coordinate matching and interactive audio/network campaigns with mixed storage, charging and downstream portable loads.
- Retain compact versus rack inventories, durable cord reservations and native save identities; require real input/output and rejected-energy heat.

**Completion checks:**

- Run brownout, depleted packs/fuel, restart/reset, bypass and simultaneous channel/cord claims with remote observers.
- Verify cold-server reload, one-time refunds and conservative power through chained sources and outages.

**Source entry points:** [BatteryElement.kt](https://github.com/reamnet/ELN1.21.1ForkPatch/blob/b7444a7b8a0c5e072b1fd35eed2b74a282c3348c/src/main/kotlin/mods/eln/transparentnode/battery/BatteryElement.kt), [OffGridElement.kt](https://github.com/reamnet/ELN1.21.1ForkPatch/blob/b7444a7b8a0c5e072b1fd35eed2b74a282c3348c/src/main/kotlin/mods/eln/transparentnode/offgrid/OffGridElement.kt), [BackupElements.kt](https://github.com/reamnet/ELN1.21.1ForkPatch/blob/b7444a7b8a0c5e072b1fd35eed2b74a282c3348c/src/main/kotlin/mods/eln/transparentnode/standby/BackupElements.kt), [CompactUpsElement.kt](https://github.com/reamnet/ELN1.21.1ForkPatch/blob/b7444a7b8a0c5e072b1fd35eed2b74a282c3348c/src/main/kotlin/mods/eln/transparentnode/ups/CompactUpsElement.kt). Filter `storage` in [the full source audit](completion-source-audit.tsv) for every assigned file.

**Existing review/evidence:** [compact-ups.md](compact-ups.md), [rated-lamp-supplies.md](rated-lamp-supplies.md), [standby-checkpoint-validation.json](standby-checkpoint-validation.json). Historical counts in those reports apply to their recorded commits.

<a id="classic"></a>
## Inherited devices, sensing, control and world interactions

**Assessment:** behavior-review. **Prerequisites:** solver, mounting, network.

**Reuse:** Native classic six-node/simple/transparent devices and their passing circuit/voxel regressions.

**Needed:**

- Review every remaining shared device in the source audit: sensors, meters/loggers, relays/timers, logic/math/chips, redstone/wireless, charger, antennas, turret, miner, transporter, signs and other classic machines.
- Compare electrical/thermal thresholds, inventory/filter semantics, packet bounds, process timers, world effects, damage and configuration/save defaults—not just class names or rendered meshes.
- Add missing source-specific capabilities only through native ports and lifecycle rules; record intentional voltage/name/cable-slot differences.

**Completion checks:**

- For each reviewed device record source contract, native counterpart, actual behavior tests and open visual/interaction gaps.
- Exercise real world/crop/entity effects and native voxel hosting, automation, removal and reload without loosening old checks.

**Source entry points:** [SwitchgearKind.kt](https://github.com/reamnet/ELN1.21.1ForkPatch/blob/b7444a7b8a0c5e072b1fd35eed2b74a282c3348c/src/main/kotlin/mods/eln/transparentnode/switchgear/SwitchgearKind.kt), [TreeResinCollectorElement.java](https://github.com/reamnet/ELN1.21.1ForkPatch/blob/b7444a7b8a0c5e072b1fd35eed2b74a282c3348c/src/main/java/mods/eln/sixnode/TreeResinCollector/TreeResinCollectorElement.java), [BatteryChargerElement.java](https://github.com/reamnet/ELN1.21.1ForkPatch/blob/b7444a7b8a0c5e072b1fd35eed2b74a282c3348c/src/main/java/mods/eln/sixnode/batterycharger/BatteryChargerElement.java), [DiodeElement.java](https://github.com/reamnet/ELN1.21.1ForkPatch/blob/b7444a7b8a0c5e072b1fd35eed2b74a282c3348c/src/main/java/mods/eln/sixnode/diode/DiodeElement.java). Filter `classic` in [the full source audit](completion-source-audit.tsv) for every assigned file.

**Existing review/evidence:** [ghost-consumers.md](ghost-consumers.md), [extended-consumers.md](extended-consumers.md), [gui-parity.md](gui-parity.md). Historical counts in those reports apply to their recorded commits.

<a id="entities"></a>
## Railroad and replicator/entity parity

**Assessment:** behavior-review. **Prerequisites:** solver, network, rendering.

**Reuse:** Native entities, minecart/rail interfaces and source entity assets/behavior for comparison.

**Needed:**

- Review electric minecart powered simulation, under-track/overhead supply, charger/reporting and fuel/energy/save behavior; adapt missing source changes.
- Review replicator AI, attacks, hunger, spawn attributes, drops and tracking. The unused source ReplicatorModel is excluded and is not a required new mesh.
- Check entity registration/serialization, chunk/dimension travel, interaction, damage and render/sound authority.

**Completion checks:**

- Run actual entities over powered/unpowered rail sections and through save/server restarts; verify energy and item ownership.
- Test AI/drops and passenger/entity synchronization on a dedicated server with observer clients.

**Source entry points:** [ConfigurableAiWander.kt](https://github.com/reamnet/ELN1.21.1ForkPatch/blob/b7444a7b8a0c5e072b1fd35eed2b74a282c3348c/src/main/kotlin/mods/eln/entity/ConfigurableAiWander.kt), [ReplicatorCableAI.kt](https://github.com/reamnet/ELN1.21.1ForkPatch/blob/b7444a7b8a0c5e072b1fd35eed2b74a282c3348c/src/main/kotlin/mods/eln/entity/ReplicatorCableAI.kt), [ReplicatorEntity.kt](https://github.com/reamnet/ELN1.21.1ForkPatch/blob/b7444a7b8a0c5e072b1fd35eed2b74a282c3348c/src/main/kotlin/mods/eln/entity/ReplicatorEntity.kt), [ReplicatorHungryAttack.kt](https://github.com/reamnet/ELN1.21.1ForkPatch/blob/b7444a7b8a0c5e072b1fd35eed2b74a282c3348c/src/main/kotlin/mods/eln/entity/ReplicatorHungryAttack.kt). Filter `entities` in [the full source audit](completion-source-audit.tsv) for every assigned file.

<a id="tools"></a>
## Tools, copy/configuration, scanners and circuit import

**Assessment:** behavior-review. **Prerequisites:** registry, network, classic.

**Reuse:** Native tools, Wire Snips, filters, configuration objects and diagnostic helpers.

**Needed:**

- Review drills, meters/thermometers, mining/ore tools, brushes, configuration copier and moving helpers against source inventory/component rules.
- Port or reconcile Falstad importer/netlist/tool, command feedback and supported circuit mappings; source names alone do not prove a native equivalent.
- Include safe main/off-hand and remote-use authority, retained condition/paid materials, invalid settings and source guide acquisition.

**Completion checks:**

- Use survival/main/off-hand, loaded/unloaded targets, invalid files/settings and native/source saved tool data.
- Check exact configuration round trips, item wear/returns and bounded circuit import with clear unsupported-element feedback.

**Source entry points:** [HeatingCorpElement.java](https://github.com/reamnet/ELN1.21.1ForkPatch/blob/b7444a7b8a0c5e072b1fd35eed2b74a282c3348c/src/main/java/mods/eln/item/HeatingCorpElement.java), [ThermalIsolatorElement.kt](https://github.com/reamnet/ELN1.21.1ForkPatch/blob/b7444a7b8a0c5e072b1fd35eed2b74a282c3348c/src/main/kotlin/mods/eln/item/ThermalIsolatorElement.kt), [OreScannerConfigElement.kt](https://github.com/reamnet/ELN1.21.1ForkPatch/blob/b7444a7b8a0c5e072b1fd35eed2b74a282c3348c/src/main/kotlin/mods/eln/item/electricalitem/OreScannerConfigElement.kt), [ConfigCopyToolDescriptor.java](https://github.com/reamnet/ELN1.21.1ForkPatch/blob/b7444a7b8a0c5e072b1fd35eed2b74a282c3348c/src/main/java/mods/eln/item/ConfigCopyToolDescriptor.java). Filter `tools` in [the full source audit](completion-source-audit.tsv) for every assigned file.

**Existing review/evidence:** [wire-snips.md](wire-snips.md). Historical counts in those reports apply to their recorded commits.

<a id="network"></a>
## Packets, server lifecycle and input authority

**Assessment:** behavior-review. **Prerequisites:** registry, mounting.

**Reuse:** Native typed packets, bounded device controls, owner publication and build-commit handshake.

**Needed:**

- Account for every active source packet/handler/server event and its destination equivalent, including key controls, tooltips, entity controls, configuration and periodic publication.
- Validate sender/menu/owner/dimension/range, payload lengths, finite values and packet versioning. Keep client classes out of dedicated-server initialization.
- Preserve disconnect cleanup, loaded installations, delayed tasks, exact packet consumption and no retained stale worlds or duplicated owner tokens.

**Completion checks:**

- Fuzz relevant controls without changing production tolerances; run different owners, stale menus, logout and chunk/dimension transitions.
- Use at least driver/operator and observer clients for timing/authority cases; keep exact native publication/reload tests.

**Source entry points:** [DelayedTaskManager.kt](https://github.com/reamnet/ELN1.21.1ForkPatch/blob/b7444a7b8a0c5e072b1fd35eed2b74a282c3348c/src/main/kotlin/mods/eln/server/DelayedTaskManager.kt), [PacketHandler.kt](https://github.com/reamnet/ELN1.21.1ForkPatch/blob/b7444a7b8a0c5e072b1fd35eed2b74a282c3348c/src/main/kotlin/mods/eln/PacketHandler.kt), [ServerKeyHandler.kt](https://github.com/reamnet/ELN1.21.1ForkPatch/blob/b7444a7b8a0c5e072b1fd35eed2b74a282c3348c/src/main/kotlin/mods/eln/ServerKeyHandler.kt), [ElnFMLEventsHandler.kt](https://github.com/reamnet/ELN1.21.1ForkPatch/blob/b7444a7b8a0c5e072b1fd35eed2b74a282c3348c/src/main/kotlin/mods/eln/eventhandlers/ElnFMLEventsHandler.kt). Filter `network` in [the full source audit](completion-source-audit.tsv) for every assigned file.

**Existing review/evidence:** [decorative-lighting-saved-owners.json](decorative-lighting-saved-owners.json), [cord-outlets-validation.json](cord-outlets-validation.json). Historical counts in those reports apply to their recorded commits.

<a id="rendering"></a>
## Rendering, covers, distant models and graphics compatibility

**Assessment:** partial. **Prerequisites:** mounting, network.

**Reuse:** Native GL recording shim, item/node render factories, cached catenaries and loaded tall-pole bounds.

**Needed:**

- Review source terrain batching/atlases, packed emission, covers/selection, moving/status parts, transparency and per-owner bounds against native 26.3 rendering.
- Treat distant-model streaming/retention and source Sodium/Distant Horizons hooks as platform-specific work. Loaded-pole bounds do not implement unloaded/distant streaming.
- Preserve source mesh/material provenance while checking normals, colors, light/emission, windows, clipping and resource-pack overrides; geometry emission alone is insufficient.

**Completion checks:**

- Capture matching source/native views for each family, lit/unlit and active/inactive, through resource reload and fresh clients.
- Check hardware GPU, alternative renderer/mod absent/present, distant loaded/unloaded owners, multi-client retention and draw/mesh allocation costs.

**Source entry points:** [ElnRenderTypes.java](https://github.com/reamnet/ELN1.21.1ForkPatch/blob/b7444a7b8a0c5e072b1fd35eed2b74a282c3348c/src/main/java/mods/eln/client/gl/ElnRenderTypes.java), [UuidManager.kt](https://github.com/reamnet/ELN1.21.1ForkPatch/blob/b7444a7b8a0c5e072b1fd35eed2b74a282c3348c/src/main/kotlin/mods/eln/client/UuidManager.kt), [LoopedSoundManager.kt](https://github.com/reamnet/ELN1.21.1ForkPatch/blob/b7444a7b8a0c5e072b1fd35eed2b74a282c3348c/src/main/kotlin/mods/eln/sound/LoopedSoundManager.kt), [ClientGuiHandler.java](https://github.com/reamnet/ELN1.21.1ForkPatch/blob/b7444a7b8a0c5e072b1fd35eed2b74a282c3348c/src/main/java/mods/eln/client/ClientGuiHandler.java). Filter `rendering` in [the full source audit](completion-source-audit.tsv) for every assigned file.

**Existing review/evidence:** [power-poles.md](power-poles.md), [gui-parity.md](gui-parity.md), [decorative-lighting.md](decorative-lighting.md). Historical counts in those reports apply to their recorded commits.

<a id="gui"></a>
## Source-matching menus and user interaction

**Assessment:** partial. **Prerequisites:** registry, network.

**Reuse:** Native MenuSpecs/ElnScreen/layouts, 13 classic matched layout variants and first decorative menus.

**Needed:**

- Create a full source-to-native screen/variant list, including dynamic aim/fan/suspension, full cabinets, outlets, manufacturing, panels, vehicles and inherited classics.
- Match panel dimensions, slots/filters/limits, control rectangles, visible/enabled states, keyboard/mouse behavior, tooltips and labels; retain intentional native cable-slot differences.
- Use mutually exclusive control regions where the source overlaps hidden widgets. Preserve overlap/bounds validation and use source/reference captures for actual pixel comparison.

**Completion checks:**

- Check every registered menu on server/client, shift-click/automation, invalid controls, GUI scales, translation lengths and fresh reload.
- Capture side-by-side 1.21.1/26.3 views and record coordinates versus unresolved skin/font differences without claiming universal pixel parity.

**Source entry points:** [GuiHandler.java](https://github.com/reamnet/ELN1.21.1ForkPatch/blob/b7444a7b8a0c5e072b1fd35eed2b74a282c3348c/src/main/java/mods/eln/GuiHandler.java), [Gui.java](https://github.com/reamnet/ELN1.21.1ForkPatch/blob/b7444a7b8a0c5e072b1fd35eed2b74a282c3348c/src/main/java/mods/eln/gui/Gui.java), [GuiButtonEln.java](https://github.com/reamnet/ELN1.21.1ForkPatch/blob/b7444a7b8a0c5e072b1fd35eed2b74a282c3348c/src/main/java/mods/eln/gui/GuiButtonEln.java), [GuiCheckbox.java](https://github.com/reamnet/ELN1.21.1ForkPatch/blob/b7444a7b8a0c5e072b1fd35eed2b74a282c3348c/src/main/java/mods/eln/gui/GuiCheckbox.java). Filter `gui` in [the full source audit](completion-source-audit.tsv) for every assigned file.

**Existing review/evidence:** [gui-parity.md](gui-parity.md), [decorative-lighting.md](decorative-lighting.md). Historical counts in those reports apply to their recorded commits.

<a id="resources"></a>
## Crafting, materials, assets, sound and translations

**Assessment:** partial. **Prerequisites:** registry.

**Reuse:** Existing native datagen, paid recipe aliases, resource/model indexes and import provenance ledger.

**Needed:**

- Audit raw resources, all 2,496 source generated runtime data files and actual recipe/registration references. Regenerate native equivalents from reviewed inputs; do not copy a generated source catalog over native identities. Port required models/materials/textures/icons/atlases, blockstates, particles, sounds/subtitles, tags, loot and data; keep unused assets as review items rather than automatic required registrations.
- Review full acquisition/manufacturing chains, named/dynamic recipes, energy/fuel/material costs and native counterpart aliases. Register food/chemical/fluid/material content and retain recipe compatibility.
- Wrap new user strings, regenerate native language data and update model/item indexes. Review translations beyond English and optional JEI/recipe-book behavior.

**Completion checks:**

- Every referenced runtime asset resolves from the packaged JAR; verify each sound event and translated menu at runtime.
- Datagen repeats with zero writes; survival recipes/loot/automation preserve paid inputs and existing outputs. Keep the provenance audit honest about exact versus adapted copies.

**Source entry points:** [CraftingRecipes.kt](https://github.com/reamnet/ELN1.21.1ForkPatch/blob/b7444a7b8a0c5e072b1fd35eed2b74a282c3348c/src/main/kotlin/mods/eln/craft/CraftingRecipes.kt), [RecipeBook.kt](https://github.com/reamnet/ELN1.21.1ForkPatch/blob/b7444a7b8a0c5e072b1fd35eed2b74a282c3348c/src/main/kotlin/mods/eln/craft/RecipeBook.kt), [ElnDataGenerators.kt](https://github.com/reamnet/ELN1.21.1ForkPatch/blob/b7444a7b8a0c5e072b1fd35eed2b74a282c3348c/src/main/kotlin/mods/eln/datagen/ElnDataGenerators.kt), [ElnDataProviders.kt](https://github.com/reamnet/ELN1.21.1ForkPatch/blob/b7444a7b8a0c5e072b1fd35eed2b74a282c3348c/src/main/kotlin/mods/eln/datagen/ElnDataProviders.kt). Filter `resources` in [the full source audit](completion-source-audit.tsv) for every assigned file.

**Existing review/evidence:** [imports.json](imports.json), [import-audit.json](import-audit.json), [wire-manufacturing.md](wire-manufacturing.md). Historical counts in those reports apply to their recorded commits.

<a id="world-content"></a>
## World generation, progression, tutorials and guides

**Assessment:** behavior-review. **Prerequisites:** registry, resources, classic.

**Reuse:** Native world/content registration and guide/tutorial mechanisms, plus source ore and wiki material.

**Needed:**

- Review ore biome modifiers, generation/config defaults, deposits/terrain/block items, achievements and acquisition progression against the source.
- Reconcile wiki/search/recipe/circuit-lesson pages and tutorial signs with actual native device names, recipes, voltages and controls; adapt missing discovery/key interactions.
- Check existing versus newly generated chunks and optional guide/recipe integration without moving old world identities.

**Completion checks:**

- Generate controlled fresh worlds and verify ore distribution/config paths; prove existing chunks are not silently regenerated.
- Follow a survival starter-to-machine progression and validate guide search, displayed recipes, lessons and unlock triggers.

**Source entry points:** [OreDescriptor.kt](https://github.com/reamnet/ELN1.21.1ForkPatch/blob/b7444a7b8a0c5e072b1fd35eed2b74a282c3348c/src/main/kotlin/mods/eln/ore/OreDescriptor.kt), [OreScannerManager.kt](https://github.com/reamnet/ELN1.21.1ForkPatch/blob/b7444a7b8a0c5e072b1fd35eed2b74a282c3348c/src/main/kotlin/mods/eln/ore/OreScannerManager.kt), [Data.java](https://github.com/reamnet/ELN1.21.1ForkPatch/blob/b7444a7b8a0c5e072b1fd35eed2b74a282c3348c/src/main/java/mods/eln/wiki/Data.java), [Default.java](https://github.com/reamnet/ELN1.21.1ForkPatch/blob/b7444a7b8a0c5e072b1fd35eed2b74a282c3348c/src/main/java/mods/eln/wiki/Default.java). Filter `world-content` in [the full source audit](completion-source-audit.tsv) for every assigned file.

<a id="integrations"></a>
## MQTT, Modbus, computer/mechanical bridges and addon API

**Assessment:** conditional-review. **Prerequisites:** network, panel, registry.

**Reuse:** Native Modbus files and existing optional bridges where registered; file presence is not an enabled integration.

**Needed:**

- Inventory source MQTT managers/meters/signals, Modbus RTU/RS485/serial tools, CC:Tweaked, OpenComputers Rebooted, Create, ElectroEnergetics, Jade/Waila and media-provider API contracts.
- Determine which source features are active/config-gated, which native bridges are registered, and which excluded legacy exporters/scaffolding should stay excluded. Port supported contracts using 26.3-compatible APIs and main-thread/world ownership rules.
- Handle optional dependencies absent/present, broker/serial disconnects, bounded queues, reconnect/subscription cleanup and no duplicate demand. Keep EAMedia jukebox/speakers/Plex/web as a separate addon scope.

**Completion checks:**

- Record a support/availability decision and concrete smoke test for each integration; do not silently enable development or build-excluded code.
- Run dedicated-server startup without optional mods, then protocol/control and reconnect tests with each selected dependency. No source-era dependency pin is assumed compatible.

**Source entry points:** [MqttTypes.kt](https://github.com/reamnet/ELN1.21.1ForkPatch/blob/b7444a7b8a0c5e072b1fd35eed2b74a282c3348c/src/main/kotlin/mods/eln/mqtt/MqttTypes.kt), [ModbusRtuElement.java](https://github.com/reamnet/ELN1.21.1ForkPatch/blob/b7444a7b8a0c5e072b1fd35eed2b74a282c3348c/src/main/java/mods/eln/sixnode/modbusrtu/ModbusRtuElement.java), [Rs485CableElement.java](https://github.com/reamnet/ELN1.21.1ForkPatch/blob/b7444a7b8a0c5e072b1fd35eed2b74a282c3348c/src/main/java/mods/eln/sixnode/rs485cable/Rs485CableElement.java), [ComputerCraftIoElement.java](https://github.com/reamnet/ELN1.21.1ForkPatch/blob/b7444a7b8a0c5e072b1fd35eed2b74a282c3348c/src/main/java/mods/eln/transparentnode/computercraftio/ComputerCraftIoElement.java). Filter `integrations` in [the full source audit](completion-source-audit.tsv) for every assigned file.

**Existing review/evidence:** [appliance-cord-review.md](appliance-cord-review.md). Historical counts in those reports apply to their recorded commits.

<a id="verification"></a>
## Source oracles, whole-world performance and release verification

**Assessment:** validation-needed. **Prerequisites:** registry, network, rendering, gui.

**Reuse:** 690 passing root tests (three existing optional previews skipped), 15 CAD tests, 846 ForkPatch native cases and historical golden evidence at the lighting checkpoint.

**Needed:**

- Map all source unit/property/devtest campaigns to native equivalents. Keep independent source oracles for food/climate, driving, optics, power, acquisition, configuration and interactions; do not replace expectations with implementation output. Review source workflows/scripts, metrics/profiler diagnostics and documentation as independent campaign/export references; their presence does not mean they execute on 26.3.
- Review the two historical golden failures explicitly: 537 assertions at the current code checkpoint, including preserved older differences and the new bulb acceptance changes.
- Run representative loaded source/native worlds with arrays, converter banks, wiring, mechanics/fluids and appliances; compare tick percentiles, matrix rebuilds, allocations, caches and unload behavior.
- Finish hardware GPU/audio, simultaneous multi-client, optional-mod/resource-pack, clean-JAR and release-install campaigns. Keep logs, exact code marker/checksum and stated limitations.

**Completion checks:**

- All intended-scope source cases have a native result or an explicit open/conditional record; old tests, tolerances, goldens and skips remain intact.
- Every release claim links to actual evidence; package/install/upgrade checks use the intended release scope, not just a successful compile or a mesh count.

**Source entry points:** [AllMenusClientChecks.java](https://github.com/reamnet/ELN1.21.1ForkPatch/blob/b7444a7b8a0c5e072b1fd35eed2b74a282c3348c/src/main/java/mods/eln/devtest/AllMenusClientChecks.java), [ClientDevHooks.java](https://github.com/reamnet/ELN1.21.1ForkPatch/blob/b7444a7b8a0c5e072b1fd35eed2b74a282c3348c/src/main/java/mods/eln/devtest/ClientDevHooks.java), [ClientSmokeTest.java](https://github.com/reamnet/ELN1.21.1ForkPatch/blob/b7444a7b8a0c5e072b1fd35eed2b74a282c3348c/src/main/java/mods/eln/devtest/ClientSmokeTest.java), [DevHooks.java](https://github.com/reamnet/ELN1.21.1ForkPatch/blob/b7444a7b8a0c5e072b1fd35eed2b74a282c3348c/src/main/java/mods/eln/devtest/DevHooks.java). Filter `verification` in [the full source audit](completion-source-audit.tsv) for every assigned file.

**Existing review/evidence:** [decorative-lighting-validation.json](decorative-lighting-validation.json), [decorative-lighting-golden-delta.json](decorative-lighting-golden-delta.json), [native-suite-review.md](native-suite-review.md). Historical counts in those reports apply to their recorded commits.

<a id="migration"></a>
## Full 1.21.1 world/item/entity conversion

**Assessment:** implementation-needed. **Prerequisites:** registry, mounting, network.

**Reuse:** Explicit native identity conflict helpers and native owner round trips; neither is a general world converter.

**Needed:**

- Design an opt-in, versioned source-world conversion with backups/dry run and an origin marker so native saves are never reinterpreted as source data.
- Map blocks/state properties, descriptors/items/components, node graphs, inventories, entities/passengers/cargo, ghosts, circuits, paid wire/cord ledgers, channels, cells, temperatures and config schema.
- Handle duplicate names/IDs, half-built installations, hidden legacy hardware, absent optional mods and partial/unloaded regions; preserve ownership and avoid double refunds.
- Export a source fixture/world corpus and compare topology, contents, settings, behavior and idempotence after conversion and repeated fresh-server loads.

**Completion checks:**

- Native worlds stay unchanged; dry-run reports every unresolved mapping before writes and repeated conversion is safe.
- Converted source scenes retain exact paid materials/energy state and perform equivalent work with validated source-to-native schema transforms.

**Existing review/evidence:** [registry-conflicts.md](registry-conflicts.md), [ghost-migration-review.md](ghost-migration-review.md), [power-electronics-review.md](power-electronics-review.md). Historical counts in those reports apply to their recorded commits.

<a id="build-compat"></a>
## Bootstrap, platform hooks, configs and compatibility mixins

**Assessment:** behavior-review. **Prerequisites:** registry, network.

**Reuse:** Pinned native Gradle/NeoForge setup, native config/handshake and full local JDK 21/25 installations.

**Needed:**

- Review source entry points, JSON configuration defaults/migration, loader metadata, capability/attribute providers, optional feature gates and source mixin/access contracts.
- Audit Sodium/Distant Horizons, covered-cable weather and refrigerator-spoilage mixins against actual 26.3 target classes. Source mixin JSON or a source build succeeding is not compatibility proof.
- Keep project Gradle/wrapper/dependency pins unchanged for this planning task. Any future dependency/platform change needs a concrete compatibility proposal and the applicable authorization.

**Completion checks:**

- Dedicated and client startup are side-safe with every optional feature absent; selected present configurations pass actual runtime checks.
- Configuration defaults, migrations, handshake/build identity and failure behavior are documented and tested without suppressing linkage errors.

**Source entry points:** [CoveredCableWeatherMixin.java](https://github.com/reamnet/ELN1.21.1ForkPatch/blob/b7444a7b8a0c5e072b1fd35eed2b74a282c3348c/src/main/java/mods/eln/compat/mixin/CoveredCableWeatherMixin.java), [DistantHorizonsNodeMixin.java](https://github.com/reamnet/ELN1.21.1ForkPatch/blob/b7444a7b8a0c5e072b1fd35eed2b74a282c3348c/src/main/java/mods/eln/compat/mixin/DistantHorizonsNodeMixin.java), [FfSpoilageRefrigeratorMixin.java](https://github.com/reamnet/ELN1.21.1ForkPatch/blob/b7444a7b8a0c5e072b1fd35eed2b74a282c3348c/src/main/java/mods/eln/compat/mixin/FfSpoilageRefrigeratorMixin.java), [FreshnessRefrigeratorMixin.java](https://github.com/reamnet/ELN1.21.1ForkPatch/blob/b7444a7b8a0c5e072b1fd35eed2b74a282c3348c/src/main/java/mods/eln/compat/mixin/FreshnessRefrigeratorMixin.java). Filter `build-compat` in [the full source audit](completion-source-audit.tsv) for every assigned file.

<a id="review-remainder"></a>
## Remaining shared support code and explicit review queue

**Assessment:** behavior-review. **Prerequisites:** registry, solver.

**Reuse:** Current native support classes, helpers and all file-level source/audit links.

**Needed:**

- Review every file assigned here in completion-source-audit.tsv, including miscellaneous common helpers, math/data/config contracts and any source item or framework that lacks a narrower adapter.
- For each item record the actual native equivalent or an intentional exclusion, the behavioral delta and the required test. Renamed/split/combined classes need an explicit equivalence mapping.
- Move newly identified feature work into a named workstream with dependencies and acceptance checks. An audit assignment or import-ledger row never marks a file behaviorally complete.

**Completion checks:**

- No active source file disappears from the review surface; every remaining unknown has an actionable open review entry.
- A future completion claim requires evidence for shared and byte-identical classes too, not just an empty list of missing names.

**Source entry points:** [LiveDataManager.kt](https://github.com/reamnet/ELN1.21.1ForkPatch/blob/b7444a7b8a0c5e072b1fd35eed2b74a282c3348c/src/main/kotlin/mods/eln/misc/LiveDataManager.kt), [Obj3D.java](https://github.com/reamnet/ELN1.21.1ForkPatch/blob/b7444a7b8a0c5e072b1fd35eed2b74a282c3348c/src/main/java/mods/eln/misc/Obj3D.java), [Obj3DFolder.java](https://github.com/reamnet/ELN1.21.1ForkPatch/blob/b7444a7b8a0c5e072b1fd35eed2b74a282c3348c/src/main/java/mods/eln/misc/Obj3DFolder.java), [MetricsSubsystem.kt](https://github.com/reamnet/ELN1.21.1ForkPatch/blob/b7444a7b8a0c5e072b1fd35eed2b74a282c3348c/src/main/kotlin/mods/eln/metrics/MetricsSubsystem.kt). Filter `review-remainder` in [the full source audit](completion-source-audit.tsv) for every assigned file.

**Existing review/evidence:** [source-inventory.json](source-inventory.json), [imports.json](imports.json). Historical counts in those reports apply to their recorded commits.

## Handoff and evidence requirements

For every implementation batch record: pinned source files/hashes, source behavior and native counterpart, explicit identity/recipe decisions, changed simulation/ownership/render/menu paths, tests with independent expected results, runtime captures and remaining limitations. Keep old native IDs, goldens, tolerances and skips intact. Preserve exact component/condition, energy/heat/material ledgers, cached matrices and resident-only queries.

Use the established full JDKs outside tracked files: JDK 21 for the pinned source and JDK 25 for the target. Keep both projects’ pinned Gradle configuration. Do not compile changed main classes while a client/test JVM reads the same build outputs. Commit code first, assemble from that clean code commit, verify packaged class/resource bytes and record its marker/checksum in an evidence-only follow-up.

Runtime evidence is versioned in [the lighting checkpoint](decorative-lighting-validation.json) and [the exact historical golden delta](decorative-lighting-golden-delta.json). Those reports apply only to their recorded source fingerprints/commits; newly indexed adapters remain validation-pending until independently recorded. This documentation audit does not rerun or extend runtime claims.

Regenerate the indexes after a feature batch and review status manually; the generator never marks a stream complete:

```sh
python3 tools/port/plan_completion.py --source ../ELN1.21.1ForkPatch
```

Generated data, build/tool scripts and references in the support index still require the explicit tasks above; that file coverage does not turn them all into runtime port requirements. Optional dependency availability, external EAMedia code and source-world fixture exports require separate review. Hardware tests and behavioral review can reveal further deltas; those remain accountable work rather than an unsupported claim that all unknown issues are solved.
