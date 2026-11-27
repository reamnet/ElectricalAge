# ForkPatch steam power station

Source reference `b7444a7b8a0c5e072b1fd35eed2b74a282c3348c` in the read-only 1.21.1 checkout. Target baseline `592778111` on `local/awg-upstream-integration`. The target began clean and matched its published branch. The existing distribution verification was still running when the source review started.

## Dependencies

| Component | ForkPatch behavior | 26.3 status and plan |
| --- | --- | --- |
| Steam and water handling | The large turbine draws labeled steam from an internal tank through a fluid capability. Its source process consumes the fluid. | Native boiler, pipes, pumps and condenser already model heat, water, steam, pressure and exhaust. Reuse them. No separate ForkPatch large steam boiler or generator has been found in the source registrations and recipes. |
| Large Steam Turbine | Transparent descriptor 278, three-times model scale, two raised shaft couplings and 27-times capacity. An installed tiered blade controls efficiency and wears with use. | Missing. Adapt native pressure-driven shaft turbine to the large footprint and tiered blades. Keep native exhaust and energy transfer so it can operate in a recirculating plant. |
| Large Generator | Transparent descriptor 280, same large footprint, 108 kW at 3,200 V and 200 rad/s. | Missing. Reuse native shaft generator and its load-dependent torque, terminal and thermal processing. |
| Shaft couplings | Two side positions are ghost shaft ports at the rotor's height. | Ordinary joints and network present. The native ghost manager does not yet expose shaft endpoints. |
| Turbine Blade Set | Item descriptor 7743, structural crafting part. | Native equivalent already supplies turbine repairs at its own existing ID. Reuse it without duplicating the item. |
| Blade tiers | Exchangeable iron, steel, alloy and tungsten blades. | Tiered items are already registered. The native turbine currently uses a separate repair part. |
| Reinforced Shaft and Fixed Shaft | Reinforced Shaft item 7742 depends on Fixed Shaft. | Missing. Source Fixed Shaft descriptor 274 is already occupied by the native LP Steam Turbine. Preserve that mapping. |
| Motor Cowling, Heavy Winding and Copper Thermal Cable | Generator and turbine crafting ingredients. | Present. Source High Current Cable availability is still being checked. |

## Checkpoint

The shaft construction dependency group is verified. Fixed Shaft uses new descriptor 285 and its original model, icon and recipe. It anchors a native shaft at zero and releases it on removal. Initial isolated compilation succeeded, but datagen returned 1 because original Reinforced Shaft descriptor 7742 is already occupied by Radar Goggles. Reinforced Shaft now uses free descriptor 1156 and its original icon. The steel-first recipe uses the fork's steel tags and has load-time fallbacks. Blade Set remains the existing native crafting item with its repair recipe.

The isolated command `./gradlew runClientData runGameTestServer -PelnGameTests=forkpatch_fixed_shaft_locks_and_releases,wp11_shaft_network` returned 0. Datagen changed only the new dependencies and the associated English strings. The anchor locked a spinning flywheel at 0 rad/s, absorbed no drive work while locked, and delivered 1,000 J to the released shaft. The native shaft control case still stored 3,135 J at 100 rad/s.

The baseline at `592778111` completed 2,135 GameTests with four required failures (process exit 4). They were `golden_descriptors`, `golden_slots`, `jei_every_item_reachable` and `awg_voxel_ceiling_to_wall_large_aluminum`. See `/tmp/eln-distribution-full.log`. The local client tools passed the JDK and display checks. There was no configured RCON server in this worktree when tested.

Next implement the original raised shaft couplings, large turbine and generator. Verify both shaft axes and a complete furnace-to-load plant.
