# Modular substation progress

## Starting point

Feature branch `feature/modular-substation` in `ElectricalAge-modular-substation` starts at `1470db3bf2ec0bd6124dc100dd55704712900762`. The integration branch was clean and published at that revision. The initial reference is ForkPatch 1.21.1 `b7444a7b8a0c5e072b1fd35eed2b74a282c3348c`. The 26.3 baseline passed 2,137 of 2,140 GameTests and 1,405 root unit tests. The two historical goldens and JEI reachability of the nibble memory chip remain baseline failures.

## Architecture and plan

- Existing fixed 69-to-12.5 kV and 230-to-69 kV equipment stays registered through `SubstationKind`. Its phase buses, safe transformer process, protection latch and screens are references for the configurable product line.
- `GridElement` and `GridLink` own selection, span payment, independent phase channels, saved links and chunk reconnect. `GridRender` already receives the paid wire surface with gauge, jacket and metal. New suspended spans should continue through this path.
- Existing direct-connect line and feed insulators expose a single bus and select service circuit independently. A busbar uses the same single-phase grid contract for insulator spans. The rigid joint requires mutually compatible, face-adjacent terminals. Passive structural pieces have no conductive port.
- Busbar nodes are standard transparent nodes. Native `NodeConnection` connects installed rigid contacts and owns disconnection. Native serial resistance and thermal loads model each section. Geometry follows the connected faces as published by the node.
- Implement the busbar and support family, then extend the terminal adapter to the equipment where geometric and phase alignment allow it. Next implement disconnects with protected manual/remote contacts, configurable transformers on native MNA, and reclosers on a physical-time protection process. Finish with structural recipes, client screenshots and representative powered yard tests.
- Model work uses the native OBJ loader, reproducible geometry and normal descriptor inventory rendering. Register explicit new IDs and use datagen for recipes, models and localization.

## Checkpoint

Initial inspection complete. The isolated feature worktree is active. Implement the first busbar slice and run compilation, datagen and powered adjacency, phase, insulator and reload tests. No new behavior has been verified at this point.

## Rigid busbar milestone

Eight working busbars are registered at IDs 14080–14087. The four profiles use independent copper and aluminum resistance, mass, current ratings and inventory sprites. Face-adjacent bars with matching phases form native simulator contacts. Six independently published arms cover straight, corner, tee, cross and vertical layouts. Phase selection is locked while attached. The tested line/feed insulator uses the established AWG span path.

The powered test delivered 49.9909 A at the far branch and 599.9011 V on the vertical branch. The adjacent unmatched phase stayed deenergized. The same test verified exact spool payment, paid material identity, client arm publication, node reload and removal of a single branch. All four focused server tests passed with exit 0, including the AWG span catalog and three-phase distribution interaction. Two data runs wrote 49 files and then zero. All 959 original manifest records match their baseline values. Logs are `/tmp/eln-modular-busbar-model.log` for the first successful data pass and `/tmp/eln-modular-busbar-fit.log` for the second data pass and passing tests.

Next verify direct feed insulators on a rigid busbar and check the installed models in a live client. The standalone switching, conversion, protection and support equipment remain pending.

## Direct feed milestone

A feed insulator can use a busbar's face as its mounting surface. It latches the bar's explicit phase and uses one native rigid contact. Its outer clamp still uses the normal paid grid span. Line-only insulators retain their existing placement. Legacy installations retain the original unbound circuitry.

In the 69 kV focused fixture, an ordinary tower and the mounted feed insulator rejected a wrong-phase attempt before consumption, accepted the phase B bare span, powered the rigid load at 6.9000 A and left phase A deenergized. Node reload restored one rigid contact and one overhead span. All 11 focused tests passed with Gradle exit 0, including all five existing insulator voltage families, AWG appearance and existing wooden phase bundles. Log `/tmp/eln-modular-insulator.log`. Next capture the client geometry and verify the independent equipment devices.

## Yard steelwork and disconnect milestone

