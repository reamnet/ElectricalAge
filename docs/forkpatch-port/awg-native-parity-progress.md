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
- Focused server (all original 65 selected AWG/join/battery/shaft IDs plus new long route): exit 0, 67/67 including harness, 35s. Every requested ID observed; no failures. Log `/tmp/awg-native-parity-game.log`.
- Before-fix case reproduced native success/AWG refusal. After fix: 1040 voxel route, interrupted rollback after 35 parts preserves world/spool/simulator baseline, successful debit 65 m, refund 65 m. Existing powered AWG, three real physical chunk eviction cycles, all70 descriptor catalog and thermal/ownership/rollback tests pass in focused suite.
- Audit completed at `awg-native-parity-followup.md`: shared infrastructure, necessary checks, conversion/terminal/legacy parity gaps and shared-code next steps. No geometry feature implementation or Qwen integration.
- Registry/descriptor identities/specifications/save fields untouched. All 4264 generated hashes still match verified datagen; no asset/lang/recipe inputs changed, so datagen not rerun. Historical goldens and dependency pins untouched. `git diff --check` passes.
- Full suite was not rerun for this one-condition adapter fix; latest full baseline remains 2091/2094 with historical three failures, recorded in integration report. Final focused server includes the new fixture and all AWG cases.
- Next: commit verified fix/test/audit locally; no push or feature/develop update. Further geometry/pin/legacy-control changes require targeted ownership/provenance work documented in audit, not a replacement engine.
