package mods.eln.utility.modular

import mods.eln.Eln
import mods.eln.client.gui.ElnScreen
import mods.eln.ghost.GhostBlock
import mods.eln.ghost.GhostGroup
import mods.eln.gl.GL11.*
import mods.eln.gridnode.AimedGridTerminals
import mods.eln.gridnode.GridDescriptor
import mods.eln.gridnode.GridElement
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
import mods.eln.sim.nbt.NbtThermalLoad
import mods.eln.utility.ServiceTap
import mods.eln.utility.SubstationProtection
import net.minecraft.client.gui.GuiGraphicsExtractor
import net.minecraft.nbt.CompoundTag
import net.minecraft.network.chat.Component
import net.minecraft.world.entity.player.Inventory
import net.minecraft.world.entity.player.Player
import net.minecraft.world.item.ItemStack
import net.minecraft.world.phys.AABB
import net.minecraft.world.phys.Vec3
import java.io.DataInputStream
import java.io.DataOutputStream
import java.util.Locale
import kotlin.math.abs
import kotlin.math.ceil

private fun formatRatio(value: Double) = String.format(Locale.ROOT, "%.4f", value)

internal val yardVoltages=listOf(230,400,800,3200,11000,12500,69000,230000)

enum class ModularTransformerKind(val title: String, val highVolts: Int, val defaultOutput: Int,
                                  val kva: Int, val x: Double, val height: Double, val spacing: Double) {
    DISTRIBUTION("Configurable Distribution Substation Transformer",12500,400,250,1.03,1.23,.71),
    POWER("Configurable Medium Power Transformer",69000,12500,5_000,1.42,1.61,.93),
    TRANSMISSION("Configurable Transmission Transformer",230000,69000,90_000,1.86,2.29,1.27);
    val id get()=14104+ordinal
    val tiers get()=yardVoltages.filter { it<=highVolts }
    val far get()=x+.21
    val halfWidth get()=spacing+.31
    val outputHeight get()=height*.68
}

