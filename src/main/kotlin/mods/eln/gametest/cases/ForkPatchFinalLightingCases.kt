package mods.eln.gametest.cases

import mods.eln.Eln
import mods.eln.gametest.*
import mods.eln.misc.*
import mods.eln.node.transparent.*
import mods.eln.sim.mna.component.VoltageSource
import mods.eln.sixnode.lampsocket.*
import mods.eln.transparentnode.festive.*
import mods.eln.transparentnode.floodlight.*
import mods.eln.transparentnode.worklight.*
import mods.eln.transparentnode.cord.*
import net.minecraft.nbt.CompoundTag
import net.minecraft.world.item.ItemStack
import java.io.DataInputStream
import kotlin.math.abs

/** Production adapters under the native solver, actual world placement, menu and publication. */
object ForkPatchFinalLightingCases {
    @JvmStatic fun cases():List<TestCase> = listOf(4416,4417,7994,7995,8006,8007).map{Festive(it)}+listOf(Flood(false),Flood(true),Work(false),Work(true),Emergency(4500),Emergency(4501),Wear(),Anchors(),CandleCarry(),Wireless())
    private open class Base:TestCase {
        override fun id()="forkpatch_final_lighting_base"
        override fun width()=24;override fun height()=12;override fun depth()=24;override fun floorDepth()=24
        override fun build(c:TestContext){}
        override fun measure(c:TestContext){}
        fun place(c:TestContext,id:Int):TransparentNodeElement=c.placeTransparent(id,c.at(10,1,10))
        fun graphics(c:TestContext,e:TransparentNodeElement){
            mods.eln.client.ClientRenders.registerAll()
            val at=e.coordonate().pos();val tile=TransparentNodeEntity(at,c.level().getBlockState(at));tile.setLevel(c.level())
            try{val data=DataInputStream(e.node.publishPacket.toByteArray().inputStream());data.readByte();repeat(4){data.readInt()};data.readUTF();tile.serverPublishUnserialize(data)
                val r=tile.elementRender
                c.check("lighting publication factory consumes complete packet",r!=null&&data.available()==0,"${e.javaClass},remaining=${data.available()}")
                if(r!=null){mods.eln.gl.GlFrame.beginRenderFrame();val frame=mods.eln.gl.GlFrame.open();try{r.draw();c.check("lighting original model or procedural mesh emits real geometry",frame.batches().values.sumOf{it.vertices()}>100,"")}finally{mods.eln.gl.GlFrame.close()}}
            }finally{tile.clientDestroy()}
        }
    }
    private class Festive(val id:Int):Base(){
        lateinit var e:StringLightsElement;lateinit var source:VoltageSource
        val watts get()=when(id){4416->35.0;4417->5.0;else->15.0}
        override fun id()="forkpatch_final_lighting_festive_${id}_paid_saved_drawn"
        override fun build(c:TestContext){
            e=place(c,id) as StringLightsElement
            if(e is FairyLightsElement){val at=e.coordonate();e.readItemStackNBT(CompoundTag().apply{putInt("festiveVolts",120);putDouble("fairyStartX",at.x-4.0);putDouble("fairyStartY",at.y+2.0);putDouble("fairyStartZ",at.z+.5);putDouble("fairyEndX",at.x+.5);putDouble("fairyEndY",at.y+.5);putDouble("fairyEndZ",at.z+.5)})}
            source=VoltageSource("paid festive",e.input,null).setU(120.0);Eln.simulator.addElectricalComponent(source)
        }
        override fun measure(c:TestContext){try{
            c.check("festive source wattage paid by real source",abs(e.loadResistor.p-watts)<.001&&abs(abs(source.u*source.current)-watts)<.001,"$id ${e.loadResistor.p}")
            c.check("festive actual input lights world",e.lightValue==15,"")
            val tag=CompoundTag();e.writeToNBT(tag);c.check("festive retains source-rated supply and channel",tag.getIntOr("festiveVolts",0)==120&&tag.getStringOr("festiveChannel","")=="xmas",tag.toString())
            val portable=CordSupport.device(e)?.cordPortable==true;c.check("only source holiday candle is portable",portable==(id==4417),"$id $portable")
            if(e is FairyLightsElement)c.check("fairy exact anchors and suspended wire survive save",tag.getDoubleOr("fairyStartX",0.0)==e.coordonate().x-4.0&&tag.getDoubleOr("fairyEndY",0.0)==e.coordonate().y+.5,tag.toString())
            c.check("festive native source-positioned menu",mods.eln.gui.MenuSpecs.of(e.descriptor).height()==206,"")
            graphics(c,e)
        }finally{Eln.simulator.removeElectricalComponent(source)}}
    }
    private class Flood(val motor:Boolean):Base(){
        lateinit var e:FloodlightElement;lateinit var source:VoltageSource
        val volts get()=if(motor)240.0 else 120.0
        override fun id()="forkpatch_final_lighting_flood_${if(motor)"motor"else "manual"}_paid_signals_save"
        override fun build(c:TestContext){
            e=place(c,if(motor)4353 else 4352) as FloodlightElement
            repeat(2){e.inventory.setItem(it,Eln.findItemStack(if(motor)"240V LED Light Bulb"else "120V LED Light Bulb"))};e.inventory.setChanged()
            if(!motor){e.aim(0,90.0);e.aim(1,35.0);e.aim(2,20.0)}
            source=VoltageSource("paid flood",e.electricalLoad,null).setU(volts);Eln.simulator.addElectricalComponent(source)
        }
        override fun measure(c:TestContext){try{
            val power=if(motor)45.0 else 30.0
            c.check("flood both bulbs draw source watts",abs(e.lamps.sumOf{it.p}-power)<.001&&abs(abs(source.u*source.current)-power)<.001,"${e.lamps.sumOf{it.p}}")
            c.check("flood replaceable bulbs illuminate",e.light>=14,"")
            c.check("motorized/manual control policy",e.aim(0,20.0)==!motor&&!e.aim(1,Double.NaN)&&!e.aim(2,46.0),"")
            if(motor){e.swivelControl.state=mods.eln.Eln.SVU*.25;e.headControl.state=mods.eln.Eln.SVU*.5;e.beamControl.state=mods.eln.Eln.SVU;e.slowProcessList.forEach{it.process(.05)};c.check("motor flood three actual signal gates control aiming",abs(e.swivelAngle-90)<1e-8&&abs(e.headAngle-90)<1e-8&&abs(e.beamWidth-45)<1e-8,"${e.swivelAngle},${e.headAngle},${e.beamWidth}")}
            val saved=CompoundTag();e.writeToNBT(saved);val restored=FloodlightElement(TransparentNode(),e.fixture);restored.readFromNBT(saved)
            c.check("flood saved frame angles bulbs restore",restored.rotationAxis==e.rotationAxis&&restored.blockFacing==e.blockFacing&&restored.swivelAngle==e.swivelAngle&&restored.headAngle==e.headAngle&&restored.beamWidth==e.beamWidth&&restored.inventory.getItem(1).count==1,"")
            val spec=mods.eln.gui.MenuSpecs.of(e.fixture);c.check("flood source slot positions and limits",spec.width()==176&&spec.height()==166&&spec.slots().size==2&&spec.slots()[0].x()==71&&spec.slots()[1].x()==89&&spec.slots().all{it.limit()==1},"")
            c.check("flood unsupported technology refused",!e.fixture.accepts(Eln.findItemStack("120V Fluorescent Light Bulb")),"")
            graphics(c,e)
        }finally{Eln.simulator.removeElectricalComponent(source)}}
    }
    private class Work(val generator:Boolean):Base(){
        lateinit var e:WorkLightElement;var external:VoltageSource?=null;var before=0.0
        override fun id()="forkpatch_final_lighting_work_${if(generator)"gasoline"else "external"}_paid_fuel_aim"
        override fun ticks()=120
        override fun build(c:TestContext){e=place(c,7936)as WorkLightElement;e.inventory.setItem(0,Eln.findItemStack("120V LED Light Bulb",2));e.inventory.setChanged()
            if(generator){e.inlet.put(mods.eln.oil.Oil.Cut.GASOLINE.source.get(), 1000, 293.15);before=e.tankLevel;e.networkUnserialize(c.stream{it.writeByte(80)})}
            else {external=VoltageSource("paid work light",e.positiveLoad,null).setU(120.0);Eln.simulator.addElectricalComponent(external)}
            e.networkUnserialize(c.stream{it.writeByte(83);it.writeDouble(35.0)});e.networkUnserialize(c.stream{it.writeByte(86);it.writeDouble(-40.0)})
        }
        override fun measure(c:TestContext){try{
            c.check("work light actual supply illuminates two replaceable heads",e.bulbCount==2&&e.brightness>=14&&abs(e.lamp.p-30)<.01,"${e.bulbCount},${e.brightness},${e.lamp.p},${e.positiveLoad.u}")
            if(generator)c.check("work light gasoline source bills fuel and remains running",e.on&&e.tankLevel<before&&e.tankLevel>0&&abs(e.powerSource.effectiveP-30)<.01,"${e.on},${e.tankLevel},${e.powerSource.effectiveP}")
            else c.check("work light external supply pays load with engine off",!e.on&&e.tankLevel==0.0&&abs(abs(external!!.current*external!!.u)-30)<.01,"")
            val saved=CompoundTag();e.writeToNBT(saved);val restored=WorkLightElement(TransparentNode(),e.descriptor);restored.readFromNBT(saved)
            c.check("work light save restores separate head aims and inventory",restored.aim.pan(0)==35.0&&restored.aim.tilt(1)==-40.0&&restored.inventory.getItem(0).count==2,"")
            val carried=WorkLightElement(TransparentNode(),e.descriptor);carried.readItemStackNBT(e.itemStackNBT);carried.inlet.make(0.0, 1.0, 293.15)
            c.check("work light dropped assembly preserves paid fuel but never auto-starts",abs(carried.tankLevel-e.tankLevel)<1e-9&&!carried.on,"")
            if(generator){e.networkUnserialize(c.stream{it.writeByte(81)});c.check("work wireless selection disables engine immediately",!e.on&&e.powerSource.p==0.0,"")}
            val spec=mods.eln.gui.MenuSpecs.of(e.descriptor);c.check("work source menu dimensions and two-bulb limit",spec.height()==290&&spec.slots()[0].x()==17&&spec.slots()[0].y()==60&&spec.slots()[0].limit()==2,"")
            graphics(c,e)
        }finally{external?.let{Eln.simulator.removeElectricalComponent(it)}}}
    }
    private class Emergency(val id:Int):Base(){
        lateinit var e:mods.eln.sixnode.EmergencyLampElement;lateinit var source:VoltageSource;var initial=0.0
        override fun id()="forkpatch_final_lighting_emergency_${id}_paid_charge_saved_outage"
        override fun build(c:TestContext){e=c.placeSix(id,c.at(10,1,10))as mods.eln.sixnode.EmergencyLampElement;e.poweredByCable=true;initial=e.charge;source=VoltageSource("paid emergency",e.load,null).setU(e.desc.nominalVoltage);Eln.simulator.addElectricalComponent(source)}
        override fun measure(c:TestContext){try{
            c.check("emergency true source charging consumes rated watts",!e.on&&e.charge>initial&&abs(abs(source.u*source.current)-e.desc.chargePower)<.01,"${e.charge},${source.current}")
            val stack=e.dropItemStack;val restored=mods.eln.sixnode.EmergencyLampElement(mods.eln.node.six.SixNode(),Direction.YN,e.desc);restored.restoreLightItem(mods.eln.generic.ItemNbt.get(stack))
            c.check("emergency pickup retains paid charge and source settings",restored.charge==e.charge&&restored.poweredByCable,"")
            source.setU(0.0);e.load.state=0.0;val charge=e.charge;e.process.process(1.0)
            c.check("emergency outage consumes battery at rated lamp watts",e.on&&abs(e.charge-(charge-e.desc.consumption))<1e-8,"")
            c.check("emergency source voltage cord policy preserved",CordSupport.device(e)!!.acceptsCord(if(id==4500)CordRating.V120 else CordRating.V240)&&!CordSupport.device(e)!!.acceptsCord(CordRating.V48),"")
        }finally{Eln.simulator.removeElectricalComponent(source)}}
    }
    private class Anchors:Base(){
        override fun id()="forkpatch_final_lighting_two_click_fairy_anchors_paid_placement"
        override fun measure(c:TestContext){
            val player=TransportTestPlayer(c.level(),com.mojang.authlib.GameProfile(java.util.UUID.randomUUID(),"fairyTester")).also{it.setGameMode(net.minecraft.world.level.GameType.SURVIVAL)}
            val stack=Eln.transparentNodeItem.getDescriptor(7994)!!.newItemStack().apply{count=3}
            player.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND,stack)
            fun use(x:Int):net.minecraft.world.InteractionResult {
                val support=c.at(x,2,10);c.floor(support)
                return stack.useOn(net.minecraft.world.item.context.UseOnContext(player,net.minecraft.world.InteractionHand.MAIN_HAND,net.minecraft.world.phys.BlockHitResult(net.minecraft.world.phys.Vec3(support.x+.5,support.y+.5,support.z+1.0),net.minecraft.core.Direction.SOUTH,support,false)))
            }
            c.check("fairy first anchor does not consume hardware",use(4)==net.minecraft.world.InteractionResult.SUCCESS&&stack.count==3&&mods.eln.generic.ItemNbt.get(stack)!!.contains("fairyFirst"),"")
            c.check("fairy overlong span rejected without consuming or clearing anchor",use(14)==net.minecraft.world.InteractionResult.FAIL&&stack.count==3&&mods.eln.generic.ItemNbt.get(stack)!!.contains("fairyFirst"),"")
            c.check("fairy second valid anchor consumes exactly one fixture",use(10)==net.minecraft.world.InteractionResult.SUCCESS&&stack.count==2&&!mods.eln.generic.ItemNbt.get(stack)!!.contains("fairyFirst"),"")
            val node=mods.eln.node.NodeManager.instance.getNodeFromCoordonate(Coordonate(c.at(10,2,11),c.level()))as? TransparentNode
            val e=node?.element as? FairyLightsElement
            c.check("fairy placed owner retains both exact support anchors",e!=null&&abs(e.end.distanceTo(e.start)-6.0)<1e-12&&e.start.x==c.at(4,2,10).x+.5,"")
            if(e!=null){val saved=CompoundTag();e.writeToNBT(saved);val reloaded=FairyLightsElement(TransparentNode().apply{coordonate=Coordonate(c.at(10,2,11),c.level())},e.descriptor);reloaded.readFromNBT(saved);c.check("fairy saved suspended path restores both anchors",reloaded.start==e.start&&reloaded.end==e.end,"")}
        }
    }
    private class CandleCarry:Base(){
        override fun id()="forkpatch_final_lighting_candle_partial_support_paid_carry"
        override fun measure(c:TestContext){
            val p=TransportTestPlayer(c.level(),com.mojang.authlib.GameProfile(java.util.UUID.randomUUID(),"candleTester"))
            val outletPos=c.at(3,1,10);c.floor(outletPos.north())
            val od=Eln.transparentNodeItem.getDescriptor(7968)!!;val outlet=(od.parentItem as TransparentNodeItem).placeAt(od.newItemStack(),p,c.level(),outletPos,Direction.ZN)!!.element as OutletElement
            val at=c.at(6,1,10);c.level().setBlockAndUpdate(at.below(),net.minecraft.world.level.block.Blocks.STONE_SLAB.defaultBlockState())
            val e=c.placeTransparent(4417,at)as HolidayCandleElement
            c.check("candle partial support follows slab central height",e.mountOffset==-.5&&c.level().getBlockState(at).getShape(c.level(),at).bounds().minY==-.5,"")
            val cord=CordSupport.device(e)!!.cord;check(cord.attach(outlet.anchor(0),true));val token=cord.token
            c.check("candle carries full saved appliance and paid lead",cord.carry(p)&&!CordSupport.resident(e)&&mods.eln.generic.ItemNbt.get(p.mainHandItem)!!.contains("carriedAppliance"),"")
            val d=Eln.transparentNodeItem.getDescriptor(4417)!!;val dest=c.at(8,1,10);val replacement=(d.parentItem as TransparentNodeItem).placeAt(p.mainHandItem,p,c.level(),dest,Direction.YN)!!.element as HolidayCandleElement;p.mainHandItem.shrink(1)
            val fresh=CordSupport.device(replacement)!!.cord;fresh.poll()
            c.check("candle replacement transfers original paid claim without duplicating",fresh.token==token&&fresh.paid&&fresh.connected&&outlet.occupied(0),fresh.state)
        }
    }
    private class Wireless:Base(){
        lateinit var work:WorkLightElement;lateinit var lamp:mods.eln.sixnode.EmergencyLampElement
        lateinit var supply:mods.eln.sixnode.lampsupply.LampSupplyElement;lateinit var source:VoltageSource
        override fun id()="forkpatch_final_lighting_work_emergency_wireless_paid_source_retirement"
        override fun build(c:TestContext){
            work=place(c,7936)as WorkLightElement;work.inventory.setItem(0,Eln.findItemStack("120V LED Light Bulb",2));work.inventory.setChanged()
            lamp=c.placeSix(4500,c.at(8,1,10))as mods.eln.sixnode.EmergencyLampElement
            val channel="final wireless "+java.util.UUID.randomUUID();lamp.channel=channel
            work.networkUnserialize(c.stream{it.writeByte(82);it.writeUTF(channel)});work.networkUnserialize(c.stream{it.writeByte(81)})
            supply=c.placeSix(4162,c.at(6,1,10))as mods.eln.sixnode.lampsupply.LampSupplyElement
            supply.networkUnserialize(c.stream{it.writeByte(mods.eln.sixnode.lampsupply.LampSupplyElement.setPowerName.toInt());it.writeByte(0);it.writeUTF(channel)})
            source=VoltageSource("paid wireless lighting",supply.powerLoad,null).setU(120.0);Eln.simulator.addElectricalComponent(source)
        }
        override fun measure(c:TestContext){try{
            c.check("wireless lights and emergency charging paid by real supply",work.brightness>=14&&!lamp.on&&lamp.isConnectedToLampSupply&&abs(abs(source.u*source.current)-40.0)<.02,"${work.brightness},${lamp.on},${source.current*source.u}")
            supply.sixNode.disconnect()
            c.check("wireless source retirement severs all fixture pin adjacency synchronously",work.positiveLoad.u==0.0&&lamp.load.u==0.0&&supply.powerLoad.connectedComponents.none{component->component.connectedStates.any{it===work.positiveLoad||it===lamp.load}},"")
            work.slowProcessList.forEach{it.process(.05)};lamp.process.process(.05)
            c.check("retired wireless source cannot provide free fixture power",work.brightness==0&&lamp.on&&!lamp.isConnectedToLampSupply,"")
        }finally{Eln.simulator.removeElectricalComponent(source)}}
    }
    private class Wear:Base(){
        override fun id()="forkpatch_final_lighting_source_bulb_lifetime_reset_conditions"
        override fun measure(c:TestContext){
            for(r in RatedLampBulbs.ratings){val stack=Eln.findItemStack(r.title);val d=Utils.getItemObject(stack)as mods.eln.item.LampDescriptor
                d.setLifeInTag(stack,.5);val initial=SourceLampLife.hours(d)*.5
                val fraction=SourceLampLife.account(stack,d,0.0,60.0)
                c.check("source normalized used bulb condition imports once without renewal",abs(fraction-.5)<1e-12&&abs(mods.eln.generic.ItemNbt.get(stack)!!.getDoubleOr("sourceLampHours",-1.0)-initial)<1e-10,"${r.title},$fraction")
                mods.eln.generic.ItemNbt.edit(stack){it.putDouble("sourceLampHours",initial/2)}
                c.check("absolute source hours remain authoritative after prior configuration",abs(SourceLampLife.account(stack,d,0.0,1.0)-.25)<1e-12,r.title)
            }
            @Suppress("UNCHECKED_CAST")
            val configHours=mods.eln.ElnConfig::class.java.getDeclaredField("SOURCE_LAMP_HOURS").apply{isAccessible=true}.get(null)as Array<net.neoforged.neoforge.common.ModConfigSpec.DoubleValue>
            @Suppress("UNCHECKED_CAST")
            val configInfinite=mods.eln.ElnConfig::class.java.getDeclaredField("SOURCE_LAMP_INFINITE").apply{isAccessible=true}.get(null)as Array<net.neoforged.neoforge.common.ModConfigSpec.BooleanValue>
            val hours=configHours[4].get();val infinite=configInfinite[4].get();val aging=mods.eln.server.SaveConfig.instance.electricalLampAging
            try {
                mods.eln.server.SaveConfig.instance.electricalLampAging=true
                configInfinite[4].set(false);configHours[4].set(672.0)
                val used=Eln.findItemStack("120V LED Light Bulb");val rated=Utils.getItemObject(used)as mods.eln.item.LampDescriptor
                rated.setLifeInTag(used,.5);SourceLampLife.account(used,rated,0.0,1.0)
                configHours[4].set(1344.0)
                c.check("raising configured lifetime preserves absolute used hours",abs(SourceLampLife.account(used,rated,0.0,1.0)-.25)<1e-12,"")
                configHours[4].set(168.0)
                c.check("lowering configured lifetime caps hours at the new maximum",SourceLampLife.account(used,rated,0.0,1.0)==1.0,"")
                configInfinite[4].set(true)
                c.check("per-technology infinite life suppresses overvoltage wear",SourceLampLife.account(used,rated,180.0,3600.0)==1.0,"")
                configInfinite[4].set(false)
                c.check("restoring finite life consumes exact rated source hours",abs(SourceLampLife.account(used,rated,120.0,3600.0)-167.0/168)<1e-12,"")
            }finally{configHours[4].set(hours);configInfinite[4].set(infinite);mods.eln.server.SaveConfig.instance.electricalLampAging=aging}
            val d=Eln.findItemStack("120V LED Light Bulb");val bulb=Utils.getItemObject(d)as mods.eln.item.LampDescriptor;bulb.setLifeInTag(d,.2)
            val e=c.placeSix(4116,c.at(10,1,10))as LampSocketElement;e.inventory.setItem(0,d);e.inventory.setChanged()
            SourceLampLife.requestReset();c.check("reset restores only installed source bulbs",bulb.getLifeInTag(e.inventory.getItem(0))==1.0,"")
            val untouched=Eln.findItemStack("120V LED Light Bulb");bulb.setLifeInTag(untouched,.2)
            c.check("reset never renews later inserted or inventory bulbs",abs(SourceLampLife.account(untouched,bulb,0.0,1.0)-.2)<1e-12,"")
        }
    }
}
