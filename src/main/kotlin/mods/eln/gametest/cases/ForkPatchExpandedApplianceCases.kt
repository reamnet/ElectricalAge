package mods.eln.gametest.cases

import mods.eln.Eln
import mods.eln.gametest.*
import mods.eln.misc.*
import mods.eln.node.transparent.*
import mods.eln.transparentnode.kitchen.*
import mods.eln.transparentnode.laundry.*
import mods.eln.transparentnode.fan.*
import mods.eln.transparentnode.fusion.*
import mods.eln.utility.*
import mods.eln.sim.IProcess
import mods.eln.sim.mna.component.*
import net.minecraft.nbt.CompoundTag
import net.minecraft.world.item.*
import net.minecraft.world.phys.*
import java.io.DataInputStream
import kotlin.math.abs

object ForkPatchExpandedApplianceCases {
    @JvmStatic fun cases():List<TestCase> = KitchenType.entries.map{Kitchen(it)}+FanType.entries.flatMap{listOf(Fan(it,false),Fan(it,true))}+MatterMachineKind.entries.map{Matter(it)}+SubstationKind.entries.flatMap{listOf(Station(it,false),Station(it,true))}+MatterMachineKind.entries.map{Finish(it)}+mods.eln.sixnode.lampsocket.RatedLampBulbs.ratings.map{BulbPower(it)}+FanType.entries.filter{!it.industrial}.map{FanCarry(it)}+SubstationKind.entries.flatMap{listOf(StationFault(it,false),StationFault(it,true))}+listOf(GridCore(),Bulbs(),Processes(),Hazard())
    private open class Base:TestCase {
        override fun id()="forkpatch_expanded_base"
        override fun width()=24;override fun height()=12;override fun depth()=24;override fun floorDepth()=24
        override fun ticks()=40
        override fun build(c:TestContext){}
        override fun measure(c:TestContext){}
        fun place(c:TestContext,id:Int,x:Int=10,z:Int=10):TransparentNodeElement {c.floor(c.at(x,0,z));return c.placeTransparent(id,c.at(x,1,z))}
        fun graphics(c:TestContext,e:TransparentNodeElement) {
            mods.eln.client.ClientRenders.registerAll()
            val at=e.node.coordonate.pos();val tile=TransparentNodeEntity(at,c.level().getBlockState(at));tile.setLevel(c.level())
            try {
                val data=DataInputStream(e.node.publishPacket.toByteArray().inputStream());data.readByte();repeat(4){data.readInt()};data.readUTF();tile.serverPublishUnserialize(data)
                val render=tile.elementRender
                c.check("expanded publication: native render factory consumes whole packet",render!=null&&data.available()==0,"${e.javaClass.simpleName},remaining=${data.available()}")
                if(render!=null){mods.eln.gl.GlFrame.beginRenderFrame();val f=mods.eln.gl.GlFrame.open();try{render.draw();c.check("expanded graphics: original model draws real vertices",f.batches().values.sumOf{it.vertices()}>100,"")}finally{mods.eln.gl.GlFrame.close()}}
            }finally{tile.clientDestroy()}
        }
    }
    private class Kitchen(val type:KitchenType):Base(){
        lateinit var e:KitchenElement;lateinit var power:VoltageSource
        override fun id()="forkpatch_expanded_kitchen_${type.name.lowercase()}_paid_processing_save_menu"
        override fun ticks()=360
        override fun build(c:TestContext){
            e=place(c,7936+type.subId) as KitchenElement
            val input=when(type){KitchenType.TOASTER->ItemStack(Items.BREAD);KitchenType.COFFEE->ItemStack(Items.COCOA_BEANS);KitchenType.KETTLE->water();KitchenType.BLENDER->ItemStack(Items.APPLE);KitchenType.RICE->ItemStack(Items.WHEAT);KitchenType.FRIDGE,KitchenType.MINI_FRIDGE->ItemStack(Items.WATER_BUCKET);else->ItemStack(Items.BEEF)}
            e.inventory.setItem(0,input);if(type in listOf(KitchenType.COFFEE,KitchenType.BLENDER,KitchenType.RICE))e.inventory.setItem(1,water())
            val before=e.inventory.getItem(0).copy();e.slowProcessList.forEach{it.process(.1)}
            c.check("kitchen outage: no unpaid product or progress",e.inventory.getItem(2).isEmpty&&e.work.joules==0.0&&ItemStack.isSameItemSameComponents(before,e.inventory.getItem(0)),"")
            power=VoltageSource("native kitchen mains",e.load,null).setU(type.volts);Eln.simulator.addElectricalComponent(power)
            val spec=mods.eln.gui.MenuSpecs.of(e.descriptor)
            c.check("kitchen menu: source slot count, coordinates and panel",spec.slots().size==type.slotCount&&spec.slots()[0].x()==17&&spec.slots()[2].x()==135&&spec.width()==176,"")
        }
        override fun measure(c:TestContext){try{
            if(!type.refrigerator)c.check("kitchen paid production: real electrical work yields food",!e.inventory.getItem(2).isEmpty&&e.inventory.getItem(0).isEmpty,"$type ${e.status},${e.work.joules},${e.load.u}")
            else c.check("refrigerator: powered storage cools physically and preserves inventory",e.temperature<20.0&&e.inventory.containerSize==type.slotCount&&e.inventory.getItem(0).`is`(Items.WATER_BUCKET),"${e.temperature}")
            val expected = when (type) {
                KitchenType.TOASTER -> KitchenFoods.toast.get()
                KitchenType.COFFEE -> KitchenFoods.coffee.get()
                KitchenType.KETTLE -> KitchenFoods.hotWater.get()
                KitchenType.BLENDER -> KitchenFoods.smoothie.get()
                KitchenType.RICE -> KitchenFoods.grains.get()
                else -> null
            }
            if (expected != null) {
                val output = e.inventory.getItem(2)
                c.check("paid kitchen cycle makes the exact native food",output.`is`(expected) && output.count==1,"$type: $output")
                if (type in listOf(KitchenType.COFFEE, KitchenType.BLENDER, KitchenType.RICE)) {
                    val remainder = e.inventory.getItem(1)
                    val paidWater = if (type==KitchenType.RICE) remainder.`is`(Items.GLASS_BOTTLE) && remainder.count==1 else remainder.isEmpty
                    c.check("paid kitchen cycle consumes one water bottle and preserves the native remainder",paidWater,"$type: $remainder")
                }
            }
            c.check("kitchen electrical: real source supplies the passive load",power.current.isFinite()&&e.load.u>type.volts*.99,"")
            graphics(c,e)
        }finally{Eln.simulator.removeElectricalComponent(power)}}
        fun water()=net.minecraft.world.item.alchemy.PotionContents.createItemStack(Items.POTION,net.minecraft.world.item.alchemy.Potions.WATER)
    }
    private class Fan(val type:FanType,val off:Boolean):Base(){
        lateinit var e:FanElement;lateinit var power:VoltageSource
        override fun id()="forkpatch_expanded_fan_${type.name.lowercase()}_${if(off)"off" else "running"}_motor_cord_save"
        override fun ticks()=100
        override fun build(c:TestContext){e=place(c,7964+type.ordinal)as FanElement;if(off)e.networkUnserialize(c.stream{it.writeByte(80)});power=VoltageSource("native fan mains",e.load,null).setU(type.volts);Eln.simulator.addElectricalComponent(power)}
        override fun measure(c:TestContext){try{
            c.check("fan motor: powered speed responds to physical startup and switch",if(off)e.rotor.speed==0.0 else e.rotor.speed>.15,"${e.rotor.speed}")
            c.check("fan motor: real wattage billed only when enabled",if(off)abs(power.current)<1e-7 else abs(abs(power.current*power.u)-type.watts)<type.watts*.02,"${power.current*power.u}")
            val speed=e.rotor.speed;if(!off){e.networkUnserialize(c.stream{it.writeByte(80)});e.slowProcessList.forEach{it.process(.1)};c.check("fan rotor: blades coast rather than stop instantly",e.rotor.speed>0&&e.rotor.speed<speed,"")}
            c.check("fan carrying: only portable source types accept carrying",e.cordPortable==!type.industrial,"")
            c.check("fan recipe/UI: native176x230 controls and empty device inventory",mods.eln.gui.MenuSpecs.of(e.descriptor).height()==230&&e.inventory.containerSize==0,"")
            graphics(c,e)
        }finally{Eln.simulator.removeElectricalComponent(power)}}
    }
    private class Matter(val kind:MatterMachineKind):Base(){
        lateinit var e:MatterMachineElement;lateinit var power:VoltageSource
        override fun id()="forkpatch_expanded_matter_${kind.name.lowercase()}_real_power_water_components"
        override fun ticks()=100
        override fun build(c:TestContext){
            e=place(c,8000+kind.subId)as MatterMachineElement
            val tool=ItemStack(Items.IRON_PICKAXE).apply{damageValue=120;set(net.minecraft.core.component.DataComponents.CUSTOM_NAME,net.minecraft.network.chat.Component.literal("Retain my tool"))}
            val input=when(kind){MatterMachineKind.WASHER->tool;MatterMachineKind.DRYER->WetEquipment.wash(tool);MatterMachineKind.FUSION->ItemStack(Items.COBBLESTONE)}
            e.inventory.setItem(0,input)
            if(kind==MatterMachineKind.WASHER){e.water.tank.fill(net.neoforged.neoforge.fluids.FluidStack(net.minecraft.world.level.material.Fluids.WATER,2000),true)}
            c.check("matter machine: zero work before accepted electricity",e.work.joules==0.0&&e.inventory.getItem(1).isEmpty,"")
            power=VoltageSource("native matter machine mains",e.supply,null).setU(kind.volts);Eln.simulator.addElectricalComponent(power)
        }
        override fun measure(c:TestContext){try{
            c.check("matter machine: accepted real electrical steps buy work",e.work.joules>1000&&e.work.joules<=kind.watts*10,"${kind},${e.status},${e.work.joules},${e.supply.u}")
            c.check("matter machine: no premature products or consumed template",e.inventory.getItem(1).isEmpty&&!e.inventory.getItem(0).isEmpty,"")
            if(kind==MatterMachineKind.WASHER)c.check("washer water: exactly one paid litre reserved",e.waterReserved&&e.water.tank.fluidAmount==1000,"")
            val spec=mods.eln.gui.MenuSpecs.of(e.descriptor);c.check("matter menu: source two slots and208x240 panel",spec.width()==208&&spec.height()==240&&spec.slots()[0].limit()==1,"")
            graphics(c,e)
        }finally{Eln.simulator.removeElectricalComponent(power)}}
    }
    private class Station(val kind:SubstationKind,val opened:Boolean):Base(){
        lateinit var e:SubstationElement;val sources=ArrayList<VoltageSource>();val consumers=ArrayList<Resistor>()
        override fun id()="forkpatch_expanded_substation_${kind.name.lowercase()}_${if(opened)"open"else "loaded"}_three_phase_conservation"
        override fun build(c:TestContext){e=place(c,kind.id)as SubstationElement
            repeat(3){i->val source=VoltageSource("native substation phase$i",e.inputs[i],null).setU(kind.inputVolts*(1-i*.05));sources+=source;Eln.simulator.addElectricalComponent(source);val r=Resistor(e.outputs[i],null).apply{this.r=kind.outputVolts*kind.outputVolts/(kind.wattsPerPhase*.1)};consumers+=r;Eln.simulator.addElectricalComponent(r)}
            if(opened)e.operate(true)
        }
        override fun measure(c:TestContext){try{
            repeat(3){i->
                c.check("substation phase $i: fixed passive ratio or open disconnect",if(opened)abs(e.outputs[i].u)<.01 else abs(e.outputs[i].u/(kind.inputVolts*(1-i*.05)*kind.ratio)-1)<.01,"${e.outputs[i].u}")
            }
            val input=sources.sumOf{abs(it.u*it.current)};val output=consumers.sumOf{it.p}
            c.check("substation conservation: output never exceeds real input",output<=input+1e-4&&(opened||output>1000),"$input -> $output")
            c.check("substation wiring: explicit load bank preserves three distinct phases",e.gridLoads(53).size==3&&e.gridPhase(36)==0&&e.gridPhase(38)==2&&e.gridLoads(32)[0]!==e.gridLoads(36)[0],"")
            graphics(c,e)
        }finally{sources.forEach{Eln.simulator.removeElectricalComponent(it)};consumers.forEach{Eln.simulator.removeElectricalComponent(it)}}}
    }
    private class Finish(val kind:MatterMachineKind):Base(){
        lateinit var e:MatterMachineElement;lateinit var source:VoltageSource
        override fun id()="forkpatch_expanded_matter_${kind.name.lowercase()}_finish_saved_paid_cycle"
        override fun ticks()=100
        override fun build(c:TestContext){
            e=place(c,8000+kind.subId)as MatterMachineElement
            val tool=ItemStack(Items.IRON_PICKAXE).apply{damageValue=120;set(net.minecraft.core.component.DataComponents.CUSTOM_NAME,net.minecraft.network.chat.Component.literal("My saved tool"))}
            e.inventory.setItem(0,when(kind){MatterMachineKind.WASHER->tool;MatterMachineKind.DRYER->WetEquipment.wash(tool);MatterMachineKind.FUSION->ItemStack(Items.COBBLESTONE)})
            e.inventoryChange(e.inventory)
            val paidProgress = e.requiredJoules-1000.0
            e.work.load(paidProgress,e.requiredJoules)
            if (kind==MatterMachineKind.WASHER) c.after(20) {
                c.check("powered washer without water produces no wet equipment or additional work",
                    e.inventory.getItem(1).isEmpty && e.work.joules==paidProgress,"${e.status}: ${e.work.joules}")
                e.water.tank.fill(net.neoforged.neoforge.fluids.FluidStack(net.minecraft.world.level.material.Fluids.WATER,1000),true)
            }
            c.check("matter saved progress: credit alone cannot complete an unpaid cycle",e.inventory.getItem(1).isEmpty,"")
            source=VoltageSource("finish saved paid cycle",e.supply,null).setU(kind.volts);Eln.simulator.addElectricalComponent(source)
        }
        override fun measure(c:TestContext){try{
            val output=e.inventory.getItem(1)
            c.check("matter cycle: committed electricity produces one result",output.count==1,"${kind},${e.status},$output")
            if(kind==MatterMachineKind.FUSION)c.check("fusion duplication: exactly one plain block and retained sample",output.`is`(Items.COBBLESTONE)&&e.inventory.getItem(0).count==1&&output.componentsPatch.isEmpty,"")
            else {
                c.check("laundry cycle: input consumed once",e.inventory.getItem(0).isEmpty,"")
                val tool=if(kind==MatterMachineKind.WASHER)WetEquipment.unpack(output)else output
                c.check("laundry cycle: repaired tool retains its actual name",tool.`is`(Items.IRON_PICKAXE)&&tool.damageValue==0&&tool.hoverName.string=="My saved tool","")
                if(kind==MatterMachineKind.WASHER) {
                    c.check("paid washer cycle yields one registered wet-equipment carrier",output.`is`(WetEquipment.item.get()) && output.count==1,"$output")
                    c.check("washer cycle: exactly one litre spent",e.water.tank.fluidAmount==0&&!e.waterReserved,"")
                }
            }
        }finally{Eln.simulator.removeElectricalComponent(source)}}
    }
    private class BulbPower(val rating:mods.eln.sixnode.lampsocket.RatedLampBulbs.Rating):Base(){
        lateinit var e:mods.eln.sixnode.lampsocket.DecorativeLampElement;lateinit var source:VoltageSource
        override fun id()="forkpatch_expanded_bulb_${rating.id}_actual_fixture_power"
        override fun ticks()=160
        override fun build(c:TestContext){
            val style=mods.eln.sixnode.lampsocket.DecorativeLampStyle.CHANDELIER
            val p=c.at(8,6,8);c.floor(p.above());e=c.placeSix(style.lookup,p,Direction.YP)as mods.eln.sixnode.lampsocket.DecorativeLampElement
            e.inventory.setItem(0,Eln.sharedItem.getDescriptor(rating.id)!!.newItemStack(style.bulbs));e.inventoryChange(e.inventory);e.networkUnserialize(c.stream{it.writeByte(3)})
            source=VoltageSource("source technology fixture",e.positiveLoad,null).setU(rating.volts);Eln.simulator.addElectricalComponent(source)
        }
        override fun measure(c:TestContext){try{
            c.check("bulb ${rating.title}: full fixture pays every installed bulb",abs(e.lampResistor.p-rating.watts*e.installedBulbs)<1e-5,"${e.lampResistor.p}")
            c.check("bulb ${rating.title}: actual native lighting process emits rated light",e.lightValue==(if(rating.volts==12.0)11 else 14),"${e.lightValue}")
        }finally{Eln.simulator.removeElectricalComponent(source)}}
    }
    private class GridCore:Base(){
        lateinit var e:GridSourceTransformerElement;lateinit var source:VoltageSource;lateinit var load:Resistor
        override fun id()="forkpatch_expanded_grid_converter_prerequisite_real_ratio_and_protection"
        override fun build(c:TestContext){e=place(c,7875)as GridSourceTransformerElement;source=VoltageSource("grid prerequisite source",e.primaryLoad,null).setU(120000.0);load=Resistor(e.secondaryLoad,null).apply{r=30000.0*30000/10000};Eln.simulator.addElectricalComponent(source);Eln.simulator.addElectricalComponent(load)}
        override fun measure(c:TestContext){try{
            c.check("grid prerequisite: fixed120/30kV ratio draws real source power",abs(e.secondaryLoad.u/30000-1)<.001&&load.p<=abs(source.u*source.current)+.01&&load.p>9900,"")
            val spec=mods.eln.gui.MenuSpecs.of(e.descriptor)
            c.check("grid prerequisite: actual native reset menu is registered",spec.width()==240&&spec.height()==166,"")
            val sample=GridSourceTransformerElement(TransparentNode(),e.descriptor);sample.protection.sample(.25,1.3,0.0)
            val copy=GridSourceTransformerElement(TransparentNode(),e.descriptor);copy.readItemStackNBT(sample.itemStackNBT)
            c.check("grid prerequisite: pickup/replacement cannot erase latched condition",copy.protection.tripped,"")
            graphics(c,e)
        }finally{Eln.simulator.removeElectricalComponent(source);Eln.simulator.removeElectricalComponent(load)}}
    }
    private class FanCarry(val type:FanType):Base(){
        override fun id()="forkpatch_expanded_fan_${type.name.lowercase()}_paid_carry_and_coast"
        override fun build(c:TestContext){
            val player=TransportTestPlayer(c.level(),com.mojang.authlib.GameProfile(java.util.UUID.randomUUID(),"fanCarrier"))
            val at=c.at(5,1,5);c.floor(at.north());val d=Eln.transparentNodeItem.getDescriptor(7968)!!
            val outlet=(d.parentItem as TransparentNodeItem).placeAt(d.newItemStack(),player,c.level(),at,Direction.ZN)!!.element as mods.eln.transparentnode.cord.OutletElement
            val e=place(c,7964+type.ordinal,7,5)as FanElement
            check(e.cord.attach(outlet.anchor(0),true));e.rotor.step(type.start,1.0)
            val token=e.cord.token
            check(e.cord.carry(player));val stack=player.mainHandItem
            mods.eln.generic.ItemNbt.edit(stack){it.putLong("carriedAt",c.level().gameTime-20)}
            val p=c.at(8,1,5);c.floor(p.below());val fresh=(stack.item as TransparentNodeItem).placeAt(stack,player,c.level(),p,Direction.YN)!!.element as FanElement;stack.shrink(1);fresh.cord.poll()
            c.check("fan carrying: paid lead and live owner transfer once",fresh.cord.token==token&&fresh.cord.connected&&fresh.cord.paid&&!mods.eln.transparentnode.cord.CordSupport.resident(e),"")
            c.check("fan carrying: saved rotor physically coasts while held",abs(fresh.rotor.speed-(1.0-1.0/type.coast))<1e-9,"${fresh.rotor.speed}")
            e.cord.onBroken();c.check("fan carrying: stale owner cannot reclaim the cable",fresh.cord.connected&&outlet.occupied(0),"")
        }
    }
    private class StationFault(val kind:SubstationKind,val overload:Boolean):Base(){
        lateinit var e:SubstationElement;val sources=ArrayList<VoltageSource>();val loads=ArrayList<Resistor>()
        override fun id()="forkpatch_expanded_substation_${kind.name.lowercase()}_actual_${if(overload)"overload"else "overvoltage"}_latch"
        override fun ticks()=140
        override fun build(c:TestContext){
            e=place(c,kind.id)as SubstationElement
            c.check("substation prerequisite: cable descriptor is initialized before registration",(e.descriptor as SubstationDescriptor).cableDescriptor!=null,"")
            repeat(3){i->val v=VoltageSource("substation fault phase$i",e.inputs[i],null).setU(kind.inputVolts*if(overload)1.0 else 1.3);sources+=v;Eln.simulator.addElectricalComponent(v)
                val r=Resistor(e.outputs[i],null).apply{this.r=kind.outputVolts*kind.outputVolts/(kind.wattsPerPhase*if(overload)2.5 else .1)};loads+=r;Eln.simulator.addElectricalComponent(r)}
        }
        override fun measure(c:TestContext){try{
            c.check("substation fault: real accepted power trips and opens both banks",e.protection.tripped&&!e.primaryClosed&&!e.secondaryClosed,"$kind trip=${e.protection.tripped},heat=${e.protection.heat}")
            c.check("substation fault: opened bank actually deenergizes downstream",e.outputs.all{abs(it.u)<.01},"${e.outputs.map{it.u}}")
            if(!overload){e.networkUnserialize(c.stream{it.writeByte(80)});c.check("substation fault: control cannot close into continuing overvoltage",e.protection.tripped&&!e.primaryClosed,"")}
        }finally{sources.forEach{Eln.simulator.removeElectricalComponent(it)};loads.forEach{Eln.simulator.removeElectricalComponent(it)}}}
    }
    private class Bulbs:Base(){
        override fun id()="forkpatch_expanded_bulbs_all_six_technologies_native_ratings_and_life"
        override fun measure(c:TestContext){
            val ratings=mods.eln.sixnode.lampsocket.RatedLampBulbs.ratings
            c.check("bulb matrix:18 unique source identities and destination IDs",ratings.size==18&&ratings.map{it.sourceId}.toSet().size==18&&ratings.map{it.id}.toSet().size==18,"")
            for(r in ratings){val d=Eln.sharedItem.getDescriptor(r.id)as mods.eln.item.LampDescriptor
                c.check("bulb ${r.title}: source ratings and technology",d.nominalU==r.volts&&d.nominalP==r.watts&&d.nominalLife==r.seconds&&d.type==r.type,"")
                val stack=d.newItemStack();d.setLifeInTag(stack,.37);c.check("bulb ${r.title}: saved condition remains on item",d.getLifeInTag(stack)==.37&&d.r==r.volts*r.volts/r.watts,"")
            }
            c.check("bulb migration: stock 48 V and 230 V bulbs keep original IDs",(Eln.sharedItem.getDescriptor(293)as mods.eln.item.LampDescriptor).nominalU==Eln.LVU&&(Eln.sharedItem.getDescriptor(294)as mods.eln.item.LampDescriptor).nominalU==Eln.MVU,"")
        }
    }
    private class Processes:Base(){
        override fun id()="forkpatch_expanded_processes_fault_latch_cooling_wet_components_and_fusion_value"
        override fun measure(c:TestContext){
            val p=SubstationProtection();repeat(4){p.sample(.05,1.3,0.0)};c.check("substation protection: insulation delay cannot trip early",!p.tripped,"");p.sample(.05,1.3,0.0);c.check("substation protection: sustained overvoltage latches and cannot reset energized fault",p.tripped&&!p.reset(1.3),"")
            val tag=CompoundTag();p.write(tag);val loaded=SubstationProtection();loaded.read(tag);c.check("substation protection: latch survives native save",loaded.tripped,"");c.check("substation protection: safe manual reset",loaded.reset(1.0)&&!loaded.tripped,"")
            repeat(80){loaded.sample(.05,1.0,2.0)};c.check("substation protection: sustained overload trips thermal bank",loaded.tripped&&!loaded.reset(1.0),"");repeat(210){loaded.sample(.05,0.0,0.0)};c.check("substation protection: real cooling permits reset",loaded.reset(0.0),"")
            val w=MachineWork();w.account(1.0,1000.0,2000.0);w.account(Double.NaN,1000.0,2000.0);c.check("work accounting: nonfinite time cannot earn energy",w.joules==1000.0,"")
            val tool=ItemStack(Items.DIAMOND_PICKAXE).apply{damageValue=100;set(net.minecraft.core.component.DataComponents.CUSTOM_NAME,net.minecraft.network.chat.Component.literal("Keep me"))};val wet=WetEquipment.wash(tool);val dry=WetEquipment.unpack(wet)
            c.check("laundry components: wet carrier cannot be equipped and restores repaired named tool",!wet.isDamageableItem&&dry.`is`(Items.DIAMOND_PICKAXE)&&dry.damageValue==0&&dry.hoverName.string=="Keep me","")
            c.check("fusion values: real loaded recipes map ordinary block sample",FusionValues.cost(ItemStack(Items.COBBLESTONE))!=null,"")
            c.check("fusion bounds: invalid values rejected and source high tier capped",FusionCost.fromValue(Double.NaN)==null&&FusionCost.fromValue(1e30)!!.watts==500000.0,"")
            val h=MicrowaveBatteryHeat();c.check("microwave hazard: warning time cannot be skipped",!h.advance(.1,100000.0)&&h.hissing,"");c.check("microwave hazard: real powered warning fuse eventually ruptures",h.advance(2.0,1000.0),"")
        }
    }
    private class Hazard:Base(){
        lateinit var fan:FanElement;lateinit var source:VoltageSource
        override fun id()="forkpatch_expanded_fan_industrial_real_blade_geometry"
        override fun ticks()=100
        override fun build(c:TestContext){fan=place(c,7964)as FanElement;source=VoltageSource("industrial rotor hazard",fan.load,null).setU(240.0);Eln.simulator.addElectricalComponent(source)}
        override fun measure(c:TestContext){val center=Vec3.ZERO
            c.check("industrial fan: blade disk touches facing entity",FanBlades.touches(AABB(-.2,-.2,-.1,.2,.2,.1),center,Direction.ZP,2.28),"")
            c.check("industrial fan: nearby casing/behind blade plane is safe",!FanBlades.touches(AABB(-.2,-.2,.4,.2,.2,.8),center,Direction.ZP,2.28),"")
            val cow=requireNotNull(net.minecraft.world.entity.EntityTypes.COW.create(c.level(),net.minecraft.world.entity.EntitySpawnReason.COMMAND))
            try{val p=fan.node.coordonate;cow.setPos(p.x+.5,p.y+.5+fan.type.centerY-.3,p.z+.5);c.level().addFreshEntity(cow);val before=cow.health;fan.slowProcessList.toList().forEach{it.process(.05)};c.check("industrial fan: actual turning blades damage a live entity",cow.health<before,"${cow.health}/$before")}finally{cow.discard();Eln.simulator.removeElectricalComponent(source)}

        }
    }
}
