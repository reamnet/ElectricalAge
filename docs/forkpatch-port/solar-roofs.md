# Solar roofs

The four existing panels (3073–3076) accept bottom-half vanilla stairs. The stair
facing defines the uphill direction and the panel uses the source's 45° roof
pitch around its mounting foot. Flat surfaces and top-half stairs keep the
existing flat geometry. Rotating panels retain their separate manual/tracker
adjustment; the tracker projects the sun onto the pitched panel's rotation axis.

A large array still reserves five cells, with its three uphill cells one block
higher. Existing stairs stay in place. The return terminal moves into an uphill
reservation instead of sharing a roof support block. An occupied uphill cell
rejects placement before any reservations or nodes are created. Breaking a
reservation removes the owner, the remaining reservations and the return node.

Roof direction is saved as `roofUphill` and published with the existing panel
state. Old saves without that field remain flat. Restore reads the saved mount
without sampling terrain; passive support/entity probes only inspect resident
chunks. Invalid saved directions become flat and nonfinite saved/manual angles
cannot enter the solar calculations. Panel settings carried in an item do not
carry the roof's orientation to a different mounting surface.

Native collision and outline shapes use eight thin strips, matching the source
collision approximation. Four cached shapes are shared by all panels and ghost
blocks. The root's `roof=0..4` block state and unused native ghost metadata 9–12
replicate the shape to clients without an observer lookup or block-entity query.
Existing metadata 0–8 and every registry identity retain their prior meanings.
Model drawing pitches the source mesh about its mounting foot after drawing the
floor cable. The geometric render tests check that the pitched vertices remain
within the native culling margin; interactive GPU, selection and multiplayer
presentation still require a client pass.

`ForkPatchSolarRoofTest` checks source geometry, footprint/return placement,
pitched incidence, tracker projection and invalid directions. The 18 native
`forkpatch_solar_roof_*` cases cover every panel type and horizontal roof direction,
actual item placement, preserved stairs, replicated root/ghost collisions, real
loaded I–V behavior, tracker/manual controls, actual node reloads, real client
publication/drawing, obstruction atomicity, unloaded probes and array teardown.
Unsaved return nodes are reconstructed by their array owner during reload. The
fixtures reload that owner and remove the array before the generic test harness
attempts its separate per-node persistence pass.

The daylight sampling point follows the source front/pitch transform. Native local
weather remains graded. Shadow tracing uses resident chunks, native 0–15 opacity,
bounded voxel traversal and conservative world-surface heightmaps; see the
[solar sampling guide](solar-sampling.md). This adapter does not convert a source
world or retrofit old flat arrays onto slopes merely because stairs later appear
below them.