Four new isolators share the native `TransferContact`, paid wire links, automatic gang targeting and explicit single-phase targeting. A loaded opening advances persisted contact wear and warns the operator. SCADA uses its existing loaded-target checks and can operate the same switch without bypassing wear. The new models have moving blades, porcelain stacks and conductor clamps. Six passive steel, ceramic and foundation pieces use matching collision profiles. Busbars publish a mounting stem when they sit on a matching ceramic support. The supports have no electrical load or connection mask.

The gang fixture delivered 79.9840, 99.9800 and 119.9760 A on the three independent circuits before opening. After the loaded opening, all three currents were zero and contact wear was 12.93 percent. Reload retained the open state and wear. The fixture also verified the nonconductive support. The direct feed fixture now uses the ordinary item-use path. All six focused tests passed with exit 0 in `/tmp/eln-modular-switch-retry.log`. The game client captured and visually confirmed a continuous copper elbow at `tools/mctest/shots/modular-substation/busbar-junction.png`. The visible scene is the busbar geometry. Powered insulator mounting was verified through the game tests.

Next add configurable transformer models, commissioning controls and powered step-up, step-down and overload cases. Capture the new switching equipment in the actual client during final yard verification.

## Configurable transformer milestone

Three configurable tank-and-radiator transformers use native scalar MNA taps and separate phase windings. Each physical size limits its permitted voltage tiers, effective ratio and kVA. The menu supports step-up and step-down, one or three windings, taps in 1.25 percent steps, capacity derating, oil-limit selection and protection reset. Windings are isolated scalar phase equivalents with a common simulator reference. Delta and wye wiring requires a phase-angle and explicit return model. The large models show a conservator and an oil-temperature-driven fan.

Six focused GameTests passed (exit 0, `/tmp/eln-modular-transformers.log`). At 230-to-69 kV and 400-to-3.2 kV, outputs matched the selected ratio, tap and series loss. The 230 kV fixture measured 1.739 MW on phase C including winding loss. Invalid settings were rejected, and measured overload opened every winding and survived node reload with all settings intact. Datagen generated the three sets of registration assets and recipes. Live menu and model verification remains ahead.

## Recloser milestone

Three yard reclosers use native phase contacts and the existing physical-time protection controller. Their additional settings allow a definite overcurrent delay, bounded reclose attempts and interval, automatic enable/disable and manual opening, closing and lockout reset. Legacy pole controllers retain their default immediate pickup and their existing saved fields. Remote close honours lockout. Current, voltage, position, attempt count and latest fault are published to the panel and status lamp.

Seven focused cases passed with exit 0 in `/tmp/eln-modular-reclosers.log`. A cleared fault restored service on one attempt. A persistent fault opened every phase and locked after exactly two attempts, retained configuration and lockout across reload, then carried normal current following manual reset after fault clearance. The legacy SCADA lockout regression passed. Next run all six assembled yard cases and verify the models and menus in the live client before the full regression run.

## Assembled yards

The eleven-case dedicated GameTest run passed with exit 0. Six fixtures use actual saved Minecraft grid links and native rigid joints. Three connect the existing utility or transmission pole through a mounted feed insulator, rigid busbar, disconnect, transformer, recloser and outgoing bus. They delivered 400 V from 12.5 kV, 12.5 kV from 69 kV, and 69 kV from 230 kV within 0.02 percent at the test load. The double-bus fixture transferred a common load from 115 V to 240 V with a verified break-before-make interval. The two distribution fault fixtures verified a first-attempt reclose and persistent-fault lockout through the same assembled station, with the transformer's own protection remaining ready. All built nodes passed the harness's save round trip. Log `/tmp/eln-modular-yard.log`.

Menu frames now use native screen-only specs and periodic data publishing. The controls fit in 162 and 179 GUI pixels. Next run the thermal failure and actual bank-click cases, open the completed yard in a client and start full regressions.

## Connection and material stress checks

The native ghost hit forwarded to real equipment clamps on reversed gang switches, created three independent covered-AWG conductors, and charged and refunded the exact three-wire length. A busbar driven beyond its material fusion threshold burns through the native failure lifecycle, opens rigid links and stays open after reload. The test exposed a dummy reconnect into a burnt neighbor. The rigid joint guard now rejects burnt terminals, and the retry measured zero current and zero retained joints. Loaded disconnects also emit the native electric-spark effect at the breaking terminal. Seven focused tests passed with exit 0 in `/tmp/eln-modular-bus-failure-retry.log`.

