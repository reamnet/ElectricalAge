# ForkPatch steam power station

Source reference `b7444a7b8a0c5e072b1fd35eed2b74a282c3348c` in the read-only 1.21.1 checkout. Target baseline `592778111` on `local/awg-upstream-integration`. The target began clean and matched its published branch. The existing distribution verification was still running when the source review started.

## Dependencies

| Component | ForkPatch behavior | 26.3 status and plan |
| --- | --- | --- |
| Steam and water handling | The large turbine draws labeled steam from an internal tank through a fluid capability. Its source process consumes the fluid. | Native boiler, pipes, pumps and condenser already model heat, water, steam, pressure and exhaust. Reuse them. No separate ForkPatch large steam boiler or generator has been found in the source registrations and recipes. |
| Large Steam Turbine | Transparent descriptor 278, three-times model scale, two raised shaft couplings and 27-times capacity. An installed tiered blade controls efficiency and wears with use. | Ported using the native pressure-driven turbine, large footprint, blade slot and exhaust. |
| Large Generator | Transparent descriptor 280, same large footprint, 108 kW at 3,200 V and 200 rad/s. | Ported using native generator droop, torque and thermal processing. |
| Shaft couplings | Two side positions are ghost shaft ports at the rotor's height. | The large reservations expose owned shaft nodes. Standard joints connect at the raised couplings. |
| Turbine Blade Set | Item descriptor 7743, structural crafting part. | Native equivalent already supplies turbine repairs at its own existing ID. Reuse it without duplicating the item. |
| Blade tiers | Exchangeable iron, steel, alloy and tungsten blades. | Tiered items are already registered. The native turbine currently uses a separate repair part. |
| Reinforced Shaft and Fixed Shaft | Reinforced Shaft item 7742 depends on Fixed Shaft. | Both ported and tested. LP Steam Turbine and Radar Goggles keep the two occupied source IDs. |
| Motor Cowling, Heavy Winding and Copper Thermal Cable | Generator and turbine crafting ingredients. | Present. The source High Current Cable was a hidden current-based cable. The recipe uses the native Copper Cable bulk-current ingredient. |
| Furnace, combustion chamber and boiler | The source turbine obtains steam from another system. | Native fuel and heat transfer. The compatible source has no separate large furnace-to-steam multiblock to import. |
| Alloy pipe, pressure regulation and gauges | No turbine pressure simulation in the source. | Native tiered piping, throttles, pipe pressure sensors, relief valves, pressure regulators and meters. |
| Condensate handling | The source turbine consumes its incoming steam fluid without an exhaust. | Native steam exhaust returns unused work to the fluid. Plant Steam Condenser, shaft motor and pumps already exist on this branch. |

## Checkpoint

The shaft construction dependency group is verified. Fixed Shaft uses new descriptor 285 and its original model, icon and recipe. It anchors a native shaft at zero and releases it on removal. Initial isolated compilation succeeded, but datagen returned 1 because original Reinforced Shaft descriptor 7742 is already occupied by Radar Goggles. Reinforced Shaft now uses free descriptor 1156 and its original icon. The steel-first recipe uses the fork's steel tags and has load-time fallbacks. Blade Set remains the existing native crafting item with its repair recipe.

The isolated command `./gradlew runClientData runGameTestServer -PelnGameTests=forkpatch_fixed_shaft_locks_and_releases,wp11_shaft_network` returned 0. Datagen changed only the new dependencies and the associated English strings. The anchor locked a spinning flywheel at 0 rad/s, absorbed no drive work while locked, and delivered 1,000 J to the released shaft. The native shaft control case still stored 3,135 J at 100 rad/s.

The baseline at `592778111` completed 2,135 GameTests with four required failures (process exit 4). They were `golden_descriptors`, `golden_slots`, `jei_every_item_reachable` and `awg_voxel_ceiling_to_wall_large_aluminum`. See `/tmp/eln-distribution-full.log`. The local client tools passed the JDK and display checks. There was no configured RCON server in this worktree when tested.

Commit `1ea307aca` contains the two construction dependencies. The large machine implementation and three focused cases are in the worktree and are being compiled. Next validate the raised couplings in both orientations and the furnace-to-load plant. Check how a separate exhaust pipe couples to the same native fluid network.

## Large machine adaptation

