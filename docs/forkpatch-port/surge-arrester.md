# Pole Surge Arrester and paid pole kits

The standalone arrester preserves source lookup **8067 (126:3)** and registry
name `pole_surge_arrester`. Its original porcelain mesh, native icon/menu/factory
and source recipe are included. Supply enters on the left or bottom; front bottom
is inhibit and front top signals spent capacity. Nominal line voltage is
configurable; enable and configuration changes cannot replenish the cartridge.

A healthy shunt stays open. Above 130% nominal magnitude, the source resistive
shunt law draws 500 kW at nominal voltage, scaled with voltage squared. The
200 kJ cartridge is debited by actual accepted electrical intervals and then
fails open. As in the source, the final conducting interval is real dissipation:
it is retained even when it crosses the remaining-capacity threshold. This is
not an ideal instantaneous energy/current clamp, and a large surge can exceed
the threshold by that final interval. No converter probes or trial-time energy
debits are introduced; stable shunts retain native LU factors.

One paid arrester item can instead be installed on an overhead HV distribution
pole. Each primary phase has its own native shunt; the shared cartridge follows
the source all-phase activation/budget rule without tying phases together.
Secondary-only poles and underground cabinets do not accept the HV attachment.
Right-click installs one; duplicates consume nothing. Sneak with an empty hand
to remove a partly consumed kit. A spent kit can be replaced, returning the spent
physical item. Native server inventory prediction is used for refunds.

World NBT retains installation and remaining joules. Pole carried/control
snapshots deliberately omit paid installation state, and forged carried fields
cannot create it. Removal, replacement and actual block break return the correct
remaining capacity; a repeated break callback cannot refund twice. A removed kit
can be placed as a standalone arrester without refreshing its capacity. Spent and
invalid/nonfinite cartridge records fail open. Pole HUD/publication and the source
porcelain/lead geometry show the installed kit and its spent state.

The native entity-join listener searches existing loaded nodes in the strike's
own dimension. It chooses the nearest eligible protector within eight blocks
(pole kits use the primary-rack height), debits 40 kJ of **external** strike energy,
and marks the bolt visual-only. That debit does not turn on an additional shunt
load on a healthy grid. Five full strikes exhaust a fresh cartridge. A per-bolt
marker prevents a repeated join/handling path from consuming capacity twice.
The lookup never requests missing chunks. Disabled/inhibited standalone devices,
spent cartridges and protectors in another dimension cannot claim the strike.

SCADA can enable/disable the standalone arrester through its native endpoint;
those controls cannot refill a spent cartridge. Full source-world conversion,
interactive audio/GPU/multiplayer, tall-pole culling and broader source attachment
parity remain separate backlog work.

See [validation](surge-arrester-validation.json) for exact checks and counts.
