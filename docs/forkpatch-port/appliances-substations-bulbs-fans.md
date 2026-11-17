# Kitchen, laundry, fusion, substations, bulbs and fans

Continuation from `cfbad5a720d42279c104c00609963d97c5f8477d` on
`port/forkpatch-3.0.77-26.3`. Source remains read-only at
`b7444a7b8a0c5e072b1fd35eed2b74a282c3348c`. The earlier kitchen-assets-only
checkpoint is superseded by the implementations below. This does not complete
all37 workstreams of the whole-mod port.

## Kitchen and laundry

| Source lookup | Device | Native implementation |
|---|---|---|
|7937|Electric Microwave|120V/1000W,6-second paid food processing; powered battery warning/rupture fuse|
|7938|Electric Toaster|120V/800W,5 seconds; bread to toasted bread|
|7939|Flat Electric Cooktop|240V/2000W,10 seconds; food-only smelting recipes|
|7940|Electric Refrigerator|120V/80W;27 food-storage slots, ice compartment, cooling and opening door|
|7941|Electric Coffee Maker|120V/900W,10 seconds; cocoa and water bottle|
|7954|Electric Kettle|120V/1500W,8 seconds; water to hot water|
|7955|Countertop Blender|120V/400W,5 seconds; fruit plus water|
|7956|Electric Rice Cooker|120V/700W,12 seconds; wheat/tagged rice and returned bottle|
|7963|Electric Mini Oven|120V/1300W,8 seconds; food-only smelting recipes|
|7996|Electric Mini Fridge|120V/55W;9 food-storage slots and ice compartment|
|7997|Patio Electric Grill|120V/1800W,10 seconds; food-only smelting recipes|
|8009|Electric Washing Machine|120V/800W,60 seconds; repairs one damaged item, spends1000mB water, seals repaired components in unusable wet carrier|
|8010|Electric Dryer|240V/6000W,120 seconds; restores usable equipment from wet carrier; dedicated30A cord or fixed wiring|
|8011|Fusion Matter Reactor|3200V;100–500kW for600–1800 seconds based on mapped block value; retains sample|

Kitchen work is credited only after accepted electrical solver steps. Solver
trials, outages and item replacement cannot buy progress. Native inventory writes
can bypass change callbacks, so input reference/count/components are also checked
before processing. Blocked outputs/water and wrong voltage prevent paid jobs.
Microwave battery danger requires real powered warning time before rupture.

Five foods (`hot_water`, `fruit_smoothie`, `steamed_grains`, `toasted_bread`,
`fresh_coffee`) and `wet_equipment` are native items. Drinks return bottles;
wet equipment preserves the complete repaired item components and cannot be
used/equipped as the original tool. The native transactional washer capability
accepts water, exposes no external extraction, and reserves one litre per cycle.
Stored water, the reservation, controls, progress, inventories and component data
are saved; no offline work is granted.

Fusion maps server crafting/cooking recipes and datapack seeds using the original
MIT-licensed value graph. `fusion_values/defaults.json`, the blacklist and
`assets/eln/licenses/ProjectE.txt` are retained. Results are plain block items;
sample inventories, enchantments and arbitrary components cannot be cloned.
Reloads invalidate cached values. Unsupported dynamic recipe outputs are excluded.
There is no ProjectE runtime dependency or EMC currency.

Original models, renderer palettes, doors/glass/heating displays and operating
sounds are reused. Kitchen menus preserve source slot coordinates and variable
fridge storage height; laundry/fusion use source208×240 layout.

**Integration limits:** refrigerator environment uses20°C ambient until the room
climate adapter is ported. External spoilage-mod timer manipulation is not ported;
food storage/cooling and freezing work independently. This is not a claim of
source-world conversion, every external recipe/compatibility hook, human GUI
parity or hardware audio certification.

## Standalone and ceiling fans

Source7964–7967 now register Industrial Five Block Fan, Classic Box Fan, Metal
Shop Box Fan and Compact Desktop Box Fan. Their ratings are respectively
240V/1500W,120V/45W,120V/85W and120V/20W. Startup/coast times are12/20,4/8,5/10
and3/6 seconds. Speed controls accept25–100%; absent/wrong power stops the motor
and leaves the rotor coasting. Original moving rotor geometry, sound pitch/volume,
source176×230 controls and collision footprints are retained.

The three small fans use the existing paid cord carrying transaction. Their
original owner/token transfers once, and the saved rotor coasts for actual held
time before replacement. The industrial fan is fixed; its exposed blade disk
uses real living-entity damage with vanilla cooldown, not the whole reservation
box. The five decorative ceiling fans were already implemented with separately
paid motors, physical coasting and the industrial ceiling-blade hazard. This
brings all nine source standalone/ceiling fan types into native registrations.
HVAC/room-climate appliances remain a separate unfinished stream; the source
standalone fans do not implement that system themselves.

## Substations and prerequisite

| Lookup | Equipment | Ratings |
|---|---|---|
|8016|69kV to12.5kV transformer|5MW design loading per phase|
|8017|230kV to69kV transformer|30MW design loading per phase|
|8018|69kV three-phase disconnect|5MW design loading per phase|
|8019|230kV three-phase disconnect|30MW design loading per phase|
|7875|Grid DC-DC Converter prerequisite|source fixed120kV/30kV scalar transformer|

