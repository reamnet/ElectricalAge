# Decorative lighting: first fixed-fixture batch

Reviewed source: `b7444a7b8a0c5e072b1fd35eed2b74a282c3348c` in
`../ELN1.21.1ForkPatch`. Continued from native checkpoint `7f5497f4` after
checking the remote branch. The source catalog contains **108 decorative styles**;
this batch registers **95 fixed fixtures**, leaving the other 13 unregistered.

The original OBJ/MTL files and inventory icons are copied byte for byte. One shared
renderer retains their mesh parts, source palette, absent-bulb visibility, lit lenses,
glass, mounting rotations and item poses. World render bounds include the complete
fixture depth. Source reservations use the native empty clearance shape rather than
solid ghost cubes. Mesh emission is checked for every fixture; this alone does not
establish pixel-for-pixel parity on every graphics device.

## Using the fixtures

Place sconces on a wall, chandeliers/recessed/shop/factory lights on a ceiling,
and bollard/post lights on a floor. The plain holder supports any full supporting
face. Insert matching bulbs by using them on the fixture or opening its menu.
A partially filled fixture works with its installed bulbs; unused exposed bulb
positions stay empty. Use a wrench to turn the fixture.

The source-sized **176×166** menu retains the power button at `(16,7)`, channel
field at `(17,37)`, bulb slot at `(17,60)` and player inventory at `(8,84)`.
Recessed fittings use lamp supplies only. Other fixtures select a lamp-supply
channel or native cable power. Native built-in cable terminals replace the old
required cable item slot. Portable cords are not implemented for this batch.

Source **120 V / 15 W** and **240 V / 22.5 W** LED bulbs are now available with
672-hour nominal lifetimes. Use a matching rated lamp supply or actual wired
voltage. The 50/200 V native bulbs also remain compatible when supplied at their
own ratings. Each installed bulb contributes its real parallel conductance to
both wired loads and supply billing; brightness and aging use the per-bulb rating.
No extra electricity is created by choosing a channel.

Bulb quantity, exact item components/condition, orientation, power mode and channel
survive native owner reload. Repeated insertion stops at capacity and cannot merge
fresh bulbs into a used stack. The existing native stack-aging model is retained;
this is not a rewrite of the source's separate technology/configuration system.
Light projection uses original head coordinates and beam directions, stops at
opaque/unloaded cells and refreshes short-lived light leases. Cylinder sconces
project both upwards and downwards.

## Stable identities and paid crafting

