# Native surface outlets and fixed-machine cords

Nine source connector ratings retain their source lookups and passive behavior:
120/240 V household, 24/48/200/480/800/3200 V machine/industrial, and the distinct
240 V 30 A dryer connector. Ten native outlet/strip devices and nine cord items
use native registration, recipes, menus, client factories and source meshes.
The Experimental Wall Outlet identity remains unchanged; its English display and
lookup alias are 120 V Wall Outlet. All preceding native identities remain intact.

Use a matching cord on a source outlet, then on a compatible fixed machine.
Successful survival attachment consumes exactly one actual held cord; selection
alone consumes nothing. Sneak-use a cord on the consumer to unplug it, or use the
source's socket controls. Refunds use the source rating's cord item, enter the
player inventory when possible and drop actual remaining items when full.
Repeated removal cannot refund the same owner twice. Selection edits preserve
unrelated data on the remaining held stack. Creative connections are unpaid and
do not refund a survival resource.

Each cord is one passive resistor with resistance .08 + .015 times its physical
length in blocks, with an eight-block reach. Contacts add .005 ohms. There is no
ideal voltage conversion, local outlet breaker or invented power/current source.
Branch protection remains upstream. Real voltage sag and link/contact losses
reach the existing native device process. Polls never pay work or change stable
solver topology. Unplug restores the original native cable inlet.

The [extended consumer continuation](extended-consumers.md) adds explicit native
cooler, transmitter, turret and incubator selection; its mod build and fifteen
new cases await dependency network access. Prior passing evidence below covers
the initial consumers only.

Initial fixed consumer support is explicit: Fabricator, all three wire machines,
legacy electrical machines, electrical furnace with installed rated heating
hardware, and native battery chargers. Their existing inventories, menus,
mechanical/thermal behavior and accepted-step work processes remain authoritative.
Only matching existing voltage classes accept a cord: a native 50 V machine does
not accept the source's 48 V connector. Generators, storage outputs and unrelated
signal loads do not acquire an appliance inlet. The old native Lamp Supply has
no explicit mains rating; cord inputs for newer source mains-supply variants
remain a separate adapter rather than assigning it an invented voltage class.

Corded inputs are isolated in both normal node wrappers and native voxel
end/pin construction, including explicit edge pins, without suppressing other
signal/thermal/bus paths. Wireless outlet mode bridges the native lamp supply's
actual load through a passive .08-ohm link. It does not also report equivalent
lamp demand. Only loaded, added, nonburnt sources in range and the same dimension
qualify. Teardown removes bridges before their source loads leave simulation. Explicit
wireless refresh also refuses an inactive outlet load; normal native resume
restores its bridge to the real supply.
The default channel is the pinned source and native legacy `Default channel`;
explicit saved custom channel names remain intact. Source generalized breaker-panel
channels remain part of the pending panel port.

Sources own UUIDs and two/six saved token reservations. Live physical occupancy
and logical reservation are separate. A bounded weak reference identifies a
resident owner across native reconnects without retaining unloaded instances.
Only the matching token can reclaim a saved reservation. Duplicate live owners
cannot steal or refund the original owner's token. Clearing a socket while its
consumer is unloaded prevents a later silent reclaim. Resident source replacement
releases/refunds once; temporary source-owner absence disconnects power and waits
without loading chunks or manufacturing a refund. Strip cycles are rejected by a
resident-only upstream walk with a 64-source visit limit. A suspended consumer
keeps its paid logical reservation while all physical polls refuse to reconnect
it; ordinary native resume reclaims the same reservation once.

World-only cord data retains source position/UUID/rating/socket/token and paid
state. Unknown ratings, missing token identity and invalid slots/facing fail
closed. Ordinary picked/configuration items carry no link or reservation ledger.
The last connection-status label survives a native snapshot; only a real physical
poll can restore the conductor. Surface outlet geometry uses front/horizontal
block state, so collision workers do not request block entities or chunks.

Source body and plug parts draw through the native mesh pipeline. Cord paths use
bounded terrain samples and a cached joined mesh; draws cannot advance machine
work or ownership. Bounds include their physical endpoint. Native controls expose
mode, channel, horizontal mounting and individual/all-socket release. Horizontal
rotation immediately republishes resident linked endpoints without paying work or
changing stable LU. Render teardown releases the cached mesh. This is
headless publication/geometry evidence; interactive graphics and multiplayer
remain unverified.

This checkpoint implements surface-mounted outlets and fixed equipment. Recessed
wall replacement/rendering/restoration, portable-appliance carry, table-lamp and
other appliance consumers, and compact four-cell/four-socket UPS remain pending.
The compact UPS registration guard stays in place. Source-world conversion and
historical 1.12 golden reconciliation are also unfinished. See the
[source and native integration review](appliance-cord-review.md),
[validation](cord-outlets-validation.json) and [remaining work](remaining-work.md).
