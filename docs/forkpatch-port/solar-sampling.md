# Solar sampling and native shadows

The source daylight sampling point rotates with the panel front, then follows the
saved roof pitch about the mounting foot. The native adapter now uses that point
for actual environment checks. The supported native full faces and stair mounting
have no additional inset; broader partial-surface mounting remains a separate
shared-framework review.

Two inherited shadow errors are corrected in the destination adapter. Modern
block light dampening is **0–15**, whereas the legacy calculation used 255 as its
denominator and transmitted radiation through opaque roofs. Sampling at one step
along the dominant axis also missed some diagonally crossed cells and truncated
negative coordinates toward zero. Native voxel traversal now visits each crossed
cell, floors negative coordinates, handles integer boundaries toward the sun and
uses the modern dampening scale. Glass remains transparent and partial opacity
retains graded attenuation.

A resident chunk's `WORLD_SURFACE` heightmap gives a conservative first clear Y.
Above that surface, the ray jumps to the next column or exits the world without
reading individual blocks. A chunk reference is cached only during one synchronous
query. The server uses `getChunkNow`; missing terrain ends sampling without a load
or generation request, retaining the source policy for unloaded terrain. Work is
capped at 4,096 voxel/column visits so horizontal sun rays have a finite budget.
Unknown terrain beyond the resident area or visit cap is unexamined, as it is
beyond the source's resident-terrain sampling area.

The destination's local graded weather integration remains. Values clamp to the
normal 0–1 range; malformed arrays and nonfinite values produce zero radiation
rather than exceptions or nonfinite solver input. Saved/nonfinite light factors
also open the finite Norton equivalent, and valid light can recover normally.

Eight ray regressions include independent analytic ray/box comparisons in 250
seeded scenes, a thin diagonal obstruction, negative/integer coordinates, native
partial opacity, clear-heightmap jumps, build-height termination, missing terrain
and horizontal/invalid rays. Additional physical Norton regressions exercise
invalid saved light and recovery. The eighteen roof cases place an opaque shade
above each actual pitched sample point and verify full shadow/recovery.

The representative native field case creates 96 real solar panels. It opens the
GameTest enclosure’s transparent barrier cells directly above its own modules so
the measurement represents an outdoor array. At clear noon
it checks illumination, counts heightmap columns/block reads, and requires zero
per-voxel block reads above the clear array. It also checks actual stone, glass,
leaves, local graded weather, malformed-provider handling and recovery. The case also records seven alternating paired timing/allocation rounds after two
warmup pairs, comparing the shortcut with full resident-voxel traversal in the same
scene. Independent geometric tests and equal clear-scene transmission validate
that comparison. These are work-count and ray-sampling measurements in one real
native scene; they are not whole-mod loaded-world CPU comparisons against both
complete source/destination baselines.

The recorded unfiltered run measured median ray CPU time of about 6.14µs for
full voxel traversal and 0.185µs with heightmaps (about 33× faster). Allocation rose
from about 48 to 120 bytes per ray. This trades roughly 72 extra bytes per query for
the lower CPU cost; [all paired rounds](solar-ray-benchmark.json) remain available.