| Native/source lookup | Fixture | Bulbs |
|---|---|---|
| 4116 | Twin Parking Lot Light | 2 |
| 4117 | Warehouse Wall Floodlight | 1 |
| 4118 | Household Desk Lamp | 1 |
| 4119 | Round Recessed Ceiling Light | 1 |
| 4120 | Basic Chandelier | 5 |
| 4121 | Mini Dot Spotlight | 1 |
| 4122 | Porch Lantern Sconce | 1 |
| 4123 | Basic Lamp Holder | 1 |
| 4124 | Thin Fluorescent Shop Light | 2 |
| 4125 | Suspended Factory Fluorescent Light | 4 |
| 4127 | Candle Arm Foyer Chandelier | 6 |
| 4128 | Crystal Tier Foyer Chandelier | 6 |
| 4129 | Wagon Wheel Foyer Chandelier | 8 |
| 4130 | Lantern Cage Foyer Chandelier | 4 |
| 4131 | Modern Sputnik Foyer Chandelier | 8 |
| 4132 | Up Down Cylinder Wall Sconce | 2 |
| 4133 | Fabric Shade Wall Sconce | 1 |
| 4134 | Glass Globe Wall Sconce | 1 |
| 4135 | Industrial Cage Wall Sconce | 1 |
| 4136 | Double Candle Wall Sconce | 2 |
| 4137 | Ceramic Drum Table Lamp | 1 |
| 4138 | Green Banker Table Lamp | 1 |
| 4139 | Articulated Task Table Lamp | 1 |
| 4140 | Deep Baffle Recessed Light | 1 |
| 4141 | Trimless Recessed Downlight | 1 |
| 4142 | Adjustable Gimbal Recessed Light | 1 |
| 4143 | Garden Bollard Light | 1 |
| 4144 | Gooseneck Barn Light | 1 |
| 4145 | Traditional Lantern Post Light | 1 |
| 4352 | Window Candle Light | 1 |
| 4353 | Standing Drum Floor Lamp | 1 |
| 4354 | Under Cabinet LED Strip | 2 |
| 4355 | Recessed LED Channel | 2 |
| 4356 | Indirect Cove LED Strip | 2 |
| 4357 | Surface Linear LED Strip | 4 |
| 4358 | Flexible Neon LED Strip | 3 |
| 4359 | Grand Halo Ring Chandelier | 16 |
| 4360 | Grand Crystal Cascade Chandelier | 20 |
| 4361 | Sculptural Branch Chandelier | 18 |
| 4362 | Spiral Rain Chandelier | 20 |
| 4363 | Grand Empire Chandelier | 16 |
| 4364 | Heavy Duty Stadium Wall Floodlight | 4 |
| 4365 | Armored Oval Bulkhead Light | 1 |
| 4366 | Industrial Wall Pack Light | 2 |
| 4367 | Rugged Outdoor Canopy Light | 4 |
| 4368 | Industrial Yard Area Light | 2 |
| 4369 | Offshore Caged Wellglass Light | 1 |
| 4370 | Offshore Sealed Linear Light | 2 |
| 4371 | Offshore Platform Floodlight | 2 |
| 4372 | Modern LED Street Light | 1 |
| 4373 | Boulevard Lantern Street Light | 2 |
| 4374 | Warm White Christmas Fairy Lights | 6 |
| 4375 | Multicolor Christmas String Lights | 6 |
| 4376 | Christmas Icicle Lights | 6 |
| 4377 | Christmas Star Wall Light | 1 |
| 4378 | Halloween Pumpkin String Lights | 4 |
| 4379 | Halloween Ghost String Lights | 4 |
| 4380 | Halloween Bat Wall Light | 1 |
| 4381 | Jack O Lantern Table Lamp | 1 |
| 4382 | Mini Pinhole Dot Light | 1 |
| 4383 | Mini Bezel Dot Spotlight | 1 |
| 4384 | Mini Eyeball Dot Spotlight | 1 |
| 4385 | Mini Deep Baffle Dot Light | 1 |
| 4386 | Mini Twin Dot Light | 2 |
| 4387 | Twin Adjustable Eave Floodlight | 2 |
| 4392 | Large Area Floodlight | 4 |
| 4393 | Quad Head Parking Lot Light | 4 |
| 4394 | Round Canopy Parking Lot Light | 4 |
| 4395 | Offset Twin Arm Parking Lot Light | 2 |
| 4401 | Patio Festoon Party Lights | 5 |
| 4402 | Electric Poolside Tiki Light | 1 |
| 4404 | Poolside Globe String Lights | 4 |
| 4405 | Poolside Party Lantern | 1 |
| 4406 | Deck Post Cap Light | 1 |
| 4407 | Deck Step Riser Light | 1 |
| 4480 | Nautical Pier Bollard Light | 1 |
| 4481 | Marine Pier Bulkhead Light | 1 |
| 4482 | Harbor Gooseneck Pier Light | 1 |
| 4483 | Pier Lantern Post Light | 1 |
| 4484 | Low Profile Dock Light | 1 |
| 4485 | Louvered Walkway Bollard | 1 |
| 4486 | Shielded Walkway Light | 1 |
| 4487 | Modern Blade Walkway Light | 1 |
| 4488 | Mushroom Walkway Light | 1 |
| 4489 | Arched Walkway Light | 1 |
| 4490 | Recessed Brick Patio Wall Light | 1 |
| 4491 | Round Stone Wall Light | 1 |
| 4492 | Slotted Patio Wall Light | 1 |
| 4493 | Recessed Square Patio Wall Light | 1 |
| 4494 | Stone Wall Wash Light | 1 |
| 4495 | Shoebox Parking Lot Light | 1 |
| 4496 | Twin Wide Area Parking Lot Light | 2 |
| 4497 | Three Arm Parking Lot Light | 3 |
| 4498 | Gumdrop Sidewalk Light | 1 |
| 4499 | Dual Gumdrop Sidewalk Light | 2 |

