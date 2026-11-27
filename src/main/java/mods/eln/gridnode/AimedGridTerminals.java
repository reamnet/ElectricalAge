package mods.eln.gridnode;

import mods.eln.misc.Direction;
import net.minecraft.world.phys.Vec3;

/** Devices with physically distinct clamp locations can select the conductor under the player's hit. */
public interface AimedGridTerminals {
    int gridTerminalAt(Direction side, Vec3 ownerLocalPoint);
    int gridBundleFor(int terminal);
}
