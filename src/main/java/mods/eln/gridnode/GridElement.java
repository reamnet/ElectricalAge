package mods.eln.gridnode;

import mods.eln.generic.GenericItemBlockUsingDamageDescriptor;
import mods.eln.misc.*;
import mods.eln.node.transparent.TransparentNode;
import mods.eln.node.transparent.TransparentNodeDescriptor;
import mods.eln.node.transparent.TransparentNodeElement;
import mods.eln.sim.ElectricalLoad;
import mods.eln.sim.ThermalLoad;
import mods.eln.sixnode.electricalcable.ElectricalCableDescriptor;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.phys.Vec3;
import org.apache.commons.lang3.tuple.Pair;

import java.io.DataOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.UUID;

/**
 * Created by svein on 22/08/15.
 */
abstract public class GridElement extends TransparentNodeElement {
    /**
     * The last place any given player tried to link two instance nodes.
     */
    private record Pending(Coordonate coordinate, int terminal) { }
    private static final HashMap<UUID, Pending> pending = new HashMap<>();
    public final HashSet<GridLink> gridLinkList = new HashSet<>();
    public final HashSet<GridLink> gridLinksBooting = new HashSet<>();
    protected GridDescriptor desc;
    public final int connectRange;
    private float idealRenderingAngle;
    private boolean gridConnecting;
    private final SpanAmbientSample spanAmbient=new SpanAmbientSample();
    public double spanAmbientCelsius() { return spanAmbient.sample(coordonate().world().getGameTime(),()->mods.eln.sim.Ambient.relative(coordonate())+20.0); }

    public GridElement(TransparentNode node, TransparentNodeDescriptor descriptor, int range) {
        super(node, descriptor); desc = (GridDescriptor)descriptor; connectRange = range;
    }
    public boolean gridReady() {
        var veto = mods.eln.node.NodeBase.simulationVeto;
        return !node.isDestructing() && !isBurnt() && (node.isAdded() || gridConnecting) && (veto == null || !veto.test(node));
    }
    public boolean allowsNewGridLinks() { return true; }
    public boolean acceptsGridCable(ElectricalCableDescriptor cable) { return GridCablePolicy.accepts(cable); }
    public boolean acceptsSavedGridTerminal(int terminal) { return !gridLoads(terminal).isEmpty(); }
    public int selectedGridTerminal(Direction side) { return side.getInt(); }
    public java.util.List<ElectricalLoad> gridLoads(int terminal) {
        Direction side=Direction.fromInt(terminal);
        ElectricalLoad load=side==null?null:getGridElectricalLoad(side);
        return load==null?java.util.List.of():java.util.List.of(load);
    }
    public String gridCircuitKey(int terminal) { return "LEGACY"; }
    public int renderWireCount(int terminal) { return 2; }
    public Vec3 gridCablePoint(int terminal,int wire) { return getCablePoint(Direction.fromInt(terminal),wire); }
    public Vec3 renderedGridCablePoint(int terminal,int wire) { return gridCablePoint(terminal,wire).yRot((float)Math.toRadians(idealRenderingAngle)); }