All 95 fixture patterns and yields follow the reviewed source, except the fifteen pier/path/stone fixtures (4480\u20134494): the reviewed source defines no crafting recipes for them, so this batch adds no recipe changes for that family. Recipe ingredients
use the existing native equivalents: Classic Lamp Socket → Lamp Socket A,
Spot Lamp Socket → Lamp Socket B Projector, and the source copper ingot → vanilla
copper. The Standing Drum Floor Lamp's source base, the Ceramic Drum Table Lamp,
is unregistered, so its paid shapeless recipe uses the Basic Lamp Holder plus an
iron ingot instead. The five luxury chandeliers craft from their registered foyer chandelier bases plus the source material: Grand Halo Ring from Modern Sputnik plus gold, Grand Crystal Cascade from Crystal Tier plus diamond, Sculptural Branch from Modern Sputnik plus copper, Spiral Rain from Crystal Tier plus an iron chain, and Grand Empire from Candle Arm plus gold. The seven window/heavy/offshore fixtures craft from their registered bases plus source materials: Window Candle Light from the Basic Lamp Holder plus honeycomb, Armored Oval Bulkhead Light from the Robust Lamp Socket plus iron bars, Industrial Wall Pack Light from the Robust Lamp Socket plus an iron ingot (the source crafts it from the unported Warehouse Wall Floodlight, so this stand-in base is documented), Rugged Outdoor Canopy Light from the Robust Lamp Socket plus an iron block, Industrial Yard Area Light from the Gooseneck Barn Light plus an iron ingot, Offshore Caged Wellglass Light from the Robust Lamp Socket plus yellow dye, and Offshore Sealed Linear Light from the Thin Fluorescent Shop Light plus yellow dye. The eight seasonal fixtures craft from their registered strip or holder bases plus source seasonals: Warm White Christmas Fairy Lights, Multicolor Christmas String Lights, Christmas Icicle Lights and Halloween Ghost String Lights from the Flexible Neon LED Strip plus yellow, red, light blue or white dye respectively, Halloween Pumpkin String Lights from the Flexible Neon LED Strip plus a pumpkin, Christmas Star Wall Light from the Basic Lamp Holder plus a gold nugget, Halloween Bat Wall Light from the Basic Lamp Holder plus black dye, and Jack O Lantern Table Lamp from the Basic Lamp Holder plus a carved pumpkin. The six party/deck fixtures craft from their registered bases plus source materials: Patio Festoon Party Lights from the Flexible Neon LED Strip plus orange dye, Electric Poolside Tiki Light from the Traditional Lantern Post Light plus bamboo, Poolside Globe String Lights from the Flexible Neon LED Strip plus a glass bottle, Poolside Party Lantern from the Porch Lantern Sconce plus light blue dye, Deck Post Cap Light from the Basic Lamp Holder plus an oak slab, and Deck Step Riser Light from the Flat Lamp Socket plus a spruce slab. The five desk/dot/table fixtures craft from their registered bases plus source materials: Household Desk Lamp from the Lamp Socket A plus white wool and an iron plate, Mini Dot Spotlight (two) from the Lamp Socket B Projector plus an iron ingot and glass panes, Ceramic Drum Table Lamp from the Basic Lamp Holder plus white wool and terracotta, Green Banker Table Lamp from the Basic Lamp Holder plus green stained glass and a copper ingot, and Articulated Task Table Lamp from the Lamp Socket B Projector plus an iron ingot and an iron plate. Existing native recipes keep their outputs.
The nine warehouse/stadium/offshore/area/mini-dot fixtures craft from their registered bases: the
Warehouse Wall Floodlight from an iron ingot, an iron plate, glass panes and the Lamp Socket B
Projector; the Heavy Duty Stadium Wall Floodlight, the Offshore Platform Floodlight and the Large
Area Floodlight from the Warehouse Wall Floodlight plus an iron block, yellow dye or a diamond;
and the five mini dot lights from the Mini Dot Spotlight plus an iron plate and an iron nugget, a
gold nugget, a lever, black dye or a glass pane.

The eight parking/street/eave/sidewalk fixtures craft from their registered bases: the
Twin Parking Lot Light (base for the three parking upgrades; the source Spot Lamp Socket
maps to the Lamp Socket B Projector) from the Robust Lamp Socket plus two iron ingots and
an iron plate; the Modern LED Street Light from the Street Light plus two iron ingots and
an iron plate; the Boulevard Lantern Street Light from the Traditional Lantern Post Light
plus two iron chains and an iron plate; the Quad Head Parking Lot Light from the Twin
Parking Lot Light plus an iron block and an iron plate; the Round Canopy Parking Lot Light
from the Twin Parking Lot Light plus two quartz and an iron plate; the Offset Twin Arm
Parking Lot Light from the Twin Parking Lot Light plus two copper ingots and an iron plate;
the Adjustable Gimbal Recessed Light (two) from the Lamp Socket B Projector plus four iron
ingots; and the Twin Adjustable Eave Floodlight from the Mini Bezel Dot Spotlight plus an
iron plate. The source defines no crafting recipes for the Shoebox Parking Lot Light, the
Twin Wide Area Parking Lot Light, the Three Arm Parking Lot Light or either Gumdrop
sidewalk light, so these five remain uncraftable here to match the source.

