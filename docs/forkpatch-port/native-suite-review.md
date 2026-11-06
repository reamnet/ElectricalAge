# Unfiltered native validation and historical references

The solar-roof, variable-inductor and sampling checkpoint ran **810 unfiltered
GameTests**: **808 passed**, two failed, and none were skipped. The failing cases
are `golden_descriptors` and `golden_slots`. All 344 port cases pass within that
expanded run, including pitched solar roofs, the variable inductor and the 96-panel
sampling scene. Recipe references and every other behavioral case pass.

The initial unfiltered run also exposed two layout failures: battery low-voltage
disconnect and heat-furnace external control. The wind/watermill fixture changed
the shared fake player's facing without restoring it. Subsequent layouts then
placed supplies outside their platforms or inside the furnace. Saving and
restoring yaw/pitch in `finally` fixes both; the rerun verifies the original
battery protection and furnace signal behavior without changing either device.

[Machine-readable differences](native-suite-review.json) retain all failing
assertions and their original messages. These are **404 assertions in two test
cases**, rather than 401 failed test cases:

| Historical comparison | Observed differences | Review still required |
| --- | ---: | --- |
| Descriptor identity absent from 1.12 | 286 | Establish source identities and explicit destination conflict mappings against pinned 3.0.77; do not derive approvals from current target IDs alone. |
| Descriptor values changed | 4 | The four original solar panels now use ForkPatch Norton ratings; review the source constructor values and retained destination behavior. |
| Slot acceptance changed | 15 | Legacy battery slots recognize added battery types; review item categories and actual charging/discharge limits. |
| Device has no 1.12 container | 81 | Incoming cabinet/pole/control families need a source-aware menu reference. |
| Descriptor class has no 1.12 container | 17 | Native adapters use new descriptor classes; map each reviewed source menu contract explicitly. |
| Slot comparison summary | 1 | Aggregate of the acceptance differences above. |

The solar differences are specific: small fixed/rotating panels report 37 V open
circuit and 240 W peak, versus 14.75 V/65 W in 1.12; 2x3 panels report 111 V/1440 W,
versus 118 V/520 W. Their series resistance also changes. The pinned source's
`registration/TransparentNodeRegistration.kt` defines `smallSolarVoc/Vmp/Imp/Isc`
and the large-array series/parallel arrangement. Those source values and the
passing Norton circuit tests are evidence for review, not an automatic waiver.

The variable-inductor addition contributes one new historical identity, one device
without an old container and one new descriptor class. These remain explicit
review differences; the passing native core/menu cases do not waive them.

No expected golden JSON, skip list, assertion, or comparison tolerance was
changed. Completing historical reconciliation and achieving a passing unfiltered
suite remain open work. Full source-world conversion, interactive GPU/audio and
multiplayer checks, and the remaining feature families are separate requirements.

Reproduce the review after a fresh unfiltered run:

```sh
python3 tools/port/review_native_results.py \
  --report /tmp/eln26-native.xml \
  --log /workspace/.cloud-setup/native.log \
  --output docs/forkpatch-port/native-suite-review.json
```
