# Partial-support mounting review

The current solar adapter covers its four existing identities on full native
supports and bottom-half stairs. Roof direction, tilted geometry, uphill ghost
reservations, return terminal and daylight sample point are adapted and tested.
Broader source `SurfaceMounting` behavior remains a shared-framework port.

The pinned source keeps electrical owner positions on the grid while shifting the
mounting plane into unused space in a supporting cell. It examines collision
boxes crossing the center of the supporting face, chooses the nearest physical
surface, and computes an inset toward that support. A bottom slab therefore gives
a half-block floor inset; a bottom stair's upper central face gives zero inset.
The stair result is why the completed roof adapter needs pitch without an extra
support translation. An unknown neighboring chunk is not evidence that support
has disappeared, and passive queries use resident block states.

This behavior spans more than placement acceptance:

- `node/six/SixNodeBlock.kt` applies support/selection behavior and
  `SixNodeElementRender.kt` applies the visual offset.
- `node/transparent/TransparentNodeDescriptor.kt` defines support and offset hooks;
  the block entity translates collision boxes and the element renderer translates
  the visual body. Power-cabinet support checks use the same surface rules.
- `cable/CableRender.kt` uses both endpoints' mounting planes. Cable rise is
  `max(0, ownInset - otherInset)`, keeping both halves above the higher surface.
- `client/terrain/TerrainModels.kt` and partial lamp picking also use the offset.
  Omitting either path creates selection/collision/render disagreement.

The destination already has voxel placement, native oriented/cached collision
shapes, shortened in-line parts and cached cable/span geometry. Its generic floor
check accepts full sturdy faces plus special floor types; the solar descriptor
adds stairs explicitly. Porting the source helper alone would not update these
other native paths or establish complete partial-support parity.

A future adapter should preserve native voxel ownership and stable grid positions,
translate outline/collision/render/cable endpoints coherently, and invalidate any
cached offset when the relevant support shape changes. Avoid unbounded per-frame
collision-box allocation and avoid requesting chunks during global node restore.
Do not replace the existing shape caches or voxel placement wholesale.

Required cases include top/bottom slabs, all stair facings/halves, ceilings and
walls, support removal/rotation, mixed-height cable endpoints, ghost ownership,
selection/ray picking, already-loaded and missing neighboring chunks, old save
round trips and client publication. Distinguish partial-mount offsets from the
already validated solar 45° pitch and saved roof direction. Interactive client
selection/GPU checks remain necessary after server and geometric tests pass.
