package mods.eln.gametest

import mods.eln.Eln
import mods.eln.i18n.I18N.tr
import mods.eln.misc.Direction
import mods.eln.vehicle.*
import mods.eln.transparentnode.industrial.WorkshopParts
import net.minecraft.commands.CommandSourceStack
import net.minecraft.core.BlockPos
import net.minecraft.network.chat.Component
import net.minecraft.world.SimpleMenuProvider
import net.minecraft.world.entity.Relative

/** Operator-only live cars and stations backed by normal saved source blocks and traction packs. */
object RoadVehicleLab {
    @JvmStatic fun build(src:CommandSourceStack,origin:BlockPos):Int {
        val c=WorldTestContext(src.level,origin)
        for(x in 0..32)for(z in 0..32)c.floor(c.at(x,0,z))
        val rows=com.google.gson.JsonArray()
        for(kind in RoadVehicleKind.entries){
            val at=c.at(4,1,4+kind.ordinal*10)
            val station=c.placeTransparent(8079,at) as VehicleChargerElement
            val feed=at.relative(station.front.left().toEnumFacing());c.floor(feed.below())
            val source=c.placeSix(192,feed) as mods.eln.sixnode.electricalsource.ElectricalSourceElement
            source.networkUnserialize(c.stream{it.writeByte(1);it.writeFloat(240f)})
            val car=RoadVehicleEntity((if(kind==RoadVehicleKind.CART)RoadVehicles.cart else RoadVehicles.coupe).get(),src.level,kind)
            car.snapTo(at.x+4.5,at.y.toDouble(),at.z+.5,0f,0f)
            repeat(kind.packs){i->val pack=Eln.findItemStack(WorkshopParts.PACK,1);val battery=RoadVehicles.battery(pack)!!;battery.setEnergy(pack,battery.getEnergyMax(pack)*.5);car.items.setItem(i,pack)}
            check(src.level.addFreshEntity(car));check(station.attach(car))
            rows.add(com.google.gson.JsonObject().apply{addProperty("kind",kind.name);addProperty("entityId",car.id);addProperty("x",car.x);addProperty("y",car.y);addProperty("z",car.z);addProperty("station",at.toShortString())})
        }
        java.nio.file.Files.writeString(java.nio.file.Path.of("eln-road-vehicle-gallery.json"),com.google.gson.GsonBuilder().setPrettyPrinting().create().toJson(rows)+"\n")
        src.sendSuccess({Component.literal("Cart, coupe and two real240V charging stations; positions in eln-road-vehicle-gallery.json")},true)
        return 2
    }
    @JvmStatic fun menu(src:CommandSourceStack,ordinal:Int):Int {
        val player=src.playerOrException;val kind=RoadVehicleKind.entries.getOrNull(ordinal)?:return 0
        val car=src.level.getEntitiesOfClass(RoadVehicleEntity::class.java,player.boundingBox.inflate(512.0)).filter{it.kind==kind}.minByOrNull{it.distanceToSqr(player)}?:return 0
        player.teleportTo(src.level,car.x+3,car.y,car.z,emptySet<Relative>(),player.yRot,player.xRot,true)
        player.openMenu(SimpleMenuProvider({id,inv,_->VehicleMenu(id,inv,car)},Component.literal(tr(kind.title)))){it.writeVarInt(car.id)}
        return 1
    }
}
