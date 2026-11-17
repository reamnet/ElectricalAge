# Wrap-up and resumable handoff — 2026-11-17

## Copy/paste continuation

Continue draft PR1 on reamnet/ElectricalAge, branch `port/forkpatch-3.0.77-26.3`, base `ports/26.3`. Preserve title/base/draft state. Inspect `git status`, fetch origin and inspect newer commits before editing. Never clobber local edits. User has authorized validated commits/pushes as reamnet; local git push worked during this session. This is an unfinished whole-mod port, not a release certification.

Repository: `/home/matthew/projects/electrical-age-port/ElectricalAge`. Read `AGENTS.md`, `CLAUDE.md`, `docs/testing.md`, this handoff, `remaining-work.md`, `port-completion-plan.md`, `compact-ups.md`, and each requested-system validation file. Source is read-only `../ELN1.21.1ForkPatch` at `b7444a7b8a0c5e072b1fd35eed2b74a282c3348c`.

Run `source /tmp/eln-port-java-env.sh`; JDK25 targets the port, JDK21 is for source. Do not change pinned Gradle/wrapper/dependencies. Use `:test` for root filters. Never compile main classes while a client/test JVM reads the same outputs. Never weaken goldens, assertions, tolerances or skips. Preserve paid materials, energy/heat, UUID/owner/save invariants, cached LU factors and resident-only queries.

Recent pushed checkpoints: `dd3cbee2` standalone test tools; `c2a61876` twelve-circuit panel + carrying; `f4ed2b6c` seven fire-alarm devices. Road vehicles and the initial wrap-up were committed/pushed as `2f9cbefd`; later documentation commits record final evidence. Use `git log -5` for exact SHAs. Do not restore prepared `/tmp` vehicle code over the integrated implementation. No coding subagent remains active.

## Implemented scope and evidence

| Area | Implemented scope | Evidence / remaining qualification |
|---|---|---|
| Lighting | All108 decorative styles, fans, motion, aiming/suspension/underwater adapters, String Lights4418, source LEDs302/303, recipes and byte-identical original assets | Lighting matrix and decorative-lighting docs; prior software-rendered lighting GUI checks exist. More bulb technologies, lifetime/configuration and broader visual parity remain. |
| Poles/wires | Steel transmission poles/risers8014/15/20/21,30 distribution variants, paid spans, streetlights, services/protection; native100-wire catalog and nine cord ratings | Existing pole/service/cord native suites. This does not mean every source cable consumer, routing/mounting or substation is complete. |
| Manufacturing/power | Fabricator4480, roller4608/insulator4609/combiner4610, source prerequisites, renewables/storage/ATS/UPS/compactUPS, converter/drive/SCADA adapters; forklift/pallet/pack charger/welder | Existing production tests and remaining-work feature inventory; adapter coverage is not blanket source parity. |
| Twelve-circuit panel7988 | Three buses,12 real branches, typed breakers5–400A, thermal trip/reset, channels/dusk controls, exact312×386 layout, source-rated switch/relay recipe prerequisites | `breaker-panel.md`, `panel-carry-validation.json`;698 root pass/3 existing skips, CAD15 pass;962 ForkPatch pass then58 targeted after final guard. Live GUI pending. |
| Portable carrying | Existing table-lamp families and compactUPS, paid cord release, owner snapshot/energy/inventory preservation, creative duplication guard, voxel sibling preservation | `portable-carry.md`; not all future appliances/fans are portable yet. |
| Fire alarms8053–8059 | All seven devices,120V panel, real battery/24V field network, latch/silence/reset/test, smoke/radiant heat detection, assets/sounds, source300×382 menus | `fire-alarms.md`, `fire-alarm-validation.json`:976/976 ForkPatch pass, root698pass/3skips, CAD15. Room climate adapter is absent: source20C fallback plus actual radiant sources. Live GUI/audio pending. |
| Road vehicles | Cart/coupe, seats/driving/hazard, native inventory/save/render/control,240V100A station8079 and reusable lead7494, source recipes | `road-vehicles.md`, `road-vehicle-validation.json`:28/28 native pass. Live driving/GUI/multiplayer/fresh-JVM recovery pending. |
| Test tools | `tools/mctest/mctest`: doctor, RCON, server/client, lab, simulation, logs, HTTP, regression | Doctor hard checks pass. Earlier infrastructure stand-in tests are not gameplay tests. Real native GameTest server now boots; old panel model-load blocker is resolved. |