The turbine and generator keep source transparent IDs 278 and 280. Their three-block-high frames reuse the native ghost reservations with two owned shaft ports one block above the base. The turbine uses the existing HP expansion and wet-steam safeguards with a 200 rad/s rating and the source three-times model scale, 27-times capacity scaling and speed-efficiency curve. The branch's fluid simulation uses mass and pressure instead of the 1.21.1 labeled-steam tank. Steam can therefore pass through the native exhaust, including unused expansion work, and the original mB/s work formula is replaced by actual expansion. The 194,400 mB tank is translated into a steam mass using the native steam unit at the design density. The generator keeps the 3,200 V, 108 kW and 200 rad/s ratings, and limits generated current to 150 percent of rated current through the native generator droop model.

First large-machine validation compiled and generated assets, then exposed missing floor support under the test joints. The next run proved both axes delivered 8.58 kW, and removing a coupling detached the joint and removed the machine. Owner deserialization rejoined the same joint without an extra shaft member. The integrated test's single-furnace setup produced too little shaft work for its target, so the new case uses four fueled furnaces and starts at rest. The registry manifest check preserves all 955 baseline entries and finds exactly four additions. Focused rerun log `/tmp/eln-steam-plant.log`.

Pressure validation found that a wide-open cold turbine can bleed the boiler down toward atmospheric pressure before the station is useful. A later unassisted test ran the furnaces at maximum heat with a cold flow restriction and overheated its boiler. The current full-chain case therefore enables native regulators on its furnaces and turbine and explicitly accounts for the controlled starting spin in its energy balance. This is the expected next run to inspect, along with the true remote chunk eviction test. Logs are in `/tmp/eln-steam-station-validation.log`. Once these pass, run both datagen passes, the unfiltered suite and `build`, then launch the local visual check. Source asset paths for this port are the 1.21.1 `model/turbinesteam` OBJ set, `textures/blocks` machine icons, and the original generator and steam-turbine sound events.

Actual server chunk eviction passed across two chunk columns. The old generator block entity was removed and replaced. The same simulator shaft and its owner supplied 976 W before eviction, 859 W while the chunks were absent and 799 W after reload. Its output decreased as the shaft spent its stored kinetic energy. Removing the machine then removed both owned port nodes and left the joint independent. CPU-side publication drew 1,280 vertices for the empty turbine and 3,792 for the generator with complete packet decoding.

The regulated open-loop test put 392 kJ onto the shaft before exhausting its initial fill. A rated fill of 32,000 game mB is 2.16 kg in the native fluid model. The current test feeds a pressurized water reservoir through a real alloy pipe and the boiler's native float valve. The large turbine also now avoids multiplying the small stage's closed-valve leakage and high-speed parasitic loss by its full 27-times capacity. Full-chain rerun `/tmp/eln-steam-station-fed-retry.log`. An isolated pre-feed build was superseded by the final build listed below.

## Placement and operation

1. Place the large turbine and generator on solid ground, with their raised shaft couplings facing each other. Put a native Joint on a solid pedestal one block above the machines' bases. The parts each reserve a three-block-high footprint.
2. Pipe a Large Boiler's steam outlet to the top center of the turbine. Its exhaust connects one block up on the rear of the casing. A native condenser can take this exhaust. Use the boiler's front feed-water flange to keep its heating surface covered.
3. Supply heat through the Large Boiler's shell faces. Install a turbine blade through the turbine menu. Use a pressure regulator or throttle to let the boiler build pressure. The turbine retains the native overspeed trip and wet-steam failure behavior.
4. Take electrical power from the Large Generator's front or rear floor-level terminal. Use cable insulated for the operating voltage.

## Stable station verification

The six-furnace plant passed with pressurized feedwater, native furnace control and the turbine's regulated inlet at 8 bar. It moved 10.16 kg of steam through the turbine while the float valve maintained 31.9 buckets in the boiler. During the run the furnaces transferred 35.72 MJ of heat, the turbine delivered 2.674 MJ of shaft work, and the electrical load received 985.8 kJ. This exceeds the 897.4 kJ initial shaft energy even before accounting for the energy still in the shaft. The plant ended at 185 rad/s, 3.67 kW at the shaft, and 1.367 kW at the load without a fault. The blade remained installed and wore to 96.98 percent. All 17 saved nodes round-tripped. See `/tmp/eln-steam-station-fed-retry.log` (exit 0).

