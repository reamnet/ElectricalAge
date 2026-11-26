# Native AWG parity follow-up

## Current implementation checkpoint
- Follow-up implementation, full verification and push complete in isolated local/awg-upstream-integration worktree. Code9ded03df1 (lamp/meter),706fa16bf (mode menu) and reportingf71bd96cb pushed to origin/local/awg-upstream-integration; final reporting-only checkpoint follows.
- Ordinary rated bulbs no longer use farming tint; all18 classifications tested, actual farming remains pink. Powered native MultiMeter/Thermometer/AllMeter clicks now read AWG before/after reload.
- All70 Cu/Al variants offer per-spool C/tool radial Native voxel / Legacy six-node. Optional inventory NBT flag, strict matching-held-spool packet30, existing legacy backend, stale route/preview cleanup. Real Cu/Al main-hand legacy and native placements have exact1m/2m payment.
- Focused units59/59 and server78/78 pass. Full build/root exit0,18m13s: root1370/0fail/6skip, CAD18/0fail, fluidcore104/0fail/2skip.
- Full server session15349 FINISHED:2103/2106 pass, exactly historical golden_descriptors/golden_slots/jei_every_item_reachable, Gradle exit1/server exit3,16m17s. All51 AWG cases and three actual chunk evictions observed. No active test JVM.
- Fresh JAR build/libs/eln-26.3-0.1.0-port.jar SHA256 4494585ba54d87fb63e1de0d220889a6ed2bc176d31a8365af28693fe6b711ec; companion .jar.sha256 written.32 relevant packaged classes match verified compiled output.
- Final checks pass:934 registry mappings and specifications unchanged; no generated/assets/golden/pin changes; diff checks clean; source feature7b91ee884/reliability573af9324 unchanged. No Qwen worktree changes.
- Report: awg-lamp-meter-placement-followup.md. No coding/testing work pending. Next user action: install fresh JAR, hold Cu/Al spool, press configured tool key (default C), choose placement mode. Branch push succeeded; final checkpoint also pushed and remote matched HEAD. No main/feature/develop merge. No live GUI/rendering claim.

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

## Priority 3 in progress
- Atomic virtual split plans now allow fourth-arm joins and two cuts in the same original paid edge; one replacement/undo owns all children. Multi-original collision exclusion remains scoped to exactly the planned replaced parts. Native visual segments inherited/sliced on paid splits.
- First repeated-tap server exit1: installation, rollback, heat/exposure/length conservation, native parity, powered I-V, reload and partial removal passed; loop KCL assertion sampled after thermal restamping. Moved the strict 1e-6 A KCL observation to simulator electrical phase (same convention as prior AWG reliability), not a tolerance relaxation.
- Current session69410, `/tmp/awg-parity-repeat-game2.log`, expanded all focused cases plus new milestone fixtures. Await exit before further class edits. Pending priority4 shared reach/legacy selection cleanup, final full build/unit/server and fresh JAR/push.

## Priority 3 verified
- Expanded focused server 77/77, exit0, `/tmp/awg-parity-repeat-game2.log`. Native and AWG fourth-arm and same-host double-cut loop fixtures pass with atomic rollback, exact new-wire payment, conserved 400 J/exposure/one-meter original ledger, reload/reconnect and powered partial removal.
- Electrical-phase loop currents: load11.63436436084 A = original middle9.48090514781 A + new loop2.15345921303 A (within 1e-6 A). All conductor voltage drops agree with descriptor length/temperature resistance.
- Next commit this milestone, finish safe legacy control/reach consolidation and strengthen negative visual packet checks; then full regression/build/push. Keep explicit legacy compatibility functions rather than silently deleting saved-world-era workflows.

