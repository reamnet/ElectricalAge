# Native AWG parity follow-up

- Worktree: ElectricalAge-awg-upstream-integration, branch local/awg-upstream-integration.
- Baseline: 2c6b81655; clean at start. Latest verified integration: 2091/2094 full server (three historical failures), root 1368 with no failures; final focused 67/67. No need to repeat startup integration audit.
- Scope: reuse native infrastructure; no independent geometry engine, no Qwen worktree edits or unverified geometry integration.
- Findings: shared RoutePlacer/spec, Router/CableRouter, previews and Mimic/Laying already reused. AWG nativeLayout still imposes legacy 64 m cap unrelated to descriptor/payment or router budget. Add paired native/AWG 65 m waypoint route to prove parity and exact debit/refunds; show failure before removing only this native-path cap.
- Other audit areas: duplicate live validation vs stale preview safety, native-piece conversion/stubs/knuckles, machine pin offsets, legacy centered placement. Record ownership/compatibility reasons before changing.
- Next: add paired fixture, run it against baseline, remove verified arbitrary cap, rerun AWG/unit/shared routing checks. Preserve identities/specifications/save keys and current damage/terminal/material invariants.

## Verified gap and fix
- Fixture initial compile exit 1: adapted AABB constructor to current Vec3 API; no production change.
- Before-fix server run (`/tmp/awg-native-length-before2.log`) exit 1: shared router found 1040 voxels, native placement and persistence passed; AWG failed solely with its 64 m refusal.
- Removed only nativeLayout's inherited 64 m ceiling, retaining positive-length validation. Legacy direct helpers retain their old bounds. No routing/geometry/search/save format changes.
- Added 35-part interrupted placement rollback assertions for world emptiness, simulator registrations and unchanged spool; success debit/refund assertions for 65 m. Next focused units + AWG/shared GameTests.

## Completed verification
- Focused root `:test --tests '*Awg*Test' --tests '*KitPreviewLifecycleTest' --tests '*KitOutlineTest'`: exit 0, 39 tests / 0 failures / 0 skips, 51s. Log `/tmp/awg-native-parity-unit.log`.
- Focused server (all original 65 selected AWG/join/battery/shaft IDs plus new long route): exit 0, 67/67 including harness, 38s. Every requested ID observed; no failures. Log `/tmp/awg-native-parity-game.log`.
- Before-fix case reproduced native success/AWG refusal. After fix: 1040 voxel route, interrupted rollback after 35 parts preserves world/spool/simulator baseline, successful debit 65 m, refund 65 m. Existing powered AWG, three real physical chunk eviction cycles, all70 descriptor catalog and thermal/ownership/rollback tests pass in focused suite.
- Audit completed at `awg-native-parity-followup.md`: shared infrastructure, necessary checks, conversion/terminal/legacy parity gaps and shared-code next steps. No geometry feature implementation or Qwen integration.
- Registry/descriptor identities/specifications/save fields untouched. All 4264 generated hashes still match verified datagen; no asset/lang/recipe inputs changed, so datagen not rerun. Historical goldens and dependency pins untouched. `git diff --check` passes.
- Full suite was not rerun for this one-condition adapter fix; latest full baseline remains 2091/2094 with historical three failures, recorded in integration report. Final focused server includes the new fixture and all AWG cases.
- Completed code/audit commit: `c7cfb61610adc1087f86da998b7ecbd7163b1a15`. Working tree clean after final checkpoint commit. No push or feature/develop update. Next parity work is paired powered offset-pin coverage and shared piece provenance as documented in the audit, not a replacement engine.

## Implementation continuation from b0edb1264
- Priority 1 in progress: reuse CableRouter's existing piece converter with an undecorated-centerline option. AWG owns paid raw edges; attach native emitted visual geometry to those edges without charging decorative extensions/stubs. Extend endpoint corner ownership for two-end partial transitions. Preserve old NBT/payload reads; new optional visual/corner fields only when needed.
- Next: implement conversion/save/packet integration, paired powered stair/slab-wrap fixtures and rollback/refund checks. Then offset terminals, repeated taps, controls. No Qwen files/branches touched. Final full regression/JAR/push authorized by user after verified milestones.

