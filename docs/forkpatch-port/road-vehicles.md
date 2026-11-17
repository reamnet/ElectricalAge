# Road vehicles and charging — implementation checkpoint

Source: ForkPatch b7444a7b8a0c5e072b1fd35eed2b74a282c3348c (read-only).

Implemented shared items7492/7493 (cart/coupe),7494 (100A charging lead), and transparent device8079 (240V100A station). Shared-item IDs do not replace six-node watch IDs. All four source recipes and six original OBJ/MTL files are included.

The cart has four seats, one20kWh pack,10m/s limit and18kW motor; the coupe has two seats, six packs/120kWh,45m/s limit and220kW motor. Native server-authoritative entities implement bounded driver controls, timeout, collisions, steering, energy consumption, pack discharge limits and the cart's dangerous-turn hazard. Native source-positioned176×188 menus expose six pack bays (one active for cart) and nine cargo slots. Pack components, cargo and health persist; control inputs, riders and charging connections are intentionally transient on restart.

The dedicated lead selects/plugs/unplugs a vehicle within six blocks. A station owns one mutually exclusive target, rejects mounted/driven/invalid owners, and checks live UUID identity. Charging accepts200–264V magnitude, limits100A/24kW, uses94% efficiency and distributes accepted energy fairly across packs. Solver trials/retries cannot manufacture energy. This is a dedicated vehicle adapter, not a20A appliance cord. Reconnect after a world restart.

Native renderer/input/payload registration includes original palettes, rotating wheels and translucent glass. These are implementation/native checks, not live visual certification.

28/28 real native GameTests pass: headings/drive, driver/seats/timeout, menus/save/placement/drops, graphics extraction, charging lead identity/range, turn hazard, positive/negative/sag/overvoltage charging, retry accounting, placed power ports and mixed-pack distribution. See [machine-readable evidence](road-vehicle-validation.json). Live client driving, menu screenshots, sound, fresh-JVM recovery and simultaneous multiplayer remain pending.

For later live testing use `/elnlab roadvehicles <pos>` and `/elnlab roadmenu 0` or `1` from a player context. Helpers compile; this session has not run them in a live client. Preserve existing worlds and use a new private test world.
