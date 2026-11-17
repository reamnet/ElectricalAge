# Wrap-up — 2026-11-17 · Kitchen/Laundry/Fusion slice

Repo: /home/matthew/projects/electrical-age-port/ElectricalAge
Branch: port/forkpatch-3.0.77-26.3 · Draft PR #1 (kept: title/base/draft)
HEAD: 6705a4847a6b557042ed14c693ff0d63635e779e — clean, 0/0 vs origin (pushed & in sync).
Source (read-only, do NOT modify): ../ELN1.21.1ForkPatch @ b7444a7b8a0c5e072b1fd35eed2b74a282c3348c

## What is DONE and SHIPPED (this + prior sessions)
- 14 kitchen/laundry/fusion DEVICE MODEL ASSETS committed & pushed (`6705a484`):
  14 OBJ/MTL model dirs + rebuilt/sorted model index.txt (fixed the `triajmparkinglight`
  typo -> `triarmparkinglight`). 42 legacy dangling index entries are pre-existing and
  harmless (loadObj fails-and-continues). No code/tests/goldens touched.
- Full earlier port (see overnight-handoff.md / wrap-up-handoff.md): devices, manufacturing,
  cords, panels/alarms/vehicles, decorative lighting, GUI coordinate matching, mctest infra.
- Baseline (must not regress): native GameTest 1470 sel / 1468 pass / 2 FAIL (HISTORICAL
  golden_descriptors + golden_slots 765-assertion delta — do NOT fix/regenerate/weaken) / 0 skip.
  ForkPatch native 1004 pass. Root :test 698 pass / 3 pre-existing skips. CAD 15/15.
  Datagen repeat (runClientData x2) = 0 writes (3631 files).

## What is NOT done (this slice — be explicit)
The 14 kitchen/laundry/fusion DEVICES ARE NOT FUNCTIONAL. Models are committed but
NO Kotlin code exists:
- No `src/main/kotlin/mods/eln/transparentnode/kitchen/`, `laundry/`, or `fusion/` dirs.
- No KitchenElement / WasherElement / FusionMatterReactor / etc.
- Not registered in ElnDeviceRegistry / ClientRenders / ClientScreens / MenuSpecs.
- The 5 kitchen foods (hot_water, fruit_smoothie, steamed_grains, toasted_bread, fresh_coffee)
  are not registered.
Status: MODELS committed · CODE not-started. Do NOT report these devices as done.

## Concrete next work (from prior handoff — ready to execute in one pass)
Locked ID map (namespace transparentNodeItem, id = group*64 + sub; do NOT re-derive):
- Group 124 (base 7936): MICROWAVE 7937(1) · TOASTER 7938(2) · STOVE 7939(3) · FRIDGE 7940(4)
  · COFFEE 7941(5) · KETTLE 7954(18) · BLENDER 7955(19) · RICE 7956(20) · MINI_OVEN 7963(27)
  · MINI_FRIDGE 7996(60) · GRILL 7997(61)
- Group 125 (base 8000): WASHER 8009(9) · DRYER 8010(10) · FUSION 8011(11)
- KitchenType.subId (trust): ordinal 9->60; 10->61; 0..4->1+ord; 5..7->18+ord-5; else->27+ord-8.

Implementation steps:
[C] Write `transparentnode/kitchen/`: KitchenType.kt(+KitchenWork), RefrigeratorThermal.kt,
    MicrowaveBatteryHeat.kt, KitchenRecipe.kt, KitchenFoods.kt, KitchenElement.kt,
    KitchenDescriptor.kt, KitchenMenus.kt, KitchenRender.kt, KitchenScreen.kt, kitchenStatus.
    Use ElnScreen (Fabricator pattern); render via transparentNodedescriptor; foods via
    ElnRegistry.ITEMS.registerItem. Wire the 5 registration sites + 5 foods.
[D] Build/test/datagen; capture new golden-delta JSON (precedent decorative-lighting-golden-delta.json).
    Commit code + push.
