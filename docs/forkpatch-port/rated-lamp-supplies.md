# Rated lamp-supply continuation

Source baseline: `b7444a7b8a0c5e072b1fd35eed2b74a282c3348c` in
`../ELN1.21.1ForkPatch`. Starting native checkpoint: `527e3061` on PR #1's
existing `port/forkpatch-3.0.77-26.3` branch.

## Identity and manufacturing

| Source six-node lookup | Source device | Native six-node lookup | Native item |
| --- | --- | --- | --- |
| 4160 | 120V Lamp Supply | 4162 | `eln:120v_lamp_supply` |
| 4161 | 240V Lamp Supply | 4161 | `eln:240v_lamp_supply` |

Native lookup **4160**, `eln:lamp_supply`, remains the established **Lamp Supply**.
Its name, recipe, cable slot, channel behavior and existing saves remain valid;
it has no declared mains class and is deliberately excluded from appliance-cord
inputs. The source lookup helper makes the conflict explicit. It is not a
source-world converter.

The original four-iron/one-copper recipe still produces the generic native
supply. Craft that paid enclosure by itself to obtain the 120 V adapter. This
staged recipe avoids two competing outputs for the source's identical 120 V
pattern. The 240 V adapter uses the original source pattern: two iron ingots,
two Advanced Machine Blocks and one copper ingot. Basic Machine Blocks cannot
replace the advanced components. Both adapters reuse the native DistributionBoard
enclosure, inventory icon, screen, render factory and menu rather than importing
the source's obsolete client hooks.

## Electrical and channel behavior

The adapters feed the existing native `LampSupplyElement.powerLoad`. Their cords
use the existing paid socket reservations, resident-only owner lookup, native
unload/reload lifecycle and passive conductor resistance. Nominal acceptance
matches the source `MachineCordDevice`: 120 V accepts the 120 V cord; 240 V
accepts the two existing 240 V cord classes. A cord's selected socket must still
match its exact connector class. Wrong-voltage cords cannot claim a socket.

While plugged, the original power inlet is isolated through the shared native
SixNode and VoxelNode connection policy. Unplug restores only its original front
terminal. Signal/thermal machinery and other hosted parts keep their native
ownership. Stable cord and channel polls do not rebuild cached LU matrices.

All three native lighting channels, wireless signal controls and optional cable
range extensions remain in use. A rated supply can also feed the existing
wireless outlet bridge: the actual upstream source pays the downstream load and
the input cord, outlet contacts and bridge losses exactly once. These are passive
connections; the rating does not regulate voltage or create a local breaker.
The native lamp-demand accumulator remains authoritative for ordinary lamp
consumers. This continuation does not relabel the destination's 50/200 V bulbs
as 120/240 V bulbs or port the remaining source lighting families.

## Bounded native channel decoding

The adapter review reproduced inherited failures on the generic and both rated
supplies. Invalid packet channel indices threw; invalid aggregator bytes were
stored and later indexed a three-element array; a fourth saved channel overran
the owner's fixed arrays. Wireless-control name edits also registered phantom
power channels, leaving stale entries when the owner unloaded.

The shared native decoder now rejects invalid packet indices/modes before
mutation, reads at most three saved channels, restores sparse indexed saves and
resets absent channels when reading legacy one-channel data. Invalid saved
aggregators recover to the native Toggle default. Wireless-control edits change
only the signal-control setting. Valid power names, all three aggregator modes,
toggle history and ordinary native saves keep their existing serialization.
Regression cases run on all three identities, including the preserved generic
supply; malformed/truncated packets, repeated reads and exact valid round trips
remain covered separately from the new electrical adapter.

## Validation scope

Production and historical-golden evidence is recorded in
[rated-lamp-supplies-validation.json](rated-lamp-supplies-validation.json).
The native cases cover the identity conflict, loaded crafting recipes, actual
survival cord use/refund, both ratings, legacy/voxel inlet isolation, paid owner
reload, each native channel, passive power/loss accounting, source unload and
cached LU preservation. Existing tests, expected goldens, skips and tolerances
remain intact.

