package mods.eln.gridnode

import mods.eln.cable.CableRenderDescriptor
import mods.eln.sixnode.electricalcable.*
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import java.io.*

class GridCableAppearanceTest {
    private fun wire(metal: UtilityCableMaterial,pixels: Float,insulated: Boolean)=UtilityCableDescriptor(
        "fixture",metal,3.309,20.0,insulated,if(insulated)600.0 else 0.0,false,180.0,"fixture",
        CableRenderDescriptor("eln","sprites/white.png",pixels,.65f))
    @Test fun appearanceUsesDescriptorThicknessAndPreservesMetalVersusInsulation() {
        for(metal in UtilityCableMaterial.entries) {
            val thin=GridCableAppearance.of(wire(metal,.65f,false))
            val thick=GridCableAppearance.of(wire(metal,3.25f,false))
            assertEquals(5.0,thick.radius().toDouble()/thin.radius(),1e-6)
            assertEquals(if(metal==UtilityCableMaterial.COPPER)0xffb87345.toInt() else 0xffaeb7bd.toInt(),thin.tint())
            assertTrue(thin.metal())
            val insulated=GridCableAppearance.of(wire(metal,.65f,true))
            assertFalse(insulated.metal())
            assertEquals(-1,insulated.tint())
        }
    }
    @Test fun codecRoundTripsAndRejectsTruncatedOrInvalidGeometry() {
        val valid=GridCableAppearance(.02f,0xffb87345.toInt(),true)
        val out=ByteArrayOutputStream();valid.write(DataOutputStream(out))
        val bytes=out.toByteArray()
        assertEquals(valid,GridCableAppearance.read(DataInputStream(ByteArrayInputStream(bytes))))
        for(size in 0 until bytes.size)assertThrows(IOException::class.java) {
            GridCableAppearance.read(DataInputStream(ByteArrayInputStream(bytes.copyOf(size))))
        }
        for(radius in listOf(Float.NaN,Float.POSITIVE_INFINITY,0f,-.1f,1.01f))assertThrows(IOException::class.java) {
            val bad=ByteArrayOutputStream();GridCableAppearance(radius,-1,false).write(DataOutputStream(bad))
            GridCableAppearance.read(DataInputStream(ByteArrayInputStream(bad.toByteArray())))
        }
        for(bad in listOf(bytes.copyOf().also { it[4]=0 },bytes.copyOf().also { it[8]=2 }))
            assertThrows(IOException::class.java) { GridCableAppearance.read(DataInputStream(ByteArrayInputStream(bad))) }
    }
}
