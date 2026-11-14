# Electric forklift and equipment pallet

The Electric Forklift and Warehouse Equipment Pallet retain shared-item IDs
7490/7491 in `shared_item_stack_one`, with native entity IDs
`eln:electric_forklift` and `eln:equipment_pallet`. Existing destination bikes
and their controls are unchanged.

Craft the forklift from three welded frames, two copper cables, an electrical
motor, and three iron blocks. Craft a pallet from six oak planks and two sticks.
The forklift is supplied without a battery. Craft and charge a 48 V 20 kWh
Traction Battery in the EV Pack Charger first.

Use a transport item against a clear block face to place it. A blocked placement
keeps the item. A packed pallet containing inventory is consumed even in creative
mode so placing it cannot make a second copy of its cargo.

## Driving and lifting

While parked and unoccupied, sneak with a traction pack to install it; sneak
empty-handed to remove it with its remaining charge. Right-click to mount.
Movement keys drive and steer. Page Up/Down raise and lower the forks, and G
picks up or unloads a pallet. Sneak dismounts. Keys are configurable in the
Electrical Age vehicles category.

The server owns movement, battery use, lift height, and cargo. The client sends
only four bounded controls, with neutral input while a screen is open. Inputs
expire after five ticks without refresh. Forward speed approaches 3.6 blocks/s
and reverse speed 1.6 blocks/s; acceleration is bounded. Acceleration, rolling
resistance, occupied idle draw, and lifting debit the actual removable pack.
Loaded lifting pays for the greater mass. Lowering does not recharge the pack,
and empty packs cannot create energy. Fork travel is bounded to 0–2.5 m.

Stop and lower the forks to pick up a nearby pallet in front of the forklift.
An open pallet cannot be picked up. Holding G produces one pickup/unload action
until released. Pickup removes the world pallet and stores one inventory-bearing
item as cargo; unload creates one pallet only when there is room. Pack, cargo,
and lift height survive native entity saves. Motion and control inputs restart
at rest on reload.

## Pallet storage and recovery

Right-click a pallet for its 27-slot native inventory. Server-side menu slots
reject pallets and forklifts, including shift-click insertion. Sneak empty-handed
to pack a closed pallet into one item. While its inventory is open, breaking or
forklift pickup cannot create a second copy. Packing, unpacking, and entity saves
preserve item components and exact counts.

Breaking a forklift returns one base item, its actual remaining-energy pack,
and one loaded pallet if carried. Repeated damage to a discarded entity cannot
refund them again. The native renderer uses source models, a snapshot of lift
and cargo state, and the 26.3 geometry submission API. Render culling bounds cover rotated forks
and raised cargo independently of the collision box. Registered client controls
and headless render-state/geometry checks are covered; interactive client/audio
and multiplayer sessions remain part of wider port validation.
