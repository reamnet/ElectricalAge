# Lamp colour, AWG meters and placement choice

Follow-up to the verified native parity release `e033d0805`.

- `9ded03df1`: fixes ordinary rated bulbs being classified as farming lights merely because they have positive plant-growth values. Decorative fixtures now use their registered bulb technology; real farming bulbs retain their pink light. Work-light packets reuse the same classification. No bulb electrical, lifetime or growth specifications changed.
- AWG cables now participate in native multimeter, thermometer and AllMeter clicks. Readings use the selected conductor's actual endpoint voltage, independent current and absolute temperature, including after reload. Calls without a hit also select AWG cable parts.
- `706fa16bf`: adds a per-spool placement choice for all70 intact single-conductor copper/aluminum variants using the existing tool radial menu.

## Using the placement option

Hold the spool in your main hand and press the tool-menu key (**C** by default; the tooltip shows its current binding). Choose:

- **Native voxel cable**: existing routing, waypoints, previews and exact routed-length payment. This remains the default for old and new spools.
- **Legacy six-node cable**: original per-block cable-element placement, consuming one metre per placed cable. In converted worlds the original element may be hosted inside a voxel block; the legacy placement and physics backend is unchanged.

The choice stays on that spool. Switching modes cancels a pending route and clears its preview without paying or refunding cable. Escape cancels a menu selection. Existing explicit compatibility-route controls remain available in native mode.

## Verification

- 52 focused lamp/AWG units passed for the meter/tint fix; powered meter GameTests7/7 passed.
- Expanded lamp/AWG/radial/preview units59/59 passed. Placement/packet tests verify all70 variants retain exact length/UID/components and survive stack copying, reject malformed/mismatched choices, and cancel stale routes. Actual main-hand Cu/Al legacy placement and return to native routing debit exactly1 m and2 m respectively.
- Expanded focused server78/78 passed, including reload, ownership, rollback, three actual chunk evictions and prior native routing regressions.
- Full `build :test`: exit0,18m13s. Root **1,370 tests / 0 failures / 6 existing skips**; CAD **18 / 0 failures**; fluidcore **104 / 0 failures / 2 existing skips**.
- Full unfiltered GameTests: **2,103/2,106 passed**,16m17s (Gradle exit1 / server exit3 for the same historical `golden_descriptors`, `golden_slots`, `jei_every_item_reachable`). No new failed cases. All **51 AWG cases**, the all70 placement-mode matrix, actual powered meter clicks and **three physical chunk-eviction cycles** pass.
- All **934 existing registry mappings**, descriptor specifications, placed-wire persistence, generated assets, golden snapshots and dependency pins remain unchanged. The only new saved option is the optional per-spool placement boolean, defaulting to native.
- Logs: `/tmp/awg-mode-full-build.log`, `/tmp/awg-mode-full-game.log`, `/tmp/awg-mode-final-unit-build.log`, `/tmp/awg-mode-final-focused-game.log`.

## Fresh artifact and branch

Verified code commits: `9ded03df1` (lamp/meter) and `706fa16bf` (mode menu). Reporting-only commits follow.

JAR: `build/libs/eln-26.3-0.1.0-port.jar`.
SHA-256: `4494585ba54d87fb63e1de0d220889a6ed2bc176d31a8365af28693fe6b711ec`.
Companion checksum: `build/libs/eln-26.3-0.1.0-port.jar.sha256`.
The JAR contains byte-identical verified compiled output for32 relevant classes. This supersedes the earlier e0cc... parity JAR.

Destination: `reamnet/ElectricalAge:local/awg-upstream-integration`; main/develop/feature branches remain unchanged. Verified fixes/menu/report were pushed successfully through f71bd96cb; the final reporting-only checkpoint is pushed to the same branch. The delivery response confirms its final tip.

No live GUI or graphical colour inspection is claimed. Automated checks prove technology selection and actual server meter/placement behavior. The menu reuses RadialMenu; no separate routing, placement or rendering engine was introduced.
