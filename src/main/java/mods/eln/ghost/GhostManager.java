package mods.eln.ghost;



import net.minecraft.core.BlockPos;
import mods.eln.compat.WorldCompat;
import mods.eln.Eln;
import mods.eln.misc.Coordonate;
import mods.eln.misc.Utils;
import mods.eln.node.NodeBase;
import mods.eln.node.NodeManager;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.Level;

import java.util.Hashtable;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;

public class GhostManager {
    public GhostManager(String par1Str) {
    }

    Map<Coordonate, GhostElement> ghostTable = new Hashtable<Coordonate, GhostElement>();
    Map<Coordonate, GhostObserver> observerTable = new Hashtable<Coordonate, GhostObserver>();

    public void clear() {
        ghostTable.clear();
        observerTable.clear();
    }

    public void init() {
    }


	/*
    public void addGhost(GhostElement element) {
		ghostTable.put(element.elementCoordonate, element);
	}*/

    public GhostElement getGhost(Coordonate coordonate) {
        return ghostTable.get(coordonate);
    }

    /** Gives the ghost at c the id `to` if observer watches it with the id `from` (an observer renumbering its ghosts). */
    public void renumberGhost(Coordonate c, Coordonate observer, int from, int to) {
        GhostElement g = ghostTable.get(c);
        if (g != null && g.UUID == from && g.observatorCoordonate.equals(observer)) g.UUID = to;
    }

    public void removeGhost(Coordonate coordonate) {
        removeGhostNode(coordonate);
        ghostTable.remove(coordonate);
    }

    public void addObserver(GhostObserver observer) {
        observerTable.put(observer.getGhostObserverCoordonate(), observer);
    }

    public GhostObserver getObserver(Coordonate coordonate) {
        return observerTable.get(coordonate);
    }

    public void removeObserver(Coordonate coordonate) {
        observerTable.remove(coordonate);
    }

