# Wire Snips

Source shared-item lookup **7873** is registered as native `wire_snips`. Right-click
in either hand opens its own native menu and screen. The source iron/rubber recipe
is generated and checked through the server recipe manager. No previous native
item identity is replaced.

The menu has one filtered input slot and the player's inventory. Choose a whole
number of metres from1 through999999, smaller than the available paid length.
The original spool keeps the fractional remainder and its ledger identity; the
cut spool receives the requested metres and a fresh ledger identity. Both retain
temperature and other wire metadata. Cutting therefore cannot turn hot winding
wire into a new cold default spool, refill a partial spool, change its conductor
count/material, or repair damaged stock. Intact multicore and existing damaged
utility wire retain their respective item types and paid metal mass.

Server-side checks validate the owning player, the bound tool, encoded action,
input type/count and available length. Moving or dropping the tool revokes cut
authority. Nonfinite/fractional/overflowing input is rejected instead of silently
rounding it. The source action range is retained, with only the single input slot
accepted. Shift transfers use real slots and leave nonwire items alone. The held
tool slot is protected while the menu is open.

The native server-only inventory return helper delivers cut pieces and closing
remainders to the inventory or world when full. Closing clears the input before
returning it, so repeated removal cannot refund twice. The native screen uses
container-button packets and keeps text-entry keys from operating the inventory.

See [validation](wire-snips-validation.json). The headless cases exercise actual
item use, native server menus/buttons, transfers, owner/length guards, full-inventory
world drops, hot fractional spools, damaged/multicore stock and real crafting.
Interactive screen/GPU and multiplayer validation remain pending. Other wire tools,
manufacturing machines, spool/jacket production and full source-world conversion
are separate remaining work.
