# AWG reliability progress

## Current checkpoint
- Task: establish existing AWG coverage and baseline, then harden electrical ownership, lifecycle, rollback and preview cleanup without changing routing geometry.
- Worktree: ElectricalAge-gpt-awg-reliability; branch `local/gpt-awg-reliability`.
- Initial/current commit: `7b91ee884`.
- Initial Git status: clean. No unrelated WIP in this worktree.
- Read: supplied AGENTS.md, CLAUDE.md, docs/testing.md.
- Last verified action: initial Git inspection (exit 0).
- Changes: this progress file only.
- Tests: pending environment doctor, build and focused AWG suite.
- Constraints: no Qwen worktree changes, no golden/registry/dependency changes, no pushes; preserve existing AWG geometry.
- Blockers: historical mod-load blocker in supplied instructions requires current verification; GameTest padding forces chunks, so genuine unload needs an isolated location and observed unload events.

## Next actions
1. Read latest AWG parity audit and relevant code/test commits; map completed coverage.
2. Run mctest doctor and baseline build/root AWG tests with required Java environment.
3. Inspect world chunk events and electrical registration ownership; add focused integration coverage for missing invariants.
4. Record commands, exit codes, measurements, limitations and commits at each checkpoint.

## Audit and environment checkpoint
- Existing audit: `docs/forkpatch-port/awg-native-parity.md`, through `7b91ee884`. Recent commits already cover powered off-center taps, sag landing splits and inner/outer corner endpoint ownership. Existing `AwgNativeRouteCase`, `AwgRoutingReliabilityCase`, `AwgJunctionCase` cover stale previews, duplicate confirmations, interrupted placement, blocked/underfunded taps, simulator rollback and mixed metals. Avoid duplicating these.
- Chunk contract: `NodeBlockEntity.onChunkUnloaded()` deliberately retains the node; `setRemoved()` skips node destruction on chunk unload. EA simulates unloaded chunks (also documented in testing.md). Disconnect-on-unload would change the global simulation contract. Actual chunk event coverage must check retained continuity and identity, not assume nodes are retired.
- `source /tmp/eln-port-java-env.sh; python3 tools/mctest/mctest doctor`: exit 1; JDKs/desktop tooling pass, worktree has no run/server.properties or built jar. No live game attempted.
- `./gradlew build :test --tests '*Awg*Test'`: exit 1, before compilation: shared `/tmp/eln-port-gradle` immutable transform cache corrupted. Shared cache left untouched.
- Retrying with isolated `GRADLE_USER_HOME=/home/matthew/.cache/eln-awg-gradle` (copied dependency and NeoForm caches, fresh transforms), log `/tmp/awg-reliability-baseline-isolated.log`.
- Verified source defect: preview generation resets on `forget(UUID)`. A queued generation 1 can become current again after same-UUID reconnect starts generation 1. Fix uses globally unique generation counter while still releasing per-player maps; two deterministic lifecycle regressions added. These changes were made during dependency preparation, so the isolated build also compiles the fix; it is not a pristine source baseline.
- Current changes: progress document, KitPreview.java, KitPreviewLifecycleTest.java. No geometry changes.
- Next: finish build; run focused preview tests, then use GameTests to verify current runtime availability and existing AWG coverage before adding chunk test.

## Chunk fixture prepared
- Added `AwgChunkLifecycleCase` to CableCases: remote X=1,000,014 circuit spans X=15/16 within its chunk pair. FULL tickets at radius zero retain one chunk while releasing its neighbour. Assertions require actual absent `getChunkNow` plus removed original block entity, retained adjacent chunk, same node/run/terminal identities, powered current before/during/after, and retirement on post-reload removal.
- This fixture has NOT run yet. It must not be described as verified. Fixed remote location is intentionally away from forced GameTest structures; explicit cleanup removes its blocks and releases tickets.
- Node state is independent of block entity lifetime by design; no disconnect-on-unload change proposed.
- Existing catalog GameTests enumerate 42 copper + 18 aluminum + 10 high-voltage variants and verify their identities and powered operation. Existing mixed junction case already measures KCL, resistance at temperature, fusion, exact refunds and unaffected branch continuity.

