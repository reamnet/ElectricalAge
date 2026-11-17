package mods.eln.gametest.cases

import mods.eln.Eln
import mods.eln.gametest.*
import mods.eln.gl.GlFrame
import mods.eln.generic.*
import mods.eln.misc.*
import mods.eln.node.transparent.*
import mods.eln.registry.ElnRegistry
import mods.eln.sim.IProcess
import mods.eln.sim.mna.component.VoltageSource
import mods.eln.transparentnode.industrial.WorkshopParts
import mods.eln.transparentnode.cord.*
import mods.eln.vehicle.*
import net.minecraft.nbt.CompoundTag
import net.minecraft.world.InteractionHand
import net.minecraft.world.entity.*
import net.minecraft.world.entity.item.ItemEntity
import net.minecraft.world.item.*
import net.minecraft.world.level.GameType
import net.minecraft.world.level.storage.*
import net.minecraft.util.ProblemReporter
import net.minecraft.world.phys.*
import java.util.UUID
import kotlin.math.*

/** Native entities, driver payloads, saved inventories and measured station energy. */
object ForkPatchRoadVehicleCases {
    @JvmStatic fun cases():List<TestCase> = RoadVehicleKind.entries.flatMap {k->listOf(0f,90f,180f,270f).map{Drive(k,it)}+listOf(MenuSave(k),Placement(k),Breaks(k),Graphics(k))}+
        listOf(Seats(),Guards(),Corner(),Charge(RoadVehicleKind.CART,240.0),Charge(RoadVehicleKind.COUPE,240.0),Charge(RoadVehicleKind.COUPE,264.0),Charge(RoadVehicleKind.CART,-240.0),Charge(RoadVehicleKind.CART,199.0),Charge(RoadVehicleKind.COUPE,265.0),ChargeChange(),WiredStation(),MixedBays())
    private open class Base:TestCase {
        val entities=ArrayList<Entity>();val players=ArrayList<TransportTestPlayer>()
        override fun id()="forkpatch_road_vehicle_base"
        override fun width()=24;override fun height()=12;override fun depth()=24;override fun floorDepth()=24
        override fun ticks()=1
        override fun build(c:TestContext) {}
        override fun measure(c:TestContext) {}
        fun player(c:TestContext,suffix:String="driver")=TransportTestPlayer(c.level(),com.mojang.authlib.GameProfile(UUID.nameUUIDFromBytes((id()+suffix).toByteArray()),"roadFixture")).also{
            it.setGameMode(GameType.SURVIVAL);it.setItemInHand(InteractionHand.MAIN_HAND,ItemStack.EMPTY);players+=it
        }
        fun vehicle(c:TestContext,kind:RoadVehicleKind,yaw:Float=0f,x:Int=12,z:Int=12):RoadVehicleEntity {
            val e=RoadVehicleEntity((if(kind==RoadVehicleKind.CART)RoadVehicles.cart else RoadVehicles.coupe).get(),c.level(),kind);val at=c.at(x,1,z)
            e.snapTo(at.x+.5,at.y.toDouble(),at.z+.5,yaw,0f);check(c.level().addFreshEntity(e));entities+=e;return e
        }
        fun pack(joules:Double)=Eln.findItemStack(WorkshopParts.PACK,1).also{RoadVehicles.battery(it)!!.setEnergy(it,joules)}
        fun install(e:RoadVehicleEntity,joules:Double=1_000_000.0){repeat(e.kind.packs){e.items.setItem(it,pack(joules))}}
        fun energy(e:RoadVehicleEntity)=(0 until e.kind.packs).sumOf{i->e.items.getItem(i).let{RoadVehicles.battery(it)?.getEnergy(it)?:0.0}}
        fun mount(e:RoadVehicleEntity,p:TransportTestPlayer){p.snapTo(e.x+3,e.y,e.z,0f,0f);check(p.startRiding(e,true,false))}
        fun drive(e:RoadVehicleEntity,p:TransportTestPlayer,ticks:Int,forward:Int=1,turn:Int=0){repeat(ticks){VehicleControls.handle(p,VehicleControls(forward,turn));e.tick()}}
        fun station(c:TestContext,x:Int=4,z:Int=4):VehicleChargerElement {c.floor(c.at(x,0,z));return c.placeTransparent(8079,c.at(x,1,z)) as VehicleChargerElement}
        fun cleanup(){players.forEach{it.stopRiding();it.inventory.clearContent();it.isShiftKeyDown=false};entities.forEach{it.discard()}}
    }
    private class Drive(val kind:RoadVehicleKind,val yaw:Float):Base() {
        override fun id()="forkpatch_road_vehicle_${kind.name.lowercase()}_drive_${yaw.toInt()}"
        override fun measure(c:TestContext){try{
            val e=vehicle(c,kind,yaw);install(e);val p=player(c);mount(e,p)
            val start=e.position();val before=energy(e);drive(e,p,if(kind==RoadVehicleKind.CART)20 else 12)
            val delta=e.position().subtract(start);val r=yaw*PI/180
            c.check("road driving: server input moves along heading and spends actual packs",delta.x*(-sin(r))+delta.z*cos(r)>1.0&&energy(e) in 0.0..<before,"$kind $yaw: $delta,energy=${energy(e)}")
            c.check("road driving: client position authority stays disabled",!e.isClientAuthoritative&&e.isLocalInstanceAuthoritative,"")
            val heading=e.yRot;drive(e,p,4,turn=1)
            c.check("road steering: bounded controls turn the moving vehicle",abs(e.yRot-heading)>1.0,"")
            drive(e,p,50,forward=0);val stopped=e.position();repeat(20){e.tick()}
            c.check("road controls: stale input cannot retain throttle",e.speed==0.0&&e.position().subtract(stopped).horizontalDistance()<.01,"")
        }finally{cleanup()}}
    }
    private class Seats:Base(){
        override fun id()="forkpatch_road_vehicle_seats_driver_authority_and_missing_hardware"
        override fun measure(c:TestContext){try{
            for(kind in RoadVehicleKind.entries){
                val e=vehicle(c,kind);install(e);val riders=(0 until kind.seats).map{player(c,"${kind.name}$it")}
                riders.forEach{it.snapTo(e.x+3,e.y,e.z,0f,0f);check(it.startRiding(e,false,false))}
                val extra=player(c,kind.name+"extra")
                c.check("road seats: source capacity and controlling passenger are exact",e.passengers.size==kind.seats&&e.controllingPassenger===riders.first()&&!extra.startRiding(e,false,false),"$kind")
                val before=e.position();repeat(8){e.controls(extra,1.0,1.0);VehicleControls.handle(extra,VehicleControls(1,1));e.tick()}
                c.check("road authority: non-riders cannot move a vehicle",e.speed==0.0&&e.position().subtract(before).horizontalDistance()<1e-8,"")
                repeat(8){e.controls(riders.first(),Double.NaN,Double.POSITIVE_INFINITY);e.tick()}
                c.check("road controls: nonfinite input is rejected",e.speed==0.0&&e.yRot.isFinite(),"")
                e.items.setItem(kind.packs-1,ItemStack.EMPTY);drive(e,riders.first(),10)
                c.check("road hardware: missing required pack bays prevent driving",e.speed==0.0,"")
                e.ejectPassengers();e.discard()
            }
        }finally{cleanup()}}
    }
    private class MenuSave(val kind:RoadVehicleKind):Base(){
        override fun id()="forkpatch_road_vehicle_${kind.name.lowercase()}_menu_and_real_save"
        override fun measure(c:TestContext){try{
            val e=vehicle(c,kind);install(e,123456.0);e.items.setItem(6,ItemStack(Items.DIAMOND,7));e.items.setItem(14,ItemStack(Items.IRON_INGOT,3))
            val p=player(c);val menu=VehicleMenu(1,p.inventory,e)
            c.check("road menu: source six bays, nine cargo slots and player inventory",menu.slots.size==51&&(0..5).all{i->menu.slots[i].x==35+i*18&&menu.slots[i].y==32&&menu.slots[i].isActive==(i<kind.packs)}&&menu.slots[6].y==74&&menu.slots[15].y==106&&menu.slots.last().y==164,"")
            c.check("road menu: inactive bays and recursive road vehicle cargo are rejected",(0..5).all{i->menu.slots[i].mayPlace(pack(0.0))==(i<kind.packs)}&&!menu.slots[6].mayPlace(Eln.findItemStack(kind.title,1)),"")
            e.charger=UUID.randomUUID();val before=energy(e)
            c.check("road menu: attached charging locks real battery extraction",!menu.slots[0].mayPickup(p)&&menu.quickMoveStack(p,0).isEmpty&&energy(e)==before,"")
            e.charger=null;menu.removed(p)
            val saved=TagValueOutput.createWithContext(ProblemReporter.DISCARDING,c.level().registryAccess()).also(e::saveWithoutId).buildResult()
            val fresh=RoadVehicleEntity((if(kind==RoadVehicleKind.CART)RoadVehicles.cart else RoadVehicles.coupe).get(),c.level(),kind)
            fresh.load(TagValueInput.create(ProblemReporter.DISCARDING,c.level().registryAccess(),saved));entities+=fresh
            c.check("road save: actual battery energy and all cargo survive ValueInput/Output",energy(fresh)==before&&(0..14).all{ItemStack.isSameItemSameComponents(e.items.getItem(it),fresh.items.getItem(it))&&e.items.getItem(it).count==fresh.items.getItem(it).count},"")
            c.check("road save: transient movement, passengers and charging claims are not restored",fresh.speed==0.0&&fresh.controllingPassenger==null&&!fresh.plugged,"")
        }finally{cleanup()}}
    }
    private class Placement(val kind:RoadVehicleKind):Base(){
        override fun id()="forkpatch_road_vehicle_${kind.name.lowercase()}_paid_placement"
        override fun measure(c:TestContext){try{
            val p=player(c);val stack=Eln.findItemStack(kind.title,1);val d=GenericItemUsingDamageDescriptor.getDescriptor(stack) as RoadVehicleItem
            val at=c.at(12,0,12)
            check(d.onItemUse(stack,p,c.level(),at.x,at.y,at.z,net.minecraft.core.Direction.UP.get3DDataValue(),.5f,1f,.5f))
            val area=AABB(at).inflate(4.0);val cars=c.level().getEntitiesOfClass(RoadVehicleEntity::class.java,area);entities.addAll(cars)
            c.check("road placement: one paid item creates exactly one source vehicle",stack.isEmpty&&cars.size==1&&cars.single().kind==kind,"")
            val blocked=Eln.findItemStack(kind.title,1)
            c.check("road placement: a collision refuses placement without spending hardware",!d.onItemUse(blocked,p,c.level(),at.x,at.y,at.z,net.minecraft.core.Direction.UP.get3DDataValue(),.5f,1f,.5f)&&blocked.count==1,"")
        }finally{cleanup()}}
    }
    private class Breaks(val kind:RoadVehicleKind):Base(){
        override fun id()="forkpatch_road_vehicle_${kind.name.lowercase()}_single_material_refunds"
        override fun measure(c:TestContext){try{
            val e=vehicle(c,kind);install(e,222222.0);e.items.setItem(6,ItemStack(Items.DIAMOND,7));val area=e.boundingBox.inflate(4.0)
            check(e.hurtServer(c.level(),c.level().damageSources().generic(),100f))
            val drops=c.level().getEntitiesOfClass(ItemEntity::class.java,area).filter{it.isAlive};entities.addAll(drops)
            val packs=drops.filter{RoadVehicles.battery(it.item)!=null}
            c.check("road removal: vehicle, original packs and cargo drop exactly once",drops.sumOf{if(it.item.item===Eln.findItemStack(kind.title,1).item)it.item.count else 0}==1&&packs.size==kind.packs&&packs.all{RoadVehicles.battery(it.item)!!.getEnergy(it.item)==222222.0}&&drops.sumOf{if(it.item.`is`(Items.DIAMOND))it.item.count else 0}==7,"")
            c.check("road removal: dead owners cannot refund again",!e.hurtServer(c.level(),c.level().damageSources().generic(),100f)&&c.level().getEntitiesOfClass(ItemEntity::class.java,area).size==drops.size,"")
        }finally{cleanup()}}
    }
    private class Graphics(val kind:RoadVehicleKind):Base(){
        override fun id()="forkpatch_road_vehicle_${kind.name.lowercase()}_native_geometry_and_clear_windows"
        override fun measure(c:TestContext){
            for(yaw in listOf(0f,90f,180f,270f)){
                GlFrame.beginRenderFrame();val f=GlFrame.open()
                try{
                    mods.eln.gl.GL11.glRotatef(-yaw,0f,1f,0f);VehicleModels.draw(kind,.5,true,45f)
                    val bounds=mods.eln.client.entity.RoadVehicleRenderer.localBounds(kind,yaw)
                    c.check("road graphics: source body, wheels and glazing fit native culling bounds",f.batches().values.sumOf{it.vertices()}>1000&&f.batches().values.all{b->(0 until b.floats step GlFrame.STRIDE).all{i->bounds.contains(b.data[i].toDouble(),b.data[i+1].toDouble(),b.data[i+2].toDouble())}},"$kind yaw=$yaw")
                    val glass=f.batches().entries.filter{it.key.blend()==GlFrame.Blend.ALPHA}
                    c.check("road glazing: original glass uses a transparent pass with source16percent alpha",glass.isNotEmpty()&&glass.any{(_,b)->(0 until b.floats step GlFrame.STRIDE).any{i->java.lang.Float.floatToRawIntBits(b.data[i+5]) ushr 24==41}},"")
                }finally{GlFrame.close()}
            }
        }
    }
    private class Guards:Base(){
        override fun id()="forkpatch_road_vehicle_station_lead_range_ownership_and_drive_interlock"
        override fun measure(c:TestContext){try{
            val s=station(c);val car=vehicle(c,RoadVehicleKind.CART,x=8,z=4);install(car);val p=player(c)
            val lead=Eln.findItemStack(VehicleLeadItem.TITLE,1);p.setItemInHand(InteractionHand.MAIN_HAND,lead)
            check(s.onBlockActivated(p,Direction.YP,0f,0f,0f));car.interact(p,InteractionHand.MAIN_HAND,Vec3.ZERO)
            c.check("vehicle lead: actual block then entity interaction claims one loaded station",car.plugged&&s.vehicle()===car&&lead.count==1&&VehicleChargers.connected(car),"")
            c.check("vehicle station: a20A appliance cord cannot substitute for100A hardware",CordSupport.device(s)==null,"")
            car.interact(p,InteractionHand.OFF_HAND,Vec3.ZERO)
            c.check("vehicle charge interlock: plugged cars cannot accept a driver",!p.startRiding(car,false,false)&&car.controllingPassenger==null,"")
            val duplicate=station(c,x=4,z=10);val tag=CompoundTag();s.writeToNBT(tag);duplicate.readFromNBT(tag)
            val other=vehicle(c,RoadVehicleKind.CART,x=8,z=10)
            c.check("vehicle station identity: a cloned saved UUID cannot steal a resident owner",!duplicate.attach(other)&&VehicleChargers.find(s.identity)===s&&s.vehicle()===car,"")
            p.isShiftKeyDown=true;car.interact(p,InteractionHand.MAIN_HAND,Vec3.ZERO);p.isShiftKeyDown=false
            c.check("vehicle lead: source sneak interaction releases both sides",!car.plugged&&s.vehicle()==null,"")
            val far=vehicle(c,RoadVehicleKind.CART,x=20,z=20)
            c.check("vehicle lead: reach and passenger guards refuse unsafe attachment",!s.attach(far),"")
            mount(car,p);c.check("vehicle station: occupied cars refuse a charging claim",!s.attach(car),"");p.stopRiding()
            val old=s.node.coordonate.dimention
            try{s.node.coordonate.dimention=old xor 0x40000000;c.check("vehicle station: foreign coordinate owners cannot provide a live charging endpoint",!s.attach(car),"")}finally{s.node.coordonate.dimention=old}
        }finally{cleanup()}}
    }
    private class Corner:Base(){
        override fun id()="forkpatch_road_vehicle_cart_dangerous_corner_hazard"
        override fun measure(c:TestContext){try{
            val e=vehicle(c,RoadVehicleKind.CART,x=6,z=6);install(e);val p=player(c);p.setPermanentlyInvulnerable(true);mount(e,p)
            drive(e,p,50);c.check("cart corner precondition: actual paid acceleration reaches hazard speed",e.speed>=7.5,"${e.speed}")
            repeat(12){if(e.isAlive){VehicleControls.handle(p,VehicleControls(1,1));e.tick()}}
            c.check("cart hazard: sustained fast sharp corner destroys the cart and ejects its occupants",!e.isAlive&&!p.isPassenger&&p.remainingFireTicks>0,"")
        }finally{cleanup()}}
    }
    private class Charge(val kind:RoadVehicleKind,val volts:Double):Base(){
        lateinit var car:RoadVehicleEntity;lateinit var s:VehicleChargerElement;lateinit var source:VoltageSource;lateinit var clock:IProcess
        var paid=0.0
        override fun id()="forkpatch_road_vehicle_${kind.name.lowercase()}_measured_charge_${if(volts<0)"negative_" else ""}${abs(volts).toInt()}v"
        override fun ticks()=80
        override fun build(c:TestContext){
            s=station(c);car=vehicle(c,kind,x=8,z=4);install(car,0.0);check(s.attach(car))
            source=VoltageSource("actual100A vehicle station input",s.input,null).setU(volts);Eln.simulator.addElectricalComponent(source)
            clock=IProcess{dt->paid+=abs(source.u*source.current)*dt};Eln.simulator.addElectricalProcess(clock)
        }
        override fun measure(c:TestContext){try{
            val valid=abs(volts) in 200.0..264.0
            if(valid){
                c.check("vehicle charging: measured accepted source energy reaches actual packs at94percent",paid>1000.0&&abs(energy(car)-paid*.94)<1e-4&&abs(s.stored-energy(car))<1e-6,"paid=$paid,stored=${energy(car)},meter=${s.stored}")
                c.check("vehicle charging:100A/24kW limits hold in the native solver",abs(s.sink.current)<=100.0000001&&s.sink.p<=24000.00001&&s.watts>1000,"I=${s.sink.current},P=${s.sink.p}")
                c.check("vehicle charging: equal empty bays share charge without hidden capacity",(0 until kind.packs).map{RoadVehicles.battery(car.items.getItem(it))!!.getEnergy(car.items.getItem(it))}.let{it.max()-it.min()<1e-6},"")
            }else c.check("vehicle charging: sag/overvoltage cannot create stored energy",energy(car)==0.0&&s.watts==0.0&&paid<1e-6,"paid=$paid,stored=${energy(car)}")
            c.check("vehicle charging: parked energy does not permit driving while connected",car.plugged&&car.speed==0.0&&!player(c).startRiding(car,false,false),"")
            mods.eln.client.ClientRenders.registerAll();val at=s.node.coordonate.pos();val tile=TransparentNodeEntity(at,c.level().getBlockState(at));tile.setLevel(c.level())
            try{val data=java.io.DataInputStream(s.node.publishPacket.toByteArray().inputStream());data.readByte();repeat(4){data.readInt()};data.readUTF();tile.serverPublishUnserialize(data);val render=tile.elementRender as? VehicleChargerRender;c.check("vehicle station publication: meters and actual vehicle ID reach the renderer",render!=null&&data.available()==0&&render.target==car.id&&render.stored==s.stored,"")}finally{tile.clientDestroy()}
        }finally{Eln.simulator.removeElectricalProcess(clock);Eln.simulator.removeElectricalComponent(source);source.breakConnection();cleanup()}}
    }
    private class ChargeChange:Base(){
        lateinit var car:RoadVehicleEntity;lateinit var s:VehicleChargerElement;lateinit var source:VoltageSource;lateinit var clock:IProcess
        var elapsed=0.0;var atChange=0.0;var changed=false
        override fun id()="forkpatch_road_vehicle_charging_voltage_step_fails_closed_before_accounting"
        override fun ticks()=80
        override fun build(c:TestContext){
            s=station(c);car=vehicle(c,RoadVehicleKind.COUPE,x=8,z=4);install(car,0.0);check(s.attach(car))
            source=VoltageSource("vehicle input transient",s.input,null).setU(240.0);Eln.simulator.addElectricalComponent(source)
            clock=IProcess{dt->elapsed+=dt;if(!changed&&elapsed>=1.0){atChange=energy(car);changed=true;source.u=300.0}}
            Eln.simulator.addElectricalProcess(clock)
        }
        override fun measure(c:TestContext){try{
            c.check("vehicle charging transient: unsafe accepted voltage cannot add energy",changed&&atChange>1000&&energy(car)==atChange&&s.watts==0.0&&s.sink.r>=1e18,"before=$atChange,after=${energy(car)},P=${s.watts}")
            val before=energy(car);repeat(100){s.controller.rootSystemPreStepProcess()}
            c.check("vehicle charging retries: command probes never advance battery work",energy(car)==before,"")
        }finally{Eln.simulator.removeElectricalProcess(clock);Eln.simulator.removeElectricalComponent(source);source.breakConnection();cleanup()}}
    }
    private class WiredStation:Base(){
        lateinit var car:RoadVehicleEntity;lateinit var s:VehicleChargerElement
        override fun id()="forkpatch_road_vehicle_real_placed_source_and_station_ports"
        override fun ticks()=100
        override fun build(c:TestContext){
            s=station(c);car=vehicle(c,RoadVehicleKind.CART,x=8,z=4);install(car,0.0)
            val at=s.node.coordonate.pos().relative(s.front.left().toEnumFacing());c.floor(at.below())
            val source=c.placeSix(192,at) as mods.eln.sixnode.electricalsource.ElectricalSourceElement
            source.networkUnserialize(c.stream{it.writeByte(1);it.writeFloat(240f)});check(s.attach(car))
        }
        override fun measure(c:TestContext){try{
            c.check("vehicle station ports: an actual placed native240V source feeds charging",abs(s.input.u)>239.0&&s.watts>11000.0&&energy(car)>10000.0,"U=${s.input.u},P=${s.watts},E=${energy(car)}")
        }finally{cleanup()}}
    }
    private class MixedBays:Base(){
        override fun id()="forkpatch_road_vehicle_full_and_near_full_bays_redistribute_measured_charge"
        override fun measure(c:TestContext){try{
            val e=vehicle(c,RoadVehicleKind.COUPE);install(e,0.0)
            e.items.setItem(0,pack(72_000_000.0));e.items.setItem(1,pack(71_999_999.0))
            val before=energy(e);val stored=e.acceptCharge(1200.0,.05)
            val values=(0..5).map{RoadVehicles.battery(e.items.getItem(it))!!.getEnergy(e.items.getItem(it))}
            c.check("vehicle mixed bays: measured energy is conserved with source efficiency",abs(stored-1128.0)<1e-9&&abs(energy(e)-before-1128.0)<1e-7,"stored=$stored,delta=${energy(e)-before}")
            c.check("vehicle mixed bays: full bays remain bounded and spare input reaches other packs",values[0]==72_000_000.0&&values[1]==72_000_000.0&&values.drop(2).all{abs(it-281.75)<1e-9},"$values")
            val safe=energy(e)
            c.check("vehicle charge guards: malformed work cannot change stored energy",e.acceptCharge(Double.NaN,.05)==0.0&&e.acceptCharge(1200.0,Double.NaN)==0.0&&e.acceptCharge(-1.0,.05)==0.0&&energy(e)==safe,"")
        }finally{cleanup()}}
    }

}
