package mods.eln.gametest.cases

import mods.eln.Eln
import mods.eln.gametest.*
import mods.eln.misc.*
import mods.eln.node.six.*
import mods.eln.sim.IProcess
import mods.eln.sim.mna.component.VoltageSource
import mods.eln.sixnode.lampsocket.*
import net.minecraft.core.BlockPos
import net.minecraft.world.level.block.Blocks
import java.io.*
import kotlin.math.abs

/** Actual owner packets, paid motor components, refreshed underwater leases and fresh-owner saves. */
object ForkPatchLampControlsCases {
    @JvmStatic fun cases(): List<TestCase> = listOf(Settings(),Motion(),Water(),Motor(false),Motor(true))
    private open class Base: TestCase {
        override fun id()="forkpatch_lamp_controls_base"
        override fun width()=24;override fun height()=12;override fun depth()=24;override fun floorDepth()=24
        override fun build(c: TestContext) {}
        override fun measure(c: TestContext) {}
        fun place(c: TestContext,s: DecorativeLampStyle,dx: Int=8): DecorativeLampElement {
            val side=if(s.ceiling)Direction.YP else Direction.XN
            val p=c.at(dx,6,8);c.floor(p.relative(side.toEnumFacing()))
            return c.placeSix(s.lookup,p,side) as DecorativeLampElement
        }
    }
    private class Settings: Base() {
        override fun id()="forkpatch_lamp_aim_suspension_control_packets_and_fresh_owner_save"
        override fun measure(c: TestContext) {
            var e=place(c,DecorativeLampStyle.EAVE_TWIN)
            val resistor=e.lampResistor;val load=e.positiveLoad
            e.networkUnserialize(c.stream { it.writeByte(4);it.writeUTF("native channel still event four") })
            e.networkUnserialize(c.stream { it.writeByte(DecorativeLampElement.AIM_FIRST+2);it.writeDouble(38.0) })
            e.networkUnserialize(c.stream { it.writeByte(DecorativeLampElement.AIM_FIRST+3);it.writeDouble(-52.0) })
            e.networkUnserialize(c.stream { it.writeByte(DecorativeLampElement.AIM_FIRST);it.writeDouble(Double.NaN) })
            c.check("lamp controls: native channel event four and independently aimed second head do not collide",e.channel=="native channel still event four" && e.aim.pan(1)==38.0 && e.aim.tilt(1)==-52.0 && e.aim.pan(0)==0.0,"")
            c.check("lamp controls: aim never replaces electrical components",e.lampResistor===resistor && e.positiveLoad===load,"")
            val old=e;e=(Reload.node(c.level(),e.coordonate.pos()).fresh as SixNode).getElement(old.side) as DecorativeLampElement
            c.check("lamp controls: actual reload preserves both head axes and channel",e!==old && e.aim.pan(1)==38.0 && e.aim.tilt(1)==-52.0 && e.channel=="native channel still event four","")
            var w=place(c,DecorativeLampStyle.WAREHOUSE_HIGH_BAY,15)
            w.networkUnserialize(c.stream { it.writeByte(DecorativeLampElement.SUSPENSION);it.writeDouble(3.36) })
            c.check("lamp controls: GUI packet quantizes suspension to paid source quarter-metre setting",w.suspension.drop==3.25,"")
            val previous=w;w=(Reload.node(c.level(),w.coordonate.pos()).fresh as SixNode).getElement(previous.side) as DecorativeLampElement
            c.check("lamp controls: actual warehouse reload retains extra drop",w!==previous && w.suspension.drop==3.25,"")
            for(f in listOf(e,w)) {
                val d=f.sixNodeElementDescriptor as DecorativeLampDescriptor
                val menu=mods.eln.gui.MenuSpecs.of(d)
                c.check("lamp controls: source-sized special panels reserve inventory beneath controls",menu.height()==d.panelHeight && menu.slots().single().y()==60,"${d.style.name}: ${menu.height()}")
            }
        }
    }
    private class Motion: Base() {
        override fun id()="forkpatch_motion_lamp_mode_channel_and_open_circuit_accounting"
        override fun measure(c: TestContext) {
            val e=place(c,DecorativeLampStyle.MOTION)
            e.inventory.setItem(0,Eln.findItemStack("120V LED Light Bulb",1));e.inventoryChange(e.inventory)
            e.networkUnserialize(c.stream { it.writeByte(4);it.writeUTF("motion channel") })
            e.networkUnserialize(c.stream { it.writeByte(DecorativeLampElement.MOTION_MODE);it.writeByte(2) })
            c.check("motion lamp: forced on retains exact installed bulb resistance",e.isLampEnergized && abs(e.lampResistor.r-960.0)<1e-9 && e.channel=="motion channel","")
            e.networkUnserialize(c.stream { it.writeByte(DecorativeLampElement.MOTION_MODE);it.writeByte(3) })
            c.check("motion lamp: forced off removes wired and wireless bulb demand",!e.isLampEnergized && e.lampResistor.r.isInfinite() && e.wirelessResistance.isInfinite(),"")
            val old=e;val saved=(Reload.node(c.level(),e.coordonate.pos()).fresh as SixNode).getElement(e.side) as DecorativeLampElement
            c.check("motion lamp: fresh owner restores mode without reinterpreting the native channel",saved!==old && saved.motionMode==3 && saved.channel=="motion channel","")
        }
    }
    private class Water: Base() {
        lateinit var p: BlockPos
        override fun id()="forkpatch_pool_light_water_preserving_lease_refresh_expiry_and_air"
        override fun build(c: TestContext) {
            p=c.at(4,2,4);c.level().setBlock(p,Blocks.WATER.defaultBlockState(),3)
            LightBlockEntity.addLight(c.level(),p.x,p.y,p.z,12,2)
            c.check("pool leases: source water becomes a lit waterlogged native lease",c.level().getBlockState(p).block is LightBlock && c.level().getBlockState(p).getValue(LightBlock.WATERLOGGED) && c.level().getFluidState(p).isSource,"")
            LightBlockEntity.addLight(c.level(),p.x,p.y,p.z,14,5)
            c.check("pool leases: refresh preserves the stored water",c.level().getFluidState(p).isSource,"")
        }
        override fun measure(c: TestContext) {
            c.check("pool leases: expired overlapping handles restore source water rather than deleting it",c.level().getBlockState(p).`is`(Blocks.WATER) && c.level().getFluidState(p).isSource,"${c.level().getBlockState(p)}")
            val air=c.at(10,2,10);LightBlockEntity.addLight(c.level(),air.x,air.y,air.z,0,5)
            c.check("pool leases: zero light creates no block",c.level().getBlockState(air).isAir,"")
            val lava=c.at(12,2,12);c.level().setBlock(lava,Blocks.LAVA.defaultBlockState(),3)
            LightBlockEntity.addLight(c.level(),lava.x,lava.y,lava.z,12,5)
            c.check("pool leases: unrelated fluids are never replaced",c.level().getBlockState(lava).`is`(Blocks.LAVA),"")
        }
    }
    private class Motor(val wireless: Boolean): Base() {
        lateinit var e: DecorativeLampElement
        lateinit var source: VoltageSource
        lateinit var clock: IProcess
        var peak=0.0
        var off=false
        var sawPaid=false
        var elapsed=0.0
        override fun ticks()=180
        override fun id()="forkpatch_ceiling_fan_${if(wireless)"wireless" else "wired"}_paid_motor_without_bulb_and_coast"
        override fun build(c: TestContext) {
            e=place(c,DecorativeLampStyle.FAN_CLASSIC)
            if(wireless) {
                val supply=c.placeSix(4162,c.at(3,1,3)) as mods.eln.sixnode.lampsupply.LampSupplyElement
                val channel="fan ${java.util.UUID.randomUUID()}";e.channel=channel
                supply.networkUnserialize(c.stream { it.writeByte(mods.eln.sixnode.lampsupply.LampSupplyElement.setPowerName.toInt());it.writeByte(0);it.writeUTF(channel) })
                source=VoltageSource("fan wireless",supply.powerLoad,null).setU(120.0)
            } else {
                e.networkUnserialize(c.stream { it.writeByte(3) })
                source=VoltageSource("fan wired",e.positiveLoad,null).setU(120.0)
            }
            Eln.simulator.addElectricalComponent(source)
            clock=IProcess { dt ->
                elapsed+=dt;peak=maxOf(peak,e.fanPower)
                if(elapsed>1 && !off && abs(abs(source.u*source.current)-55.0)<1e-4)sawPaid=true
                if(elapsed>5.5 && !off) {
                    off=true
                    e.networkUnserialize(c.stream { it.writeByte(DecorativeLampElement.FAN_SWITCH) })
                }
            }
            Eln.simulator.addSlowProcess(clock)
        }
        override fun measure(c: TestContext) { try {
            if(wireless) {
                c.check("ceiling fan: wireless bulb mode cannot spin an unconnected source motor",!sawPaid && peak==0.0 && abs(source.u*source.current)<1e-6 && e.installedBulbs==0,"")
                return
            }
            c.check("ceiling fan: no bulb is required for a paid 55W motor",sawPaid && e.installedBulbs==0,"paid=$sawPaid speed=${e.fanPower} elapsed=$elapsed")
            c.check("ceiling fan: five-second startup and physical coast survive opening the switch",peak>.99 && off && !e.fanEnabled && e.fanPower>0.0 && e.fanPower<peak,"peak=$peak speed=${e.fanPower} elapsed=$elapsed")
            c.check("ceiling fan: opened motor draws no power while rotor coasts",abs(source.u*source.current)<1e-6,"${source.u*source.current}")
        }finally { Eln.simulator.removeSlowProcess(clock);Eln.simulator.removeElectricalComponent(source);source.breakConnection() } }
    }
}
