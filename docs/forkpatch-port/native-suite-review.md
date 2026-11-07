# Unfiltered native validation and historical references

The chip-fabrication checkpoint ran **1052 unfiltered GameTests**:
**1050 passed**, two failed, and none were skipped. The failures are
`golden_descriptors` and `golden_slots`. All **586 port cases pass** in this expanded
run. Every other native behavioral and recipe case passes. The complete root run
passes 666 tests with three optional previews skipped (669 selected); all 15 CAD
tests and the default 200 fluid corpus are included.

[Machine-readable differences](native-suite-review.json) retain all failing
assertions and messages. These are **437 assertions in two test cases**, not 437
failed cases. No expected snapshots, exception/skip lists or numeric tolerances
were changed.

| Historical comparison | Observed differences | Review still required |
| --- | ---: | --- |
| Identity absent from 1.12 | 306 | Establish each source identity and intentional destination mapping; additions alone are not approved exceptions. |
| Descriptor values changed | 4 | Four original solar panels use source Norton ratings; compare source constructors and retained native behavior. |
| Slot acceptance changed | 15 | Review added battery categories and their actual charging/discharge limits. |
| Device has no 1.12 container | 91 | Incoming cabinet/pole/control/converter menus need a source-aware reference. |
| Descriptor class has no 1.12 container | 20 | Map new descriptor/menu contracts explicitly; the grounded converters share one native menu registration. |
| Slot comparison summary | 1 | Aggregate of the acceptance differences. |

Small fixed/rotating solar panels report 37 V open circuit and 240 W peak rather than
14.75 V/65 W in 1.12; large arrays report 111 V/1440 W rather than 118 V/520 W. Series
resistance also changes. Pinned source `TransparentNodeRegistration.kt`, the Norton
circuit checks and native world cases supply evidence for reconciliation, not an
automatic waiver.

Native converter 128 and `dc_dc_converter` remain unchanged. Source 129 and131–134
keep their numeric lookups; source 130 has the explicit distinct native
`winding_dc_dc_converter` name. Wire Snips 7873 adds a shared-item identity. Their
real native circuit/menu/crafting and paid-inventory checks pass, but that does
not waive the historical missing-reference assertions. See the
[registry conflicts](registry-conflicts.md), [converter family](converter-family.md)
[Wire Snips](wire-snips.md) and [manufacturing](wire-manufacturing.md).

The earlier unfiltered run also found two order-dependent battery/furnace layouts
because a renewable fixture did not restore shared fake-player facing. The fixture
now saves/restores yaw and pitch in `finally`; those original behavioral cases pass
in this new full run without changing their production devices or assertions.

See [complete current evidence and artifact](session6-validation.json). Full
source-world conversion, remaining equipment/integrations, interactive GPU/audio,
multiplayer and representative loaded-world CPU comparisons remain unfinished.

The native silicon-plate-to-wafer route adds a deliberate recipe to both existing
plate-machine input menus. Their accepted inputs are therefore included in the
retained slot differences. The new Fabricator/basic components add source lookup
identities and a native menu, not an exception to historical comparison checks.