## Local client and final evidence

The local continuation installs Temurin **21.0.13+9** and **25.0.5+8** in
`/tmp/eln-port-jdks`, with its dependency cache at `/tmp/eln-port-gradle`.
`source /tmp/eln-port-java-env.sh` selects those homes for subsequent local
commands. These are temporary local installations, not changes to the pinned
Gradle wrapper, properties or build script.

The starting checkpoint passes 680 root tests (three optional previews skipped)
and 15 CAD tests. Its unfiltered native run reproduces 1,203/1,205 passes and all
469 historical golden failed assertions before implementation begins.

The final production content, including the title-layout fix, is committed at
`19ad0deea05aad49f213fc92ea29321043b792df`:

- Full root build: **683 selected, 680 passed, three existing optional previews
  skipped**; default **200-case fluid corpus** included. All **15 CAD tests** pass.
- Unfiltered native: **1,224 selected, 1,222 passed**, zero skips. All **758
  ForkPatch cases**, including **19 rated-supply cases**, pass.
- Only historical `golden_descriptors` and `golden_slots` fail. All **469**
  previous failed assertions remain unchanged; the two identities and two
  containers add **four** visible differences, totaling **473**.
  [Exact delta](rated-lamp-supplies-golden-delta.json). No expected reference,
  existing test, skip or tolerance was changed.
- All **558 provenance entries** pass. All **678 previous identities** and
  **997 English entries** retain their exact values; two of each are added.
  Repeat data generation writes **zero files**.
- The clean development JAR matches all **1,774 Java classes, 889 Kotlin classes
  and 4,971 processed resources**. Build marker `19ad0deea05a`;
  SHA-256 `cb533255661128722af05d12d77c4004bcff31f2fb9db575389823cd71baa434`.

Controlled interaction uses software OpenGL/llvmpipe on a temporary Xvfb display,
1280×720 with automatic GUI scale. The world is a copy of this session's native
GameTest world; no user world or 1.21.1 source world is used. Commands seed the
exhibit floor, materials and initial player position. Native item placement,
menus, inventory transfers and survival cord attachment are actual interactions.

- [120 V controls](evidence/rated-lamp-supplies/120v-controls-720p.png): all three
  names and Biggest/Smallest/Toggle controls; seven real cables increase range
  from 32 to 39 blocks.
- [240 V controls](evidence/rated-lamp-supplies/240v-controls-720p.png) remain
  independently configured.
- [Two paid survival cords](evidence/rated-lamp-supplies/paid-cords-720p.png): one
  cord of each matching rating is consumed, leaving two of each in inventory.
- [Completed resource reload](evidence/rated-lamp-supplies/resource-reload-720p.png)
  retains both native meshes and physical endpoints.
- [Fresh-JVM restoration](evidence/rated-lamp-supplies/fresh-jvm-120v-720p.png)
  retains channels, aggregator modes and all seven installed cables. Direct
  comparison of the four native saved owners also verifies unchanged paid cord
  tokens, source UUIDs and matching socket reservations for both ratings.

The first visual pass caught [header overlap](evidence/rated-lamp-supplies/header-before.png):
the shared layout measured only the shorter generic title. Its title sample now
includes the longer rated name; a fresh JVM verifies the corrected spacing.
All three virtual-display client runs save and exit normally. The initial WSLg
attempt can capture the window but loses input focus; it is not counted as a
successful interactive check.

The manual exhibit outlets are unpowered. Real accepted work and all passive
losses are covered by the production native cases, separately from this geometry
and ownership pass. Hardware graphics, audio, multiplayer and universal
resource-pack parity remain unverified.

Portable carry, generalized panel channels, source-world conversion and broader
appliance/lighting adapters remain unfinished. Headless publication and menu
checks do not establish interactive GPU, audio or multiplayer parity.
