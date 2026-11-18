# Final source lighting adapters

Source: `../ELN1.21.1ForkPatch` at `b7444a7b8a0c5e072b1fd35eed2b74a282c3348c` (read-only). Starting native checkpoint: `21b361c6afd0edea88b114eb4ecd2a485386a404`; fetch found no newer remote commit and the working tree was clean. This continuation finishes the remaining standalone fixture implementations and source bulb lifetime configuration. The earlier 108 decorative styles, 18 bulb identities, step-down unit, rated lamp supplies, pole lights and nine fan variants remain in place.

## Added fixtures

| Native lookup | Source fixture | Adapter |
|---|---|---|
| transparent4352 | 120V Basic Floodlight | Two independent replaceable LED/halogen bulbs, manual swivel/head/cone, all24 perpendicular mount frames, shared optical/model frame, real parallel electrical demand, obstruction and resident-chunk bounded beams |
| transparent4353 | 240V Motorized Floodlight | Same two-bulb optics; three actual signal inputs control swivel/head/cone. Manual packets cannot override signals. The reviewed source does not specify a separate motor wattage. |
| transparent4416 | Christmas Tree | Original tree/star/two blinking strips and clearance reservation;35W;120/240V cord or physical lamp-supply connection |
| transparent4417 | Holiday Candle | Original base/glass/emissive mesh;5W; central partial-support height; paid cord ownership and full-state carrying |
| transparent7936 | Gasoline Construction Work Light | Original model; two independently aimed heads; two matching replaceable bulbs; source2kW/two-bucket gasoline generator or cable/cord/wireless input; fuel/aim/inventory persistence and engine sound |
| transparent7994/7995 | White/Multicolor Fairy Lights on Green Wire | Source procedural wire/lamps, two exact surface anchors,8m maximum span, source sag and cached geometry;15W |
| transparent8006/8007 | Warm White/Multicolor Deck Globe String Lights | Suspended black wire and hanging globes at approximately48cm intervals; same paid two-anchor placement and15W supply |
| six4500/4501 | 120V/240V Emergency Lamp | Source-rated charge power10/25W, capacity6000/12000J, battery lighting5/10W, real charging supply, cord compatibility, paid charge retained on pickup |

`String Lights` at transparent4418 was already implemented and retains its identity and native publication. Emergency lamps use free supplemental IDs4500/4501 because native six4111/4112 are occupied by the established50V/200V variants. Those old identities and ratings remain intact. The18 source bulb lookup mapping remains302–319; no source enum ordinal is used as a native identity.

The work light isolates its generator component **and pins** when stopped: a zero-watt ideal voltage source must not clamp an external supply. Cord/wireless selection stops the engine, and fuel is not used in external mode. Wireless fixture feeds are real passive MNA connections, retired synchronously with their sources; assigning a receiver voltage or reporting fictitious watts is insufficient.

Original OBJ/MTL/model textures and available source inventory icons are copied byte-for-byte, with provenance in `imports.json`. Fairy/deck geometry and work-light GUI items use their source-style procedural/3D renders. Emergency icons use the original emergency-lamp bitmap. The native work-light recipe uses the existing200V Fuel Generator as its paid engine assembly; the resulting fixture regulates to the installed source bulbs. This explicit native ingredient substitution avoids adding an otherwise unrelated generator identity. The two fairy color variants use the source common String Lights bitmap for particles; their GUI items render their actual colored meshes.

## Bulb configuration and wear

`config/eln-synced.toml` now contains `lighting.lamps.<technology>.nominalLifeInHours` and `infiniteLifeEnabled` for incandescent/carbon/fluorescent/farming/led/halogen. Source defaults are24/12/168/48/672/96 real hours. Settings sync through the existing NeoForge synced specification; the pinned Gradle configuration is unchanged.

Source-rated bulbs use the source hours/voltage aging curve: no wear below minimum voltage, linear wear up to nominal, exponentially faster above nominal (10x at125% voltage). Used native bulb condition stays normalized in `life`; `sourceLampHours` retains absolute source hours so increasing configuration cannot renew a used bulb. Lowering configured life caps remaining absolute hours. Explicit native condition writes invalidate the hours cache. Existing50V/200V bulbs retain their old identities, normalized save format and aging branch; their use in the new fixtures respects the existing global aging switch.