    @Override
    public boolean onBlockActivated(Player player, Direction side, float x, float y, float z) {
        ItemStack stack=player.getMainHandItem();
        var item=GenericItemBlockUsingDamageDescriptor.getDescriptor(stack);
        if (!(item instanceof ElectricalCableDescriptor cable)) return false;
        UUID id=player.getUUID();
        int terminal=selectedGridTerminal(side);
        if (!allowsNewGridLinks() || !acceptsGridCable(cable) || !GridCablePolicy.acceptsTerminal(cable,terminal) || !GridCablePlan.acceptsChannels(cable,gridLoads(terminal).size())) {
            Utils.sendMessage(player,mods.eln.i18n.I18N.tr("Select an overhead grid terminal and intact power cable"));pending.remove(id);return true;
        }
        var first=pending.get(id);
        GridElement other=first==null?null:GridLink.getElementFromCoordinate(first.coordinate());
        if (other==this) { pending.remove(id);Utils.sendMessage(player,mods.eln.i18n.I18N.tr("Cancelled connection"));return true; }
        if (other==null) {
            pending.put(id,new Pending(coordonate(),terminal));Utils.sendMessage(player,mods.eln.i18n.I18N.tr("Setting starting point"));return true;
        }
        pending.remove(id);
        int length=GridCablePlan.requiredUnits(cable,(int)Math.ceil(other.coordonate().trueDistanceTo(coordonate())),gridLoads(terminal).size());
        var utility=cable instanceof mods.eln.sixnode.electricalcable.UtilityCableDescriptor wire?wire:null;
        boolean enough=utility==null?stack.getCount()>=length:stack.getCount()==1 && (player.isCreative() || utility.getRemainingLengthMeters(stack)+1e-6>=length);
        if (!enough) { Utils.sendMessage(player,mods.eln.i18n.I18N.tr("You need %1$ units of cable",length));return true; }
        try {
            if (!canConnect(other) || !validLOS(other)) throw new IllegalArgumentException(mods.eln.i18n.I18N.tr("Cannot connect these two objects"));
            ItemStack paid=utility==null?stack.copyWithCount(length):utility.segment(stack,(double)length);
            if (GridLink.addLink(other,this,first.terminal(),terminal,cable,length,paid)) {
                if(utility==null)stack.shrink(length);else if(!player.isCreative())utility.consume(stack,(double)length);player.getInventory().setChanged();Utils.sendMessage(player,mods.eln.i18n.I18N.tr("Added connection"));
            } else Utils.sendMessage(player,mods.eln.i18n.I18N.tr("Already connected"));
        } catch (IllegalArgumentException ex) { Utils.sendMessage(player,ex.getMessage()); }
        return true;
    }

    @Override
    public void initialize() {
        connect();
        for (GridLink link : new HashSet<>(gridLinksBooting)) link.connect();
        gridLinksBooting.clear();
        updateIdealRenderAngle();
    }

    @Override
    public void connectJob() {
        super.connectJob();
        gridConnecting=true;
        try { for (GridLink link : new HashSet<>(gridLinkList)) link.connect(); }
        finally { gridConnecting=false; }
    }

    @Override
    public void disconnectJob() {
        // Spans must leave before either endpoint's electrical states do.
        for (GridLink link : new HashSet<>(gridLinkList)) link.disconnect();
        super.disconnectJob();
    }

    @Override
    public void onBreakElement() {
        super.onBreakElement();
        HashSet<GridLink> copy = new HashSet<GridLink>(gridLinkList);
        for (GridLink link : copy) {
            node.dropItem(link.onBreakElement());
        }
    }

    @Override
    public void selfDestroy() {
        super.selfDestroy();
        HashSet<GridLink> copy = new HashSet<GridLink>(gridLinkList);
        for (GridLink link : copy) {
            link.selfDestroy();
        }
    }

    @Override
    public void writeToNBT(CompoundTag nbt) {
        super.writeToNBT(nbt);

        Integer i = 0;
        CompoundTag gridLinks = Utils.newNbtTagCompund(nbt, "gridLinks");
        for (GridLink link : gridLinkList.stream().sorted(java.util.Comparator.comparing(link -> { var tag=new CompoundTag();link.writeToNBT(tag, "");return tag.toString(); })).toList()) {
            link.writeToNBT(Utils.newNbtTagCompund(gridLinks, i.toString()), "");
            i++;
        }
    }

    @Override
    public void readFromNBT(CompoundTag nbt) {
        super.readFromNBT(nbt);

        assert gridLinkList.isEmpty();
        final CompoundTag gridLinks = nbt.getCompoundOrEmpty("gridLinks");
        for (Integer i = 0; ; i++) {
            final CompoundTag linkTag = gridLinks.getCompoundOrEmpty(i.toString());
            if (linkTag.isEmpty())
                break;
            gridLinksBooting.add(new GridLink(linkTag, ""));
        }
    }

    abstract protected ElectricalLoad getGridElectricalLoad(Direction side);

    // TODO: This should check if the wire isn't passing through blocks.
    private boolean validLOS(GridElement other) {
        return true;
    }

    // Return false if connecting grid elements that can't connect.
    protected boolean canConnect(GridElement other) {
        return true;
    }

