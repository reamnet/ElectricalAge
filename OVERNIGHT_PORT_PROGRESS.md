# OVERNIGHT_PORT_PROGRESS — ForkPatch 3.0.77 → NeoForge 26.3

Authoritative progress record. After ANY context compaction: read this file first,
identify the last completed milestone + current task, check ONLY the files needed,
and resume immediately. Do NOT re-investigate the environment (all details below).

## Current state (keep concise; update at every milestone)
- Start: 2026-11-17 08:36 UTC. Target: functional completion by ~12h; validation/fixes to ~18h.
- Repo: /home/matthew/projects/electrical-age-port/ElectricalAge
- Branch: port/forkpatch-3.0.77-26.3. **AHEAD of origin by 3 local commits — NOT pushed** (f078b7ca, bcbd7b2c, 70839aea).
- **HEAD: 70839aea** ("Use real source textures for the 27 panel/alarm/relay/vehicle/switch item icons").
- 3 in-flight UNCOMMITTED files (resume point): AlarmLab.kt, PanelCarryLab.kt (null-safe hardening), tools/mctest/mctest (RCON codec fix).
- **RCON VERIFIED LIVE** this session: `mctest server exec version` -> 26.3 protocol 777; `list` -> 0 players. Wide 12-byte codec (length i32, id i32, type i32) + 1.2s quiescence works vs the live dev server.
- **CLIENT-LAUNCH BUG (pre-existing, committed):** `mctest client start` passes `--args=--quickPlayMultiplayer ...`; NeoForge DevLaunch (net.neoforged.devlaunch.Main) does not honor `--args=` -> `ClassNotFoundException: --quickPlayMultiplayer`, BUILD FAILED in ~10s. Clean fix = build.gradle `runs.client.programArgs` (version-risky quickPlay arg form). PLAN: bounded fix to plain `runClient` boot (title screen) for boot/render verification; device GUIs stay visual-pending (need connected world + human visual QA).
- Server: RUNNING (runServer JVM 190731 + GradleDaemon 190594), RCON live 25575, game port 26035. STOP BOTH (kill runServer JVM `grep fml.modFolders` + GradleDaemon) before recompiling main.
- Baseline (green): Native GameTest 1470 sel / 1468 pass / 2 FAIL (golden_descriptors+golden_slots, HISTORICAL — keep documented, do NOT fix/regenerate/weaken) / 0 skip. Root :test 701 sel / 698 pass / 3 skip. CAD 15/15. 1004 ForkPatch cases pass. Provenance 1099 pass. Datagen repeat = 0 writes (3631 files).

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
- mctest: `alias mctest='python3 tools/mctest/mctest'`. State/logs tools/mctest/.state/; shots tools/mctest/shots/ (git-ignored). Build: `./gradlew --no-daemon --max-workers=2 :build :cad:test`; `:test` (NOT bare `test`) for root-only filters.
- Labs need a RESIDENT chunk (BlockPosArgument.getLoadedBlockPos). On the dedicated dev server, ensure via `/forceload add <x> <z>` (chunk) around the lab pos, run `/elnlab <sub> <pos>`, then `/forceload remove`. Lab re-runs at already-occupied positions fail (expected).
- Source (read-only, do NOT modify): ../ELN1.21.1ForkPatch @ b7444a7b8a0c5e072b1fd35eed2b74a282c3348c.

## Remaining work inventory (from wrap-up-handoff.md)
### Easier isolated slices (bounded, verifiable) — do first
- [ ] Missing source recipes/translations/tooltips/catalog for existing working adapters. (VERIFY which already exist before duplicating.)
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
### 2026-11-17 (this session, resume)
- Verified state: HEAD 70839aea, 3 ahead (not pushed), 3 uncommitted files, server live.
- RCON re-verified LIVE: `version`=26.3/proto777, `list`=0 players. Codec fix is CORRECT for this server.
- Reviewed in-flight diffs: AlarmLab/PanelCarryLab = null-safe placement + try/catch mains + failure reporting (defensive, changes only failure behavior). mctest = RCON 12-byte codec + quiescence (verified).
- Confirmed ModDev generates clientRunProgramArgs.txt from neoForge.runs DSL; clean client-args fix needs build.gradle programArgs (version-risky quickPlay). Deciding bounded plain-runClient fix.
- NEXT: commit 3 in-flight files locally -> certify artifact (stop server, clean build) -> validate labs live (/forceload) -> bounded client fix -> grind remaining work.

## Test results (record as performed)
- [x] RCON: `server exec version` -> 26.3; `list` -> 0 players (live, this session).
- [ ] /elnlab alarms + panelcarry re-run on hardened labs (needs clean build + /forceload).
- [ ] client boot to title + screenshot (bounded fix).
- [ ] ./gradlew build artifact + sha256 + commit identity.
- [ ] targeted :test + datagen re-run to confirm baseline after commit.

## Next actionable task
1. Commit 3 in-flight files LOCALLY (NO push). Then STOP server (kill runServer JVM + GradleDaemon) -> clean `./gradlew --no-daemon --max-workers=2 :build :cad:test` -> record sha256 + commit. Re-run `:test` + `runClientData` to confirm baseline (2 historical goldens only).
