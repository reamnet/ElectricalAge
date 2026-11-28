package mods.eln.utility.modular

import mods.eln.Eln
import mods.eln.client.gui.ElnScreen
import mods.eln.ghost.GhostBlock
import mods.eln.ghost.GhostGroup
import mods.eln.gl.GL11.*
import mods.eln.gridnode.AimedGridTerminals
import mods.eln.gridnode.GridDescriptor
import mods.eln.gridnode.GridRender
import mods.eln.gui.ElnNodeMenu
import mods.eln.i18n.I18N.tr
import mods.eln.misc.Direction
import mods.eln.misc.LRDU
import mods.eln.misc.Utils
import mods.eln.node.NodePeriodicPublishProcess
import mods.eln.node.transparent.*
import mods.eln.registry.ElnDeviceRegistry
import mods.eln.sim.IProcess
import mods.eln.sim.mna.component.Resistor
import mods.eln.sim.nbt.NbtElectricalLoad
import mods.eln.sound.SoundCommand
import mods.eln.transparentnode.standby.TransferContact
import mods.eln.utility.RecloserControl
import net.minecraft.client.gui.GuiGraphicsExtractor
import net.minecraft.nbt.CompoundTag
import net.minecraft.network.chat.Component
import net.minecraft.server.level.ServerLevel
import net.minecraft.world.entity.player.Inventory
import net.minecraft.world.entity.player.Player
import net.minecraft.world.item.ItemStack
import net.minecraft.world.phys.AABB
import net.minecraft.world.phys.Vec3
import java.io.DataInputStream
import java.io.DataOutputStream
import kotlin.math.abs
import kotlin.math.ceil

enum class ModularRecloserKind(val title: String,val phases: Int,val voltage: Int,val amps: Int,
                               val width: Double,val terminalHeight: Double,val spacing: Double) {
    SINGLE("Single Phase Substation Recloser",1,12500,450,.59,.18,0.0),
    THREE("Three Phase Substation Recloser",3,69000,900,.87,.49,.59),
    HEAVY("Heavy Duty Substation Recloser",3,230000,2000,1.20,.86,.90);
    val id get()=14108+ordinal
}

