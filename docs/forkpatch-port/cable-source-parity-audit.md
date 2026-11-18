# Copper/aluminum cable source-value audit

Reviewed port commit `2b23daf1484c6e71307ec7dfcf55bfd6970406f2` against the clean, read-only1.21.1 source at `b7444a7b8a0c5e072b1fd35eed2b74a282c3348c`. This is a verification/documentation slice; no production code, values, tests, tolerances, skips or Gradle configuration changed.

The catalog's numeric ratings match. This does **not** certify that every source cable feature is already ported. Three concrete behavior gaps are recorded below.

## Exact catalog and physics checks

| Check | Result |
|---|---|
| Single-conductor size specifications | All21 rows match: size, mm², amperage, voltage, jacket temperature and material eligibility |
| Multicore size specifications | All26 rows match: per-core area, conductor count, amperage, voltage, temperature, flat/round construction, palette reference and eligibility |
| High-voltage specifications | All10 rows match:12/2 AWG at1/5/20/40/150kV |
| Registration identities | All170 intact/bare/damaged identities match source allocator order and names; no mismatches |
| Conductor palettes | All11 families/17 palette entries have the same color arrays/order |
| Electrical model | `WirePhysics.kt` is byte-identical to the pinned source |
| Thermal/material model | `WireThermalPhysics.kt` is byte-identical to the pinned source |
| Spools | Same10kg default stock formula,128m creative spool,1m placed segment and1e-6m length epsilon; catalog default lengths match despite harmless arithmetic regrouping |
| Shapes | Reviewed registration formulas retain source single/multicore widths, flat heights and cross-section thresholds |

The source registers copper from26AWG through1000kcmil. Aluminum starts at2AWG and includes the larger sizes; it intentionally does not register every small copper gauge. There are110 single-core/HV state identities and60 multicore state identities,170 total;100 are usable non-melted variants. The two molten-metal piles are separate from those170 cables.

| Material constant | Copper | Aluminum |
|---|---:|---:|
| Resistivity at20°C, Ω·mm²/m |0.017241|0.028264|
| Temperature coefficient, /°C |0.00393|0.00403|
| Density, kg/m³ |8960|2700|
| Metal melting point, °C |1085|660|

For example,12AWG copper remains3.309mm²,20A,600V and105°C for the ordinary insulated variant; its resistance at20°C is approximately0.00521033545Ω/m. Both materials' ordinary2AWG row remains33.631mm²,100A,600V and105°C, with resistance determined by the different material resistivities. Bare variants use the source180°C descriptor limit; full metal fusion uses the separate material melting point/latent heat.

The complete size tables, source/target physics hashes, identity results and test counts are in `cable-source-parity-audit.json`. Registration source: `registration/SixNodeRegistration.kt`; native registration: `sixnode/electricalcable/UtilityCables.kt`. Electrical configuration formulas retain nominalV×A power, per-endpoint half resistance,1.5× maximum descriptor voltage and1.1× maximum-current power allowance. The utility elements use their dedicated enthalpy model rather than the source legacy cable spike limiter, which the source explicitly disables for utility cable.

## Fresh functional tests

All195 selected native cases passed with0 skips:

- 179 `eln:forkpatch_wire*` cases: catalog coverage/manufacturing across all100 usable variants, paid metal/rubber/power, roller/insulator/combiner, aluminum, HV insulation tiers, placement/spool accounting, heating, melting/faults, hot snips, saves and refunds.
- 11 `eln:forkpatch_multicore*` cases: independent cores, routing, palettes, faults, thermal transitions and persistence.
- 5 `eln:forkpatch_bundle_span*` cases: jacket/core payment, independent power/heat, save/refund, invalid bundles and creative handling.

All26 selected unit tests passed with0 skips: `ForkPatchWirePhysicsTest`5, `ForkPatchWireThermalTest`15, `ForkPatchGridCablePlanTest`6. Commands use Java25 from `/tmp/eln-port-java-env.sh`, root-only `:test` filters and real `runGameTestServer` runs with separate fresh universes. Raw reports/log paths are in the JSON. No production changes were needed for these tests.

## Remaining behavior differences

1. **Room-temperature coupling:** source `UtilityCable.kt:updateThermalProperties` adds the room thermal manager's temperature delta to surroundings. Native `UtilityCableElement.refresh` currently uses biome/ambient air for both arguments. Enclosed-room thermal feedback is unfinished.
2. **Shock after insulation failure:** source `UtilityCableFailureProcess` calls `shockNearbyPlayers` with a one-second cooldown and voltage-dependent damage. The native adapter creates/persists the real fault circuit but does not reproduce that player-shock process. Passing fault/current tests does not establish contact-damage parity.
3. **Cooling after an electrical insulation fault:** source retains insulated cooling until the descriptor is melted/replaced. Native passes `insulated && !insulationFailed`, exposing the conductor thermally as soon as the fault latches. This changes post-fault temperature evolution despite identical physical helper constants. Reconcile the intended policy and test both stages before claiming exact behavior parity.

Native temperature save-reference handling is intentionally adapted, and complete source-world migration remains a broader port requirement. No new visual/GPU, multiplayer or contact-hazard gameplay campaign was performed here. Therefore the supported conclusion is **matching cable ratings and working tested core systems, with the above integration gaps**, rather than a blanket100% identical port.
