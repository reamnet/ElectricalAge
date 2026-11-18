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

---

## 11. Dust port — Lapis Dust + Diamond Dust (2026-11-18, this session)

Scope: port the two ForkPatch 1.21.1 dust items, complete one before the other, reusing the
target's existing `registerDust` pattern. Source baseline `b7444a7b` (read-only
`../ELN1.21.1ForkPatch`); target start HEAD `72670385`.

### Items implemented
- **Lapis Dust** — registry ID `eln:shared_item@579` (group 9 `shared_item`, subId=3; 9×64+3=579).
- **Diamond Dust** — registry ID `eln:shared_item@580` (group 9 `shared_item`, subId=4; 9×64+4=580).
- Both are pure input items (no machine behavior, no electrical rating). Confirmed absent in the
  target before implementation (no matching current item name/model; present only in the 177-row
  candidate inventory of `unported-items.md`).

### Original source behavior (ForkPatch 1.21.1)
- Registered by `registerDust(9)` (`ItemRegistration.kt:110`, method `:773`); Lapis Dust
  (`:804`, ore `dustLapis`) and Diamond Dust (`:814`, ore `dustDiamond`); display names
  "Lapis Dust"/"Diamond Dust".
- Textures `textures/items/diamonddust.png` / `lapisdust.png`.
- Consumed as inputs by: (a) a Small Solar Panel recipe (`CraftingRecipes.kt:1284`, Lapis Dust), and
  (b) the **Inert Canister** recipe (`CraftingRecipes.kt:1578`, 6× Lapis Dust + 1× Diamond Dust
  shapeless).

### Registry IDs
- `eln:shared_item@579` = Lapis Dust; `eln:shared_item@580` = Diamond Dust. `eln-ids.json` now 876
  keys. `eln-ids.json` entry format: `{descriptor: GenericItemUsingDamageDescriptorWithComment,
  display, kind: shared_item, name}`.

### Implementation changes (target)
- `src/main/java/mods/eln/registry/batch/Wp12aContent.java` — `registerDust(9)` (line 46) already
  existed; Lapis (subId=3, `:427`, ore `dustLapis`) and Diamond (subId=4, `:440`, ore `dustDiamond`)
  added to the dust switch, mirroring the target's other dusts.
- `src/main/resources/eln-ids.json` — added `@579`/`@580` entries.
- Textures `src/main/resources/assets/eln/textures/item/{lapis,diamond}_dust.png` — **byte-identical
  to source** (sha256 `2b917903…` diamond, `1a051de6…` lapis; target uses `textures/item/` singular,
  matching all 645 existing target dusts).
- Generated under `src/generated/resources/`: item+model JSON for both, `c/tags/item/dusts/{lapis,
  diamond}.json`, the `dusts.json` index, and `item.eln.{lapis,diamond}_dust` lang entries. Ore tag
  rule `dust`→`c:dusts/` (`OreDictTags.java:47`) maps `dustLapis`/`dustDiamond` accordingly.
- **Recipes deliberately NOT changed:** the target's existing Small Solar Panel recipe
  (`ElnRecipes.java:1412`) uses different materials (iron ingot + Low Voltage Cable + plateSilicon)
  and is pre-existing working functionality; the Inert Canister is out of scope and was not ported.
  Net effect: the dusts are pure input items with **no target consumer** (documented limitation).

### Actual test results (level labeled; nothing claimed that did not run)
- **[compile-verified]** `:build` BUILD SUCCESSFUL (log `/tmp/eln-dust-build2.log`).
- **[tests-passed]** `:test` 704/704 (0 fail, 0 err, 3 pre-existing skips) — proves the mod
  constructs and both dusts register.
- **[datagen-verified]** `runClientData` ×2: 8 new writes → 0 writes on re-run (idempotent).
- **[automated GameTest]** `runGameTestServer`: 1552 tests; only 2 required FAIL =
  `eln:golden_descriptors` + `eln:golden_slots` (HISTORICAL, expected, not weakened); the dusts do
  not appear in any failure (log `/tmp/eln-dust-gametest2.log`).
- **[env OK]** `mctest doctor` all HARD checks pass.
- **[live smoke-test]** `mctest server start` boots on MC 26.3; RCON live; `server exec version` →
  26.3 / protocol 777.
- **[INCONCLUSIVE — not claimed passed]** in-game item presence via `summon`/`data modify` was not
  discriminating (a bogus `@9999` was also accepted). Registration is proven by mod-load (`:test`
  704/704) + datagen, not by in-game summon.

### Remaining limitations
- No target consumer recipe for either dust (Inert Canister out of scope; target Small Solar Panel
  uses different materials and was left unchanged). The dusts are obtainable/registered inputs with
  no in-target use until a consumer is ported.
- In-game (player) pickup/use/drop behavior not validated by a connected player (mod-load +
  datagen + GameTest only).

### Known blockers
- None for this item. The AGENTS.md "12-circuit panel WIP blocks all boot" note is **stale for the
  current build** — `runServer` boots and `:test`/GameTest run, so no blocker was hit and no WIP was
  touched.

### Commit reference
- Local commit for this session (no push): single new local commit on top of `72670385` on branch
  `port/forkpatch-3.0.77-26.3`, subject `Port lapis_dust and diamond_dust from ForkPatch 1.21.1`;
  inspect with `git log --oneline -1`. External queue script handles pushing.

## 12. Graphite port — Graphite Rod + 2x Graphite Rods (2026-11-18, this session)

Scope: port the two in-scope ForkPatch 1.21.1 `registerArc(69)` items, complete one before the other,
reusing the target's existing `sharedItem` registration pattern. Source baseline `b7444a7b` (read-only
`../ELN1.21.1ForkPatch`); target start HEAD `da075906` (dust commit).

### Items implemented
- **Graphite Rod** — registry ID `eln:shared_item@4416` (group 69 `shared_item`, subId=0; 69×64+0=4416).
- **2x Graphite Rods** — registry ID `eln:shared_item@4417` (group 69 `shared_item`, subId=1; 69×64+1=4417).
- Both are plain durability items: the source `GraphiteDescriptor` extends `GenericItemUsingDamageDescriptor`
  with an empty body (no machine behavior, no electrical rating). Confirmed absent in the target before
  implementation.
- Out of scope and NOT ported: `3x_graphite_rods`, `4x_graphite_rods`, Synthetic Diamond.

### Original source behavior (ForkPatch 1.21.1)
- `registerArc(69)` (`ItemRegistration.kt:1223`, called at `:126`): Graphite Rod subId 0 (`:1231`,
  `GraphiteDescriptor`), 2x Graphite Rods subId 1 (`:1239`); 3x (`:1247`) / 4x (`:1255`) out of scope.
- Textures `textures/items/graphiterod.png` / `2xgraphiterods.png`.
- Craft recipes (`CraftingRecipes.kt:1710` `recipeGraphite()`): `Graphite Rod ×2` shaped "I" from
  `2x Graphite Rods` (`:1719`); `2x Graphite Rods` shapeless from 2× `Graphite Rod` (`:1723`).
