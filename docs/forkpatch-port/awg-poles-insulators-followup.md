# Pole connections, wire appearance and placeable insulators

Implementation and regression verification are complete on `local/awg-upstream-integration`. Clean packaging and authorized publication follow the reporting commit. Base: `b34d43a6e49d8626e85e97fdbce65679e1ebe827`. No other worktree was modified.

## Changes

- `c44ddf3e4`: bare six-node AWG descriptors referenced nonexistent `sprites/currentcable.png`. Use the existing neutral white texture and copper/aluminum tint, including junction caps. Ordinary native caps retain their old default.
- `bb2dadb8d`: port all ten original ForkPatch line/feed insulators: 800 V, 3.2 kV, 12.5 kV, 69 kV and 230 kV. Preserve original source IDs 8043–8052 and `insulatorCircuit` data. Reuse native grid links, six-face placement, model rendering, menus and recipes. Original source attribution: `b7444a7b8a0c5e072b1fd35eed2b74a282c3348c`.
- `0c9749ab5`: elevated pole reservations had empty outlines, preventing player ray hits; the secondary rack extended outside its shaft reservations. Relevant poles now use selectable, noncolliding native ghost reservations, including the service rack. Existing owned ghosts migrate on reload. Deferred repair uses loaded chunks only, server-thread scheduling, node identity checks and bounded retries; foreign blocks are preserved. Selection follows the reserved equipment envelope and can be broader than the visible mesh.
- `26a653e7b`: span rendering ignored the paid conductor and used one fixed width/texture. Publish descriptor width and bare metal tint into the existing native catenary renderer. Cache meshes by endpoints and appearance, preserving reuse for ordinary status updates. Keep the old unversioned packet reader; validate the new appearance fields. Paid stacks, electrical resistance, thermal calculations and the catenary algorithm are unchanged.

- `8fe6d56f6`: the first full run caught eight powered-lamp regressions from reserving four service-rack cells. Native mounting already rotates the rack, so reserve its single actual cell and leave lamp heads clear. Added six wooden/steel orientation cases; all existing lamp assertions remain unchanged.

- `9a10770da`: a real resident/unloaded chunk-boundary probe exposed native neighbour notifications waking chunks during migration, and a ghost being created over an air-looking preserved orphan. Add quiet native air-creation with node/orphan ownership guards; suppress neighbour/shape updates only for decorative repair. The unchanged probe then passed, as did all 117 combined scenarios.

The Twelve Circuit American Breaker Panel was already ported (ID 7988), with recipe, model and modern controls. All nine existing panel scenarios passed renewed focused verification; no duplicate panel item was added.

## Verified focused results

| Check | Result |
|---|---|
| Bare-wire root tests |35 passed |
| Actual registered AWG catalog server |4 passed, including harness; all 70 variants |
| Insulator/grid/layout root tests |51 passed |
| Insulator server |7 passed, including harness |
| Pole/panel/insulator server |23 passed, including harness |
| Appearance/AWG/grid root tests |43 passed |
| Appearance/pole/panel/insulator server |24 passed, including harness |
| Paired native appearance and four tower saved-mesh cases |6 passed, including harness |
| Corrected rack + all lamp/tower/panel/insulator/style cases |116 passed, including harness |
| Final AWG/grid/node/orphan root checks |53 passed |
| Final combined server checks |117 passed, including harness |
| New-item datagen, repeated |All 4,314 generated files byte-identical |
| Existing registry mappings |All 934 unchanged; exactly 10 insulator additions |

Insulator tests cover all 60 type/mount combinations. A twelve-metre copper 2 AWG span driving 10 kΩ measured approximately 0.080, 0.320, 1.250, 6.900 and 23.000 A at the five rated voltages, with expected native conductor voltage drop. Duplicate confirmation, reload continuity and real support removal all pass; removal refunds exactly 12 m once and disconnects the opposite endpoint.

The native span mesh test covers all 70 AWG types, checking actual emitted vertex colour, texture and proportional thickness after publication. Bare copper usesRGB `b87345`, aluminum`aeb7bd`. The smallest/largest AWG visual width ratio is 5; paired native overhead widths differ by 2. Every 14 m span refunds its original item and exact length once. Old packet layout and mesh-cache reuse are tested.

