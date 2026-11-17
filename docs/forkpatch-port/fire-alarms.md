# Fire alarms

All seven pinned source devices are registered at transparent-node lookups
8053–8059: control panel, pull station, horn strobe, bell, strobe, smoke detector
and heat detector. Their21 original OBJ/MTL files and three alarm OGG sounds are
byte-identical copies. The old generic alarm remains available.

The120V control panel has24Wh (86400J) of rechargeable backup, starting empty.
Accepted physical steps pay its5W controller draw, linked field-device demand
and5W backup charging. Fields use a panel lease or their own18–30V supply;
owned/self-powered fields are not charged again to another panel. Channels use
world identity,128m range and50-tick leases. Owner removal clears leases, and
empty channel histories are removed rather than retained forever.

A real pull, detector or signal input latches the panel. Reset refuses an active
source; silence stops audible output while strobes keep flashing. Source test,
forget-missing, threshold, radius, labels, channel and enable controls use their
original event IDs and saved keys. The source300×382 screens retain player
inventory at `(8,300)` and source control coordinates. Plain empty-hand click
operates a pull station; sneak-click opens setup. The menu catalog uses that
actual gesture and retains every existing menu-open/rebuild assertion.

Smoke follows a bounded path through connected air, stopping at full walls and
fluids. Heat detects actual nearby fire, lava, magma and lit campfires through
short-range radiation. Every environment query checks an already-loaded chunk.
The separate source room-climate system is not ported: its ambient-temperature
contribution remains pending; current heat behavior uses the source20C fallback
plus those real radiant sources.

Fixtures have nonblocking collisions and model-oriented click outlines in all
six orientations. Native OBJ metadata initializes negative-only maxima to zero;
fixture depths are therefore taken from the original meshes' actual extents.
The geometry tests preserve a strict thin-outline check instead of accepting an
oversized box. Rendering preserves source colors, pull handles, LED states and
synchronized flashes; source horn/bell loops and trouble chirps are registered.

Validation: all13 focused alarm cases pass, and all976 ForkPatch native cases
pass with no skips. Root698/701 pass with the same3 optional preview skips;
CAD15/15 pass. Datagen repeats with zero writes;1085 provenance entries pass.
The three golden tests execute: recipes pass, while the two historical
comparisons retain all746 previous exact differences and add15 alarm identity/
menu assertions (761 total). Expectations, tolerances and skips are unchanged.
See [evidence](fire-alarm-validation.json) and [golden delta](fire-alarm-golden-delta.json).

Live client, hardware audio/GPU and multi-client validation are separate pending
checks. `/elnlab alarms <pos>` prepares a normal saved-source alarm scene;
`/elnlab devicemenu <lookup>` opens its nearest resident setup screen from a
player context. Use a private test world.
