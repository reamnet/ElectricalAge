# Variable inductor

Source identity `96:35` (**6179**) is registered as `eln:variable_inductor`. The
existing fixed Power Inductor stays at 6178 with its native model, slots and saves.
The new adapter reuses that destination's shortened winding mesh, voxel placement
and LU-based inductor implementation.

A one-item ferromagnetic core slot enables the normal terminal resistance. Without
a valid core, both terminals use high impedance. A signal input is separate from
the left/right power terminals. Its normalized value selects the nearest of the
source's 19 E6 levels, from **0.1 H to 100 H**. An unconnected input selects 100 H;
finite out-of-range signals clamp to the range. Nonfinite input also falls back to
the unconnected setting rather than entering rounding or solver arithmetic.

Stable quantized input does not dirty the native matrix. The native tests repeat
1,000 updates and check that the LU stamp remains valid. Changing to another E6
level changes the inductor at the normal physical control boundary. The source's
simplified control semantics are retained: this is not a mechanical variable-core
model and does not simulate the work or voltage spike of field collapse.

Current remains in the native inductor's save field. `variableInductance` also
retains the active level across restore, before the signal connections rebuild.
Core insertion/removal follows normal server inventory notifications; removing it
resets the field, as in the source. The one-slot native menu, actual winding render,
live control/inductance display, item model, borrowed native fixed-inductor icon
and source crafting pattern are present. The recipe uses three Copper Cables, one
Signal Cable and one Iron Ingot. GPU interaction and multiplayer presentation
remain part of the broader client validation backlog.

Four root regressions cover every level, rounding boundaries, nonfinite/range
handling, monotonicity and the native backward-Euler RL transient. Eight GameTests
cover all six mounting faces with real signal/power circuits, stable LU factors,
publication/rendering, floating/invalid controls, real core-slot insertion, safe
invalid-core handling, owner reload, saved current and core removal. The existing
catalog checks additionally exercise the new menu, renderer and drawn item.