    public void removeGhostAndBlockWithObserver(Coordonate observerCoordonate) {
        Iterator<Entry<Coordonate, GhostElement>> iterator = ghostTable.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<Coordonate, GhostElement> entry = iterator.next();
            GhostElement element = entry.getValue();
            if (element.observatorCoordonate.equals(observerCoordonate)) {
                iterator.remove();
                removeGhostNode(element.elementCoordonate);
                WorldCompat.setBlockToAir(element.elementCoordonate.world(), element.elementCoordonate.x, element.elementCoordonate.y, element.elementCoordonate.z);
            }
        }
    }

    public void removeGhostAndBlockWithObserver(Coordonate observerCoordonate, int uuid) {
        Iterator<Entry<Coordonate, GhostElement>> iterator = ghostTable.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<Coordonate, GhostElement> entry = iterator.next();
            GhostElement element = entry.getValue();
            if (element.observatorCoordonate.equals(observerCoordonate) && element.getUUID() == uuid) {
                iterator.remove();
                removeGhostNode(element.elementCoordonate);
                WorldCompat.setBlockToAir(element.elementCoordonate.world(), element.elementCoordonate.x, element.elementCoordonate.y, element.elementCoordonate.z);
            }
        }
    }

    public void removeGhostAndBlockWithObserverAndNotUuid(Coordonate observerCoordonate, int uuid) {
        Iterator<Entry<Coordonate, GhostElement>> iterator = ghostTable.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<Coordonate, GhostElement> entry = iterator.next();
            GhostElement element = entry.getValue();
            if (element.observatorCoordonate.equals(observerCoordonate) && element.getUUID() != uuid) {
                iterator.remove();
                removeGhostNode(element.elementCoordonate);
                WorldCompat.setBlockToAir(element.elementCoordonate.world(), element.elementCoordonate.x, element.elementCoordonate.y, element.elementCoordonate.z);
            }
        }
    }

    public void removeGhostNode(Coordonate c) {
        NodeBase node = NodeManager.instance.getNodeFromCoordonate(c);
        if (node == null) return;
        node.onBreakBlock();
    }

    public void removeGhostAndBlock(Coordonate coordonate) {
        removeGhost(coordonate);
        WorldCompat.setBlockToAir(coordonate.world(), coordonate.x, coordonate.y, coordonate.z); //caca1.5.1
    }



    public void loadFromNBT(CompoundTag nbt) {
        for (CompoundTag o : Utils.getTags(nbt)) {
            CompoundTag tag = (CompoundTag) o;

            GhostElement ghost = new GhostElement();
            ghost.readFromNBT(tag, "");
            ghostTable.put(ghost.elementCoordonate, ghost);
        }
    }

    public void saveToNBT(CompoundTag nbt, int dim) {
        int nodeCounter = 0;

        for (GhostElement ghost : ghostTable.values()) {
            if (dim != Integer.MIN_VALUE && ghost.elementCoordonate.dimention != dim) continue;
            CompoundTag nbtGhost = new CompoundTag();
            ghost.writeToNBT(nbtGhost, "");
            nbt.put("n" + nodeCounter++, nbtGhost);
        }
    }

    public void unload(int dimensionId) {
        Iterator<GhostElement> i = ghostTable.values().iterator();

        while (i.hasNext()) {
            GhostElement n = i.next();
            if (n.elementCoordonate.dimention == dimensionId) {
                i.remove();
            }
        }
    }

    public boolean canCreateGhostAt(Level world, int x, int y, int z) {
        if (!WorldCompat.blockExists(world, x, y, z)) { // 1.7.10 chunkExists = chunk loaded
            return false;
        } else if (!world.getBlockState(new BlockPos(x, y, z)).canBeReplaced()) {
            return false;
        } else return true;
    }

    /** Restore a purely decorative reservation without waking adjacent chunks or replacing saved nodes. */
    public boolean createGhostIfAir(Coordonate at, Coordonate observer, int uuid, int meta) {
        if (meta < 0 || meta > 15 || !(at.world() instanceof net.minecraft.server.level.ServerLevel world)) return false;
        if (!world.getServer().isSameThread()) throw new IllegalStateException("Ghost repair requires the server thread");
        var chunk = world.getChunkSource().getChunkNow(at.x >> 4, at.z >> 4);
        if (chunk == null || !chunk.getBlockState(at.pos()).isAir() || ghostTable.containsKey(at)) return false;
        if (NodeManager.instance != null && (NodeManager.instance.getNodeFromCoordonate(at) != null || NodeManager.instance.hasOrphanAt(at))) return false;
        var state = Eln.ghostBlock.defaultBlockState().setValue(mods.eln.compat.BlockMeta.META, meta);
        // No removeBlock: even air variants can otherwise notify neighbours. Collision remains empty.
        if (!world.setBlock(at.pos(), state, Block.UPDATE_CLIENTS | Block.UPDATE_KNOWN_SHAPE)) return false;
        Coordonate key = new Coordonate(at);
        ghostTable.put(key, new GhostElement(key, observer, uuid));
        return true;
    }

    public void createGhost(Coordonate coordonate, Coordonate observerCoordonate, int UUID) {
        createGhost(coordonate, observerCoordonate, UUID, Eln.ghostBlock, GhostBlock.tCube);
    }

    public void createGhost(Coordonate coordonate, Coordonate observerCoordonate, int UUID, Block block, int meta) {
        // descriptors (and their ghost groups) are built before the ghost block is registered: null = the ghost block
        if (block == null) block = Eln.ghostBlock;
        WorldCompat.setBlockToAir(coordonate.world(), coordonate.x, coordonate.y, coordonate.z);
        if (WorldCompat.setBlock(coordonate.world(), coordonate.x, coordonate.y, coordonate.z, block, meta, 3)) {
            coordonate = new Coordonate(coordonate);
            GhostElement element = new GhostElement(coordonate, observerCoordonate, UUID);
            ghostTable.put(element.elementCoordonate, element);
        }
    }
}
