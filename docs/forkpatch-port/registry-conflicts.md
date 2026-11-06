# Source and destination legacy lookup conflicts

Keep existing destination item/block identities. Incoming source offsets must be
read with their actual registration group, not inferred from adjacent code.

The pinned source calls `registerGridDevices(123)`. Its standalone Mains Step-down
Unit is `123:10` (**7882**). The same method also explicitly registers newer
renewables in group 124: MPPT 7944, hybrid 7945, utility wind 7946, watermill 7947.
The step-down's production constant and native lookup are 7882; it must not occupy
the wind turbine's 7946 identity. This was corrected before publishing the adapter.

An older destination checkpoint already assigned its floor Programmable UPS to
**7947**, while the pinned source floor UPS is **7883**. Keep the destination's
7947 and its `programmable_ups` registry name. The incoming watermill cannot reuse
that lookup key. The watermill adapter uses destination lookup **16383** (`255:63`) and the stable
`large_watermill_3_2kv` name. `LargeRenewableDescriptor.destinationIdForSource`
explicitly maps incoming source 7947 to 16383 and preserves wind 7946. This helper
is for source imports; it must never remap an existing destination UPS item. Do not silently replace the
UPS or move its existing key to make the incoming table look identical. Full
source-world migration is still pending; these lookup tables alone do not provide
save conversion.
