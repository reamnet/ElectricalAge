# OVERNIGHT_PORT_PROGRESS — ForkPatch 3.0.77 → NeoForge 26.3

Authoritative progress record. After ANY context compaction: read this file first,
identify the last completed milestone + current task, check ONLY the files needed,
and resume immediately. Do NOT re-investigate the environment (all details below).

## Current state (keep concise; update at every milestone)
- **ACTIVE (2026-11-17, session resume from dcfa8883).** PUSH IS AUTHORIZED this session (user explicitly said push all local/ported work + list what's left).
- Repo: /home/matthew/projects/electrical-age-port/ElectricalAge
- Branch: port/forkpatch-3.0.77-26.3. **HEAD dcfa8883, 0/0 vs origin (clean, in sync).**
- This session's mission: port **Kitchen/Laundry/Fusion + complex manufacturing families** (14 devices: 11 kitchen @ group 124, washer/dryer @ group 125 sub 9/10, fusion @ group 125 sub 11, + complex electrical machines), commit+push each validated batch, then list what's left.
- **BASELINE (from dcfa8883, previously verified):** Native GameTest 1470 sel / 1468 pass / 2 FAIL (golden_descriptors + golden_slots — HISTORICAL 765-assertion delta, documented, do NOT fix/regenerate/weaken) / 0 skip. ForkPatch native 1004 pass. Root :test 698 pass / 3 pre-existing skips. CAD 15/15. Datagen repeat 0 writes (3631 files).
- Kitchen API-substitution table + ID table + execution plan: see checkpoint handoff summary (kept in this file's §This-session plan).

## Hard constraints (user-mandated; do not violate)
- NO push / merge / force-push / history rewrite. Local commits only. Preserve all prior work.
- Don't weaken goldens/assertions/skips; don't delete/disable tests; don't change expected values to match wrong behavior.
- Don't change pinned Gradle/wrapper/deps. Preserve paid materials, energy/heat, UUID/owner/save invariants, cached LU factors, resident-only queries.
- Don't compile changed main classes while a test/client JVM uses the same build outputs.
- Don't claim false completion. Label every item: not-started / partial / implemented / compile-verified / tests-passed / live-verified / visual-pending / blocked.
- Only test in disposable dev worlds. Don't modify personal files / production servers / unrelated repos.
- Fix infra (mctest) "enough to continue" only. Distinguish mock/stand-in RCON + desktop scrot (plumbing) from REAL in-game validation.

## Env (do not re-investigate)
- `source /tmp/eln-port-java-env.sh` -> JDK25 /home/matthew/jdks/jdk-25.0.5+8 (target; plain `java`), JDK21 /home/matthew/jdks/jdk-21.0.13+9 (source), GRADLE_USER_HOME=/tmp/eln-port-gradle. python3 3.14.4 (mctest stdlib-only). DISPLAY=:0 (real 1920x1080 cloud desktop). xdotool + scrot on PATH.
- RCON (run/server.properties): enable-rcon=true, port 25575, pw eln-local-dev-only, server-port 26035, online-mode=false, gamemode creative.
- mctest: `alias mctest='python3 tools/mctest/mctest'`. State/logs tools/mctest/.state/; shots tools/mctest/shots/ (git-ignored).
- Build: `./gradlew --no-daemon --max-workers=2 :build :cad:test`; `:test` (NOT bare `test`) for root-only filters; native GameTest via `runGameTestServer`.
- Labs need a RESIDENT chunk (BlockPosArgument.getLoadedBlockPos). Use `/forceload add <x> <z>` around the lab pos, run `/elnlab <sub> <pos>`, then `/forceload remove`. Lab re-runs at already-occupied positions fail (expected).
- Source (read-only, do NOT modify): ../ELN1.21.1ForkPatch @ b7444a7b8a0c5e072b1fd35eed2b74a282c3348c.

## Plan for THIS session (ordered; update as done)
1. [x] Clean `:build :cad:test` (log /tmp/eln-build-1.log).
2. [x] `runGameTestServer` (native 1470) -> 1468 pass / 2 historical golden fails / 0 skip.
3. [x] `runClientData` x2 -> 0 writes (datagen idempotence).
4. [x] Refresh docs/forkpatch-port/artifact-certification.json from d980ab76 (sha256 9fb9ab..., 9087/0/0).
5. [x] Live server-side lab validation via RCON (gallery, alarms, panelcarry, roadvehicles) -> artifacts under run/.
6. [x] Client boot to title + screenshot (render black — visual pending).
7. [~] Easier isolated slices: assessed as next-session work for a stronger/longer window; documented in overnight-handoff.md §7/§9.
8. [x] Final handoff docs/forkpatch-port/overnight-handoff.md + this file's final state.
## Remaining work inventory (from wrap-up-handoff.md; carried forward)
### Easier isolated slices (bounded, verifiable)
- [ ] Missing source recipes/translations/tooltips/catalog for existing working adapters (VERIFY which already exist before duplicating).
- [ ] More source-coordinate GUI matching for remaining classic variants.
- [ ] Additional bulb variants sharing proven physics.
### Medium
- [ ] Extend portable carrying to genuinely portable implemented families; additional appliance wrappers.
- [ ] Tools/configuration/copy flows; multicore manufacturing/routing + pole attachment/recovery cases.
### Hard
- [ ] Substations 8016-8019 + HV transformer/disconnect protection.
- [ ] Room thermal/humidity, HVAC, evaporative/growing/intakes, water/material/refrigeration.
- [ ] Kitchen/laundry/fusion + complex manufacturing families.
- [ ] Partial-block mounting, ghosts/picking, broad cable/consumer ownership.
### Hardest / broad (beyond one 12-18h window)
- [ ] Solver/shared-code parity; nonlinear/thermal/mechanical/fluid coupling; old-world/item/entity migration.
- [ ] Multi-client/lifecycle/perf, GPU/audio/human parity, progression/tutorial/worldgen, platform hooks, MQTT/Modbus/computer/addon bridges.

## Session log (append entries as work completes)
### 2026-11-17 (this session, resume from d980ab76; FINAL)
- Verified HEAD d980ab76, 4 ahead (not pushed). Java env OK.
- Clean `:build :cad:test` passed; native GameTest 1470/1468/2(hist goldens)/0; 1004 ForkPatch pass.
- Datagen idempotence: 0 writes (3631 files).
- Artifact certified from d980ab76: sha256 9fb9ab07..., 9087/0/0 (stale 8767 count corrected; that was c59e5471).
- Live server-side labs via RCON: gallery 227/620, 7 alarms, 2 vehicles, lighting gallery, devicevolts.
- Client boot to title captured (title-boot.png); render black in container (GL limit) — visual pending.
- Player-joined behavior blocked (0 players online; DevLaunch quickPlay flaky) — documented as pending, NOT fixed-by-waiting.
- Wrote docs/forkpatch-port/overnight-handoff.md (final deliverable); refreshed this file.
- NEXT (user): explicit push approval if/when desired; commit of docs+tooling is local-only.

## Test results (record as performed)
- [PASS] clean `:build :cad:test` (root 698/701 pass + 3 pre-existing skips; CAD 15/15)
- [PASS] runGameTestServer native: 1470 sel / 1468 pass / 2 fail (golden_descriptors, golden_slots — HISTORICAL, documented, not weakened) / 0 skip; 1004 ForkPatch cases pass
- [PASS] runClientData x2: 0 writes (3631 files)
- [PASS] live /elnlab server-side: gallery (227/620), alarms (7), roadvehicles (2), lighting gallery, devicevolts
- [PASS] client title boot + screenshot (render black — visual parity PENDING, human QA needed)
- [BLOCKED] player-joined live behavior / GUI / audio / multi-client / fresh-JVM save recovery (needs connected player + real GPU desktop; see overnight-handoff.md §6/§9)