- Machine recipe (`CraftingRecipes.kt:2437`): compressor `Coal Plate ×4` from `Graphite Rod` at 80,000 J.
- `Graphite Rod` is also an input to the source advanced-chip recipe (`CraftingRecipes.kt:1665`) — out of scope.

### Registry IDs
- `eln:shared_item@4416` = Graphite Rod; `eln:shared_item@4417` = 2x Graphite Rods. `eln-ids.json` entry
  format: `{descriptor: GenericItemUsingDamageDescriptor, display, kind: shared_item, name}`.

### Implementation changes (target)
- `src/main/java/mods/eln/registry/batch/Wp12aContent.java` — new `registerArc(69)`: subId 0 (Graphite Rod)
  and subId 1 (2x Graphite Rods) with `GenericItemUsingDamageDescriptor`, mirroring the source (whose
  `GraphiteDescriptor` adds no behavior).
- `src/main/java/mods/eln/datagen/ElnRecipes.java` — new `recipeForkPatchGraphite()`: the two mutually
  dependent craft rows from source `:1719`/`:1723`.
- `src/main/java/mods/eln/registry/ElnMachineRecipes.java` — `recipeCompressor()`: added `Coal Plate ×4`
  from `Graphite Rod` @ 80,000 J (source `:2437`); the pre-existing `Coal Plate ×4` from Diamond row retained.
- `src/main/resources/eln-ids.json` — added `@4416`/`@4417` entries.
- Textures `src/main/resources/assets/eln/textures/item/{graphite_rod,2x_graphite_rods}.png` —
  **byte-identical to source** (md5 `2129f6e2…` / `62f24e40…`; target uses `textures/item/` singular,
  matching existing target items).
- Generated under `src/generated/resources/`: item+model JSON for both, `data/eln/recipe/{graphite_rod,
  2x_graphite_rods}.json`, the matching recipe advancements, and `item.eln.{graphite_rod,2x_graphite_rods}`
  lang entries.

### Actual test results (level labeled; nothing claimed that did not run)
- **[compile-verified]** `:build` BUILD SUCCESSFUL (log `/tmp/eln-graphite-build.log`).
- **[tests-passed]** `:test` 704/704 (0 fail, 0 err, 3 pre-existing skips) — identical to the dust baseline;
  proves the mod constructs and both items register.
- **[datagen-verified]** `runClientData` ×2: 0 new writes on both runs (generated JSONs present and correct;
  idempotent). Logs `/tmp/eln-graphite-dg1.log`, `/tmp/eln-graphite-dg2.log`.
- **[automated GameTest]** `runGameTestServer`: 1552 tests; only 2 required FAIL = `eln:golden_descriptors` +
  `eln:golden_slots` (HISTORICAL, expected, not weakened); no graphite-related failure
  (log `/tmp/eln-graphite-gametest.log`).
- **[env OK]** `mctest doctor` all HARD checks pass.
- **[live smoke-test]** `mctest server start` boots on MC 26.3; RCON live; `server exec version` → 26.3 /
  protocol 777. The leftover dev server from the smoke test was stopped before handoff (verified gone);
  no client running.
- **[NOT RUN — honestly labeled]** in-game crafting/compression of the new items by a connected player was
  NOT performed (no connected player in this environment). Registration + recipes proven by mod-load
  (`:test`), datagen and GameTest instead.

### Remaining limitations
- `3x_graphite_rods` / `4x_graphite_rods` and Synthetic Diamond are NOT ported (out of scope).
- The source advanced-chip consumer recipe (`CraftingRecipes.kt:1665`, uses Graphite Rod + Synthetic
  Diamond) is not in scope; the in-target consumer of Graphite Rod is the compressor Coal Plate recipe
  (plus the 2x-rods craft pair).
- No player-joined in-game crafting/compression validation.

### Known blockers
- None for this item. `runServer` boots and `:test`/GameTest run, so no blocker was hit and no WIP was touched.

### Commit reference
- Local commit for this session (no push): new local commit on top of `da075906` on branch
  `port/forkpatch-3.0.77-26.3`, subject `Port graphite_rod and 2x_graphite_rods from ForkPatch 1.21.1`;
  inspect with `git log --oneline -1`. External queue script handles pushing.

## 13. Clutch plate port — Iron Clutch Plate + Copper Clutch Plate (2026-11-18, this session)

Scope: port the two in-scope ForkPatch 1.21.1 `ClutchPlateItem` items (iron subId 54, copper subId 57),
complete one before the other, reusing the target's `sharedItem` registration pattern (group 120
`registerMiscItem`). Source baseline `b7444a7b` (read-only `../ELN1.21.1ForkPatch`); target start HEAD
`63f67c15` (graphite commit).

### Items implemented
- **Iron Clutch Plate** — registry ID `eln:shared_item@7734` (group 120 `shared_item`, subId=54; 120×64+54=7734).
  Ratings `(maxEF, minEF, maxDTF, minDTF, wearSpeed, explodes)` = `5120, 640, 640, 160, 0.0001, false` —
  exact vs source.
- **Copper Clutch Plate** — registry ID `eln:shared_item@7737` (group 120, subId=57; 120×64+57=7737).
  Ratings = `8192, 4096, 1024, 512, 0.0003, false` — exact vs source.
