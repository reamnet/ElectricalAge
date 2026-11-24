package mods.eln.node.voxel

import mods.eln.cable.CableRenderDescriptor
import mods.eln.sixnode.electricalcable.UtilityCableDescriptor
import mods.eln.sixnode.electricalcable.UtilityCableMaterial
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class AwgGeometryTest {
    // Exercise the real part constructor without installing a world or changing descriptor registries.
    private val constructor = AwgCablePart::class.java.getDeclaredConstructor(
        UtilityCableDescriptor::class.java, Int::class.javaPrimitiveType, IntArray::class.java
    ).apply { isAccessible = true }

    @Test fun crossSectionsRoundUpToVoxelsOnEveryMountingFace() {
        // Pixel sizes used by the single-conductor catalog, including the sub-voxel floor.
        for ((pixels, expectedWidth, expectedHeight) in listOf(
            Triple(.65f, 1, 1), Triple(.95f, 1, 1), Triple(1.25f, 2, 1),
            Triple(1.75f, 2, 2), Triple(2.35f, 3, 3), Triple(3.25f, 4, 3)
        )) {
            val wire = wire(UtilityCableMaterial.COPPER, true, pixels)
            for (face in 0..5) {
                val normal = CablePart.normalAxis(face)
                val along = (normal + 1) % 3
                val points = IntArray(6) { 8 }
                points[normal] = CablePart.plane(face)
                points[normal + 3] = points[normal]
                points[along] = 0
                points[along + 3] = 16
                val part = constructor.newInstance(wire, face, points)
                assertEquals(expectedWidth, part.visual().tierInfo().width())
                assertEquals(expectedHeight, part.visual().tierInfo().height())
                val boxes = part.boxes()
                assertFalse(boxes.isEmpty())
                for (box in boxes) {
                    for (axis in 0..2) {
                        assertTrue(box[axis] >= 0 && box[axis + 3] <= 16, box.contentToString())
                        assertTrue(box[axis] < box[axis + 3], box.contentToString())
                    }
                }
                assertTrue(boxes.any { it[normal] == points[normal] || it[normal + 3] == points[normal] })
            }
        }
    }

    private fun wire(material: UtilityCableMaterial, insulated: Boolean, pixels: Float) =
        UtilityCableDescriptor("AWG test", material, 3.309, 20.0, insulated, if (insulated) 600.0 else 0.0,
            false, 105.0, "AWG render fixture", CableRenderDescriptor("eln", "sprites/cable.png", pixels, maxOf(.65f, pixels - .3f)))

    @Test fun bareMetalsKeepTheirTintWhileInsulationUsesTheJacketColour() {
        for (material in UtilityCableMaterial.entries) for (insulated in listOf(false, true)) {
            val part = constructor.newInstance(wire(material, insulated, 1.25f), 0, intArrayOf(0, 0, 8, 16, 0, 8))
            val expected = if (insulated) 0 else if (material == UtilityCableMaterial.COPPER) 0xffb87345.toInt() else 0xffaeb7bd.toInt()
            assertEquals(expected, part.bareTint())
        }
    }
}