## Priority 4 verified
- Ordinary native session start now clears stale explicit legacy selection. Legacy direct compatibility clicks reuse CablePlacer.reachable instead of duplicated AABB/reach math (including native reach slack). Normal placement remains native; explicit old single-block/two-click workflows retained to avoid removing functionality/save-era operations.
- Existing shared-session fixture now starts from a legacy selection and verifies native session takes ownership and clears it. Historical underfunded fourth-arm check retained unchanged, label clarified to distinguish insufficient funds from the removed junction-degree restriction.
- Focused server 77/77, exit0, 1m (`/tmp/awg-parity-controls-game.log`), all new milestone fixtures plus original AWG/join/battery/shaft cases pass.
- Next commit control cleanup, add negative coverage for new optional geometry payload/save fields and strengthen loaded fourth-arm check, then final full build/unit/server sequentially, fresh JAR/hash, policy review and verified push.

## Final targeted validation before full regression
- Added negative checks for malformed optional native visual NBT, invalid second-corner ownership, every truncated native geometry packet prefix and oversized visual count. New fields fail closed; legacy formats remain covered by existing tests.
- Strengthened fourth-arm fixture with real 1/2/3-ohm parallel loads. Electrical sample: source21.17368128110 A = trunk11.47781110043 A + other loads9.69587018067 A, within1e-6 A; removal opens only its own parallel load and preserves the other load/trunk.
- Targeted stair/slab/fourth-arm/same-host loop server 5/5 including harness, exit0,47s (`/tmp/awg-parity-final-regressions.log`). No class edits remain pending after test-only verification commit.
- Next: full `build :test` (fresh JAR), then full unfiltered server sequentially with no concurrent heavy jobs; verify actual failure baseline, generated/registry/snapshot invariants, produce final report and push verified branch to fork.

## Full regression/artifact checkpoint
- Full build session11127 remains in root :test, active worker; no reported failure yet. No parallel server/build jobs.
- Fresh mod JAR assembled at build/libs/eln-26.3-0.1.0-port.jar (38 MiB), SHA-256 e0cc93dbb16af38baedc64b678a46a6dec8309ae7d449ff40c88bbcb1bb0befb. Companion .jar.sha256 written. Artifact provisional until final gates; no source changes since958effa59.
- Final report prepared at awg-native-implementation-results.md; update actual full-test results and push status only after completion.

## Full build passed
- `./gradlew build :test`: exit0,18m24s, `/tmp/awg-native-parity-full-build.log`.
- build/test-results/test: 1368 tests / 0 failures / 0 errors / 6 skips
- cad/build/test-results/test: 18 tests / 0 failures / 0 errors / 0 skips
- fluidcore/build/test-results/test: 104 tests / 0 failures / 0 errors / 2 skips
- Next: two sequential datagen runs/hash verification, then unfiltered GameTests with no competing jobs.

## Data generation passed / full server start
- Both runClientData exits0 (24s/22s); strict SHA-256 equality for4264 files across both and original integration baseline, no exclusions.
- Starting unfiltered `./gradlew runGameTestServer`; log `/tmp/awg-native-parity-full-game.log`. No parallel build/data/client jobs and no filters/budget overrides. CodeHEAD958effa59.

## Full server checkpoint
- Session14612 remains active, batch26 (long upstream fluid plants); 2105 total, 1332 passed, 2 failures observed, 16 active. Only golden_descriptors/golden_slots reported so far; final result not yet known.
- No source edits since958effa59 and no parallel heavy jobs. Await completion, then final report/policy audit/JAR byte/hash verification and authorized fork-branch push.

## Full server and final review passed
- runGameTestServer unfiltered: 2102/2105, historical3 only, Gradle exit1 / server exit3,16m24s. All50 AWG IDs (including11 added since prior full baseline) explicitly observed. All3 physical chunk evictions observed. Log /tmp/awg-native-parity-full-game.log.
- Artifact contains byte-identical current verified class files for 32 relevant classes; SHA256 unchanged after datagen. All934 registry objects equal baseline; existing descriptor/physics/run/length code, golden JSON, generated assets and pins untouched. Diff checks pass. Source refs unchanged and upstream/reliability/Arc ancestors preserved.
- Final report prepared. Next action authorized: reporting commit and fork integration-branch push, with remote verification. No feature/develop/main merge.

