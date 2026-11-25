# AWG upstream integration progress

## Current checkpoint
- Task: audit original upstream delta, integrate compatible updates and six reliability commits, verify without changing source worktrees.
- Integration branch: `local/awg-upstream-integration`.
- Integration worktree: `/home/matthew/projects/electrical-age-port/ElectricalAge-awg-upstream-integration`.
- Local AWG source: `feature/awg-voxel-cables` at `7b91ee88416f5e4410a8e58eff744cdced523342` (three commits ahead of origin at task start).
- Reliability source: `local/gpt-awg-reliability` at `573af9324604ebf0555f147061e08e7af977b8f2`.
- Original upstream URL: https://github.com/Electrical-Age/ElectricalAge (remote `upstream`, NOT fork origin).
- Fetched original upstream `develop`: `2be56a18c59d1dd129bdb81d7ebee32ac0eb4c71` (November 25, 2026).
- Fork remote: https://github.com/reamnet/ElectricalAge (`origin`). No push/merge into feature or develop authorized.
- Six reliability commits verified: `1e2e7291d`, `182adb885`, `11a7839cd`, `2817ed508`, `75468da24`, `573af9324`.
- Worktree inventory: 38 worktrees, statuses read with optional Git locks disabled; Qwen inert_canister worktree has existing edits to Wp12aContent.java and eln-ids.json, preserved. New integration tree is clean before this document. Inventory in `/tmp/awg-integration-worktree-inventory.json`.
- Last verified action: original upstream fetch and isolated branch/worktree creation, exit 0. Initial status scan overlapped new checkout; rechecked new tree after checkout to remove transient status artifacts.
- Current HEAD: `7b91ee884`; uncommitted: this document only.
- Tests: pending; previous reliability baseline 1,992/1,995 server tests with three historical failures, not assumed for this integration.

## Exact next steps
1. Read applicable instructions and build/test guidance in this worktree.
2. Compute ancestry and upstream-only delta; build per-commit integration matrix before mutation.
3. Integrate reliability history without duplicates, then compatible upstream groups with attribution and deliberate conflict resolutions.
4. Compile/focused verification per group; full root/AWG/fluid/shaft/GameTest and twice datagen after stable integration.
5. Record results, remaining limits and final branch commit. Do not push or update feature/develop.

## Ancestry and merge decision
- Merge base: `29b2d2200f4c8bd301493d3f541abb5faaf07c56`; local feature has 226 commits and original upstream 229 commits beyond it. `git cherry` finds no exact patch-equivalent upstream commits among the missing 229.
- A three-way ancestry merge is appropriate; unrelated-history merge or wholesale source replacement is unnecessary. Preflight reports 36 conflicted paths, including generated outputs. Preserve original upstream commit ancestry/attribution and adapt conflicts locally.
- Upstream changes include fluid finite-state recovery/time/energy accounting, asynchronous fluid scheduling, shared route spec/placer and join rules, battery lead-acid behavior, shaft component/tag recipes, screen/Janet/GUI fixes, Ground Radar and distillation devices. Source and commit inventories are in `/tmp/awg-upstream-{commits,shortlog,files}.txt`.
- Critical registry collision found BEFORE merge: upstream Radar Goggles uses shared_item@7740; port already uses @7740 for Motor Cowling. Preserve Motor Cowling and assign the new-to-port goggles an unused ID; verify every old port mapping remains identical.
- Upstream removes working legacy 48V/230V heat turbines. Defer that removal to preserve port features and historical saves; keep their registrations/classes/assets/sounds and existing golden comparison allowances while retaining new upstream schematic steam turbine icons.
- Upstream adds an optional Inventory Profiles Next compile-only dependency solely for an ignore annotation. Preserve dependency pins/config: defer that dependency/annotation, keep the unrelated screen fixes from the same commit. Upstream non-version test-run options may be retained.
- Historical golden JSON snapshots are untouched upstream; golden README/source annotations describe intentional upstream battery/new-device behavior. Preserve snapshot contents and do not add skips to hide failures.

