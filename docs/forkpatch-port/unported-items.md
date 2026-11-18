# Remaining item inventory — 2026-11-17

Source baseline: `b7444a7b8a0c5e072b1fd35eed2b74a282c3348c`. Target checkpoint: `86ab755f`.

This is a static inventory audit, **not a certified exhaustive runtime missing-item count**. It compares all 930 source generated item models with 895 target generated item entries and the target descriptor manifest, then separates known renames, source-rated variants, and integration/hidden items. Matching names do not prove matching behavior; generated source models can also be stale. The complete 177-row difference inventory is below and in `unported-items-audit.json`. Rows marked candidates require descriptor/registration acceptance before being treated as confirmed omissions.

## Main remaining groups

- Climate: Portable Electric Space Heater; Modern, Farmhouse and Corner Fireplace TV Stands; Window Air Conditioner; Wall-mounted Mini-split Air Conditioner; Electric Patio Heater.
- Machines: 480 V Industrial Fluid Pump; Climate Controlled Growing Bed; Thermal Heat Exchanger; missing shaft/large-machine families listed below.
- Mechanics: Clutch, Fixed Shaft, Radial Motor, Crank Shaft, Reinforced Shaft, Rolling Shaft Machine, Large Flywheel, Large Generator, Large Shaft Motor, Large Gas Turbine, Large Steam Turbine, Polarized Shaft Generator and Motor; clutch plates/pin, Heavy Winding, Motor Cowling and four material-specific turbine blades.
- Controls/instruments: Current Source, Digital Display, Nixie Tube, Multicolor LED Vumeter, Humidity Sensor, Thermometer Sensor, MQTT Energy Meter and MQTT Signal Controller; extra wireless controls, current-rated switches/fuses and regulator chips.
- Tools/materials: Config Copy Tool, Falstad Import Tool, Turbo and Irresponsible Electrical Drills, Electric Minecart, graphite rods, arc materials/canisters, additional dusts and Synthetic Diamond.
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
| `10a_lead_fuse` | No matching current item name/model found; registration/recipe parity still needs acceptance testing. |
| `12.5_kv_cable_feed_insulator` | No matching current item name/model found; registration/recipe parity still needs acceptance testing. |
| `12.5_kv_line_insulator` | No matching current item name/model found; registration/recipe parity still needs acceptance testing. |
| `12v_ldo_regulator_chip` | No matching current item name/model found; registration/recipe parity still needs acceptance testing. |
| `15a_lead_fuse` | No matching current item name/model found; registration/recipe parity still needs acceptance testing. |
| `20a_lead_fuse` | No matching current item name/model found; registration/recipe parity still needs acceptance testing. |
| `20a_switch` | No matching current item name/model found; registration/recipe parity still needs acceptance testing. |
| `230_kv_cable_feed_insulator` | No matching current item name/model found; registration/recipe parity still needs acceptance testing. |
| `230_kv_line_insulator` | No matching current item name/model found; registration/recipe parity still needs acceptance testing. |
| `2x_graphite_rods` | No matching current item name/model found; registration/recipe parity still needs acceptance testing. |
| `3.2_kv_cable_feed_insulator` | No matching current item name/model found; registration/recipe parity still needs acceptance testing. |
| `3.2_kv_line_insulator` | No matching current item name/model found; registration/recipe parity still needs acceptance testing. |
| `3.3v_boost_regulator_chip` | No matching current item name/model found; registration/recipe parity still needs acceptance testing. |
| `3.3v_ldo_regulator_chip` | No matching current item name/model found; registration/recipe parity still needs acceptance testing. |
| `30a_lead_fuse` | No matching current item name/model found; registration/recipe parity still needs acceptance testing. |
| `3x_graphite_rods` | No matching current item name/model found; registration/recipe parity still needs acceptance testing. |
| `480_v_industrial_fluid_pump` | No matching current item name/model found; registration/recipe parity still needs acceptance testing. |
| `4x_graphite_rods` | No matching current item name/model found; registration/recipe parity still needs acceptance testing. |
| `50a_lead_fuse` | No matching current item name/model found; registration/recipe parity still needs acceptance testing. |
| `50a_switch` | No matching current item name/model found; registration/recipe parity still needs acceptance testing. |
| `5a_lead_fuse` | No matching current item name/model found; registration/recipe parity still needs acceptance testing. |
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
| `clutch_pin` | No matching current item name/model found; registration/recipe parity still needs acceptance testing. |
| `coal_clutch_plate` | No matching current item name/model found; registration/recipe parity still needs acceptance testing. |
| `cold_water_bucket` | No matching current item name/model found; registration/recipe parity still needs acceptance testing. |
| `config_copy_tool` | No matching current item name/model found; registration/recipe parity still needs acceptance testing. |
| `copper_clutch_plate` | No matching current item name/model found; registration/recipe parity still needs acceptance testing. |
| `corner_fireplace_tv_stand` | No matching current item name/model found; registration/recipe parity still needs acceptance testing. |
| `crank_shaft` | No matching current item name/model found; registration/recipe parity still needs acceptance testing. |
| `current_source` | No matching current item name/model found; registration/recipe parity still needs acceptance testing. |
| `diamond_dust` | No matching current item name/model found; registration/recipe parity still needs acceptance testing. |
| `digital_display` | No matching current item name/model found; registration/recipe parity still needs acceptance testing. |
| `direct_utility_pole` | No matching current item name/model found; registration/recipe parity still needs acceptance testing. |
| `electric_minecart` | No matching current item name/model found; registration/recipe parity still needs acceptance testing. |
| `electric_patio_heater` | No matching current item name/model found; registration/recipe parity still needs acceptance testing. |
| `falstad_import_tool` | No matching current item name/model found; registration/recipe parity still needs acceptance testing. |
| `farmhouse_fireplace_tv_stand` | No matching current item name/model found; registration/recipe parity still needs acceptance testing. |
| `fixed_shaft` | No matching current item name/model found; registration/recipe parity still needs acceptance testing. |
| `gold_clutch_plate` | No matching current item name/model found; registration/recipe parity still needs acceptance testing. |
| `graphite_rod` | No matching current item name/model found; registration/recipe parity still needs acceptance testing. |
| `grid_switch` | No matching current item name/model found; registration/recipe parity still needs acceptance testing. |
| `heavy_winding` | No matching current item name/model found; registration/recipe parity still needs acceptance testing. |
| `high_current_cable` | No matching current item name/model found; registration/recipe parity still needs acceptance testing. |
| `hot_water_bucket` | No matching current item name/model found; registration/recipe parity still needs acceptance testing. |
| `humidity_sensor` | No matching current item name/model found; registration/recipe parity still needs acceptance testing. |
| `inert_canister` | No matching current item name/model found; registration/recipe parity still needs acceptance testing. |
| `iron_clutch_plate` | No matching current item name/model found; registration/recipe parity still needs acceptance testing. |
| `iron_turbine_blade` | No matching current item name/model found; registration/recipe parity still needs acceptance testing. |
| `irresponsible_electrical_drill` | No matching current item name/model found; registration/recipe parity still needs acceptance testing. |
| `lapis_dust` | No matching current item name/model found; registration/recipe parity still needs acceptance testing. |
| `large_flywheel` | No matching current item name/model found; registration/recipe parity still needs acceptance testing. |
| `large_gas_turbine` | No matching current item name/model found; registration/recipe parity still needs acceptance testing. |
| `large_generator` | No matching current item name/model found; registration/recipe parity still needs acceptance testing. |
| `large_shaft_motor` | No matching current item name/model found; registration/recipe parity still needs acceptance testing. |
| `large_steam_turbine` | No matching current item name/model found; registration/recipe parity still needs acceptance testing. |
| `lead_clutch_plate` | No matching current item name/model found; registration/recipe parity still needs acceptance testing. |
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
| `steel_turbine_blade` | No matching current item name/model found; registration/recipe parity still needs acceptance testing. |
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