Operator command `/eln resetLampLives` resets source-rated bulbs currently installed in active resident sockets (including extended voxel slots), floodlights and work lights. It does not set an unbounded flag that renews future inserted bulbs or offline inventories. This immediate resident-only implementation replaces the old source's short global polling flag. Six-node emergency pickup separately preserves finite clamped charge, preventing repeated placement of the same paid lamp from replenishing its battery.

## Controls and validation

Floodlight menus keep the source176x166 panel, three tracks at(7,8)/(7,26)/(7,44), and bulb slots at(71,62)/(89,62). Work lights retain the176x290 panel, bulb slot(17,60), engine/mode/channel region and separate head aim controls. Festive fixtures use the source176x206 supply/channel controls. Work/flood aim settings expose native configuration-copy hooks; the broader general-purpose configuration-tool workstream remains separate.

Validation results and artifact locations are recorded in `final-lighting-validation.json`. The full run selected1552 native cases:1550 passed, with only the two historical golden checks failing; all1086 ForkPatch cases pass, including all16 new lighting cases. Root704 selected/701passed/3 unchanged optional skips; CAD15/15. Catalog publication649/649, menus304/304 and item draws436/436 pass. Datagen repeats with0 writes/removals (3917 files); all1283 provenance entries pass. All832 prior golden differences remain exact and25 additions account for the new identities/menus; expectations are unchanged. The later client-layout/operator-scene-only fix was compiled and passed all16 targeted native cases again. Tests cover actual source wattage, fuel use/external operation, signal control, world/save/item state, full publication/render consumption, source menu slots, anchor placement consumption, partial support/carry ownership, source retirement and configurable absolute bulb life. They supplement the unchanged existing test suite and goldens.

For a real client, build `/elnlab finallights <x> <y> <z>` on loaded terrain. It creates all nine standalone fixtures with ordinary saved electrical sources, lamp supplies and paid cord claims. `/elnlab devicemenu <lookup>` opens the nearest resident transparent fixture's actual native menu. World generation by this operator-only scene may request chunks; fixture runtime projection and wireless lookup remain resident-only.

## Live client evidence

The real dedicated server built9/9 standalone fixtures at3200,100,3200. All nine source-specific menus opened in an actual joined NeoForge client; screenshots live in `evidence/final-lighting/`. A real basic-floodlight slider change persisted at88degrees after reopening. Native tests cover both work-light heads and all three motorized signal inputs. The corrected work-light menu was captured without the inventory-label overlap; a later manual work-head screenshot attempt stalled, so it is not claimed as verified. Live supply voltage/current records in `final-lighting-live-menus.json` establish real demand for every fixture: approximately30/45/35/5/30/15/15/15/15W, including actual connection losses.

The operator gallery explicitly faces each lamp supply toward its adjacent saved electrical source. The first scene used a wrong supply orientation and was unpowered; it was removed/rebuilt, and the recorded final supply evidence is from the corrected powered scene. Tests exposed and fixed the stopped-generator input clamp and near-nominal light-level truncation. The survival anchor test also needed an explicitly survival-mode test player; no original assertions/tolerances were loosened.

This machine uses OpenGL4.5/Mesa llvmpipe. Reducing the local client view distance to2, GUI scale to2 and frame cap to30 made the nine-menu capture possible; intermittent vanilla terrain-driver stalls remained. Local options were backed up/restored, the added test forceloads were removed, and both managed JVMs were stopped. Desktop window grabs were black and the mctest helper selected RenderPearl's hidden utility window, so evidence uses Minecraft's F2 framebuffer capture on the actual main window. The mctest quick-play shortcut also does not preserve all required NeoForge arguments: a working temporary launcher copied `build/moddev/clientRunProgramArgs.txt` (parsed with `shlex`, comments enabled) and appended the local quick-play address. Project Gradle/wrapper/dependency configuration and test infrastructure were unchanged.

## Acceptance boundaries

Implementation, native solver tests and catalog rendering do not establish universal pixel parity or hardware audio/GPU parity. Matching original assets, source coordinates and mesh geometry is evidence toward visual parity. Human side-by-side source/native views, real hardware sound/GPU checks, simultaneous multiplayer and broad world migration remain release-level acceptance work. General tools, unrelated cord consumers and the rest of the37 whole-fork workstreams are not closed by this lighting slice. Consult `port-completion-plan.md` for those tasks; historical fixture-not-registered notes elsewhere are superseded by this document once final validation is recorded.
