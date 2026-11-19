# Wire manufacturing

Native Wire Roller 4608, Wire Insulator 4609 and Wire Combiner 4610 use the existing
utility cable catalog and metre/thermal APIs. Wheels 7552–7554 and paid Wound Wire
Bundle 7556 retain free source lookup slots; all preceding native identities remain
unchanged. The bootstrap recipes use native tags, motors and machine blocks. The base mod
has no aluminum ingot: modpacks may provide `c:ingots/aluminum`, while carried
aluminum buffers and existing native aluminum spools retain their material.
Aluminum wheel recipes need that tag as in the source.

At 200 V the passive loads draw 300/160/120 W. Processing rates are 6/4/5 metres per
second, with voltage-rating layer costs applied to insulation. Only accepted
electrical steps advance work; repeated readiness, menu or matrix checks create
none. Sag reduces processing power, output blocking opens the load, and a power
outage preserves already paid work for the same input/options/hardware. Copying
identical settings also preserves existing work without importing any. Changing
that job clears its partial work. Native cached LU factors remain in use.

The roller absorbs ingots as 1 kg each and retains fractional material credit.
Rubber contributes 32 base insulation-metres per item. Combination consumes the
shortest participating length from 2–8 matching single-core spools, preserving each
longer remainder. The nine-slot combiner retains historical output 5 and inputs
0,1,2,3,4,6,7,8. Menus and transactional sided automation use the same predicates.

World saves retain slots, scalar buffers, selection, partial work and physical
energy counters. Ordinary picked items and configuration copies carry settings
only. Actual destruction drops inventory separately and adds owned scalar buffers
to the one physical machine drop; real survival placement consumes that carrier.
Repeated break or carried-read callbacks cannot refill resources. Partial work
never travels in carried settings. Source direct world scalar keys are retained;
this does not establish a complete 1.21.1 world converter.

Hot wire keeps its temperature through insulation. Combining mixed-temperature
cores conserves their sensible heat in the wound bundle. Unsupported, damaged,
already failed, stacked or malformed stock is rejected. Bundles are single-stack
physical objects with their material, count, area, target label and fractional
length. Invalid/nonfinite saved scalar values fail closed.

Native publication drives the real menu/screen factories, source enclosure meshes,
moving rollers/combiner, insulation bath and bounded client-clock smoke. Options and paid-length snapshots
are cached until inventory/material changes, reload or control changes; native mesh draws do not scan the
catalog or advance effects. Optional JEI 31 categories expose source 32 m examples
calculated from the same native catalog and production rules, including retained
ingot surplus, wheel catalysts and processing energy. Large ingot quantities are
shown as legal stack batches; the machine retains credit between loads. Examples
requiring an unavailable ingot tag are omitted rather than displaying empty input.

The 163 native tests include 130 powered catalog cases covering all 100 intact
wire descriptors: every bare gauge/material, single/multicore jacket and bundle
target. The remaining cases cover four fronts, paid fractional outputs, three/eight-core
bundles, mixed-temperature heat, sag and stable LU, outages/job changes, packets,
configuration, actual break/place, actual owner reload, transactional output,
bootstrap crafting, all production examples and a powered automated chain through
all three devices. See [validation](wire-manufacturing-validation.json).
Interactive screens/GPU, particles, optional JEI runtime and multiplayer still
need hands-on validation. Remaining wire tools, wider equipment and whole-world
migration remain in the [backlog](remaining-work.md).

The [cache diagnostic](wire-manufacturing-cache-benchmark.json) records seven
alternating samples of 1,000 checks after warmup in the final unfiltered run.
Median thread CPU time was 0.366 ms for cached native readiness/options and
2.589 ms for an uncached native roller catalog query; median thread allocations
were 0 and 439,296 bytes. These are different explicitly measured paths
in one server, with no timing thresholds. Timing varied between runs; raw samples
are retained. This does not establish whole-mod or original-source performance.