Datagen wrote 19 files on the recloser pass and zero on the subsequent complete content pass. The registry keeps 959 original records and appends exactly 24 entries, each with a recipe, item model and its own 32×32 icon. The inspected icon sheet is included with the local screenshots. Full regression and live yard verification are next. The integration branch stays at the original baseline.

## Live commissioning and lifecycle checkpoint

Starting verification commit is `00ef43f52`. The 69 kV distribution yard passed in the actual Minecraft client and the dedicated-server fixture. The vanilla harness clears the test structures after reporting, so screenshots of assembled GameTests must be taken while the fixture is running. The persistent gallery supported live model inspection and native transformer and recloser menu checks. A real transformer tap click updated the server-backed menu from step 0 to 1. A direct empty-hand click closed the gang disconnect and the server inspection reported it closed. The menu review identified a doubled percent sign and a prefixed dimensionless winding ratio. Their display corrections and a new powered bay chunk eviction GameTest are pending compilation.

The root build, CAD and fluidcore tests are in `/tmp/eln-modular-build.log`. The unfiltered run in `/tmp/eln-modular-full-retry.log` uses the separate `run/gametest-modular-full` save. An initial parallel launch during resource copying failed to load the mod. The rerun uses stable build outputs. Let the active runs and client exit before compiling the pending files, run the focused modular and compatibility cases, and rerun the full suite against final code.

### Live evidence

Screenshots are kept under `tools/mctest/shots/modular-substation/` in this worktree. `69kv-yard-overview.png` and `69kv-yard-equipment.png` show the live powered 69 kV fixture from the front and beside the switch. `230kv-yard-overview.png` shows the 230 kV fixture. `double-bus-yard.png` was captured during the verified break-before-make transfer. The live client also saved the separate gang blade positions, the medium transformer, the two commissioning menus, the copper elbow and the equipment icon sheet. The gallery is stored in the disposable `run/saves/ELNModularSubstation` client world. The client closed after saving with exit 0.

### Commissioning

Lay one busbar run per phase. Sneak-use a busbar item in the air to select A, B or C before placing it. Adjacent bars of the same phase connect on any face. Ceramic supports carry the bars without conducting. Fit bars beside or above each matching disconnect, transformer and recloser clamp. Use a voltage-appropriate feed insulator on the end bar for each incoming or outgoing suspended line. Use the overhead wire item between the feeder and the exposed insulator. Set the transformer's voltage tiers and winding count before attaching the rigid bars, then commission its tap, capacity and thermal settings. The recloser controls expose pickup, delay, retry settings and reset. Low-voltage split-phase service still uses the existing pole transformer and service equipment downstream of the yard.

The six `modular_yard_*` fixtures in `ModularYardCases.kt` provide the end-to-end examples. They are registered as normal GameTests, so the dedicated runner can select their comma-separated IDs with `-PelnGameTests=...`. An op can also run one in the client with `/test run eln:modular_yard_69kv_distribution`. The lab gallery provides persistent unpowered models for inspecting the newly registered family. Repeated commissioning in real multiplayer and a subjective art review remain open.

### Powered chunk eviction

The focused lane compiled the exact feature source in a disposable local clone to keep build output separate from the active regression JVMs. Early attempts observed the bay leaving full status while its block entity remained resident because the incoming forced chunk's world-generation halo still covered it. Moving the powered bay 190 blocks away put it outside that halo with both endpoints within their native 192-block grid range.

The final dedicated run passed all six tests, including the existing AWG three-cycle eviction and the new `modular_powered_bay_real_chunk_eviction`. Minecraft removed the old transformer block entity and unloaded its chunk with zero retained tickets while the incoming busbar stayed loaded. The 3.2 kV to 230 V transformer at a +1 tap delivered 0.2329 A before, during and after eviction, with the two unused windings isolated. Reload created a fresh block entity, retained the transformer configuration and left exactly two paid grid spans with one thermal conductor each. Breaking the reloaded transformer detached both sides. The run exited 0 in `/tmp/eln-modular-eviction-separated.log`.