    // TODO: One pole turns, all connected cables should be recalculated,
    // not just the ones being rendered here.
    /* Compute a rendering angle that minimizes any straight-on cables. */
    public void updateIdealRenderAngle() {
        if (desc.rotationIsFixed()) {
            switch (front) {
                case XN:
                    idealRenderingAngle = 0;
                    break;
                case XP:
                    idealRenderingAngle = 180;
                    break;
                case YN:
                    idealRenderingAngle = 90;
                    break;
                case YP:
                    idealRenderingAngle = 90;
                    break;
                case ZN:
                    idealRenderingAngle = 270;
                    break;
                case ZP:
                    idealRenderingAngle = 90;
                    break;
            }
            //System.out.println(idealRenderingAngle);
        } else if (gridLinkList.size() == 0) {
            idealRenderingAngle = 0;
        } else {
            // Compute angles.
            double angles[] = new double[gridLinkList.size()];
            int i = 0;
            for (GridLink link : gridLinkList) {
                Coordonate vec = link.a.subtract(link.b);
                // Angles 180 degrees apart are equivalent.
                if (vec.z < 0)
                    vec = vec.negate();
                double h = Math.sqrt(vec.x * vec.x + vec.z * vec.z);
                if (h > 1e-9) angles[i++] = Math.acos(vec.x / h);
            }
            // This could probably be optimised with a bit of math, but w.e.
            double optAngle = 0;
            double optErr = Double.POSITIVE_INFINITY;
            for (i = 0; i < 128; i++) {
                // Check a half-circle.
                double angle = Math.PI * i / 128.0;
                double error = 0;
                for (double a : angles) {
                    double err = Math.abs(Math.sin(angle - a));
                    error += err * err * err;
                }
                if (error < optErr) {
                    optAngle = angle;
                    optErr = error;
                }
            }
            idealRenderingAngle = (float) Math.toDegrees(-optAngle);
        }
    }

    @Override
    public void networkSerialize(DataOutputStream stream) {
        super.networkSerialize(stream);
        try {
            stream.writeFloat(idealRenderingAngle);
            // Each wire pair should be drawn by exactly one node.
            // Check for which ones it's this one.
            ArrayList<GridLink> ourLinks = new ArrayList<GridLink>();
            for (GridLink link : gridLinkList) {
                if (link.a.equals(coordonate()) && link.getOtherElement(this)!=null) {
                    ourLinks.add(link);
                }
            }
            // The renderer needs to know, for each catenary:
            // - Vec3 of the starting point.
            // - Vec3 of the end point.
            // There's a finite number of starting points, and a potentially unlimited number of endpoints...
            // But until we get protocol buffers or something, simple remains good.
            // So we'll just send pairs, even if there's some duplication.
            stream.writeInt(ourLinks.size());
            for (GridLink link : ourLinks) {
                GridElement target = link.getOtherElement(this);
                int terminal=link.getTerminal(this), remote=link.getTerminal(target);
                int count=renderWireCount(terminal);
                stream.writeInt(count);
                Coordonate offset=link.b.subtract(link.a);
                for (int i=0;i<count;i++) {
                    Vec3 start=renderedGridCablePoint(terminal,i);
                    Vec3 end=target.renderedGridCablePoint(remote,i).add(offset.x,offset.y,offset.z);
                    writeVec(stream,start);writeVec(stream,end);
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    protected Vec3 getCablePoint(Direction side, int i) {
        if (i >= 2) throw new AssertionError("Invalid cable point index");
        Obj3D.Obj3DPart part = (i == 0 ? desc.plus : desc.gnd).get(0);
        BoundingBox bb = part.boundingBox();
        return bb.centre();
    }

    private void writeVec(DataOutputStream stream, Vec3 sp) throws IOException {
        stream.writeFloat((float) sp.x);
        stream.writeFloat((float) sp.y);
        stream.writeFloat((float) sp.z);
    }

    @Override
    public String multiMeterString(Direction side) {
        ElectricalLoad electricalLoad = getGridElectricalLoad(side);
        return Utils.plotUIP(electricalLoad.getU(), electricalLoad.getI());
    }

    @Override
    public String thermoMeterString(Direction side) {
        ThermalLoad thermalLoad = getThermalLoad(side, LRDU.Up);
        return Utils.plotCelsius("T", thermalLoad.Tc);
    }
}
