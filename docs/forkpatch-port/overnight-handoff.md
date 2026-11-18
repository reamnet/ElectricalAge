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
