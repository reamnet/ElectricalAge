# Remaining item inventory — 2026-11-17

Source baseline: `b7444a7b8a0c5e072b1fd35eed2b74a282c3348c`. Target checkpoint: `86ab755f`.

This is a static inventory audit, **not a certified exhaustive runtime missing-item count**. It compares all 930 source generated item models with 895 target generated item entries and the target descriptor manifest, then separates known renames, source-rated variants, and integration/hidden items. Matching names do not prove matching behavior; generated source models can also be stale. The complete 177-row difference inventory is below and in `unported-items-audit.json`. Rows marked candidates require descriptor/registration acceptance before being treated as confirmed omissions.

## Main remaining groups

- Climate: Portable Electric Space Heater; Modern, Farmhouse and Corner Fireplace TV Stands; Window Air Conditioner; Wall-mounted Mini-split Air Conditioner; Electric Patio Heater.
- Machines: 480 V Industrial Fluid Pump; Climate Controlled Growing Bed; Thermal Heat Exchanger; missing shaft/large-machine families listed below.
- Mechanics: Clutch, Fixed Shaft, Radial Motor, Crank Shaft, Reinforced Shaft, Rolling Shaft Machine, Large Flywheel, Large Generator, Large Shaft Motor, Large Gas Turbine, Large Steam Turbine, Polarized Shaft Generator and Motor; clutch machine + pin + gold/lead/coal clutch plates (iron + copper clutch plates ported 2026-11-18), Heavy Winding, Motor Cowling and four material-specific turbine blades (iron + steel turbine blades ported 2026-11-18; alloy + tungsten remain).
- Controls/instruments: Current Source, Digital Display, Nixie Tube, Multicolor LED Vumeter, Humidity Sensor, Thermometer Sensor, MQTT Energy Meter and MQTT Signal Controller; extra wireless controls, current-rated switches/fuses and regulator chips.
- Tools/materials: Config Copy Tool, Falstad Import Tool, Turbo and Irresponsible Electrical Drills, Electric Minecart, the graphite rod 4x compressor recipe (1 Synthetic Diamond @ 80000 J — blocked, Synthetic Diamond unported), arc materials/canisters, additional dusts and Synthetic Diamond.
- Utility: standalone line/feed insulators, Ceiling Color Segregated Cable Tray, older Direct Utility Pole/Transmission Tower/Grid Switch and railroad Overhead Lines. New 69/230 kV steel poles are already ported and are different items.
- Source-specific ratings: chargers, heating elements, dissipators, egg incubator, defence turret, industrial processing machines, sockets and shaft motor. Existing legacy machines do not establish source 48/120/240/480 V parity.

## Additional source names outside the generated-model difference

- Optional integration: Create Shaft Adapter; Industrial Create Shaft Adapter.
- Optional media addon: Jukebox Standing Speaker; Jukebox Ceiling Speaker (the three media devices also appear below). Source without its media provider uses unavailable placeholders.
- Development only: Conduit; Isolation Transformer. These are not normal source survival omissions.
- Excluded commented/uninvoked source content: T1 Transmission Cable, T2 Transmission Cable, Under Track Power and Large Wind Turbine are commented registrations; StreetLightWall has a registration method but no invocation was found. Do not count these as active source items merely because their names occur in code.

## Already registered, with separate incomplete behavior

- Utility cables: all 170 Al/Cu identities and audited numeric tables are present. Source room-temperature coupling, insulation-failure player shock and post-fault insulated cooling remain incomplete; see `cable-source-parity-audit.md`.
- Source shaft-machine behavior/rating changes still need reconciliation even for same-name inherited Generator, Steam/Gas Turbine, joints and flywheel items.
- Additional cord/carry consumers, partial-block mounting, migration, GUI/visual parity and broader gameplay acceptance are behavior/test work, not necessarily absent item registrations.

All 108 decorative lighting styles, all 18 source bulb variants, nine fans, the final standalone lighting batch, kitchen/laundry/fusion batch, substations 8016–8019, 12-circuit panel, fire alarms and the implemented road-vehicle/charging families are already ported. This does not mean every interaction has received hardware or multi-client playtesting. See the current completion notes for validation limits.

## Full generated-model difference inventory

Each row retains the exact source model identifier so names can be traced without ambiguity. “Candidate” is deliberately not a claim of verified runtime absence.

### Creative/hidden source item (10)