## Push completed
- `git push -u origin local/awg-upstream-integration`: exit0, fork remote https://github.com/reamnet/ElectricalAge.git fast-forwarded 2c6b81655 -> b1f0cbd7e with all verified implementation/reporting history.
- Final reporting-only checkpoint follows that successful push and is pushed to the same branch. Use git rev-parse HEAD / the remote ref for the final tip. No force push or main/develop/feature merge/update. No active JVM; local tree clean after checkpoint commit.
- Deliverable JAR/hash and complete test results are in awg-native-implementation-results.md. Full suite has the historical3 failures and no new failed cases. Remaining unsupported pure-decoration-owner/sag-tap cases are explicit; no live graphical claim.

## Lamp tint and meter follow-up (November26)
- Start clean at pushed e033d0805, same isolated integration worktree. User reports pink bulbs and unreadable new voxel/AWG cable meters; continue fixes, no new parity audit.
- Verified cause: VoxelNode meter dispatch/side fallback omit AwgCablePart. Add explicit AWG endpoint/current and absolute-temperature readings while reusing native meter interaction/formatters.
- Verified colour classification defect: DecorativeLampObjRender classifies any vegetableGrowRate>0 as farming; every rated bulb has positive growth values. Use registered rated technology first, with legacy type/growth fallback. Preserve actual farming/holiday colours.
- Next: implement targeted tests for all18 rated bulb technologies and actual powered AWG meter clicks before/after reload; compile/focused tests and fresh JAR. Prior full baseline2102/2105 historical3 only. No active JVM.

## Lamp/meter fixes verified
- Rated bulbs now resolve SourceLampLife visual technology before legacy growth/type fallback; decorative fixtures and work-light packets reuse it. All18 technology identities and legacy defaults tested; actual farming stays pink. No growth/life/electrical parameters changed.
- AWG meters use actual endpoint voltage, independent current and absolute temperature; ordinary native click handling and side fallback now include AWG parts. Powered real MultiMeter/Thermometer/AllMeter clicks validated before/after reload at six terminal configurations.
- Focused units 52 tests, exit0,1m08s, /tmp/awg-lamp-meter-unit.log. Powered server7/7, exit0,29s, /tmp/awg-lamp-meter-game.log. No live visual rendering claim.
- User additionally requests explicit copper/aluminum placement-mode menu. Next reuse existing tool radial key (C), per-spool native/legacy-six-node mode, existing legacy placement backend, strict held-spool packet validation and mode-change session cleanup.

## Spool menu implementation checkpoint
- Menu reuses RadialMenu and wrench/tool key, no independent UI/control engine. Native default; legacy uses original per-block UtilityCableElement placement/accounting, even when six-node elements are hosted in converted voxel blocks. Existing explicit sneak compatibility routing preserved for native mode.
- New optional utilityLegacyPlacement stack field; no registry, electrical or saved placed-wire fields changed. Mode packets validate size/mode/held descriptor and player permissions, never modify paid length/UID. Changes clear native route, preview generation/outline and compatibility selection.
- Compile exit0,1m14s. New all70 mode/packet/actual Cu+Al placement GameTest in session99660.

## Menu focused verification checkpoint
- Initial mode/native-session/meter server4/4 exit0,48s (/tmp/awg-placement-mode-game1.log). All70 defaults/persistence/accounting, strict malformed/stale packet rejection, session cancellation and real Cu/Al legacy/native placement passed.
- Fixture updated to use its own survival-mode player, leaving shared harness state untouched. Assemble/focused units exit0,51s (/tmp/awg-mode-final-unit-build.log).
- Broader server session46141 active (/tmp/awg-mode-final-focused-game.log). Next commit verified menu after observing all expected IDs, then full root/server regression sequentially and final JAR/push.