class ModularRecloserDescriptor(val kind: ModularRecloserKind) :
    GridDescriptor(kind.title,SubstationStock.model,ModularRecloserElement::class.java,
        ModularRecloserRender::class.java,"textures/wire.png",ElnDeviceRegistry.veryHighVoltageCableDescriptor) {
    internal val terminals=EquipmentTerminals(kind.phases,kind.width,kind.terminalHeight,kind.spacing)
    init {
        ghostGroup=GhostGroup().apply {
            val x=ceil(kind.width+.11-.5).toInt()
            val z=ceil(kind.spacing+.29-.5).toInt()
            for(i in -x..x)for(j in 0..ceil(kind.terminalHeight+.11-.5).toInt())for(k in -z..z)
                if(i!=0 || j!=0 || k!=0)addElement(i,j,k,Eln.ghostBlock,GhostBlock.tSelectableClearance)
        }
    }

    override fun rotationIsFixed()=true
    override fun isFloor(below: net.minecraft.world.level.block.Block) =
        SubstationSupports.equipmentFloor(below) || super.isFloor(below)

    fun drawRecloser(front: Direction,closed: Boolean,locked: Boolean) {
        glPushMatrix()
        try {
            front.glRotateXnRef()
            val x=kind.width;val y=kind.terminalHeight;val z=kind.spacing
            SubstationStock.draw {
                for(side in listOf(-1,1)) {
                    box(-x,-.5,side*(z+.21)-.05,x,-.4,side*(z+.21)+.05,0x4a5d67)
                    box(side*x*.61-.048,-.4,-z-.17,side*x*.61+.048,-.24,z+.17,0x7c898a)
                }
                repeat(kind.phases) { i ->
                    val lane=terminals.lane(i)
                    val tankRadius=.14+y*.046
                    box(-x*.52,y-tankRadius-.070,lane-tankRadius,x*.52,y+tankRadius*.65,lane+tankRadius,0x59717d)
                    for(side in listOf(-1,1)) {
                        val edge=side*x
                        val end=side*x*.52
                        val lo=minOf(edge,end);val hi=maxOf(edge,end)
                        box(lo,y-.057,lane-.057,hi,y+.057,lane+.057,0x9cb2b2)
                        repeat(if(kind==ModularRecloserKind.HEAVY)6 else 3) { r ->
                            val at=end+(edge-end)*(.19+r*.115)
                            box(at-.016,y-.106,lane-.104,at+.016,y+.102,lane+.104,0x758f95)
                        }
                        box(edge-.071,y-.073,lane-.071,edge+.071,y+.069,lane+.071,0xb19260)
                    }
                    box(-.055,y+tankRadius*.62,lane-.08,.055,y+tankRadius*.82,lane+.08,0x516470)
                }
                val cabinet=z+.22
                box(-.30,-.41,cabinet-.12,.12,-.03,cabinet+.06,0x4b6067)
                box(-.283,-.392,cabinet+.061,.102,-.047,cabinet+.076,0x6c7f83)
                box(-.22,-.170,cabinet+.077,-.16,-.105,cabinet+.092,when {
                    locked->0xf0aa43
                    closed->0x48c787
                    else->0xe45d4d
                })
                box(.02,-.33,cabinet+.076,.055,-.18,cabinet+.107,0x242c31)
            }
        } finally { glPopMatrix() }
    }

    override fun drawItem(entity: Boolean) {
        glPushMatrix()
        try {
            val scale=.67/kind.width
            glScaled(scale,scale,scale)
            glRotated(-33.0,0.0,1.0,0.0)
            drawRecloser(Direction.XN,true,false)
        } finally { glPopMatrix() }
    }

    override fun addInformation(stack: ItemStack,player: Player?,list: MutableList<String>,advanced: Boolean) {
        list.add(tr("Rated %1$ V, adjustable pickup to %2$ A",kind.voltage,kind.amps))
        list.add(tr("Interrupts overcurrent. Persistent faults lock out automatic closing."))
        list.add(tr("Fit rigid busbars to the marked sockets on either side or on top."))
    }
}