| Source item/model | Finding |
| --- | --- |
| `creative_cable` | Separate from ordinary survival inventory; native equivalent not established. |
| `creative_power_capacitor` | Separate from ordinary survival inventory; native equivalent not established. |
| `creative_power_inductor` | Separate from ordinary survival inventory; native equivalent not established. |
| `creative_power_resistor` | Separate from ordinary survival inventory; native equivalent not established. |
| `high_current_relay` | Separate from ordinary survival inventory; native equivalent not established. |
| `low_current_relay` | Separate from ordinary survival inventory; native equivalent not established. |
| `medium_current_relay` | Separate from ordinary survival inventory; native equivalent not established. |
| `portable_nan` | Separate from ordinary survival inventory; native equivalent not established. |
| `the_nope_wand` | Separate from ordinary survival inventory; native equivalent not established. |
| `unreleasedium` | Separate from ordinary survival inventory; native equivalent not established. |

### Mapped/present — exclude from missing list (16)

| Source item/model | Finding |
| --- | --- |
| `allmeter` | Multimeter |
| `analog_vumeter` | Analog Gauge |
| `classic_lamp_socket` | Lamp Socket A |
| `ghostblock` | Internal native ghost block |
| `legacy_dc-dc_converter` | Native converter 128 is retained; source converter 130 maps to Winding DC-DC Converter |
| `lightblock` | Internal native light block |
| `long_suspended_lamp_socket__no_swing_` | Long Suspended Lamp Socket |
| `on_off_regulator_10_percent` | On/off regulator 10% |
| `on_off_regulator_1_percent` | On/off regulator 1% |
| `portable_condensator` | Portable Capacitor |
| `portable_condensator_pack` | Portable Capacitor Pack |
| `power_capacitor_84` | Native transparent descriptor 84 |
| `power_inductor_80` | Native transparent descriptor 80 |
| `spot_lamp_socket` | Lamp Socket B Projector |
| `suspended_lamp_socket__no_swing_` | Suspended Lamp Socket |
| `wireless_analyser` | Wireless Analyzer |

### Missing source item / distinct variant candidate (100)

