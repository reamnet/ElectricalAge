# Appliance cord and outlet adapter review

Reviewed source `b7444a7b8a0c5e072b1fd35eed2b74a282c3348c` after the native wire
manufacturing adapter. This is the next implementation contract; the equipment
below remains unregistered and has no native runtime validation.

| Source lookup | Device/item | Nominal voltage / current |
| --- | --- | --- |
| transparent 7968 | Experimental Wall Outlet (display: 120 V Wall Outlet) | 120 V / 20 A, two sockets |
| transparent 7969 | 240 V Wall Outlet | 240 V / 20 A, two sockets |
| transparent 7991 | Six Socket Power Strip | 120 V / 20 A, six sockets |
| transparent 8000–8005 | Machine/workshop/industrial outlets | 24, 48, 200, 480, 800, 3200 V; first three 32 A, others 125 A |
| transparent 8012 | 240 V 30 A Dryer Outlet | 240 V / 30 A |
| shared 7874/7875 | Flexible /240 V Appliance Cord | 120/240 V |
| shared 8000–8005 | Machine/workshop/industrial cords | same six industrial ratings |
| shared 8006 | 240 V 30 A Dryer Cord | 240 V / 30 A |
| transparent 8076 | Four Cell Plug-in UPS | separate compact battery-backed socket source |

The 19 cord/outlet slots above are free in the current 652-entry native manifest;
recheck before registration. Shared and transparent lookup spaces are separate.
The compact UPS must be checked independently against the native UPS identities
and physical cell inventory. Native programmable UPS 7947 must remain unchanged.

Source files under `transparentnode/cord` separate rating, physical attachment,
interaction, source reservations, consumer adapters, recessed-wall wiring and
client rendering. `MachineCordSupport` explicitly selects consumer families and
their original power inlet; it must not make generators/storage outputs or signal
loads into appliance inputs. Wire manufacturing uses a 200 V inlet. Native device
menus, shafts, fluids and solver components remain authoritative.

A cord is a passive resistor between the actual source and appliance loads, with
resistance `.08 + .015 * distance` ohms and an eight-block reach. Attachment changes
create/remove its one component; a half-second physical poll handles residency,
source identity and state. Polling/mesh draws must never repeatedly rebuild a
stable circuit. A plugged consumer must not simultaneously retain an accidental
surface-cable or wireless source for the same input. Some bipolar consumers need
a matching return conductor; preserve their real inlet topology.

A source owns a UUID, two/six indexed socket reservations, revoked tokens and
live attached instances. A cord owns its distinct token, selected source UUID,
rating/socket and paid flag. Saving/unloading/reconnecting must preserve logical
ownership without duplicate passive links, duplicate claims or refunds. Distinct
loaded instances with the same saved token cannot both claim one socket. Reject
loops through strips and battery-backed sources. World/dimension/source UUID and
rating checks matter even when the old position contains a new valid outlet.

Native lookups must remain resident-only. The source uses world-position node
queries and `hasChunkAt`; adapt them to native loaded-node/chunk contracts. A
missing unloaded source should disconnect physical power and wait, while an
actually removed/replaced resident source releases/refunds exactly once. Do not
force-load its chunk or treat temporary unload as a second refundable removal.

The source deliberately has no local outlet breaker: its contact is passive and
branch overcurrent protection belongs upstream. Its retained `breaker` accessor
and `tripped=false` are compatibility details, not a reason to invent local
protection or an ideal voltage supply. Wireless mode needs the existing native
wireless channel power accounting; never charge both that route and the cord.

Recessed outlets share a saved wall block state, custom support/restoration and
front-face cable endpoints. An appliance immediately in front must still need
its plug. Surface cable reconnection works from both ends and avoids duplicate
connections. Native wall rendering/picking and voxel endpoints differ from the
source; copying the old terrain/render hooks is insufficient. Keep regular
surface-mount and horizontal/strip geometry consistent with socket anchor tips.

Source carried appliances put an entire world snapshot under `carriedAppliance`.
The native adapter must distinguish an actual consumed physical carry from ordinary
picked/configuration items. A carry cannot clone inventory, stored battery/heat,
paid cords, downstream socket reservations or process work. Clear downstream
attachments before taking the source snapshot; cancellation/failure must leave
one owner. Existing native EV cells, UPS energy and wire-machine paid buffers
already have ownership rules that this path must preserve.

Required production cases: real sag/loss and current through cords; same-voltage
but incompatible connector variants; all ratings; socket capacity/token races;
strip/UPS loops; resident replacement versus unload/reload; actual survival
attach/unplug/break/carry and aborted operations; mixed dimension/reach guards;
recessed support removal and ordinary wall restoration; native cable isolation;
wireless/cord exclusion; real client publication, cached cord paths, plug meshes,
menus, recipes and multiplayer. Interactive/GPU checks remain separate from
headless publication/mesh checks.