## Preliminary integration matrix
| Area | Classification | Decision and checks |
| --- | --- | --- |
| Common history through 29b2d2200 | Already present | Keep shared ancestry; no duplicate replay |
| Six reliability commits | Safe to integrate | Merge exact history first; focused preview/AWG tests |
| Fluid finite-state recovery, tank volume/energy/layer accounting, broken junction deletion | Safe; additional testing | Integrate author chain through tip; unit/replay/pipe/tank GameTests |
| Fluid scheduling/budgets/quarantine and diagnostics | Safe; additional testing | Preserve author timing contracts, test background and long plants |
| Shared KitPlacer/KitSpec routing and join rules | Requires adaptation | Carry AWG cached preview/payment/rollback/tap behavior and unique worker generations into shared path |
| Shaft composition and plate-tag recipes | Requires adaptation | Retain ForkPatch additions; regenerate intended author recipe changes twice |
| Battery lead-acid curve and finite NBT reads | Safe; additional testing | Keep author physical model and tests, preserve save compatibility |
| Radar/still/coil/breather additions | Requires adaptation | Resolve goggles numeric collision; preserve old mappings and test new devices |
| Legacy heat turbine removal | Intentionally excluded | Keep functional port devices, registrations, sound and assets |
| Optional IPN dependency/annotation | Deferred | No pinned dependency changes; independent UI fixes still integrate |
| Existing Arc Furnace/ForkPatch/AWG content | Already local; protected | Preserve local deltas during auto-merges and compare registry/assets after datagen |
| Historical snapshots | Protected | No JSON changes or weakened tests |

## Reliability history checkpoint
- Merge commit `917d55a114a289d2df98b3924dc7554139276951` contains all six exact reliability commits, with no conflicts or duplicate replay.
- `GRADLE_USER_HOME=/home/matthew/.cache/eln-awg-gradle ./gradlew :test --tests '*Awg*Test' --tests '*KitPreviewLifecycleTest' --tests '*KitOutlineTest'`: exit 0, 39 tests, 45s. Log `/tmp/awg-integration-reliability-tests.log`.
- Upstream review confirms canonical controller is `RoutePlacer` / `RoutePlacer.Spec`, not separate cable/pipe plans. AWG must become a spec-backed paid-conductor path with atomic debit and cached-route revalidation; retaining two independent plan maps would discard upstream's intended shared UX.
- Goggles ID @7740 conflicts with Motor Cowling; @7741 is Heavy Winding. New goggles will use the free @7742 slot, keeping every previously published port ID untouched.
- Next: merge original upstream with `--no-commit`; resolve deliberate compatibility adaptations and compile before committing the upstream group. No test JVM currently uses this worktree's outputs.

## Upstream merge adaptation checkpoint
- Merge is IN PROGRESS (`MERGE_HEAD` original upstream 2be56a18c); do not abort/reset. HEAD before merge is `8375a0c42`; merge not committed until compiled/focused checks pass.
- All textual conflicts resolved and staged. Original upstream histories retained. Detailed 229-commit matrix: `docs/forkpatch-port/awg-upstream-integration-matrix.tsv` (verification status provisional until final runs).
- Shared routing: added `AwgRouteSpec`, using upstream RoutePlacer's one plans map, lapse/undo/waypoints and native Router. AWG backend retains explicit endpoint taps, descriptor profiles, cached-route live validation, exact held-spool identity and atomic self-debit (generic kit.take must never debit AWG a second time). Generic cable/pipe JoinRun improvements remain upstream's.
- Preview: upstream shared job path plus reliability's globally unique generations, forget/invalidate, dimension guard and main-thread AWG layout/payment validation; upstream last-note cleanup preserved. Existing reliability tests retained, native session test now reads shared RoutePlacer.PLANS.
- Laying: combined upstream prevalidated pipe joins with local try/finally rollback, failure injection, host restoration and preservation of preexisting empty voxel nodes. AWG split undo remains in AwgCablePart.
- Grid: port multi-wire terminal/channel publishing and geometry caching retained; upstream LOS adapted to every rendered conductor and ghost exclusions, upstream span culling bounds calculated from those same published wires. Existing custom cable policy not replaced by upstream's older two-wire assumptions.
- Other conflicts: retain ForkPatch render registrations with upstream shared primitive/front renderers; keep I-V solar assertion rather than replacing with old constant-power assertion; combine recessed-outlet restoration/drop behavior with upstream onSelfDestruction ghost cleanup; node block keeps cord handling and upstream kit routing to pole foot.
- Registry invariant checked: all 928 original mappings remain byte-value equal; exactly six additions (Hot Blade Set, Radar Goggles @7742, two radars, still pot and coil). Arc Furnace named paths have no changes from pre-merge HEAD. Dependency diff contains test-run options only, no pinned versions/new dependency.
- Generated recipe/cache conflicts temporarily retain local files pending authoritative datagen. Sound/OBJ manifests union preserves port entries and adds upstream entries; new upstream native oil icons replace obsolete fallbacks while custom work-light/rated-supply icons remain.
- Current command: `./gradlew compileJava compileTestJava`, isolated Gradle home, log `/tmp/awg-integration-upstream-compile.log`, exec session 56043. No client/test JVM yet.
- Exact next action: inspect compile exit/errors, repair API adaptation errors without weakening tests, then focused unit and AWG/shared-routing/fluid/shaft GameTests before committing upstream merge. Complete registry/source policy audit and twice datagen after compilation.

