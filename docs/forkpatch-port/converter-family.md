# Fixed and one-way DC/DC converters

The grounded source converter family now has native adapters using the shared
paid-winding, menu, thermal, publication and source-model implementation.

| Source lookup | Native lookup/name | Transfer policy |
| --- | --- | --- |
| 129 | 129 `variable_dc_dc_converter` | Reversible variable ratio or voltage target; see [variable converter](variable-dcdc.md) |
| 130 | 130 `winding_dc_dc_converter` | Reversible ratio from installed secondary/primary winding metres or legacy cable counts, bounded to 1/256–256 |
| 131 | 131 `one_way_dc_dc_converter` | One-way fixed winding ratio |
| 132 | 132 `one_way_boost_vdc_dc_converter` | One-way boost |
| 133 | 133 `one_way_buck_vdc_dc_converter` | One-way buck |
| 134 | 134 `one_way_boost_buck_vdc_dc_converter` | One-way boost/buck |

Native128 remains `TransformerDescriptor` and `dc_dc_converter`, with its old item,
recipe and save path. Source130's display name would collide with that existing
identity, so the new native display is **Winding DC-DC Converter**. The explicit
`FixedDcDcDescriptor.nativeRegistryNameForSource(130)` returns its distinct native
name and does not remap128. That name mapping is not full source-world conversion.
Source135's development-only floating Isolation Transformer is not registered.

Both reversible variants reuse native `SafeTransformerProcess`. The fixed unit has
no signal terminals and ignores variable-control packets; only physical windings
select its ratio. One-way variants retain the source V1 ratio/power policy and
matched winding rule for historical variable devices. Explicit V2 upgrade selects
ratio1 and remains disabled. V2 fixed one-way converters reject reverse transfer
and open when fixed-ratio current/voltage limits cannot be met. V2 variable units
reuse native `RegulatedPowerProcess`, with installed conductor current/voltage
limits, mode-specific gain bounds, 1 MW electronics ceiling and 97% electronics
efficiency. They cannot backfeed an energized output. A boost unit opens when a
short would require gain below1, and can current-limit when the requested gain is
physically available; removing the fault restores healthy windings.

Actual winding resistors account for copper losses separately. Electrical heat is
sampled only after accepted solver steps. Legacy settings and thermal/item saves
remain native, with no hardware/heat copied into carried control settings. V1
saves are not silently upgraded. The [variable winding guide](variable-dcdc.md)
describes fractional paid lengths, hot-wire extraction and deferred damaged-stock
replacement shared by the entire new family.

Solver trials read configured winding caches, avoiding inventory reads and NBT
updates during algebraic convergence. Native renderers cache winding descriptors
when publication arrives, rather than parsing spool metadata on every frame.
Fixed units omit the unused signal state. Stable control/thermal properties retain
native cached LU factors; the solver and shared fluid/mechanical infrastructure
are unchanged. These are scoped work-reduction checks, not comparative loaded-world
CPU measurements against both complete ports.

All six incoming converters have source OBJ/MTL/texture/icon assets, native model
index entries, filtered four-slot menus, item models, recipes and registered client
factories. Fixed screens display physical ratio; variable screens expose mode/value,
enable and explicit upgrade packets. Source recipes use cable material, basic chips,
redstone and signal diodes in their distinct patterns. Native128 still uses its
original copper coils. The source buck upgrade uses a basic chip, not redstone.
UPS and standalone Mains Step-down recipes now use variable129, resolving the
temporary native128 ingredient substitution. Existing native utility-pole/grid
recipes continue to reference their historical converter128.

See [validation](converter-family-validation.json). Remaining work includes the
development isolation unit, the broader inherited transformer/capacitor review,
full source-world conversion, copy-tool integration, interactive GPU/audio and
multiplayer checks, historical goldens and representative loaded-world comparisons.
