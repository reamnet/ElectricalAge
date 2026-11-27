# Distribution span placement

Base: ddbbcd9f2 on local/awg-upstream-integration. Reference: ForkPatch b7444a7b8a0c. Separate upstream synchronization remains in its existing staging worktree.

A ghost used to forward a hit relative to its own cell. A pole therefore never got the hit location relative to its attachment points. The grid click path also always used the menu selection, which defaults to primary phase A on wooden poles with primary hardware. This prevented regular upper and rack clicks from selecting the intended bundles. The line-of-sight check likewise read the menu selection rather than the actual link endpoints.

Elevated wooden pole clicks now select the nearest rendered attachment and its bundle, with nearest individual conductor retained for a connection to a single terminal. Sneaking and base clicks retain explicit menu selection. Forwarding is scoped to wooden distribution poles. The existing grid link maps conductor index to matching conductor index and charges/refunds the actual paid spool. Bundles use terminals 52 (primary line), 53 (protected output), and 50 (service). Native bare overhead cable is rejected on wooden service terminals; AWG service keeps the original insulated, intact requirement. Jacket ratings remain electrical limits used for insulation behavior, rather than a new hard voltage filter.

| Family | ForkPatch attachment rule | 26.3 implementation |
| --- | --- | --- |
| Single-phase, single-phase transformer and feeder | One primary conductor, service rack bundle | Same counts through automatic aiming, nearest single when connecting to a single terminal |
| Three-phase, three-phase transformer and feeder | Three primary conductors, service rack bundle | Same counts and native phase-index mapping |
| Cutout, regulator, recloser, air-break and fused tap | Nearest line or protected output, one conductor for a single-phase tap | Select through the same rule, using the variant's existing terminal availability |
| Secondary and secondary streetlight | Service bundle and insulated cable | Same bundle and insulated AWG policy |
| Secondary-fed and streetlight variants with primary hardware | Primary and service are separate | Same circuit separation with hit-based targeting |
| Underground cabinets and feeders without overhead terminals | Use placed cables or the appropriate overhead riser/weatherhead | Existing underground restrictions retained |
| Service/weatherhead/standalone insulators | Device-specific single terminal or service bundle | Existing selected terminal behavior, with automatic wooden endpoint narrowed to its nearest matching single conductor when applicable |
| 69/230 kV pole and riser | Existing transmission placement in this port | Existing transmission path preserved |

The current render model preserves its existing physical arm orientation. The original ForkPatch also optimizes wooden arm angles across a connected run; this interaction repair does not transplant that render-graph behavior. Headless mesh and world checks do not establish live visual or audio parity.

The wall controls already requested eln:click, but the 26.3 asset index lacked that event and its OGG. Restored both from ForkPatch.

Focused server: 11/11 passed in /tmp/eln-distribution-focused.log. Forward and reversed hit tests use differently oriented wooden poles, verify bare primary and insulated copper 14 AWG and aluminum 2 AWG service, and test independent terminals, underfunded rejection, coexistence and exact 42 m refunds. Existing manual single-phase and four 69/230 kV pole and riser cases pass. The restored click.ogg matches the original byte-for-byte.

Full verification logs: /tmp/eln-distribution-build.log and /tmp/eln-distribution-full.log. Assembly log: /tmp/eln-distribution-assemble.log. Previous baseline: /tmp/eln-eleven-game.log (2129/2133, including one missed AWG large-aluminum corner refund in addition to the three historical cases).
