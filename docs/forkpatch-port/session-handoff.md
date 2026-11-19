> Resume from implementation `904059663c5085778a55c675b5e60461fd3cfd4d` or newer documentation commits on the same PR branch. Read [expanded-systems handoff](appliances-substations-bulbs-fans.md) first.

> Final expanded-systems verification: 1536 native selected/1534 passed/2 historical golden failures/zero skips; all1070 ForkPatch cases and all66 added cases pass. Root698pass/3 unchanged optional skips; CAD15pass. Datagen repeats with0 writes. Golden832 is recorded without expectation changes. Live GUI/audio/multiplayer acceptance remains pending. See [current feature handoff](appliances-substations-bulbs-fans.md).

> **Expanded systems continuation:** Kitchen/laundry/fusion, substations8016–8019 plus7875 prerequisite, all18 source bulbs, and four standalone fans now have native implementations. See [implementation and acceptance details](appliances-substations-bulbs-fans.md) and `expanded-systems-validation.json` for current evidence; historical not-implemented notes below predate this slice.

> Final wrap verification:1470 native selected,1468pass,2 historical golden failures; all1004 ForkPatch cases pass. Root698pass/3 existing skips, CAD15pass, datagen repeat0 writes. See `requested-systems-validation.json`.

> **2026-11-17 wrap-up:** See [current handoff](wrap-up-handoff.md) for implemented panel/carrying, all seven alarms, road vehicles/charger/lead, test evidence and remaining difficulty. Historical “not registered” notes below are superseded by that checkpoint; pending live GUI/gameplay checks remain explicit.

# Handoff — lamp completion and review since bb962e1

## Active requested systems continuation

User request: finish the12-circuit panel, road vehicles with their240V100A
station/plug-in lead, fire alarms, and portable carrying where supported.
Panel/carrying slice is implemented; see `breaker-panel.md`, `portable-carry.md`
and `panel-carry-validation.json`. All962 ForkPatch cases pass, then58 targeted
cases pass after the voxel-preservation guard and live-scene helper. Root698
pass/3 unchanged skips; CAD15/15. Datagen repeats with zero writes;1055 import
entries pass. Golden746 retains all714 old exact assertions, adding32 identities/
menus. Earlier unfiltered1424-case run exposed8 non-golden issues; passing
subsequent ForkPatch tests cover their fixes. Final unfiltered rerun remains due.

Fire alarms are implemented: see `fire-alarms.md` and
`fire-alarm-validation.json`. All976 ForkPatch cases pass; root698pass/3skips,
CAD15/15, datagen repeat0,1085 provenance entries pass. Goldens761 preserves
all746 earlier assertions. Live GUI remains pending.

Road vehicles, station and lead are integrated and pass28/28 native cases. See `road-vehicles.md`, `road-vehicle-validation.json` and the wrap-up handoff. Do not restore temporary prepared code.

Live GUI is pending. RealDISPLAY`:0` works through `tools/mctest/mctest`; do not
attemptXvfb in this environment. `/elnlab panelcarry <pos>` constructs paid native
panel/UPS/table-lamp hardware; `/elnlab devicemenu7988` opens the nearest panel
from a player context. Use a private new server world; preserve existing worlds.
Do not compile while client/test JVMs use the shared build outputs. The previously
certified lamp-only jar is stale for these new changes; rebuild/certify at the end.

The standalone test-tooling commit`dd3cbee2` has been pushed to the existing PR
branch as reamnet. Preserve draftPR1 title/base and all earlier commits.

User requested wrapping up and pushing all work on 2026-11-16. Continue in draft
PR #1 on `reamnet/ElectricalAge`, branch `port/forkpatch-3.0.77-26.3`, base
`ports/26.3`. Keep PR title/base/draft unchanged. Use the GitHub connector as
**reamnet**; its profile was verified. Local gh authentication is broken.
Fetch and inspect the current HEAD and local edits before doing anything.

