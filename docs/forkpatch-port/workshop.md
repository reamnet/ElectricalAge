# Charging packs and welding equipment

The native adapters preserve source IDs 8071 (Programmable EV Pack Charger),
8072 (Electric Welding Station), 7488 in `shared_item_stack_one` (48 V 20 kWh
Traction Battery), and 7489 in `shared_item` (Welded Machine Frame). The unrelated
six-node devices at 7488/7489 keep their original identities.

## Programmable EV Pack Charger

Connect power to the left or bottom terminal, insert one battery, and open the
machine to program nominal voltage, input power, and start/end hours. Nominal
voltage accepts 120–480 V and programmed power accepts 100–50,000 W. Equal hours
mean continuous operation; a start hour later than the end hour spans midnight.
Hours follow the world's day clock: tick 0 is 06:00 and tick 18,000 is midnight.

The load is resistive: at half nominal voltage it draws one quarter of programmed
power. It stores 94% of accepted input energy, capped by the individual battery's
charge rating and remaining capacity. Higher supply voltage cannot bypass that
charge rating. An absent or full battery, disabled control, high front-bottom
inhibit signal, unavailable owner chunk, or voltage above 130% of nominal opens
the input. The front-top trip terminal remains wired low as in the source.

The 20 kWh traction battery is crafted empty from five UPS Industrial Battery
Modules, two iron plates, and two copper cables. Its charge rating is 11,520 W
and discharge rating is 50,000 W. Other real battery types retain their own
capacities and charge ratings. Battery energy lives in the removable item;
manual extraction, shift-click, hopper removal, replacement, clearing, and saving
flush cached charge first. Replacing the pack resets the session counter.

## Electric Welding Station

Connect power to the left or bottom terminal. The left slot takes an iron ingot
or damaged tool; the middle slot takes one iron ingot. Keep the output slot clear.
The machine needs 120 kJ of accepted energy per operation. At its default 240 V,
6,000 W settings that is 20 seconds of powered welding. Voltage sag slows work.

Two iron ingots produce one Welded Machine Frame. A damaged tool and one iron
ingot produce the same tool with one quarter of its maximum durability restored,
rounded down with a minimum repair of one point. Names, enchantments, custom
data, and other item components are preserved. Work stops when the output is
occupied; hoppers insert only valid inputs and extract only the output. The
original arc sound and spark effect accompany powered welding.

Changing the workpiece's item or components resets progress. A placed-world save
preserves progress only with a matching saved workpiece template. Carried-machine
and configuration-tool data transfer settings without copying batteries, machine
inventories, paid welding work, or meter history.

Native save checks are not a converter for complete 1.21.1 worlds. Appliance
cords/outlets and interactive client, audio, and multiplayer validation remain
part of the wider port backlog.
