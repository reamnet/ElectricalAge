# Twelve-circuit breaker panel

Native transparent-node lookup **7988** preserves the pinned ForkPatch lookup.
The original `twelvecircuitpanel` OBJ and both MTL files are copied byte for byte
and included in the native model index. The source menu is312×386 with twelve
separate breaker slots at `(8 + column*152, 34 + row*20)` and player inventory
at `(8,304)`; controls are beside those slots rather than covering them.

## Electrical behavior and controls

The panel has independent L1/L2/240V buses and twelve separately protected
branches. Branches alternate L1/L2 by index, with a240V option per branch. Top
or bottom receives the240V service feeder; the opposite vertical cable port
serves the selected branch. Changing that selection or feeder side reconnects
the real cable topology. Native `WiredServiceSplit` supplies120V equivalents
only from an actual placed240V feeder, using the existing passive service taps.
Saved grid terminal/circuit identities match the native service family.

Each branch requires a paid breaker item. Thermal accumulation follows the
source198-unit budget: `(abs(I)/rating)^2 - 1` above105% rated current; cooling
is6.6 units/second. Trips remain latched until explicit reset or a real hardware
rating replacement. Source `panelNEnabled`, `panelNTripped`, `panelNHeat` and
`panelN240` keys are retained. Item settings alone never copy breaker inventory.
SCADA cannot remotely close a tripped or empty branch.

Power and switch channels, Any/All/Toggle aggregation, dusk hysteresis, source
range choices32/64/128/256/512, and feeder controls use the original event IDs.
The shared wireless adapter includes native lamp supplies and panel outputs.
Nearest resident sources win within the same dimension, including OFF sources.
Persistent consumer demands replace an identity's previous resistance; physical
String Lights bridges use real conductors. Disconnect removes demand/bridges
before the source leaves the solver. No conductor is registered twice.

## Paid hardware and compatibility

The existing generic breaker at6272 and meters/fuse holder at6276–6278 retain
their identities. Source rated breakers are added alongside them:

| Rating (A) | Native six-node lookup |
|---|---|
| 5 | 6288 (source6272 collides with the existing generic breaker) |
| 10 / 15 / 20 | 6273 / 6274 / 6275 |
| 30 / 40 / 50 / 60 / 100 | 6279 / 6280 / 6281 / 6282 / 6283 |
| 125 / 200 / 400 | 6285 / 6286 / 6287 |

The legacy generic breaker remains usable as a400A panel item; its standalone
behavior remains the historical cable-based limit. Rated standalone breakers
use the smaller of cable and hardware current limits.

The source breaker recipes depend on a5A switch (6213) and four rated control
relays (6021–6024). These are real native devices, with source contact voltage,
current, resistance and5V/12V coil thresholds, rather than recipe-only items.
Existing generic relays retain their saved IDs and behavior. Rated relay faults
use the native failure system and current-overload budget; this preserves the
port's burnout/repair behavior rather than introducing the source's explosion
implementation. Their switch/relay wire recipes and all twelve breaker recipes
retain the source materials. The panel recipe comes from sourceUtilityRecipes.

## Validation

The selected native run `/tmp/eln-requested-panel-forkpatch-v4.xml` passes
**962/962** ForkPatch cases, zero skips. It includes real wall-fed120/240V
branches, actual overload isolation and saved trip/SCADA lockout, all12 slots,
complete native render publication, physical relay coil/contact behavior and
wireless paid-demand/disconnect cases. Root698/701 pass with three unchanged
optional preview skips; CAD15/15 pass. Datagen succeeds with exact source recipes.

An earlier unfiltered1424-case run exposed8 non-golden issues, now corrected
and covered by the passing ForkPatch regression. See
[validation](panel-carry-validation.json) for the distinction between runs. The
746 exact golden assertion differences retain all714 earlier assertions and
add32 for new source identities/menus; expectations are unchanged. Live GUI
checks and the final unfiltered rerun remain pending.