Repository: `/home/matthew/projects/electrical-age-port/ElectricalAge`.
Pinned read-only source: `../ELN1.21.1ForkPatch` at
`b7444a7b8a0c5e072b1fd35eed2b74a282c3348c`. Source remains clean.
Starting HEAD this session was `973bf4626483452f73d8a130cc86a73709d2968d`;
there were no newer remote commits at the final fetch before publication.
Initial untracked fan/suspension models/icons were preserved and incorporated.

## Read first

- `review-since-bb962e1.md`: findings across all 14 commits / 935 files / 20 code files.
- `decorative-lighting.md`, `decorative-lighting-validation.json`.
- `remaining-work.md`, `port-completion-plan.md`, `lighting-port-matrix.md`.
- `compact-ups.md`, `imports.json`, `import-audit.json`.

## Implemented this session

- All 108 decorative fixture registrations, including motion4126, fans4388–4391
  and4408, adjustable warehouse4409. IDs use explicit source ordinals/ranges.
- Source LightAim, per-head housing/beam transforms, wide beams, original item
  poses, banker/party/rig colors and complete models. Emissive threshold is8.
- Motion modes with nonconflicting event16, real resident living-entity/LOS
  sensing and physical30s hold. Native event4 remains the channel string.
- Separate paid fan motors; physical lead required; startup/coast, rotor sound,
  industrial blade hazard, save/publication. Motors attach to solver pins only
  while registered, preventing an orphan-component reload crash found in testing.
- Warehouse0–7m quarter-metre suspension, matching source-sized menus/controls.
- Water-preserving native light leases with loaded-only/build-height guards.
- Corrected all108 source recipes:103 shaped+5 mini-dot shapeless, including47
  full upgrades and all20 outdoor recipes. Earlier uncraftable-outdoor claims
  were wrong. Independent recipe expectations reject ingredient underpayment.
- Actual source String Lights4418, paid15W120/240V, original assets, wall pose,
  source-sized GUI/inventory, saves and real wireless electrical connection.
  Source disconnect synchronously removes receiver links. Nearest-OFF source
  blocks a farther enabled source. Paid cords auto-select their120/240V rating.
- Decorative fixture cord input and exact source household plug compatibility;
  physical input suppresses wireless double-billing. Portable carry remains open.
- Shared wireless supply selection now rejects foreign dimensions/stale owners.
- Completion generator repaired; all7132 source paths /37 workstreams retained.
  Import ledger1044 entries passes, including all432 fixture models/icons.

## Validation: do not confuse completed and pending runs

Completed baseline at973bf462:
- Root693 selected690passed3existing optional skips; CAD15/15.
- Native1393 selected1391passed0skips; all927 ForkPatch cases passed.
- Historical goldens alone failed, with697 exact failed assertions.

Completed intermediate full build this session:
- `/tmp/eln-lamps-full-build2.log`: root700 selected697passed3same optional
  skips; CAD15/15. This predates the last GUI/cord-test/lifecycle refinements.
- Datagen repeated writes zero files, including the current validation invocation.

Completed intermediate native run:
- `/tmp/eln-lamps-native-all-v4.xml` and `/tmp/eln-lamps-native-all-v4.log`:
  1413selected1407passed6failed0skips;947ForkPatch cases,4failed.
- All589 catalog devices publish/draw; all246 server menus pass.
- Two failures are unchanged historical golden tests:714assertions, preserving
  every prior697 exactly and adding17 for seven fixtures+String Lights.
- Other failures: missing4x4x4/10x6x10 test templates prevented exact-recipe and
  two String Lights tests from starting; added templates fix their setup.
  New fan-coast test timer was appended after connect and never registered;
  fixed explicit slow-process registration/removal without relaxing assertions.
- The earlier focused run crashed on the orphan fan component. Its production
  fix is in place; v4 ran fully without that crash. Do not count crashed runs as passes.

