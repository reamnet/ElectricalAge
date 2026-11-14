# Variable Frequency Motor Drive

The native adapter preserves source lookup **8069 (126:5)** and registry name
`variable_frequency_motor_drive`. It has the original mesh, a reproducible native
inventory icon and the source crafting bill (iron plates, redstone, copper cable,
a redstone block and a machine block). Native menus and publication factories
provide the settings and actual running state.

Left or bottom is supply; right is output. Front bottom is signal inhibit; front
top is the latched thermal-trip signal. Settings are nominal input voltage,
nominal output voltage, 0–60 Hz target, ramp time and nominal current rating.
The source ramp is linear at `60 / rampSeconds` Hz/s; a 30 Hz target with a five
second full-range ramp takes 2.5 seconds to reach.

This is the source's **scalar V/Hz model**, not an AC waveform/phasor solver.
A native power-conserving `ServiceTap` provides the frequency-scaled voltage,
followed by real output impedance `nominalOutput / ratedCurrent`. At nominal
input and 60 Hz the short current is bounded by the configured current rating;
at lower frequency it falls proportionally. Supply sag passes through the tap.
This is not an ideal constant-current clamp: higher input or external backfeed
can exceed that nominal current. Loaded output sags, and winding losses heat the
case. Backfeed remains passive and power-conserving, consistent with the source.

Disable or signal inhibit ramps down rather than immediately opening; the tap
opens below 0.01 Hz. A case above 120 C latches a trip and opens immediately.
Enabling cannot clear the latch; explicit reset is accepted only below 80 C.
The 15 kJ/K case and 12 W/K cooling use the source model. Native accounting runs
once per accepted physical interval, including the last powered interval before
trip. It therefore cannot omit heating when electrical and slow process rates
differ. Steady frequency and resistance retain cached native LU factors.

Saves and carried NBT retain settings, heat, enable state, trip latch and consumed
energy history. Like the source, loading restarts frequency at zero instead of
reapplying a live motor command. Settings-tool transfers cannot cool the case,
clear a fault or copy meter history. No paid inventory or stored-energy item is
created. Appliance cords and full source-world conversion remain separate work.

The [validation report](motor-drive-validation.json) records production circuits,
real placed orientations, controls, hot fault/reload, native menus, geometry and
actual client publication. Interactive GPU/motor/audio and multiplayer checks
are not established by those headless checks.