[E] Laundry (washer/dryer g125 sub 9/10; ISidedFluidHandler + wet equipment). Read source first. Commit+push.
[F] Fusion (EMC value graph, ~3200V/100000W/600s, g125 sub 11). Read fusion/FusionValues.kt first. Commit+push.
[G] Complex manufacturing (electricalmachine/*: Compressor, Macerator, Magnetizer, OldArcFurnace,
    PlateMachine) — port feasible; document rest blocked/partial.
[H] Final docs + validation JSON + list what's left + confirm baseline not regressed + push all.

## Key API substitutions (source -> target; verified present in target)
- ElnRegistry.registerItem / CreativeTabPopulator / .n(...) -> ElnRegistry.ITEMS.registerItem(name){p->Item(p...){}}
- CordDevice.cordVoltage -> implement CordDevice directly: override val cord=ApplianceCord(this,this,load);
  override val cordPortable get()=type!=FRIDGE; override fun acceptsCord(r:CordRating)=r.accepts(type.volts)
- KitchenContainer/BasicContainer/SlotWithSkinAndComment/GuiContainerEln -> MenuSpec+SlotSpec + ElnScreen
- IItemRenderer/ItemRenderType -> hasItemDraw()=true + drawItem(entity:Boolean){draw(0.0)} + draw(front,...)
- mods.eln.client.gl.GL11 -> mods.eln.gl.GL11
- TerrainModels.body / drawCable -> descriptor.draw(front,...); cable via getCableRender(side,lrdu)
- RefrigeratorPreservation -> REMOVE food-NBT side effects (neutral); keep fridge temp/door/cold-storage
- DelayedDestruction/IDestructible -> node.physicalSelfDestruction(0f) + world.explode(...,2f,false,ExplosionInteraction.NONE)
- getAmbientTemperatureCelsius() -> fridge ambient = constant 20.0C (document)
- Eln.config.getBooleanOrElse -> hardcode ExplosionInteraction.NONE
Present in target: Eln.findItemStack, LoopedSound(mods.eln.sound), ghost.GhostGroup, NbtElectricalLoad,
Resistor, ISidedFluidHandler, Utils.plotUIP/plotVolt/plotPower/plotValue, serialiseItemStack,
LampMounting.surface, PowerPanelDisplay.equipmentNumber, BatteryDescriptor/BatteryItem,
TransparentNodeElementInventory(n,64,this), NodeBase.maskElectricalPower.

## Still-to-verify (quick targeted rg -n before/while coding)
- Creative-tab population for the 5 foods: TABS = DeferredRegister<CreativeModeTab> at ElnRegistry.java:62,
  tab id eln:eln (PipeTransmitterCases.java:395). Eln.creativeTabMachines NOT found — don't assume.
- BatteryItem.getPriority(stack) / BatteryDescriptor / Utils.getItemObject signatures.
- SoundCommand verySmallRange()/mulVolume(.35f,1f) existence.
- node coordinate accessor name (coordinate() vs coordonate typo) on target TransparentNodeElement.

## Constraints (do NOT violate)
- Preserve native IDs, save compat, paid hardware/materials/condition, conservative energy/material/heat,
  cached LU factors, resident-only world queries, UUID/owner/save invariants.
- Do NOT weaken goldens/assertions/tolerances/skips; no test deletion/disabling; no expectation changes
  to fit wrong behavior; no undocumented skips.
- Do NOT change pinned Gradle/wrapper/deps. Do NOT invent APIs — read actual target code.
- 3 materially different failed approaches on a feature -> record blocker + attempted + likely fix,
  mark blocked, move on, return later.
- Label honestly (not-started/partial/implemented/compile-verified/tests-passed/live-verified/
  visual-pending/blocked). Only test in disposable dev worlds. Do NOT merge PR #1.

## Commands
- source /tmp/eln-port-java-env.sh  (JDK25 target, JDK21 source, GRADLE_USER_HOME=/tmp/eln-port-gradle)
- ./gradlew --no-daemon --max-workers=2 :build :cad:test
- ./gradlew --no-daemon --max-workers=2 runGameTestServer   (native, 1470)
- ./gradlew --no-daemon --max-workers=2 runClientData        (x2 -> 0 writes)
- :test (NOT bare test) for root-only filters
- Do NOT compile changed main classes while a test/client JVM uses the same build outputs.