class ModularTransformerDescriptor(val kind: ModularTransformerKind) :
    GridDescriptor(kind.title,SubstationStock.model,ModularTransformerElement::class.java,
        ModularTransformerRender::class.java,"textures/wire.png",ElnDeviceRegistry.veryHighVoltageCableDescriptor) {
    private val models = mapOf(1 to EquipmentTerminals(1,kind.x,kind.height,kind.spacing,kind.outputHeight),
        3 to EquipmentTerminals(3,kind.x,kind.height,kind.spacing,kind.outputHeight))
    internal fun terminals(phases: Int)=models.getValue(phases)
    init {
        ghostGroup=GhostGroup().apply {
            val dx=ceil(kind.far-.5).toInt();val dz=ceil(kind.halfWidth-.5).toInt()
            for(x in -dx..dx)for(y in 0..ceil(kind.height+.11-.5).toInt())for(z in -dz..dz)
                if(x!=0 || y!=0 || z!=0)addElement(x,y,z,Eln.ghostBlock,GhostBlock.tSelectableClearance)
        }
    }
    override fun rotationIsFixed()=true
    override fun isFloor(below: net.minecraft.world.level.block.Block) =
        (below as? TransparentNodeBlock)?.descriptor.let {
            (it as? SubstationSupportDescriptor)?.kind==SubstationSupportKind.BASE || super.isFloor(below)
        }

    fun drawMachine(front: Direction, phases: Int, tripped: Boolean, fanAngle: Double) {
        glPushMatrix()
        try {
            front.glRotateXnRef()
            val x=kind.x
            val z=kind.halfWidth
            val tankTop=kind.height*.49
            SubstationStock.draw {
                for(side in listOf(-1,1)) {
                    box(-x,-.5,side*z-.075,x,-.36,side*z+.075,0x424f59)
                    box(-x+.12,-.36,side*z-.04,x-.12,-.28,side*z+.04,0x76858a)
                }
                box(-x*.72,-.37,-z*.75,x*.72,tankTop,z*.75,0x556c74)
                box(-x*.76,tankTop-.059,-z*.79,x*.76,tankTop+.059,z*.79,0x697981)
                box(-x*.65,-.22,-z*.80,x*.65,-.12,z*.80,0x415059)
                for(side in listOf(-1,1))repeat(8) { i ->
                    val p=x*(-.64+i*.182)
                    val a=side*z*.71
                    val b=side*z*.92
                    box(p,-.23,minOf(a,b),p+.075,tankTop-.12,maxOf(a,b),0x70848c)
                }
                if(kind!=ModularTransformerKind.DISTRIBUTION) {
                    box(-x*.53,tankTop+.19,-.19,x*.55,tankTop+.47,.19,0x617985)
                    box(-x*.48,tankTop+.47,-.14,x*.50,tankTop+.54,.14,0x768d94)
                    for(side in listOf(-1,1))
                        box(side*x*.37-.056,tankTop+.06,-.055,side*x*.37+.056,tankTop+.23,.055,0x4c6165)
                    glPushMatrix()
                    try {
                        glTranslated(.37,.12,z*.95)
                        box(-.18,-.18,-.022,.18,.18,.022,0x3b444b)
                        glRotated(fanAngle,0.0,0.0,1.0)
                        box(-.14,-.034,.027,.14,.034,.047,0x8f9ba1)
                        box(-.034,-.14,.027,.034,.14,.047,0x8f9ba1)
                        box(-.046,-.046,.048,.046,.046,.069,0x47555e)
                    } finally { glPopMatrix() }
                }
                for(bank in listOf(32,36))repeat(phases) { i ->
                    val clamp=terminals(phases).point(bank+i,0)
                    val y=clamp.y
                    val bottom=if(bank==32)tankTop*.54 else tankTop*.25
                    val tankMount=if(clamp.x<0) -x*.62 else x*.62
                    box(minOf(tankMount,clamp.x),bottom-.115,clamp.z-.092,
                        maxOf(tankMount,clamp.x),bottom+.018,clamp.z+.092,0x53646b)
                    box(clamp.x-.118,bottom-.13,clamp.z-.126,clamp.x+.118,bottom+.052,clamp.z+.126,0x53646b)
                    box(clamp.x-.060,bottom,clamp.z-.060,clamp.x+.060,y,clamp.z+.060,0xb0bfb8)
                    val ribs=if(bank==32)7 else 4
                    repeat(ribs) { j ->
                        val ribY=bottom+(y-bottom)*(.1+.76*j/(ribs-1))
                        box(clamp.x-.103,ribY,clamp.z-.085,clamp.x+.103,ribY+.037,clamp.z+.085,0x7f9da1)
                        box(clamp.x-.080,ribY+.001,clamp.z-.103,clamp.x+.080,ribY+.038,clamp.z+.103,0x8aa5a6)
                    }
                    box(clamp.x-.077,y-.059,clamp.z-.072,clamp.x+.077,y+.052,clamp.z+.072,0xb89357)
                }
                box(-.38,-.23,z*.78,.02,.16,z*.94,0x465961)
                box(-.305,.043,z*.94,-.248,.104,z*.97,if(tripped) 0xe14f3e else 0x52bb85)
            }
        } finally { glPopMatrix() }
    }

    override fun drawItem(entity: Boolean) {
        glPushMatrix()
        try {
            val scale=.62/kind.far
            glScaled(scale,scale,scale)
            glTranslated(0.0,-kind.height*.40,0.0)
            glRotated(-35.0,0.0,1.0,0.0)
            drawMachine(Direction.XN,3,false,15.0)
        } finally { glPopMatrix() }
    }
    override fun addInformation(stack: ItemStack,player: Player?,list: MutableList<String>,advanced: Boolean) {
        list.add(tr("Up to %1$ V and %2$ kVA across the active windings",kind.highVolts,kind.kva))
        list.add(tr("Configurable step-up or step-down ratio, independent phase windings"))
    }
}

