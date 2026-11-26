# Native AWG implementation results

Started from `b0edb1264` on `local/awg-upstream-integration`. Work is confined to its isolated worktree. No Qwen worktree or unfinished geometry branch was edited or merged.

## Implemented milestones

| Commit | Change | Verified behavior |
| --- | --- | --- |
| `f61ef755c` | Native decorative-piece conversion | The same CableRouter converter can emit undecorated paid centerlines. AWG edges own native visual fills without purchasing them; both-end corner ownership supports partial-plane transitions. Optional bounded save/packet fields preserve old reads. Paired stairs/slab climbs include inside-block outer wraps and match native collision occupancy exactly. |
| `57a3e9ee0` | Native offset terminal bridge | Uses native End/Pin/FacePins metadata and CableConnections/NodeConnection lifecycle. Powered tests cover -2/0/+2 voxel pin offsets within a host and across a block boundary, save/load and reconnecting either node alone. Legacy metadata-free midpoint lookup remains scoped to its actual contract. |
| `a039d7359` | Repeated junction operations | Fourth arms and two taps/cuts in one host use atomic virtual split plans. One original edge becomes multiple children with exact paid length, heat, exposure and corner/visual ownership. Failed placement restores the original state and simulator baseline. |
| `acc2c1f6d` | Native-preferred controls | Starting native placement clears stale compatibility selection. Legacy compatibility clicks use native reach validation. Explicit old single-block/two-click controls remain to preserve functionality. |
| `958effa59` | Failure and powered regression coverage | New optional malformed saves and every truncated new geometry packet prefix fail closed. A powered fourth arm feeds three parallel loads; removing one opens only its load. |

No new search, collision, stair, slab, wrap, sag or rendering engine was added. Native Router/CableRouter, CablePart geometry, CableHang, RoutePlacer, KitPreview/KitOutline, Mimic and Laying remain the implementation. AWG continues to own descriptor physics, exact wire accounting and independently heated electrical edges.

## Measurements and focused checks

- Stair and slab transition circuits after reload: about **11.577645167 A**, paid wire resistance **0.036481070 ohm**, consistent with 12 V into a 1-ohm load plus cable resistance. Every arm agrees within 1e-6 A. Exactly **7 m** debited/refunded, with host and simulator restoration on interrupted installation.
- Offset terminal circuits: about **11.7648 A** in-host and **11.7723 A** across a boundary. Assertions include the native source's **0.01152-ohm** series resistance and actual AWG length/temperature resistance. Exact funding, reload and individual reconnect checks pass.
- Same-host parallel loop: **11.63436436084 A = 9.48090514781 A + 2.15345921303 A**, sampled in the electrical phase with 1e-6 A tolerance. Repeated cuts preserve the original one-meter ledger, **400 J** of heat and **0.125** insulation exposure.
- Four-way parallel circuit: **21.17368128110 A = 11.47781110043 A + 9.69587018067 A**. Individual arm removal preserves the original powered trunk and unrelated parallel load.
- Expanded focused server: **77/77**, including all original focused AWG/native joins/battery/shaft cases and new milestone cases. Final strengthened regressions: **5/5** including harness. Focused unit/router/packet checks: **42 passing**.

Initial failures and fixes are recorded in [the progress checkpoint](awg-native-parity-progress.md): a source-series-resistance test assumption, cross-node reconnection asymmetry fixed in the bridge, temporary fixture components not broken before reload, and a KCL measurement moved to electrical phase. No tolerance, golden snapshot or assertion was weakened.

## Full verification and artifact

Verified code commit: `958effa59`; subsequent commits contain reporting only.

| Gate | Actual result |
| --- | --- |
| `./gradlew build :test` | Exit 0, 18m24s. Root: 1,368 tests / 0 failures / 6 existing skips; CAD: 18 / 0 failures; fluidcore: 104 / 0 failures / 2 existing skips. |
| `runClientData` twice | Both exit 0 (24s / 22s). All 4,264 files byte-identical across both and the earlier integration baseline, including caches. |
| Unfiltered `runGameTestServer` | **2,102/2,105 passed**, 16m24s total; Gradle exit 1 / server exit 3 for the historical failures below. |
| AWG in unfiltered suite | All **50** AWG cases observed and passing, including all 11 new cases and all70 catalog variants. |
| Actual chunk lifecycle | All **three physical chunk eviction cycles** pass in the unfiltered run. Existing adjacent-only/shape-specific coverage limitations remain as documented in the reliability report. |
| Policy/artifact checks | All **934** existing registry mappings identical to b0edb1264; descriptor physics/ratings, existing run/length models, historical goldens, generated assets and dependency pins unchanged. JAR bytes match 32 relevant verified compiled class files. |

The only failed cases are exactly the prior historical set: `golden_descriptors`, `golden_slots`, `jei_every_item_reachable`. JEI still reports one unreachable `nibble_memory_chip` of 887 listed / 2,431 ways. No additional failure appeared. No golden snapshots or failure-hiding skips were changed.

Logs: `/tmp/awg-native-parity-full-build.log`, `/tmp/awg-native-parity-full-game.log`, `/tmp/awg-native-parity-datagen1.log`, `/tmp/awg-native-parity-datagen2.log`.

Fresh assembled mod JAR (local build artifact, not committed binary):
`build/libs/eln-26.3-0.1.0-port.jar` (38 MiB).

SHA-256: `e0cc93dbb16af38baedc64b678a46a6dec8309ae7d449ff40c88bbcb1bb0befb`.
Companion: `build/libs/eln-26.3-0.1.0-port.jar.sha256`.

No live client/GPU rendering claim is made. The development branch was successfully pushed to the fork through reporting commit b1f0cbd7e; the final reporting-only checkpoint is pushed to the same branch. The final remote tip is confirmed in the delivery response.

## Preserved contracts and limits

All 70 existing AWG identities, electrical/rating/thermal descriptor values, registry IDs and existing saved fields remain unchanged. Added geometry/second-corner fields are optional; old parts and packets remain readable. MNA ownership, zero-paid full-block knuckles, native sag samples, material refunds and inactive malformed-part preservation remain covered by existing regression cases.

Ordinary native stair/slab/partial-plane/inside-block wrap conversion is now covered by paired powered fixtures (Copper 12 AWG; all70 catalog and existing large/mixed gauge fixtures also run). This is not an exhaustive graphical test of every shape with every cross-section. Hosts containing only decoration with no paid edge to own it still fail closed; displaced sag-curve taps remain ineligible. No custom routing workaround was introduced for those limits. Explicit legacy controls remain for compatibility, while ordinary placement is native. Green preview appearance and actual client dimension/disconnect flows still need the existing manual checklist; no live client was visually inspected.

The authorized destination is `reamnet/ElectricalAge:local/awg-upstream-integration`. Main, develop and feature branches are not merged or updated. The original upstream/reliability/Arc Furnace histories remain ancestors, and source feature/reliability refs remain unchanged.
