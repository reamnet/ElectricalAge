# Extended fixed-consumer cord checkpoint

Continuation of PR #1, **Port ForkPatch 3.0.77 devices, manufacturing and cords
to NeoForge 26.3**, from `3f54f0ca`. Source remains ForkPatch 3.0.77 at
`b7444a7b8a0c5e072b1fd35eed2b74a282c3348c`.

## Implemented, awaiting native validation

The source `MachineCordSupport` explicitly includes several existing native
consumer families not selected by the preceding 26.3 adapter. This checkpoint
adds the incubator, active thermal dissipator, transmitter antenna and turret
families to the native cord selector. Each exposes its actual existing inlet
and nominal voltage through narrow read-only methods. The transmitter's command
and signal loads cannot be mistaken for its power inlet.

| Native lookup | Consumer | Cord compatibility |
| --- | --- | --- |
| 4130 | 200 V active thermal dissipator | 200 V workshop |
| 450 | Medium power transmitter antenna | 200 V workshop |
| 452 | High power transmitter antenna | 800 V industrial |
| 4224 | Defence turret | Existing HV cable rating, currently 800 V |
| 2624 / 4128 / 448 | Incubator / small active cooler / low power transmitter | Existing 50 V rating; none of the nine source connectors match |

No new voltage class is invented: a 48 V cord still cannot power 50 V hardware.
Receivers, generators and storage outputs remain outside this consumer selector.
Native physics, inventory, cooling, antenna controls and turret state machines
remain authoritative. Existing cord hooks handle paid ownership, native save
round trips, inlet isolation, publication and release.

## Added regression coverage

Fifteen additional native cases are registered under `forkpatch_cord_*`:

- Four powered-port cases check the actual inlet, unchanged signal/thermal
  terminals, stable LU factors, unplug restoration and one refund. The active
  cooler also checks its original 60 W resistive fan load.
- Four real node reload cases check retained paid token, new load identity,
  source reservation reclaim and exactly one source-initiated refund.
- Four survival block-interaction cases check actual item consumption and
  repeated unplug refunds through the existing cord interaction path.
- Three 50 V cases reject all available connector ratings, including attempted
  48 V attachment without payment or reservation.

These cases are **written but not executed**. Both the root build and native
launcher stop at `:createMinecraftArtifacts`: the environment proxy returns
HTTP 403 for `maven.neoforged.net` when resolving NeoForm Runtime 2.0.31. Changed
mod code has not compiled. The independent CAD suite passes 15 tests; that does
not validate these adapters. Import provenance and whitespace checks pass.
See [the exact validation status and commands](extended-consumers-validation.json).

## Reviewed boundaries and next work

Recessed outlets require a native wall-state replacement/restoration contract,
replicated geometry and matching external cable endpoints. Surface outlets must
not be presented as recessed support just by moving their mesh.

The miner and transporter have additional `AutoMinerPowerNode` and
`TeleporterPowerNode` ghost inlets which directly return the owner's load.
Simply adding them to the selector would let those terminals bypass the cord
inlet isolation. Their connections must be disconnected/rebuilt together with
owner attach/unplug, suspend/resume and reload; add powered competing-source
cases before enabling those families. Motors additionally need their actual
bipolar return topology. These adapters are deliberately still pending.

Portable appliances/carry, rated lamp-supply variants, compact UPS and source
world conversion remain pending as recorded in the feature backlog. Historical
golden failures have not been waived or regenerated.

## Session record — 2026-11-13

1. Located the existing remote port branch and PR #1; continued the exact branch
   tip instead of starting from the main-branch checkout.
2. Compared source consumer selection against native power and control loads;
   inspected native save hooks, wall placement and separate ghost power nodes.
3. Added explicit native consumer bindings and the fifteen regression cases.
4. Attempted root build and native cord suite; recorded dependency-policy failures
   separately from application failures. Ran the independent CAD tests.
5. Extended the source import ledger and recorded current evidence separately
   from the preceding checkpoint's successful full regressions.

The environment draft contains dependency-host additions and `api.github.com`
for GitHub CLI PR editing. They must be applied in environment settings before
retrying blocked operations. Git HTTPS transport remains usable. No credentials
are needed for the public dependency downloads. This checkpoint is development
work for review, not a validated release.