## Explicit placement menu verified
- Broader focused server78/78 exit0,38s (/tmp/awg-mode-final-focused-game.log), including all51 AWG cases plus native joins/battery/shaft and harness. Independent mode fixture uses its own player.
- Assemble + focused lamp/AWG/radial/preview units59/59 exit0,51s (/tmp/awg-mode-final-unit-build.log).
- Controls: hold an intact single-conductor copper/aluminum spool, open tool menu (default C), select Native voxel cable or Legacy six-node cable. Choice persists on spool copies/save data. Legacy uses original per-block element and one-meter debit; native retains existing routed-length debit. Esc cancels menu choice; mode change cancels stale route/preview, never pays cable.
- No live GUI/visual inspection claimed. Next commit menu independently, full build/root then full unfiltered server sequentially; assemble/hash final JAR and push verified follow-up commits to same development branch.

## Full follow-up regression start
- Verified menu committed706fa16bf; lamp/meter fix9ded03df1. All source changes committed. Full build :test session3496, /tmp/awg-mode-full-build.log. No parallel heavy jobs. After exit run full unfiltered server, verify new total (prior2105 plus one new menu case), artifact hash and policy; push verified branch.

## Full follow-up build passed
- build :test exit0,18m13s, /tmp/awg-mode-full-build.log.
- build/test-results/test: 1370 tests / 0 failures / 0 errors / 6 skips
- cad/build/test-results/test: 18 tests / 0 failures / 0 errors / 0 skips
- fluidcore/build/test-results/test: 104 tests / 0 failures / 0 errors / 2 skips
- Starting full runGameTestServer with no filters/budget overrides, /tmp/awg-mode-full-game.log. No other JVM from tests/build remains active. CodeHEAD706fa16bf.

## Full follow-up server checkpoint
- Session15349 active at long batch26; 2106 total / 1330 passed / 2 failures observed / 19 active. Only historical golden_descriptors/golden_slots so far. No source changes or competing heavy jobs.
- Root1370 + CAD18 + fluidcore104 previously passed; next finish server, verify new artifact hash, update follow-up report and push9ded03df1 +706fa16bf/reporting to origin same branch.

## Full follow-up verification completed
- Full server2103/2106; exactly historical3 failures, all51 AWG IDs present/pass, three actual evictions. Full build1370 root/18 CAD/104 fluidcore with0 failures. No source changes since706fa16bf.
- Fresh JAR SHA256 4494585ba54d87fb63e1de0d220889a6ed2bc176d31a8365af28693fe6b711ec;32 relevant Java/Kotlin packaged classes byte-equal verified outputs. All934 mappings and descriptor parameters unchanged; generated/resources/goldens/pins untouched.
- Follow-up report and menu instructions finalized. Next authorized action: commit report/checkpoint and push same fork development branch, no merge to protected/source branches.

## Follow-up push completed
- git push origin local/awg-upstream-integration exit0: remote fast-forward e033d0805 -> f71bd96cb, containing verified fixes/menu/report. This final reporting-only checkpoint is also pushed; final tip is git rev-parse HEAD / confirmed remote ref.
- No main/develop/feature update, no other-worktree edits, no unfinished source changes. Deliver fresh JAR/checksum and C-menu instructions. All follow-up implementation/testing complete; no live graphical claim.

## Pole selection and bare-wire appearance follow-up
- Base b34d43a6e, isolated integration branch; initially clean. User reports unselectable elevated primary/secondary pole connections, fixed-width/uncoloured spans and missing bare six-node textures.
- Confirmed bare descriptor references absent sprites/currentcable.png; native node cap also resets tint. Fix existing native renderer through small hooks, retaining geometry/accounting.
- Pending: pole owned-ghost selection/secondary rack coverage, descriptor-driven span appearance, focused/full tests and fresh JAR. No gameplay/visual verification claimed for these fixes yet.

