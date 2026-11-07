# Power-electronics adapters and remaining review

Reviewed against pinned source `b7444a7b8a0c5e072b1fd35eed2b74a282c3348c`.
The variable inductor 6179 is now adapted. The variable converter129 now has a production adapter, native menu/publication,
paid-winding protection and native save tests; see [the adapter](variable-dcdc.md).
Fixed130 and grounded one-way131–134 now have [native adapters](converter-family.md).
Development-only isolation135, the broader inherited shared-code review and full
source-world conversion remain open.

## Preserve the existing converter identity

The source calls `registerTransformer(2)`:

| Source lookup | Source descriptor | Destination at this checkpoint |
| --- | --- | --- |
| 128 (2:0) | Hidden Legacy DC-DC Converter | Existing `TransformerDescriptor`, `dc_dc_converter`; preserve its identity/saves |
| 129 (2:1) | Variable DC-DC Converter | Registered native adapter, `variable_dc_dc_converter` |
| 130 (2:2) | DC-DC Converter | Registered as `winding_dc_dc_converter`, preserving native128 |
| 131 (2:3) | One-way DC-DC Converter | Registered native adapter |
| 132 (2:4) | One-way Boost vDC/DC Converter | Registered native adapter |
| 133 (2:5) | One-way Buck vDC/DC Converter | Registered native adapter |
| 134 (2:6) | One-way Boost/Buck vDC/DC Converter | Registered native adapter |
| 135 (2:7) | Isolation Transformer | Free lookup; source registration is development-only |

The duplicate display/registry name at 130 must be resolved explicitly while keeping
native 128. A free numeric lookup does not make a duplicate native registry name
valid. Choose an explicit new native name and record source-import mappings when
implementing it; do not replace the existing converter or silently reinterpret a
native world as a source world.

## Shared behavior requiring integration checks

- `DcDcControl.kt` stores a version, mode, value and enable state. Missing version
  means historical linear V1 signal mapping. V2 allows logarithmic mapping and
  voltage targeting. Do not upgrade old saves implicitly or reinterpret a voltage
  value as a ratio when changing modes. Source packet IDs 40–43 are transactional
  and avoid the base grounding packet.
- Existing native `ControlMapping`, `ConverterCore`, checked probes,
  `SafeTransformerProcess`, conservative power processes and cached LU factors are
  already present. Reuse those validated implementations, including accepted-step
  energy/thermal timing and finite network checks. Presence of these helpers does
  not establish complete device/UI parity.
- Both source and current native `UtilityCableDescriptor` extend
  `ElectricalCableDescriptor`. The native spool already exposes
  `getRemainingLengthMeters`, `setRemainingLengthMeters`, `segment` and `consume`.
  Winding amount must come from the paid metre ledger for utility wire and from
  stack count for older cable items. Preserve partial lengths, melted state,
  conductor selection and inventory/drop accounting.
- `DcDcWinding.kt` has separate strict V1 and flexible V2 construction rules,
  real winding resistors, per-winding voltage/current limits, insulation/melt
  handling and temperature carried on removed wire. Its thermal heat accumulator
  must integrate with the native accepted-step delivery rather than count algebraic
  solver retries as elapsed time. Utility multicore resistance/mass/cooling uses
  one selected conductor, not free thermal mass from unused cores.
- Native wire thermal geometry already caches radius/surface/jacket quantities.
  Preserve that optimization when adapting the source winding model. Check cooling
  and absolute/ambient temperature conversion before changing shared thermal code.
- Adapt the source models/coil sizing, explicit client factories, one-way mode
  displays, native slot predicates, settings/configuration packets and crafting
  patterns together. Use real publication and paid hot-wire removal/reload tests.

The variable converter129 checkpoint implements shared controls/windings.
Fixed130 and one-way131–134 now share those winding/model adapters.
Next: development isolation135, broader inherited shared-code review and source-world conversion.
Required cases include V1 saves, disabled-by-default V2 upgrades, voltage-target
zero/off, reverse flow, short/overload recovery, heterogeneous cable windings,
paid partial-spool refunds, hot/melted inventory saves, real menus, source models
and coupled renewable/drive networks. Representative loaded-world CPU comparisons
against both complete baselines remain separate work.