FINAL v6 validation completed successfully except for the two unchanged
historical golden identity/reference failures:
- Root 701 selected, 698 passed, 3 existing optional skips, 0 failed; CAD 15/15.
- Native 1416 selected, 1414 passed, 2 historical golden failures, 0 skipped;
  all 950 ForkPatch cases passed. All 589 catalog devices and 246 server menus
  passed; datagen repeated writes were zero.
- 714 exact golden assertions preserve the previous 697 and add 17 across seven
  fixtures and String Lights. No assertion/tolerance/skip was weakened.
- Logs/XML/review: `/tmp/eln-lamps-all-validation-v6.log`,
  `/tmp/eln-lamps-native-all-v6.xml`, `/tmp/eln-lamps-native-review-v6.json`.
  See `decorative-lighting-validation.json` and `decorative-lighting-golden-delta.json`.

Compile of the final GUI helper and String Lights heading fix succeeded. Games
were stopped before compilation and after the interactive run. Clean JAR
certification is recorded below and in `artifact-certification.json`.

Run `tools/port/review_native_results.py` on the final XML+log. Resolve any
non-golden failure, retaining every assertion/tolerance/skip. Update the lighting
validation/golden delta and PR body with actual final counts. New recipe, sensor,
cord and String tests must really execute; structure-placement failures are failures.

## GUI / real game verification

Live dedicated server and actual Minecraft client connected over loopback under
Xvfb `:97`/Mesa llvmpipe. The private world `/tmp/eln-lamps-server/world` was
preserved; do not reset it. `LightingLab` built 108/108 fixtures with zero
placement failures. Helper lookup now selects the nearest resident duplicate
and uses the connection-aware server teleport. Screenshots and exact partial
coverage are listed in `decorative-lighting-validation.json`.

Verified actual aim pan/reset/head selection, motion on/off, fan on/off, warehouse
suspension adjustment, and String Lights placement/panel voltage/channel. After
save and dedicated-server restart the String Lights GUI displayed the retained
240 V and `gui test channel`. Fixed source title overlap; screenshot shows a single
`Holiday lighting` heading. The gallery and these GUI checks are representative,
not exhaustive of all 108 panels. Two-client behavior, hardware GPU and audio were
not verified. Do not claim all client menus pass solely from server menu tests.

Operator commands: `/elnlab lamps x y z` builds the 108-fixture scene;
`/elnlab lampmenu LOOKUP` opens the nearest resident fixture menu. The world,
logs and screenshot sources are under `/tmp/eln-lamps-*`; no system binaries or
project pins were changed.

## Toolchain / publication

`/tmp/eln-port-java-env.sh` points to Java25 and Java21 in`/home/matthew/jdks` and
Gradle cache`/tmp/eln-port-gradle`. Keep pinned Gradle/wrapper/dependencies intact.
Use`:test` for root filters, not bare`test` (would also filter CAD).

Clean artifact certification completed from published code commit
`c59e547138e53409f7a1bd282175ec25e16bb74d`. `:jar` succeeded with a clean
worktree. All 8,767 compiled class/resource entries match packaged bytes exactly;
the only additional entries are the manifest and the declared commons-math3 and
Kotlin stdlib jar-in-jar dependencies. Marker is `c59e547138e5`; artifact SHA256
is `c19d8227540b495a9033a404740a33b12c5e7747446fb36a69d6f7b6c223cab8`.
Full evidence is in `artifact-certification.json`. This records a local build;
the JAR itself is not attached to the repository or release.

All work is being published to the existing draft branch at user request,
including the completed native/client verification and its stated limits. Confirm remote HEAD equals local
and worktree clean. GitHub git-data tools support blobs/trees/commit/update_ref
with expected-SHA lease; preserve reamnet authorship and PR metadata.

## Still unfinished beyond fixture adapters

Portable carry, partial-block mounting/picking review, additional bulb technologies
and source technology/lifetime/configuration, wider save/source-world conversion,
hardware GPU/audio parity and human playtesting. The37-workstream plan remains
open;108 registrations do not mean the whole fork is complete.