| Source item/model | Finding |
| --- | --- |
| `100a_switch` | No matching current item name/model found; registration/recipe parity still needs acceptance testing. |
| `10a_lead_fuse` | **Ported 2026-11-18** (ForkPatch 1.21.1): `eln:shared_item@6289` (subId 17, group 98 `sharedItem`); reuses the cable-fuse `ElectricalFuseDescriptor` with a new `currentLimit` parameter (10.0 A, `cableDescriptor` null) — heat R `getSmallRs()×2`, in-circuit R `getSmallRs()×1`; warm limit `Eln.cableWarmLimit` (130 °C), trips at 0.9× warm limit (WP16b factor 0.9 preserved over source 0.8); blown state reuses existing `BlownFuse` (no new blown item). Craft recipe `rcr` ×4 (rubber + Copper 16 AWG Cable 300V) exact vs source `CraftingRecipes.kt:2781-2782`; shared `electricalfuse` texture (byte-identical, md5 `199a8e8b…`, no new file). `:test` 704/704, `runClientData` idempotent (0 lines 2nd-run diff), GameTest 1552 (same 2 historical golden fails), live `summon` oracle resolves to "10A Lead Fuse" (5A regression + bogus-ID control → Air). See OVERNIGHT_PORT_PROGRESS.md 2026-11-18 10A entry. 15A/20A/30A/50A out of scope. |
| `12.5_kv_cable_feed_insulator` | No matching current item name/model found; registration/recipe parity still needs acceptance testing. |
| `12.5_kv_line_insulator` | No matching current item name/model found; registration/recipe parity still needs acceptance testing. |
| `12v_ldo_regulator_chip` | No matching current item name/model found; registration/recipe parity still needs acceptance testing. |
| `15a_lead_fuse` | No matching current item name/model found; registration/recipe parity still needs acceptance testing. |
| `20a_lead_fuse` | No matching current item name/model found; registration/recipe parity still needs acceptance testing. |
| `20a_switch` | **Ported 2026-11-18** (ForkPatch 1.21.1): `eln:six_node@6214` (subId 6, group 97 `sixNodeItem`), item `eln:switch_20a`; reuses the existing 5A-switch `ElectricalSwitchDescriptor` pattern — cable `stdCableRender200V`, obj `LowVoltageSwitch`, nom 600 V / 12000 W, rs 0.01, max 600 V / 12000 W, `cableThermalLoadInitializer.copy()`, signalSwitch false, icon `switch` (values exact vs source `SixNodeRegistration.kt:1350-1356`); craft recipe `"  I"," I ","CAC"` (rubber + Copper Cable + Copper 12 AWG Cable 600V) exact vs source `CraftingRecipes.kt:678-684`; texture `textures/item/switch_20a.png` byte-identical to source `textures/blocks/switch.png` (md5 `158dbf77…`, same as `switch_5a.png`). `:build` EXIT=0; `:test` 704/704 (0 fail, 3 pre-existing skips); `runClientData` idempotent (zero 2nd-run diff); live `summon` oracle resolves to "20A Switch" (5A regression → "5A Switch"; bogus-ID control → Air). See OVERNIGHT_PORT_PROGRESS.md 2026-11-18 20A entry. 50A in the next commit. |
| `230_kv_cable_feed_insulator` | No matching current item name/model found; registration/recipe parity still needs acceptance testing. |
| `230_kv_line_insulator` | No matching current item name/model found; registration/recipe parity still needs acceptance testing. |
| `2x_graphite_rods` | **Ported 2026-11-18** (ForkPatch 1.21.1): registered `eln:shared_item@4417`, textures byte-identical, craft recipe (2x rods ⇄ 2× rods), compressor Coal Plate ×4 recipe preserved. See OVERNIGHT_PORT_PROGRESS.md 2026-11-18 graphite entry. |
| `3.2_kv_cable_feed_insulator` | No matching current item name/model found; registration/recipe parity still needs acceptance testing. |
| `3.2_kv_line_insulator` | No matching current item name/model found; registration/recipe parity still needs acceptance testing. |
| `3.3v_boost_regulator_chip` | No matching current item name/model found; registration/recipe parity still needs acceptance testing. |
| `3.3v_ldo_regulator_chip` | No matching current item name/model found; registration/recipe parity still needs acceptance testing. |
| `30a_lead_fuse` | No matching current item name/model found; registration/recipe parity still needs acceptance testing. |
| `3x_graphite_rods` | **Ported 2026-11-19** (ForkPatch 1.21.1): `eln:shared_item@4418` (id 69, subId 2; plain `GenericItemUsingDamageDescriptor`, source `GraphiteDescriptor` adds nothing); texture `textures/item/3x_graphite_rods.png` byte-identical to source (md5 `2180777c…`); recipes shaped 3x→3× rod + shapeless 3× rod→3x and 2x+1 rod→3x (source `CraftingRecipes.kt:1719-1761`); datagen item/model/lang/recipe/advancement generated, `runClientData` idempotent. `:build` EXIT=0; `:test` 704/704 (3 pre-existing skips); `runGameTestServer` 1552 tests, only the 2 historical golden fails (unchanged); live summon oracle → "3x Graphite Rods" (1x/2x regressions + bogus-ID control → Air). See OVERNIGHT_PORT_PROGRESS.md 2026-11-19 3x entry. |
| `480_v_industrial_fluid_pump` | No matching current item name/model found; registration/recipe parity still needs acceptance testing. |
| `4x_graphite_rods` | **Ported 2026-11-19** (ForkPatch 1.21.1): `eln:shared_item@4419` (id 69, subId 3; plain `GenericItemUsingDamageDescriptor`, source `GraphiteDescriptor` adds nothing); texture `textures/item/4x_graphite_rods.png` byte-identical to source (md5 `93ffd68a…`); recipes: shaped `graphite_rod_3` 4x→4× rod, shapeless `4x_graphite_rods` 4× rod→4x, `4x_graphite_rods_2` 2x+2 rod→4x, `4x_graphite_rods_3` 2x+2x→4x, `4x_graphite_rods_4` 3x+rod→4x (source `CraftingRecipes.kt:1721,1740-1762`); datagen item/model/lang/recipe/advancement generated, `runClientData` idempotent. `:build` EXIT=0; `:test` 704/704 (3 pre-existing skips); `runGameTestServer` 1552 tests, only the 2 historical golden fails (unchanged); live summon oracle → "4x Graphite Rods" (1x/2x/3x regressions + bogus-ID control → Air). Not ported: compressor 4x from 1 Synthetic Diamond @ 80000 J (source `CraftingRecipes.kt:2428-2435`) — blocked, Synthetic Diamond unported. See OVERNIGHT_PORT_PROGRESS.md 2026-11-19 4x entry. |
| `50a_lead_fuse` | No matching current item name/model found; registration/recipe parity still needs acceptance testing. |
| `50a_switch` | **Ported 2026-11-18** (ForkPatch 1.21.1): `eln:six_node@6215` (subId 7, group 97 `sixNodeItem`), item `eln:switch_50a`; reuses the existing 5A/20A-switch `ElectricalSwitchDescriptor` pattern — cable `stdCableRender800V`, obj `HighVoltageSwitch`, nom 600 V / 30000 W, rs 0.005, max 600 V / 30000 W, `cableThermalLoadInitializer.copy()`, signalSwitch false, icon `switch` (values exact vs source `SixNodeRegistration.kt:1357-1363`); craft recipe `"AAI","AIA","CAC"` (rubber + Copper Cable + Copper 6 AWG Cable 600V) exact vs source `CraftingRecipes.kt:685-691`; texture `textures/item/switch_50a.png` byte-identical to source `textures/blocks/switch.png` (md5 `158dbf77…`, same as `switch_20a.png`). `:build` EXIT=0; `:test` 704/704 (0 fail, 3 pre-existing skips); `runClientData` idempotent (zero 2nd-run diff); live `summon` oracle resolves to "50A Switch" (20A regression → "20A Switch"; bogus-ID control → Air). See OVERNIGHT_PORT_PROGRESS.md 2026-11-18 50A entry. Both switches (20A + 50A) now ported. |
| `5a_lead_fuse` | **Ported 2026-11-18** (ForkPatch 1.21.1): `eln:shared_item@6288` (subId 16, group 98 `sharedItem`); reuses the cable-fuse `ElectricalFuseDescriptor` with a new `currentLimit` parameter (5.0 A, `cableDescriptor` null) — heat R `getSmallRs()×2`, in-circuit R `getSmallRs()×1`; warm limit `Eln.cableWarmLimit` (130 °C), trips at 0.9× warm limit (WP16b factor 0.9 preserved over source 0.8); blown state reuses existing `BlownFuse` (no new blown item). Craft recipe `rcr` ×4 (rubber + Copper 20 AWG Cable 300V) exact vs source `CraftingRecipes.kt:2777-2784`; texture byte-identical (md5 `199a8e8b…`, shared `electricalfuse` icon). `:test` 704/704, `runClientData` idempotent (0 lines 2nd-run diff), GameTest 1552 (same 2 historical golden fails), live `summon` oracle resolves to "5A Lead Fuse" (bogus-ID control → Air). See OVERNIGHT_PORT_PROGRESS.md 2026-11-18 5A entry. 10A in the next commit; 15A/20A/30A/50A out of scope. |
| `5v_boost_regulator_chip` | No matching current item name/model found; registration/recipe parity still needs acceptance testing. |
| `5v_ldo_regulator_chip` | No matching current item name/model found; registration/recipe parity still needs acceptance testing. |
| `69_kv_cable_feed_insulator` | No matching current item name/model found; registration/recipe parity still needs acceptance testing. |
| `69_kv_line_insulator` | No matching current item name/model found; registration/recipe parity still needs acceptance testing. |
| `800_v_cable_feed_insulator` | No matching current item name/model found; registration/recipe parity still needs acceptance testing. |
| `800_v_line_insulator` | No matching current item name/model found; registration/recipe parity still needs acceptance testing. |
| `alloy_turbine_blade` | No matching current item name/model found; registration/recipe parity still needs acceptance testing. |
| `arc_clay_block` | No matching current item name/model found; registration/recipe parity still needs acceptance testing. |
| `arc_clay_ingot` | No matching current item name/model found; registration/recipe parity still needs acceptance testing. |
| `arc_metal_block` | No matching current item name/model found; registration/recipe parity still needs acceptance testing. |
| `arc_metal_ingot` | No matching current item name/model found; registration/recipe parity still needs acceptance testing. |
| `canister_of_arc_water` | No matching current item name/model found; registration/recipe parity still needs acceptance testing. |
| `canister_of_water` | No matching current item name/model found; registration/recipe parity still needs acceptance testing. |
| `ceiling_color_segregated_cable_tray` | No matching current item name/model found; registration/recipe parity still needs acceptance testing. |
| `climate_controlled_growing_bed` | No matching current item name/model found; registration/recipe parity still needs acceptance testing. |
| `clutch` | No matching current item name/model found; registration/recipe parity still needs acceptance testing. |
| `clutch_pin` | **Ported 2026-11-19** (ForkPatch 1.21.1): `eln:shared_item@7735` (subId 55), `GenericItemUsingDamageDescriptorWithComment` with the 2-line tooltip, exact vs source `ItemRegistration.kt:1764` + `Clutch.kt:88`; texture byte-identical (md5 `2081a0ea…`); craft recipe ported (2× steel ingot → 4 pins, shaped `s/s`, `c:ingots/steel`-conditional per the established steel-recipe pattern); `:test` 704/704, `runClientData` idempotent, GameTest 1552 (only the 2 historical golden fails), live server-side `summon minecraft:item` resolves to “Clutch Pin” (bogus-ID control rejected). See OVERNIGHT_PORT_PROGRESS.md 2026-11-19 clutch session entry. |
| `coal_clutch_plate` | **Ported 2026-11-19** (ForkPatch 1.21.1): `eln:shared_item@7739` (subId 59), ratings `1024/128/128/32/0.1/explodes=true` exact vs source `Clutch.kt:38` (`explodes=true` unique among the six plates); texture byte-identical (md5 `0255f883…`); craft recipe ported (coal plate center + 4 tungsten-dust cross); `:test` 704/704, `runClientData` idempotent, GameTest 1552 (only the 2 historical golden fails), live server-side `summon minecraft:item` resolves to “Coal Clutch Plate” (bogus-ID control rejected). See OVERNIGHT_PORT_PROGRESS.md 2026-11-19 clutch session entry. |
| `cold_water_bucket` | No matching current item name/model found; registration/recipe parity still needs acceptance testing. |
| `config_copy_tool` | No matching current item name/model found; registration/recipe parity still needs acceptance testing. |
| `copper_clutch_plate` | **Ported 2026-11-18** (ForkPatch 1.21.1): `eln:shared_item@7737` (subId 57), ratings `8192/4096/1024/512/0.0003/false` exact vs source `Clutch.kt:38`; texture byte-identical (md5 `dbc662bb…`); craft recipe ported (copper plate center + 4 tungsten-dust cross); `:test` 704/704, `runClientData` idempotent, live server-side `summon minecraft:item` resolves to “Copper Clutch Plate” (bogus-ID control rejected). See OVERNIGHT_PORT_PROGRESS.md 2026-11-18 clutch-plate entry. |
| `corner_fireplace_tv_stand` | No matching current item name/model found; registration/recipe parity still needs acceptance testing. |
| `crank_shaft` | No matching current item name/model found; registration/recipe parity still needs acceptance testing. |
| `current_source` | No matching current item name/model found; registration/recipe parity still needs acceptance testing. |
| `diamond_dust` | IMPLEMENTED 2026-11-18 — `eln:shared_item@580` (subId=4, group 9 shared_item). Registration + byte-identical texture (sha256-verified vs source `diamonddust.png`) + generated model/item + `c:dusts/diamond` tag + `item.eln.diamond_dust` lang all present; `:test` 704/704 pass, GameTest 1552 (only 2 historical golden fails), `runClientData` idempotent (0 writes). No target consumer recipe — source consumes it in Inert Canister (out of scope). Previously: no matching current item name/model found; registration/recipe parity still needs acceptance testing. |
| `digital_display` | No matching current item name/model found; registration/recipe parity still needs acceptance testing. |
| `direct_utility_pole` | No matching current item name/model found; registration/recipe parity still needs acceptance testing. |
| `electric_minecart` | No matching current item name/model found; registration/recipe parity still needs acceptance testing. |
| `electric_patio_heater` | No matching current item name/model found; registration/recipe parity still needs acceptance testing. |
| `falstad_import_tool` | No matching current item name/model found; registration/recipe parity still needs acceptance testing. |
| `farmhouse_fireplace_tv_stand` | No matching current item name/model found; registration/recipe parity still needs acceptance testing. |
| `fixed_shaft` | No matching current item name/model found; registration/recipe parity still needs acceptance testing. |
| `gold_clutch_plate` | **Ported 2026-11-19** (ForkPatch 1.21.1): `eln:shared_item@7736` (subId 56), ratings `10240/2048/1024/512/0.001/false` exact vs source `Clutch.kt:38`; texture byte-identical (md5 `3eb2bc1c…`); craft recipe ported (gold plate center + 4 tungsten-dust cross); `:test` 704/704, `runClientData` idempotent, live server-side `summon minecraft:item` resolves to “Gold Clutch Plate” (bogus-ID control rejected). See OVERNIGHT_PORT_PROGRESS.md 2026-11-19 clutch-plate entry. |
| `graphite_rod` | **Ported 2026-11-18** (ForkPatch 1.21.1): registered `eln:shared_item@4416`, texture byte-identical, craft + compressor recipes preserved (Coal Plate ×4 at 80 kJ). See OVERNIGHT_PORT_PROGRESS.md 2026-11-18 graphite entry. |
| `grid_switch` | No matching current item name/model found; registration/recipe parity still needs acceptance testing. |
| `heavy_winding` | No matching current item name/model found; registration/recipe parity still needs acceptance testing. |
| `high_current_cable` | No matching current item name/model found; registration/recipe parity still needs acceptance testing. |
| `hot_water_bucket` | No matching current item name/model found; registration/recipe parity still needs acceptance testing. |
| `humidity_sensor` | No matching current item name/model found; registration/recipe parity still needs acceptance testing. |
| `inert_canister` | No matching current item name/model found; registration/recipe parity still needs acceptance testing. |
| `iron_clutch_plate` | **Ported 2026-11-18** (ForkPatch 1.21.1): `eln:shared_item@7734` (subId 54), ratings `5120/640/640/160/0.0001/false` exact vs source `Clutch.kt:38`; texture byte-identical (md5 `96feefcd…`); craft recipe ported (iron plate center + 4 tungsten-dust cross); `:test` 704/704, `runClientData` idempotent, live server-side `summon minecraft:item` resolves to “Iron Clutch Plate” (bogus-ID control rejected). See OVERNIGHT_PORT_PROGRESS.md 2026-11-18 clutch-plate entry. |
| `iron_turbine_blade` | **Ported 2026-11-18** (ForkPatch 1.21.1): `eln:shared_item_stack_one@1152` (subId 0, group 18 `sharedItemStackOne`), tier `iron` 0.8/0.8 (temp/corrosion resistance), `"Suitable for mild, clean fuels"`; condition NBT (`condition` 1.0→0.0) + tooltip (`Nominal Lifetime`/`Condition`/tier line) + per-tier config `items.turbineBlades.iron.{nominalLifeHours=10.0,infiniteLifeEnabled=false}`; texture byte-identical (sha256 `b0e1cfe3…`); craft recipe ported (` I ` / `IMI` / ` I `, iron ingots + Machine Block); `:test` 704/704, `runClientData` idempotent (0 writes 2nd run), GameTest 1552 (2 historical golden fails), live `summon` oracle resolves to "Iron Turbine Blade" (bogus-ID control rejected). See OVERNIGHT_PORT_PROGRESS.md 2026-11-18 turbine-blade entry. Turbine consumer machines + alloy/tungsten tiers out of scope. |
| `irresponsible_electrical_drill` | No matching current item name/model found; registration/recipe parity still needs acceptance testing. |
| `lapis_dust` | IMPLEMENTED 2026-11-18 — `eln:shared_item@579` (subId=3, group 9 shared_item). Registration + byte-identical texture (sha256-verified vs source `lapisdust.png`) + generated model/item + `c:dusts/lapis` tag + `item.eln.lapis_dust` lang all present; `:test` 704/704 pass, GameTest 1552 (only 2 historical golden fails), `runClientData` idempotent (0 writes). No target consumer recipe — source consumes it in Inert Canister (out of scope) and a Small Solar Panel recipe that the target implements with different materials. Previously: no matching current item name/model found; registration/recipe parity still needs acceptance testing. |
| `large_flywheel` | No matching current item name/model found; registration/recipe parity still needs acceptance testing. |
| `large_gas_turbine` | No matching current item name/model found; registration/recipe parity still needs acceptance testing. |
| `large_generator` | No matching current item name/model found; registration/recipe parity still needs acceptance testing. |
| `large_shaft_motor` | No matching current item name/model found; registration/recipe parity still needs acceptance testing. |
| `large_steam_turbine` | No matching current item name/model found; registration/recipe parity still needs acceptance testing. |
| `lead_clutch_plate` | **Ported 2026-11-19** (ForkPatch 1.21.1): `eln:shared_item@7738` (subId 58), ratings `15360/1024/1536/768/0.0015/false` exact vs source `Clutch.kt:38`; texture byte-identical (md5 `499ae9ed…`); craft recipe ported (lead plate center + 4 tungsten-dust cross); `:test` 704/704, `runClientData` idempotent, live server-side `summon minecraft:item` resolves to “Lead Clutch Plate” (bogus-ID control rejected). See OVERNIGHT_PORT_PROGRESS.md 2026-11-19 clutch-plate entry. |
| `low_current_cable` | No matching current item name/model found; registration/recipe parity still needs acceptance testing. |
| `medium_current_cable` | No matching current item name/model found; registration/recipe parity still needs acceptance testing. |
| `modern_fireplace_tv_stand` | No matching current item name/model found; registration/recipe parity still needs acceptance testing. |
| `motor_cowling` | No matching current item name/model found; registration/recipe parity still needs acceptance testing. |
| `mqtt_energy_meter` | No matching current item name/model found; registration/recipe parity still needs acceptance testing. |
| `mqtt_signal_controller` | No matching current item name/model found; registration/recipe parity still needs acceptance testing. |
| `multicolor_led_vumeter` | No matching current item name/model found; registration/recipe parity still needs acceptance testing. |
| `nixie_tube` | No matching current item name/model found; registration/recipe parity still needs acceptance testing. |
| `overhead_lines` | No matching current item name/model found; registration/recipe parity still needs acceptance testing. |
| `polarized_shaft_generator` | No matching current item name/model found; registration/recipe parity still needs acceptance testing. |
| `polarized_shaft_motor` | No matching current item name/model found; registration/recipe parity still needs acceptance testing. |
| `portable_electric_space_heater` | No matching current item name/model found; registration/recipe parity still needs acceptance testing. |
| `radial_motor` | No matching current item name/model found; registration/recipe parity still needs acceptance testing. |
| `reinforced_shaft` | No matching current item name/model found; registration/recipe parity still needs acceptance testing. |
| `rolling_shaft_machine` | No matching current item name/model found; registration/recipe parity still needs acceptance testing. |
| `steel_turbine_blade` | **Ported 2026-11-18** (ForkPatch 1.21.1): `eln:shared_item_stack_one@1153` (subId 1, group 18 `sharedItemStackOne`), tier `steel` 1.2/1.2 (temp/corrosion resistance), `"Handles moderate conditions"`; condition NBT + tooltip + per-tier config `items.turbineBlades.steel.{nominalLifeHours=64.0,infiniteLifeEnabled=false}` (source `TurbineBladeDescriptor.kt` `BladeConfigData("steel", 64.0)`); texture byte-identical (sha256 `b25777bc…`); craft recipe ported (`SIS`/`SMS`/`SIS`, steel ingots + iron center + Machine Block; generates a steel-tag variant and an iron-fallback variant via `firstExistingOre("ingotSteel","ingotIron")`); `:test` 704/704, `runClientData` idempotent (0 writes 2nd run), GameTest 1552 (same 2 historical golden fails), live `summon` oracle resolves to "Steel Turbine Blade" (bogus-ID control rejected; iron blade re-checked, still resolves). See OVERNIGHT_PORT_PROGRESS.md 2026-11-18 turbine-blade entries. Turbine consumer machines + alloy/tungsten tiers out of scope. |
| `synthetic_diamond` | No matching current item name/model found; registration/recipe parity still needs acceptance testing. |
| `thermal_heat_exchanger` | No matching current item name/model found; registration/recipe parity still needs acceptance testing. |
| `thermometer_sensor` | No matching current item name/model found; registration/recipe parity still needs acceptance testing. |
| `transmission_tower` | No matching current item name/model found; registration/recipe parity still needs acceptance testing. |
| `tungsten_turbine_blade` | No matching current item name/model found; registration/recipe parity still needs acceptance testing. |
| `turbo_electrical_drill` | No matching current item name/model found; registration/recipe parity still needs acceptance testing. |
| `wall-mounted_mini-split_air_conditioner` | No matching current item name/model found; registration/recipe parity still needs acceptance testing. |
| `window_air_conditioner` | No matching current item name/model found; registration/recipe parity still needs acceptance testing. |
| `wireless_paddle_button` | No matching current item name/model found; registration/recipe parity still needs acceptance testing. |
| `wireless_paddle_switch` | No matching current item name/model found; registration/recipe parity still needs acceptance testing. |
| `wireless_toggle_button` | No matching current item name/model found; registration/recipe parity still needs acceptance testing. |
| `wireless_toggle_switch` | No matching current item name/model found; registration/recipe parity still needs acceptance testing. |

