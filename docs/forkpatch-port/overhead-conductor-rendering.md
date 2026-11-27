# Covered and insulated overhead conductors

## Baseline

- Target `local/awg-upstream-integration` at `f436e4ba3ec9be9ff710d42f56e526a3a72e5122` was clean and published. Reference is ForkPatch 1.21.1 at `b7444a7b8a0c5e072b1fd35eed2b74a282c3348c`.
- `GridLink` saves the paid `ItemStack`; `GridElement` reads the actual cable descriptor from that stack on publication. The descriptor and electrical data survive. `GridCableAppearance` treated bare AWG as metal but assigned every intact jacket the attachment's legacy `textures/wire.png`, with no conductor color at the ends and no overhead-specific diameter. Both `LineInsulatorRender` and utility `TransmissionRender` use `GridRender.drawCables()`.
- Source `OverheadCableAppearance`, `OverheadSpanPublication` and `GridRender` use solid colors. The conductor half-width is `sqrt(areaMm² / π) × 0.006`, the covering adds `18% + 0.002` blocks, and jacket, copper and aluminum use `#222629`, `#D96B2E` and `#C7CCD6`. Source uses the same jacket for different voltage ratings, with short exposed metal at terminations. The separate triplex rack, twist and splice placement builds on adaptive anchor routing that is outside the shared material fallback.
- Prior full server baseline was 2,137 / 2,140 GameTests. Historical failures were `golden_descriptors`, `golden_slots` and JEI reachability for `nibble_memory_chip`.

## Checkpoint

Capture the original appearance on a disposable 26.3 client world, correct shared publication and mesh styling, run focused packet/mesh and placement tests, capture the same spans after reopening the save, then run the full suites and package the release. The 1.21.1 checkout supplies the visual formula and colors.

## Implementation and focused coverage

- Every current grid attachment publishes through `GridElement` and uses the shared `GridRender` mesh. GRD2 packets carry a core radius, metal color and explicit surface. GRD1 and unversioned packets retain their original rendering. The paid ItemStack, electrical channels and save format supply the same data as before.
- Use the source square half-width and a dark solid surface for intact AWG coverings and insulation. Both ends have a short exposed core connected to the jacket by a closed shoulder, without overlapping faces. The centerline uses the existing 26.3 sag sampling, exact attachment points, phase spacing and direction. Native, non-AWG wires keep the original surface, including the T1 equipment texture.
- Updated GameTests emit the geometry for all 70 AWG descriptors, check each jacket and core color/width and count shared triangle edges. Added assertions cover the ten line/feed air mounts, service bundles, all transmission pole selection cases, wooden single/three-phase and secondary attachment, and an insulated pole-to-insulator service link. Existing powered and material-ledger assertions remain.
- Live baseline reproduced both gray-textured 12 m spans by right-clicking placeable insulators in Minecraft 26.3. Copper 2 AWG 20 kV is in front and aluminum 4/0 AWG 1 kV is behind. The baseline screenshots and matched camera commands are retained in the disposable test setup.
- Initial one-case server test passed. The broader focused run uses every explicit case ID in the overhead, insulator, transmission selection, distribution and underground families. Full verification and live after captures remain pending.

## Attachment matrix

| Attachment | Path and checks |
| --- | --- |
| Standalone line/feed insulators | All five voltages and both mounting types use one paid conductor. Tested jacket publication at each voltage, both metal types, support removal, powered bare runs and node reload. Live spans used 12.5 kV and 69 kV line insulators. |
| Wooden primary distribution | One phase, three phases and protected-phase terminals share the same grid draw path. Existing equipment checks exercise transformers, regulators, cutouts, reclosers, fused taps, streetlights and overhead risers. Live three-phase copper used 20 kV covering. |
| Secondary utility poles, transformer and streetlight racks, secondary-fed poles | Intact insulated service cable uses three independent published wires on the rack or one wire on a selected terminal. Tests exercise copper 14 AWG and aluminum 2 AWG, both placement directions and exact core-length refunds. Live secondary used aluminum 4/0 AWG, 1 kV jacket. |
| Mixed insulator and pole | The 800 V insulator's selected L1 terminal and the wooden pole L1 rack share one insulated AWG span. |
| Service masts and weatherheads | Shared renderer for the four overhead mast styles. A paid Triplex 1/0 service bundle exercises the real publication, saved heat and jacket-length ledger. Meter bases, wall breaker panels, ground cabinets and handholes use their existing wall/underground entry points. |
| Transmission | Both 69 kV and 230 kV pole and riser variants retain their native terminal logic. The `grid_span_awg_material_and_thickness` fixture renders the complete 70-descriptor AWG catalog on a 69 kV pole. Native bare spans and original T1/DCDC equipment remain controls. |

## Live Minecraft 26.3

In a disposable world, connected a 12 m copper 2 AWG 20 kV span and an aluminum 4/0 AWG 1 kV span by right-clicking the insulators with the matching spools. Closed Minecraft normally, reopened the same save after the fix, and captured the same poses. Both jackets became continuous dark surfaces with visibly different widths, copper at the front clamp and aluminum at the rear clamp. A separate pair of wooden poles received three covered copper primary runs and three insulated aluminum secondary runs through normal upper and rack clicks; all six rendered on the same poles.

Travelled about 500 blocks from the insulator fixture. The integrated server confirmed `execute unless loaded` for the owner chunk, then `execute if loaded` upon return. Both spans republished with their updated surfaces. Screenshots are in `tools/mctest/shots/overhead-conductors/`, with `before-spans.png`, `after-spans.png`, `before-terminals.png`, `after-terminals.png`, `aluminum-terminal.png`, `after-chunk-reload.png` and `primary-and-secondary.png`. The Minecraft client stopped and saved normally. mctest doctor found no standalone RCON configuration, so these captures used Minecraft F2 and the integrated server. Packet replay covers client synchronization; independent multiplayer clients were not used. Overhead placement currently gives a starting-point message and a targeted attachment outline. The voxel route preview is a different path.

No registry IDs, conductor specifications, recipes, assets or saved data schemas changed. The source's adaptive triplex anchor relocation, twist and splice geometry are separate from this appearance fix. Its per-wire covering and core dimensions now render on the existing 26.3 rack and conductor arrangement.

## Regression checkpoint

The two appearance unit tests and all 84 focused GameTests, including the harness test, passed with Gradle exit 0. Logs are `/tmp/eln-overhead-focused.log` and `/tmp/eln-overhead-matrix-retry.log`. The first 83-case matrix exposed a missing stone support in the new secondary-pole fixture. The fixture now explicitly places that support and its mixed span and exact material refund passed. The retry includes every case from that matrix.

Run the complete build, root, CAD and fluidcore tests in `/tmp/eln-overhead-build.log`, and the unfiltered GameTest server in `/tmp/eln-overhead-full.log`. Compare its failures with the baseline before assembling a clean-commit production JAR and verifying the GitHub branch.
