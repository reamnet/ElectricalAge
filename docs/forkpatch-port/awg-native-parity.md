# Native voxel and AWG routing parity

For the current post-upstream architecture and remaining differences, see
[the native adapter follow-up audit](awg-native-parity-followup.md). The sections below
record earlier implementation stages. Actual straight powered chunk eviction/reload
coverage is recorded in `gpt-awg-reliability-progress.md`; shape-specific and visual
validation limits still apply.

The audit starts at `20e69f93c`. Both items already share the native session,
router, preview packet and outline renderer. AWG has a second, older placement
mode that constructs centered routes directly and remains available during
validation of its replacement.

| Capability | Native kit | AWG at audit start | Main difference |
|---|---|---|---|
| Clicks, pivots, undo and expiry | Shared kit session | Shared kit session, plus legacy controls | Legacy compatibility |
| Floors, walls, ceilings and multiple bends | Native pieces, including polylines | Straight electrical edges split at bends | Per-edge accounting and native visual bend fill |
| Inner corners and face changes | Native piece extensions where needed | Same-block coincident endpoints, one marked corner per edge | Explicit electrical endpoint identity |
| Outer wraps and knuckles | Native extensions or a knuckle | Rejected | Zero-length connector lifetime, endpoint ownership and rendering |
| Short gaps | Native straight air allowance, sag mesh | No air allowance and full-block support check | Duplicate restrictions plus AWG persistence and accounting |
| Steps, slabs and host geometry | Native micro-geometry and mimic rollback | Block-face planes and air or voxel cells | Save validation and rollback of changed host blocks |
| Junctions | Arbitrary eligible tap points | Centered floor taps up to three arms | Split heat and paid wire, prevent unintended junctions |
| Machine ports | Native snapping, multiple offsets | Shared snapping, AWG terminal adapter requires a centered edge | Pin identity and masks need end-to-end coverage |
| Preview, placement and refunds | Native outline, placement and kit debit | Shared outline and cached result, AWG translates and revalidates | Separate electrical and material backend is required |

## After the first parity expansion

| Capability | Result | Remaining difference |
|---|---|---|
| Shared controls, waypoints, preview and confirmed route | Existing native session and cached route remain in use | Live client visuals still need an in-game check |
| Straight supported runs, bends and inner face transitions | Existing AWG edges remain in use | A single edge still cannot own a cross-face terminal at both ends |
| Short gaps | Native 20-voxel air limit, support and clearance probes, and sag renderer | AWG currently requires straight flights with supported takeoff and landing on the same face |
| Gap accounting | The debited and recovered length is the routed centerline, consistent with native kits | The outline shows centerline bounds, as on native kits. The installed sag is sampled display geometry |
| Save and rollback | Existing straight saves read as before. Sag geometry has an optional bounded field and client payload | Dedicated chunk unload coverage for the new shapes remains pending |
| Outer wraps, knuckles, along-edge extensions | Rejected | A removable connector must own continuity between explicitly identified neighbors. Native visual extensions cannot safely be turned into positive-length AWG heater/resistor segments or paid again |
| Slabs, stairs and decorative hosts | Rejected where they require micro-planes or replacing a host | Extend the AWG save geometry and verify host restoration on every rollback path before enabling |
| Grounded and wall branches, machine connectors | Existing centered floor taps and centered machine connections remain available | Native off-center and non-floor taps need endpoint-specific split, port and damage ownership tests. Sagged spans also need a split operation that preserves the sampled curve |
| Historical placement | Ordinary offhand six-node and older sneak routing still available | Retire the redundant centered router only after the shared path covers its tested shapes |

## Full-block outer knuckles

AWG uses the native outer-turn search and its emitted full-block knuckle mesh.
A knuckle owns an ideal zero-volt MNA connection between its two explicit
ports. Adjoining paid edges opt into those ports in their saved endpoint masks.
Removing or disconnecting the knuckle releases its connection and references
without removing the arms or charging for decorative geometry. Each arm keeps
its own wire length, resistance, temperature and failure state. The knuckle
stores a descriptor and edge position and owns no recoverable wire.

Floor-to-wall, wall-to-ceiling and wall-to-wall exterior edges have paired
native geometry fixtures. The floor-to-wall power fixture checks opening the
load by removing only the knuckle and keeping a parallel load powered.
Inside-block outer wraps on steps or slabs still need a representation that
maps the native extensions to paid edges and restores any replaced host on
rollback. Client mesh bounds and packet round trips are covered headlessly.

## Partial mounting planes and off-center taps

The shared native pieces can now retain their actual mounting plane on an
imitated slab. Collision and restoration use the native Mimic and Laying
paths. Tests compare the native kit and AWG slab bounds and check rollback,
reload and exact removal refunds.

Supported straight edges on floors, walls and ceilings now use the same
native lattice snap as generic cable taps. The AWG ledger splits by the
actual distance on the edge and distributes stored heat and insulation
exposure. At that stage, sagged runs and explicitly marked corner arms were ineligible
for taps. The former need a split of their sampled mesh, and the latter
need their connector endpoint ownership carried through a split.

Stair climbs and inside-block outer wraps can contain native visual
extensions or stubs whose geometric length differs from the paid
centerline. They remain rejected until the native piece conversion returns
enough source-segment information to associate those shapes with paid
conductors without adding wire or refunds. A partial slab surface alone
does not remove that limitation.

## Taps on supported sagged landings and corner arms

A tap outside a sampled sag interval can now split the straight landing of a
supported part. The split moves complete sample intervals to their respective
child edges and rebases the arc offsets. It retains the original sample
positions, normals and tangents. An aim on the displaced curve remains
ineligible. The native support check and the planar branch geometry are still
required.

Supported straight corner arms can be tapped away from an explicitly marked
connection point. The split assigns each old inner-corner mark or outer-port
bit only to the child that keeps that endpoint. The tap does not create
another corner connector.

### Client and chunk check

1. In a disposable world, use a native kit and comparable AWG spool on two
   identical copies of a floor, slab, stair, and full-block outer corner.
   Right-click a start face, look toward the endpoint, and add a waypoint.
   Capture both the ghost and installed shape. Repeat with a short gap and a
   tap on a supported straight landing or a corner arm. Check that a
   displaced sag target cannot install a planar tee.
2. Record Survival spool length before confirmation, after placement, and
   after shears removal. A rejected stair wrap must leave the spool and
   world unchanged. Compare supported shapes and material accounts after
   reopening the world.
3. Place the connector near a chunk boundary such as X=15/16, with one
   arm across that boundary. Away from spawn and force-loaded chunks,
   teleport at least 1,024 blocks away and let the server unload the area. Return and test power,
   then remove the connector and confirm continuity is lost only through
   that joint. Remove and count the recoverable paid pieces separately.

Headless tests compare mesh data and exercise node save/reload. They do not
verify the live client outline or the actual server chunk lifecycle.
