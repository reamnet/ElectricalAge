# Ghost-state import review

This is a source/destination schema review, not an implemented world converter.
The pinned source and native target use different ghost properties and meanings.
Copying numeric metadata would silently turn some clearance cells into collision
walls or battery shapes.

| Meaning | Source `shape` | Current native `meta` |
| --- | ---: | ---: |
| Ordinary cube reservation | 0 | 0, subject to owner-specific shape review |
| Floor plate | 1 | 1 |
| Ladder | 2 | 2 |
| Clearance | 3 | 8 for native clearance collision; selection behavior needs review |
| Lamp clearance | 4 | No dedicated native lamp variant; review the lighting adapter |
| Wall extension, front north | 5 | 5 |
| Wall extension, front south | 6 | 6 |
| Wall extension, front east | 7 | 4 |
| Wall extension, front west | 8 | 3 |
| Native floor battery/cabinet | Owner-dependent source geometry | 7 |
| Pitched solar reservation | Source cube with owner collision/pitch | 9–12, derived from saved uphill direction |

These are observed semantic correspondences, not approved blanket conversion
rules. Source wall AABBs and native cabinet AABBs also differ, even where a number
coincides. Source clearance has empty collision but different outline behavior;
source lamp clearance has its own selection treatment. Validate each owner's
footprint, leads, picking and native geometry before deciding an import mapping.
Existing native metadata 0–8 must retain its current meanings.

Fixed flat solar owners now use native half-height state 13, with scheduled
resident-only repair for their older cube reservations. Pitched states 9–12 remain
separate. Source import still needs owner context to select the correct state;
do not globally reinterpret cube 0 for unrelated machines. See [flat arrays](flat-solar.md).

The source ghost property is named `shape` with range 0–8; the native property is
`meta` with range 0–15. Native solar root states additionally store `roof=0..4`.
A source pitched panel's saved `roofUphill`, front, ghost positions and return
terminal must agree with those native states. The unsaved return node is rebuilt
by its array owner; it is not a standalone saved node to instantiate separately.

The native adapter already tests old flat saves, all four pitched directions,
client-readable ghost collision, actual owner reload and reservation teardown.
Those native round trips do not convert source chunks, saved ghost ownership or
source block-state properties. A complete converter must preserve observer links,
paid hardware/inventories and stable existing native saves, and must distinguish
an incoming source world from a previously native one.

Relevant definitions: source `mods/eln/ghost/GhostBlock.kt`, native
`mods/eln/ghost/GhostBlock.java`, `SolarPanelBlock.java` and `SolarRoofMount.kt`.
Also retain the separate numeric/name conflicts in [registry-conflicts.md](registry-conflicts.md).
