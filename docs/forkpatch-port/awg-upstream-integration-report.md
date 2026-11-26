# AWG upstream integration review

## Sources and disposition

Integration branch: `local/awg-upstream-integration`, isolated worktree `ElectricalAge-awg-upstream-integration`.

- Original developer `Electrical-Age/ElectricalAge:develop`, fetched November 25, 2026: `2be56a18c59d1dd129bdb81d7ebee32ac0eb4c71`.
- LOCAL feature base (not remote-tracking branch): `7b91ee88416f5e4410a8e58eff744cdced523342`.
- Reliability history merged intact: `1e2e7291d`, `182adb885`, `11a7839cd`, `2817ed508`, `75468da24`, `573af9324`.
- Shared ancestor: `29b2d2200f4c8bd301493d3f541abb5faaf07c56`. All 229 upstream commits retained by ancestry merge; no history rewrite or Qwen branch integration.

The [229-row integration matrix](awg-upstream-integration-matrix.tsv) records every upstream commit, affected paths, disposition and technical reason. The [progress log](awg-upstream-integration-progress.md) records commands, exit codes and checkpoints.

## Incorporated changes and conflict resolutions

- Fluid persistence healing, finite-value/mass/energy guards, tank sharing/layer/volume accounting, deleted junction contents, background jobs, budget/quarantine/debt/heat bookkeeping and diagnostics integrated with upstream checks.
- Canonical RoutePlacer/Spec sessions and JoinRun cable/pipe overlap rules integrated. New AwgRouteSpec carries the existing AWG native router, taps, cached route validation, exact spool identity and atomic self-payment into that controller. Generic kit payment cannot charge AWG twice. Existing transactional Laying rollback and preexisting-host restoration retained.
- Unique preview generations, stale-worker checks, server dimension invalidation, client teardown cleanup and malformed preview packet validation preserved in the shared worker path. Live AWG layout/payment validation stays on the server thread.
- Lead-acid battery float/absorption/gassing curves and finite saved-state reads integrated. Shaft component/winding/blade recipes and shared plate tags integrated with regenerated resources.
- Computer/Dynamo/Wire2/screen lifecycle fixes, shared rendering primitives, unit/message improvements, ground radars/goggles, still pot, condenser coil and breather changes integrated.
- Grid line-of-sight and render bounds adapted to the port's published multi-conductor geometry and terminal/channel rules, preserving caches and custom cable policy. Restored upstream pole-ghost cable-kit guidance in a small follow-up, with refusal/material/no-link assertions.
- ForkPatch renderers, custom work-light/rated-supply fallback icons, cord handling, recessed outlet removal/restoration and ghost cleanup retained. Variable inductor rendering uses upstream's shared primitive base. Stronger existing I-V solar assertions retained.
- All 928 original registry mappings remain unchanged. Six deliberate new entries added. New radar goggles use free slot 7742 because upstream 7740 collides with existing Motor Cowling (7741 is Heavy Winding). All 70 AWG variants retain identities/specifications; catalog and powered tests pass. Arc Furnace completion remains an ancestor and all 25 runtime cases pass.

## Deliberate exclusions

- Upstream retirement of legacy heat turbines is excluded: the port has functional published devices and saved worlds. IDs 257/264, models, sounds, renderers, powered fixture and JEI hiding remain supported. Upstream tests requiring removal are inapplicable to this explicit compatibility policy; the retained powered test passes.
- Optional IPN compile-only dependency/annotation deferred under the pinned-dependency restriction. Independent screen/UI fixes from that commit remain integrated. No Gradle/wrapper/dependency version pins changed.
- No unfinished Qwen geometry changes merged. No AWG routing redesign or new cable types.

## Verification

| Check | Result |
| --- | --- |
| Initial integrated reliability units | 39 passing, exit 0 |
| Upstream compile | exit 0 after adapting removed helper/base-class/constant references |
| Focused root units | 55 passing, exit 0 |
| Fluidcore units | 104 tests, 0 failures, 2 skips |
| Focused AWG/join/battery/shaft server | 66/66 including harness, exit 0 |
| Arc Furnace/legacy turbine/pole LOS server | 28/28 including harness, exit 0 |
| Data generation twice | both exit 0; all 4,264 files byte-identical including cache |
| Full `build :test` | exit 0; root 1,368 tests, 0 failures, 6 skips; CAD 18 passing; fluidcore 104, 0 failures, 2 skips |
| Final follow-up compile and focused server | exit 0; 67/67 including all 39 AWG and new refusal assertions |
| Full unfiltered GameTest | 2,091/2,094 passing; Gradle exit 1, server exit 3; 17m21s |

Full server failures remain `golden_descriptors`, `golden_slots`, `jei_every_item_reachable`. JEI reports one unreachable item (`nibble_memory_chip`, 887 listed / 2,431 ways). These are the same historical failing cases; snapshots and assertions were not weakened. No newly failing cases observed. The full run covers the merge snapshot before the final small pole-message follow-up; that follow-up compiled successfully and passed all 67 focused cases (66 requested IDs observed plus harness), exit 0.

All three actual AWG physical chunk-eviction cycles pass in the unfiltered run. Reload creates fresh block entities while preserving owned terminals, temperature, insulation exposure and melted-wire inactivity. Both paid conductors carry the measured load current (example 11.603657 A each, differences below 1e-9 A). Post-reload removal releases MNA component references. Unloaded chunks deliberately retain simulation under the existing global contract. Both-chunk eviction is proven; independent adjacent-only eviction is limited by FULL-ticket dependencies. Corner/sag/branched-specific physical eviction is not newly claimed.

Stale confirmation/spool-funding, exact debit, interrupted/partial/obstructed rollback, split/refund, malformed/orphan preservation and branch-removal tests pass. No AWG or Arc Furnace generated paths changed. The 93 changed generated paths reflect upstream recipes/new content. Historical golden JSON remains unchanged.

## Local commits

- `29c5fc757`: initial sources/audit decisions.
- `917d55a11`: exact reliability history merge.
- `8375a0c42`: verified reliability checkpoint.
- `a869d0b19`: original upstream ancestry merge and compatibility adaptations.
- `30a70b894`: verified pole guidance follow-up.
- Final report/checkpoint commit follows these; `git rev-parse local/awg-upstream-integration` identifies the final documentation tip.

## Remaining risks and next step

The three historical failing cases remain unresolved. Live green-preview appearance and actual client dimension/disconnect flows still require the manual checklist in the reliability progress document; no live client was visually inspected. Chunk-shape/residency limitations above remain. Optional IPN support remains deferred.

The integration is ready for review and an approved update of the AWG feature branch, subject to accepting the documented historical failures and remaining manual checks. Verify the feature branch still points to the recorded base, then fast-forward it to the integration branch in its own clean worktree after approval. No push, feature/develop update, or other-worktree edits were performed.