The source LED lookups 293/294 conflict with the established 50/200 V bulbs.
Explicit mapping moves the new 120/240 V LEDs to **302/303**, preserving both old
identities and all their data. Their original three-glass/three-silicon/cable
patterns already craft the old bulbs. The new rated patterns add **two Iron
Plates**, keeping those materials and the two-bulb yield without creating an
ambiguous recipe. This avoids converting used bulbs into fresh ones.

## Remaining lighting work

The other **13 decorative styles** remain separate work:
the ceiling fans 4388–4391, the Tropical Patio Ceiling Fan 4408 and the
Adjustable Suspended Warehouse Light 4409, the pool lights 4396–4400, the
Multicolor Party Wash 4403 and the Outdoor Motion Floodlight 4126, plus motion
detection, suspended-warehouse adjustment and portable table/carry adapters. Partial-block mounting and source-world conversion
need their own adapters. The rest of the source bulb technologies and their
per-technology lifetime/configuration controls are not registered by this batch.
The original native golden references, assertions, tolerances and skips are retained.
The catalog fixture setup now raises hanging models enough to keep their downward
clearance cells above the test floor; no model is omitted.

[Fresh client gallery](evidence/decorative-lighting/fresh-client-gallery.png),
[shop light](evidence/decorative-lighting/shop-light-close.png),
[saved channel](evidence/decorative-lighting/post-saved-channel.png) and
[five installed bulbs](evidence/decorative-lighting/five-bulb-chandelier.png)
show the software-OpenGL exhibit. A dedicated server cold-loads the same four
owners; a real network client retains the five-bulb stack and changes the post
channel to `Lighting network`. All saved stacks/condition, orientation and power
modes remain exact. See [saved-owner checks](decorative-lighting-saved-owners.json).
Simultaneous multi-client behavior remains unverified.
The exhibit is unpowered; delivered power and beam rules
are checked by the production/native regressions.

Validation and any runtime limitations are recorded in
[the evidence](decorative-lighting-validation.json). Portable carry and the
[broader backlog](remaining-work.md) remain unfinished.

## Production checks

The full build passes **690 of 693 root tests**, retaining the same three optional
preview skips, and all **15 CAD tests**. The full default 200-case fluid corpus
remains included. All **74 new native lighting cases** pass. Unfiltered native
selects **1,387 tests: 1,385 pass**, with no skips; all **921 ForkPatch cases** pass.
Both whole-catalog publication/drawing checks retain all **575 devices**; the item
drawing check covers all **364 items**.
Repeated data generation writes zero files. All **680 provenance entries** pass;
all 768 preceding identities and 1,091 English entries keep their exact values.
There are 13 new identities and 13 new English entries in this batch.

Only the historical `golden_descriptors` and `golden_slots` tests fail. The original
expectations are unchanged; the two new bulbs naturally expand acceptance in ten
existing socket menus.
The [exact delta](decorative-lighting-golden-delta.json) records **685 failed
assertions**; the 537 pre-batch failures, the twelve added by the six
4353–4358 styles, the ten added by the five luxury chandeliers, the fourteen
added by the seven 4352, 4365–4370 styles, the sixteen added by the eight
4374–4381 seasonal styles, the twelve added by the six 4401, 4402,
4404–4407 party/deck fixtures and the thirty added by the fifteen
4480–4494 pier/path/stone fixtures and the ten added by the five
4118, 4121, 4137–4139 desk/dot/table fixtures and the eighteen
added by the nine 4117, 4364, 4371, 4382–4386, 4392
warehouse/flood/dot fixtures and the twenty-six added by the thirteen
4116, 4142, 4372–4373, 4387, 4393–4395, 4495–4499
parking/street/eave/sidewalk fixtures all remain exact. The matching slot summary
remains 112/137. No
tolerance, assertion or skip was relaxed.

The clean development JAR at code commit `3451e318d78c` matches all **1,774 Java
classes, 933 Kotlin classes, one Kotlin module file and 5,215 processed resources**.
All tested class bytes remain exact; only the build marker changes from the tested
working tree to the clean code commit. SHA-256: `2fc8759d0e12aa390518e1d3e0affc28c3313c1223b25ef90423ba0eb33936ba`.
This artifact predates the 4352, 4353–4363, 4365–4370, 4374–4381,
4401, 4402, 4404–4407, 4480–4494, 4118, 4121, 4137–4139 and 4117, 4364, 4371, 4382–4386, 4392,
4116, 4142, 4372–4373, 4387, 4393–4395, 4495–4499 batches; extending the parity record to the current
branch HEAD would require a fresh clean build.