Latest changed files are the transformer display, `ModularChunkCase.kt`, the test registry and this checkpoint. Next verify the corrected menu when reopening the saved client world, finish the running regression jobs and assemble the feature JAR.

### Full regression review in progress

The unfiltered run on `00ef43f52` completed 2,157 GameTests in 23 minutes 33 seconds and returned exit 1 from Gradle (server exit 5). The three baseline failure identities remained. JEI reported its same single unreachable item despite the catalog growing from 912 to 936. Two further failures require follow-up. The recipe overlap test found four real clashes caused by the starter busbar ingot patterns, including vanilla copper recipes and ForkPatch's aluminum-tagged Arc Clay storage block. The combined-cycle test met its output, conservation and persistence checks and failed the solver progress assertion after a 0.45-second pause in simulation. The substation code does not modify the fluid solver or gas plant. Next regenerate with distinct busbar recipes, run the recipe overlap and combined-cycle cases, then rerun the unfiltered suite.

The root build exited 0 on `00ef43f52`. JUnit recorded 1,405 passes and six skips. CAD reported 18 passes. Fluidcore reported 102 passes and two skips. The two unmodified subproject test outputs came from Gradle's cache. In the restarted live client, the saved transformer still showed tap +1 with ratio 0.1834 and a single percent sign. The switch retained its saved open position. Screenshots `transformer-restarted.png` and `recloser-restarted.png` accompany the restart evidence.

The small busbars now use a diagonal ingot pattern. Each medium and large busbar uses the matching previous size, which separates the recipes from commodity storage and other copper crafting. The transmission patterns keep their original inputs. Two canonical datagen runs produced six recipe updates and then zero writes across 4,529 files. Fourteen focused cases passed on the canonical worktree with exit 0. The recipe test checked all 2,007 crafting recipes and reported zero clashes. The separate four-case run also passed the gas-plant combined cycle and powered chunk eviction with exit 0. Logs are `/tmp/eln-modular-canonical-data{1,2}.log`, `/tmp/eln-modular-delivery-focused.log` and `/tmp/eln-modular-recipe-and-fluid.log`.

Next run the unfiltered final GameTest suite, record any repeated or baseline failures, review the final manifest and produce the JAR from the resulting feature commit. No integration or Qwen worktree was used for writes.

### Final model review

The final unfiltered service run began on `7574a903e` with 2,158 tests and separate save `run/gametest-modular-final`. CAD and fluidcore tests were also executed again with `--rerun` and returned 0. The live fan check heated the medium transformer to 75 °C using the test lab, and consecutive photos show the blades at distinct angles. That view exposed an air gap between the external bushings and the transformer tank. A model-only patch connects each bushing base to the housing, leaving terminal coordinates intact. It is in focused verification in the separate scratch build. Reopen the client on those classes to inspect the corrected mount before committing.

The seven-case model and compatibility run passed with exit 0 in `/tmp/eln-modular-bushing-focused.log`. The corrected live view `transformer-bushing-mounts.png` shows both outer banks attached to the tank. `transformer-fan-a.png` and `transformer-fan-b.png` show the earlier fan movement. In the rebuilt client, the recloser menu accepted the keyboard-activated Close control. The client changed to Closed and a server inspection agreed. The evidence is `recloser-control-response.png` and `/tmp/eln-modular-bushing-client.log`. The patch adds three lines of rendering geometry to the already tested electrical and data revision. Next record the unfiltered result and assemble the final committed branch in the canonical worktree.

## Verification and delivery

The unfiltered dedicated suite completed 2,158 GameTests on `7574a903e`. There were 2,155 passes, up from 2,137 of 2,140 at the integration baseline. The server exited 3 and Gradle returned 1 for `golden_descriptors`, `golden_slots` and `jei_every_item_reachable`. The two 1.12 comparisons still report expected later catalog entries. JEI still reports only the historical nibble memory chip among 936 items. The recipe overlap and combined-cycle cases both passed in the unfiltered rerun. The powered chunk eviction and every assembled yard also passed. The full log is `/tmp/eln-modular-complete-game.log`.