class ModularRecloserElement(node: TransparentNode, descriptor: TransparentNodeDescriptor) :
    RigidEquipment(node,descriptor), AimedGridTerminals {
    private val spec=descriptor as ModularRecloserDescriptor
    private val kind=spec.kind
    internal override val terms=spec.terminals
    private val inventory=TransparentNodeElementInventory(0,1,this)
    override fun getInventory()=inventory
    val inputs=List(kind.phases){NbtElectricalLoad("yardRecloserIn$it").apply { rs=.00001 }}
    val outputs=List(kind.phases){NbtElectricalLoad("yardRecloserOut$it").apply { rs=.00001 }}
    private val contacts=inputs.zip(outputs,::TransferContact)
    val control=RecloserControl().apply { pickupAmps=kind.amps*.5;pickupDelay=.15;setManual(false) }
    var tripReason=0
        private set
    private val sample=DoubleArray(kind.phases)
    val peak get()=contacts.maxOf { abs(it.current) }

    init {
        electricalLoadList.addAll(inputs+outputs)
        for(pin in inputs+outputs)electricalComponentList.add(Resistor(pin,null).apply { r=1e12 })
        electricalComponentList.addAll(contacts)
        electricalProcessList.add(IProcess { dt->
            for(i in contacts.indices)sample[i]=contacts[i].current
            if(control.step(dt,sample.asList())) {
                if(!control.closed)tripReason=if(control.locked)2 else 1
                contactChanged()
            }
        })
        slowProcessList.add(NodePeriodicPublishProcess(node,.5,.15))
        applyContacts()
    }

    private fun applyContacts() { contacts.forEach { it.closed=control.closed } }
    private fun contactChanged() {
        applyContacts()
        needPublish()
        if(coordonate().world() is ServerLevel)
            play(SoundCommand("eln:utility_recloser_bump").smallRange().mulVolume(.55))
    }
    fun configure(pickup: Double,delay: Double,seconds: Double,attempts: Int,automatic: Boolean): Boolean {
        if(!pickup.isFinite() || pickup !in 1.0..kind.amps.toDouble() || !delay.isFinite() || delay !in .05..2.0 ||
            !seconds.isFinite() || seconds !in 1.0..60.0 || attempts !in 1..5)return false
        control.pickupAmps=pickup;control.pickupDelay=delay;control.retrySeconds=seconds
        control.maxFailures=attempts;control.automatic=automatic
        needPublish()
        return true
    }
    fun setClosed(value: Boolean): Boolean {
        if(!control.setManual(value))return false
        contactChanged()
        return true
    }
    fun reset() {
        control.reset()
        tripReason=0
        contactChanged()
    }

    override fun gridTerminalAt(side: Direction,point: Vec3)=terms.closest(point,front)
    override fun gridBundleFor(terminal: Int)=terms.bundle(terminal)
    override fun getGridElectricalLoad(side: Direction)=inputs.first()
    override fun gridLoads(terminal: Int): List<NbtElectricalLoad> {
        val bank=when(terms.bank(terminal)) { 0->inputs;1->outputs;else->return emptyList() }
        return terms.phase(terminal)?.let { listOf(bank[it]) } ?: bank.takeIf { terms.count(terminal)==3 }.orEmpty()
    }
    override fun gridPhase(terminal: Int)=terms.phase(terminal).takeIf { kind.phases>1 }
    override fun gridCircuitKey(terminal: Int)=terms.circuitKey(terminal)
    override fun phaseBusTerminals(terminal: Int)=if(kind.phases==1 && terms.count(terminal)==1)listOf(32,36) else listOf(terminal)
    override fun renderWireCount(terminal: Int)=terms.count(terminal)
    override fun gridCablePoint(terminal: Int,wire: Int)=terms.point(terminal,wire)
    override fun renderedGridCablePoint(terminal: Int,wire: Int)=front.rotateFromXN(gridCablePoint(terminal,wire))
    override fun getConnectionMask(side: Direction,lrdu: LRDU)=0
    override fun getElectricalLoad(side: Direction,lrdu: LRDU)=null
    override fun getThermalLoad(side: Direction,lrdu: LRDU)=null
    override fun hasGui()=true
    override fun multiMeterString(side: Direction)=Utils.plotUIP(outputs.first().u,peak)
    override fun thermoMeterString(side: Direction)=""
    override fun getWaila()=linkedMapOf(tr("Protection") to recloserStatus(control.closed,control.locked),
        tr("Peak phase current") to Utils.plotAmpere("",peak),tr("Reclose attempts") to control.attempts.toString(),
        tr("Last fault") to recloserReason(tripReason))

    override fun writeToNBT(tag: CompoundTag) {
        super.writeToNBT(tag)
        control.writeYard(tag)
        tag.putInt("yardTripReason",tripReason)
    }
    override fun readFromNBT(tag: CompoundTag) {
        super.readFromNBT(tag)
        control.readYard(tag)
        control.pickupAmps=control.pickupAmps.coerceAtMost(kind.amps.toDouble())
        tripReason=tag.getIntOr("yardTripReason",0).coerceIn(0,2)
        applyContacts()
    }
    override fun networkSerialize(out: DataOutputStream) {
        super.networkSerialize(out)
        out.writeBoolean(control.closed);out.writeBoolean(control.locked);out.writeBoolean(control.automatic)
        out.writeFloat(control.pickupAmps.toFloat());out.writeFloat(control.pickupDelay.toFloat())
        out.writeFloat(control.retrySeconds.toFloat());out.writeByte(control.maxFailures)
        out.writeByte(control.attempts);out.writeByte(tripReason);out.writeDouble(outputs[0].u);out.writeDouble(peak)
    }
    override fun networkUnserialize(input: DataInputStream): Byte {
        val id=super.networkUnserialize(input)
        val pickup=control.pickupAmps;val delay=control.pickupDelay;val retry=control.retrySeconds
        val max=control.maxFailures;val auto=control.automatic
        when(id.toInt()) {
            80->setClosed(!control.closed)
            81->reset()
            82->configure(pickup,delay,retry,max,!auto)
            83->configure(if (pickup+kind.amps*.1>kind.amps)kind.amps*.1 else pickup+kind.amps*.1,delay,retry,max,auto)
            84->configure(pickup,listOf(.05,.15,.5,1.0,2.0).firstOrNull { it>delay } ?: .05,retry,max,auto)
            85->configure(pickup,delay,listOf(1.0,3.0,5.0,15.0).firstOrNull { it>retry } ?: 1.0,max,auto)
            86->configure(pickup,delay,retry,max%5+1,auto)
        }
        return id
    }
}

