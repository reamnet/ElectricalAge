# ForkPatch decorative lighting on NeoForge 26.3

The native catalog contains all **108 decorative fixtures** from ForkPatch
`b7444a7b8a0c5e072b1fd35eed2b74a282c3348c`. This includes the five ceiling fans,
Outdoor Motion Floodlight and Adjustable Suspended Warehouse Light. The
[fixture matrix](lighting-port-matrix.md) lists every source lookup, model and
remaining shared review obligation. Original fixture OBJ/MTL meshes and icons
are byte-identical to the pinned source.

This completes the fixture registrations and their dedicated adapters; it does
not complete portable carry, partial-block mounting, all source bulb technologies
or source-world conversion. See [remaining work](remaining-work.md).

## Using the fixtures

Insert bulbs individually or through the fixture menu, up to its source capacity.
All installed bulbs contribute parallel electrical demand and retain their saved
quantity and condition. Used bulbs cannot silently merge with fresh bulbs. The
source-rated 120 V / 15 W and 240 V / 22.5 W LEDs use native IDs 302 and 303.
Existing 50/200 V bulb identities and recipes are preserved.

Fixtures retain the native lamp-supply channel and built-in LV cable modes.
Wireless-only recessed fixtures do not expose cable terminals. A paid appliance
cord overrides wireless input while attached; a disconnected or missing outlet
cannot leave a second wireless input active. Portable carrying remains separate
work.

- **Aimed fixtures:** select a head, change pan/tilt in 5° steps, or reset it.
  Each head keeps its own saved angles. Its visible housing and emitted beam
  use the same pivot. Parking/street heads retain the source axial pan behavior.
- **Motion floodlight:** choose night motion, motion at any time, always on or
  always off. Living entities must be in front, within eight blocks and visible
  through the actual world geometry. Movement holds the light on for 30 physical
  seconds; a stationary entity does not keep refreshing that timer.
- **Ceiling fans:** the fan switch controls a separately paid motor. A physical
  power lead is required, even when bulbs use a wireless channel. Startup takes
  five seconds and coasting takes ten seconds. The industrial fan's actual blade
  disk is hazardous while spinning; the full reserved volume is not a damage box.
- **Suspended warehouse light:** adjust extra drop from 0–7 m in 0.25 m steps.
  The model and emitted light move together and the setting survives reload.
- **Pool fixtures:** projected light leases can occupy source water. They preserve
  water while lit and restore it when the last lease expires. Light projection
  never requests missing chunks or replaces other fluids.

The standard panel is 176×166, aimed panels are 176×242 and suspension panels are
176×198. Bulb slot and controls follow source coordinates. The native inventory
and menu are sized together. Identical coordinates and models do not establish
universal pixel parity across game renderers or resource packs.

## Recipes

All **108 fixture recipes** now follow the pinned source: 103 shaped recipes and
five source mini-dot shapeless recipes. The 47 shaped upgrades pay their original
base, three upgrade materials and an iron plate. All 20 outdoor recipes are
present, including 4495–4499. Earlier statements that these outdoor recipes were
absent from the source were incorrect.

Standing Drum Floor Lamp uses Ceramic Drum Table Lamp, Industrial Wall Pack
uses Warehouse Wall Floodlight, and seasonal/party upgrades that name String
Lights use that actual device. Source String Lights is registered at transparent
lookup 4418, with its original model, wall mounting, configurable 120/240 V,
15 W demand, saved controls and paid two-item bootstrap recipe.

The source LED ingredient costs overlap legacy native bulb recipes. Distinct
paid recipes retain the additional iron plates already used by this port, so
neither old bulb recipes nor their output voltages change.

## Review and verification

[Review since bb962e1](review-since-bb962e1.md) documents the recipe, rendering,
underwater, wireless ownership and simulator lifecycle corrections. Runtime
results, exact commands and limitations are in
[lighting validation](decorative-lighting-validation.json). Historical golden
expectations remain unchanged; the observed differences are recorded separately
in [the golden delta](decorative-lighting-golden-delta.json).

The operator-only `/elnlab lamps x y z` builds a reproducible 108-fixture client
scene using native saved electrical sources, lamp supplies and fan cords.
`/elnlab lampmenu LOOKUP` opens a resident fixture's real menu for GUI checks.
These commands create creative-mode lab hardware; they do not establish survival
material accounting. Native recipe, insertion, solver and ownership tests cover
that accounting independently.

Representative controls were also exercised in a live client connected to a
dedicated server: aim pan/reset/head selection, motion-light mode, fan on/off,
warehouse-suspension adjustment, and String Lights voltage/channel controls.
The String Lights panel was reopened after a server save and restart and showed
the retained 240 V setting and `gui test channel`. Its heading now follows the
source layout without overlapping the inventory or supply label. Screenshots
are in [interactive GUI evidence](evidence/decorative-lighting/interactive-aim-panel.png),
[motion](evidence/decorative-lighting/interactive-motion-off.png),
[fan](evidence/decorative-lighting/interactive-fan-off.png),
[suspension](evidence/decorative-lighting/interactive-suspension-1-25.png) and
[reloaded String Lights](evidence/decorative-lighting/interactive-stringlights-reloaded.png).
This is representative interaction coverage; it does not verify every fixture
panel, multiplayer behavior, hardware GPU output or audio.

Earlier four-owner software-OpenGL evidence remains available:
[gallery](evidence/decorative-lighting/fresh-client-gallery.png),
[shop light](evidence/decorative-lighting/shop-light-close.png),
[saved channel](evidence/decorative-lighting/post-saved-channel.png),
[five bulbs](evidence/decorative-lighting/five-bulb-chandelier.png) and
[saved-owner checks](decorative-lighting-saved-owners.json).
These are historical observations, not verification of the newly enabled specials.
