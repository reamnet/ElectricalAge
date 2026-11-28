# Rigid busbars and substation commissioning

## Build a bay

1. Place the disconnect, transformer and recloser on their supports. Leave the switches open. Set a configurable transformer’s primary and secondary voltages and winding count before wiring its sockets.
2. Use the small brass-centered pads on the side or top of the equipment. Right-click a pad with a busbar. The pad fixes the exact bank and phase, and the attached metal extension reaches the corresponding clamp. The pads rotate with the equipment. A three-phase unit has three sockets on each bank, and a single-phase unit has one on each bank. The side and top pads are alternative approaches to the same clamp.
3. Right-click a placed busbar with an Electrical Age brush. Hold a brush in the off hand while placing a busbar to assign the color before any joint can form. An application uses one charge in survival unless the section already has that color. Sneak-right-click a busbar with an empty main hand to clear its paint. White is a circuit color, and unpainted sections form their own group.
4. Use different colors for independently supplied runs that touch along their sides, vertically or at a junction. Matching colors can join across metal types or section sizes. The takeoffs for different clamps on the same machine remain separated at the sockets. Beyond the takeoffs, use paint or physical spacing to keep independent runs apart. When both approaches to one clamp are occupied by modern sections, they only conduct through it if they have the same color. Remove the unused approach if the clamp has two competing colors.
5. Mount a cable feed insulator directly on each incoming or outgoing busbar. The face touching the bar is its rigid electrical contact. Attach the overhead span from the feed’s conductor end to the corresponding pole or standalone feed. Select an insulator rated for the feeder voltage. Paint has no electrical phase assignment. The overhead routing rules still apply to the chosen pole terminals and spans.
6. Connect a separate load to each outgoing feeder and close the switches. Check each phase’s voltage and current with the multimeter or inspection interface. Inspection shows the bar’s paint color and any equipment terminal it physically reaches. Open each switch in turn and confirm that all downstream load currents stop.

Choose any of the 16 brush colors for each circuit. A red bar on a transformer’s primary B socket is still connected to primary B. Its adjoining red sections conduct as one run. A separate red run may connect to secondary B without crossing the transformer’s primary and secondary loads. Match the pad to the expected bank before energizing the transformer.

## Continuous current and thermal behavior

The eight items retain their original sizes, base continuous-current ratings and material properties. Resistance is for one section at 20 °C. The simulation assigns half to each end, uses the actual current through each joint, and increases the resistance as the metal heats.

| Profile | Section area | Copper rating | Aluminum rating | Copper resistance | Aluminum resistance | Copper mass | Aluminum mass |
|---|---:|---:|---:|---:|---:|---:|---:|
| Small | 60 mm² | 180 A | 144 A | 287 µΩ | 471 µΩ | 0.538 kg | 0.162 kg |
| Medium | 240 mm² | 500 A | 400 A | 71.8 µΩ | 118 µΩ | 2.15 kg | 0.648 kg |
| Large | 800 mm² | 1,200 A | 960 A | 21.6 µΩ | 35.3 µΩ | 7.17 kg | 2.16 kg |
| Extra-Large | 2,000 mm² | 2,500 A | 2,000 A | 8.62 µΩ | 14.1 µΩ | 17.9 kg | 5.40 kg |

Bars use the existing bare-conductor cooling, heat capacity and phase-change model shared with Electrical Age’s wire simulation. Current above the continuous rating heats the metal according to its actual I²R losses. Sustained overload can consume the material’s fusion energy and open the circuit. Copper melts at 1,085 °C and aluminum at 660 °C. The upstream breaker or recloser supplies immediate fault protection.

A fresh bar can carry any supported network voltage. Choose its size for current and thermal duty, and choose the insulators and equipment for the network voltage. The multimeter still reads the actual operating voltage.

## Existing worlds

Back up the world and de-energize a bay before converting it. The 24 modular substation item IDs, recipes, equipment settings and saved paid spans retain their identities.

Existing sections with no paint state load into separate **Legacy phase A**, **Legacy phase B** and **Legacy phase C** groups. Their prior phase matches and transformer contact eligibility stay in effect until the player converts the section. These groups remain separate from the unpainted group and all brush colors. The old style phase stripes identify these groups, and inspection names them explicitly. A fresh section placed next to an old one needs the circuit to be converted before it can extend that run.

Convert a whole isolated run with one brush color, or clear it if the whole run will use the unpainted group. Do this with its feeders open because joins are recalculated on each click. Check both banks and every phase against the multimeter. Previously ineligible transformer contacts become available as each old section is converted.

Previously paid suspended spans load with their saved electrical terminals and cable data. To convert a wired modular terminal, break its wired endpoint in survival to recover the paid spans, reinstall the endpoint, and route the spans through mounted feed insulators. Painted bars retain their color when picked up, saved or reloaded.

## Development and verification

The earlier implementation could find nominal contact cells on ghost blocks, but each bar carried a preset electrical phase. The item started in phase A and could only join an identically numbered clamp. Transformer banks also rejected profiles below the former voltage tier. The old mounting surface exposed little guidance about which ghost face belonged to a particular clamp.

The layout now supplies both the rendered socket pad and the rotated world contact. An equipment contact uses the specific terminal load and publishes its bar’s extension endpoint. A color change updates only the affected physical joints in the solver and keeps the conductor’s voltage and thermal state. The ordinary node publication supplies the client with the selected color, arms and clamp offsets.

The focused GameTests use the normal item interaction with ray hits on each of the ten equipment variants at four rotations. They cover separate loaded windings, switch opening, mixed sizes and materials, recoloring, brush charges, insulator feeds, voltage-independent loaded circuits, material failure, migration, saved spans, client publication and actual chunk eviction. See [Testing](testing.md) for the harness and test structure generator.

## Loaded commissioning measurements

The 26.3 client built a distribution bay with three ordinary electrical sources, six overhead spans, three feed insulators on each side of the rigid yard, a gang disconnect, a configurable transformer, a recloser and three grounded 2.20 kΩ resistors. All equipment and conductors were placed with player clicks. The resistors were set with coal dust in their inventories, and the sources were set in their voltage screens.

| Equipment phase | Circuit color | Primary supply | Load voltage | Load current | Load power |
|---|---|---:|---:|---:|---:|
| A | Blue | 12.50 kV | 400.0 V | 0.1818 A | 72.72 W |
| B | Yellow | 11.50 kV | 368.0 V | 0.1673 A | 61.55 W |
| C | Red | 10.50 kV | 336.0 V | 0.1527 A | 51.31 W |

The simulator reported a separate network for each row. Opening the disconnect or the recloser dropped the downstream current to zero on all three phases. Closing the controls restored the three loaded readings.
