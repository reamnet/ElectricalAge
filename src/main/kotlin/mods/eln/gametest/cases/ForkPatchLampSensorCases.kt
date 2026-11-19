package mods.eln.gametest.cases

import mods.eln.Eln
import mods.eln.gametest.*
import mods.eln.misc.Direction
import mods.eln.sixnode.lampsocket.*
import mods.eln.sim.mna.component.VoltageSource
import net.minecraft.core.BlockPos
import net.minecraft.world.entity.decoration.ArmorStand
import net.minecraft.world.entity.EntityType
import net.minecraft.world.entity.EntitySpawnReason
import net.minecraft.world.level.block.Blocks
import net.minecraft.world.phys.Vec3
import kotlin.math.abs

/** Real resident entity queries and collision/LOS; no global world-clock mutations. */
object ForkPatchLampSensorCases {
    @JvmStatic fun cases(): List<TestCase> = listOf(MotionScene(),IndustrialBladeScene())
    private open class Base:TestCase {
        override fun width()=24;override fun height()=12;override fun depth()=24;override fun floorDepth()=24
        override fun build(c: TestContext) {}
        override fun measure(c: TestContext) {}
    }
    private class MotionScene:Base() {
        private lateinit var lamp: DecorativeLampElement
        override fun id()="forkpatch_motion_actual_entity_los_hold_and_electrical_demand"
        override fun build(c: TestContext) {
            val pos=c.at(8,5,8);c.floor(pos.west())
            lamp=c.placeSix(DecorativeLampStyle.MOTION.lookup,pos,Direction.XN) as DecorativeLampElement
            lamp.inventory.setItem(0,Eln.findItemStack("120V LED Light Bulb",2));lamp.inventoryChange(lamp.inventory)
            mode(c,3)
        }
        private fun mode(c: TestContext,value: Int)=lamp.networkUnserialize(c.stream {it.writeByte(DecorativeLampElement.MOTION_MODE);it.writeByte(value)})
        override fun measure(c: TestContext) {
            val world=c.level();val style=DecorativeLampStyle.MOTION
            val mount=Vec3(lamp.coordonate.x+.5,lamp.coordonate.y+.5,lamp.coordonate.z+.5)
            val origin=style.worldPoint(mount,lamp.side,lamp.front,Vec3(.55,-.12,0.0))
            val forward=style.worldDirection(lamp.side,lamp.front,Vec3(1.0,0.0,0.0))
            val stand=ArmorStand(world,origin.x,origin.y,origin.z).apply {setNoGravity(true)}
            fun eye(point: Vec3) {stand.setPos(point.x,point.y-stand.eyeHeight,point.z)}
            val front=origin.add(forward.scale(3.0))
            eye(front);check(world.addFreshEntity(stand))
            val obstacle=BlockPos.containing(origin.add(forward.scale(1.5)))
            val previous=world.getBlockState(obstacle)
            try {
                mode(c,1)
                lamp.beforePhysicalStep(.3)
                c.check("motion real front entity switches on both paid bulb resistances",lamp.isLampEnergized && abs(lamp.lampResistor.r-120.0*120/30)<1e-9,"energized=${lamp.isLampEnergized},R=${lamp.lampResistor.r}")
                val hold=MotionLightControl()
                c.check("motion real resident front entity is detected",hold.update(lamp,.25),"")
                repeat(119) {c.check("motion stationary entity preserves hold before physical30s expiry",hold.update(lamp,.25),"step=$it")}
                c.check("motion stationary entity does not refresh expired30s hold",!hold.update(lamp,.25),"")
                lamp.beforePhysicalStep(29.5)
                c.check("motion actual demand remains during physical hold",lamp.isLampEnergized,"")
                lamp.beforePhysicalStep(.25)
                c.check("motion actual demand opens after physical30s stationary hold",!lamp.isLampEnergized && lamp.lampResistor.r.isInfinite(),"R=${lamp.lampResistor.r}")
                eye(origin.subtract(forward.scale(3.0)))
                c.check("motion real entity behind sensor is ignored",!MotionLightControl().update(lamp,.25),"")
                eye(origin.add(forward.scale(9.0)))
                c.check("motion real entity beyond8m is ignored",!MotionLightControl().update(lamp,.25),"")
                eye(front)
                world.setBlock(obstacle,Blocks.STONE.defaultBlockState(),3)
                c.check("motion actual solid occluder suppresses resident entity LOS",!MotionLightControl().update(lamp,.25),obstacle.toString())
                world.setBlock(obstacle,previous,3)
                c.check("motion clearing real occluder restores detection",MotionLightControl().update(lamp,.25),"")
                stand.discard();mode(c,2)
                c.check("motion always-on mode creates demand without an entity",lamp.isLampEnergized && lamp.lampResistor.r.isFinite(),"")
                mode(c,3)
                c.check("motion always-off mode opens actual demand",!lamp.isLampEnergized && lamp.lampResistor.r.isInfinite(),"")
            } finally {stand.discard();world.setBlock(obstacle,previous,3);mode(c,3)}
        }
    }
    private class IndustrialBladeScene:Base() {
        private lateinit var lamp: DecorativeLampElement
        private lateinit var source: VoltageSource
        override fun id()="forkpatch_industrial_fan_actual_entity_blade_damage_and_stopped_safety"
        override fun ticks()=120
        override fun build(c: TestContext) {
            val pos=c.at(8,6,8);c.floor(pos.above())
            lamp=c.placeSix(DecorativeLampStyle.FAN_INDUSTRIAL.lookup,pos,Direction.YP) as DecorativeLampElement
            if(lamp.isWirelessPower)lamp.networkUnserialize(c.stream {it.writeByte(3)})
            source=VoltageSource("blade safety paid motor",lamp.positiveLoad,null).setU(120.0)
            Eln.simulator.addElectricalComponent(source)
        }
        override fun measure(c: TestContext) {
            val world=c.level();val style=DecorativeLampStyle.FAN_INDUSTRIAL
            val center=style.worldPoint(Vec3(lamp.coordonate.x+.5,lamp.coordonate.y+.5,lamp.coordonate.z+.5),lamp.side,lamp.front,Vec3(.4575,0.0,0.0))
            val created=mutableListOf<net.minecraft.world.entity.Entity>()
            try {
            val inside=(requireNotNull(net.minecraft.core.registries.BuiltInRegistries.ENTITY_TYPE.getValue(net.minecraft.resources.Identifier.withDefaultNamespace("pig"))).create(world,EntitySpawnReason.COMMAND) as net.minecraft.world.entity.Mob).also {created+=it}.apply {setNoGravity(true);setNoAi(true);setPos(center.x,center.y-.4,center.z)}
            val outside=(requireNotNull(net.minecraft.core.registries.BuiltInRegistries.ENTITY_TYPE.getValue(net.minecraft.resources.Identifier.withDefaultNamespace("pig"))).create(world,EntitySpawnReason.COMMAND) as net.minecraft.world.entity.Mob).also {created+=it}.apply {setNoGravity(true);setNoAi(true);setPos(center.x+3.0,center.y-.4,center.z)}
                check(world.addFreshEntity(inside));check(world.addFreshEntity(outside))
                c.check("blade hazard precondition is genuinely powered motor",lamp.fanPower>.99 && abs(lamp.fanMotor.p-300.0)<1e-4,"speed=${lamp.fanPower},W=${lamp.fanMotor.p}")
                val hazard=CeilingFanHazard(lamp);val before=inside.health;val outsideBefore=outside.health
                hazard.tick()
                val after=inside.health
                c.check("blade disk hits actual intersecting entity for source speed-squared damage",abs((before-after)-2.5*lamp.fanPower*lamp.fanPower)<1e-4,"before=$before,after=$after,speed=${lamp.fanPower}")
                c.check("blade disk spares actual entity outside radius",outside.health==outsideBefore,"")
                repeat(20) {hazard.tick()}
                c.check("blade repeated polling in same world tick cannot add damage",inside.health==after,"${inside.health}")
                lamp.networkUnserialize(c.stream {it.writeByte(DecorativeLampElement.FAN_SWITCH)})
                lamp.afterPhysicalStep(10.0)
                c.check("industrial rotor coasts to stopped state after source10s",lamp.fanPower==0.0,"${lamp.fanPower}")
                hazard.tick()
                c.check("stopped blade disk inflicts no new damage",inside.health==after && outside.health==outsideBefore,"")
            } finally {created.forEach {it.discard()};Eln.simulator.removeElectricalComponent(source);source.breakConnection()}
        }
    }
}