## Bare six-node texture verification
- Replaced absent sprites/currentcable.png with existing neutral sprites/white.png. Utility renderer applies copper/aluminum tint; native junction API retains default white cap, optional preservation used only for bare intact utility wire. No save/physics/registry changes.
- Existing focused AWG units exit0 (/tmp/awg-bare-texture-test.log). New render regression initially failed because test passed null to Kotlin non-null renderer constructor; fixture corrected to exercise shared tint emission without a world. New focused units exit0,59s (/tmp/awg-bare-texture-regression2.log).
- Catalog GameTests now check real legacy texture resources for every70 descriptor; session25932 /tmp/awg-bare-texture-game.log pending.
- New user request: placeable insulators. Original ForkPatch source located read-only in ELN1.21.1ForkPatch/src/main/kotlin/mods/eln/utility/insulator/{LineInsulator.kt,LineInsulatorClient.kt}; source IDs8043..8052 (800/3200/12500/69000/230000V line/feed). Not present in integration tree. Must adapt native GridElement circuit keys (target HV0/HV1/HV2, source HV_SINGLE), native model hitboxes, all six mounts, recipes/models/manifest and GUI. Source assets live assets/eln/model/lineinsulator*. Do not touch similarly named Qwen worktrees or integrate their unverified code.
- Next: finish catalog tests and commit texture fix independently; then pole selection/span styling and standalone insulator port, with focused verification before full regression/assemble/push.

- Catalog server4/4 (three catalog scenarios + harness) exit0,28s (/tmp/awg-bare-texture-game.log), all70 actual descriptors have valid legacy textures; material electrical/drop/reload/refund checks preserved. Focused root35 tests/0 failures including new headless native cap tint/white texture check. No live GPU verification. Texture fix ready for independent commit.

## Standalone insulator port in progress
- Texture fix independently committed c44ddf3e4. Correct focused root total35/0 (prior checkpoint typo48 corrected).
- Original source five voltage tiers/two mount types adapted to native GridElement/GridRender, existing six-face mount selection, native block model geometry, ElnScreen/MenuSpec GUI. Ten source IDs8043..8052 added without changing existing934 mappings. Original OBJ/MTLs copied from read-only ForkPatch source, never Qwen trees.
- Initial compile found two source API differences (inventory getter and render descriptor field), corrected; compileKotlin compileJava exit0,1m01s (/tmp/awg-insulator-compile2.log). Recipes and six new GameTests added afterward; first focused run session46317 /tmp/awg-insulator-game1.log pending.
- Pending test-fixture review: feed test should check e.getElectricalLoad(front,LRDU.Left), not front.inverse (source intentionally excludes outer clamp face); use 2AWG bare conductor for powered tests. Must correct if observed. Datagen twice, focused suite/full regression, fresh JAR, commits/push still required.
- Earlier pole selectable ghosts/secondary-rack coverage and descriptor-sized/material-coloured spans remain unfinished. Do not report those fixed.

## Insulator focused server verified
- Fixed source/26.3 API mismatches; vanilla recipe ingredient uses existing vanilla("white_terracotta") helper. Added five OBJ index entries after mod-load caught missing model registration. Initial powered fixtures requested a nonexistent structure; now reuse existing32x28x16 native pole fixture without changing assertions.
- Focused server7/7 exit0,36s (/tmp/awg-insulator-game4.log): six new scenarios + harness. All60 type/mount combinations and their ordinary harness persistence passed. Five powered tiers measure Kirchhoff/Ohm current and voltage drop over exact12m paid AWG span, repeated clicks preserve28/40m balance, circuit locking and one-link reload pass.
- Datagen1 session67743 /tmp/awg-insulator-datagen1.log active; next verify generated delta, hash tree, rerun datagen, root/focused regression, inspect diff/commit insulator port. Original source attribution b7444a7b8a0c5e072b1fd35eed2b74a282c3348c.

