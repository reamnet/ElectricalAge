# Distribution continuation after the current-variant checkpoint

The latest continuation passes 532 destination regressions, three optional preview
skips and 182 native cases. See [its validation](legacy-distribution-validation.json)
and [the parity report](distribution-parity.json). All eighteen current source
construction variants and seven hidden variants are implemented. The fused-tap
lamp (8022) and retired non-light three-phase cabinet (8034) are now covered;
five hidden legacy adapters remain.

## Hidden legacy adapters

- **8023, transformer plus fused-tap street light:** this needs two contact/fuse
  roles. The current ordinary transformer contact group must not be reused as
  the separate HV branch. Keep transformer-primary pickup separate from branch
  pickup and prove that one blown fuse cannot disable the other healthy path.
- **8036, retired three-phase underground cabinet with light:** preserve its three
  independent primary circuits and hidden identity. Adapt the source cabinet
  model, five-block fixture clearance and saved channels; keep new overhead links
  disabled. Do not expose retired equipment through new recipes.
- **8035, single-phase underground cabinet with light:** preserve cabinet
  terminals and real LV lamp supply, and adapt the five-block fixture clearance.
- **7949, twin LED pole:** preserve both heads and the shared 120 W consumption,
  source 120/240 V selection, and recovery-only inventory for genuine old hardware.
  Settings or an old rating field alone cannot manufacture a transformer/fuse.
- **8132, single-phase feeder with light:** retain the fused underground primary,
  independent secondary service, 120/240 V lamp selection and existing riser ports.

Source references are `utility/DistributionPole.kt`, `DistributionClient.kt`,
`UndergroundCabinetVisual.kt` and the utilitylight fixture controls in the pinned
ForkPatch checkout. Keep original offsets and hidden flags.

For the twin-pole recovery, also inspect `TwinPoleMigration.kt` and
`PoleHardware.migrate`: the source gates translation on the absence of
`utilityLamp` and presence of `mainsOutputVoltage`. Carry its old mains/aim and
`poleSelectedTerminal` settings into the new keys once. Higher-rating conversion
requires an actual old step-down item in slot zero; validate the saved rating
against the five supported ratings before conversion. Preserve the actual
`poleFuse` cartridge and prove that repeated reloads and carried-item reads cannot
repeat recovery. These recovery adapters are still pending, not implemented by
this checkpoint.

## Remaining related work

Paid streetlights currently attach to the adapted distribution poles. Other
source-supported pole families still need their native secondary-port adapters;
check `LegacyPolePorts.kt`, `PoleStreetlightAttachment.kt` and native
`ElectricalPoleElement` before adding any legacy-pole hook. Never use its primary
load as an attachment's secondary supply.

Adaptive primary/service alignment, can/rack clearance routing, height/surge
attachments, wire manufacturing/trimming and full source-world migration remain
separate work. Preserve current catenary caches, paid spool/jacket ledgers,
phase-inheritance guards, accepted physical-step energy accounting and existing
JVM method descriptors when extending them. The broader appliance, vehicle,
industrial and optional integration workstreams are still tracked in
[the main port notes](../forkpatch-port.md).