### Optional integration (5)

| Source item/model | Finding |
| --- | --- |
| `eln_to_cee_power_bridge` | Separate integration/addon scope; not a normal standalone core-item omission. |
| `elnprobe` | Separate integration/addon scope; not a normal standalone core-item omission. |
| `powered_browser_television` | Separate integration/addon scope; not a normal standalone core-item omission. |
| `powered_web_jukebox` | Separate integration/addon scope; not a normal standalone core-item omission. |
| `wall-mounted_browser_television` | Separate integration/addon scope; not a normal standalone core-item omission. |

### Source rating/variant parity outstanding (39)

| Source item/model | Finding |
| --- | --- |
| `120v_battery_charger` | A legacy native machine/counterpart exists; source-specific voltage, power or variant is not established. |
| `120v_fast_battery_charger` | A legacy native machine/counterpart exists; source-specific voltage, power or variant is not established. |
| `240v_active_thermal_dissipator` | A legacy native machine/counterpart exists; source-specific voltage, power or variant is not established. |
| `240v_battery_charger` | A legacy native machine/counterpart exists; source-specific voltage, power or variant is not established. |
| `240v_compressor` | A legacy native machine/counterpart exists; source-specific voltage, power or variant is not established. |
| `240v_copper_heating_element` | A legacy native machine/counterpart exists; source-specific voltage, power or variant is not established. |
| `240v_evaporative_heat_sink` | A legacy native machine/counterpart exists; source-specific voltage, power or variant is not established. |
| `240v_fuel_generator` | A legacy native machine/counterpart exists; source-specific voltage, power or variant is not established. |
| `240v_iron_heating_element` | A legacy native machine/counterpart exists; source-specific voltage, power or variant is not established. |
| `240v_macerator` | A legacy native machine/counterpart exists; source-specific voltage, power or variant is not established. |
| `240v_magnetizer` | A legacy native machine/counterpart exists; source-specific voltage, power or variant is not established. |
| `240v_plate_machine` | A legacy native machine/counterpart exists; source-specific voltage, power or variant is not established. |
| `240v_tungsten_heating_element` | A legacy native machine/counterpart exists; source-specific voltage, power or variant is not established. |
| `240v_turbine` | A legacy native machine/counterpart exists; source-specific voltage, power or variant is not established. |
| `240v_type_e_socket` | A legacy native machine/counterpart exists; source-specific voltage, power or variant is not established. |
| `240v_type_j_socket` | A legacy native machine/counterpart exists; source-specific voltage, power or variant is not established. |
| `24v_egg_incubator` | A legacy native machine/counterpart exists; source-specific voltage, power or variant is not established. |
| `24v_small_active_thermal_dissipator` | A legacy native machine/counterpart exists; source-specific voltage, power or variant is not established. |
| `480v_defence_turret` | A legacy native machine/counterpart exists; source-specific voltage, power or variant is not established. |
| `480v_heavy_tungsten_heating_element` | A legacy native machine/counterpart exists; source-specific voltage, power or variant is not established. |
| `480v_tungsten_heating_element` | A legacy native machine/counterpart exists; source-specific voltage, power or variant is not established. |
| `48v_compressor` | A legacy native machine/counterpart exists; source-specific voltage, power or variant is not established. |
| `48v_copper_heating_element` | A legacy native machine/counterpart exists; source-specific voltage, power or variant is not established. |
| `48v_fuel_generator` | A legacy native machine/counterpart exists; source-specific voltage, power or variant is not established. |
| `48v_iron_heating_element` | A legacy native machine/counterpart exists; source-specific voltage, power or variant is not established. |
| `48v_macerator` | A legacy native machine/counterpart exists; source-specific voltage, power or variant is not established. |
| `48v_magnetizer` | A legacy native machine/counterpart exists; source-specific voltage, power or variant is not established. |
| `48v_plate_machine` | A legacy native machine/counterpart exists; source-specific voltage, power or variant is not established. |
| `48v_tungsten_heating_element` | A legacy native machine/counterpart exists; source-specific voltage, power or variant is not established. |
| `48v_turbine` | A legacy native machine/counterpart exists; source-specific voltage, power or variant is not established. |
| `shaft_motor` | A legacy native machine/counterpart exists; source-specific voltage, power or variant is not established. |
| `small_240v_copper_heating_element` | A legacy native machine/counterpart exists; source-specific voltage, power or variant is not established. |
| `small_240v_iron_heating_element` | A legacy native machine/counterpart exists; source-specific voltage, power or variant is not established. |
| `small_240v_tungsten_heating_element` | A legacy native machine/counterpart exists; source-specific voltage, power or variant is not established. |
| `small_480v_heavy_tungsten_heating_element` | A legacy native machine/counterpart exists; source-specific voltage, power or variant is not established. |
| `small_480v_tungsten_heating_element` | A legacy native machine/counterpart exists; source-specific voltage, power or variant is not established. |
| `small_48v_copper_heating_element` | A legacy native machine/counterpart exists; source-specific voltage, power or variant is not established. |
| `small_48v_iron_heating_element` | A legacy native machine/counterpart exists; source-specific voltage, power or variant is not established. |
| `small_48v_tungsten_heating_element` | A legacy native machine/counterpart exists; source-specific voltage, power or variant is not established. |

