# Chip Fabricator adapter review

Source `b7444a7b8a0c5e072b1fd35eed2b74a282c3348c` registers Fabricator at
transparent lookup 4480 (`registerFab(70)`). Supporting shared lookups 8064–8068
are Silicon Wafer, Transistor, NTC Thermistor, Nibble Memory Chip and Arithmetic
Logic Unit. All six source slots are free in the current 652-entry native
manifest. This is a source review; no native Fabricator or basic-component
adapter is registered by this checkpoint.

The source `transparentnode/Fabricator.kt` uses a grounded 40-ohm resistor
(200 V / 1 kW), a 4,000 J budget per attempt and three slots: output 0, silicon
wafer1 and copper plate2. Its power entry is explicitly global ZN/Down, and its
mesh does not rotate with `front`; resolve that source contract deliberately
rather than silently reusing the new wire machines' all-face floor ports.
The `fabricator` OBJ/MTL/texture is already indexed for the Wire Insulator.
A true Fabricator also draws its X/Z etcher parts; do not substitute the
insulator's body/bath renderer or advance animation during mesh capture.

There are 15 operation IDs 0–14. Transistors yield 16 per sheet; D/JK flip-flops,
PAL, oscillator and the nine analog operations yield 4. Arithmetic Logic Unit
is the displayed 8 Bit ALU operation: two items per sheet with 50% yield. Lookup
of its three new resource items must be deferred until native registration is
complete. Preserve existing native logic-chip identities and crafting routes.

The source process has correctness defects that should not be reproduced:

- It compares modern item `descriptionId` to the old unnamespaced strings
  `Silicon_Wafer.name` and `Copper_Plate.name`. Native input acceptance should use
  actual descriptor/item identities or the intended validated tags.
- Existing output capacity uses strict `< maxStackSize`, rejecting an exactly
  full legal result. Check complete item/components compatibility and `<=`.
- Its completion block can run independently of the current `canOutput` and
  `hasInputs` result whenever stored work is sufficient. An empty/changed input
  or incompatible output must never inherit enough work to mint a free chip.
- A failed 50% yield consumes both inputs without debiting 4,000 J. Every attempt
  must debit its complete paid budget once, including failures. Persist an
  attempt/result boundary so reload cannot reroll or refund an already committed
  attempt. Keep inventory, selection and accepted-work mutation atomic.
- Only operation packets clear the work accumulator. Native external input/job
  changes need explicit handling, while a same-job electrical outage should not
  erase already paid energy. Algebraic trials and GUI/render ticks cannot pay
  or complete work.

World keys `operation` and `powerConsumed` need source-aware validation and a
native save contract. Invalid/nonfinite/negative state must fail closed. Real
world reload may retain legitimate partial work; ordinary picked/configuration
items must not copy it, input/output inventories or another machine's attempt.
Use native sided transfer capabilities with only output 0 extractable and the
correct wafer/plate inputs. Real UI packets must reject truncated/out-of-range
operations without changing a live job.

No explicit Fabricator or Silicon Wafer bootstrap recipe was found in the pinned
craft registration or data recipes inspected. The source's recipe-check loop
only reports missing recipes; it does not generate them. Establish the intended
survival recipe/wafer production route before calling the adapter a complete
playable manufacturing chain. Do not invent a free wafer conversion or replace
existing native chip recipes merely to fill that gap.

Required production checks include each of the 15 outputs; actual 4 kJ sag/normal
power; exact-capacity output and metadata compatibility; failed-yield paid attempt;
no-input/output-blocked/job-change boundaries; interrupted power and actual owner
reload; save/copy/break/drop ownership; source slot order, native automation,
menus, complete publication and all three moving mesh parts. Statistical yield
checks should use a reproducible physical attempt sequence rather than a flaky
single random-success expectation. Interactive GPU/JEI and multiplayer remain
separate validation.
