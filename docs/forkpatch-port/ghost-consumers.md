# Miner and transporter cord inputs

Continuation of PR #1 from `ce694aa3fa3e8a91fdbcb60c0f0262ab2a1a1b37`.
The ForkPatch 3.0.77 source remains pinned at
`b7444a7b8a0c5e072b1fd35eed2b74a282c3348c`.

## Native adapter

Auto Miner 2688 and Experimental Transporter 4160 now select their original
800 V power loads for physical cords. Their existing inventory, processing,
menus, save identities and voltage watchdogs remain authoritative. There are
no new registry entries or replacement machine implementations.

Both machines expose additional `GhostNode` power terminals. Those terminals
now use the same cord mask/load gate as the owner. Merely masking future load
queries would leave existing cable connections alive: the owner's native
connection lifecycle therefore disconnects its auxiliary nodes before removing
the machine loads and rebuilds their connections after ordinary native resume.
Attach/unplug uses that lifecycle; cord polls never invoke it. Inactive,
nonresident and burnt owners expose no auxiliary power inlet.

Reverse power still invokes the existing polar-device watchdog and burns the
machine in place. Cord polling also refuses burnt transparent/six-node owners,
so a later physical poll cannot restore a conductor to a load removed by native
failure handling. Its paid socket reservation remains available for one refund.

## Regression cases

Eight new `forkpatch_cord_ghost_*` GameTests cover both machine families:

- An actual adjacent powered HV cable is connected before attachment. A paid
  800 V cord isolates that competing 600 V feed and supplies the original load.
- Two thousand unchanged polls preserve cached LU factors and ghost isolation.
- Unplug rebuilds native cable connections; a separately solved case verifies
  that the original load receives the restored 600 V feed.
- Reverse polarity retains native burnout; subsequent polls cannot reactivate
  the machine, and repeated unplug returns exactly one paid cord.
- Owner suspension, explicit ghost reconnect attempts, ordinary resume, actual
  node reload, fresh terminal ownership and source-initiated refunds retain one
  paid token and never reconnect an inactive owner.

Fixtures preserve shared fake-player facing and account for the transporter's
rotated four-block item placement offset. They use the normal registered item
placement path and leave native electrical assertions enabled.

## Session record — 2026-11-13

1. Located existing PR #1 and its branch; verified GitHub authentication as
   `reamnet` and continued the exact PR tip.
2. Restored dependency access with the prepared cloud toolchains and compiled
   the previously blocked cooler/transmitter/turret/incubator checkpoint.
3. Compared source machine-consumer selection with native ghost power nodes;
   added owner lifecycle isolation and explicit miner/transporter inlets.
4. Added native competing-source, cached-LU, reload and refund cases. Initial
   failures identified fixture placement and retained polar watchdog behavior;
   the reverse-polarity check also exposed the burnt-owner polling gap, which
   is now guarded without changing the watchdog.
5. Validation commands and final outcomes are recorded in
   [continuation-validation.json](continuation-validation.json).

## Completed validation

- Final `:build :cad:test`: 670 root tests passed, three optional previews
  skipped (673 selected), and all 15 CAD tests passed; the default 200-case
  fluid corpus remains enabled.
- Focused cords: all 75 cases passed, including the preceding fifteen extended
  consumers and the eight new auxiliary-terminal cases.
- Unfiltered native: 1,144 of 1,146 cases passed. All 680 ForkPatch port cases
  passed. Only historical `golden_descriptors` and `golden_slots` failed; all
  467 existing failed golden assertions remain visible.
- Provenance: all 531 entries pass. JAR bytes match 2,610 primary compiled
  files and 4,951 processed resources. Source checkout, registry/resource files,
  Gradle pins, historical expectations and existing skip lists are unchanged.

## Remaining scope

This adds cord connectivity to existing native machines, not full behavioral
parity for every source mining or transport change. Motors need their bipolar
return topology. Recessed outlets, portable appliances, compact UPS, remaining
families, source-world conversion and interactive client/multiplayer work remain
in [the backlog](remaining-work.md). Historical golden expectations remain
unchanged; this is a development checkpoint, not a complete port release.