private fun recloserStatus(closed: Boolean,locked: Boolean) =
    if(locked)tr("Locked out") else if(closed)tr("Closed") else tr("Open")

private fun recloserReason(reason: Int) = when(reason) {
    1->tr("Overcurrent")
    2->tr("Repeated overcurrent")
    else->tr("None")
}

class ModularRecloserRender(entity: TransparentNodeEntity,descriptor: TransparentNodeDescriptor) : GridRender(entity,descriptor) {
    var closed=false;var locked=false;var automatic=true
    var pickup=0f;var delay=0f;var retry=0f;var max=0;var attempts=0;var reason=0
    var volts=0.0;var amps=0.0
    override fun networkUnserialize(input: DataInputStream) {
        super.networkUnserialize(input)
        closed=input.readBoolean();locked=input.readBoolean();automatic=input.readBoolean()
        pickup=input.readFloat();delay=input.readFloat();retry=input.readFloat()
        max=input.readUnsignedByte();attempts=input.readUnsignedByte();reason=input.readUnsignedByte()
        volts=input.readDouble();amps=input.readDouble()
    }
    override fun renderBounds(): AABB {
        val box=AABB(tileEntity.blockPos).inflate(3.0)
        return super.renderBounds()?.minmax(box) ?: box
    }
    override fun draw() {
        drawCables()
        val equipment = transparentNodedescriptor as ModularRecloserDescriptor
        equipment.drawRecloser(front,closed,locked)
        RigidEquipment.drawSockets(equipment, equipment.terminals, front)
    }
}

class ModularRecloserScreen(menu: ElnNodeMenu,inv: Inventory,title: Component,render: Any) : ElnScreen(menu,inv,title,248,179) {
    private val view=render as ModularRecloserRender
    override fun init() {
        super.init()
        button(8,25,113,{if(view.closed)tr("Open") else tr("Close")},{view.clientSendId(80)})
        button(127,25,113,{tr("Reset lockout")},{view.clientSendId(81)})
        button(8,52,113,{if(view.automatic)tr("Auto reclose on") else tr("Auto reclose off")},{view.clientSendId(82)})
        button(127,52,113,{tr("Pickup %1$ A",view.pickup.toInt())},{view.clientSendId(83)})
        button(8,79,113,{tr("Trip delay %1$ s",view.delay)},{view.clientSendId(84)})
        button(127,79,113,{tr("Retry %1$ s",view.retry)},{view.clientSendId(85)})
        button(8,106,232,{tr("Maximum retries: %1$",view.max)},{view.clientSendId(86)})
    }
    override fun extractDevice(g: GuiGraphicsExtractor,mx: Int,my: Int,seconds: Float) {
        label(g,8,133,tr("%1$   Attempts %2$",recloserStatus(view.closed,view.locked),view.attempts))
        label(g,8,145,tr("Last fault: %1$",recloserReason(view.reason)))
        label(g,8,157,tr("%1$   %2$",Utils.plotVolt("",view.volts),Utils.plotAmpere("",view.amps)))
    }
}

object ModularReclosers {
    @JvmStatic fun register() = ModularRecloserKind.entries.forEach {
        Eln.transparentNodeItem.addDescriptor(it.id,ModularRecloserDescriptor(it))
    }
}