## Coverage map (existing source, execution results still pending)
| Concern | Existing coverage / implementation |
| --- | --- |
| Paid edges and independent resistance | AwgCableEdgeTest; AwgCableRunTest; AwgWorldCase |
| Three-way / mixed-metal branches, heat, fault isolation | AwgJunctionCase (two variants) |
| Inner/outer corner ownership and arm removal | AwgNativeOuterCase; AwgNativeTapCase; AwgTransitionCase |
| Sag persistence / supported landing taps | AwgNativeSpanCase; AwgNativeTapCase; AwgPersistenceTest |
| Cached preview stale obstruction, repeated confirmation | AwgNativeRouteCase |
| Partial placement rollback and preserved existing circuits | AwgRoutingReliabilityCase; AwgJunctionCase |
| Malformed saved part and inactive orphans | AwgWorldCase; AwgPersistenceTest; NodeSaveLoadTest |
| Repeat removal and exact material refunds | AwgWorldCase; AwgPurchaseTest; AwgWireLengthTest |
| Real partial chunk eviction | New AwgChunkLifecycleCase, not yet run |
| Preview queued-worker teardown | New KitPreviewLifecycleTest, not yet run |

## Manual client validation checklist
Use a disposable Survival world and record spool lengths, screenshots and current readings.
1. Compare green/red preview and installed bounds for a straight run, inner corner, full-block outer knuckle, short sag span, supported landing tap and three-way junction on floor/wall/ceiling.
2. Cancel with each supported control; switch items, dimensions, disconnect and reconnect. No outline or pending route from the previous context should return.
3. Change support, obstruction and available spool length after preview, before confirmation. Rejection must preserve old cables and spool balance; double confirmation must not debit twice.
4. Power a branched circuit across X=15/16 away from spawn/forced chunks. Unload/revisit and restart the world. Compare currents, heat/damage, conductor shapes, ownership and exact shears refunds. EA intentionally keeps unloaded node circuits powered.
5. Remove an arm and then the knuckle/junction. Only the connected branch should lose continuity.
No actual client rendering has been launched or visually inspected in this worktree.

## First verified change
- Initial isolated build failed (exit 1) on eight new fixture compilation errors: ChunkPos is now a record with integer constructor and accessors. Corrected locally; no preexisting compilation failure established.
- `GRADLE_USER_HOME=/home/matthew/.cache/eln-awg-gradle ./gradlew build :test --tests '*Awg*Test' --tests '*KitPreviewLifecycleTest'`: exit 0, BUILD SUCCESSFUL (1m16s). Includes subproject build checks and focused root tests; does not establish full root/GameTest baseline.
- Preview generation fix and both new lifecycle unit tests pass. No registry IDs, specifications, goldens or routing geometry changed.
- Focused GameTest command now running: `./gradlew runGameTestServer -PelnGameTests=awg_actual_chunk_lifecycle,awg_voxel_mixed_metal_junction,awg_voxel_world_three_way_junction,awg_catalog_copper,awg_catalog_aluminum,awg_catalog_high_voltage` with isolated cache, log `/tmp/awg-reliability-gametest.log`.
- Next: inspect runtime results, fix fixture issues if any, then expand regression execution. Do not compile main classes until this GameTest process exits.

## First GameTest results and fixture correction
- Focused six-case server run: exit 1. Mod loads successfully here; supplied historical construct-mod blocker is obsolete for this commit.
- Copper (42), aluminum (18), high-voltage (10), homogeneous and mixed-metal junction cases PASS, including their persistence/cleanup checks.
- New lifecycle fixture FAILS its required actual eviction observation; electrical continuity and terminal identity checks pass, but these do not establish chunk eviction.
- Investigation of current Minecraft ServerChunkCache/ChunkMap source: a FULL-ticket neighbour keeps target generation data resident through the dependency halo; absence as FULL is not the same as actual block-entity eviction. Radius-zero tickets did not provide independent adjacent eviction.
- Fixture corrected to release BOTH circuit chunk tickets, then require actual removed anchor + absent target chunk and absent source chunk. This is meaningful actual cross-boundary unload/reload coverage, but NOT partial eviction with an adjacent chunk remaining resident. That requested scenario remains a harness/ticket-topology limitation.
- Re-run: `./gradlew runGameTestServer -PelnGameTests=awg_actual_chunk_lifecycle`, log `/tmp/awg-reliability-chunk.log`. Pending.
- First run measured 11.611313 A through the loaded cable/resistor at the attempted unload checkpoint and 11.611277 A after attempted reload (thermal evolution). These are loaded-chunk measurements, not eviction results.
- Current commit `1e2e7291d`; uncommitted CableCases registration and AwgChunkLifecycleCase. No main compilation while game JVM active.

