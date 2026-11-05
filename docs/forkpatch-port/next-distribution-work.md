# Distribution continuation after the legacy lighting checkpoint

The latest checkpoint passes 545 destination regressions with three optional CAD
preview skips and 200 selected native server cases. Data generation covers 617
registry identities without missing icons. See [validation](legacy-lighting-validation.json)
and [variant parity](distribution-parity.json). All eighteen current construction
variants and eleven hidden legacy variants are implemented: 29 of 30 in total.

## Remaining hidden adapter: twin LED secondary pole (7949)

Preserve both heads and their shared 120 W load, explicit 120/240 V selection,
and recovery-only inventory for genuine old hardware. Settings or an old rating
field alone cannot manufacture a transformer/fuse. The existing standalone Mains
Step-down Unit identity must be adapted so actual old items can be recognized.

Inspect `TwinPoleMigration.kt` and `PoleHardware.migrate` in the pinned source.
Settings translation is gated on the absence of `utilityLamp` and presence of
`mainsOutputVoltage`. Carry old mains/lamp/dusk/aim/height and
`poleSelectedTerminal` settings into the new keys once. Higher-rating conversion
requires an actual old step-down item in slot zero and a supported rating. Preserve
an actual old `poleFuse` record in recovery slot one. Repeated reloads or carried
reads must not repeat recovery. Never reconnect a former HV span as an LV supply.

## Remaining related work

Paid streetlights currently attach to adapted distribution poles. Other source
pole families need native secondary-port adapters (`LegacyPolePorts.kt` and
`ElectricalPoleElement`); do not use a primary load as an attachment's LV supply.
Adaptive primary/service alignment, can/rack clearance routing, height/surge
attachments, wire manufacturing/trimming and full source-world migration remain.
Keep catenary caches, paid spool/jacket ledgers, phase-inheritance guards, physical
step energy accounting and existing JVM method descriptors.

The unfiltered native smoke run also observed gallery placement/menu harness
failures outside the selected port validation. See [findings](unfiltered-native-smoke-findings.json);
that run is not a passing full-native baseline. Broader work remains in
[the port notes](../forkpatch-port.md).