Native GameTests run Minecraft server code and are real in-engine automated tests. They do not establish human gameplay, client GUI or audio parity. Original models and source coordinates do not establish universally identical rendering.

## Validation and release limits

Final whole-run: **1470 selected,1468 passed,2 failed,zero skips; all1004 ForkPatch cases pass**. Only `golden_descriptors` and `golden_slots` fail. Exactly765 assertions are recorded; all761 prior differences remain and four were added by vehicle-station identity/menu coverage. No non-golden assertions fail. Expectations were never regenerated or weakened. Root701 selected/698pass/3 unchanged optional skips; CAD15/15. Build tasks pass; combined Gradle exits nonzero on the two goldens. Current provenance1099 entries passes; final datagen repeat succeeds with zero writes and zero stale removals. See `requested-systems-validation.json`, `requested-systems-native-review.json`, `requested-systems-golden-delta.json` and `requested-systems-provenance.json`.

Full wrap-run log: `/tmp/eln-requested-systems-all-validation-v1.log`; report: `/tmp/eln-requested-systems-native-all-v1.xml`; private world: `/tmp/eln-requested-systems-native-all-v1-world`. Temporary paths may disappear; committed JSON summaries preserve observed results. The previously certified lamp-only JAR is stale. No new downloadable release or complete artifact certification is claimed.

## Next work, ordered

1. Review final whole-suite results and exact golden delta; fix any non-golden regression before claiming a clean native checkpoint. Repeat datagen and check zero writes if not recorded.
2. Live private-world server/client checks: panel12 branches/trip/reset and slots; carry cord release/restore/voxel siblings; all seven alarms/smoke/heat/backup/silence; both vehicles/seat controls/charging/pack/cargo. Capture screenshots and logs, test fresh JVM save recovery and two clients. Use existing mctest tooling and `/elnlab panelcarry`, `alarms`, `roadvehicles`, `devicemenu`, `roadmenu` helpers. Preserve user worlds. Helpers are not themselves proof of live verification.
3. Build from the pushed commit, record JAR SHA256 and commit identity, and certify that actual artifact. Keep hardware GPU/audio and human assessment explicitly pending when unavailable.
4. Continue broader37-workstream completion plan and source audit. All7132 source files are accounted for as review obligations, not all implemented or behaviorally certified.

## Remaining work and relative difficulty

| Difficulty | Work still needed | Why / prerequisites |
|---|---|---|
| Easier isolated slices | Missing source recipes/translations/tooltips/catalog fixes for existing working adapters; additional bulb variants sharing proven physics; more source-coordinate GUI matching | Reuses current infrastructure. Still verify ratings, inventories, recipes, save compatibility and screenshots. All108 decorative fixtures are already implemented. |
| Medium | Extend carrying to genuinely portable implemented families; additional appliance wrappers; tools/configuration/copy flows; multicore manufacturing/routing and pole attachment/recovery cases | Needs paid inventory/owner lifecycle and source-specific behavior. A registration alone is insufficient. |
| Hard | Substations8016–8019, high-voltage transformer/disconnect protection | Multi-domain electrical safety, insulation, protection and conservative solver integration. |
| Hard | Room thermal/humidity, HVAC, evaporative/growing/intakes, water/material/refrigeration systems | Missing shared environment/fluids foundations; fire heat currently uses fallback plus radiant sources. |
| Hard | Kitchen/laundry/fusion and complex manufacturing families | Inventory, heat, fluids, paid work, fault and persistence coupling. |
| Hard | Partial-block mounting, ghosts/picking, broad cable/consumer ownership | Cross-cutting voxel/cable/save invariants; easy to duplicate or lose paid hardware. |
| Hardest / broad | Solver/shared-code parity, nonlinear/thermal/mechanical/fluid coupling; complete old-world/item/entity migration | Must preserve conservation, cached LU performance, condition and historical identities across real saves. |
| Broad validation | Multi-client/lifecycle/performance, GPU/audio/human parity, progression/tutorial/worldgen, platform hooks, optional MQTT/Modbus/computer/addon bridges | Requires feature gates, source-oracle coverage and sometimes real external hardware/users. |

The37-stream plan remains the detailed backlog including inherited devices, rails/replicators/tools/scanners, imports, guides, resources and platform integration. Its generated historical status tables may predate this implementation checkpoint; this handoff and linked validation override stale “not registered” panel/alarm/road notes. There is no defensible single percentage or time estimate for the whole remaining port without reviewing each acceptance check.