## Actual eviction verified
- Corrected single lifecycle GameTest: exit 0, 2/2 required tests passed (includes harness automatic case), 55s Gradle run. Actual target AND source chunk eviction observed; original anchor removed; fresh anchor after reload resolves the identical AWG part and terminal objects. Post-reload removal retires the node/run.
- Measurements: 11.611424493 A before eviction; 11.611313275 A while both chunks absent; 11.611277232 A after reload; 11.611216983 A before removal. Heating is allowed to continue while absent; temperature-dependent resistance explains drift.
- Material: 2.125 m spool becomes exactly 0.125 m for two 1 m conductors.
- Assertion wording corrected to describe terminal identity rather than imply total global MNA counts. No snapshot/deserialization is used for eviction.
- Partial eviction with an immediately adjacent resident chunk remains unverified. Corners/sags/melted/insulation-damaged parts have existing save/reload tests, but have NOT yet been put through this actual eviction fixture.

## Next hardening checkpoint
- Commit `182adb885`: actual powered cross-boundary eviction fixture and registration.
- Extending fixture to three real eviction/reload cycles, retaining exact terminal objects each time and cleaning remote drops.
- Client inspection found world-space KitOutline boxes survive client level teardown (no cleanup hook). Added clear on client level unload, explicit preview generation forgetting on server dimension change and level guards on shared generic/pipe workers.
- Decoder previously allocated using signed packet count and accepted arbitrary states/inverted boxes. Added bounded count/state/bounds validation, preserving last valid outline on malformed/truncated input; wire format unchanged.
- Added KitOutlineTest: valid packet/teardown, malformed/truncated atomic rejection, and empty cancellation. Tests currently running with AWG/preview suite, log `/tmp/awg-reliability-preview-tests.log`.
- No graphical rendering claim: tests decode stored geometry and cleanup without a GPU/client.

## Preview cleanup verified
- `./gradlew :test --tests '*Awg*Test' --tests '*KitPreviewLifecycleTest' --tests '*KitOutlineTest'`: exit 0, 39 tests, zero failures, 2m03s.
- KitOutline decoder/clear tests are headless; LevelEvent teardown and dimension event hooks compile, but real client visuals and dimension/disconnect flows are still manual checks.
- Repeated chunk fixture now seeds a 60 C powered run with exposure 0.375 through the save schema, plus a separate fused run; each actual eviction/reload checks elevated heat, unchanged exposure, fused electrical inactivity and owned terminal identity. Final removal checks owned loads leave MNA and release component references.
- Three-cycle focused GameTest running; `/tmp/awg-reliability-cycles.log`. Await result before committing fixture expansion.

## Repeat-cycle diagnostic
- Three-cycle run and instrumented re-run both exit 1 solely on strict current equality measured after the simulator tick. All actual evictions, damage/heat/fused-state checks, terminal identity checks and final owned-load cleanup PASS.
- Instrumented example: load 11.602606412 A, feeder 11.602606413 A, hot second edge 11.604042363 A. The hot edge's slow thermal process restamps resistance AFTER MNA solves; reading V/R after that stamp compares new resistance against previous electrical voltages. This is a measurement phase issue, not evidence of a current imbalance during the solve.
- Correcting fixture measurement by adding an electrical process sampler (Simulator runs these immediately after `mna.step()`, before slow thermal restamping). Keep strict 1e-6 A current equality; no tolerance weakened. Sampler explicitly removed in fixture cleanup.
- Next: rerun focused lifecycle, then full root/build and full GameTest regression. Current commit `11a7839cd`; uncommitted fixture expansion only plus this log.

## Three actual eviction cycles verified
- Phase-correct focused lifecycle run: exit 0, 2/2 required tests pass, 54s (`/tmp/awg-reliability-cycles-sampled.log`). Three target block entities actually unload; both chunk sides absent each cycle. Heat >30 C, insulation exposure exactly 0.375 and separate fused state survive every cycle.
- Strict electrical samples agree to <1e-9 A: e.g. load 11.603565784842 A, feeder 11.603565784255 A, second 11.603565784255 A while chunks absent. No simulator physics change was needed.
- Terminal object identities remain unchanged after every actual reload; after removal their MNA membership and connected-component references are empty. This is scoped resource evidence, not an assertion that all global object counts must vanish.
- Next: full build/root test baseline, then full GameTest suite. Preserve test outputs and record actual failures, including any beyond the historical three.