## Compile and focused unit verification
- Compile attempt 1: exit 1, upstream removed Batch1Cases.field and PowerInductorSixRender still referenced by ForkPatch fixtures/render. Adapted callers to TestContext.field and VariableInductorRender to PowerPrimitiveSixRender; preserved all fixture assertions and mesh behavior.
- Compile attempt 2: exit 1, two AWG legacy-selection references to moved CablePlacer.IDLE_TICKS. Updated to canonical RoutePlacer.IDLE_TICKS (no behavior change).
- Compile attempt 3: `compileJava compileTestJava`, exit 0, 40s (`/tmp/awg-integration-upstream-compile3.log`).
- Focused unit command (see `/tmp/awg-integration-focused-unit.log`): root AWG, preview lifecycle/codec, CoreStepNonFinite, FluidNodeHeal, TankShareLayers, TankGroupShare, ArcFurnace and VariableInductor filters plus `:fluidcore:test`; exit 0, 1m07s. Root 55 tests / 0 failures / 0 skips; fluidcore 104 tests / 0 failures / 2 existing skips.
- Legacy heat policy includes retaining its old powered TurbineCase rather than the upstream RemovedHeatTurbine cases that require unsupported IDs. Kept two legacy hidden entries and adjusted JEI hidden count to 16 (+ absent Stasis anchor), matching four newly hidden generic cable coils and two retained legacy devices. No reachability assertion relaxed and historical snapshots untouched.
- Restored 10 legacy turbine model assets as well as original icon and sound-source assets. Pinned dependency set unchanged.
- Current run: first `runClientData`, exec session 46588, log `/tmp/awg-integration-datagen1.log`, isolated Gradle home. No class edits/compilation until its JVM exits.
- Next: inspect datagen errors/output and registry invariants; run focused AWG/shared-join/battery/fluid/shaft GameTests; correct real failures; run second datagen and compare generated trees; commit verified upstream merge, then full root/full server checks. All source edits remain confined to integration tree; merge still in progress.

## Runtime and datagen checkpoint
- First `runClientData`: exit 0, 22s. 4,264 generated files including cache; 93 tracked generated paths differ from pre-merge local HEAD. No AWG asset or Arc Furnace generated path changed. Shaft recipe variants replaced by upstream part/tag recipes as intended; new device resources generated from registered descriptors.
- Cache files have volatile timestamp headers; second-run comparison must report raw and header-normalized cache differences separately, while requiring strict byte equality for every asset/recipe/lang output.
- Focused real server command selected 39 AWG cases, 14 generic cable/pipe join cases, 4 lead-acid battery cases, and 8 shaft cases (IDs saved in `/tmp/awg-integration-focus-ids.txt`): exit 0, 37s, all selected cases observed and passed. Log `/tmp/awg-integration-focused-game.log`.
- All three actual chunk evictions pass after shared controller integration. Original no-double-debit, stale preview, spool-funding, off-center/sag/corner tap and rollback assertions remain active and pass.
- Documentation now explicitly states legacy heat retention instead of upstream-only removal/orphan expectations. Historical golden JSON remains unchanged.
- Exact next steps: run second datagen and compare all generated outputs; review/summarize the full staged upstream delta and matrix; commit upstream merge after these checks. Then run full root/build and unfiltered GameTests sequentially (new upstream wall-time fluid budget makes CPU contention undesirable), inspect actual failure baseline, and finish integration report. No server/test JVM remains active from focused runs.

## Upstream merge ready for local commit
- Second `runClientData`: exit 0, 22s. SHA-256 comparison of all 4,264 files is identical to first run, INCLUDING cache files; no normalization/exclusion needed. Idempotence proven byte-for-byte.
- Arc Furnace wildcard did not match a standalone unit suite (its coverage lives in world GameTests). Corrected verification explicitly: all 25 `old_arc_furnace_*` cases, retained `wp10b_turbine`, and upstream `wp11_pole_span_needs_line_of_sight` observed and passed; 28/28 total with harness case, exit 0, 30s. Log `/tmp/awg-integration-arc-focused-game.log`.
- All 928 existing manifest mappings remain identical; six deliberate additions. No AWG/Arc generated path changed; no historical snapshot JSON or dependency/wrapper pin changed. Generated shaft recipe changes reviewed against upstream c62c880c3/a2eeebfbb intent (component stages/windings and common plate tags, replacing numbered old alternatives).
- Detailed matrix covers all 229 upstream commits; per-commit full-regression status remains pending. Compatible author changes retained with common ancestry; partial exclusions are explicit for legacy heat retirement and optional IPN annotation/dependency.
- Next: commit merge with these focused checks, run `build :test` unfiltered, then full `runGameTestServer` without filters and without concurrent CPU-heavy tests (upstream fluid budget now uses real wall time). Record actual new totals/failure names; fix any new regressions and reverify relevant scope.
