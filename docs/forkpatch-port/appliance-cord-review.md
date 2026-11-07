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

The 19 cord/outlet slots above are free in the current 658-entry native manifest;
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

The native Fabricator adapter now exposes its original 200 V input as `power` and
keeps its ZN/Down surface terminal fixed. Its ordinary item/configuration payload
contains only `operation`; paid work, inputs and the deterministic yield seed/
attempt counter stay world-owned. Source `MachineCordDevice` keeps the default `cordPortable=false`, so the
Fabricator remains fixed equipment when connected. If a later feature explicitly
adds actual carry, it must transfer those fields through one consumed physical
snapshot, without separately dropping inputs or refunding the paid cord. Copy/pick helpers must continue using the
restricted settings payload. The shared native electrical-machine process now
accumulates work in its accepted electrical callback; cord attachment must retain
that callback and isolate only the original input, preserving recipe/booster
logic and native cached LU behavior.

Before implementation, keep decoding separate from physical claims. Source
`CordRating.read` defaults an unknown name to 120 V, while `CordAnchor.read`
clamps socket indices to 0–5. Native decoding should reject an unknown rating or
out-of-range socket instead of turning malformed state into another valid
connector. The outlet's actual capacity is authoritative. Preserve an explicit
source-save conversion path separately from validation of new native state.

Source `cordWouldLoop` walks upstream anchors until it reaches a disconnected
source or repeated UUID. Adapt that walk to resident-only native owner lookups
with a documented visit budget; budget exhaustion must reject an attachment.
A saved reservation token and a live physical claimant are different: unload
removes the conductor/live claim while retaining the reservation, and permanent
removal releases/refunds once. Repeated claim/poll/read paths must not advance
Fabricator or inherited machine work. These are reviewed implementation
requirements; no outlet/cord adapter or native test coverage is claimed yet.

Concrete native inlet review for the first consumer adapters:

| Native consumer | Existing load / surface port | Adapter constraint |
| --- | --- | --- |
| `fabricator.FabricatorElement` | `power`, 200 V, global ZN/Down | Keep fixed source orientation and accepted-work callback; copy settings only through its restricted payload. |
| `wiremachine.WireMachineElement` | `power`, 200 V, bottom-edge ports | Keep paid buffers and real break/place ownership; source machine-cord adapters remain nonportable. |
| `electricalmachine.ElectricalMachineElement` | private `electricalLoad`, descriptor voltage and `powerLrdu` mask | Add a narrow inlet adapter rather than replacing its load/process/recipes; preserve the accepted-step callback added here. |
| `electricalfurnace.ElectricalFurnaceElement` | package-local `electricalLoad`, back/Down mask | Read voltage from the installed native heating element; missing/invalid hardware must reject a cord. |
| `batterycharger.BatteryChargerElement` | public `powerLoad`, native power on every edge | Retain this destination improvement when unplugged; suppress every native inlet edge when plugged. |
| `lampsupply.LampSupplyElement` | public `powerLoad`, front LRDU | Keep native channel accounting and prevent simultaneous alternate power inputs. |

Both connection masks and load lookup paths need the same plugged-input policy;
several native getters return the load more broadly than their masks. Preserve
signal, thermal, shaft and fluid connections. This table records inspected native
entry points, not permission to make fields public or select the first arbitrary
load on every device. Motors, other consumers and newer native-only equipment
still need their own explicit inlet/topology review.

## Native voxel and carry integration points

Porting only the source `SixNode` wrappers would leave native voxel inputs
connected. `VoxelNode.ends()` and `VoxelNode.pins()` read hosted elements' loads
and masks directly; explicit `edgePins()` are another path. The ordinary
`SixNode` same-block connection helpers also access elements directly. Review
all of those paths when suppressing a plugged consumer's original power inlet.
Filter only the selected power load and its power bits; preserve signal, thermal,
bus and other pins, native voxel geometry, offset slots and cable-net ownership.
Native tests must include both same-block and adjacent-block voxel cables and
offset hosted consumers, with the original links restored after unplugging.

