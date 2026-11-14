# Four Cell Plug-in UPS continuation

Pinned source: `b7444a7b8a0c5e072b1fd35eed2b74a282c3348c` in
`reamnet/ELN1.21.1ForkPatch`. This continuation follows the recessed outlet
checkpoint `e8e635ba0af245c1a4dffefa599e2367a296567d` on the same port branch.

## Device and operation

Lookup **8076**, `eln:four_cell_plug_in_ups`, uses the original compact UPS mesh
and source crafting pattern. Place it on a floor. Insert one to four compatible
portable batteries or UPS cells; each slot contributes its own actual capacity,
charge limit and discharge limit independently. The existing floor/wall UPS
identities still use complete 24-cell racks and retain their 48-slot menus.

Select 120 V or 240 V output. The inverter supplies at most **3 kW total**, subject
to the installed cells' discharge limits. Charging accepts a matching 120 V or
240 V appliance cord and limits its charging stage to **20 A / 3 kW**. Real cord
and contact resistance cause voltage drop. Input outages use stored cell energy;
invalid input voltage stops the charger instead of creating replacement power.
The original two floor-plane cable terminals remain available. An input cord
isolates the alternate cable inlet; the output terminal shares the backed-up bus
with all four sockets.

To wire the input, use a matching cord on an upstream outlet, then on the UPS.
To wire an output, select the UPS with an output-rated cord, then use the cord on
a compatible consumer or strip. A saved upstream selection takes precedence over
source selection when attaching the UPS input. Sneak-use a cord on the UPS to
unplug its nearest occupied output. The menu provides a separate input unplug
button and four numbered output unplug buttons.

All four occupied **or reserved** output sockets lock the output voltage, including
consumers whose conductors are temporarily disconnected or whose owners are
unloaded. Unplug every output before changing rating. Manual bypass directly
passes matching input voltage through and disables the inverter. It provides no
regulation or battery backup; the 20 A limit belongs to the charger, not the
passive bypass contact. Upstream protection remains necessary as with other
native passive contacts.

## Completed implementation steps

1. Added the native `CordSocketSource` contract. Outlet and UPS sources expose
   their actual resident owner and real electrical output. Cord lookup remains
   bounded to loaded chunks and preserves pending claims without forcing loads.
2. Extended the owner walk through UPS and strip input cords. Self-links, cycles,
   duplicate resident tokens and exhaustion of the existing 64-source bound fail
   without reserving or consuming another cord. Temporarily absent upstream
   owners reject new links, while established downstream leases remain connected
   to a resident UPS so actual battery backup survives upstream chunk unload.
3. Added four durable UUID reservations with weak resident-owner references.
   Disconnect preserves reservations; explicit unplug/removal clears them and
   refunds each paid cord once. Stale source teardown cannot detach the restored
   resident owner's links or drop its inventory.
4. Parameterized the native UPS bank to four independently active items while
   retaining full UPS rack defaults. Existing accepted-step power controllers own
   inverter/charger transfer, cell debits and rejected-energy heat.
5. Added explicit cabinet endpoint encoding, packet data and four indexed plug
   positions. Invalid cabinet/surface/strip/recessed combinations and unsuitable
   connector classes are rejected. Surface and recessed geometry remain distinct.
6. Registered the source identity, recipe, enclosure and reproducible inventory
   icon; regenerated the native model index, item resources and English strings.
7. Added a compact 320×240 native menu with four real filtered cell slots,
   120/240 V selection, output/bypass controls and independent unplug actions.
8. Added native and solver regressions covering item energy, leases, reloads,
   physical output cords, actual survival use and conservative electrical steps.

## Validation and limitations

Current production, native, client and artifact results are recorded in
[compact-ups-validation.json](compact-ups-validation.json). Earlier evidence
files retain their historical results; no golden snapshots, tolerances or
existing skips are waived by this continuation.

Portable carry remains a separate unfinished adapter. Ordinary picked/dropped
settings never contain installed cells, source UUIDs, socket reservations or paid
cords. No source `upsRevoked` moving-owner ledger is imported: native moving
appliance ownership is not enabled. The four-cell device's ordinary native saves
use `upsSocketId` and `upsSocket0`–`upsSocket3`; general source-world conversion
remains separate. The full-size UPS has not been added to the cord whitelist.

## Upstream-owner unload regression

The first implementation exposed an inherited cord-walk problem: when an
upstream outlet owner was temporarily absent, the loop guard treated the
unresolved walk as a cycle and detached an established downstream link. For a
resident UPS this incorrectly discarded its battery-backed output cord.

`forkpatch_compact_ups_real_output_cord_strip_upstream_unload_backup` reproduces
the problem with real outlet → UPS → strip owners and a passive load. Against
commit `33113951` it loses the output lease and delivers **0 W**. The follow-up
keeps established downstream reservations when an upstream owner is pending,
while rejecting new attachments through the unresolved chain. Genuine cycles
and the 64-owner bound still fail closed. The final report records the rerun,
including actual battery debit, delivered work, native packet/mesh state and
unchanged cached LU matrices during repeated lease polls.