- Both confirmed absent in the target (unported-items inventory row "No matching current item
  name/model found") before implementation.
- Out of scope and NOT ported: `clutch` (machine), `clutch_pin`, `gold_clutch_plate`, `lead_clutch_plate`,
  `coal_clutch_plate`; clutch machine/element classes.

### Original source behavior (ForkPatch 1.21.1)
- `src/main/kotlin/mods/eln/mechanical/Clutch.kt:38` — `ClutchPlateItem(name, maxEF, minEF, maxDTF,
  minDTF, wearSpeed, explodes) : GenericItemUsingDamageDescriptor(name)`: wear tracked as a `wear` double
  in item NBT; `maxStaticEnergyF` = Linear(0→maxEF, 1→minEF), `dynamicMaxTransferF` = Linear(0→maxDTF,
  1→minDTF), `slipWearF` = Linear(0→0, 1000→wearSpeed); `addInformation` shows a Condition line
  (New < 0.2 / Good < 0.5 / Used < 0.8 / End of life < 0.9 / Bad); `infiniteLifeEnabled` flag.
  The plates are the consumable side of a clutch — actual wear/rating consumption happens in the (not
  ported) clutch machine.
- Registration `src/main/kotlin/mods/eln/registration/ItemRegistration.kt:1760` (iron), `:1773` (copper);
  gold `:1767`, lead `:1779`, coal `:1785` out of scope.
- Recipes `src/main/kotlin/mods/eln/craft/CraftingRecipes.kt:1897` (iron) / `:1899-1905` (copper):
  plate center + 4 tungsten-dust cross; `'I'`/`'C'` = `plateIron`/`plateCopper`, `'t'` = config dict
  `runtime.dictionary.tungstenDust` (default `dustElnTungsten`).
- Textures `textures/items/{iron,copper}clutchplate.png`.

### Registry IDs
- `eln:shared_item@7734` = Iron Clutch Plate; `eln:shared_item@7737` = Copper Clutch Plate.
  `eln-ids.json` entries: `{descriptor: ClutchPlateItem, display, kind: shared_item, name}`.

### Implementation changes (target)
- NEW `src/main/java/mods/eln/mechanical/ClutchPlateItem.kt` — parity with source `Clutch.kt:38`;
  wear NBT via target `ItemNbt.edit/get` (26.3 CUSTOM_DATA component) in place of 1.12
  `ItemStack.getTagCompound`/`setTagCompound`; Condition line + `infiniteLifeEnabled` preserved.
- `src/main/java/mods/eln/registry/batch/Wp12aContent.java:988-995` — `registerMiscItem(120)` group:
  subId 54 (iron) + subId 57 (copper) with exact source ratings.
- `src/main/java/mods/eln/datagen/ElnRecipes.java:2130-2144` — both craft recipes (identical shape);
  ingredient refs use the target tag pattern `#eln:high_quality_plates/{iron,copper}` + `#eln:dusts/tungsten`
  (deliberate target-pattern reuse; shape/behavior identical to source).
- `src/main/resources/eln-ids.json` — added `@7734`/`@7737`.
- Textures `src/main/resources/assets/eln/textures/items/{iron,copper}clutchplate.png` —
  **byte-identical to source** (md5 `96feefcd…` / `dbc662bb…`).
- Generated under `src/generated/resources/`: item + model JSON for both, `data/eln/recipe/
  {iron,copper}_clutch_plate.json`, matching recipe advancements, and `item.eln.{iron,copper}_clutch_plate`
  lang entries.

### Actual test results (level labeled; nothing claimed that did not run)
- **[compile-verified]** main+test compiled during the `:test`/`runClientData` runs (prior agent).
- **[tests-passed]** `:test` 704/704 (0 fail, 0 err, 3 pre-existing skips) — identical to the graphite/dust
  baselines; proves the mod constructs and both plates register (prior agent run).
- **[datagen-verified]** `runClientData` idempotence proven: 3rd run → md5 diff of generated tree empty
  (`/tmp/gen-before.md5` vs `/tmp/gen-after.md5`); item/model/recipe/advancement/lang JSONs present and
  correct (prior agent runs).
- **[live server-side oracle]** live MC 26.3 dev server via mctest RCON (this session):
  `summon minecraft:item 0 -60 0 {Item:{id:"eln:iron_clutch_plate",count:1}}` → "Summoned new Iron Clutch
  Plate"; same for copper → "Summoned new Copper Clutch Plate"; bogus `eln:totally_bogus_item_xyz` →
  "Summoned new Air" (negative control). Both registry IDs + display names + item components resolve at
  runtime. Probes cleaned up after the run.
- **[NOT RUN — honestly labeled]** player-joined in-game crafting/wear/Condition validation NOT performed
  (client-join blocked; see below). Plate behavior is item-level only — the clutch machine that consumes
  wear/ratings is out of scope and not ported.

### Remaining limitations
- `clutch` machine, `clutch_pin`, gold/lead/coal clutch plates NOT ported (out of scope).
- No player-joined in-game validation (crafting, wear, Condition toasts untested in-world).
- No audio / multi-client / fresh-JVM save-recovery validation.

### Known blockers
- Client live-join is BLOCKED in this environment (3 approaches exhausted): (1) quickPlay arg injection —
  ModDevGradle 2.0.148 rejects `--quickPlayMultiplayer` as a client program arg; leading-space workaround
  passes Gradle but DevLaunch dies in `Main.main`; (2) blind title navigation — keyboard input IS delivered
  (world list, singleplayer, `/seed` all worked) but the Multiplayer button was never positively reached
  and screen capture is all-black (RenderPearl offscreen), so no visual oracle; (3) `/connect` from
  singleplayer — command does not exist in 26.3 (no `Connect*` command class in the patched jar).
  This is a test-infra limitation, documented only (not fixed — out of scope for this item port).
- The AGENTS.md "12-circuit-breaker mod-load blocker" no longer reproduces in this window: `runServer`
  boots cleanly and the server stayed up for the oracle.

### Commit reference
- Local commit for this session (no push): new commit on top of `63f67c15` on branch
  `port/forkpatch-3.0.77-26.3`, subject `Port iron_clutch_plate and copper_clutch_plate from ForkPatch 1.21.1`;
  inspect with `git log --oneline -1`. External queue script handles pushing.

## 14. Turbine blade port — Iron Turbine Blade (2026-11-18, this session)

Scope: port the in-scope ForkPatch 1.21.1 turbine-blade item `iron_turbine_blade` (the consumable a
large turbine runs on), complete before the steel tier. Reused the target's `sharedItemStackOne`
registration pattern (group 18) and `ElnConfig`/datagen/texture conventions. Source baseline `b7444a7b`
(read-only `../ELN1.21.1ForkPatch`); target start HEAD `bcaadf5e` (clutch commit).

### Items implemented
- **Iron Turbine Blade** — registry ID `eln:shared_item_stack_one@1152` (group 18 `sharedItemStackOne`,
  subId=0; 18×64+0=1152). tierName=`iron`, temperatureResistance=0.8, corrosionResistance=0.8,
  tierDescription=`"Suitable for mild, clean fuels"` — exact vs source `ItemRegistration.kt:1458-1468`.
  Confirmed absent in the target (unported-items row "No matching current item name/model found") before
  implementation.
- Out of scope and NOT ported: `steel_turbine_blade` (follow-on commit), `alloy_turbine_blade`,
  `tungsten_turbine_blade`, and the turbine/shaft/consumer machines. No target consumer reads the blade
  condition yet — behavior is item-level only (same posture as the clutch plates vs the clutch machine).

### Original source behavior (ForkPatch 1.21.1)
- `src/main/kotlin/mods/eln/item/TurbineBladeDescriptor.kt` —
  `TurbineBladeDescriptor(name, tierName, temperatureResistance, corrosionResistance, tierDescription) :
  GenericItemUsingDamageDescriptorUpgrade(name)`: condition stored as a `condition` double in item NBT
  (1.0 new → 0.0 broken; 1.0 default), survives inventory moves; `getDefaultNBT()` sets `condition: 1.0`;
  `addInformation` shows `Nominal Lifetime: %h`, `Condition: New/Good/Used/Bad/End of life` (thresholds
  1.0/0.5/0.15/0.01), and the tier line; `infiniteLifeEnabled` per tier. Nominal life from config
  `items.turbineBlades.<tier>.nominalLifeHours` (iron default **10.0**, steel 64.0, alloy 400.0,
  tungsten 2000.0) via `BladeConfigData`/`TurbineBladeLists`.
- Registration `ItemRegistration.kt:1458` `registerTurbineBladeItems(18)` on `sharedItemStackOne` —
  **not** `sharedItem`, source comment: "blades store durability in NBT, stacking would corrupt it".
- Recipe `craft/CraftingRecipes.kt:2499` (iron): ` I ` / `IMI` / ` I `, iron ingots + Machine Block.
- Texture `textures/items/ironturbineblade.png`.

### Registry IDs
- `eln:shared_item_stack_one@1152` = Iron Turbine Blade. `eln-ids.json` entry:
  `{descriptor: TurbineBladeItem, display: "Iron Turbine Blade", kind: shared_item_stack_one,
  name: iron_turbine_blade}`.

### Implementation changes (target)
- NEW `src/main/java/mods/eln/mechanical/TurbineBladeItem.kt` — parity with source
  `TurbineBladeDescriptor.kt`; condition NBT via target `ItemNbt` (26.3 CUSTOM_DATA component) in place
  of 1.12 `ItemStack.getTagCompound`/`setTagCompound`; tooltip + tier gating
  (`TURBINE_BLADE_TIERS`) preserved. One Kotlin fix vs the prior agent's draft: the two `%$` format
  strings need `\$` escapes (Kotlin string templates) — done this session; build then passed.
- `src/main/java/mods/eln/ElnConfig.java` — new `items.turbineBlades.iron.{nominalLifeHours (10.0,
  range 0.001–87600), infiniteLifeEnabled (false)}` + accessors `turbineBladeLifeHours(tier)` /
  `turbineBladeInfiniteLife(tier)` + default-hours map. Tier arrays (`bladeTiers`/`bladeDefaultHours`)
  are loops, so the steel commit appends `"steel"`/`64.0` without structural changes.
- `src/main/java/mods/eln/registry/batch/Wp12aContent.java` — `registerTurbineBlade(18)` in
  `preInit()`; `addElement(0 + (18 << 6), TurbineBladeItem(TR_NAME(Type.NONE, "Iron Turbine Blade"),
  "iron", 0.8, 0.8, "Suitable for mild, clean fuels"))`.
- `src/main/java/mods/eln/datagen/ElnRecipes.java` (`recipeMiscItem`) — iron craft recipe, exact shape
  vs source; ingredient refs use the target pattern (`ingotIron`, `Machine Block`).
- `src/main/resources/eln-ids.json` — added `@1152`.
- Texture `src/main/resources/assets/eln/textures/items/ironturbineblade.png` — **byte-identical to
  source** (sha256 `b0e1cfe3e545d0bb6171d319780508b20ef8c2a2d6aab5bdd91a1285958fa8b8`).
- Generated under `src/generated/resources/`: item + model JSON, `data/eln/recipe/iron_turbine_blade.json`,
  matching recipe advancement, `item.eln.iron_turbine_blade` lang entry.

### Actual test results (level labeled; nothing claimed that did not run)
- **[compile-verified]** `:build` BUILD SUCCESSFUL in 5m44s (`/tmp/eln-iron-build3.log`).
- **[tests-passed]** `:test` 704/704 (0 fail, 0 err, 3 pre-existing skips) — identical to the
  dust/graphite/clutch baselines; proves the mod constructs and the blade registers.
- **[datagen-verified]** `runClientData` ×2: first run generated the 5 new JSON/lang entries (expected);
  second run md5 diff of the generated tree = **0 lines** (idempotent).
- **[automated GameTest]** `runGameTestServer`: **1552 tests complete** in 7.56 min, exactly the 2
  historical golden fails (`eln:golden_descriptors`, `eln:golden_slots`) — no new failures vs baseline
  (`/tmp/eln-iron-gametest.log`).
- **[env OK]** `mctest doctor` — all HARD checks pass.
- **[live server-side oracle]** live MC 26.3 dev server (protocol 777) via mctest RCON (this session):
  `summon minecraft:item 0 -60 0 {Item:{id:"eln:iron_turbine_blade",count:1}}` → "Summoned new Iron
  Turbine Blade"; bogus `eln:totally_bogus_item_xyz` → "Summoned new Air" (negative control). Registry
  ID + display name + item component resolve at runtime. Probes killed; server stopped.
- **[NOT RUN — honestly labeled]** player-joined in-game crafting/wear/Condition validation NOT performed
  (client-join blocked per the clutch entry; documented, not fixed). No consumer machine in target for
  the wear path yet (out of scope).

### Remaining limitations
- `steel_turbine_blade` (follow-on commit), alloy/tungsten tiers, and all turbine/consumer machines NOT
  ported (out of scope).
- No player-joined in-game validation (crafting, wear, Condition toasts untested in-world).
- No audio / multi-client / fresh-JVM save-recovery validation.

### Known blockers
- Client live-join remains BLOCKED in this environment (clutch-entry blockers unchanged); the AGENTS.md
  "12-circuit-breaker mod-load blocker" still does not reproduce: `runServer` boots cleanly on MC 26.3
  and the GameTest server ran all 1552 tests.

### Commit reference
- Local commit for this session (no push): new commit on top of `bcaadf5e` on branch
  `port/forkpatch-3.0.77-26.3`, subject `Port iron_turbine_blade from ForkPatch 1.21.1`; inspect with
  `git log --oneline -1`. External queue script handles pushing.

## 15. Turbine blade port — Steel Turbine Blade (2026-11-18, this session)

Scope: port the second assigned ForkPatch 1.21.1 item `steel_turbine_blade`, completed after the iron
tier (committed first as `33ffede4`). Reused the iron-tier `TurbineBladeItem` descriptor — the tier set
(`TurbineBladeItem.kt`) extended from `{"iron"}` to `{"iron","steel"}` — the `ElnConfig` per-tier loop,
and the `recipeMiscItem` datagen block. Source baseline `b7444a7b` (read-only `../ELN1.21.1ForkPatch`);
target start HEAD `33ffede4` (iron commit).

### Items implemented
- **Steel Turbine Blade** — registry ID `eln:shared_item_stack_one@1153` (group 18 `sharedItemStackOne`,
  subId=1; 18×64+1=1153). tierName=`steel`, temperatureResistance=1.2, corrosionResistance=1.2,
  tierDescription=`"Handles moderate conditions"` — exact vs source `ItemRegistration.kt:1470-1478`.
  Confirmed absent in the target (unported-items row "No matching current item name/model found") before
  implementation.
- Out of scope and NOT ported: `alloy_turbine_blade` (source 2.0/2.0, 400.0 h),
  `tungsten_turbine_blade` (source 3.5/3.5, 2000.0 h), and all turbine/shaft/consumer machines
  (`large_gas_turbine`, `large_steam_turbine`, etc.). No target consumer reads blade condition yet —
  behavior is item-level only.

### Original source behavior (ForkPatch 1.21.1)
- Same descriptor as the iron tier (see §14): condition in item NBT (1.0 → 0.0), tooltip with nominal
  lifetime/Condition/tier line, per-tier `infiniteLifeEnabled`.
- Nominal life `items.turbineBlades.steel.nominalLifeHours` = **64.0** (source `TurbineBladeDescriptor.kt:70`
  `BladeConfigData("steel", 64.0)`); default `infiniteLifeEnabled=false`.
- Registration `ItemRegistration.kt:1470` `1 + (18 shl 6)` on `sharedItemStackOne` (stack_one, not
  sharedItem: condition NBT would be corrupted by stacking).
- Recipe `craft/CraftingRecipes.kt:2506-2512`: `SIS` / `SMS` / `SIS`, `'S' = firstExistingOre("ingotSteel",
  "ingotIron")`, `'I' = ingotIron`, `'M' = Machine Block`.
- Texture `textures/items/steelturbineblade.png`.

### Registry IDs
- `eln:shared_item_stack_one@1153` = Steel Turbine Blade. `eln-ids.json` entry:
  `{descriptor: TurbineBladeItem, display: "Steel Turbine Blade", kind: shared_item_stack_one,
  name: steel_turbine_blade}`.

### Implementation changes (target)
- `src/main/java/mods/eln/mechanical/TurbineBladeItem.kt` — `TURBINE_BLADE_TIERS` set extended to
  `{"iron","steel"}` (1 line; descriptor logic unchanged).
- `src/main/java/mods/eln/ElnConfig.java` — `bladeTiers`/`bladeDefaultHours` arrays append `"steel"`/`64.0`
  (loop unchanged); per-tier keys `items.turbineBlades.steel.{nominalLifeHours (64.0, range 0.001–87600),
  infiniteLifeEnabled (false)}` generated by the existing loop.
- `src/main/java/mods/eln/registry/batch/Wp12aContent.java` — `registerTurbineBlade(18)` adds
  `addElement(1 + (18 << 6), TurbineBladeItem(TR_NAME(Type.NONE, "Steel Turbine Blade"), "steel", 1.2, 1.2,
  "Handles moderate conditions"))`.
- `src/main/java/mods/eln/datagen/ElnRecipes.java` — steel craft recipe in the same `recipeMiscItem` block,
  exact shape vs source; `'S' = firstExistingOre("ingotSteel","ingotIron")` preserves the source fallback
  semantics (datagen emits a steel-tag variant and an iron-fallback variant).
- `src/main/resources/eln-ids.json` — added `@1153`.
- Texture `src/main/resources/assets/eln/textures/items/steelturbineblade.png` — **byte-identical to
  source** (sha256 `b25777bcb8ef16964f24fb7baa4a25dd9ad765530c772d8ca0281c5f2b571c60`; re-verified this
  session on both sides).
- Generated under `src/generated/resources/`: `assets/eln/items/steel_turbine_blade.json`,
  `assets/eln/models/item/steel_turbine_blade.json`, `data/eln/recipe/steel_turbine_blade{,_2}.json`,
  matching recipe advancements, `item.eln.steel_turbine_blade` lang entry.

### Actual test results (level labeled; nothing claimed that did not run)
- **[compile-verified]** `./gradlew build` BUILD SUCCESSFUL in 5m, exit 0 (`/tmp/eln-steel-build.log`).
- **[tests-passed]** `:test` 704/704 (0 fail, 0 err, 3 pre-existing skips) — identical to the iron-tier
  baseline; proves the mod constructs and the blade registers.
- **[datagen-verified]** `runClientData` ×2: run 1 generated the steel item/model + 2 recipe variants +
  2 advancements + lang entry; run 2 md5 diff of the generated tree = **0 lines** (idempotent).
- **[automated GameTest]** `runGameTestServer`: **1552 GAME TESTS COMPLETE in 7.436 min**; exit 2 = exactly
  the same 2 historical golden fails as the iron-tier baseline (`eln:golden_descriptors`,
  `eln:golden_slots`) — no new failures (`/tmp/eln-steel-gametest.log`).
- **[env OK]** `mctest doctor` — all HARD checks pass.
- **[live server-side oracle]** live MC 26.3 dev server via mctest RCON:
  `summon minecraft:item 0 -60 0 {Item:{id:"eln:steel_turbine_blade",count:1}}` → "Summoned new Steel
  Turbine Blade"; iron regression check still resolves; bogus `eln:totally_bogus_item_xyz` → "Summoned new
  Air" (negative control). Registry ID + display name resolve at runtime. Probes killed; server stopped.
- **[NOT RUN — honestly labeled]** player-joined in-game crafting/wear/Condition validation NOT performed
  (client-join remains blocked in this environment per the clutch entry). No consumer machine exists in the
  target for the wear path yet (out of scope).

### Remaining limitations
- `alloy_turbine_blade` + `tungsten_turbine_blade` tiers and all turbine/consumer machines NOT ported
  (out of scope).
- No player-joined in-game validation (crafting, wear, Condition toasts untested in-world).
- No audio / multi-client / fresh-JVM save-recovery validation.

### Known blockers
- Client live-join remains BLOCKED in this environment (clutch-entry blockers unchanged); the AGENTS.md
  "12-circuit-breaker mod-load blocker" still does not reproduce: `runServer` boots cleanly on MC 26.3 and
  the GameTest server ran all 1552 tests.

### Commit reference
- Local commit for this session (no push): new commit on top of `33ffede4` on branch
  `port/forkpatch-3.0.77-26.3`, subject `Port steel_turbine_blade from ForkPatch 1.21.1`; inspect with
  `git log --oneline -2`. External queue script handles pushing.

## 16. Fuse port — 5A Lead Fuse (2026-11-18, this session)

Scope: port the first assigned ForkPatch 1.21.1 current-limited fuse `5a_lead_fuse`; the 10A tier
follows in the next commit (same descriptor change). Reused the existing cable-fuse
`ElectricalFuseDescriptor` — the class already existed in the target for the four cable lead fuses;
the new `currentLimit` parameter path is the only descriptor change, and the cable-fuse path is
untouched. Source baseline `b7444a7b` (read-only `../ELN1.21.1ForkPatch`); target start HEAD
`dda988ae` (steel-blade commit).

### Items implemented
- **5A Lead Fuse** — registry ID `eln:shared_item@6288` (group 98 `sharedItem`, subId=16;
  98×64+16=6288). `currentLimit=5.0 A`, `cableDescriptor=null` — exact vs source
  `SixNodeRegistration.kt:1641-1647`. Confirmed absent in the target (unported-items row "No
  matching current item name/model found") before implementation.
- Out of scope and NOT ported: `15a_lead_fuse`, `20a_lead_fuse`, `30a_lead_fuse`, `50a_lead_fuse`
  (separate unported-items rows); source `creativeTab` assignment (no-op in the target) and source
  `renderItem()` (removed in target, client rendering — not restored).

### Original source behavior (ForkPatch 1.21.1)
- Current-limited fuse: no cable descriptor, rated current instead.
  `ElectricalFuseDescriptor(name, null, obj("ElectricalFuse"), 5.0)`;
  `isUsable() = cableDescriptor != null || currentLimit != null`;
  `resistanceOhms() = Eln.getSmallRs()*2` (heat R); `warmLimitCelsius() = Eln.cableWarmLimit` (130 °C);
  `thermalRp() = warmLimit/(I²·R)`; `thermalC() = I²·R·Eln.cableHeatingTime/warmLimit` → steady-state
  T = (I/I_limit)² × warmLimit, i.e. 5 A into a 5 A fuse reaches 130 °C.
- Installed in the existing Electrical Fuse Holder (source subId 6, same block); the fuse-holder
  element (`sixnode/ElectricalFuse.kt`) uses `isUsable()`/`resistanceOhms()`/`thermalRp()`/
  `thermalC()`/`warmLimitCelsius()` and trips at `T > warmLimit × 0.9`, replacing the fuse with the
  shared `BlownFuse` descriptor (icon `blownelectricalfuse` in source).
- Recipe `craft/CraftingRecipes.kt:2777-2778`: `rcr`, `'r'=itemRubber`, `'c'=Copper 20 AWG Cable
  300V`, output ×4.
- Texture `textures/items/electricalfuse.png` (shared fuse icon).

### Registry IDs
- `eln:shared_item@6288` = 5A Lead Fuse. `eln-ids.json` entry:
  `{descriptor: ElectricalFuseDescriptor, display: "5A Lead Fuse", kind: shared_item,
  name: 5a_lead_fuse}`. Distinct registry slot from `eln:six_node@6288` (the 5A breaker panel
  descriptor) — different registries, no collision.

### Implementation changes (target)
- `src/main/java/mods/eln/item/ElectricalFuseDescriptor.kt` — 4th constructor param
  `currentLimit: Double? = null` + secondary 3-arg constructor (Java callers keep compiling);
  `init` gates on `isUsable()`; added `isUsable()`, `resistanceOhms()`, `warmLimitCelsius()`,
  `thermalRp()`, `thermalC()` — 1:1 vs source (rendering methods remain removed per the 26.x P3
  plan).
- `src/main/java/mods/eln/sixnode/ElectricalFuse.kt` — `ElectricalFuseHolderElement` now routes
  through the descriptor's `isUsable()`/`resistanceOhms()`/`thermalRp()`/`thermalC()`/
  `warmLimitCelsius()`; cable-fuse math unchanged (`cableDescriptor?.electricalRs?.times(2.0)` ≡
  `resistanceOhms()` on the cable path); trip comment documents WP16b factor 0.9 preserved over
  source 0.8; `refreshSwitchResistor()` sets `R = getSmallRs()` (1×) for current-limited fuses,
  ultra-impedance for non-usable (matches the source 1×/2× convention pinned by
  `Wp9aCases.java:466-470`); `fuseOk` draw gate switched to `isUsable()`.
- `src/main/java/mods/eln/registry/batch/Wp9aContent.java` — `registerElectricalFuse(98)` adds
  `addElement(16 + (98 << 6), new ElectricalFuseDescriptor("5A Lead Fuse", null, obj.getObj("ElectricalFuse"), 5.0))`
  after the BlownFuse block.
- `src/main/java/mods/eln/datagen/ElnRecipes.java` — 5A recipe in the same `recipeMiscItem` block as
  the cable fuses, exact shape vs source.
- `src/main/resources/eln-ids.json` — added `@6288`.
- Texture `src/main/resources/assets/eln/textures/items/electricalfuse.png` — **byte-identical to
  source** (md5 `199a8e8bbcc4d7a3fff9dd38d0f41938`; verified both sides this session). The cable
  fuses' models already reference this icon name.
- Generated under `src/generated/resources/`: `assets/eln/items/5a_lead_fuse.json`,
  `assets/eln/models/item/5a_lead_fuse.json` (layer0 `eln:items/electricalfuse`),
  `data/eln/recipe/5a_lead_fuse.json`, matching recipe advancement, `item.eln.5a_lead_fuse` lang
  entry (auto-generated from `eln-ids.json` `display`).

### Actual test results (level labeled; nothing claimed that did not run)
- **[compile-verified]** `./gradlew build` BUILD SUCCESSFUL (`/tmp/eln-fuse5a-compile.log`);
  re-confirmed by the GameTest server boot.
- **[tests-passed]** `:test` 704/704 (0 fail, 0 err, 3 pre-existing skips) — identical to the
  blade baselines (`/tmp/eln-fuse5a-test.log`); proves the mod constructs and the fuse registers.
- **[datagen-verified]** `runClientData` ×2: run 1 generated the item/model + recipe + advancement +
  lang entry; run 2 md5 diff of the 3957-file generated tree = **0 lines** (idempotent)
  (`/tmp/eln-fuse5a-datagen{1,2}.log`, `/tmp/eln-fuse5a-md5-{1,2}.txt`).
- **[automated GameTest]** `runGameTestServer`: **1552 GAME TESTS COMPLETE in 7.503 min**; exit 2 =
  exactly the same 2 historical golden fails as the blade baselines (`eln:golden_descriptors`:
  six_node 2176–2186 "no such item in golden/1.12/inventory.json"; `eln:golden_slots`: lamp-socket
  bulb set / no-1.12-container specs) — no new failures; all cable-fuse goldens (low/medium/high/
  very-high lead fuses, Blown Lead Fuse) PASS (`/tmp/eln-fuse5a-gametest.log`).
- **[env OK]** `mctest doctor` — all HARD checks pass.
- **[live server-side oracle]** live MC 26.3 dev server (boots cleanly — the AGENTS.md
  "12-circuit-breaker mod-load blocker" is STALE) via mctest RCON:
  `summon minecraft:item 0 -60 0 {Item:{id:"eln:5a_lead_fuse",count:1}}` → "Summoned new 5A Lead
  Fuse"; bogus `eln:totally_bogus_item_xyz` → "Summoned new Air" (negative control). Registry ID +
  display name resolve at runtime. Probe killed ("Killed 5A Lead Fuse"); server stopped.
- **[NOT RUN — honestly labeled]** player-joined in-game crafting / fuse-holder installation / trip
  validation NOT performed (client-join remains blocked in this environment per the clutch entry).
  Trip physics exercised only through the shared descriptor code path + the GameTest suite, not a
  live in-world trip.

### Remaining limitations
- `15a_lead_fuse` / `20a_lead_fuse` / `30a_lead_fuse` / `50a_lead_fuse` NOT ported (out of scope;
  separate unported-items rows).
- No player-joined in-game validation (crafting, fuse-holder install, actual trip event untested
  in-world).
- No audio / multi-client / fresh-JVM save-recovery validation.

### Known blockers
- Client live-join remains BLOCKED in this environment (clutch-entry blockers unchanged); the
  AGENTS.md "12-circuit-breaker mod-load blocker" is confirmed STALE: `runServer` boots cleanly on
  MC 26.3 and the GameTest server ran all 1552 tests.

### Commit reference
- Local commit for this session (no push): new commit on top of `dda988ae` on branch
  `port/forkpatch-3.0.77-26.3`, subject `Port 5a_lead_fuse from ForkPatch 1.21.1`; inspect with
  `git log --oneline -2`. External queue script handles pushing.

## 17. Fuse port — 10A Lead Fuse (2026-11-18, this session)

Scope: port the second assigned ForkPatch 1.21.1 current-limited fuse `10a_lead_fuse`; the
5A/10A in-scope pair is complete with this. No new descriptor or element code was needed — the
`currentLimit` parameter path added for the 5A port carries the 10A fuse verbatim; the only
target changes are the registration line, the `eln-ids.json` entry, the datagen recipe, and the
generated artifacts. Source baseline `b7444a7b` (read-only `../ELN1.21.1ForkPatch`); target start
HEAD `67edf4a3` (5A fuse commit).

### Items implemented
- **10A Lead Fuse** — registry ID `eln:shared_item@6289` (group 98 `sharedItem`, subId=17;
  98×64+17=6289). `currentLimit=10.0 A`, `cableDescriptor=null` — exact vs source
  `SixNodeRegistration.kt:1649-1655`. Confirmed absent in the target (unported-items row "No
  matching current item name/model found") before implementation.
- Out of scope and NOT ported: `15a_lead_fuse`, `20a_lead_fuse`, `30a_lead_fuse`, `50a_lead_fuse`
  (separate unported-items rows); source `creativeTab` assignment (no-op in the target).

### Original source behavior (ForkPatch 1.21.1)
- Current-limited fuse: no cable descriptor, rated current instead.
  `ElectricalFuseDescriptor(name, null, obj("ElectricalFuse"), 10.0)`;
  `resistanceOhms() = Eln.getSmallRs()*2` (heat R); `warmLimitCelsius() = Eln.cableWarmLimit`
  (130 °C); `thermalRp() = warmLimit/(I²·R)`; `thermalC() = I²·R·Eln.cableHeatingTime/warmLimit`
  → steady-state T = (I/I_limit)² × warmLimit, i.e. 10 A into a 10 A fuse reaches 130 °C.
- Installed in the existing Electrical Fuse Holder (source subId 6, same block); the fuse-holder
  element trips at `T > warmLimit × 0.9` (WP16b factor 0.9 preserved over source 0.8), replacing
  the fuse with the shared `BlownFuse` descriptor — no 10A-specific blown item.
- Recipe `craft/CraftingRecipes.kt:2781-2782`: `rcr`, `'r'=itemRubber`, `'c'=Copper 16 AWG Cable
  300V`, output ×4.
- Texture `textures/items/electricalfuse.png` (shared fuse icon — no per-amp texture).

### Registry IDs
- `eln:shared_item@6289` = 10A Lead Fuse. `eln-ids.json` entry:
  `{descriptor: ElectricalFuseDescriptor, display: "10A Lead Fuse", kind: shared_item,
  name: 10a_lead_fuse}`, inserted between `@6288` (5A) and the next lower group. Distinct
  registry slot from `eln:six_node@6289` — different registries, no collision.

### Implementation changes (target)
- `src/main/java/mods/eln/registry/batch/Wp9aContent.java` — `registerElectricalFuse(98)` adds
  `addElement(17 + (98 << 6), new ElectricalFuseDescriptor("10A Lead Fuse", null, obj.getObj("ElectricalFuse"), 10.0))`
  directly after the 5A (subId-16) block.
- `src/main/java/mods/eln/datagen/ElnRecipes.java` — 10A recipe directly after the 5A one in the
  same `recipeMiscItem` block; `rcr` with `c = Copper 16 AWG Cable 300V` (ingredient
  `eln:copper_16_awg_cable_300v` resolves in target), output ×4.
- `src/main/resources/eln-ids.json` — added `@6289`.
- Generated under `src/generated/resources/`: `assets/eln/items/10a_lead_fuse.json`,
  `assets/eln/models/item/10a_lead_fuse.json` (layer0 `eln:items/electricalfuse`),
  `data/eln/recipe/10a_lead_fuse.json`, matching recipe advancement, `item.eln.10a_lead_fuse`
  lang entry ("10A Lead Fuse", auto-generated from `eln-ids.json` `display`).
- No new texture, no descriptor/element changes — shared `electricalfuse.png` (md5
  `199a8e8bbcc4d7a3fff9dd38d0f41938`) committed with the 5A port.

### Actual test results (level labeled; nothing claimed that did not run)
- **[compile-verified]** compiled during the `:test` build — BUILD SUCCESSFUL
  (`/tmp/eln-fuse10a-test.log`); re-confirmed by the GameTest server boot.
- **[tests-passed]** `:test` 704/704 (0 fail, 0 err, 3 pre-existing skips) — exact baseline match
  with the 5A/blade runs.
- **[datagen-verified]** `runClientData` ×2: run 1 generated the 4 artifacts above plus the lang
  entry and 2 `.cache` updates (generated tree 3957→3961 files); run 2 md5 diff of the generated
  tree = **0 lines** (idempotent) (`/tmp/eln-fuse10a-datagen{1,2}.log`,
  `/tmp/eln-fuse10a-md5-{0,1,2}.txt`).
- **[automated GameTest]** `runGameTestServer`: **1552 GAME TESTS COMPLETE IN 7.368 min**; log
  line "2 required tests failed :(" = exactly the 2 historical golden fails (`eln:golden_descriptors`:
  six_node@2176–2186 "no such item in golden/1.12/inventory.json"; `eln:golden_slots`: lamp-socket
  bulb set) — no new failures. Shell exit code 1 this run (5A session: 2); the authoritative
  signal is the named failing tests in the log, consistent with the baseline
  (`/tmp/eln-fuse10a-gametest.log`).
- **[env OK]** `mctest doctor` — all HARD checks pass.
- **[live server-side oracle]** live MC 26.3 dev server (boots cleanly — the AGENTS.md
  "12-circuit-breaker mod-load blocker" is confirmed STALE) via mctest RCON:
  `summon minecraft:item 0 -60 0 {Item:{id:"eln:10a_lead_fuse",count:1}}` → "Summoned new 10A Lead
  Fuse"; 5A regression `eln:5a_lead_fuse` → "Summoned new 5A Lead Fuse"; bogus
  `eln:totally_bogus_item_xyz` → "Summoned new Air" (negative control). Registry ID + display name
  resolve at runtime. Probes killed ("Killed 2 entities" — the Air probe spawns no item entity);
  server stopped.
- **[NOT RUN — honestly labeled]** player-joined in-game crafting / fuse-holder installation / trip
  validation NOT performed (client-join remains blocked in this environment per the clutch entry).
  Trip physics exercised only through the shared descriptor code path + the GameTest suite, not a
  live in-world trip.

### Remaining limitations
- `15a_lead_fuse` / `20a_lead_fuse` / `30a_lead_fuse` / `50a_lead_fuse` NOT ported (out of scope;
  separate unported-items rows — same descriptor path would cover them if ever assigned).
- No player-joined in-game validation (crafting, fuse-holder install, actual trip event untested
  in-world).
- No audio / multi-client / fresh-JVM save-recovery validation.

### Known blockers
- Client live-join remains BLOCKED in this environment (clutch-entry blockers unchanged); the
  AGENTS.md "12-circuit-breaker mod-load blocker" is confirmed STALE: `runServer` boots cleanly on
  MC 26.3 and the GameTest server ran all 1552 tests.

### Commit reference
- Local commit for this session (no push): new commit on top of `67edf4a3` on branch
  `port/forkpatch-3.0.77-26.3`, subject `Port 10a_lead_fuse from ForkPatch 1.21.1`; inspect with
  `git log --oneline -3`. External queue script handles pushing.

## 18. Switch port — 20A Switch (2026-11-18, this session)

Scope: port the assigned ForkPatch 1.21.1 six-node switch `20a_switch` (50A follows in the next
commit). No new descriptor or element code was needed — the existing 5A-switch
`ElectricalSwitchDescriptor` registration pattern in `Wp9aContent.java` carries the 20A switch
verbatim; the only target changes are the registration block, the `eln-ids.json` entry, the
datagen recipe line, the item texture, and the generated artifacts. Source baseline `b7444a7b`
(read-only `../ELN1.21.1ForkPatch`); target start HEAD `caa1cc82` (10A fuse commit).

### Items implemented
- **20A Switch** — registry ID `eln:six_node@6214` (group 97 `sixNodeItem`, subId=6; 97×64+6=6214),
  item `eln:switch_20a`, display name "20A Switch". Confirmed absent in the target (unported-items
  row "No matching current item name/model found") and confirmed subId 6 free before implementing.
- Out of scope for this commit and NOT ported: `50a_switch` (next commit this session),
  `100a_switch` (separate unported-items row).

### Original source behavior (ForkPatch 1.21.1)
- Rated low-voltage switch: `ElectricalSwitchDescriptor("20A Switch", stdCableRender200V,
  obj("LowVoltageSwitch"), 600, 12000, 0.01, 600, 12000, cableThermalLoadInitializer.copy(),
  false).power()` — nominal 600 V / 12000 W, series R 0.01 Ω, max 600 V / 12000 W,
  signalSwitch=false, icon `switch` — exact vs source `SixNodeRegistration.kt:1350-1356`
  (subId 6 in source group 97). The source `.power()` call is omitted in the target, matching the
  target 5A convention which also omits it (no behavioral difference: `.power()` only adds a
  power-component entry absent from the target API shape).
- Craft recipe (source `craft/CraftingRecipes.kt:678-684`): pattern `"  I"," I ","CAC"`,
  `'A'` = Rubber, `'I'` = Copper Cable, `'C'` = Copper 12 AWG Cable 600V, output ×1
  (`20a_switch.json` in source).
- Texture: source uses the single shared `textures/blocks/switch.png` for all switches.

### Registry IDs
- `eln:six_node@6214` (group 97, subId 6) — six-node descriptor slot.
- Item ID `eln:switch_20a` (eln-ids `name: "switch_20a"`), recipe file `20a_switch.json`
  (name derived from the display name, matching the source recipe file name), advancement
  `recipes/misc/20a_switch`, lang key `item.eln.switch_20a`.

### Implementation changes (target)
- `src/main/java/mods/eln/registry/batch/Wp9aContent.java` — new `ratedSwitch20a` descriptor block
  directly after the 5A block (same constructor shape as the 5A precedent at L228-230).
- `src/main/resources/eln-ids.json` — new entry between `six_node@6212` and `@6216`.
- `src/main/java/mods/eln/datagen/ElnRecipes.java` — new `addRecipe` line directly after the 5A
  line (same `recipeMiscItem` block).
- `src/main/resources/assets/eln/textures/item/switch_20a.png` — copied from source
  `textures/blocks/switch.png`, **byte-identical** (md5 `158dbf77a748c3f6b20701afad541763` both
  sides; also identical to the existing `switch_5a.png`). This makes `ElnModels.itemTexture`
  resolve `eln:item/switch_20a` instead of the `items/missing` fallback (same mechanism as the
  `switch_5a.png` precedent, commit `70839aea`).
- Generated (datagen): `assets/eln/items/switch_20a.json`,
  `assets/eln/models/item/switch_20a.json` (layer0 `eln:item/switch_20a`),
  `assets/eln/models/item/switch_20a_3d.json` (particle `eln:item/switch_20a`, zero "missing"
  textures), `data/eln/recipe/20a_switch.json` (result `eln:switch_20a`),
  `data/eln/advancement/recipes/misc/20a_switch.json`, lang `item.eln.switch_20a: "20A Switch"`.

### Actual test results (level labeled; nothing claimed that did not run)
- **[compile-verified]** `./gradlew :build` EXIT=0 (5m02s; `/tmp/m1_build.log`).
- **[tests-passed]** `:test` (inside the `:build` run): 704 tests, 0 failures, 0 errors,
  3 pre-existing skips — identical to the 10A-fuse baseline (counted from
  `build/test-results/test/TEST-*.xml`).
- **[datagen-verified]** `runClientData` ×2: run 1 generated the switch_20a artifacts above;
  run 2 `git status` diff = **zero** (idempotent; `/tmp/m1_state_before.txt` vs
  `/tmp/m1_state_after.txt`).
- **[live server-side oracle]** live MC 26.3 dev server (boots cleanly — the AGENTS.md
  "12-circuit-breaker mod-load blocker" is confirmed STALE) via mctest RCON:
  `summon minecraft:item 0 -60 0 {Item:{id:"eln:switch_20a",count:1}}` → "Summoned new 20A
  Switch"; 5A regression `eln:switch_5a` → "Summoned new 5A Switch"; bogus `eln:switch_99a` →
  "Summoned new Air" (negative control). Registry ID + display name resolve at runtime. Probes
  killed ("Killed 2 entities"); server stopped.
- **[NOT RUN — honestly labeled]** player-joined in-game placement/interaction validation NOT
  performed (client-join remains blocked in this environment). Switch electrical behavior rides
  the shared `ElectricalSwitchDescriptor` code path already exercised by the test suites, not a
  live in-world switch.

### Remaining limitations
- `50a_switch` and `100a_switch` NOT ported (50A in the next commit this session; 100A separate
  unported-items row).
- No player-joined in-game validation (placing the switch on a six-node, toggling, in-circuit
  behavior untested in-world).
- No audio / multi-client / fresh-JVM save-recovery validation.

### Known blockers
- Client live-join remains BLOCKED in this environment (unchanged from prior entries); the
  AGENTS.md "12-circuit-breaker mod-load blocker" is confirmed STALE: `runServer` boots cleanly
  on MC 26.3 and the live oracle ran.

### Commit reference
- Local commit for this milestone (no push): commit `20a_switch` on top of `caa1cc82` on branch
  `port/forkpatch-3.0.77-26.3`, subject `Port 20a_switch from ForkPatch 1.21.1`; inspect with
  `git log --oneline -3`. External queue script handles pushing.