## Final missing rollback invariant
- Added explicit stale-funding confirmation to existing AwgNativeRouteCase: after a valid cached six-meter preview, shorten the live spool to 5.9375 m; confirm must keep every route endpoint empty, retain the session and preserve the reduced spool exactly. Restore funding and confirm normally. This complements existing underfunded-preview and stale-obstruction checks without duplicating routing geometry work.
- Full root/build currently running against previously compiled main outputs; do not compile this new GameTest edit until root test JVM exits. Full GameTest run will compile and exercise it.

## Wider regression in progress
- Current HEAD `2817ed508`; branch remains `local/gpt-awg-reliability`. Commits: `1e2e7291d`, `182adb885`, `11a7839cd`, `2817ed508`.
- `./gradlew build :test` with isolated Gradle home is running (log `/tmp/awg-reliability-full-build.log`), full unfiltered root suite; no failures reported yet. Subproject/API compilation has passed so far.
- Uncommitted changes: stale-spool confirmation assertion in AwgNativeRouteCase and progress document only.
- Exact next actions: await root test exit; run focused `runGameTestServer -PelnGameTests=awg_native_kit_session`; commit verified funding assertion; run unfiltered `runGameTestServer`, inspect failures and AWG results, finalize report. No main compilation while current root JVM runs.

## Ownership and recovery audit conclusion
- `AwgCablePart` shares reference-counted terminals by world-space endpoint AND an explicit channel. Same-plane endpoints, marked inner corners and opted-in outer ports use distinct channel identities; visual crossings alone do not join interiors. A knuckle owns its zero-volt VoltageSource; each paid arm retains its own AwgCableRun/resistor, thermal energy and ledger.
- `VoxelNode.connectJob` attaches AWG parts and tracks them in activeAwg; disconnect releases the knuckle source or run, then both terminal references. Last-user release removes the MNA load and point-map entry. Actual chunk unload deliberately calls neither path; dimension/node retirement does. The repeated test exercises retained identities and final real removal rather than treating intentionally resident simulation objects as leaks.
- Malformed run validation happens during part reading before terminal acquisition; inactive malformed-part tags remain owned by the voxel node for recovery. Existing persistence/orphan tests cover this; no recovery code was replaced.
- Placement checks/debits a copy of the live spool, applies splits before installation and undoes them on failure. Existing forced-failure, blocked tap and repeated-confirmation fixtures cover state/material preservation. New stale-funding assertion specifically checks confirmation against changed inventory.
- No AWG electrical/thermal solver or save schema changes were needed. No geometry features, registry IDs/specifications, pinned dependencies, historical snapshots or Qwen worktree files were changed.

## Parallel regression isolation
- Full root suite remains active after >13 minutes, with progress observed from Janet CPU stdlib to fluid-core tests and no reported failures.
- To continue server verification safely, `/tmp/eln-awg-isolated-game.gradle` now sets EVERY project's buildDirectory to its own `build/awg-isolated-game` and every dev run directory under that build output. `--project-cache-dir /home/matthew/.cache/eln-awg-game-project` isolates project task state too. These are external local runtime settings, not tracked/pinned configuration changes.
- This avoids compiling changed main classes into outputs used by the active root test JVM. Primary `build/classes` and primary subproject outputs remain untouched by the isolated server run.
- Focused funding test running in isolation: `./gradlew --project-cache-dir /home/matthew/.cache/eln-awg-game-project -I /tmp/eln-awg-isolated-game.gradle runGameTestServer -PelnGameTests=awg_native_kit_session`, log `/tmp/awg-reliability-funding.log`.
- Exact next actions: await focused funding result and root result independently; commit funding assertion if verified; run unfiltered GameTests with the same isolated output setup. Do not run a primary-output main compilation while root JVM remains active.

## Stale funding verified
- Isolated `awg_native_kit_session` run: exit 0, 2/2 required tests passed, 2m38s including cold isolated output compilation. New changed-spool assertion PASS, existing stale obstruction/rollback/replayed confirmation/branch placement assertions PASS.
- Full root build remains active in primary outputs, no reported failures yet.
- Next: full GameTest run in isolated outputs (no filters), then reconcile both final results.
