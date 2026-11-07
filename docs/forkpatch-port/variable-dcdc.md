# Variable DC/DC converter

The source variable converter is registered at **129**, native name
`variable_dc_dc_converter`. Existing native **128** remains `TransformerDescriptor`
and `dc_dc_converter`; its registry identity, item, settings and save path are unchanged.
The fixed source converter 130 and one-way family are separate remaining adapters.

The new block uses the existing destination `SafeTransformerProcess`, checked
Thevenin probes and conservative accepted-step solver. The input and output have
actual series winding resistors. Constant ratios transfer in either direction;
voltage-target zero and disable open the internal sources. Invalid/truncated
control packets leave accepted settings intact. Missing saved control version
retains historical linear V1 behavior and matched winding lengths. Explicit V2
upgrade selects ratio 1 and remains disabled; new V2 devices allow independent
wire lengths, logarithmic signal mapping and voltage targeting.

Two cable slots, one real ferromagnetic core and optional casing use the native
server menu. Utility wire is measured by its paid fractional metre ledger, while
old electrical cables use stack count. Signal cable, empty stock, melted wire and
stacked metre spools cannot form a winding. Utility winding resistance, thermal
mass and cooling use one selected conductor and cached native wire geometry.
Packed-coil cooling retains the source square-root length correction. Electrical
samples accumulate joules only after accepted steps, and thermal delivery consumes
them once. Algebraic retries cannot multiply heating.

Native temperatures use the fixed 20 C reference. Item metadata stores absolute
Celsius, including accepted heat awaiting a thermal step. Extracting or splitting
hot wire carries its temperature, and reinserting it cannot reset it to ambient.
The new winding thermal load retains the existing `primaryThermalLoadTc` and
`secondaryThermalLoadTc` keys but writes doubles to match item precision; old float
values remain readable. First configuration preserves restored load heat even in
older saves without per-item temperature metadata. Carried/configuration settings
contain no wire, core, casing or stored heat. The settings methods are exposed;
native copy-tool integration remains part of the remaining tool adapters.

Thermal failure queues an owner-checked server task instead of mutating the
inventory during simulation iteration. Jacketed cable becomes the existing damaged
wire item with its paid metres and heat. Bare metal, which has no damaged-jacket
replacement, becomes native wire scrap retaining paid metres, metal mass and heat.
It cannot continue conducting after failure.

The source OBJ/MTL/texture and bounded coil spacing/thickness are retained. Native
publication includes real inventory, settings, voltages, currents and temperatures;
the registered renderer consumes the complete packet, draws cable stubs, animates
the optional casing doors and supplies the transformer hum. The native screen has
control packets and live measurements. Headless mesh/publication checks do not
establish interactive GPU, audio or multiplayer parity.

See [validation](variable-dcdc-validation.json). Full source-world conversion,
remaining converter variants, broader mechanical/thermal integration and historical
1.12 golden reconciliation remain unfinished. No reference snapshots, skips or
numeric tolerances were changed to accept this adapter.
