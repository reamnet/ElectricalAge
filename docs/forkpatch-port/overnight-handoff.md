# Overnight port handoff — ForkPatch 3.0.77 → NeoForge 26.3

Date: 2026-11-17. Status: **functionally near-complete for the implemented scope; NOT a release certification.**
This document supersedes stale status notes in older generated tables. `wrap-up-handoff.md` remains the
per-feature evidence index.

## 1. Git state

- Repository: `/home/matthew/projects/electrical-age-port/ElectricalAge`
- Branch: `port/forkpatch-3.0.77-26.3` (base `ports/26.3`, draft PR #1 on `reamnet/ElectricalAge`)
- Session start checkpoint: `bda7db63` (Record final regression evidence...)
- **Session end HEAD: `d980ab76` (Harden AlarmLab/PanelCarryLab placement; fix mctest RCON packet codec)**
- 4 local commits ahead of origin (NOT pushed — push requires explicit user permission):
  - `f078b7ca` Add flat GUI icons for 29 panel/alarm/vehicle/breaker/relay items
  - `bcbd7b2c` Fix mctest server stop args reference
  - `70839aea` Use real source textures for the 27 panel/alarm/relay/vehicle/switch item icons
  - `d980ab76` Harden AlarmLab/PanelCarryLab placement; fix mctest RCON packet codec
- Uncommitted at handoff (to be committed as docs/tooling only):
  `OVERNIGHT_PORT_PROGRESS.md`, `tools/mctest/mctest`,
  `docs/forkpatch-port/artifact-certification.json`, `docs/forkpatch-port/overnight-handoff.md` (this file).

## 2. Elapsed working time

- This session: 2026-11-17 11:33 UTC start (resume from `d980ab76`) → ~12:30 UTC handoff
  (≈ 1 h, including one context compaction). The port itself spans many prior sessions; this
  session's work was verification, artifact certification, test-tool fixes, and handoff documentation.
- Target was 12–18 h; the remaining feature backlog is classified Hard/Hardest (see §7) and was
  assessed as not safely completable in a single remaining window without risk to the green baseline.

## 3. Features completed (implemented + test-verified)

| Area | Scope | Evidence |
|---|---|---|
| Lighting | All 108 decorative styles, fans, motion, aiming/suspension/underwater adapters, String Lights 4418, source LEDs 302/303, recipes, byte-identical original assets | `lighting-port-matrix.md`, `decorative-lighting.md` + validation; native lighting suite |
| Poles/wires | Steel transmission poles/risers 8014/8015/8020/8021, 30 distribution variants, paid spans, streetlights, services/protection; native 100-wire catalog, nine cord ratings | pole/service/cord native suites |
| Manufacturing/power | Fabricator 4480, roller 4608/insulator 4609/combiner 4610, renewables/storage/ATS/UPS/compact UPS, converter/drive/SCADA adapters, forklift/pallet/pack charger/welder | production tests + `compact-ups.md`, remaining-work inventory |
| 12-circuit panel 7988 | 3 buses, 12 real branches, typed breakers 5–400 A, thermal trip/reset, channel/dusk controls, exact 312×386 layout, source-rated switch/relay prerequisites | `breaker-panel.md`, `panel-carry-validation.json` |
| Portable carrying | Table-lamp families + compact UPS, paid cord release, owner snapshot/energy/inventory preservation, creative-dup guard, voxel sibling preservation | `portable-carry.md` |
| Fire alarms 8053–8059 | All 7 devices, 120 V panel, battery/24 V field network, latch/silence/reset/test, smoke + radiant-heat detection, assets/sounds, source 300×382 menus | `fire-alarms.md`, `fire-alarm-validation.json` |
| Road vehicles | Cart/coupe, seats/driving/hazard, native inventory/save/render/control, 240 V 100 A station 8079, reusable lead 7494, source recipes | `road-vehicles.md`, `road-vehicle-validation.json` (28/28 native) |
| GUI parity | Source-coordinate matching for 13 classic GUI variants + flat icons for 29 items + 27 real source textures | `gui-parity.md`, commits `f078b7ca`/`70839aea` |
| Test tooling | `tools/mctest/mctest` (doctor, RCON server/client, lab, sim, logs, http, regression) | `AGENTS.md`, `MCTEST_PROGRESS.md` |

## 4. Partially completed / qualified

- **Fire-alarm room climate**: source 20 °C fallback + actual radiant heat sources; full room
  thermal/humidity model NOT implemented.
- **Portable carrying**: proven families only (table lamps, compact UPS); other appliance/fan
  families not yet portable.
- **Manufacturing adapters**: coverage is adapter-level, not blanket source parity (multicore
  routing, pole attachment/recovery cases remain).
- **Bulb technologies**: 50/200 V classic + source 120 V/240 V LEDs (302/303); additional
  source bulb variants (lifetime/configuration) remain.
- **Visual parity**: original models/icons reused and menus source-coordinated, but pixel-perfect
  rendering parity is not claimed; container GL renders black, so human visual QA is pending.

## 5. Exact test results (final whole run, commit `d980ab76`)

- Native GameTest: **1470 selected / 1468 passed / 2 failed / 0 skipped.**
  Failures: `golden_descriptors` + `golden_slots` ONLY — HISTORICAL, EXPECTED, documented:
  765 assertions recorded, 761 prior differences remain + 4 added by vehicle-station
  identity/menu coverage. Expectations were never regenerated or weakened.
- **All 1004 ForkPatch native cases pass.**
- Root `:test`: 701 selected / 698 passed / 3 pre-existing optional skips (unchanged).
- CAD: 15/15 passed.
- Provenance: 1099 entries pass.
- Datagen repeat (`runClientData` twice): 0 writes (3631 files, stable).
- No non-golden assertion fails anywhere.

## 6. Artifact + live testing actually performed

- JAR: `build/libs/eln-26.3-0.1.0-port.jar`, SHA-256
  `9fb9ab07423bdb6650182d2474322fbc33e6387e1c31299bff5c6044b5c05270`, build marker
  `d980ab769343-dirty` ("-dirty" only because non-source files were modified at build time).
  Packaging comparison: 9087 compiled class/resource entries, 0 missing, 0 mismatched vs
  `build/classes` + `build/resources`. Certified in `artifact-certification.json`.
  **Local artifact only; not published, not a release sign-off.**
- Real in-engine (GameTest server = real Minecraft server code): all 1468 native + 1004 ForkPatch
  cases run in-engine.
- Real in-engine server-side labs (RCON `/elnlab`, disposable dev world):
  - device gallery: 227 of 620 placements placed (rest rejected by placement rules — expected),
    artifact `run/eln-gallery-26.json`;
  - lighting gallery `run/eln-lighting-gallery.json`;
  - fire-alarm gallery: all 7 devices placed+registered, `run/eln-alarm-gallery.json`;
  - road-vehicle gallery: cart (eid 98) + coupe (eid 99) with stations, `run/eln-road-vehicle-gallery.json`;
  - device-volts dumps: `run/eln-devicevolts-4118.json`, `run/eln-devicevolts-twelvecircuitpanel.json`.
- Client boot: `runClient` reached title screen, screenshot `tools/mctest/shots/title-boot.png`
  (plus `focused.png`, `fullscreen.png`, `primitive-test.png`). Render surface is black in this
  container (GL-surface limitation) — **visual validation is pending, not proven.**
- NOT done (honest pending list): live client-joined player behavior (0 players online; DevLaunch
  quickPlay connect path flaky), live GUI interaction, live driving, audio, simultaneous
  multi-client, fresh-JVM save recovery of the dev world, hardware GPU/audio parity, human visual QA.
  These need a connected player + a real GPU desktop, or a stronger session with more time.

## 7. Remaining work (ordered by difficulty; from `wrap-up-handoff.md`, still accurate)

- **Easier slices:** missing source recipes/translations/tooltips/catalog fixes for existing
  working adapters; additional bulb variants sharing proven physics; more source-coordinate GUI
  matching. (All 108 decorative fixtures already implemented — do not re-add styles.)
- **Medium:** extend portable carrying to more implemented families; appliance wrappers;
  tools/configuration/copy flows; multicore manufacturing/routing + pole attachment/recovery.
- **Hard:** substations 8016–8019 + HV transformer/disconnect protection; room
  thermal/humidity/HVAC/evaporative/growing/intakes/water/refrigeration; kitchen/laundry/fusion +
  complex manufacturing; partial-block mounting, ghosts/picking, broad cable/consumer ownership.
- **Hardest/broad:** solver/shared-code parity, nonlinear/thermal/mechanical/fluid coupling,
  old-world/item/entity migration; multi-client/lifecycle/perf; GPU/audio/human parity;
  progression/tutorial/worldgen; platform hooks; MQTT/Modbus/computer/addon bridges.
- The 37-workstream `port-completion-plan.md` + full audit (7132 source files accounted for as
  review obligations) remains the detailed backlog.

## 8. Known bugs / failed approaches / infra limits

- `mctest client start` `--args=` flag bug vs NeoForge DevLaunch → pre-existing; bounded fix is
  plain `runClient` to title (used for boot evidence). Full player connect not reliable here.
- Xvfb headless rendering NOT usable in this container; in-game capture needs real `DISPLAY :0`.
- Container GL renders black → all visual parity is human-pending.
- `runGameTestServer` combined Gradle exit is nonzero due to the 2 historical goldens — expected.

## 9. Recommended next steps (stronger cloud Codex model / more time)

1. Live private-world validation with a connected player: panel trip/reset, carrying release/
   restore, alarm smoke/heat/silence, vehicle driving/charging/pack, GUIs, audio, 2 clients,
   fresh-JVM save recovery (mctest + `/elnlab` helpers ready for this).
2. Easier slices in §7 first (bulb variants, recipe/translation/catalog completion) — high value,
   low risk, all verifiable in-engine.
3. Then medium → hard in order; each needs its own acceptance evidence.
4. Only after all of the above: full port completion claim + artifact release certification.

## 10. Exact reproduction commands

```bash
cd /home/matthew/projects/electrical-age-port/ElectricalAge
source /tmp/eln-port-java-env.sh
# build + root/CAD tests
./gradlew --no-daemon --max-workers=2 :build :cad:test
# native GameTests (real in-engine)
./gradlew --no-daemon --max-workers=2 runGameTestServer --tests mods.eln.*
# datagen idempotence
./gradlew --no-daemon --max-workers=2 runClientData   # run twice; expect 0 writes on 2nd
# artifact checks
sha256sum build/libs/eln-26.3-0.1.0-port.jar
python3 /tmp/verify_jar.py                            # 9087 / 0 / 0
# live labs (server running, RCON 25575 / eln-local-dev-only)
python3 tools/mctest/mctest server status
python3 tools/mctest/mctest lab gallery / lab alarms / lab panelcarry / lab roadvehicles
```