class ModularTransformerElement(node: TransparentNode, descriptor: TransparentNodeDescriptor) :
    GridElement(node,descriptor,192), AimedGridTerminals {
    private val spec=descriptor as ModularTransformerDescriptor
    private val kind=spec.kind
    private val inventory=TransparentNodeElementInventory(0,1,this)
    override fun getInventory()=inventory
    val inputs=List(3){NbtElectricalLoad("yardTransformerIn$it").apply { rs=.00001 }}
    val outputs=List(3){NbtElectricalLoad("yardTransformerOut$it").apply { rs=.00001 }}
    private val winding=List(3){NbtElectricalLoad("yardTransformerWinding$it")}
    private val taps=inputs.zip(winding){ a,b->ServiceTap(a,b,kind.defaultOutput.toDouble()/kind.highVolts) }
    private val losses=winding.zip(outputs,::Resistor)
    private val protection=SubstationProtection()
    private val oil=NbtThermalLoad("yardOil",0.0,65.0/(kind.kva*8),1.0,kind.kva*650.0)
    private val terms get()=spec.terminals(phases)

    var primary=kind.highVolts
        private set
    var secondary=kind.defaultOutput
        private set
    var phases=3
        private set
    var tap=0
        private set
    var ratingPercent=100
        private set
    var oilLimit=110
        private set
    var energized=true
        private set
    var oilTrip=false
        private set
    val tripped get()=oilTrip || protection.tripped
    val oilC get()=oil.Tc+20
    val ratio get()=secondary.toDouble()/primary*(1+tap*.0125)
    private val perPhase get()=kind.kva*1000.0*ratingPercent/100/phases

    init {
        electricalLoadList.addAll(inputs+outputs+winding)
        for(pin in inputs+outputs+winding)electricalComponentList.add(Resistor(pin,null).apply { r=1e12 })
        electricalComponentList.addAll(taps)
        electricalComponentList.addAll(losses)
        oil.setAsSlow()
        thermalLoadList.add(oil)
        electricalProcessList.add(IProcess { dt ->
            val currentRatio=(0 until phases).maxOf { i ->
                maxOf(abs(taps[i].current)*primary/perPhase,abs(losses[i].current)*secondary/perPhase)
            }
            if(energized) {
                oil.moveEnergyTo((0 until phases).sumOf { i -> val a=losses[i].current; a*a*losses[i].r }*dt)
                if (protection.sample(dt,voltageRatio(),currentRatio) || oilC>oilLimit) {
                    if(oilC>oilLimit)oilTrip=true
                    energized=false
                    applyWindings()
                    needPublish()
                }
            } else {
                protection.sample(dt,0.0,0.0)
            }
        })
        slowProcessList.add(NodePeriodicPublishProcess(node,.65,.2))
        applyWindings()
    }

    private fun voltageRatio()=(0 until phases).maxOf { i -> maxOf(abs(inputs[i].u)/primary,abs(outputs[i].u)/secondary) }
    private fun applyWindings() {
        for(i in taps.indices) {
            taps[i].enabled=energized && !tripped && i<phases
            taps[i].ratio=ratio
            // Winding loss is calibrated at the nameplate rating of the active windings.
            losses[i].r=secondary.toDouble()*secondary/(kind.kva*1000.0/phases)*.008
        }
    }
    fun configure(p: Int,s: Int,windings: Int,tapStep: Int,capacity: Int,temperature: Int): Boolean {
        if(p !in kind.tiers || s !in kind.tiers || s.toDouble()/p !in .02..50.0 ||
            windings !in listOf(1,3) || tapStep !in -4..4 || capacity !in listOf(50,75,100) || temperature !in listOf(90,110,130))
            return false
        if(windings!=phases && (gridLinkList.isNotEmpty() || gridLinksBooting.isNotEmpty()))return false
        primary=p;secondary=s;phases=windings;tap=tapStep;ratingPercent=capacity;oilLimit=temperature
        applyWindings()
        needPublish()
        return true
    }
    fun toggle(): Boolean {
        if(tripped) {
            if(oilC>oilLimit-15 || !protection.reset(voltageRatio()))return false
            oilTrip=false
            energized=true
        } else energized=!energized
        applyWindings()
        needPublish()
        return true
    }

    override fun gridTerminalAt(side: Direction,point: Vec3)=terms.closest(point,front)
    override fun gridBundleFor(terminal: Int)=terms.bundle(terminal)
    override fun getGridElectricalLoad(side: Direction)=inputs.first()
    override fun gridLoads(terminal: Int): List<NbtElectricalLoad> {
        val bank=when(terms.bank(terminal)) { 0->inputs;1->outputs;else->return emptyList() }
        return terms.phase(terminal)?.let { listOf(bank[it]) } ?: bank.takeIf { terms.count(terminal)==3 }.orEmpty()
    }
    override fun gridPhase(terminal: Int)=terms.phase(terminal).takeIf { phases>1 }
    override fun gridCircuitKey(terminal: Int)=terms.circuitKey(terminal)
    override fun renderWireCount(terminal: Int)=terms.count(terminal)
    override fun gridCablePoint(terminal: Int,wire: Int)=terms.point(terminal,wire)
    override fun renderedGridCablePoint(terminal: Int,wire: Int)=front.rotateFromXN(gridCablePoint(terminal,wire))
    override fun getConnectionMask(side: Direction,lrdu: LRDU)=0
    override fun getThermalLoad(side: Direction,lrdu: LRDU)=null
    override fun getElectricalLoad(side: Direction,lrdu: LRDU)=null
    override fun hasGui()=true
    override fun multiMeterString(side: Direction)=Utils.plotUIP(outputs[0].u,outputs[0].i)
    override fun thermoMeterString(side: Direction)=Utils.plotCelsius("Oil",oilC)
    override fun getWaila()=linkedMapOf(tr("Winding ratio") to formatRatio(ratio),
        tr("Oil temperature") to Utils.plotCelsius("",oilC),tr("Supply") to
            if(tripped)tr("Protection tripped") else if(energized)tr("Enabled") else tr("Disabled"))

    override fun writeToNBT(tag: CompoundTag) {
        super.writeToNBT(tag)
        protection.write(tag)
        tag.putInt("yardPrimary",primary);tag.putInt("yardSecondary",secondary)
        tag.putInt("yardWindings",phases);tag.putInt("yardTap",tap);tag.putInt("yardRating",ratingPercent)
        tag.putInt("yardOilLimit",oilLimit);tag.putBoolean("yardOilTrip",oilTrip);tag.putBoolean("yardEnergized",energized)
    }
    override fun readFromNBT(tag: CompoundTag) {
        super.readFromNBT(tag)
        protection.read(tag)
        primary=tag.getIntOr("yardPrimary",kind.highVolts).takeIf { it in kind.tiers } ?: kind.highVolts
        secondary=tag.getIntOr("yardSecondary",kind.defaultOutput).takeIf { it in kind.tiers && it.toDouble()/primary in .02..50.0 } ?: primary
        phases=tag.getIntOr("yardWindings",3).takeIf { it==1 || it==3 } ?: 3
        tap=tag.getIntOr("yardTap",0).coerceIn(-4,4)
        ratingPercent=tag.getIntOr("yardRating",100).takeIf { it in listOf(50,75,100) } ?: 100
        oilLimit=tag.getIntOr("yardOilLimit",110).takeIf { it in listOf(90,110,130) } ?: 110
        oilTrip=tag.getBooleanOr("yardOilTrip",false)
        energized=!tripped && tag.getBooleanOr("yardEnergized",true)
        applyWindings()
    }
    override fun networkSerialize(out: DataOutputStream) {
        super.networkSerialize(out)
        out.writeInt(primary);out.writeInt(secondary);out.writeByte(phases);out.writeByte(tap)
        out.writeByte(ratingPercent);out.writeByte(oilLimit)
        out.writeBoolean(energized);out.writeBoolean(tripped);out.writeDouble(oilC);out.writeDouble(outputs[0].u)
    }
    override fun networkUnserialize(input: DataInputStream): Byte {
        val action=super.networkUnserialize(input)
        fun nextTier(value: Int,other: Int,primaryBank: Boolean): Int {
            val valid=kind.tiers.filter { if(primaryBank)other.toDouble()/it in .02..50.0 else it.toDouble()/other in .02..50.0 }
            return valid[(valid.indexOf(value)+1)%valid.size]
        }
        when(action.toInt()) {
            80->configure(nextTier(primary,secondary,true),secondary,phases,tap,ratingPercent,oilLimit)
            81->configure(primary,nextTier(secondary,primary,false),phases,tap,ratingPercent,oilLimit)
            82->configure(primary,secondary,if(phases==3)1 else 3,tap,ratingPercent,oilLimit)
            83->configure(primary,secondary,phases,if(tap==4)-4 else tap+1,ratingPercent,oilLimit)
            84->configure(primary,secondary,phases,tap,if(ratingPercent==100)50 else ratingPercent+25,oilLimit)
            85->configure(primary,secondary,phases,tap,ratingPercent,if(oilLimit==130)90 else oilLimit+20)
            86->toggle()
        }
        return action
    }
}