The focused pressure fixture starts with hot water, an external pressurized **water** supply and 170 rad/s of initial mechanical energy. It never injects steam into the turbine or adds shaft energy after startup.

## Live client verification

Minecraft 26.3 / NeoForge 26.3.0.75-beta started in a disposable clone of the test world. Linux desktop grabs returned black frames. On the host, the Minecraft window was identified by its title and focused so F2 produced actual Minecraft framebuffer captures.

The client ran `/test run eln:forkpatch_large_steam_power_station` in its integrated server. It drew the six active furnaces, Large Boiler, raised joint, Large Steam Turbine and Large Generator on the same platform. The running turbine reported 8.00 bar, 12.7 g/s, 165 rad/s and 3.20 kW of shaft power. The generator reported the same shaft speed. The native menu showed the installed regulator, blade, live pressure and valve opening. A button click changed automatic to manual, and another restored automatic while the station ran. The whole menu was visible at 1280×760 with GUI scale 2. At 854×480 and scale 2, the native-style panel exceeded the frame height. The client had no OpenAL device for audible playback. Two additional machines were placed by ordinary player right-clicks in the clone. A right-click on the turbine's reserved upper cell opened its menu, shift-clicking a Tungsten Turbine Blade installed it, and the slider closed the valve at 0 percent throttle.

Evidence is in `tools/mctest/shots/large-steam/` in the integration worktree. The GameTest fixture's electrical load and reservoir are bench components. Full nameplate output, cold unassisted startup, and a closed condenser loop using the new large turbine have not been established by this test.

## Compact menu follow-up

The full live check exposed clipping at 854×480 with GUI scale 2. The follow-up puts valve status on the same row as the two slots and tightens the vertical spacing. The unchanged layout engine measures 233 GUI pixels high with zero overlaps. A dedicated `LayoutTest` assertion guards the 240-pixel budget and checks both slots remain visible. The full unit build passed the new layout test.

The compact menu was checked in the real 854×480 NeoForge framebuffer at GUI scale 2. After quitting and reopening the saved world, an ordinary right-click on the manual turbine's upper shell reopened the screen. Its Tungsten Turbine Blade remained installed and both throttle and valve readings remained at 0 percent. The title, both controls, both slots and inventory fit in the captured frame. See `tools/mctest/shots/large-steam/compact-854x480-reloaded.png`.

## Release verification

| Check | Result |
| --- | --- |
| Root unit tests | 1,405 passed, 6 skipped, 0 failed across 1,411 tests. The compact menu regression passed. |
| CAD tests | 18 passed. Gradle reused the unchanged test outputs. |
| Fluidcore tests | 102 passed, 2 skipped, 0 failed across 104 tests. Gradle reused the unchanged test outputs. |
| Unfiltered GameTests | 2,137 of 2,140 passed on both full runs. The Minecraft server returned 3 and Gradle returned 1 for the same three baseline failures. |
| Final datagen | Both `runClientData` calls returned 0 and wrote zero files. All 4,385 file hashes matched. |
| Registry and recipes | All 955 preexisting mappings were unchanged, including the 100 AWG records. JEI found a survival recipe path for all four new items. The sole unreachable item was the existing `nibble_memory_chip`. |
| Production JAR | `./gradlew :jar` and `sha256sum -c` returned 0. |

The baseline at `592778111` passed 2,131 of 2,135 GameTests. The three failures that remained in the two final runs were `golden_descriptors`, `golden_slots` and `jei_every_item_reachable`. The baseline also failed `awg_voxel_ceiling_to_wall_large_aluminum`, which passed both steam-port runs. The steam port does not change AWG geometry or historical golden snapshots.

The implementation commits are `1ea307aca` for the shaft components, `d74d1eca7` for the large machines and `0a29ee4a7` for the compact menu. They were pushed to `origin/local/awg-upstream-integration`. Final logs are `/tmp/eln-steam-final-full.log`, `/tmp/eln-steam-final-units-build.log` and `/tmp/eln-steam-final-datagen-{1,2}.log`. The local client stopped through the ordinary save path, and its temporary options and resource pack were restored.

Production JAR `/home/matthew/projects/electrical-age-port/ElectricalAge-awg-upstream-integration/build/libs/eln-26.3-0.1.0-port.jar`

SHA-256 `a1dbb9f5ab225469ebf862841f66bb226f935d7ab7114151872cba75ba0f55ce`