Source carry removes a six-node device by `owner.side`. Native six-node devices
also occupy offset slots beyond the six faces, so carry must address the actual
`slot` through `SixNode.deleteSlot`, with a dedicated carry policy for its
`destroy` path. Deleting by face can select another device. Let the native voxel
`afterSubBlockDeleted` path decide whether the host block is empty; remaining
cables, pipes, carved hosts and other devices must survive.

Native `NodeBlockEntity.setRemoved` calls ordinary break only when the node is
not already destructing. `NodeBase.physicalSelfDestruction` marks it destructing
before removal and drops nothing, whereas an ordinary transparent break drops
inventory and the device. These paths are distinct; a future carry must preserve
their ownership rules and explicitly account for ghosts, actual removal failure
and cancellation. Setting a source-style `moving` flag alone does not add native
carry support. A full snapshot cannot pass through the ordinary configuration or
picked-item payload, and native subclasses that restrict `readItemStackNBT` need
an explicit actual-owner restore path rather than an unrestricted base-class
snapshot import.

`ItemNbt.get` returns a copy of native `CUSTOM_DATA`. Selection edits must be
written back through `ItemNbt.edit`/`set`; remove only the cord's selection key
when consumed instead of clearing unrelated custom data/components. The existing
`GenericItemUsingDamageDescriptor.onItemUse` is a native entry point for held
cords. Empty-hand carry still needs a carefully ordered server interaction path;
source hit resolution and event cancellation are not native runtime evidence.

Portability is an explicit source policy, separate from electrical compatibility:
`MachineCordDevice` does not override `cordPortable=false`. Source kitchen devices
permit carry except the fridge, UPS permits it only in compact form, and lamp
sockets permit it only for a table-lamp fixture. Preserve those decisions through
the corresponding native adapters; generic machine-cord support does not imply
Fabricator, plate-machine or wire-machine carry support.

Additional explicit source carry policies are nonindustrial fans, only the
`PORTABLE`/`PATIO` climate types, and only the holiday-candle festive descriptor.
Six-socket strips do not override the default nonportable policy. These policies
must remain type/descriptor decisions rather than a voltage-based whitelist.

## Compact UPS boundary

Source `CompactUpsElement` is a four-cell, four-socket source at lookup 8076.
Each cell is independently active (`UpsInventory(4, 1)`), unlike the existing
native UPS's 48 cells in complete 24-cell racks. Socket ownership uses its own
`upsSocketId`, four `upsSocketN` reservations and `upsRevoked` tokens. The socket
rating follows the selected 120/240 V output. Occupied or reserved sockets block
output-voltage changes; direct bypass requires a matching input/output rating.
Carrying a compact UPS first disconnects downstream cords before taking its
snapshot, so those reservations are not resurrected after placement.

The native `UpsPowerUnit` already contains the source compact-mode parameters:
3 kW inverter budget, 120/240 V selection, input-rating-relative acceptance and
20 A charger input limit, plus `allowVoltageChange`/`allowBypass` callbacks. Its
accepted-step battery/thermal accounting and native conservative controllers
must remain authoritative. The existing `UpsElement` still constructs the full
rack inventory and noncompact unit; `UpsDescriptor` explicitly rejects compact
registration. Passing a new descriptor flag alone would therefore not implement
the compact appliance. Parameterize the owner/inventory/menu/render path only
with the corresponding native behavioral cases and preserve the existing full
UPS identities and defaults.

Required compact checks include one through four individually active physical
cells, mixed cell ratings, extraction/save/carry flush of cached cell energy,
actual charger current and voltage-sag response, backup/restore with real output
loads, voltage-control lock with both loaded and unloaded reserved consumers,
bypass without simultaneous inverter output, four socket claim/refund races,
upstream strip/UPS loop rejection, downstream release before actual carry and
restoration without duplicated cell charge or cord refunds. The charger and
inverter limits do not turn direct bypass into a regulated ideal power source.
No compact UPS registration or native compact-device runtime coverage is claimed
by this review.