The independent model-only follow-up at `9f99a4e88` passed all seven focused tests, including the client publication and rendering catalog, powered transformers, chunk eviction and recipe overlap. The headless catalog measured 288 vertices for the small copper busbar, 2,112 for the three-phase recloser, 5,856 for the transmission gang switch and 5,904 for the medium transformer. On this run their mesh evaluations took about 4, 30, 80 and 65 microseconds respectively. The panel and models were also inspected in the real Minecraft client.

Final comparison verified all 959 original manifest records and 24 new identities. Each addition has its own icon, item model and recipe. The historical snapshots are intact. Remaining validation is simultaneous independent multiplayer clients and a full survival playthrough. Configurable transformers use the current native scalar phase equivalents. True delta and wye winding behavior would require a phase-angle and return-path simulation.

The canonical worktree is `/home/matthew/projects/electrical-age-port/ElectricalAge-modular-substation`. The feature commits are local. The production artifact target is `build/libs/eln-26.3-0.1.0-port.jar` with its `.sha256` sidecar. Build using `./gradlew :jar` and verify the embedded commit and checksum before handing over the artifact.

## Corrective branch

The remote integration branch resolved to `1849b2b99` before the corrective branch was created. The starting tree contained the AWG conductor appearance work, the lower-voltage phase-routing changes, and all 24 modular substation identities. The corrective work uses a separate Gradle user home with both configured JDKs. A first cache copy exceeded the size of `/tmp`, so the writable copy moved to `/home/matthew/.cache/eln-substation-corrective-gradle`.

### Causes and implementation

- The lower-voltage pole descriptor forced placement yaw, and its renderer received only the placement facing. ForkPatch fits each circuit family to the connected span bearings. The port now derives separate primary and secondary angles from saved spans, fits corners and terminal poles, and keeps phase order across the primary run. The model, cable endpoints, and transformer leads use the corresponding transforms. Link changes republish the pole and adjacent cable owners.
- The generated alarm and outlet models selected placeholder sprites in the GUI. Seven alarm files contained the same legacy alarm icon. Eleven cord outlet and strip files contained the same generic icon. Their GUI item definitions now render the same meshes and voltage-specific colors as the placed devices. The Type J and Type E six-node socket identities use their source sprites.
- The transparent-node click handler consumed block use before modular placement items could run. Structural placement now reaches the item handler. Horizontal brackets use the clicked wall normal and the model's plate shares that transform with its collision and outline. The steel and foundation models expose physical mounting faces. Insulator caps and busbar stems meet at the block boundary. Supported stems follow any valid mounting direction. A busbar remains available as an independent rigid section when its ceramic support is removed.
- The modular electrical equipment exposed only suspended-grid terminals. Its new rigid contact mapping binds a specific phase and bank to a placeable world cell beside or above the equipment footprint. Side contacts follow the outsides of the banks. Top contacts use separate columns for each bank. Copper and aluminum sections join the native electrical loads through ordinary EA node connections. The client receives the clamp offset used to render the conductive extension. Transformer bank settings enforce the minimum bar voltage at each bank. Current and thermal behavior use the existing device protection and bar resistance model.
- In commissioning, sneak-use a bar stack in the air to select its phase before placing it. This lets players lay adjacent phases without the newly placed item joining the previous run first. Single-winding devices use phase A. Use the existing cable feed insulators for overhead transitions. Equipment bases accept steel frames, gantries, foundations and brackets.
- Existing paid overhead spans on modular equipment load and render as before. To convert an old bay, de-energize it, recover the paid spans by breaking a wired endpoint in survival, then reinstall the endpoint and build rigid runs to the terminals. A retained span may still carry current until it is removed. Older ceramic supports retain their upright models. Older view-oriented brackets can be picked up and mounted against the intended face.

### Live verification

The 26.3 client opened the disposable `run/saves/ELNCorrective` world. The first live placement attempt found a signed-zero comparison that failed to identify the rotated axis while calculating an equipment contact. The contact code now rotates integer block normals. The fixture inventory model test passed all 18 mappings after the first client pass.

