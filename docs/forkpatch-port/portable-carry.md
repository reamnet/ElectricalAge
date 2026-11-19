# Portable carrying

Implemented after `fbbc6552473cfc72195379345464b8db5b9c2424`.
The962-case ForkPatch regression passes; live client checks and the final
unfiltered rerun are still pending.

The pinned source's `ApplianceCord.carry` and `CordInteraction` define the transfer.
Among currently available native families, source table-lamp styles and the compact
UPS are portable. Fixed fixtures, full-size UPS cabinets and ordinary machines
remain fixed. Kitchen, climate, standalone fan and festive-candle consumers can
use the same policy when their separate adapters exist.

Sneak-use a plugged portable appliance with an empty main hand to pick it up.
For a socket source, the same click first unplugs an occupied downstream socket;
a source with free outputs can then be carried. The complete owner NBT is stored
in `carriedAppliance`, with `carriedAt` identifying the pickup game tick. The held
item keeps the upstream cord token, paid flag and anchor. The source draws the
loose lead toward the holder while it is visible and within the source visual's
bounded reach. There is no electrical conductor or battery clock on the held item.

Before snapshotting a portable source, downstream leads are released and refunded
through their existing ownership checks. Moving suppresses ordinary appliance,
inventory and upstream cord drops/refunds from the removed owner. Both six-node
(including voxel slots) and transparent-node placement restore the complete
snapshot before initialization, while preserving the new placement orientation.
Normal cord polling then applies native reach, rating, dimension and resident
ownership checks. A successful placement consumes the snapshot even in creative
mode, because repeatedly placing the same stateful inventory would copy its cells.

`ForkPatchPortableCarryCases` exercises actual pickup/removal/replacement of a
paid desk lamp, exact compact UPS cell energy and downstream release, and empty
hand/unplugged/fixed-device guards. These cases also check that stale removed-owner
callbacks cannot detach or refund the restored owner's lead and that a final
unplug refunds one lead. All three pass in `/tmp/eln-requested-native-p-v3.xml`, part of a 51/51
native test run, and again in the subsequent962/962 ForkPatch regression. This establishes server-side transactions; live client checks
and the broader regression run remain separate gates.

A fourth native carry case preserves another cable part when the last appliance
is lifted from a voxel block. All four carry cases and panel regressions pass
in the58-case follow-up `/tmp/eln-requested-panel-p-v6.xml`.
