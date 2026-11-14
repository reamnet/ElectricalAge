package mods.eln.transparentnode.renewable

import mods.eln.Eln
import mods.eln.i18n.I18N.tr
import mods.eln.misc.*
import mods.eln.node.NodeBase
import mods.eln.node.NodePeriodicPublishProcess
import mods.eln.node.transparent.*
import mods.eln.sim.IProcess
import mods.eln.sim.mna.component.Resistor
import mods.eln.sim.mna.component.SwitchableVoltageSource
import mods.eln.sim.nbt.NbtElectricalLoad
import mods.eln.sim.power.*
import net.minecraft.core.BlockPos
import net.minecraft.nbt.CompoundTag
import net.minecraft.tags.FluidTags
import java.io.DataOutputStream
import kotlin.math.*

class LargeRenewableElement(node: TransparentNode,descriptor: TransparentNodeDescriptor) : TransparentNodeElement(node,descriptor) {
    private val water=(descriptor as LargeRenewableDescriptor).water
    val output=NbtElectricalLoad("renewableOutput").apply { rs=.000001 }
    private val internal=NbtElectricalLoad("renewableInternal").apply { rs=.000001 }
    val source=SwitchableVoltageSource("renewableGenerator").apply { connectTo(internal,null) }
    var availableWatts=0.0
        private set
    var environment=0.0
        private set
    val controller=RenewableSupply(source) { availableWatts }
    private var timeout=0.0
    private val slots=TransparentNodeElementInventory(0,1,this)
    override fun getInventory()=slots
    init {
        electricalLoadList.addAll(listOf(output,internal))
        electricalComponentList.addAll(listOf(source,Resistor(internal,output).setR(OUTPUT_RESISTANCE),Resistor(output,null).setR(1e10)))
        slowProcessList.add(NodePeriodicPublishProcess(node,.5,0.0))
        slowProcessList.add(IProcess { seconds -> timeout-=seconds;if(timeout<=0) { timeout=2.0;sampleEnvironment() } })
    }
    private fun offset(x: Int,y: Int,z: Int): BlockPos {
        val c=Coordonate(x,y,z,0);c.applyTransformation(front,node.coordonate);return BlockPos(c.x,c.y,c.z)
    }
    fun sampleEnvironment() {
        val world=node.coordonate.world() as? net.minecraft.server.level.ServerLevel ?: return
        if(water) {
            var moving=0
            // Canonical -X front; the wheel is behind the control pedestal. These
            // water cells are outside ghost clearance, so flowing water remains visible.
            for(z in listOf(-3,0,3)) {
                val p=offset(2,0,z)
                if(world.chunkSource.getChunkNow(p.x shr 4,p.z shr 4)!=null) {
                    val fluid=world.getFluidState(p)
                    if(fluid.`is`(FluidTags.WATER) && !fluid.isSource && !fluid.isEmpty)moving++
                }
            }
            environment=moving/3.0;availableWatts=10_000.0*environment
        } else {
            var clear=0
            // Sparse open-air samples, not a volume scan and never a chunk load.
            for(y in listOf(6,9,12))for(z in listOf(-3,0,3)) {
                val p=offset(-3,y,z)
                if(world.chunkSource.getChunkNow(p.x shr 4,p.z shr 4)!=null && world.canSeeSky(p) && world.getBlockState(p).isAir)clear++
            }
            val heightFactor=((node.coordonate.y+74)/128.0).coerceIn(.5,1.5)
            environment=(Eln.wind.getWind(100)*heightFactor+world.getRainLevel(0f)+2*world.getThunderLevel(0f)).takeIf { it.isFinite() }?.coerceAtLeast(0.0) ?: 0.0
            availableWatts=windPower(environment,clear/9.0)
        }
        needPublish()
    }
    override fun initialize() { sampleEnvironment();connect() }
    override fun connectJob() { super.connectJob();Eln.simulator.mna.addProcess(controller) }
    override fun disconnectJob() { Eln.simulator.mna.removeProcess(controller);source.enabled=false;super.disconnectJob() }
    override fun getElectricalLoad(side: Direction,lrdu: LRDU)=if(side==front && lrdu==LRDU.Down)output else null
    override fun getConnectionMask(side: Direction,lrdu: LRDU)=if(getElectricalLoad(side,lrdu)!=null)NodeBase.maskElectricalPower else 0
    override fun getThermalLoad(side: Direction,lrdu: LRDU)=null
    override fun hasGui()=false
    override fun onBlockActivated(player: net.minecraft.world.entity.player.Player,side: Direction,x: Float,y: Float,z: Float)=false
    override fun thermoMeterString(side: Direction): String?=null
    override fun multiMeterString(side: Direction)=Utils.plotUIP(output.u,source.current)
    override fun networkSerialize(stream: DataOutputStream) {
        super.networkSerialize(stream);stream.writeDouble(environment);stream.writeDouble(availableWatts);stream.writeDouble(source.p.coerceAtLeast(0.0))
        node.lrduCubeMask.getTranslate(Direction.YN).serialize(stream)
    }
    override fun readFromNBT(nbt: CompoundTag) { super.readFromNBT(nbt);availableWatts=0.0;timeout=0.0;source.enabled=false }
    override fun getWaila()=linkedMapOf(tr("Rated voltage") to tr("3200 V"),tr("Available power") to Utils.plotPower("",availableWatts),tr("Produced power") to Utils.plotPower("",source.p.coerceAtLeast(0.0)))
    companion object {
        // One percent winding drop at the 20 kW / 3.2 kV module rating.
        // This keeps limited export away from the nearly shorted input branch.
        const val OUTPUT_RESISTANCE=5.12
        fun windPower(speed: Double,exposure: Double)=if(!speed.isFinite() || !exposure.isFinite() || speed<3 || speed>=25)0.0 else
            (20_000.0*(speed/12).pow(3)*exposure.coerceIn(0.0,1.0)).coerceIn(0.0,20_000.0)
    }
}