Screenshots are under `tools/mctest/shots/corrective-substation/`. `inventory-icons-before-and-after.png` compares all 18 affected fixture thumbnails with the baseline sprites and includes the restored Type J and Type E source sprites. The hotbar columns come from actual client screenshots. `player-built-insulator-and-busbar.png` shows an ordinary right-click build at 653, 4, 1356. The Minecraft server verified the ceramic at y=5 and the copper bar at y=6. `player-built-utility-pole-corner.png` shows two three-conductor primary spans and an independently aligned secondary run of two spans. The poles and conductors were placed and connected through player clicks.

I visually checked the inventory sheet, creative hotbar, JEI screens, pole run, supported busbar, brackets, gallery and distribution bay captures. `player-built-busbar-gallery.png` and `player-built-gantry-extensions.png` show copper and aluminum stock, vertical steel stacks and a horizontal span with multiple mounting positions. The singleplayer server saved the site and the client reopened it with the structures and links intact.

The distribution bay's poles, steelwork, feed insulators, rigid bars, disconnect, transformer, recloser and grounded 2.20 kΩ resistor were installed through player interactions. A network dump identified a missing rigid segment and two spans mounted to the wrong pole. The player fitted the missing segment, removed the crossed spans and rebuilt the intended overhead links. A temporary development command supplied voltage at the first pole using the native simulator. It placed no blocks and created no links. The rated run delivered approximately 400.0 V, 0.1818 A and 72.72 W at the resistor from a 12.5 kV input. Opening either switch through the player's controls brought the load to 0 V. Closing it restored 400 V. The powered overview and measured output are in `distribution-substation-powered.png` and `distribution-rated-load-measurement.png`. The raw network dump is `run/eln-netdump/20261128T082347Z-at-742_4_1379.txt`.

The player-built transmission bays carried phase B through their mounted input feed, disconnect, transformer, recloser and output feeder. The 69 kV bay measured 12.5 kV at its outgoing feed. The 230 kV bay measured 69.0 kV there. These are no-load runs. Both unused windings registered 0 V in each bay. The raw state dumps are `run/eln-netdump/20261128T082747Z-at-775_4_1380.json` and `run/eln-netdump/20261128T082757Z-at-812_4_1380.json`. The 69 kV and 230 kV rated-voltage screenshots show the live inspections. The dedicated fixtures below measure loaded operation separately.

The completed 230 kV yard also ran all three phases together with distinct voltages. Phase A converted 210 kV to 63 kV, B converted 230 kV to 69 kV, and C converted 200 kV to 60 kV. All three outgoing feeders independently measured those values. The simulator reported three separate subsystems, each with exactly two suspended spans at the feeder transitions and native rigid contacts along the equipment chain. The screenshot is `230kv-three-phase-yard.png` and the raw dump is `run/eln-netdump/20261128T083127Z-at-812_4_1380.json`.

The gallery's two equipment rows contain all ten modular switch, transformer and recloser variants. All were fitted on steel frames, with copper and aluminum bars on both terminal banks. The live server reported two electrical joints for every installed variant. Four horizontal brackets placed against the sides of a steel column were also verified in-world. The screenshots are `equipment-gallery-first-row.png`, `equipment-gallery-second-row.png` and `brackets-four-horizontal-faces.png`.

### Automated verification

The 33-case focused regression passed and its log is `/tmp/eln-corrective-focused3.log`. It includes all ten modular equipment variants, independent phase voltage and current, attachment coordinates, native remounting after breakage, paid-wire recovery, and actual eviction of a powered rigid bay. The chunk test measured 0.2329 A before and after reloading. The assembled tier tests measured 399.9992 V, 12,499.0592 V and 68,991.2203 V. The subsequent five-case follow-up checked all three tier fixtures and enforced overhead attachment through mounted feeds. Its log is `/tmp/eln-corrective-feed-focused.log`.

The registry fingerprints matched for all 959 original records and all 24 modular substation additions. Historical golden snapshots were preserved.