Each station has three independent line loads and three output loads. Terminals
52/53 are the line/load bundles;32–34 and36–38 address individual corresponding
phases. Station terminal orientation stays fixed when spans are linked.
Transformers use the native passive linear `ServiceTap` with source ratio and
0.5% design copper-loss resistor, plus real primary/secondary contacts. Voltage
fluctuations and reverse power are not hidden by a regulator. Disconnects use
real open contacts. Finite reference leakage prevents floating disconnected
banks from making the matrix singular. Changes dirty matrix factors only when
contacts/parameters actually change.

Source station geometry, clearance reservations, moving knife contacts and the
byte-identical voltage/phase label texture are retained. Native240×266 menus
select terminals and operate primary/secondary banks.

Additional native protection is explicit: voltage above1.2× either bank rating
for0.25 seconds trips both banks. Per-phase current above1.05× design current
accumulates `(ratio²−1)×seconds` thermal credit to10; otherwise it cools at1/s.
Only committed electrical steps advance protection. Faults latch and survive
save/load. Reset refuses continuing overvoltage and heat above0.25; transformer
secondary contacts remain independently operated. These are scalar source-model
protection rules, not an AC phasor or utility protection-coordination simulator.
Broader transmission, lightning, multi-client and long-running fault campaigns
remain acceptance work.

The source substation recipe requires Grid DC-DC Converter, which was absent.
7875 now has a real fixed-ratio adapter rather than an inert recipe placeholder.
Its added native reset menu exposes the protected reset, and pickup/replacement preserves latched condition. Its source optional steel/aluminum/aluminium crafting remains tag-conditioned;
without those materials the prerequisite recipe is unavailable, as in the source.
The source winding converter ingredient resolves to native Winding DC-DC
Converter130, preserving native128. No dependency/version changes were made.

## Bulb technologies and compatibility

All18 source-rated variants are registered: incandescent, carbon, fluorescent,
farming, LED and halogen at12/120/240V. Technology defaults are respectively
24/12/168/48/672/96 hours, with source power multipliers1/.75/.5/2/.25/3 on a
60W base and voltage factors.2/1/1.5. Fluorescent warm-up is4 seconds and its
minimum voltage is75% of rating. Native lifetime/condition and lamp socket
processes remain authoritative; this is not a wholesale replacement of inherited
lighting physics or a full source configuration/migration port. The nominal-life
tooltip now displays hours rather than seconds and handles the unsynced server.

Occupied legacy bulb IDs256–294 remain unchanged.120/240V LEDs retain the already
published302/303 destinations. The other16 variants use free304–319 lookups;
source lookup mapping is explicit in `RatedLampBulbs`. Legacy50/200V recipes are
preserved. To avoid indistinguishable conflicting native recipes, the added
source-rated non-halogen family patterns include an iron-plate row; yields,
filament/conductor technology and voltage-specific conductors remain explicit.
Halogen patterns use the source recipe directly. This recipe adaptation is
intentional and does not rewrite historical golden expectations.

## Verification and remaining acceptance

**Final expanded-systems verification: 1536 native selected/1534 passed/2 historical golden failures/zero skips; all1070 ForkPatch cases and all66 added cases pass. Root698pass/3 unchanged optional skips; CAD15pass. Datagen repeats with0 writes. Golden832 is recorded without expectation changes. Live GUI/audio/multiplayer acceptance remains pending.**

See `expanded-systems-validation.json` for observed counts and evidence paths.
Focused tests cover all kitchen devices, washer/dryer/fusion progress and completed
saved cycles, all18 bulbs under real fixture power, source bulb condition/rating,
all four standalone motors/controls, paid fan carrying/coasting, real industrial
blade damage, three-phase station ratios/open contacts/conservation, actual
insulation/overload trips, fault reset and native save/publish/render/menu paths.
The in-engine renderer checks are headless geometry extraction; they are not
screenshots or real client interaction checks.

Pending: private-world live client menu/drag/shift-click interactions, visual and
audio comparison, multiplayer, fresh-JVM restart recovery, automation/compatibility
campaigns, optional-metal progression, thermal room/spoilage bridges and complete
old-world migration. Preserve pinned Gradle settings and all existing tests,
goldens, tolerances and skips when continuing.

## Resume and reproduce

Keep draft PR1 title/base/draft state and use the existing branch. Fetch/inspect
newer commits and local edits before continuing. Read `AGENTS.md`, `CLAUDE.md`,
`docs/testing.md` and the validation JSON first. Java25/Java21 are already installed;
`/tmp/eln-port-java-env.sh` supplies them without changing pinned Gradle settings.

```bash
source /tmp/eln-port-java-env.sh
python3 tools/mctest/mctest doctor
./gradlew --no-daemon --max-workers=2 :build :cad:test
```

Use a **fresh disposable universe** for every native test run; the harness can
replace the directory it is given. For the66 added cases:

```bash
./gradlew --no-daemon --max-workers=2 runGameTestServer \
  --args='net.neoforged.fml.startup.GameTestServer --tests eln:forkpatch_expanded_* --report /tmp/eln-expanded-NEW.xml --universe /tmp/eln-expanded-NEW-world'
```

Remove `--tests` for the full regression. Run `runClientData` twice and check the
second writes zero files. Run `tools/port/review_native_results.py` on the report
and captured log to inspect actual failures; do not automatically approve a red
run merely because historical goldens were previously the only failures.

For later live verification, give/place the registry names from `eln-ids.json`
in a new private test world and use `/elnlab devicemenu <lookup>` from a connected
player after placement. This helper is not a substitute for interacting with the
real slots, buttons, cords, fluids and fault controls. Do not compile main classes
while test/server/client JVMs are using the same build outputs. Shut those JVMs
down first. Never point testing tools at an existing user world.