## Insulator data generation verified
- Datagen initially caught missing flat item icon. Insulators now reuse existing native 3D model GUI item path (same special renderer as grid converters), with its existing neutral particle material; no invented bitmap or missing-icon fallback. Ten items retain their actual source meshes in hand/inventory.
- Successful runClientData exit0,48s (/tmp/awg-insulator-datagen3.log), repeat exit0,24s (/tmp/awg-insulator-datagen4.log). All4314 generated files byte-identical across these two runs, including tracked caches. Delta is ten insulator families +20 translation keys +cache indexes; all934 old registry mappings checked identical, exactly10 new.
- Restored original230kV insulator collision envelope through native shapeAt override; shorter mounts use native model hitboxes. Added headless native draw emissions and actual support removal/one-time12m refund checks to focused server fixture.
- Focused root units session33623 /tmp/awg-insulator-unit.log active; next rerun focused7 server on final code, review diff and independently commit port before continuing pole/span follow-up.

## Insulator port ready for independent commit
- Final focused root51/51 exit0,56s (/tmp/awg-insulator-unit.log). Final server7/7 exit0 (/tmp/awg-insulator-game5.log), now also validates all10 native mesh emissions and real support-block removal at every tier after reload: endpoint disappears, opposite pole disconnects and precisely12m returns once.
- Two successful datagen runs byte-identical4314 files. Existing934 mappings and all70 AWG specifications unchanged. Source IDs8043..8052 added; no historical snapshots or dependency pins changed. No live client visual claim.
- Next: independently commit insulator source/models/recipes/generated files, then fix pole selectable clearance and descriptor-driven span styling. Full suite/assemble/push deferred until final source stabilizes.

## Pole selection implementation checkpoint
- Insulator port committed bb2dadb8d; texture fix c44ddf3e4. Source/test work now targets unselectable pole reservations. New native ghost metadata14 has normal outline but empty walking collision; only new pole/long-insulator descriptors opt in. Added four rack reservations below primary arms.
- Shared GridGhosts repair runs through existing GridElement lifecycle, checks only loaded chunks and owned ghosts, never overwrites a neighbouring build, stops after completion and retries unloaded chunks at most once per second. No static owner/session map. Reuses native ghost click forwarding, no new cable routing/geometry.
- Added paired native overhead/AWG actual outline-ray/block-click cases across all four69/230kV variants and single/three-phase/secondary wooden poles. Includes independent rack payment/links, old ghost reload migration and foreign-block preservation. Compile before shared-refinement exit0 (/tmp/awg-pole-selection-compile.log); next compile/run final cases.
- Latest user request twelve-circuit panel: already registered ID7988, recipe and modern native screen exist; nine ForkPatchBreakerPanelCases cover physical120/240V branches, rating/trip/save, published controls, wire/native lamp/rated relay. Include them in next focused/full verification; no duplicate panel implementation.

## Pole selection and existing panel verified
- Paired ray/connection suite8/8 exit0 (/tmp/awg-pole-selection-game2.log), then final combined pole/insulator/panel suite23/23 exit0,55s (/tmp/awg-pole-panel-insulator-final-game.log). All nine existing twelve-circuit panel scenarios pass: source-rated hardware/recipes, real120/240V branches, thermal/native trips and saved latches, source settings/publication, wireless/native lamp demands, rated relay control and wired feeders. Panel already ported and included as Twelve Circuit American Breaker Panel (ID7988); no duplicate identity added.
- Deferred repair now schedules server-thread work outside simulator iteration, guards node identity/destruction and coalesces retries. Test fixture compile syntax corrected (Kotlin reserved is method), no assertions weakened.
- Shared metadata14 uses native selectable reservation envelopes, intentionally broader than exact mesh; walking collision stays empty. Only relevant poles/long insulators opt in; underground cabinet clearance stays unchanged. Foreign occupied cells in older saves are preserved. No live graphical verification claim.
- Next commit selection milestone; implement descriptor-driven span width and bare Cu/Al tint through existing catenary parameters, then final full regression/JAR/push.
