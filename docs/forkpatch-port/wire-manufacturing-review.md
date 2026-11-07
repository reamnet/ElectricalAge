# Wire manufacturing source contract

Reviewed against source `b7444a7b8a0c5e072b1fd35eed2b74a282c3348c`. Wire Snips 7873
and the three processing machines/supporting items are now adapted. This retained
source review is the implementation contract; current behavior and evidence are
in [the native manufacturing guide](wire-manufacturing.md).

`TransparentNodeRegistration.registerWireProcessingMachines(72)` defines:

| Source lookup | Device | Source power/rate | Existing source mesh |
| --- | --- | --- | --- |
| 4608 | Wire Roller | 300 W, 6 m/s | `platemachinea` |
| 4609 | Wire Insulator | 160 W, 4 m/s before layer cost | `fabricator` |
| 4610 | Wire Combiner | 120 W, 5 m/s | `magnetizera` |

`ItemRegistration.registerWireMachineItems(118)` defines shared items7552 Iron
Roller Wheel,7553 Steel Roller Wheel,7554 Aluminum Roller Wheel and7556 Wound Wire
Bundle. These source lookup slots were free in the645-entry baseline and are now
registered without replacing earlier entries. Existing native wire,
spans, metre ledgers and scraps must not be replaced by a second catalog.

The source implementation is split across `transparentnode/WireProcessingMachines.kt`,
`WireProduction.kt`, `WireProductionRecipes.kt` and `item/WireMachineItems.kt`.
Reuse the native `UtilityCables.all`, physical resistance/mass/thermal helpers,
metre-spool APIs, actual inventory filters and native item/fluid/circuit infrastructure.
Keep one shared production-options table rather than separate GUI and server recipes.

The source load is a passive resistor sized at200 V and nominal power. Physical
progress scales with accepted resistor power/nominal power, bounded0–1. Port the
slow physical clock without advancing production during algebraic trials or
turning voltage sag into a free nominal-power supply. Inventory/option changes
must not let a new input inherit a previous job's paid progress.

Inventory layouts are4 slots for roller,3 for insulator and9 for combiner. Outputs
are respectively3,2 and **5**. Combiner inputs are0,1,2,3,4,6,7,8; output 5 is a
preserved historical slot, not the last slot in the enlarged inventory. Source
sided automation inserts into inputs and extracts only output. Adapt those rules
together with actual native transfer capabilities and `MenuSpecs`.

Roller buffering converts accepted ingot count to paid kilograms and tracks its
material. Rubber contributes 32 base insulation-metres each. Maximum target length
is65536 m; insulation layer costs scale by voltage rating. The source carries
`loadedMaterial`, `loadedMassKg` and `insulationMetersBuffer` in its machine item,
while inventory drops separately and partial progress resets. These are paid
resources, not ordinary copied settings: verify real break/drop/place, creative
pick, settings-copy, repeated reads, orphan recovery and cancellation paths before
adopting that payload. Never copy buffers through configuration or mint a second
refund when the same saved settings are read twice.

Wound bundles preserve material, target labels/metric label, conductor count, area
and fractional length. Combiner output consumes every participating conductor for
the shortest actual input length; longer pieces must retain their paid remainder.
Insulation must match existing native descriptors and layer costs. Invalid,
melted, mismatched or unsupported inputs cannot produce a repaired/new default
spool or lose their paid stock.

Required production checks include real loaded/unpowered/sag circuits, output
blocking, native menu/automation paths, material and rubber conservation across
fractional remainders, multi-input combination, source slot5 saves, progress reset,
paid buffer transfer/refund exactly once, and real client publication/model/recipe
paths. Complete source-world conversion, interactive UI/audio/multiplayer and
representative loaded-world comparisons remain separate work.
