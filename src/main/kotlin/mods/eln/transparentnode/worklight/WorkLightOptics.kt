package mods.eln.transparentnode.worklight

import mods.eln.misc.Direction

object WorkLightOptics {
    fun pivot(head: Int)=net.minecraft.world.phys.Vec3(if(head==0)-.32 else .32,2.15,.12)
    fun lens(head: Int)=net.minecraft.world.phys.Vec3(if(head==0)-.32 else .32,2.15,.36)
    fun world(front: Direction,v: net.minecraft.world.phys.Vec3): net.minecraft.world.phys.Vec3 {
        val r=DoubleArray(3);front.right().applyTo(r,v.x);front.applyTo(r,v.z)
        return net.minecraft.world.phys.Vec3(r[0],v.y,r[2])
    }
}
