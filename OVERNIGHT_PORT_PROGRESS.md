# OVERNIGHT_PORT_PROGRESS — ForkPatch 3.0.77 → NeoForge 26.3

Authoritative progress record. After ANY context compaction: read this file first,
identify the last completed milestone + current task, check ONLY the files needed,
and resume immediately. Do not re-investigate the environment.

## Current state (keep concise; update at every milestone)
- Start: 2026-11-17 08:36 UTC. Target: functional completion by ~12h; validation/fixes to ~18h.
- Repo: /home/matthew/projects/electrical-age-port/ElectricalAge
- Branch: port/forkpatch-3.0.77-26.3. AHEAD of origin by 2 local commits (f078b7ca, bcbd7b2c) — NOT pushed.
- HEAD: bcbd7b2c (Fix mctest server stop args reference). Working tree had 27 improved textures + mctest RCON fix (uncommitted).
- RESUME POINT (this session): verified texture slice on disk. 27 of the 29 item icons were upgraded from
  flat placeholders (committed in f078b7ca) to the REAL source block/item textures (copied byte-exact from
  read-only ../ELN1.21.1ForkPatch). Verified 6 sampled icons MATCH source; all resolve via ElnModels itemTexture
  ("item/<id>.png" checked first). 
- VERIFY (this session): runClientData -> exit 0, NO "no texture" WARN, HashCache written=0 (idempotent, 3631 files).
  Root :test -> 701 tests / 0 fail / 0 err / 3 skip / 698 pass (baseline holds, no regression).
- NEXT: commit the texture improvement + mctest RCON codec fix + this progress file (local only). Then test RCON
  codec fix against the dev server; if it unblocks RCON, do live validation via /elnlab + screenshots.
- RCON: codec fix is SPEC-CORRECT (Source RCON = length(i32) id(i32) type(i16); old code swapped id/type).
  Whether it unblocks the real dev-server RCON thread is UNTESTED this session (see RCON note).
- Mod LOADS: datagen boots + writes 0. Native GameTest server boots (per wrap-up-handoff).
- Baseline (re-confirmed THIS session where noted):
  - Native GameTest whole-run: 1470 selected / 1468 pass / 2 FAIL / 0 skips (2 fails = golden_descriptors + golden_slots, HISTORICAL goldens — keep documented, do NOT fix/regenerate/weaken).
  - Root :test: 701 sel / 698 pass / 3 skip (RE-RUN this session, green). CAD 15/15.
  - All 1004 ForkPatch cases pass. Provenance 1099 pass. Datagen repeat = 0 writes (RE-RUN this session).
- JAR: build/libs/eln-26.3-0.1.0-port.jar (built earlier; re-certify with a fresh ./gradlew build before any claim).
- KNOWN BOUNDED GAP (now CLOSED): 29 item textures — 27 upgraded to source textures this session; 2 vehicle icons (electric_golf_cart, arc_runner_coupe) already matched source in f078b7ca.
## Hard constraints (user-mandated; do not violate)
- NO push / merge / force-push / history rewrite. Local commits only. Preserve all prior work.
- Don't weaken goldens/assertions/skips; don't delete/disable tests; don't change expected values to match wrong behavior.
- Don't change pinned Gradle/wrapper/deps. Preserve paid materials, energy/heat, UUID/owner/save invariants, cached LU factors, resident-only queries.
- Don't compile changed main classes while a test/client JVM uses the same build outputs.
- Don't claim false completion. Label every item: not-started / partial / implemented / compile-verified / tests-passed / live-verified / visual-pending / blocked.
- Only test in disposable dev worlds. Don't modify personal files / production servers / unrelated repos.
- Don't rewrite working infra (mctest); only fix enough to continue.
- Distinguish mock/stand-in RCON + desktop scrot (plumbing) from REAL in-game validation.

## Remaining work inventory (from wrap-up-handoff.md)
### Easier isolated slices (bounded, verifiable) — do first if time permits
- [ ] Fix 29 missing item textures (panel, alarms, vehicles, breakers/relays/switch): copy from read-only source ../ELN1.21.1ForkPatch; verify datagen WARN gone + items render in-game.
- [ ] Missing source recipes/translations/tooltips/catalog for existing working adapters.
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

## Dependencies
- Live validation (Phase B) is REQUIRED + independent; server boots (doctor green).
- Artifact cert (Phase C) needs a clean ./gradlew build from HEAD + sha256 + commit identity.
- Texture slice (Phase D) needs read-only source textures + datagen re-run; independent of B/C.
- Hard features (Phase E) are the real long tail; most beyond this window.

## Session log (append entries as work completes)
### 2026-11-17 (resume session) — texture slice verified, baseline re-confirmed
- Reconciled state: HEAD is bcbd7b2c (NOT bda7db63 as this file previously claimed). f078b7ca already
  committed the 29 item icons (flat placeholders). Working tree had 27 of them upgraded to REAL source
  textures + the mctest RCON codec fix (uncommitted). electric_golf_cart + arc_runner_coupe already matched source.
- Verified 6 sampled item PNGs are byte-identical to their read-only source block/item textures (MATCH).
- runClientData: exit 0; NO "ElnModels: no texture" WARN; HashCache written=0 (idempotent, 3631 files stable).
- Root :test re-run: 701 tests / 0 fail / 0 err / 3 skip / 698 pass (baseline holds; resource-only change, no logic impact).
- mctest RCON codec: confirmed spec-correct (Source RCON length/id/type). Still to be tested vs live dev server.
- STATUS: texture slice = tests-passed (datagen + :test green). Live-verification of the icons in-game = PENDING.
### 2026-11-17 08:36 UTC — baseline verified on disk
- HEAD bda7db63 in sync w/ origin; clean tree; doctor HARD green; mod loads (datagen 0 writes).
- Confirmed /elnlab helpers exist in LabCommands.java: gallery, roadvehicles, roadmenu, alarms,
  panelcarry, devicemenu, lamps, lampmenu, voxelgallery, burn, cables, devicevolts, netdump,
  remove, reconnect, heat, tilted, threelogger, fluidtrace, inspect, machines.
- RCON: enabled, port 25575, password eln-local-dev-only, server-port 26035, online-mode off, creative.
- Found bounded gap: 29 items missing textures (see KNOWN BOUNDED GAP).

## Test results (to record as performed)
- [ ] mctest server start + `server exec version` / `list`
- [ ] /elnlab: panelcarry, alarms, roadvehicles, devicemenu, roadmenu, gallery, lamps, devicevolts, burn, cables, netdump (capture artifacts)
- [ ] client start + screenshots of panel/alarms/vehicles/lamps GUIs (to tools/mctest/shots)
- [ ] fresh-JVM save recovery (stop+restart, verify persisted state)
- [ ] 2-client concurrent test
- [ ] ./gradlew build artifact + sha256 + commit identity
- [ ] targeted :test re-run to confirm no new regression (expect only 2 historical goldens fail)

## Next actionable task
1. Live validation: `source /tmp/eln-port-java-env.sh && python3 tools/mctest/mctest server start`.
   Then `server exec version`, `server exec list`, then run the /elnlab helpers and capture output + artifacts.
   Record results (screenshots, logs, artifact paths) in this file and in a validation JSON under docs/forkpatch-port/.