No new actual insulator chunk-eviction test was added: the new tests use node reload and support removal. Full-suite existing AWG physical chunk-eviction results must be checked below. Headless mesh emission does not constitute live graphical inspection.

## Final regression

- Complete `build :test` passed twice; final numerical run on `8fe6d56f6` took18m19s: root1,373/0 failures/6 skips, CAD18/0, fluidcore104/0/2 skips. After the server-only quiet-repair addition, all53 relevant AWG/grid/node/orphan root tests passed. Numerical, routing and cable specification paths did not change in that addition.
- Final unfiltered server on `9a10770da`: **2,125/2,128 passed**,16m03s. Gradle exit1 reflects server exit3 for exactly the historical `golden_descriptors`, `golden_slots`, `jei_every_item_reachable` failures. JEI still reports only `nibble_memory_chip`; the ten new insulators are reachable.
- All51 AWG cases and all22 newly registered cases passed; no prior case disappeared. The extra case beyond the21 explicit additions is automatic malformed-GUI-packet coverage for the insulator descriptor.
- Actual AWG lifecycle coverage logged three real target-chunk/block-entity evictions. Thermal/insulation state, reconnection and the existing lifecycle assertions passed. Insulator node reload/support-removal coverage and the new unloaded-neighbour repair probe are distinct from those physical evictions.
- Golden assertion comparison against the prior full baseline: descriptors562→572, exactly the ten new insulator IDs absent from the1.12 inventory; slots343→354, exactly their ten menus plus descriptor class lacking a1.12 container. No previous failed assertion disappeared or changed. Historical snapshots, assertions and filters are untouched.
- Final data generation passed twice,28s/23s; all4,314 tree files including cache indexes are byte-identical (4,310 generated outputs). All934 existing mappings remain identical, exactly10 source insulator mappings were added. Electrical/thermal specifications, historical IDs and dependency pins are unchanged.

Logs: `/tmp/awg-poles-insulators-final-build.log`, `/tmp/awg-poles-insulators-final-game.log`, `/tmp/awg-ghost-repair-unit.log`, `/tmp/awg-ghost-repair-combined-game.log`, `/tmp/awg-poles-insulators-final-data{1,2}.log`. The earlier incomplete server run was stopped after identifying the lamp regression; its results are not presented as a completed baseline.

## Delivery

Fresh artifact: `build/libs/eln-26.3-0.1.0-port.jar`; authoritative SHA-256 companion: `build/libs/eln-26.3-0.1.0-port.jar.sha256`. Assembly must follow a clean reporting commit because `eln-build.properties` includes the Git commit used for client/server version matching. Verify packaged main classes and the new resources against build outputs before pushing.

Publication is an ordinary fast-forward to `origin/local/awg-upstream-integration`, already authorized by the user. No merge into `feature/awg-voxel-cables`, `develop` or `main` is performed. The progress checkpoint records completion of the push and final metadata-only artifact rebuild if needed. All implementation commits are listed above; there is no implementation WIP.

Remaining acceptance work is live Minecraft visual/manual verification and the three documented historical failures. No graphical rendering or real multiplayer client connection is claimed from headless tests.

## Manual Minecraft checks

1. Install the final JAR on both client and server. Search for **Twelve Circuit American Breaker Panel** and the five **Line Insulator** / **Cable Feed Insulator** voltage tiers; verify their inventory models and recipes.
2. Mount each insulator on floor, wall and ceiling. For 800 V service use its menu to choose a circuit before linking. Feed types expose base cable terminals; line types carry the overhead conductor only. Remove support and verify one paid-wire refund.
3. On wooden poles and 69/230 kV poles/risers, select the intended cable terminal in the existing pole menu, then right-click the base or reachable equipment on each pole with the wire; use scaffolding when targeting high arms directly. Verify independent primary and service connections, including elevated crossarms and the offset service rack.
4. Compare thin/thick bare copper and aluminum spans and legacy six-node wires. Copper should be copper-coloured, aluminum gray; insulated wire retains its jacket. Check junction caps and look for missing-texture squares.
5. For legacy six-node placement, hold the copper/aluminum spool, open the tool menu (default **C**) and choose **Legacy six-node cable**; switch back to **Native voxel cable** for routed placement.
6. Power panel 120/240 V branches, operate their controls, trip/reset a breaker and reload. Confirm wiring and settings survive. Inspect lamp and green-preview appearance in a real client; these have not been visually certified here.
