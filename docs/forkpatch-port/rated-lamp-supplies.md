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

Portable carry, generalized panel channels, source-world conversion and broader
appliance/lighting adapters remain unfinished. Headless publication and menu
checks do not establish interactive GPU, audio or multiplayer parity.
