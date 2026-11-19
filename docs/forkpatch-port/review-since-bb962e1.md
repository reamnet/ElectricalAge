# Review of the ForkPatch port since bb962e1

Reviewed starting point: `973bf4626483452f73d8a130cc86a73709d2968d`, fetched from `origin/port/forkpatch-3.0.77-26.3` on 2026-11-16. The remote had no newer commits. The comparison covers all 14 commits after `bb962e1`: 935 changed files, including 20 Java/Kotlin/Python implementation, test and tooling files. Original fan/suspension assets found untracked at session start were preserved and incorporated only with their working adapters.

Source: read-only `../ELN1.21.1ForkPatch` at `b7444a7b8a0c5e072b1fd35eed2b74a282c3348c`. Project Gradle, dependency and wrapper pins are unchanged. Target builds use Java 25; Java 21 is available for the source.

## Findings and corrections

- **Recipe underpayment and wrong prerequisites:** 32 introduced upgrades used a base plus one material instead of the source shaped recipe paying three materials and an iron plate. Other upgrades used the wrong drum, wall-pack or String Lights prerequisite. All 108 fixture recipes now have an independent pinned-source oracle, including exact yields, missing-ingredient rejection, and rejection of those cheaper substitutions. Source String Lights, its paid bootstrap recipe and working 15 W adapter replace the neon-strip substitution.
- **Missing outdoor recipes:** all 20 outdoor recipes exist in the source, including 4495–4499. Earlier claims that the last five were uncraftable were incorrect. Their original dye, glass, holder and base costs are restored.
- **Incomplete visual behavior:** adjustable housings were omitted despite copied OBJ files. Source head transforms, complete housing parts, rotor, suspension, item poses, banker shade and seasonal/party colors are restored. Aiming moves the light using the same pivot and local mounting frame as the mesh. Wide fixtures use the source five-ray pattern.
- **Underwater lighting:** the old lease implementation accepted air only. Native light leases now preserve source water using a waterlogged light state and restore water after overlapping leases expire. They reject other fluids and missing chunks.
- **Unimplemented specials:** the seven missing registrations now have dedicated motion, fan and suspension behavior. Motion uses event 16 so native channel event 4 remains compatible. Fan motors have their own electrical demand, physical startup/coast, native loop sound and the industrial blade hazard. Motors require a physical lead, as in the source. Suspension saves quarter-metre settings from 0–7 m. Special menu heights match the source.
- **Wireless owner safety:** native selection previously accepted supplies from another dimension and historical owners retaining added-node flags. It now requires a resident active owner in the same dimension before selection or billing.
- **Fan lifecycle found during this review:** mode/cord changes now configure motor resistance before registration. Motor pins remain detached for ordinary lamps and wireless-only fan operation, preventing an unregistered component from re-entering the solver during reload. Native regression runs initially exposed this defect; validation is rerun after the correction.
- **Stale tooling and provenance:** completion generation hardcoded 21 fixtures and could not reproduce the later checked-in matrix. It now derives registrations and verifies explicit source ordinals. The import ledger includes all 432 model/icon files for 108 fixtures and the newly adapted controllers and sounds. This verifies provenance, independently of runtime completeness.

## Files reviewed without a new confirmed regression

Client render/screen factories retain descriptor-specific dispatch. Server menu capacity follows the actual installed bulb count and socket type. Legacy lamp resistance/power hooks retain one-bulb defaults. The distinct rated LEDs at 302/303 preserve existing native 50/200 V bulbs at 293/294. The catalog's earlier floor-clearance setup increases valid placement coverage. Newly added wall fixtures receive proper wall support rather than exclusions.

The original OBJ/MTL files and catalog-derived icons are byte-identical to the pinned source where declared verbatim. Generated resources were checked against their recipes and identity registrations; exact provenance is recorded in `imports.json` and `import-audit.json`.

## Verification

Starting checkpoint: root build and CAD tests passed. The unfiltered baseline selected 1,393 native cases: 1,391 passed, zero skips, and all 927 ForkPatch cases passed. Only `golden_descriptors` and `golden_slots` failed, with the existing 697 assertion differences. Historical golden files, tests, tolerances and optional skips have not been relaxed.

Final build, native-game and actual client GUI results are recorded in the accompanying lighting validation report after verification. An incomplete/crashed run is not counted as a pass.

## Remaining boundaries

108 registered fixtures does not establish universal visual or behavioral parity. Portable carry, partial-support mounting, additional source bulb technologies/configuration, wider migration and the remaining workstreams stay tracked in the completion plan. Source-sized controls and identical meshes do not establish pixel parity with every renderer/resource pack. Hardware visual/audio parity and human playtesting require separate evidence.
