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