## Priority 1 verified
- Shared CableRouter piece conversion now optionally omits only native decorative fills; same partition/search algorithm. AWG owns centerline resistor/heater edges, and native visual segments/fills are bound to those paid owners within their host. Native collision/mesh/preview geometry is retained without adding paid wire.
- Optional nativeVisuals/secondCornerFace NBT and bounded versioned packet extension preserve legacy reads. Both-end partial-plane corner ownership supported; no registry/descriptor changes.
- New parameterized paired fixtures: native/AWG stair climbs and slab-step partial transitions, both proving inside-block outer wraps and exact native occupied voxel sets. Powered after reload, equal arm current, temperature-dependent total resistance, exact 7 m payment/refund, interrupted host/simulator/material rollback, client-bound round trips.
- Compile exit 0. Focused units 42 / 0 failures, exit 0, 51s (`/tmp/awg-parity-decor-units.log`). First stair + existing corners/span/slab server 6/6, exit 0. Expanded all AWG/shared joins/battery/shaft + long/stair/slab/native stair server 70/70, exit 0, 38s (`/tmp/awg-parity-decor-game2.log`).
- Circuit samples stair/slab I=11.577645167 A, sum paid R=0.036481070 ohm, 12/(1+R) agreement; every arm within 1e-6 A. Refunded 7 m each. Existing actual chunk and all70 catalog fixtures remain passing.
- Next: commit this verified milestone, implement offset terminal bridge using native End/Pin/FacePins metadata and connection lifecycle; add paired powered fixtures. Then repeated taps/controls and full regression/JAR/push.

## Priority 2 in progress
- f61ef755c is the verified decorative-conversion milestone. New AWG contact bridge uses native End/Pin/FacePins metadata and native CableConnections/NodeConnection teardown; legacy midpoint lookup remains only for legacy metadata-free devices.
- Initial compile exit 1: native long[] direction helper private; shared package helper exposed instead of copying direction logic.
- First offset server run exit 1: all three offsets powered loads and per-arm current assertions passed, but fixture assumed ideal source. ElectricalSource exposes native small Rs; corrected quantitative formula to include actual pin load Rs. This is a test-model error, not evidence of an AWG physics defect.
- Current session 80742, `/tmp/awg-parity-terminal-game2.log`: all prior focused cases plus six paired powered terminal cases at -2/0/+2, in-host and across block boundary, before/after reload. Await exit before editing loaded classes.
- Next implement repeated taps through virtual split plans (same block/two cuts and fourth arm) preserving state and rollback; then legacy controls and full regression/build/push.
- Priority 2 second/third runs exposed cross-block reconnect asymmetry (fixed by refreshing AWG peers when a device reconnects, including isConnecting nodes). Network-reconnect invariant then passed. Remaining simulator crash traced to temporary fixture resistors removed without breakConnection before reload; fixture cleanup now removes and breaks all external components, including decorative fixture source/load.
- Current terminal verification session 56959, `/tmp/awg-parity-terminal-game4.log`; do not compile/edit classes until exit. No unverified terminal commit yet.

## Priority 2 verified
- Expanded focused server 74/74, exit 0, 53s (`/tmp/awg-parity-terminal-game4.log`): six paired terminal offsets in-host and across a block boundary, reload and each-node reconnect invariance, plus prior AWG/native decorative/join/battery/shaft fixtures. No crash or new failures.
- Offset current about 11.7648 A in-host / 11.7723 A across, quantitatively includes native source Rs=0.01152 ohm and descriptor-specific paid wire R; per-arm current equal within 1e-6 A. Exact wire debit and state/geometry round trips pass.
- Next: commit terminal bridge and fixture cleanup; implement repeated native taps and multi-cut atomic split plans.
