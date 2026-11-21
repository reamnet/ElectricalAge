# Upstream develop integration

ForkPatch parent `679ad0248` and upstream develop parent `29b2d2200`.

## Registry and saved worlds

- The ForkPatch variable converter retains descriptor 129 and the registry path
  `eln:variable_dc_dc_converter`. Upstream's signal-tapped winding converter
  has descriptor 135 and path `eln:signal_tapped_dc_dc_converter`, with its own
  recipe. When a node tagged with the old ID contains upstream's `tapTurns`
  field, the node loader selects the signal-tapped descriptor. Items from an
  upstream-only save that share the old registry path remain ambiguous.
- Upstream's common tiers are 48 V, 230 V, 400 V and 11 kV, with 5 V signal
  full scale. ForkPatch-specific service and appliance ratings remain on their
  explicit descriptors. Upstream rewinding of matching legacy converter ratios
  is retained alongside ForkPatch's protected isolated solve. Old circuits can
  change operating point at the new tier ratings.
- ForkPatch's multi-conductor poles retain their span format, paid cable,
  terminal routing and thermal state. Bare upstream overhead cable uses the
  same route validation. The span publication includes both the conductor count
  and upstream's stasis halo flag, requiring matching client and server builds.
- ForkPatch cord connections use the upstream Machine API's actual power load.
  Generator work lights use the upstream fuel vessel and accept old tank tags.
  Breakers retain ForkPatch branch-current limits in upstream's trip controls.

## Validation

The recorded ForkPatch baseline includes failures in `golden_descriptors` and
`golden_slots`. See [native suite review](native-suite-review.md). Golden
snapshots remain available for comparison.

- The registry table parses without duplicate keys. All 904 ForkPatch entries
  retain their original registry paths. The integrated table contains 922
  entries.
- Java and Kotlin compilation passed in the initial integrated validation run.
- The full root `:test` run on the initial integrated build completed
  1,245 tests with 1,240 passes and five skips. The same Gradle invocation
  returned an error for its separate, first datagen attempt. A targeted root
  run on the final sources passed all 28 selected tests across converter
  safety, high-voltage references, cords and the Machine API host.
- `:cad:test` passed all 18 tests. `:fluidcore:test` ran 102 tests with
  100 passes and two skips.
- The first datagen run reached mod initialization, then found two ForkPatch
  recipes still using the old `200V Fuel Generator` display name. Both recipe
  sources now use the registered `230V Fuel Generator` descriptor.
- The staged whitespace check reports inherited upstream formatting in FatFs
  and three cable test files. The files resolved during the merge pass that check.
- The recipe-name fix allowed `runClientData` to finish. It wrote the
  signal-tapped converter's blockstate, item models, recipe, advancement and
  translations. The converter uses upstream's icon at its new registry path.
- The initial integrated GameTest server completed 1,923 cases in 22.66
  minutes with 131 required failures, including the two recorded baseline
  golden failure classes. This was the diagnostic run before the follow-up
  fixes. Its other failures included legacy pole ratios, GUI fixtures and
  bounds, cord voltage changes, render fixtures, cable routing and topology,
  toolbags, and recipe coverage.
- The legacy pole's automatically fitted transformer keeps its original
  4:1 ratio until replaced. A 200 V workshop cord can still feed a stock
  device upgraded to 230 V, at the outlet's actual voltage. The 48 V tier
  now matches the existing 48 V cord. Existing keyed 800 V and 3.2 kV
  connectors still represent those actual voltages, so a retiered stock
  device can refuse its old cord. This affects stock 800 V consumers that
  upstream moved to 400 V, including ghost-fed devices.
- A repeat datagen run checked 4,243 files and wrote zero. It reported 19
  existing missing flat textures. The signal-tapped icon resolved.
- The first focused GameTest rerun completed 52 cases with 29 passes and
  23 failures. Fourteen failures still required an 800 V connector on
  a retiered stock device. Eight involved the recessed wall-port oracle,
  and one involved the signal-tapped converter at open circuit.
- The next run passed ten of eleven cases, including all eight recessed outlet
  cases and the hitbox survey. The voxel verifier now compares live links
  against stored joints plus the explicit diagonal wall-port rules.
- After adding a 1 GΩ output reference to the signal-tapped converter,
  a five-case run passed. It verified 48 V and 96 V at half and full signal,
  the existing loaded and unloaded isolated converters, and the hitbox survey.
- The consolidated 55-case rerun passed 41. The 14 remaining cases all
  required an old 800 V cord to attach to stock devices now rated 400 V.
- A further 41-case rerun of the late cable, bus and GUI failures passed
  31. Work-light, festive-string-light and outlet channel packets now use
  the shared 64-character network limit. Eight GUI fixtures still fail
  during placement, before exercising their packet handlers. Two routing
  cases still report that the planner gave up on the gap or air run.
- A final 41-case rerun passed 37, including the remaining toolbag,
  hosted-device and voxel topology cases. Its four failures were
  `golden_descriptors`, `golden_slots`, `jei_every_item_reachable` and
  `recipes_no_two_alike`. The unfiltered 1,923-case result is from the
  earlier build. Later runs rechecked the failing areas in groups.
- The full run found four pairs of identical recipe inputs and patterns
  with different outputs. File comparison confirmed those inputs and
  patterns are already present in the ForkPatch parent. The pairs are
  secondary utility pole and three-phase utility pole, 400 A battery
  disconnect and AC LiFePO4 charger, single-phase fused cutout and
  transformer poles, and single-pole contactor and two-pole changeover
  relay. The JEI reachability test reported 192 unreachable items of 874
  listed on that run.

## Open compatibility review

The high-power transmitter antenna, defence turret, Auto Miner and
Experimental Transporter are among the stock devices whose 800 V cord
fixtures now fail against their upstream 400 V power ratings. Existing
800 V cables, outlets and cord IDs are still present. A 400 V device needs
a correctly rated supply and an appropriate connection before it can be
powered in the integrated build. A mixed-version world with an installed
upstream signal-tapped converter still has a colliding block and item
registry path to review alongside the node-tag migration.


## Local logs

- [Initial unfiltered GameTests](/tmp/eln-merge-gametest.log)
- [Consolidated cord, rendering and converter GameTests](/tmp/eln-merge-final-gametest.log)
- [Cable, bus and GUI GameTests](/tmp/eln-merge-late-gametest.log)
- [Remaining GameTests and recipe audits](/tmp/eln-merge-remaining-gametest.log)
- [Root and subproject unit run](/tmp/eln-merge-validation.log)
- [Final-source targeted unit run](/tmp/eln-merge-unit-focused.log)
- [Repeat data generation](/tmp/eln-merge-final-datagen.log)
