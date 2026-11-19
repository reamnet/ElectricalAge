# Flat fixed solar-array collision

The fixed 2×3 array now gives all five reservations the source's half-height
collision and outline. Its root was already half-height. Native ghost metadata 13
carries the new cached shape to remote clients without an owner/block-entity
lookup. Cube 0, existing metadata 1–12, and the rotating array's full-height envelope
retain their meanings. No registry identities or node NBT fields change.

New fixed flat placement writes 13 directly. On owner block-entity load, a scheduled
server block tick repairs older cube reservations. The repair reads only resident
chunks with `getChunkNow`, verifies the saved owner and UUID, and updates only
that owner's old cube cells. Unknown chunks defer the work for a later scheduled
tick; unrelated reservations, special metadata and real replacement blocks are
left alone. Completed repair stops scheduling, so there is no per-frame or
steady-state simulation scan. Repair never runs from an electrical callback.

Six native cases cover four actual placement fronts, old-state owner reload and
scheduled repair, normal world collision above/below the panel, a client query
without observer data, idempotence, one retained return terminal, teardown,
foreign ownership/real-block guards, missing terrain and rotating arrays. Existing
pitched roof and solar sampling cases continue to cover their separate states.

This fixes native arrays and native save reloads. A complete source-world converter
must still translate source ghost ownership and block-state properties explicitly.
See [ghost-state import review](ghost-migration-review.md).