class ModularTransformerRender(entity: TransparentNodeEntity, descriptor: TransparentNodeDescriptor) : GridRender(entity,descriptor) {
    var primary=0;var secondary=0;var phases=3;var tap=0;var rating=100;var limit=110
    var energized=false;var tripped=false;var oil=20.0;var volts=0.0
    private var fan=0.0
    override fun networkUnserialize(input: DataInputStream) {
        super.networkUnserialize(input)
        primary=input.readInt();secondary=input.readInt();phases=if(input.readUnsignedByte()==1)1 else 3
        tap=input.readByte().toInt();rating=input.readUnsignedByte();limit=input.readUnsignedByte()
        energized=input.readBoolean();tripped=input.readBoolean();oil=input.readDouble();volts=input.readDouble()
    }
    override fun refresh(deltaT: Float) {
        super.refresh(deltaT)
        if(energized && oil>50)fan=(fan+deltaT*175)%360
    }
    override fun renderBounds(): AABB {
        val model=AABB(tileEntity.blockPos).inflate(4.0)
        return super.renderBounds()?.minmax(model) ?: model
    }
    override fun draw() {
        drawCables()
        (transparentNodedescriptor as ModularTransformerDescriptor).drawMachine(front,phases,tripped,fan)
    }
}

class ModularTransformerScreen(menu: ElnNodeMenu,inv: Inventory,title: Component,render: Any): ElnScreen(menu,inv,title,248,162) {
    private val view=render as ModularTransformerRender
    override fun init() {
        super.init()
        button(8,25,113,{tr("Primary %1$ V",view.primary)},{view.clientSendId(80)})
        button(127,25,113,{tr("Secondary %1$ V",view.secondary)},{view.clientSendId(81)})
        button(8,50,113,{tr("Windings: %1$",view.phases)},{view.clientSendId(82)})
        button(127,50,113,{tr("Tap step: %1$",view.tap)},{view.clientSendId(83)})
        button(8,75,113,{tr("Rating: %1$%",view.rating)},{view.clientSendId(84)})
        button(127,75,113,{tr("Oil trip: %1$ C",view.limit)},{view.clientSendId(85)})
        button(8,101,232,{if(view.tripped)tr("Reset protection") else if(view.energized)tr("Disable windings") else tr("Enable windings")},
            {view.clientSendId(86)})
    }
    override fun extractDevice(g: GuiGraphicsExtractor,mx: Int,my: Int,seconds: Float) {
        val ratio=if(view.primary>0)view.secondary.toDouble()/view.primary*(1+view.tap*.0125) else 0.0
        label(g,8,129,tr("Ratio %1$   Output %2$",formatRatio(ratio),Utils.plotVolt("",view.volts)))
        label(g,8,142,Utils.plotCelsius("Oil",view.oil))
    }
}

object ModularTransformers {
    @JvmStatic fun register() = ModularTransformerKind.entries.forEach {
        Eln.transparentNodeItem.addDescriptor(it.id,ModularTransformerDescriptor(it))
    }
}