### Unresolved legacy/internal/recovery mapping (7)

| Source item/model | Finding |
| --- | --- |
| `energyconverter` | Do not count as confirmed missing until descriptor IDs and replacement behavior are reconciled. |
| `experimental_battery` | Do not count as confirmed missing until descriptor IDs and replacement behavior are reconciled. |
| `legacy_signal_switch` | Do not count as confirmed missing until descriptor IDs and replacement behavior are reconciled. |
| `old_480v_arc_furnace` | Do not count as confirmed missing until descriptor IDs and replacement behavior are reconciled. |
| `signal_bus_cable` | Do not count as confirmed missing until descriptor IDs and replacement behavior are reconciled. |
| `utility_pole_cobrahead_street_light` | Do not count as confirmed missing until descriptor IDs and replacement behavior are reconciled. |
| `utility_pole_street_light` | Do not count as confirmed missing until descriptor IDs and replacement behavior are reconciled. |

## Validation and limitations

Documentation-only audit; no game, GUI or unit tests were run for this list. Source registration methods, conditional registrations and the current generated catalogs were inspected. To certify an exact complete active-item set, export both runtime registries with identical optional-mod/development settings, reconcile descriptor IDs rather than names, and test each mapped adapter. That certification remains open. This report supersedes stale missing-item assumptions in the old completion audit, but does not silently rewrite its historical evidence.
