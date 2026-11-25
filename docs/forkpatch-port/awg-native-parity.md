# Native voxel and AWG routing parity

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
