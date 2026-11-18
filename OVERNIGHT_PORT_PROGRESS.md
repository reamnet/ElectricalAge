# OVERNIGHT_PORT_PROGRESS — ForkPatch 3.0.77 → NeoForge 26.3

Authoritative progress record. After ANY context compaction: read this file first,
identify the last completed milestone + current task, check ONLY the files needed,
and resume immediately. Do NOT re-investigate the environment (all details below).

## Current state (keep concise; update at every milestone)
- **ACTIVE (2026-11-18, session resume from 72670385).** Local-commit only; do NOT push (external queue script handles pushing).
- Repo: /home/matthew/projects/electrical-age-port/ElectricalAge
- Branch: port/forkpatch-3.0.77-26.3. **HEAD 72670385 (start of session).**
- This session's mission: port **Lapis Dust (`lapis_dust`) + Diamond Dust (`diamond_dust`)** from ForkPatch 1.21.1; complete one item before the other; reuse the existing `registerDust` pattern; preserve functionality / electrical ratings / registry identity / texture / model / recipes / translations / tags / machine behavior; validate by level (compile → `:test` → datagen → GameTest → live smoke); commit locally with docs.
- **DONE:** both dusts registered — `eln:shared_item@579` (Lapis, subId=3) and `eln:shared_item@580` (Diamond, subId=4); textures byte-identical to source (sha256 verified); generated model/item/lang + `c:dusts/{lapis,diamond}` tags. `:test` 704/704 pass (proves mod constructs + both dusts register); `runClientData` idempotent (8 new writes → 0 on re-run); GameTest 1552 (only 2 historical golden fails); live `runServer` boots on MC 26.3 + RCON verified. In-game item presence = **mod-load-verified** (direct in-game summon was inconclusive — see test-results table). No target consumer recipe (source Inert Canister out of scope; target Small Solar Panel uses different materials — left unchanged).
- **BASELINE (prior session):** Native GameTest 1470 sel / 1468 pass / 2 FAIL (golden_descriptors + golden_slots — HISTORICAL, documented, do NOT fix/regenerate/weaken) / 0 skip. Root :test 698/701 pass + 3 pre-existing skips. CAD 15/15. Datagen repeat 0 writes (3631 files).
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
### 2026-11-18 (this session, resume from 72670385; FINAL)
- Verified working tree: 7 modified + 8 new files, all expected (Wp12aContent.java, eln-ids.json, lang, dusts index, 3 cache hashes; 2 textures, 2 models, 2 items, 2 tag files). No logs/screenshots/temp in the change set.
- Confirmed implementation parity vs source `../ELN1.21.1ForkPatch` (read-only @ b7444a7b): `registerDust(9)` registers Lapis (subId=3) + Diamond (subId=4); ore tags `dustLapis`/`dustDiamond`; display names "Lapis Dust"/"Diamond Dust"; textures `diamonddust.png`/`lapisdust.png` byte-identical (sha256 `2b9179…`/`1a051d…`).
- Registry IDs: `eln:shared_item@579` = Lapis (group 9 ×64 + 3), `eln:shared_item@580` = Diamond (group 9 ×64 + 4); `eln-ids.json` now 876 keys.
- Recipe parity decision (documented as limitation, NOT changed): source consumes Lapis in a Small Solar Panel recipe (`CraftingRecipes.kt:1284`) but the target's existing Small Solar Panel recipe (`ElnRecipes.java:1412`) uses different materials — left as working pre-existing functionality; source also consumes both dusts in Inert Canister (`CraftingRecipes.kt:1578`) — **Inert Canister out of scope, not ported**. Net: dusts are pure input items with no target consumer. No new recipes invented.
- Verification (each level labeled): `:build` BUILD SUCCESSFUL (compile-verified, /tmp/eln-dust-build2.log); `:test` 704/704 (tests-passed); `runClientData` ×2 = 8 writes then 0 (datagen-verified); `runGameTestServer` 1552 tests with 2 required historical golden fails (automated GameTest passed, /tmp/eln-dust-gametest2.log); `mctest doctor` HARD checks pass + `mctest server start` boots on MC 26.3, RCON live, `server exec version` → 26.3/protocol 777 (live smoke-test passed). In-game item presence via summon/`data modify` = INCONCLUSIVE (not claimed as passed).
- NEXT: stop the running mctest dev server; commit locally (no push); confirm clean tree; brief handoff.


## Test results (record as performed)
- [PASS] clean `:build :cad:test` (root 698/701 pass + 3 pre-existing skips; CAD 15/15)
- [PASS] runGameTestServer native: 1470 sel / 1468 pass / 2 fail (golden_descriptors, golden_slots — HISTORICAL, documented, not weakened) / 0 skip; 1004 ForkPatch cases pass
- [PASS] runClientData x2: 0 writes (3631 files)
- [PASS] live /elnlab server-side: gallery (227/620), alarms (7), roadvehicles (2), lighting gallery, devicevolts
- [PASS] client title boot + screenshot (render black — visual parity PENDING, human QA needed)
- [BLOCKED] player-joined live behavior / GUI / audio / multi-client / fresh-JVM save recovery (needs connected player + real GPU desktop; see overnight-handoff.md §6/§9)

### 2026-11-18 — Lapis Dust + Diamond Dust (verification level labeled per row)
- [PASS · compile-verified] `:build` BUILD SUCCESSFUL (log /tmp/eln-dust-build2.log)
- [PASS · tests-passed] `:test` 704/704 (0 fail, 0 err, 3 pre-existing skips) — proves the mod constructs and both dusts register
- [PASS · datagen-verified] `runClientData` ×2: 8 new writes → 0 writes on re-run (idempotent)
- [PASS · automated GameTest] `runGameTestServer`: 1552 tests, only 2 required FAIL = `eln:golden_descriptors` + `eln:golden_slots` (HISTORICAL, expected, not weakened); dusts do NOT appear in any failure (log /tmp/eln-dust-gametest2.log)
- [PASS · env OK] `mctest doctor` all HARD checks pass
- [PASS · live smoke-test] `mctest server start` boots on MC 26.3; RCON live; `server exec version` → 26.3 / protocol 777
- [INCONCLUSIVE · NOT claimed passed] in-game item presence via `summon`/`data modify` (NBT/armor_stand mechanics ambiguous; a bogus `@9999` was also accepted, so not discriminating). Registration is proven by mod-load (`:test` 704/704) + datagen, not by in-game summon.

